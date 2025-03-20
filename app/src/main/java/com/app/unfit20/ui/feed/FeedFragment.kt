package com.app.unfit20.ui.feed

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
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentFeedBinding
import com.app.unfit20.model.Post
import com.app.unfit20.ui.ViewModelFactory
import com.app.unfit20.ui.post.PostViewModel
import com.app.unfit20.ui.post.PostsAdapter
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory()
    }
    private lateinit var postsAdapter: PostsAdapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()

        // Load posts
        loadPosts()
    }

    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter(
            onPostClick = { post ->
                navigateToPostDetail(post.id)
            },
            onUserClick = { userId ->
                navigateToUserProfile(userId)
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
            adapter = postsAdapter
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

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPosts()
        }
    }

    private fun setupFab() {
        binding.fabCreatePost.setOnClickListener {
            // Check if user is logged in
            if (auth.currentUser == null) {
                Toast.makeText(
                    requireContext(),
                    R.string.login_required,
                    Toast.LENGTH_SHORT
                ).show()
                navigateToLogin()
            } else {
                navigateToCreatePost()
            }
        }
    }

    private fun observeViewModel() {
        // Observe feed posts
        viewModel.feedPosts.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
            binding.swipeRefresh.isRefreshing = false

            // Show/hide empty state
            if (posts.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.rvPosts.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvPosts.visibility = View.VISIBLE
            }
        }

        // Observe like results
        viewModel.likePostResult.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Toast.makeText(
                    requireContext(),
                    R.string.like_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadPosts() {
        binding.swipeRefresh.isRefreshing = true
        viewModel.loadFeedPosts()
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
            viewModel.unlikePost(post.id)
        } else {
            viewModel.likePost(post.id)
        }
    }

    private fun navigateToPostDetail(postId: String) {
        // Navigate to post detail
        findNavController().navigate(R.id.action_homeFragment_to_postDetailFragment,
            Bundle().apply {
                putString("postId", postId)
            }
        )
    }

    private fun navigateToUserProfile(userId: String) {
        // Navigate to profile
        findNavController().navigate(R.id.action_homeFragment_to_profileFragment,
            Bundle().apply {
                putString("userId", userId)
            }
        )
    }

    private fun navigateToCreatePost() {
        // Navigate to create post
        findNavController().navigate(R.id.action_homeFragment_to_createPostFragment,
            Bundle().apply {
                putString("postId", null)
            }
        )
    }

    private fun navigateToLogin() {
        // Navigate to login
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