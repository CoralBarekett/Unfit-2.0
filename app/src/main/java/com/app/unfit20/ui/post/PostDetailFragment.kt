package com.app.unfit20.ui.post

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentPostDetailBinding
import com.app.unfit20.model.Post
import com.app.unfit20.repository.UserRepository
import com.app.unfit20.ui.auth.AuthViewModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val args: PostDetailFragmentArgs by navArgs()

    private lateinit var commentsAdapter: CommentsAdapter
    private val userRepository = UserRepository()

    // We'll store postId here once we confirm it's not null
    private var postId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Safely unwrap args.postId
        postId = args.postId
        if (postId == null) {
            // If you truly never expect postId to be null, this is a fallback
            Toast.makeText(requireContext(), "Invalid post ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupToolbar()
        setupComments()
        setupListeners()
        setupMenu() // optional top-bar menu
        observeViewModel()

        // Load post data using the guaranteed non-null postId
        viewModel.loadPost(postId!!)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * If you want to keep the top bar menu (three-dot overflow in the ActionBar).
     * Otherwise, you can remove this entire method if you're using only the popup menu button.
     */
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Only add menu items if current user is the author
                val post = viewModel.post.value
                val isCurrentUserAuthor = post?.userId == userRepository.getCurrentUserId()
                if (isCurrentUserAuthor) {
                    menuInflater.inflate(R.menu.menu_post_detail, menu)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        // Because we already checked postId is not null, safe to use !!
                        findNavController().navigate(
                            PostDetailFragmentDirections.actionPostDetailFragmentToCreatePostFragment(postId!!)
                        )
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupComments() {
        commentsAdapter = CommentsAdapter { userId ->
            // Navigate to user profile when username is clicked
            navigateToUserProfile(userId)
        }
        binding.rvComments.adapter = commentsAdapter
    }

    private fun setupListeners() {
        // Like button
        binding.btnLike.setOnClickListener {
            val post = viewModel.post.value ?: return@setOnClickListener
            if (post.isLikedByCurrentUser) {
                viewModel.unlikePost(post.id)
            } else {
                viewModel.likePost(post.id)
            }
        }

        // Comment button focuses on comment input
        binding.btnComment.setOnClickListener {
            binding.etComment.requestFocus()
        }

        // Share button
        binding.btnShare.setOnClickListener {
            sharePost()
        }

        // Send comment button
        binding.btnSendComment.setOnClickListener {
            val commentText = binding.etComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                viewModel.addComment(postId!!, commentText)
                binding.etComment.text.clear()
            }
        }

        // User avatar and username click
        binding.ivUserAvatar.setOnClickListener {
            val post = viewModel.post.value ?: return@setOnClickListener
            navigateToUserProfile(post.userId)
        }

        binding.tvUsername.setOnClickListener {
            val post = viewModel.post.value ?: return@setOnClickListener
            navigateToUserProfile(post.userId)
        }
    }

    private fun observeViewModel() {
        // Observe post data
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                updateUI(it)
            }
        }

        // Observe current user for comment UI
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Load current user avatar for comment
                Glide.with(requireContext())
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.ivCurrentUserAvatar)
            }
        }

        // Observe comment result
        viewModel.commentResult.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Toast.makeText(requireContext(), R.string.comment_failed, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe like result
        viewModel.likePostResult.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Toast.makeText(requireContext(), R.string.like_failed, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe delete result
        viewModel.deletePostResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), R.string.post_deleted, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(post: Post) {
        binding.apply {
            // Post content
            tvPostContent.text = post.content

            // Username and date
            tvUsername.text = post.userName
            tvDate.text = formatDate(post.createdAt)

            // Like status: pick which icon to show
            val likeIcon = if (post.isLikedByCurrentUser) {
                R.drawable.ic_like_filled
            } else {
                R.drawable.ic_like
            }
            // Because btnLike is a TextView, we use compound drawables:
            btnLike.setCompoundDrawablesWithIntrinsicBounds(likeIcon, 0, 0, 0)

            // (Optional) Change the text from "Like" to "Liked"
            // btnLike.text = if (post.isLikedByCurrentUser) getString(R.string.liked) else getString(R.string.like)

            // Like and comment counts
            tvLikesCount.text = resources.getQuantityString(
                R.plurals.likes_count,
                post.likesCount,
                post.likesCount
            )
            tvCommentsCount.text = resources.getQuantityString(
                R.plurals.comments_count,
                post.commentsCount,
                post.commentsCount
            )

            // User avatar
            Glide.with(requireContext())
                .load(post.userAvatar)
                .placeholder(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(ivUserAvatar)

            // Post image
            if (!post.imageUrl.isNullOrEmpty()) {
                ivPostImage.visibility = View.VISIBLE
                Glide.with(requireContext())
                    .load(post.imageUrl)
                    .into(ivPostImage)
            } else {
                ivPostImage.visibility = View.GONE
            }

            // Location
            if (!post.location.isNullOrEmpty()) {
                tvLocation.visibility = View.VISIBLE
                tvLocation.text = post.location
            } else {
                tvLocation.visibility = View.GONE
            }

            // Comments
            if (post.comments.isEmpty()) {
                tvNoComments.visibility = View.VISIBLE
                rvComments.visibility = View.GONE
            } else {
                tvNoComments.visibility = View.GONE
                rvComments.visibility = View.VISIBLE
                commentsAdapter.submitList(post.comments)
            }

            // Show or hide the post options button (the anchor for popup menu)
            val isCurrentUserAuthor = post.userId == userRepository.getCurrentUserId()
            if (isCurrentUserAuthor) {
                btnPostOptions.visibility = View.VISIBLE
                btnPostOptions.setOnClickListener {
                    showPostOptionsPopup()
                }
            } else {
                btnPostOptions.visibility = View.GONE
            }
        }
    }

    private fun showPostOptionsPopup() {
        // Show the same "Edit" / "Delete" options that are in menu_post_detail
        val popupMenu = PopupMenu(requireContext(), binding.btnPostOptions)
        popupMenu.inflate(R.menu.menu_post_detail)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    findNavController().navigate(
                        PostDetailFragmentDirections.actionPostDetailFragmentToCreatePostFragment(postId!!)
                    )
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun formatDate(date: java.util.Date): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }

    private fun sharePost() {
        val post = viewModel.post.value ?: return
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

    private fun navigateToUserProfile(userId: String) {
        findNavController().navigate(
            PostDetailFragmentDirections.actionPostDetailFragmentToProfileFragment(userId)
        )
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_post)
            .setMessage(R.string.delete_post_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                // Because we already checked postId is not null, safe to use !!
                viewModel.deletePost(postId!!)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}