package com.example.cac3.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.cac3.adapter.OpportunityAdapter
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.OpportunityCategory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Browse Fragment - Search and filter opportunities
 */
class BrowseFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: OpportunityAdapter

    private lateinit var searchInput: TextInputEditText
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var resultsCountTextView: TextView
    private lateinit var emptyStateLayout: View

    private var allOpportunities: List<Opportunity> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentCategory: OpportunityCategory? = null
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        initializeViews(view)
        setupRecyclerView()
        setupSearch()
        setupCategoryFilters()
        loadOpportunities()
    }

    private fun initializeViews(view: View) {
        searchInput = view.findViewById(R.id.searchInput)
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup)
        recyclerView = view.findViewById(R.id.opportunitiesRecyclerView)
        resultsCountTextView = view.findViewById(R.id.resultsCountTextView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)

        // Setup FAB for adding opportunities
        val addFab = view.findViewById<ExtendedFloatingActionButton>(R.id.addOpportunityFab)
        addFab.setOnClickListener {
            // Navigate to AddOpportunityFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddOpportunityFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerView() {
        adapter = OpportunityAdapter { opportunity ->
            val intent = Intent(requireContext(), OpportunityDetailActivity::class.java)
            intent.putExtra("opportunity_id", opportunity.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                currentSearchQuery = query

                // Debounce search to avoid too many updates
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Wait 300ms after user stops typing
                    filterOpportunities()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupCategoryFilters() {
        categoryChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipCompetition) -> currentCategory = OpportunityCategory.COMPETITION
                checkedIds.contains(R.id.chipEmployment) -> currentCategory = OpportunityCategory.EMPLOYMENT
                checkedIds.contains(R.id.chipVolunteering) -> currentCategory = OpportunityCategory.VOLUNTEERING
                checkedIds.contains(R.id.chipClub) -> currentCategory = OpportunityCategory.CLUB
                checkedIds.contains(R.id.chipCollege) -> currentCategory = OpportunityCategory.COLLEGE
                checkedIds.contains(R.id.chipSummer) -> currentCategory = OpportunityCategory.SUMMER_PROGRAM
                else -> currentCategory = null // "All" selected or nothing
            }
            filterOpportunities()
        }
    }

    private fun loadOpportunities() {
        database.opportunityDao().getAllOpportunities().observe(viewLifecycleOwner) { opportunities ->
            allOpportunities = opportunities
            filterOpportunities()
        }
    }

    private fun filterOpportunities() {
        var filtered = allOpportunities

        // Apply category filter
        if (currentCategory != null) {
            filtered = filtered.filter { it.category == currentCategory }
        }

        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            val query = currentSearchQuery.lowercase()
            filtered = filtered.filter { opportunity ->
                opportunity.title.lowercase().contains(query) ||
                opportunity.description.lowercase().contains(query) ||
                (opportunity.organizationName?.lowercase()?.contains(query) == true) ||
                (opportunity.tags?.lowercase()?.contains(query) == true) ||
                opportunity.type.lowercase().contains(query)
            }
        }

        // Update UI
        updateResults(filtered)
    }

    private fun updateResults(opportunities: List<Opportunity>) {
        if (opportunities.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
            resultsCountTextView.text = "No opportunities found"
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE

            val categoryText = currentCategory?.let { " in ${formatCategory(it)}" } ?: ""
            val searchText = if (currentSearchQuery.isNotEmpty()) {
                " matching \"$currentSearchQuery\""
            } else {
                ""
            }
            resultsCountTextView.text = "${opportunities.size} opportunities$categoryText$searchText"

            adapter.submitList(opportunities)
        }
    }

    private fun formatCategory(category: OpportunityCategory): String {
        return category.name.replace('_', ' ').lowercase()
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    override fun onResume() {
        super.onResume()
        // Reload opportunities when fragment becomes visible
        loadOpportunities()
    }
}
