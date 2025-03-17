package com.app.unfit20.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.unfit2.R
import com.app.unfit20.databinding.FragmentHomeBinding
import com.app.unfit20.model.Post
import com.app.unfit20.ui.auth.AuthViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var postsAdapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Load posts when fragment is created
        homeViewModel.loadPosts()
    }

    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter(
            onPostClick = { post ->
                navigateToPostDetail(post)
            },
            onUserClick = { userId ->
                navigateToUserProfile(userId)
            }
        )

        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
        }
    }

    private fun setupListeners() {
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            homeViewModel.loadPosts()
        }

        // FAB to create new post
        binding.fabAddPost.setOnClickListener {
            navigateToCreatePost()
        }

        // Empty state button
        binding.layoutEmptyState.btnCreateFirstPost.setOnClickListener {
            navigateToCreatePost()
        }

        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // We're already on home, do nothing
                    true
                }
                R.id.nav_marketplace -> {
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToMarketplaceFragment()
                    )
                    true
                }
                R.id.nav_profile -> {
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToProfileFragment()
                    )
                    true
                }
                else -> false
            }
        }

        // Set home as selected
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun observeViewModel() {
        homeViewModel.postsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeViewModel.PostsState.Loading -> {
                    showLoading(true)
                    binding.swipeRefresh.isRefreshing = true
                }
                is HomeViewModel.PostsState.Success -> {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    updatePosts(state.posts)
                }
                is HomeViewModel.PostsState.Error -> {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    showError(state.message)
                }
            }
        }
    }

    private fun updatePosts(posts: List<Post>) {
        postsAdapter.submitList(posts)

        // Show empty state if there are no posts
        if (posts.isEmpty()) {
            binding.layoutEmptyState.root.visibility = View.VISIBLE
            binding.rvPosts.visibility = View.GONE
        } else {
            binding.layoutEmptyState.root.visibility = View.GONE
            binding.rvPosts.visibility = View.VISIBLE
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading && !binding.swipeRefresh.isRefreshing) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToCreatePost() {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToCreatePostFragment()
        )
    }

    private fun navigateToPostDetail(post: Post) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToPostDetailFragment(post.id)
        )
    }

    private fun navigateToUserProfile(userId: String) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToProfileFragment(userId)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}