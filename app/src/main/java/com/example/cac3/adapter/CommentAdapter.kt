package com.example.cac3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cac3.R
import com.example.cac3.data.model.Comment
import com.example.cac3.data.model.InsightType
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying comments in a RecyclerView
 */
class CommentAdapter : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        holder.bind(comment)
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val insightTypeBadge: TextView = itemView.findViewById(R.id.insightTypeBadge)
        private val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val upvoteCountTextView: TextView = itemView.findViewById(R.id.upvoteCountTextView)

        fun bind(comment: Comment) {
            userNameTextView.text = comment.userName
            commentTextView.text = comment.comment
            timestampTextView.text = getTimeAgo(comment.createdAt)
            upvoteCountTextView.text = "${comment.upvotes} helpful"

            // Set insight type badge
            insightTypeBadge.text = formatInsightType(comment.insightType)
            insightTypeBadge.setBackgroundColor(getInsightTypeColor(comment.insightType))
        }

        private fun formatInsightType(type: InsightType): String {
            return when (type) {
                InsightType.TIME_REALITY_CHECK -> "TIME"
                InsightType.HIDDEN_COSTS -> "COST"
                InsightType.APPLICATION_TIP -> "TIP"
                InsightType.IMPACT_STORY -> "STORY"
                InsightType.WARNING -> "WARNING"
                InsightType.SOCIAL_INFO -> "SOCIAL"
                InsightType.GENERAL_REVIEW -> "REVIEW"
            }
        }

        private fun getInsightTypeColor(type: InsightType): Int {
            return when (type) {
                InsightType.TIME_REALITY_CHECK -> itemView.context.getColor(R.color.subject_stem)
                InsightType.HIDDEN_COSTS -> itemView.context.getColor(R.color.error)
                InsightType.APPLICATION_TIP -> itemView.context.getColor(R.color.accent)
                InsightType.IMPACT_STORY -> itemView.context.getColor(R.color.subject_arts)
                InsightType.WARNING -> itemView.context.getColor(R.color.error)
                InsightType.SOCIAL_INFO -> itemView.context.getColor(R.color.subject_leadership)
                InsightType.GENERAL_REVIEW -> itemView.context.getColor(R.color.primary)
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hours ago"
                days < 30 -> "$days days ago"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    dateFormat.format(Date(timestamp))
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
