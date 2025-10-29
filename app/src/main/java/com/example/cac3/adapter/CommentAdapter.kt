package com.example.cac3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
 import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cac3.R
import com.example.cac3.data.model.Comment
import com.example.cac3.data.model.InsightType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CommentAdapter(
    private val currentUserId: Long,
    private val lifecycleOwner: LifecycleOwner,
    private val getRepliesLiveData: (Long) -> LiveData<List<Comment>>,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit,
    private val onReplyClick: (Comment) -> Unit,
    private val onUpvoteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(
            itemView = view,
            currentUserId = currentUserId,
            lifecycleOwner = lifecycleOwner,
            getRepliesLiveData = getRepliesLiveData,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            onReplyClick = onReplyClick,
            onUpvoteClick = onUpvoteClick
        )
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: CommentViewHolder) {
        super.onViewRecycled(holder)
        holder.clearRepliesObserver()
    }

    class CommentViewHolder(
        itemView: View,
        private val currentUserId: Long,
        private val lifecycleOwner: LifecycleOwner,
        private val getRepliesLiveData: (Long) -> LiveData<List<Comment>>,
        private val onEditClick: (Comment) -> Unit,
        private val onDeleteClick: (Comment) -> Unit,
        private val onReplyClick: (Comment) -> Unit,
        private val onUpvoteClick: (Comment) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        // Top-level comment views
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val userGradeTextView: TextView = itemView.findViewById(R.id.userGradeTextView)
        private val verifiedBadge: TextView = itemView.findViewById(R.id.verifiedBadge)
        private val insightTypeBadge: TextView = itemView.findViewById(R.id.insightTypeBadge)
        private val ratingLayout: View = itemView.findViewById(R.id.ratingLayout)
        private val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
        private val commentRatingBar: RatingBar = itemView.findViewById(R.id.commentRatingBar)
        private val ratingTextView: TextView = itemView.findViewById(R.id.ratingTextView)
        private val resourceLinksLayout: View = itemView.findViewById(R.id.resourceLinksLayout)
        private val resourceLinksTextView: TextView = itemView.findViewById(R.id.resourceLinksTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val upvoteCountTextView: TextView = itemView.findViewById(R.id.upvoteCountTextView)
        private val replyButton: TextView = itemView.findViewById(R.id.replyButton)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val repliesContainer: LinearLayout = itemView.findViewById(R.id.repliesContainer)

        // Replies observation management
        private var observedParentId: Long? = null
        private var repliesLiveData: LiveData<List<Comment>>? = null
        private var repliesObserver: Observer<List<Comment>>? = null

        fun bind(comment: Comment) {
            // Reset replies UI for recycling
            repliesContainer.removeAllViews()
            repliesContainer.visibility = View.GONE

            // User name and grade
            userNameTextView.text = comment.userName
            val grade = comment.userGrade
            if (grade != null && grade > 0) {
                userGradeTextView.text = "Grade $grade"
                userGradeTextView.visibility = View.VISIBLE
            } else {
                userGradeTextView.visibility = View.GONE
            }

            // Verified badge
            verifiedBadge.visibility = if (comment.isVerifiedParticipant) View.VISIBLE else View.GONE

            // Insight type badge
            insightTypeBadge.text = formatInsightType(comment.insightType)
            insightTypeBadge.setBackgroundColor(getInsightTypeColor(comment.insightType))

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

            // Resources
            if (!comment.resourceLinks.isNullOrEmpty()) {
                resourceLinksLayout.visibility = View.VISIBLE
                resourceLinksTextView.text = comment.resourceLinks
            } else {
                resourceLinksLayout.visibility = View.GONE
            }

            // Timestamp
            timestampTextView.text = getTimeAgo(comment.createdAt)

            // Upvote UI
            upvoteCountTextView.text = if (comment.upvotes > 0) {
                "${comment.upvotes} helpful · Mark as Helpful"
            } else {
                "Mark as Helpful"
            }
            upvoteCountTextView.setOnClickListener { onUpvoteClick(comment) }

            // Ownership actions
            val isOwnComment = comment.userId != null && comment.userId == currentUserId
            editButton.visibility = if (isOwnComment) View.VISIBLE else View.GONE
            deleteButton.visibility = if (isOwnComment) View.VISIBLE else View.GONE

            editButton.setOnClickListener { onEditClick(comment) }
            deleteButton.setOnClickListener { onDeleteClick(comment) }
            replyButton.setOnClickListener { onReplyClick(comment) }

            // Observe and render replies for top-level comments only
            if (comment.parentCommentId == null) {
                observeReplies(comment.id)
            } else {
                clearRepliesObserver()
                repliesContainer.visibility = View.GONE
            }
        }

        private fun observeReplies(parentId: Long) {
            if (observedParentId == parentId && repliesObserver != null) {
                // Already observing this parent; no-op. We'll still refresh the current list from LiveData value if present
                return
            }
            // Switch observation to new parent
            clearRepliesObserver()
            observedParentId = parentId
            repliesLiveData = getRepliesLiveData(parentId)
            val observer = Observer<List<Comment>> { replies ->
                renderReplies(replies)
            }
            repliesObserver = observer
            repliesLiveData?.observe(lifecycleOwner, observer)
        }

        private fun renderReplies(replies: List<Comment>?) {
            repliesContainer.removeAllViews()
            if (replies.isNullOrEmpty()) {
                repliesContainer.visibility = View.GONE
                return
            }
            repliesContainer.visibility = View.VISIBLE
            val inflater = LayoutInflater.from(itemView.context)
            for (reply in replies) {
                val view = inflater.inflate(R.layout.item_reply, repliesContainer, false)
                val replyUserName = view.findViewById<TextView>(R.id.replyUserName)
                val replyTimestamp = view.findViewById<TextView>(R.id.replyTimestamp)
                val replyText = view.findViewById<TextView>(R.id.replyText)

                replyUserName.text = reply.userName
                replyTimestamp.text = getTimeAgo(reply.createdAt)
                replyText.text = reply.comment

                repliesContainer.addView(view)
            }
        }

        fun clearRepliesObserver() {
            repliesLiveData?.removeObservers(lifecycleOwner)
            repliesObserver = null
            repliesLiveData = null
            observedParentId = null
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
                else -> SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean = oldItem == newItem
    }
}
