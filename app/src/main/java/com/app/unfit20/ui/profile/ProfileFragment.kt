package com.app.unfit20.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentProfileBinding
import com.app.unfit20.model.Post
import com.app.unfit20.model.User
import com.app.unfit20.repository.PostRepository
import com.app.unfit20.ui.auth.AuthViewModel
import com.app.unfit20.ui.post.PostViewModel
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel
    private val profileViewModel: ProfileViewModel by viewModels()
    private val postViewModel: PostViewModel by viewModels {
        PostViewModel.Factory(PostRepository())
    }
    private val args: ProfileFragmentArgs by navArgs()

    private lateinit var profilePagerAdapter: ProfilePagerAdapter

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

        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        setupToolbar()
        setupViewPager()
        setupListeners()
        observeViewModel()

        val currentUserId = args.userId.takeIf { !it.isNullOrEmpty() } ?: authViewModel.currentUser.value?.id
        profileViewModel.loadFullProfile(currentUserId)
        currentUserId?.let { postViewModel.loadUserPosts(it) }
    }

    override fun onResume() {
        super.onResume()
        val currentUserId = args.userId.takeIf { !it.isNullOrEmpty() } ?: authViewModel.currentUser.value?.id
        profileViewModel.loadFullProfile(currentUserId)
        currentUserId?.let { postViewModel.loadUserPosts(it) }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupViewPager() {
        profilePagerAdapter = ProfilePagerAdapter(
            this,
            onPostClick = { post -> navigateToPostDetail(post) },
            onUserClick = { userId -> navigateToUserProfile(userId) }
        )
        binding.viewPager.adapter = profilePagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.posts)
                1 -> getString(R.string.liked_posts)
                else -> null
            }
        }.attach()
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment()
            )
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            navigateToLogin()
        }

        binding.fabAddPost.setOnClickListener {
            navigateToCreatePost()
        }
    }

    private fun observeViewModel() {
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let { updateUI(it) }
        }

        profileViewModel.userLikedPosts.observe(viewLifecycleOwner) { posts ->
            Log.d("ProfileFragment", "Loaded liked posts: ${posts.size}")
            profilePagerAdapter.updateLikedPosts(posts)
        }

        postViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            Log.d("ProfileFragment", "Loaded user posts: ${posts.size}")
            profilePagerAdapter.updatePosts(posts)
        }

        profileViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        profileViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                profileViewModel.clearErrorMessage()
            }
        }
    }

    private fun updateUI(user: User) {
        binding.tvUsername.text = user.name
        binding.tvEmail.text = user.email
        binding.collapsingToolbar.title = user.name

        Glide.with(requireContext())
            .load(user.profileImageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(binding.ivProfile)

        val isOwnProfile = profileViewModel.isOwnProfile(args.userId)
        binding.btnEditProfile.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        binding.btnLogout.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        binding.fabAddPost.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
    }

    private fun navigateToPostDetail(post: Post) {
        findNavController().navigate(
            ProfileFragmentDirections.actionProfileFragmentToPostDetailFragment(post.id)
        )
    }

    private fun navigateToUserProfile(userId: String) {
        if (userId != args.userId) {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentSelf(userId)
            )
        }
    }

    private fun navigateToCreatePost() {
        findNavController().navigate(
            ProfileFragmentDirections.actionProfileFragmentToCreatePostFragment(null)
        )
    }

    private fun navigateToLogin() {
        findNavController().navigate(
            ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}