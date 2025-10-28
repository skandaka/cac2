package com.example.cac3.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac3.R
import com.example.cac3.activities.OpportunityDetailActivity
import com.example.cac3.adapter.CommitmentAdapter
import com.example.cac3.adapter.CommitmentWithOpportunity
import com.example.cac3.adapter.OpportunityAdapter
import com.example.cac3.ai.AIManager
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
    private lateinit var aiManager: AIManager

    private lateinit var welcomeTextView: TextView
    private lateinit var totalHoursTextView: TextView
    private lateinit var activeCommitmentsTextView: TextView
    private lateinit var commitmentsRecyclerView: RecyclerView
    private lateinit var noCommitmentsTextView: TextView
    private lateinit var recommendationsRecyclerView: RecyclerView
    private lateinit var recommendationsSubtitle: TextView
    private lateinit var recommendationsLoadingBar: ProgressBar
    private lateinit var aiInsightsCard: com.google.android.material.card.MaterialCardView
    private lateinit var refreshRecommendationsButton: com.google.android.material.button.MaterialButton
    private lateinit var upcomingDeadlinesScroll: android.widget.HorizontalScrollView
    private lateinit var upcomingDeadlinesContainer: android.widget.LinearLayout

    private lateinit var commitmentAdapter: CommitmentAdapter
    private lateinit var recommendationAdapter: OpportunityAdapter

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
        aiManager = AIManager(requireContext())

        initializeViews(view)
        setupRecyclerViews()
        setupRefreshButton()
        loadDashboardData()
    }

    private fun initializeViews(view: View) {
        welcomeTextView = view.findViewById(R.id.welcomeTextView)
        totalHoursTextView = view.findViewById(R.id.totalHoursTextView)
        activeCommitmentsTextView = view.findViewById(R.id.activeCommitmentsTextView)
        commitmentsRecyclerView = view.findViewById(R.id.commitmentsRecyclerView)
        noCommitmentsTextView = view.findViewById(R.id.noCommitmentsTextView)
        recommendationsRecyclerView = view.findViewById(R.id.recommendationsRecyclerView)
        recommendationsSubtitle = view.findViewById(R.id.recommendationsSubtitle)
        recommendationsLoadingBar = view.findViewById(R.id.recommendationsLoadingBar)
        aiInsightsCard = view.findViewById(R.id.aiInsightsCard)
        refreshRecommendationsButton = view.findViewById(R.id.refreshRecommendationsButton)
        upcomingDeadlinesScroll = view.findViewById(R.id.upcomingDeadlinesScroll)
        upcomingDeadlinesContainer = view.findViewById(R.id.upcomingDeadlinesContainer)
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

    private fun setupRefreshButton() {
        refreshRecommendationsButton.setOnClickListener {
            loadRecommendations()
        }
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

                    // Load upcoming deadlines carousel
                    loadUpcomingDeadlines(commitments)

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
                val userId = authManager.getCurrentUserId()
                val user = if (userId != -1L) database.userDao().getUserById(userId) else null
                val allOpportunities = database.opportunityDao().getAllOpportunitiesSync()

                // Get user's existing commitments to filter them out
                val userCommitments = if (userId != -1L) {
                    database.userDao().getUserCommitmentsSync(userId)
                } else {
                    emptyList()
                }
                val committedOpportunityIds = userCommitments.map { it.opportunityId }.toSet()

                // Filter out opportunities user is already committed to
                val availableOpportunities = allOpportunities.filter { it.id !in committedOpportunityIds }

                // Check if AI is configured
                if (aiManager.isApiKeyConfigured() && user != null) {
                    // Use AI-powered recommendations
                    loadAIRecommendations(user, availableOpportunities)
                } else {
                    // Fall back to basic recommendations
                    val userInterests = authManager.getCurrentUserInterests() ?: ""
                    val basicRecommendations = getBasicRecommendations(userInterests, availableOpportunities)
                    recommendationAdapter.submitList(basicRecommendations.take(10))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error loading recommendations: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun loadAIRecommendations(
        user: com.example.cac3.data.model.User,
        opportunities: List<Opportunity>
    ) {
        try {
            // Show loading indicator and AI card
            recommendationsLoadingBar.visibility = View.VISIBLE
            aiInsightsCard.visibility = View.VISIBLE
            refreshRecommendationsButton.visibility = View.VISIBLE
            recommendationsSubtitle.text = "✨ AI-powered recommendations tailored to your profile"

            val result = aiManager.generateRecommendations(user, opportunities, maxResults = 10)

            recommendationsLoadingBar.visibility = View.GONE

            result.onSuccess { recommendations ->
                val opportunityList = recommendations.map { it.opportunity }
                recommendationAdapter.submitList(opportunityList)

                Toast.makeText(
                    requireContext(),
                    "✨ ${opportunityList.size} AI-powered recommendations loaded!",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                // Fall back to basic recommendations on error
                aiInsightsCard.visibility = View.GONE
                val userInterests = user.interests
                val basicRecommendations = getBasicRecommendations(userInterests, opportunities)
                recommendationAdapter.submitList(basicRecommendations.take(10))
                recommendationsSubtitle.text = "Based on your interests (AI unavailable)"

                Toast.makeText(
                    requireContext(),
                    "Using basic recommendations. AI error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            recommendationsLoadingBar.visibility = View.GONE
            aiInsightsCard.visibility = View.GONE
        }
    }

    private fun getBasicRecommendations(
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

    private suspend fun loadUpcomingDeadlines(commitments: List<com.example.cac3.data.model.UserCommitment>) {
        upcomingDeadlinesContainer.removeAllViews()

        val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)
        // Use a map to track unique deadlines by opportunity ID to prevent duplicates
        val uniqueDeadlines = mutableMapOf<Long, Triple<String, Long, String>>()

        for (commitment in commitments) {
            val opportunity = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
            if (opportunity != null) {
                // Add deadlines only if not already added for this opportunity
                if (opportunity.deadline != null &&
                    opportunity.deadline!! > System.currentTimeMillis() &&
                    !uniqueDeadlines.containsKey(opportunity.id)) {
                    uniqueDeadlines[opportunity.id] = Triple(
                        opportunity.title,
                        opportunity.deadline!!,
                        opportunity.category.toString()
                    )
                }
            }
        }

        // Sort by date (nearest first)
        val sortedDeadlines = uniqueDeadlines.values.sortedBy { it.second }

        // Display horizontal carousel of deadlines
        sortedDeadlines.take(10).forEach { (title, deadline, category) ->
            upcomingDeadlinesContainer.addView(createDeadlineCard(title, dateFormat.format(java.util.Date(deadline)), category, deadline))
        }

        if (sortedDeadlines.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = getString(R.string.no_upcoming_deadlines)
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#757575"))
                setPadding(48, 48, 48, 48)
            }
            upcomingDeadlinesContainer.addView(emptyView)
        }
    }

    private fun createDeadlineCard(title: String, date: String, category: String, deadline: Long): View {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                350, // Fixed width for horizontal scrolling
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(android.graphics.Color.WHITE)
        }

        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        // Days until deadline
        val daysUntil = ((deadline - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
        val urgencyColor = when {
            daysUntil <= 7 -> "#F44336"
            daysUntil <= 30 -> "#FF9800"
            else -> "#4CAF50"
        }

        val daysText = TextView(requireContext()).apply {
            text = when {
                daysUntil == 0 -> getString(R.string.today)
                daysUntil == 1 -> getString(R.string.tomorrow)
                daysUntil < 0 -> getString(R.string.overdue)
                else -> getString(R.string.days_until, daysUntil)
            }
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor(urgencyColor))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val titleText = TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val dateText = TextView(requireContext()).apply {
            text = date
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#757575"))
        }

        val categoryText = TextView(requireContext()).apply {
            text = category.replace("_", " ")
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
        }

        container.addView(daysText)
        container.addView(titleText)
        container.addView(dateText)
        container.addView(categoryText)
        card.addView(container)

        return card
    }

    override fun onResume() {
        super.onResume()
        // Reload data when fragment becomes visible
        loadDashboardData()
    }
}
