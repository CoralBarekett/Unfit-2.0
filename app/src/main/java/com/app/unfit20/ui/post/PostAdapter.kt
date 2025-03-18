package com.app.unfit20.ui.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.unfit20.R
import com.app.unfit20.databinding.ItemPostBinding
import com.app.unfit20.model.Post
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PostAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onUserClick: (String) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Post item click listener
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPostClick(getItem(position))
                }
            }

            // User avatar and username click listener
            binding.ivUserAvatar.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position).userId)
                }
            }

            binding.tvUsername.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position).userId)
                }
            }

            // Like button click listener
            binding.btnLike.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLikeClick(getItem(position))
                }
            }

            // Comment button click listener
            binding.btnComment.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCommentClick(getItem(position))
                }
            }

            // Share button click listener
            binding.btnShare.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onShareClick(getItem(position))
                }
            }
        }

        fun bind(post: Post) {
            binding.apply {
                // Username and date
                tvUsername.text = post.userName
                tvDate.text = getTimeAgo(post.createdAt)

                // Post content
                tvPostContent.text = post.content
                tvPostContent.maxLines = 3

                // Expand button
                val btnExpand = root.findViewById<View>(R.id.btnExpand)
                if (btnExpand != null) {
                    // Show/hide based on truncated text
                    btnExpand.visibility = if (isTextTruncated(tvPostContent)) View.VISIBLE else View.GONE

                    // Toggle expansion
                    btnExpand.setOnClickListener {
                        if (tvPostContent.maxLines == 3) {
                            tvPostContent.maxLines = Int.MAX_VALUE
                            if (btnExpand is android.widget.TextView) {
                                btnExpand.text = itemView.context.getString(R.string.show_less)
                            }
                        } else {
                            tvPostContent.maxLines = 3
                            if (btnExpand is android.widget.TextView) {
                                btnExpand.text = itemView.context.getString(R.string.show_more)
                            }
                        }
                    }
                }

                // Post image
                if (!post.imageUrl.isNullOrEmpty()) {
                    ivPostImage.visibility = View.VISIBLE
                    Glide.with(ivPostImage.context)
                        .load(post.imageUrl)
                        .transform(CenterCrop(), RoundedCorners(16))
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_broken_image)
                        .into(ivPostImage)
                } else {
                    ivPostImage.visibility = View.GONE
                }

                // Location
                val tvLocation = root.findViewById<android.widget.TextView>(R.id.tvLocation)
                if (tvLocation != null) {
                    if (!post.location.isNullOrEmpty()) {
                        tvLocation.visibility = View.VISIBLE
                        tvLocation.text = post.location
                    } else {
                        tvLocation.visibility = View.GONE
                    }
                }

                // Like icon (ivLike)
                val ivLike = root.findViewById<android.widget.ImageView>(R.id.ivLike)
                if (ivLike != null) {
                    val likeIcon = if (post.isLikedByCurrentUser)
                        R.drawable.ic_like_filled
                    else
                        R.drawable.ic_like
                    ivLike.setImageResource(likeIcon)
                }

                // Like & comment counts
                tvLikesCount.text = itemView.context.resources.getQuantityString(
                    R.plurals.likes_count,
                    post.likesCount,
                    post.likesCount
                )
                tvCommentsCount.text = itemView.context.resources.getQuantityString(
                    R.plurals.comments_count,
                    post.commentsCount,
                    post.commentsCount
                )

                // User avatar
                Glide.with(ivUserAvatar.context)
                    .load(post.userAvatar)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivUserAvatar)
            }
        }

        private fun getTimeAgo(date: Date): String {
            val now = Date()
            val seconds = TimeUnit.MILLISECONDS.toSeconds(now.time - date.time)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(now.time - date.time)
            val hours = TimeUnit.MILLISECONDS.toHours(now.time - date.time)
            val days = TimeUnit.MILLISECONDS.toDays(now.time - date.time)

            return when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hr ago"
                days < 7 -> "$days days ago"
                else -> {
                    val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
                    formatter.format(date)
                }
            }
        }

        private fun isTextTruncated(textView: android.widget.TextView): Boolean {
            // This is an approximation
            val layout = textView.layout ?: return false
            val lines = layout.lineCount
            return lines > 0 && layout.getEllipsisCount(lines - 1) > 0
        }
    }

    // Method to update a single post in the list
    fun updatePost(updatedPost: Post) {
        val currentList = currentList.toMutableList()
        val position = currentList.indexOfFirst { it.id == updatedPost.id }

        if (position != -1) {
            currentList[position] = updatedPost
            submitList(currentList)
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem || (
                    oldItem.id == newItem.id &&
                            oldItem.content == newItem.content &&
                            oldItem.imageUrl == newItem.imageUrl &&
                            oldItem.likesCount == newItem.likesCount &&
                            oldItem.commentsCount == newItem.commentsCount &&
                            oldItem.isLikedByCurrentUser == newItem.isLikedByCurrentUser
                    )
        }
    }
}