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
class CommentAdapter(
    private val currentUserId: Long,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit,
    private val onReplyClick: (Comment) -> Unit,
    private val onUpvoteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view, currentUserId, onEditClick, onDeleteClick, onReplyClick, onUpvoteClick)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        holder.bind(comment)
    }

    class CommentViewHolder(
        itemView: View,
        private val currentUserId: Long,
        private val onEditClick: (Comment) -> Unit,
        private val onDeleteClick: (Comment) -> Unit,
        private val onReplyClick: (Comment) -> Unit,
        private val onUpvoteClick: (Comment) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val userGradeTextView: TextView = itemView.findViewById(R.id.userGradeTextView)
        private val verifiedBadge: TextView = itemView.findViewById(R.id.verifiedBadge)
        private val insightTypeBadge: TextView = itemView.findViewById(R.id.insightTypeBadge)
        private val ratingLayout: View = itemView.findViewById(R.id.ratingLayout)
        private val commentRatingBar: android.widget.RatingBar = itemView.findViewById(R.id.commentRatingBar)
        private val ratingTextView: TextView = itemView.findViewById(R.id.ratingTextView)
        private val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
        private val resourceLinksLayout: View = itemView.findViewById(R.id.resourceLinksLayout)
        private val resourceLinksTextView: TextView = itemView.findViewById(R.id.resourceLinksTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val upvoteCountTextView: TextView = itemView.findViewById(R.id.upvoteCountTextView)
        private val replyButton: TextView = itemView.findViewById(R.id.replyButton)
        private val editButton: android.widget.ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: android.widget.ImageButton = itemView.findViewById(R.id.deleteButton)

        private val prefs = itemView.context.getSharedPreferences("comment_upvotes", android.content.Context.MODE_PRIVATE)

        fun bind(comment: Comment) {
            // User name and grade
            userNameTextView.text = comment.userName

            if (comment.userGrade != null) {
                userGradeTextView.text = "Grade ${comment.userGrade}"
                userGradeTextView.visibility = View.VISIBLE
            } else {
                userGradeTextView.visibility = View.GONE
            }

            // Verified participant badge
            if (comment.isVerifiedParticipant) {
                verifiedBadge.visibility = View.VISIBLE
            } else {
                verifiedBadge.visibility = View.GONE
            }

            // Comment text
            commentTextView.text = comment.comment

            // Rating
            if (comment.rating != null && comment.rating > 0) {
                ratingLayout.visibility = View.VISIBLE
                commentRatingBar.rating = comment.rating.toFloat()
                ratingTextView.text = "${comment.rating}/5"
            } else {
                ratingLayout.visibility = View.GONE
            }

            // Resource links
            if (comment.hasResources && !comment.resourceLinks.isNullOrEmpty()) {
                resourceLinksLayout.visibility = View.VISIBLE
                resourceLinksTextView.text = comment.resourceLinks
            } else {
                resourceLinksLayout.visibility = View.GONE
            }

            // Timestamp and upvotes
            timestampTextView.text = getTimeAgo(comment.createdAt)

            // Check if user has already marked this as helpful
            val hasUpvoted = prefs.getBoolean("upvoted_${comment.id}", false)
            updateUpvoteUI(comment, hasUpvoted)

            // Set insight type badge
            insightTypeBadge.text = formatInsightType(comment.insightType)
            insightTypeBadge.setBackgroundColor(getInsightTypeColor(comment.insightType))

            // Show edit/delete buttons only if current user owns this comment
            val isOwnComment = comment.userId != null && comment.userId == currentUserId
            editButton.visibility = if (isOwnComment) View.VISIBLE else View.GONE
            deleteButton.visibility = if (isOwnComment) View.VISIBLE else View.GONE

            // Set click listeners
            upvoteCountTextView.setOnClickListener {
                if (!hasUpvoted) {
                    // Mark as upvoted
                    prefs.edit().putBoolean("upvoted_${comment.id}", true).apply()
                    updateUpvoteUI(comment, true)
                    onUpvoteClick(comment)
                } else {
                    // Already voted
                    android.widget.Toast.makeText(
                        itemView.context,
                        "You already marked this as helpful",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            replyButton.setOnClickListener { onReplyClick(comment) }
            editButton.setOnClickListener { onEditClick(comment) }
            deleteButton.setOnClickListener { onDeleteClick(comment) }
        }

        private fun updateUpvoteUI(comment: Comment, hasUpvoted: Boolean) {
            if (hasUpvoted) {
                // User has voted - show filled star and count
                upvoteCountTextView.text = if (comment.upvotes > 0) {
                    "${comment.upvotes} found helpful"
                } else {
                    "1 found helpful"
                }
                upvoteCountTextView.setTextColor(itemView.context.getColor(R.color.accent))
                upvoteCountTextView.setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.star_big_on, 0, 0, 0
                )
            } else {
                // User hasn't voted - show outline star and prompt
                upvoteCountTextView.text = if (comment.upvotes > 0) {
                    "${comment.upvotes} helpful · Mark as Helpful"
                } else {
                    "Mark as Helpful"
                }
                upvoteCountTextView.setTextColor(itemView.context.getColor(R.color.text_secondary))
                upvoteCountTextView.setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.star_big_off, 0, 0, 0
                )
            }
        }

        private fun formatInsightType(type: InsightType): String {
            return when (type) {
                InsightType.HELP -> "HELP"
                InsightType.OPINION -> "OPINION"
                InsightType.STUDY_RESOURCES -> "RESOURCES"
                InsightType.TIPS -> "TIPS"
                InsightType.EXPERIENCE -> "EXPERIENCE"
                InsightType.QUESTION -> "QUESTION"
                InsightType.TIME_REALITY_CHECK -> "TIME"
                InsightType.HIDDEN_COSTS -> "COST"
                InsightType.APPLICATION_TIP -> "TIP"
                InsightType.IMPACT_STORY -> "STORY"
                InsightType.WARNING -> "WARNING"
                InsightType.SOCIAL_INFO -> "SOCIAL"
            }
        }

        private fun getInsightTypeColor(type: InsightType): Int {
            return when (type) {
                InsightType.HELP -> itemView.context.getColor(R.color.accent)
                InsightType.OPINION -> itemView.context.getColor(R.color.primary)
                InsightType.STUDY_RESOURCES -> itemView.context.getColor(R.color.subject_stem)
                InsightType.TIPS -> itemView.context.getColor(R.color.subject_business)
                InsightType.EXPERIENCE -> itemView.context.getColor(R.color.subject_arts)
                InsightType.QUESTION -> itemView.context.getColor(R.color.primary)
                InsightType.TIME_REALITY_CHECK -> itemView.context.getColor(R.color.subject_stem)
                InsightType.HIDDEN_COSTS -> itemView.context.getColor(R.color.error)
                InsightType.APPLICATION_TIP -> itemView.context.getColor(R.color.accent)
                InsightType.IMPACT_STORY -> itemView.context.getColor(R.color.subject_arts)
                InsightType.WARNING -> itemView.context.getColor(R.color.error)
                InsightType.SOCIAL_INFO -> itemView.context.getColor(R.color.subject_leadership)
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
