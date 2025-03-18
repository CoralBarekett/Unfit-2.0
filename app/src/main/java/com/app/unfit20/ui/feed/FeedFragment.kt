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
import com.app.unfit20.ui.post.PostAdapter
import com.app.unfit20.ui.post.PostViewModel
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory(requireActivity().application)
    }
    private lateinit var postAdapter: PostAdapter
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
        postAdapter = PostAdapter(
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
            postAdapter.submitList(posts)
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
        findNavController().navigate(
            FeedFragmentDirections.actionFeedFragmentToPostDetailFragment(postId)
        )
    }

    private fun navigateToUserProfile(userId: String) {
        findNavController().navigate(
            FeedFragmentDirections.actionFeedFragmentToProfileFragment(userId)
        )
    }

    private fun navigateToCreatePost() {
        findNavController().navigate(
            FeedFragmentDirections.actionFeedFragmentToCreatePostFragment(null)
        )
    }

    private fun navigateToLogin() {
        findNavController().navigate(
            FeedFragmentDirections.actionFeedFragmentToLoginFragment()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}