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
    private lateinit var advancedFilterButton: com.google.android.material.button.MaterialButton

    private var allOpportunities: List<Opportunity> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentCategory: OpportunityCategory? = null
    private var searchJob: Job? = null

    // Advanced filter state
    private var freeOnly: Boolean = false
    private var virtualOnly: Boolean = false
    private var transitAccessible: Boolean = false
    private var scholarshipAvailable: Boolean = false
    private var minComments: Int = 0
    private var minStudents: Int = 0
    private var sortBy: String = "popular" // popular, commented, deadline, newest

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
        advancedFilterButton = view.findViewById(R.id.advancedFilterButton)

        // Setup FAB for adding opportunities
        val addFab = view.findViewById<ExtendedFloatingActionButton>(R.id.addOpportunityFab)
        addFab.setOnClickListener {
            // Navigate to AddOpportunityFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddOpportunityFragment())
                .addToBackStack(null)
                .commit()
        }

        // Setup advanced filter button
        advancedFilterButton.setOnClickListener {
            showAdvancedFilterDialog()
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
        lifecycleScope.launch {
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

            // Apply advanced filters
            if (freeOnly) {
                filtered = filtered.filter { opp ->
                    (opp.cost?.equals("FREE", ignoreCase = true) == true) ||
                    (opp.costMin == null || opp.costMin == 0.0) ||
                    (opp.cost == null)
                }
            }

            if (virtualOnly) {
                filtered = filtered.filter { it.isVirtual }
            }

            if (transitAccessible) {
                filtered = filtered.filter { it.transitAccessible }
            }

            if (scholarshipAvailable) {
                filtered = filtered.filter { it.scholarshipAvailable }
            }

            // Filter by minimum comments
            if (minComments > 0) {
                val commentCounts = mutableMapOf<Long, Int>()
                for (opp in filtered) {
                    val count = database.commentDao().getCommentCount(opp.id)
                    commentCounts[opp.id] = count
                }
                filtered = filtered.filter { (commentCounts[it.id] ?: 0) >= minComments }
            }

            // Filter by minimum students enrolled
            if (minStudents > 0) {
                val enrollmentCounts = mutableMapOf<Long, Int>()
                for (opp in filtered) {
                    val count = database.userDao().getCommitmentCountForOpportunity(opp.id)
                    enrollmentCounts[opp.id] = count
                }
                filtered = filtered.filter { (enrollmentCounts[it.id] ?: 0) >= minStudents }
            }

            // Apply sorting
            filtered = when (sortBy) {
                "popular" -> {
                    // Sort by number of students enrolled
                    val enrollmentCounts = mutableMapOf<Long, Int>()
                    for (opp in filtered) {
                        enrollmentCounts[opp.id] = database.userDao().getCommitmentCountForOpportunity(opp.id)
                    }
                    filtered.sortedByDescending { enrollmentCounts[it.id] ?: 0 }
                }
                "commented" -> {
                    // Sort by number of comments
                    val commentCounts = mutableMapOf<Long, Int>()
                    for (opp in filtered) {
                        commentCounts[opp.id] = database.commentDao().getCommentCount(opp.id)
                    }
                    filtered.sortedByDescending { commentCounts[it.id] ?: 0 }
                }
                "deadline" -> {
                    filtered.sortedBy { it.deadline ?: Long.MAX_VALUE }
                }
                "newest" -> {
                    filtered.sortedByDescending { it.createdAt }
                }
                else -> filtered
            }

            // Update UI
            updateResults(filtered)
        }
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

    private fun showAdvancedFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_advanced_filters, null)

        // Initialize dialog views
        val categoryChipGroup = dialogView.findViewById<ChipGroup>(R.id.categoryFilterChipGroup)
        val freeOnlySwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.freeOnlySwitch)
        val virtualOnlySwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.virtualOnlySwitch)
        val transitAccessibleSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.transitAccessibleSwitch)
        val scholarshipAvailableSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.scholarshipAvailableSwitch)
        val minCommentsSlider = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.minCommentsSlider)
        val minCommentsValue = dialogView.findViewById<TextView>(R.id.minCommentsValue)
        val minStudentsSlider = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.minStudentsSlider)
        val minStudentsValue = dialogView.findViewById<TextView>(R.id.minStudentsValue)
        val sortByRadioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.sortByRadioGroup)

        // Set current filter state
        freeOnlySwitch.isChecked = freeOnly
        virtualOnlySwitch.isChecked = virtualOnly
        transitAccessibleSwitch.isChecked = transitAccessible
        scholarshipAvailableSwitch.isChecked = scholarshipAvailable
        minCommentsSlider.value = minComments.toFloat()
        minStudentsSlider.value = minStudents.toFloat()

        // Update value displays
        minCommentsSlider.addOnChangeListener { _, value, _ ->
            minCommentsValue.text = value.toInt().toString()
        }
        minStudentsSlider.addOnChangeListener { _, value, _ ->
            minStudentsValue.text = "${value.toInt()}+"
        }

        // Set current sort option
        when (sortBy) {
            "popular" -> sortByRadioGroup.check(R.id.sortMostPopular)
            "commented" -> sortByRadioGroup.check(R.id.sortMostCommented)
            "deadline" -> sortByRadioGroup.check(R.id.sortDeadline)
            "newest" -> sortByRadioGroup.check(R.id.sortNewest)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.filter_title))
            .setView(dialogView)
            .create()

        // Apply filters button
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.applyFiltersButton).setOnClickListener {
            // Get selected category
            when (categoryChipGroup.checkedChipId) {
                R.id.chipFilterCompetition -> currentCategory = OpportunityCategory.COMPETITION
                R.id.chipFilterEmployment -> currentCategory = OpportunityCategory.EMPLOYMENT
                R.id.chipFilterVolunteering -> currentCategory = OpportunityCategory.VOLUNTEERING
                R.id.chipFilterClub -> currentCategory = OpportunityCategory.CLUB
                R.id.chipFilterSummer -> currentCategory = OpportunityCategory.SUMMER_PROGRAM
                else -> currentCategory = null
            }

            // Get filter states
            freeOnly = freeOnlySwitch.isChecked
            virtualOnly = virtualOnlySwitch.isChecked
            transitAccessible = transitAccessibleSwitch.isChecked
            scholarshipAvailable = scholarshipAvailableSwitch.isChecked
            minComments = minCommentsSlider.value.toInt()
            minStudents = minStudentsSlider.value.toInt()

            // Get sort option
            sortBy = when (sortByRadioGroup.checkedRadioButtonId) {
                R.id.sortMostPopular -> "popular"
                R.id.sortMostCommented -> "commented"
                R.id.sortDeadline -> "deadline"
                R.id.sortNewest -> "newest"
                else -> "popular"
            }

            filterOpportunities()
            dialog.dismiss()
        }

        // Clear filters button
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.clearFiltersButton).setOnClickListener {
            currentCategory = null
            freeOnly = false
            virtualOnly = false
            transitAccessible = false
            scholarshipAvailable = false
            minComments = 0
            minStudents = 0
            sortBy = "popular"
            filterOpportunities()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        // Reload opportunities when fragment becomes visible
        loadOpportunities()
    }
}
