package com.example.cac3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cac3.R
import com.example.cac3.activities.OpportunityDetailActivity
import com.example.cac3.ai.AIManager
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.Opportunity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment displaying detailed information about an opportunity
 */
class OpportunityDetailsTabFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var aiManager: AIManager
    private var opportunityId: Long = -1
    private var currentOpportunity: Opportunity? = null

    private lateinit var titleTextView: TextView
    private lateinit var organizationTextView: TextView
    private lateinit var categoryTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var deadlineTextView: TextView
    private lateinit var costTextView: TextView
    private lateinit var hoursTextView: TextView
    private lateinit var websiteTextView: TextView

    // Club-specific fields
    private lateinit var clubInfoCard: View
    private lateinit var sponsorTextView: TextView
    private lateinit var sponsorEmailTextView: TextView
    private lateinit var meetingDayTextView: TextView
    private lateinit var meetingTimeTextView: TextView
    private lateinit var meetingRoomTextView: TextView
    private lateinit var schoologyCodeTextView: TextView

    // AI Assistant fields
    private lateinit var aiAssistantCard: MaterialCardView
    private lateinit var aiSuccessProbabilityButton: MaterialButton
    private lateinit var aiApplicationHelpButton: MaterialButton

    companion object {
        private const val ARG_OPPORTUNITY_ID = "opportunity_id"

        fun newInstance(opportunityId: Long): OpportunityDetailsTabFragment {
            val fragment = OpportunityDetailsTabFragment()
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
        return inflater.inflate(R.layout.fragment_opportunity_details_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        aiManager = AIManager(requireContext())

        initializeViews(view)
        setupAIButtons()
        loadOpportunityDetails()
    }

    private fun initializeViews(view: View) {
        titleTextView = view.findViewById(R.id.titleTextView)
        organizationTextView = view.findViewById(R.id.organizationTextView)
        categoryTextView = view.findViewById(R.id.categoryTextView)
        descriptionTextView = view.findViewById(R.id.descriptionTextView)
        deadlineTextView = view.findViewById(R.id.deadlineTextView)
        costTextView = view.findViewById(R.id.costTextView)
        hoursTextView = view.findViewById(R.id.hoursTextView)
        websiteTextView = view.findViewById(R.id.websiteTextView)

        // Club-specific views
        clubInfoCard = view.findViewById(R.id.clubInfoCard)
        sponsorTextView = view.findViewById(R.id.sponsorTextView)
        sponsorEmailTextView = view.findViewById(R.id.sponsorEmailTextView)
        meetingDayTextView = view.findViewById(R.id.meetingDayTextView)
        meetingTimeTextView = view.findViewById(R.id.meetingTimeTextView)
        meetingRoomTextView = view.findViewById(R.id.meetingRoomTextView)
        schoologyCodeTextView = view.findViewById(R.id.schoologyCodeTextView)

        // AI Assistant views
        aiAssistantCard = view.findViewById(R.id.aiAssistantCard)
        aiSuccessProbabilityButton = view.findViewById(R.id.aiSuccessProbabilityButton)
        aiApplicationHelpButton = view.findViewById(R.id.aiApplicationHelpButton)
    }

    private fun setupAIButtons() {
        // Show AI card only if API key is configured
        if (aiManager.isApiKeyConfigured()) {
            aiAssistantCard.visibility = View.VISIBLE

            aiSuccessProbabilityButton.setOnClickListener {
                (activity as? OpportunityDetailActivity)?.let { detailActivity ->
                    // Call the merged AI Path to Success feature
                    currentOpportunity?.let {
                        try {
                            val method = OpportunityDetailActivity::class.java.getDeclaredMethod("showGuidanceAndChecklist")
                            method.isAccessible = true
                            method.invoke(detailActivity)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            aiApplicationHelpButton.setOnClickListener {
                (activity as? OpportunityDetailActivity)?.let { detailActivity ->
                    currentOpportunity?.let {
                        try {
                            val method = OpportunityDetailActivity::class.java.getDeclaredMethod("showApplicationHelp")
                            method.isAccessible = true
                            method.invoke(detailActivity)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else {
            aiAssistantCard.visibility = View.GONE
        }
    }

    private fun loadOpportunityDetails() {
        database.opportunityDao().getOpportunityById(opportunityId).observe(viewLifecycleOwner) { opportunity ->
            if (opportunity != null) {
                displayDetails(opportunity)
            }
        }
    }

    private fun displayDetails(opportunity: Opportunity) {
        currentOpportunity = opportunity
        titleTextView.text = opportunity.title
        organizationTextView.text = opportunity.organizationName ?: opportunity.type
        categoryTextView.text = formatCategory(opportunity.category.name)
        descriptionTextView.text = opportunity.description

        // Deadline
        if (opportunity.deadline != null) {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            deadlineTextView.text = "Deadline: ${dateFormat.format(Date(opportunity.deadline))}"
            deadlineTextView.visibility = View.VISIBLE
        } else if (opportunity.isRolling) {
            deadlineTextView.text = "Deadline: Rolling admissions"
            deadlineTextView.visibility = View.VISIBLE
        } else {
            deadlineTextView.visibility = View.GONE
        }

        // Cost
        costTextView.text = "Cost: ${opportunity.cost ?: "Not specified"}"

        // Hours
        if (opportunity.hoursPerWeek != null) {
            hoursTextView.text = "Time Commitment: ${opportunity.hoursPerWeek}"
        } else {
            hoursTextView.text = "Time Commitment: Varies"
        }

        // Website
        if (opportunity.website != null) {
            websiteTextView.text = opportunity.website
            websiteTextView.visibility = View.VISIBLE
        } else {
            websiteTextView.visibility = View.GONE
        }

        // Club-specific information
        if (opportunity.category == com.example.cac3.data.model.OpportunityCategory.CLUB) {
            displayClubInfo(opportunity)
        } else {
            clubInfoCard.visibility = View.GONE
        }
    }

    private fun displayClubInfo(opportunity: Opportunity) {
        var hasClubInfo = false

        // Sponsor
        if (!opportunity.sponsor.isNullOrBlank()) {
            sponsorTextView.text = "Sponsor: ${opportunity.sponsor}"
            sponsorTextView.visibility = View.VISIBLE
            hasClubInfo = true
        } else {
            sponsorTextView.visibility = View.GONE
        }

        // Sponsor Email
        if (!opportunity.sponsorEmail.isNullOrBlank()) {
            sponsorEmailTextView.text = "Email: ${opportunity.sponsorEmail}"
            sponsorEmailTextView.visibility = View.VISIBLE
            hasClubInfo = true
        } else {
            sponsorEmailTextView.visibility = View.GONE
        }

        // Meeting Day
        if (!opportunity.meetingDay.isNullOrBlank()) {
            meetingDayTextView.text = "Meeting Day: ${opportunity.meetingDay}"
            meetingDayTextView.visibility = View.VISIBLE
            hasClubInfo = true
        } else {
            meetingDayTextView.visibility = View.GONE
        }

        // Meeting Time
        if (!opportunity.meetingTime.isNullOrBlank()) {
            meetingTimeTextView.text = "Meeting Time: ${opportunity.meetingTime}"
            meetingTimeTextView.visibility = View.VISIBLE
            hasClubInfo = true
        } else {
            meetingTimeTextView.visibility = View.GONE
        }

        // Meeting Room
        if (!opportunity.meetingRoom.isNullOrBlank()) {
            meetingRoomTextView.text = "Room: ${opportunity.meetingRoom}"
            meetingRoomTextView.visibility = View.VISIBLE
            hasClubInfo = true
        } else {
            meetingRoomTextView.visibility = View.GONE
        }

        // Schoology Code
        if (!opportunity.schoologyCode.isNullOrBlank()) {
            schoologyCodeTextView.text = "Schoology Code: ${opportunity.schoologyCode}"
            schoologyCodeTextView.visibility = View.VISIBLE
            hasClubInfo = true
        } else {
            schoologyCodeTextView.visibility = View.GONE
        }

        // Show or hide the entire card based on whether we have any club info
        clubInfoCard.visibility = if (hasClubInfo) View.VISIBLE else View.GONE
    }

    private fun formatCategory(category: String): String {
        return category.replace('_', ' ').lowercase()
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
