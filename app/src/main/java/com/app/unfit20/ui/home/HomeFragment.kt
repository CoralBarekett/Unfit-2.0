package com.app.unfit20.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentHomeBinding
import com.app.unfit20.model.Post
import com.app.unfit20.ui.ViewModelFactory
import com.app.unfit20.ui.post.PostViewModel
import com.app.unfit20.ui.post.PostsAdapter
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels { ViewModelFactory() }
    private lateinit var postsAdapter: PostsAdapter
    private val auth = FirebaseAuth.getInstance()

    private var isLoadingMore = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        setupBottomNavigation()
        observeViewModel()
        loadPosts()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFeedPosts() // refresh on return to Home
    }

    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter(
            onPostClick = { post -> navigateToPostDetail(post.id) },
            onUserClick = { userId -> navigateToUserProfile(userId) },
            onLikeClick = { post -> handleLikeClick(post) },
            onCommentClick = { post -> navigateToPostDetail(post.id) },
            onShareClick = { post -> sharePost(post) }
        )

        binding.rvPosts.apply {
            adapter = postsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                MaterialDividerItemDecoration(
                    requireContext(),
                    LinearLayoutManager.VERTICAL
                ).apply { isLastItemDecorated = false }
            )

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    val isLastItemVisible = (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    val isNotLoadingAndNotEmpty = !isLoadingMore && totalItemCount > 0

                    if (isLastItemVisible && dy > 0 && isNotLoadingAndNotEmpty) {
                        isLoadingMore = true
                        viewModel.loadFeedPosts()
                    }
                }
            })
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadFeedPosts()
        }
    }

    private fun setupFab() {
        binding.fabAddPost.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(requireContext(), R.string.login_required, Toast.LENGTH_SHORT).show()
                navigateToLogin()
            } else {
                navigateToCreatePost()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_marketplace -> {
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToMarketplaceFragment())
                    true
                }
                R.id.nav_profile -> {
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToProfileFragment())
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun observeViewModel() {
        viewModel.feedPosts.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
            binding.swipeRefresh.isRefreshing = false
            isLoadingMore = false
        }

        viewModel.likePostResult.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Toast.makeText(requireContext(), R.string.like_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPosts() {
        binding.swipeRefresh.isRefreshing = true
        viewModel.loadFeedPosts()
    }

    private fun handleLikeClick(post: Post) {
        if (auth.currentUser == null) {
            Toast.makeText(requireContext(), R.string.login_required_like, Toast.LENGTH_SHORT).show()
            return
        }
        if (post.isLikedByCurrentUser) {
            viewModel.unlikePost(post.id)
        } else {
            viewModel.likePost(post.id)
        }
    }

    private fun navigateToPostDetail(postId: String) {
        findNavController().navigate(
            R.id.action_homeFragment_to_postDetailFragment,
            Bundle().apply { putString("postId", postId) }
        )
    }

    private fun navigateToUserProfile(userId: String) {
        findNavController().navigate(
            R.id.action_homeFragment_to_profileFragment,
            Bundle().apply { putString("userId", userId) }
        )
    }

    private fun navigateToCreatePost() {
        findNavController().navigate(
            R.id.action_homeFragment_to_createPostFragment,
            Bundle().apply { putString("postId", null) }
        )
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.loginFragment)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}