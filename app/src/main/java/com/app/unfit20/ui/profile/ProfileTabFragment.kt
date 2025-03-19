package com.app.unfit20.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentProfileTabBinding
import com.app.unfit20.model.Post
import com.app.unfit20.ui.post.PostsAdapter

class ProfileTabFragment : Fragment() {

    private var _binding: FragmentProfileTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var postsAdapter: PostsAdapter
    private var tabType: Int = TAB_POSTS
    private var posts: List<Post> = emptyList()
    private var onPostClick: ((Post) -> Unit)? = null
    private var onUserClick: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabType = it.getInt(ARG_TAB_TYPE, TAB_POSTS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        updateUI()
    }

    private fun setupRecyclerView() {
        // Only pass the callbacks you actually need
        postsAdapter = PostsAdapter(
            onPostClick = { post -> onPostClick?.invoke(post) },
            onUserClick = { userId -> onUserClick?.invoke(userId) }
            // We skip onLikeClick, onCommentClick, onShareClick if not needed here
        )
        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
        }
    }

    fun setPostsData(newPosts: List<Post>) {
        posts = newPosts
        // If the adapter is ready, show data
        if (::postsAdapter.isInitialized) {
            updateUI()
        }
    }

    fun setCallbacks(
        postClickCallback: (Post) -> Unit,
        userClickCallback: (String) -> Unit
    ) {
        onPostClick = postClickCallback
        onUserClick = userClickCallback
    }

    private fun updateUI() {
        postsAdapter.submitList(posts)

        // Choose the empty message
        val emptyMessageRes = when (tabType) {
            TAB_POSTS -> R.string.no_posts_yet
            TAB_LIKED -> R.string.no_liked_posts
            else -> R.string.no_data
        }
        binding.tvEmptyMessage.setText(emptyMessageRes)

        // Show empty layout if no posts
        if (posts.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvPosts.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvPosts.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAB_POSTS = 0
        const val TAB_LIKED = 1
        private const val ARG_TAB_TYPE = "tab_type"

        fun newInstance(tabType: Int): ProfileTabFragment {
            return ProfileTabFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB_TYPE, tabType)
                }
            }
        }
    }
}