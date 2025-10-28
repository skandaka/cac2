package com.example.cac3.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac3.R
import com.example.cac3.adapter.CommentAdapter
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.Comment
import com.example.cac3.data.model.InsightType
import com.example.cac3.util.AuthManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Fragment displaying comments/insights about an opportunity
 */
class OpportunityCommentsTabFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var authManager: AuthManager
    private lateinit var adapter: CommentAdapter

    private var opportunityId: Long = -1

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var emptyCommentsLayout: View
    private lateinit var addCommentButton: MaterialButton
    private lateinit var filterButton: MaterialButton

    // Filter state
    private var selectedInsightType: InsightType? = null
    private var verifiedOnly: Boolean = false
    private var withResourcesOnly: Boolean = false
    private var minRating: Int = 0
    private var sortOrder: String = "newest"

    companion object {
        private const val ARG_OPPORTUNITY_ID = "opportunity_id"

        fun newInstance(opportunityId: Long): OpportunityCommentsTabFragment {
            val fragment = OpportunityCommentsTabFragment()
            val args = Bundle()
            args.putLong(ARG_OPPORTUNITY_ID, opportunityId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        opportunityId = arguments?.getLong(ARG_OPPORTUNITY_ID, -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_opportunity_comments_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        authManager = AuthManager(requireContext())

        initializeViews(view)
        setupRecyclerView()
        setupAddCommentButton()
        loadComments()
    }

    private fun initializeViews(view: View) {
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView)
        emptyCommentsLayout = view.findViewById(R.id.emptyCommentsLayout)
        addCommentButton = view.findViewById(R.id.addCommentButton)
        filterButton = view.findViewById(R.id.filterButton)
    }

    private fun setupRecyclerView() {
        val currentUserId = authManager.getCurrentUserId()
        adapter = CommentAdapter(
            currentUserId = currentUserId,
            onEditClick = { comment -> showEditCommentDialog(comment) },
            onDeleteClick = { comment -> showDeleteConfirmation(comment) },
            onReplyClick = { comment -> showReplyDialog(comment) },
            onUpvoteClick = { comment -> upvoteComment(comment) }
        )
        commentsRecyclerView.adapter = adapter
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupAddCommentButton() {
        addCommentButton.setOnClickListener {
            showAddCommentDialog()
        }

        filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun loadComments() {
        // Apply filters and sorting based on current state
        val liveData = when {
            // If all filters are default, use simple query
            selectedInsightType == null && !verifiedOnly && !withResourcesOnly && minRating == 0 && sortOrder == "newest" -> {
                database.commentDao().getCommentsForOpportunity(opportunityId)
            }
            // If only sorting by most helpful
            sortOrder == "helpful" && selectedInsightType == null && !verifiedOnly && !withResourcesOnly && minRating == 0 -> {
                database.commentDao().getCommentsSorted(opportunityId, "helpful")
            }
            // If filtering by insight type only
            selectedInsightType != null && !verifiedOnly && !withResourcesOnly && minRating == 0 -> {
                database.commentDao().getCommentsByType(opportunityId, selectedInsightType!!)
            }
            // If verified only
            verifiedOnly && selectedInsightType == null && !withResourcesOnly && minRating == 0 -> {
                database.commentDao().getVerifiedComments(opportunityId)
            }
            // If with resources only
            withResourcesOnly && selectedInsightType == null && !verifiedOnly && minRating == 0 -> {
                database.commentDao().getCommentsWithResources(opportunityId)
            }
            // If min rating filter
            minRating > 0 && selectedInsightType == null && !verifiedOnly && !withResourcesOnly -> {
                database.commentDao().getHighRatedComments(opportunityId, minRating)
            }
            // Complex filter - use getFilteredComments
            else -> {
                database.commentDao().getFilteredComments(
                    opportunityId = opportunityId,
                    filterType = selectedInsightType,
                    minRating = if (minRating > 0) minRating else null,
                    verifiedOnly = verifiedOnly,
                    withResourcesOnly = withResourcesOnly,
                    sortBy = sortOrder
                )
            }
        }

        liveData.observe(viewLifecycleOwner) { comments ->
            // Apply additional sorting if needed
            val sortedComments = when (sortOrder) {
                "oldest" -> comments.sortedBy { it.createdAt }
                "helpful" -> comments.sortedByDescending { it.upvotes }
                "rating" -> comments.sortedByDescending { it.rating ?: 0 }
                else -> comments.sortedByDescending { it.createdAt } // newest
            }

            if (sortedComments.isEmpty()) {
                commentsRecyclerView.visibility = View.GONE
                emptyCommentsLayout.visibility = View.VISIBLE
            } else {
                commentsRecyclerView.visibility = View.VISIBLE
                emptyCommentsLayout.visibility = View.GONE
                adapter.submitList(sortedComments)
            }
        }
    }

    private fun showAddCommentDialog() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(requireContext(), "Please log in to add a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comment, null)
        val commentInput = dialogView.findViewById<EditText>(R.id.commentInput)
        val insightTypeSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.insightTypeSpinner)
        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.ratingBar)
        val ratingText = dialogView.findViewById<android.widget.TextView>(R.id.ratingText)
        val resourceLinksInput = dialogView.findViewById<EditText>(R.id.resourceLinksInput)
        val verificationCheckbox = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.verificationCheckbox)

        // Setup insight type spinner
        val insightTypes = InsightType.values()
        val insightTypeNames = insightTypes.map { "${it.displayName}\n${it.description}" }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, insightTypeNames)
        insightTypeSpinner.adapter = adapter

        // Setup rating bar listener
        ratingBar.onRatingBarChangeListener = android.widget.RatingBar.OnRatingBarChangeListener { _, rating, _ ->
            ratingText.text = when {
                rating == 0f -> "No rating"
                rating == 1f -> "1 star - Poor"
                rating == 2f -> "2 stars - Fair"
                rating == 3f -> "3 stars - Good"
                rating == 4f -> "4 stars - Great"
                rating == 5f -> "5 stars - Excellent"
                else -> "$rating stars"
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Your Insight")
            .setView(dialogView)
            .setPositiveButton("Post") { dialog, _ ->
                val commentText = commentInput.text.toString().trim()
                val selectedInsightType = insightTypes[insightTypeSpinner.selectedItemPosition]
                val rating = ratingBar.rating.toInt().takeIf { it > 0 }
                val resourceLinks = resourceLinksInput.text.toString().trim().takeIf { it.isNotEmpty() }
                val isVerified = verificationCheckbox.isChecked

                if (commentText.isNotEmpty()) {
                    addComment(commentText, selectedInsightType, rating, resourceLinks, isVerified)
                } else {
                    Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addComment(
        commentText: String,
        insightType: InsightType,
        rating: Int?,
        resourceLinks: String?,
        isVerified: Boolean
    ) {
        val userName = authManager.getCurrentUserName() ?: "Anonymous"
        val userGrade = authManager.getCurrentUserGrade()
        val userId = authManager.getCurrentUserId()

        val comment = Comment(
            opportunityId = opportunityId,
            userId = userId,
            userName = userName,
            userGrade = userGrade,
            comment = commentText,
            insightType = insightType,
            rating = rating,
            resourceLinks = resourceLinks,
            hasResources = !resourceLinks.isNullOrEmpty(),
            isVerifiedParticipant = isVerified
        )

        lifecycleScope.launch {
            try {
                database.commentDao().insert(comment)
                Toast.makeText(requireContext(), "Comment posted!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error posting comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter_comments, null)

        // Get views
        val sortRadioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.sortRadioGroup)
        val insightTypeSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.insightTypeFilterSpinner)
        val verifiedCheckbox = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.verifiedOnlyCheckbox)
        val withResourcesCheckbox = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.withResourcesCheckbox)
        val minRatingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.minRatingBar)
        val minRatingText = dialogView.findViewById<android.widget.TextView>(R.id.minRatingText)
        val resetButton = dialogView.findViewById<MaterialButton>(R.id.resetFiltersButton)
        val applyButton = dialogView.findViewById<MaterialButton>(R.id.applyFiltersButton)

        // Setup insight type spinner with "All" option
        val insightTypeOptions = listOf("All Insight Types") + InsightType.values().map { it.displayName }
        val spinnerAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, insightTypeOptions)
        insightTypeSpinner.adapter = spinnerAdapter

        // Set current filter values
        verifiedCheckbox.isChecked = verifiedOnly
        withResourcesCheckbox.isChecked = withResourcesOnly
        minRatingBar.rating = minRating.toFloat()

        // Select current insight type
        selectedInsightType?.let { type ->
            val index = InsightType.values().indexOf(type) + 1 // +1 for "All" option
            insightTypeSpinner.setSelection(index)
        }

        // Select current sort order
        when (sortOrder) {
            "newest" -> sortRadioGroup.check(R.id.sortNewestRadio)
            "oldest" -> sortRadioGroup.check(R.id.sortOldestRadio)
            "helpful" -> sortRadioGroup.check(R.id.sortMostHelpfulRadio)
            "rating" -> sortRadioGroup.check(R.id.sortHighestRatedRadio)
        }

        // Rating bar listener
        minRatingBar.onRatingBarChangeListener = android.widget.RatingBar.OnRatingBarChangeListener { _, rating, _ ->
            minRatingText.text = when {
                rating == 0f -> "No minimum"
                rating == 1f -> "1+ stars"
                rating == 2f -> "2+ stars"
                rating == 3f -> "3+ stars"
                rating == 4f -> "4+ stars"
                rating == 5f -> "5 stars"
                else -> "${rating.toInt()}+ stars"
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Reset button
        resetButton.setOnClickListener {
            selectedInsightType = null
            verifiedOnly = false
            withResourcesOnly = false
            minRating = 0
            sortOrder = "newest"
            loadComments()
            dialog.dismiss()
            Toast.makeText(requireContext(), "Filters reset", Toast.LENGTH_SHORT).show()
        }

        // Apply button
        applyButton.setOnClickListener {
            // Get sort order
            sortOrder = when (sortRadioGroup.checkedRadioButtonId) {
                R.id.sortOldestRadio -> "oldest"
                R.id.sortMostHelpfulRadio -> "helpful"
                R.id.sortHighestRatedRadio -> "rating"
                else -> "newest"
            }

            // Get insight type filter
            val selectedPosition = insightTypeSpinner.selectedItemPosition
            selectedInsightType = if (selectedPosition == 0) null else InsightType.values()[selectedPosition - 1]

            // Get checkbox filters
            verifiedOnly = verifiedCheckbox.isChecked
            withResourcesOnly = withResourcesCheckbox.isChecked

            // Get rating filter
            minRating = minRatingBar.rating.toInt()

            // Reload comments with filters
            loadComments()
            dialog.dismiss()
            Toast.makeText(requireContext(), "Filters applied", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showEditCommentDialog(comment: Comment) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comment, null)
        val commentInput = dialogView.findViewById<EditText>(R.id.commentInput)
        val insightTypeSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.insightTypeSpinner)
        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.ratingBar)
        val ratingText = dialogView.findViewById<android.widget.TextView>(R.id.ratingText)
        val resourceLinksInput = dialogView.findViewById<EditText>(R.id.resourceLinksInput)
        val verificationCheckbox = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.verificationCheckbox)

        // Pre-fill with existing values
        commentInput.setText(comment.comment)

        // Setup insight type spinner
        val insightTypes = InsightType.values()
        val insightTypeNames = insightTypes.map { "${it.displayName}\n${it.description}" }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, insightTypeNames)
        insightTypeSpinner.adapter = adapter
        insightTypeSpinner.setSelection(insightTypes.indexOf(comment.insightType))

        // Set rating
        if (comment.rating != null && comment.rating > 0) {
            ratingBar.rating = comment.rating.toFloat()
        }

        // Set resource links
        if (!comment.resourceLinks.isNullOrEmpty()) {
            resourceLinksInput.setText(comment.resourceLinks)
        }

        // Set verification
        verificationCheckbox.isChecked = comment.isVerifiedParticipant

        // Setup rating bar listener
        ratingBar.onRatingBarChangeListener = android.widget.RatingBar.OnRatingBarChangeListener { _, rating, _ ->
            ratingText.text = when {
                rating == 0f -> "No rating"
                rating == 1f -> "1 star - Poor"
                rating == 2f -> "2 stars - Fair"
                rating == 3f -> "3 stars - Good"
                rating == 4f -> "4 stars - Great"
                rating == 5f -> "5 stars - Excellent"
                else -> "$rating stars"
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Your Insight")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val commentText = commentInput.text.toString().trim()
                val selectedInsightType = insightTypes[insightTypeSpinner.selectedItemPosition]
                val rating = ratingBar.rating.toInt().takeIf { it > 0 }
                val resourceLinks = resourceLinksInput.text.toString().trim().takeIf { it.isNotEmpty() }
                val isVerified = verificationCheckbox.isChecked

                if (commentText.isNotEmpty()) {
                    updateComment(comment, commentText, selectedInsightType, rating, resourceLinks, isVerified)
                } else {
                    Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateComment(
        originalComment: Comment,
        commentText: String,
        insightType: InsightType,
        rating: Int?,
        resourceLinks: String?,
        isVerified: Boolean
    ) {
        val updatedComment = originalComment.copy(
            comment = commentText,
            insightType = insightType,
            rating = rating,
            resourceLinks = resourceLinks,
            hasResources = !resourceLinks.isNullOrEmpty(),
            isVerifiedParticipant = isVerified,
            updatedAt = System.currentTimeMillis(),
            lastEditedAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                database.commentDao().update(updatedComment)
                Toast.makeText(requireContext(), "Comment updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error updating comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation(comment: Comment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteComment(comment)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteComment(comment: Comment) {
        lifecycleScope.launch {
            try {
                database.commentDao().delete(comment)
                Toast.makeText(requireContext(), "Comment deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error deleting comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReplyDialog(parentComment: Comment) {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(requireContext(), "Please log in to reply", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a simple text input dialog for replies
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comment, null)
        val commentInput = dialogView.findViewById<EditText>(R.id.commentInput)

        // Hide unnecessary fields for replies
        // Hide insight type section
        dialogView.findViewById<View>(R.id.insightTypeSpinner)?.visibility = View.GONE
        dialogView.findViewById<android.widget.TextView>(R.id.insightTypeSpinner)?.parent?.let {
            (it as? View)?.visibility = View.GONE
        }

        // Hide rating section - hide the whole LinearLayout containing RatingBar
        dialogView.findViewById<android.widget.RatingBar>(R.id.ratingBar)?.parent?.let {
            (it as? View)?.visibility = View.GONE
        }

        // Hide resource links section - hide the TextInputLayout
        dialogView.findViewById<android.widget.EditText>(R.id.resourceLinksInput)?.parent?.let {
            (it as? View)?.visibility = View.GONE
        }

        // Hide verification checkbox
        dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.verificationCheckbox)?.visibility = View.GONE

        // Set hint for reply
        commentInput.hint = "Write your reply..."

        AlertDialog.Builder(requireContext())
            .setTitle("Reply to ${parentComment.userName}")
            .setView(dialogView)
            .setPositiveButton("Post Reply") { dialog, _ ->
                val replyText = commentInput.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    addReply(parentComment.id, replyText)
                } else {
                    Toast.makeText(requireContext(), "Please enter a reply", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addReply(parentCommentId: Long, replyText: String) {
        val userName = authManager.getCurrentUserName() ?: "Anonymous"
        val userGrade = authManager.getCurrentUserGrade()
        val userId = authManager.getCurrentUserId()

        val reply = Comment(
            opportunityId = opportunityId,
            parentCommentId = parentCommentId,
            userId = userId,
            userName = userName,
            userGrade = userGrade,
            comment = replyText,
            insightType = InsightType.HELP // Default type for replies
        )

        lifecycleScope.launch {
            try {
                database.commentDao().insert(reply)
                Toast.makeText(requireContext(), "Reply posted!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error posting reply", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun upvoteComment(comment: Comment) {
        lifecycleScope.launch {
            try {
                database.commentDao().upvote(comment.id)
                // No toast needed - the UI will update automatically via LiveData
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error marking comment as helpful", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
