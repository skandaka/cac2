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
    }

    private fun setupRecyclerView() {
        adapter = CommentAdapter()
        commentsRecyclerView.adapter = adapter
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupAddCommentButton() {
        addCommentButton.setOnClickListener {
            showAddCommentDialog()
        }
    }

    private fun loadComments() {
        database.commentDao().getCommentsForOpportunity(opportunityId)
            .observe(viewLifecycleOwner) { comments ->
                if (comments.isEmpty()) {
                    commentsRecyclerView.visibility = View.GONE
                    emptyCommentsLayout.visibility = View.VISIBLE
                } else {
                    commentsRecyclerView.visibility = View.VISIBLE
                    emptyCommentsLayout.visibility = View.GONE
                    adapter.submitList(comments)
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

        AlertDialog.Builder(requireContext())
            .setTitle("Add Your Insight")
            .setView(dialogView)
            .setPositiveButton("Post") { dialog, _ ->
                val commentText = commentInput.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    addComment(commentText)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addComment(commentText: String) {
        val userName = authManager.getCurrentUserName() ?: "Anonymous"
        val userGrade = authManager.getCurrentUserGrade()

        val comment = Comment(
            opportunityId = opportunityId,
            userName = userName,
            userGrade = userGrade,
            comment = commentText,
            insightType = InsightType.GENERAL_REVIEW
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
}
