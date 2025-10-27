package com.example.cac3.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cac3.R
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.OpportunityCategory
import com.example.cac3.util.AuthManager
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Analytics Fragment - Advanced insights, time management analytics, impact metrics, trends
 */
class AnalyticsFragment : Fragment() {

    private lateinit var authManager: AuthManager
    private lateinit var database: AppDatabase

    private lateinit var timeBreakdownPieChart: PieChart
    private lateinit var timeBreakdownContainer: LinearLayout
    private lateinit var impactMetricsContainer: LinearLayout
    private lateinit var trendsContainer: LinearLayout
    private lateinit var upcomingDeadlinesScroll: HorizontalScrollView
    private lateinit var upcomingDeadlinesContainer: LinearLayout

    private lateinit var totalHoursTextView: TextView
    private lateinit var totalImpactTextView: TextView
    private lateinit var weekBalanceTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager(requireContext())
        database = AppDatabase.getDatabase(requireContext())

        initializeViews(view)
        loadAnalytics()
    }

    private fun initializeViews(view: View) {
        timeBreakdownContainer = view.findViewById(R.id.timeBreakdownContainer)
        impactMetricsContainer = view.findViewById(R.id.impactMetricsContainer)
        trendsContainer = view.findViewById(R.id.trendsContainer)

        // Initialize upcoming deadlines carousel (will be added to layout later if not present)
        // These will be available once we update the layout file

        totalHoursTextView = view.findViewById(R.id.totalHoursTextView)
        totalImpactTextView = view.findViewById(R.id.totalImpactTextView)
        weekBalanceTextView = view.findViewById(R.id.weekBalanceTextView)
    }

    private fun loadAnalytics() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) return

        lifecycleScope.launch {
            try {
                val commitments = database.userDao().getUserCommitmentsSync(userId)
                val allOpportunities = database.opportunityDao().getAllOpportunitiesSync()

                // Load time breakdown analytics
                loadTimeBreakdown(commitments)

                // Load impact metrics
                loadImpactMetrics(commitments)

                // Load opportunity trends
                loadOpportunityTrends(allOpportunities)

                // Load upcoming deadlines carousel
                loadUpcomingDeadlines(commitments)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadTimeBreakdown(commitments: List<com.example.cac3.data.model.UserCommitment>) {
        val categoryBreakdown = mutableMapOf<OpportunityCategory, Int>()
        var totalHours = 0

        for (commitment in commitments) {
            val opportunity = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
            if (opportunity != null) {
                val hours = commitment.hoursPerWeek
                totalHours += hours
                categoryBreakdown[opportunity.category] =
                    categoryBreakdown.getOrDefault(opportunity.category, 0) + hours
            }
        }

        // Update total hours
        totalHoursTextView.text = "$totalHours hrs/week"

        // Calculate weekly balance (168 hours in a week)
        val remainingHours = 168 - totalHours - 50 // Assuming 50 hours for school + sleep
        weekBalanceTextView.text = when {
            remainingHours > 40 -> "Great balance! $remainingHours hrs free"
            remainingHours > 20 -> "Balanced schedule ($remainingHours hrs free)"
            remainingHours > 0 -> "Busy schedule! Only $remainingHours hrs free"
            else -> "Overcommitted! ${-remainingHours} hrs over capacity"
        }
        weekBalanceTextView.setTextColor(when {
            remainingHours > 40 -> Color.parseColor("#4CAF50")
            remainingHours > 20 -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#F44336")
        })

        // Create visual breakdown
        timeBreakdownContainer.removeAllViews()

        if (categoryBreakdown.isNotEmpty()) {
            val colors = mapOf(
                OpportunityCategory.COMPETITION to "#E91E63",
                OpportunityCategory.EMPLOYMENT to "#2196F3",
                OpportunityCategory.VOLUNTEERING to "#4CAF50",
                OpportunityCategory.CLUB to "#FF9800",
                OpportunityCategory.COLLEGE to "#9C27B0",
                OpportunityCategory.SUMMER_PROGRAM to "#00BCD4",
                OpportunityCategory.INTERNSHIP to "#3F51B5"
            )

            categoryBreakdown.entries.sortedByDescending { it.value }.forEach { (category, hours) ->
                val percentage = if (totalHours > 0) (hours * 100 / totalHours) else 0
                val card = createCategoryCard(category, hours, percentage, colors[category] ?: "#9E9E9E")
                timeBreakdownContainer.addView(card)
            }
        }
    }

    private suspend fun loadImpactMetrics(commitments: List<com.example.cac3.data.model.UserCommitment>) {
        var serviceHours = 0
        var peopleImpacted = 0
        var skillsDeveloped = mutableSetOf<String>()
        var scholarshipPotential = 0

        for (commitment in commitments) {
            val opportunity = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
            if (opportunity != null) {
                // Calculate service hours
                if (opportunity.category == OpportunityCategory.VOLUNTEERING) {
                    val durationInWeeks = if (commitment.startDate != null && commitment.endDate != null) {
                        val duration = commitment.endDate!! - commitment.startDate!!
                        (duration / (7 * 24 * 60 * 60 * 1000)).toInt()
                    } else {
                        12 // Default to 12 weeks if no dates
                    }
                    serviceHours += commitment.hoursPerWeek * durationInWeeks
                    peopleImpacted += (commitment.hoursPerWeek * 10) // Estimate 10 people per hour
                }

                // Track skills
                opportunity.tags?.split(",")?.forEach { tag ->
                    skillsDeveloped.add(tag.trim())
                }

                // Calculate scholarship potential
                if (opportunity.scholarshipAvailable) {
                    scholarshipPotential += (opportunity.cost?.toInt() ?: 0)
                }
            }
        }

        // Update impact metrics
        totalImpactTextView.text = "$serviceHours service hours"

        impactMetricsContainer.removeAllViews()

        if (commitments.isNotEmpty()) {
            // Service Hours Card
            impactMetricsContainer.addView(createMetricCard(
                "Community Service Hours",
                "$serviceHours hours",
                if (serviceHours >= 40) "Excellent progress!" else "Keep going!",
                "#4CAF50"
            ))

            // People Impacted
            impactMetricsContainer.addView(createMetricCard(
                "People Impacted",
                "$peopleImpacted people",
                "Through volunteer work",
                "#2196F3"
            ))

            // Skills Developed
            impactMetricsContainer.addView(createMetricCard(
                "Skills Developed",
                "${skillsDeveloped.size} skills",
                if (skillsDeveloped.isNotEmpty()) skillsDeveloped.take(5).joinToString(", ") else "Track your growth",
                "#FF9800"
            ))

            // Scholarship Potential
            impactMetricsContainer.addView(createMetricCard(
                "Scholarship Potential",
                "$$scholarshipPotential",
                "From current activities",
                "#9C27B0"
            ))
        }
    }

    private fun loadOpportunityTrends(opportunities: List<com.example.cac3.data.model.Opportunity>) {
        trendsContainer.removeAllViews()

        // Most popular categories
        val categoryCount = opportunities.groupingBy { it.category }.eachCount()
        val topCategories = categoryCount.entries.sortedByDescending { it.value }.take(3)

        trendsContainer.addView(createTrendCard(
            "Most Popular Categories",
            topCategories.map { "${it.key}: ${it.value} opportunities" }.joinToString("\n")
        ))

        // Upcoming deadlines count
        val now = System.currentTimeMillis()
        val upcomingDeadlines = opportunities.count {
            it.deadline != null && it.deadline!! > now && it.deadline!! < now + (30 * 24 * 60 * 60 * 1000L)
        }

        trendsContainer.addView(createTrendCard(
            "Upcoming Deadlines (30 days)",
            "$upcomingDeadlines opportunities closing soon"
        ))

        // Virtual vs In-Person
        val virtualCount = opportunities.count { it.isVirtual }
        val inPersonCount = opportunities.size - virtualCount

        trendsContainer.addView(createTrendCard(
            "Virtual vs In-Person",
            "Virtual: $virtualCount | In-Person: $inPersonCount"
        ))
    }

    private suspend fun loadUpcomingDeadlines(commitments: List<com.example.cac3.data.model.UserCommitment>) {
        // Check if views are initialized
        if (!::upcomingDeadlinesContainer.isInitialized) return

        upcomingDeadlinesContainer.removeAllViews()

        val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
        val upcomingDeadlines = mutableListOf<Triple<String, Long, String>>()

        for (commitment in commitments) {
            val opportunity = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
            if (opportunity != null) {
                // Add deadlines
                if (opportunity.deadline != null && opportunity.deadline!! > System.currentTimeMillis()) {
                    upcomingDeadlines.add(Triple(
                        opportunity.title,
                        opportunity.deadline!!,
                        opportunity.category.toString()
                    ))
                }
            }
        }

        // Sort by date (nearest first)
        upcomingDeadlines.sortBy { it.second }

        // Display horizontal carousel of deadlines
        upcomingDeadlines.take(10).forEach { (title, deadline, category) ->
            upcomingDeadlinesContainer.addView(createDeadlineCard(title, dateFormat.format(Date(deadline)), category, deadline))
        }

        if (upcomingDeadlines.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "No upcoming deadlines"
                textSize = 14f
                setTextColor(Color.parseColor("#757575"))
                setPadding(48, 48, 48, 48)
            }
            upcomingDeadlinesContainer.addView(emptyView)
        }
    }

    private fun createDeadlineCard(title: String, date: String, category: String, deadline: Long): View {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                350, // Fixed width for horizontal scrolling
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(Color.WHITE)
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
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
                daysUntil == 0 -> "TODAY"
                daysUntil == 1 -> "TOMORROW"
                daysUntil < 0 -> "OVERDUE"
                else -> "$daysUntil DAYS"
            }
            textSize = 24f
            setTextColor(Color.parseColor(urgencyColor))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val titleText = TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val dateText = TextView(requireContext()).apply {
            text = date
            textSize = 14f
            setTextColor(Color.parseColor("#757575"))
        }

        val categoryText = TextView(requireContext()).apply {
            text = category.replace("_", " ")
            textSize = 12f
            setTextColor(Color.parseColor("#9E9E9E"))
        }

        container.addView(daysText)
        container.addView(titleText)
        container.addView(dateText)
        container.addView(categoryText)
        card.addView(container)

        return card
    }

    private fun createCategoryCard(category: OpportunityCategory, hours: Int, percentage: Int, color: String): View {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(Color.WHITE)
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 24, 24, 24)
        }

        val colorBar = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(8, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = 16
            }
            setBackgroundColor(Color.parseColor(color))
        }

        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val categoryText = TextView(requireContext()).apply {
            text = category.toString().replace("_", " ")
            textSize = 16f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val statsText = TextView(requireContext()).apply {
            text = "$hours hrs/week • $percentage%"
            textSize = 14f
            setTextColor(Color.parseColor("#757575"))
        }

        textContainer.addView(categoryText)
        textContainer.addView(statsText)

        container.addView(colorBar)
        container.addView(textContainer)
        card.addView(container)

        return card
    }

    private fun createMetricCard(title: String, value: String, subtitle: String, color: String): View {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(Color.WHITE)
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val titleText = TextView(requireContext()).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#757575"))
        }

        val valueText = TextView(requireContext()).apply {
            text = value
            textSize = 32f
            setTextColor(Color.parseColor(color))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitleText = TextView(requireContext()).apply {
            text = subtitle
            textSize = 12f
            setTextColor(Color.parseColor("#9E9E9E"))
        }

        container.addView(titleText)
        container.addView(valueText)
        container.addView(subtitleText)
        card.addView(container)

        return card
    }

    private fun createTrendCard(title: String, content: String): View {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(Color.WHITE)
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val titleText = TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val contentText = TextView(requireContext()).apply {
            text = content
            textSize = 14f
            setTextColor(Color.parseColor("#757575"))
        }

        container.addView(titleText)
        container.addView(contentText)
        card.addView(container)

        return card
    }

    override fun onResume() {
        super.onResume()
        loadAnalytics()
    }
}
