package com.app.unfit20.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentProfileBinding
import com.app.unfit20.model.Post
import com.app.unfit20.model.User
import com.app.unfit20.ui.auth.AuthViewModel
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
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

        setupToolbar()
        setupViewPager()
        setupListeners()
        observeViewModel()

        val userId = args.userId.takeIf { !it.isNullOrEmpty() }
        viewModel.loadUserProfile(userId)
        viewModel.loadUserPosts(userId)
        viewModel.loadUserLikedPosts(userId)
    }

    override fun onResume() {
        super.onResume()
        val userId = args.userId.takeIf { !it.isNullOrEmpty() }
        viewModel.loadUserPosts(userId)
        viewModel.loadUserLikedPosts(userId)
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
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let { updateUI(it) }
        }

        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            Log.d("ProfileFragment", "Loaded user posts: ${posts.size}")
            profilePagerAdapter.updatePosts(posts)
        }

        viewModel.userLikedPosts.observe(viewLifecycleOwner) { posts ->
            Log.d("ProfileFragment", "Loaded liked posts: ${posts.size}")
            profilePagerAdapter.updateLikedPosts(posts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
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

        val isOwnProfile = viewModel.isOwnProfile(args.userId)
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