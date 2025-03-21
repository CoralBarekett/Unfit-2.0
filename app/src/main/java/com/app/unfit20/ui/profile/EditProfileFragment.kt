package com.app.unfit20.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentEditProfileBinding
import com.app.unfit20.model.User
import com.app.unfit20.ui.auth.AuthViewModel
import com.bumptech.glide.Glide

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var currentUser: User? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                // Display the selected image
                Glide.with(requireContext())
                    .load(uri)
                    .into(binding.ivProfile)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupBioCounter()
        setupListeners()
        observeViewModel()

        // Load current user data
        authViewModel.loadCurrentUser()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupBioCounter() {
        // Update bio character count when text changes
        binding.etBio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateBioCount(s?.length ?: 0)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Initial count
        updateBioCount(0)
    }

    private fun updateBioCount(length: Int) {
        binding.tvBioCount.text = "$length/150"
    }

    private fun setupListeners() {
        // Save button
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        // Only allow changing the photo via the edit icon
        binding.ivChangePhoto.setOnClickListener {
            openImageSelector()
        }
    }

    private fun observeViewModel() {
        // Observe current user
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                currentUser = it
                populateUserData(it)
            }
        }

        // Observe update profile result
        authViewModel.updateProfileResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), R.string.profile_update_failed, Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun populateUserData(user: User) {
        // Populate form fields with user data
        binding.etName.setText(user.name)
        binding.etEmail.setText(user.email)

        // Set bio if available
        user.bio?.let {
            binding.etBio.setText(it)
            updateBioCount(it.length)
        }

        // Load profile image
        Glide.with(requireContext())
            .load(user.profileImageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(binding.ivProfile)
    }

    private fun openImageSelector() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        // Validate name
        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.name_required)
            return
        }

        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        // Update profile
        authViewModel.updateProfile(name, selectedImageUri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}