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
import com.app.unfit20.model.Comment
import com.app.unfit20.model.Post
import com.app.unfit20.repository.UserRepository
import com.app.unfit20.ui.auth.AuthViewModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val args: PostDetailFragmentArgs by navArgs()

    private lateinit var commentsAdapter: CommentsAdapter
    private val userRepository = UserRepository()

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

        postId = args.postId
        if (postId == null) {
            Toast.makeText(requireContext(), "Invalid post ID", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupToolbar()
        setupComments()
        setupListeners()
        setupMenu()
        observeViewModel()

        viewModel.loadPost(postId!!)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                val post = viewModel.post.value
                val isCurrentUserAuthor = post?.userId == userRepository.getCurrentUserId()
                if (isCurrentUserAuthor) {
                    menuInflater.inflate(R.menu.menu_post_detail, menu)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
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
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupComments() {
        commentsAdapter = CommentsAdapter { userId ->
            navigateToUserProfile(userId)
        }
        binding.rvComments.adapter = commentsAdapter
    }

    private fun setupListeners() {
        binding.btnLike.setOnClickListener {
            val post = viewModel.post.value ?: return@setOnClickListener
            if (post.isLikedByCurrentUser) {
                viewModel.unlikePost(post.id)
            } else {
                viewModel.likePost(post.id)
            }
        }

        binding.btnComment.setOnClickListener {
            binding.etComment.requestFocus()
        }

        binding.btnShare.setOnClickListener {
            sharePost()
        }

        binding.btnSendComment.setOnClickListener {
            val commentText = binding.etComment.text.toString().trim()
            if (commentText.isNotEmpty() && postId != null) {
                val post = viewModel.post.value ?: return@setOnClickListener
                val currentUser = authViewModel.currentUser.value ?: return@setOnClickListener
                val newComment = Comment(
                    id = "temp_${System.currentTimeMillis()}",
                    postId = postId!!,
                    userId = currentUser.id,
                    userName = currentUser.name,
                    userAvatar = currentUser.profileImageUrl,
                    content = commentText,
                    createdAt = Date()
                )
                val updatedPost = post.copy(
                    comments = post.comments + newComment,
                    commentsCount = post.commentsCount + 1
                )
                viewModel.setLocalPost(updatedPost)
                commentsAdapter.submitList(updatedPost.comments.toList())
                viewModel.addComment(postId!!, commentText)
                binding.etComment.text.clear()
            }
        }

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
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let { updateUI(it) }
        }

        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                Glide.with(requireContext())
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.ivCurrentUserAvatar)
            }
        }

        viewModel.commentResult.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Toast.makeText(requireContext(), R.string.comment_failed, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.likePostResult.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Toast.makeText(requireContext(), R.string.like_failed, Toast.LENGTH_SHORT).show()
            }
        }

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
            tvPostContent.text = post.content
            tvUsername.text = post.userName
            tvDate.text = formatDate(post.createdAt)

            val likeIcon = if (post.isLikedByCurrentUser) R.drawable.ic_like_filled else R.drawable.ic_like
            btnLike.setCompoundDrawablesWithIntrinsicBounds(likeIcon, 0, 0, 0)

            tvLikesCount.text = resources.getQuantityString(R.plurals.likes_count, post.likesCount, post.likesCount)
            tvCommentsCount.text = resources.getQuantityString(R.plurals.comments_count, post.commentsCount, post.commentsCount)

            Glide.with(requireContext())
                .load(post.userAvatar)
                .placeholder(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(ivUserAvatar)

            if (!post.imageUrl.isNullOrEmpty()) {
                ivPostImage.visibility = View.VISIBLE
                Glide.with(requireContext()).load(post.imageUrl).into(ivPostImage)
            } else {
                ivPostImage.visibility = View.GONE
            }

            if (!post.location.isNullOrEmpty()) {
                tvLocation.visibility = View.VISIBLE
                tvLocation.text = post.location
            } else {
                tvLocation.visibility = View.GONE
            }

            if (post.comments.isEmpty()) {
                tvNoComments.visibility = View.VISIBLE
                rvComments.visibility = View.GONE
            } else {
                tvNoComments.visibility = View.GONE
                rvComments.visibility = View.VISIBLE
                commentsAdapter.submitList(post.comments.toList())
            }

            val isCurrentUserAuthor = post.userId == userRepository.getCurrentUserId()
            if (isCurrentUserAuthor) {
                btnPostOptions.visibility = View.VISIBLE
                btnPostOptions.setOnClickListener { showPostOptionsPopup() }
            } else {
                btnPostOptions.visibility = View.GONE
            }
        }
    }

    private fun showPostOptionsPopup() {
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

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }

    private fun sharePost() {
        val post = viewModel.post.value ?: return
        val shareText = "Check out this post from ${post.userName}\n\n${post.content}\n\nShared from Unfit20 App"

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