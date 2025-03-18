package com.app.unfit20.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentProfileBinding
import com.app.unfit20.ui.ViewModelFactory
import com.app.unfit20.ui.auth.AuthViewModel
import com.app.unfit20.ui.post.PostAdapter
import com.app.unfit20.ui.post.PostViewModel
import com.bumptech.glide.Glide
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val args: ProfileFragmentArgs by navArgs()
    private val postViewModel: PostViewModel by viewModels {
        ViewModelFactory(requireActivity().application)
    }
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var postAdapter: PostAdapter
    private val auth = FirebaseAuth.getInstance()

    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                updateProfileImage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Load user profile
        loadUserProfile()

        // Load user posts
        loadUserPosts()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val isCurrentUser = args.userId == auth.currentUser?.uid
        setHasOptionsMenu(isCurrentUser)
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                navigateToPostDetail(post.id)
            },
            onUserClick = { userId ->
                // We're already on the user profile
            },
            onLikeClick = { post ->
                handleLikeClick(post)
            },
            onCommentClick = { post ->
                navigateToPostDetail(post.id)
            },
            onShareClick = { post ->
                sharePost(post)
            }
        )

        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                MaterialDividerItemDecoration(
                    requireContext(),
                    LinearLayoutManager.VERTICAL
                ).apply {
                    isLastItemDecorated = false
                }
            )
        }
    }

    private fun setupListeners() {
        // Edit profile click listener (only shown for current user)
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun observeViewModel() {
        // Observe user profile
        authViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvUsername.text = it.name
                binding.tvBio.text = it.bio ?: getString(R.string.no_bio)

                Glide.with(requireContext())
                    .load(it.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.ivProfileImage)

                // Only show edit button for current user
                val isCurrentUser = it.id == auth.currentUser?.uid
                binding.btnEditProfile.visibility = if (isCurrentUser) View.VISIBLE else View.GONE
            }
        }

        // Observe user posts
        postViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)

            // Show/hide empty state
            if (posts.isEmpty()) {
                binding.tvNoPosts.visibility = View.VISIBLE
                binding.rvPosts.visibility = View.GONE
            } else {
                binding.tvNoPosts.visibility = View.GONE
                binding.rvPosts.visibility = View.VISIBLE
            }

            // Update post count
            binding.tvPostCount.text = posts.size.toString()
        }

        // Observe update profile result
        authViewModel.updateProfileResult.observe(viewLifecycleOwner) { result ->
            if (result) {
                Toast.makeText(
                    requireContext(),
                    R.string.profile_updated,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.profile_update_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Observe like result
        postViewModel.likePostResult.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Toast.makeText(
                    requireContext(),
                    R.string.like_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadUserProfile() {
        val userId = args.userId.ifEmpty {
            auth.currentUser?.uid ?: return
        }

        authViewModel.getUserData(userId)
    }

    private fun loadUserPosts() {
        val userId = args.userId.ifEmpty {
            auth.currentUser?.uid ?: return
        }

        postViewModel.loadUserPosts(userId)
    }

    private fun handleLikeClick(post: Post) {
        // Check if user is logged in
        if (auth.currentUser == null) {
            Toast.makeText(
                requireContext(),
                R.string.login_required_like,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (post.isLikedByCurrentUser) {
            postViewModel.unlikePost(post.id)
        } else {
            postViewModel.likePost(post.id)
        }
    }

    private fun navigateToPostDetail(postId: String) {
        findNavController().navigate(
            ProfileFragmentDirections.actionProfileFragmentToPostDetailFragment(postId)
        )
    }

    private fun sharePost(post: Post) {
        val shareText = buildString {
            append("Check out this post from ${post.userName}\n\n")
            append(post.content)
            append("\n\nShared from Unfit20 App")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_post)))
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profile, null)

        val etUsername = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUsername)
        val etBio = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBio)
        val btnChangePhoto = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnChangePhoto)

        // Pre-fill current values
        val currentUser = authViewModel.userData.value
        etUsername.setText(currentUser?.name)
        etBio.setText(currentUser?.bio)

        // Setup change photo button
        btnChangePhoto.setOnClickListener {
            openImageSelector()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_profile)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newUsername = etUsername.text.toString().trim()
                val newBio = etBio.text.toString().trim()

                if (newUsername.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.username_required,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                authViewModel.updateProfile(newUsername, newBio, selectedImageUri)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openImageSelector() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun updateProfileImage(uri: Uri) {
        // Show preview in dialog
        Toast.makeText(
            requireContext(),
            R.string.photo_selected,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setPositiveButton(R.string.logout) { _, _ ->
                authViewModel.logout()
                findNavController().navigate(
                    ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}