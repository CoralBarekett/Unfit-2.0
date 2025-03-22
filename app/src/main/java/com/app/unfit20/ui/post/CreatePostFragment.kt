// Updated CreatePostFragment.kt with logging for post creation and refreshing profile & home
package com.app.unfit20.ui.post

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentCreatePostBinding
import com.app.unfit20.ui.ViewModelFactory
import com.bumptech.glide.Glide

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val args: CreatePostFragmentArgs by navArgs()

    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory()
    }

    private var selectedImageUri: Uri? = null
    private var selectedLocation: String? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                showImagePreview(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupListeners()
        observeViewModel()

        if (!args.postId.isNullOrEmpty()) {
            loadPostForEditing(args.postId!!)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (binding.etPostContent.text.toString().isNotEmpty() || selectedImageUri != null) {
                showDiscardConfirmationDialog()
            } else {
                findNavController().navigateUp()
            }
        }

        val isEditing = !args.postId.isNullOrEmpty()
        binding.toolbar.title = if (isEditing) getString(R.string.edit_post) else getString(R.string.create_post)
    }

    private fun setupListeners() {
        binding.btnPost.setOnClickListener {
            val content = binding.etPostContent.text.toString().trim()

            if (content.isEmpty() && selectedImageUri == null) {
                Toast.makeText(requireContext(), R.string.post_content_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (args.postId.isNullOrEmpty()) {
                Log.d("CreatePost", "Creating new post...")
                viewModel.createPost(content, selectedImageUri, selectedLocation)
            } else {
                Log.d("CreatePost", "Updating post: ${args.postId}")
                viewModel.updatePost(args.postId!!, content, selectedImageUri, selectedLocation)
            }
        }

        binding.cardAddImage.setOnClickListener {
            openImageSelector()
        }

        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            binding.ivPostImagePreview.visibility = View.GONE
            binding.btnRemoveImage.visibility = View.GONE
        }

        binding.cardAddLocation.setOnClickListener {
            showLocationPicker()
        }
    }

    private fun observeViewModel() {
        viewModel.postState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostViewModel.PostState.Loading -> {
                    showLoading(true)
                }
                is PostViewModel.PostState.Success -> {
                    showLoading(false)
                    Log.d("CreatePost", "Post created/updated successfully")
                    Toast.makeText(
                        requireContext(),
                        if (args.postId.isNullOrEmpty()) R.string.post_created else R.string.post_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack(R.id.homeFragment, false)
                }
                is PostViewModel.PostState.Error -> {
                    showLoading(false)
                    Log.e("CreatePost", "Error creating post: ${state.message}")
                    showError(state.message)
                }
            }
        }
    }

    private fun loadPostForEditing(postId: String) {
        viewModel.getPost(postId).observe(viewLifecycleOwner) { post ->
            post?.let {
                binding.etPostContent.setText(it.content)

                if (!it.imageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(it.imageUrl)
                        .into(binding.ivPostImagePreview)

                    binding.ivPostImagePreview.visibility = View.VISIBLE
                    binding.btnRemoveImage.visibility = View.VISIBLE
                }

                selectedLocation = it.location
                if (!it.location.isNullOrEmpty()) {
                    binding.tvLocationPreview.text = it.location
                    binding.tvLocationPreview.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun openImageSelector() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun showImagePreview(uri: Uri) {
        binding.ivPostImagePreview.visibility = View.VISIBLE
        binding.btnRemoveImage.visibility = View.VISIBLE

        Glide.with(requireContext())
            .load(uri)
            .into(binding.ivPostImagePreview)
    }

    private fun showLocationPicker() {
        val locations = arrayOf(
            "Jerusalem, IL",
            "Tel Aviv, IL",
            "Bat Yam, IL",
            "Rishon Lezion, IL"
        )

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_location)
            .setItems(locations) { _, which ->
                selectedLocation = locations[which]
                binding.tvLocationPreview.text = selectedLocation
                binding.tvLocationPreview.visibility = View.VISIBLE
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDiscardConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.discard_post)
            .setMessage(R.string.discard_post_confirmation)
            .setPositiveButton(R.string.discard) { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnPost.isEnabled = !isLoading
        binding.etPostContent.isEnabled = !isLoading
        binding.cardAddImage.isEnabled = !isLoading
        binding.cardAddLocation.isEnabled = !isLoading
        binding.btnRemoveImage.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}