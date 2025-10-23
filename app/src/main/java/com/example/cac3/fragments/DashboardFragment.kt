package com.example.cac3.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac3.R
import com.example.cac3.activities.OpportunityDetailActivity
import com.example.cac3.adapter.CommitmentAdapter
import com.example.cac3.adapter.CommitmentWithOpportunity
import com.example.cac3.adapter.OpportunityAdapter
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.Opportunity
import com.example.cac3.util.AuthManager
import kotlinx.coroutines.launch

/**
 * Dashboard Fragment - Personalized home screen showing user's commitments and recommendations
 */
class DashboardFragment : Fragment() {

    private lateinit var authManager: AuthManager
    private lateinit var database: AppDatabase

    private lateinit var welcomeTextView: TextView
    private lateinit var totalHoursTextView: TextView
    private lateinit var activeCommitmentsTextView: TextView
    private lateinit var commitmentsRecyclerView: RecyclerView
    private lateinit var noCommitmentsTextView: TextView
    private lateinit var recommendationsRecyclerView: RecyclerView
    private lateinit var calendarView: android.widget.CalendarView
    private lateinit var selectedDateTextView: TextView
    private lateinit var calendarCommitmentsTextView: TextView

    private lateinit var commitmentAdapter: CommitmentAdapter
    private lateinit var recommendationAdapter: OpportunityAdapter

    // Store commitments for calendar filtering
    private var allCommitments: List<CommitmentWithOpportunity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager(requireContext())
        database = AppDatabase.getDatabase(requireContext())

