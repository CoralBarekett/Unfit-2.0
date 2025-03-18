package com.app.unfit20.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.unfit20.R
import com.app.unfit20.databinding.ItemCommentBinding
import com.app.unfit20.model.Comment
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CommentsAdapter(
    private val onUserClick: (String) -> Unit
) : ListAdapter<Comment, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // User avatar click listener
            binding.ivUserAvatar.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position).userId)
                }
            }

            // Username click listener
            binding.tvUsername.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position).userId)
                }
            }
        }

        fun bind(comment: Comment) {
            binding.apply {
                // Set comment content
                tvComment.text = comment.content

                // Set comment date
                tvDate.text = getTimeAgo(comment.createdAt)

                // Set user info
                tvUsername.text = comment.userName

                // Load user avatar with Glide
                Glide.with(ivUserAvatar)
                    .load(comment.userAvatar)
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
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
}