package com.app.unfit20.ui.post

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentPostDetailBinding
import com.app.unfit20.model.Post
import com.app.unfit20.repository.UserRepository
import com.app.unfit20.ui.ViewModelFactory
import com.app.unfit20.ui.auth.AuthViewModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory() // Adjust if your factory requires Application: ViewModelFactory(requireActivity().application)
    }
    private val authViewModel: AuthViewModel by viewModels()
    private val args: PostDetailFragmentArgs by navArgs()

    private lateinit var commentsAdapter: CommentsAdapter
    private val userRepository = UserRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        // Set options menu so that we can show edit/delete if user is the author
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupComments()
        setupListeners()
        observeViewModel()

        // Load post details using Safe Args
        viewModel.loadPost(args.postId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
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
                viewModel.addComment(args.postId, commentText)
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
        // Observe post details
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let { updateUI(it) }
        }
        // Observe current user to load comment avatar
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
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
            // Username and formatted date
            tvUsername.text = post.userName
            tvDate.text = formatDate(post.createdAt)
            // Like icon and counts
            ivLike.setImageResource(
                if (post.isLikedByCurrentUser) R.drawable.ic_like_filled else R.drawable.ic_like
            )
            tvLikesCount.text = resources.getQuantityString(
                R.plurals.likes_count, post.likesCount, post.likesCount
            )
            tvCommentsCount.text = resources.getQuantityString(
                R.plurals.comments_count, post.commentsCount, post.commentsCount
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
            // Comments section
            if (post.comments.isEmpty()) {
                tvNoComments.visibility = View.VISIBLE
                rvComments.visibility = View.GONE
            } else {
                tvNoComments.visibility = View.GONE
                rvComments.visibility = View.VISIBLE
                commentsAdapter.submitList(post.comments)
            }
            // If current user is the post author, enable options menu for edit/delete
            val isCurrentUserAuthor = post.userId == userRepository.getCurrentUserId()
            setHasOptionsMenu(isCurrentUserAuthor)
        }
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
                viewModel.deletePost(args.postId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_post_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                findNavController().navigate(
                    PostDetailFragmentDirections.actionPostDetailFragmentToCreatePostFragment(args.postId)
                )
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}