        initializeViews(view)
        setupRecyclerViews()
        setupCalendar()
        loadDashboardData()
    }

    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = java.util.Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.US)
            selectedDateTextView.text = dateFormat.format(selectedDate.time)

            // Filter commitments for this date
            displayCommitmentsForDate(selectedDate.timeInMillis)
        }
    }

    private fun displayCommitmentsForDate(dateInMillis: Long) {
        val commitmentsOnDate = allCommitments.filter { commitment ->
            // Check if the commitment is active on this date
            val startDate = commitment.commitment.startDate ?: 0L
            val endDate = commitment.commitment.endDate ?: Long.MAX_VALUE

            dateInMillis >= startDate && dateInMillis <= endDate
        }

        if (commitmentsOnDate.isEmpty()) {
            calendarCommitmentsTextView.visibility = View.GONE
        } else {
            calendarCommitmentsTextView.visibility = View.VISIBLE
            val commitmentsText = commitmentsOnDate.joinToString("\n") {
                "• ${it.opportunity.title} (${it.commitment.hoursPerWeek} hrs/week)"
            }
            calendarCommitmentsTextView.text = "Commitments on this day:\n$commitmentsText"
        }
    }

    private fun initializeViews(view: View) {
        welcomeTextView = view.findViewById(R.id.welcomeTextView)
        totalHoursTextView = view.findViewById(R.id.totalHoursTextView)
        activeCommitmentsTextView = view.findViewById(R.id.activeCommitmentsTextView)
        commitmentsRecyclerView = view.findViewById(R.id.commitmentsRecyclerView)
        noCommitmentsTextView = view.findViewById(R.id.noCommitmentsTextView)
        recommendationsRecyclerView = view.findViewById(R.id.recommendationsRecyclerView)
        calendarView = view.findViewById(R.id.calendarView)
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView)
        calendarCommitmentsTextView = view.findViewById(R.id.calendarCommitmentsTextView)
    }

    private fun setupRecyclerViews() {
        // Commitments RecyclerView
        commitmentAdapter = CommitmentAdapter { commitmentWithOpp ->
            val intent = Intent(requireContext(), OpportunityDetailActivity::class.java)
            intent.putExtra("opportunity_id", commitmentWithOpp.opportunity.id)
            startActivity(intent)
        }
        commitmentsRecyclerView.adapter = commitmentAdapter
        commitmentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Recommendations RecyclerView
        recommendationAdapter = OpportunityAdapter { opportunity ->
            val intent = Intent(requireContext(), OpportunityDetailActivity::class.java)
            intent.putExtra("opportunity_id", opportunity.id)
            startActivity(intent)
        }
        recommendationsRecyclerView.adapter = recommendationAdapter
        recommendationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadDashboardData() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            return
        }

        // Set welcome message
        val userName = authManager.getCurrentUserName() ?: "Student"
        welcomeTextView.text = getString(R.string.welcome_user, userName)

        // Load commitments
        loadCommitments(userId)

        // Load recommendations
        loadRecommendations()
    }

    private fun loadCommitments(userId: Long) {
        // Observe commitments LiveData
        database.userDao().getUserCommitments(userId).observe(viewLifecycleOwner) { commitments ->
            if (commitments.isEmpty()) {
                noCommitmentsTextView.visibility = View.VISIBLE
                commitmentsRecyclerView.visibility = View.GONE
                totalHoursTextView.text = "0"
                activeCommitmentsTextView.text = "0"
                return@observe
            }

            // Load opportunities for each commitment
            lifecycleScope.launch {
                try {
                    val commitmentsWithOpportunities = mutableListOf<CommitmentWithOpportunity>()
                    var totalHours = 0

                    for (commitment in commitments) {
                        // Get opportunity using suspend function
                        val opportunity = database.opportunityDao()
                            .getOpportunityByIdSync(commitment.opportunityId)

                        if (opportunity != null) {
                            commitmentsWithOpportunities.add(
                                CommitmentWithOpportunity(commitment, opportunity)
                            )
                            totalHours += commitment.hoursPerWeek
                        }
                    }

                    // Update UI on main thread
                    noCommitmentsTextView.visibility = View.GONE
                    commitmentsRecyclerView.visibility = View.VISIBLE
                    commitmentAdapter.submitList(commitmentsWithOpportunities)
                    totalHoursTextView.text = totalHours.toString()
                    activeCommitmentsTextView.text = commitments.size.toString()

                    // Store commitments for calendar
                    allCommitments = commitmentsWithOpportunities

                    // Update calendar display for today
                    displayCommitmentsForDate(System.currentTimeMillis())

                } catch (e: Exception) {
                    e.printStackTrace()
                    noCommitmentsTextView.visibility = View.VISIBLE
                    commitmentsRecyclerView.visibility = View.GONE
                }
            }
        }
    }

    private fun loadRecommendations() {
        lifecycleScope.launch {
            try {
                val userInterests = authManager.getCurrentUserInterests() ?: ""
                val allOpportunities = database.opportunityDao().getAllOpportunities().value ?: emptyList()

                // Simple recommendation: filter by user interests
                val recommendations = getRecommendationsForUser(userInterests, allOpportunities)

                recommendationAdapter.submitList(recommendations.take(10))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getRecommendationsForUser(
        interests: String,
        opportunities: List<Opportunity>
    ): List<Opportunity> {
        if (interests.isEmpty()) {
            // If no interests, return high-priority opportunities
            return opportunities.sortedByDescending { it.priority }.take(10)
        }

        val interestList = interests.split(",").map { it.trim().lowercase() }

        // Score opportunities based on interest match
        return opportunities.map { opportunity ->
            var score = opportunity.priority

            // Check if tags match interests
            val tags = opportunity.tags?.lowercase() ?: ""
            for (interest in interestList) {
                if (tags.contains(interest)) {
                    score += 10
                }
            }

            // Check if description matches interests
            val description = opportunity.description.lowercase()
            for (interest in interestList) {
                if (description.contains(interest)) {
                    score += 5
                }
            }

            Pair(opportunity, score)
        }
        .sortedByDescending { it.second }
        .map { it.first }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when fragment becomes visible
        loadDashboardData()
    }
}
