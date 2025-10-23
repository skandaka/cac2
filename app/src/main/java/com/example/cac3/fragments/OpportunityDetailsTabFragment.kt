package com.example.cac3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cac3.R
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.Opportunity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment displaying detailed information about an opportunity
 */
class OpportunityDetailsTabFragment : Fragment() {

    private lateinit var database: AppDatabase
    private var opportunityId: Long = -1

    private lateinit var titleTextView: TextView
    private lateinit var organizationTextView: TextView
    private lateinit var categoryTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var deadlineTextView: TextView
    private lateinit var costTextView: TextView
    private lateinit var hoursTextView: TextView
    private lateinit var websiteTextView: TextView

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

        initializeViews(view)
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
    }

    private fun loadOpportunityDetails() {
        database.opportunityDao().getOpportunityById(opportunityId).observe(viewLifecycleOwner) { opportunity ->
            if (opportunity != null) {
                displayDetails(opportunity)
            }
        }
    }

    private fun displayDetails(opportunity: Opportunity) {
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
    }

    private fun formatCategory(category: String): String {
        return category.replace('_', ' ').lowercase()
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
