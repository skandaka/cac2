package com.example.cac3.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.cac3.R
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.CommitmentStatus
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.UserCommitment
import com.example.cac3.fragments.OpportunityCommentsTabFragment
import com.example.cac3.fragments.OpportunityDetailsTabFragment
import com.example.cac3.util.AuthManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

/**
 * Detailed view of an opportunity with tabs for details and comments
 */
class OpportunityDetailActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var authManager: AuthManager

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var commitmentFab: ExtendedFloatingActionButton

    private var opportunityId: Long = -1
    private var opportunity: Opportunity? = null
    private var hasCommitment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opportunity_detail)

        // Handle edge-to-edge display for camera cutout
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(0, insets.systemWindowInsetTop, 0, 0)
            insets
        }

        database = AppDatabase.getDatabase(this)
        authManager = AuthManager(this)

        opportunityId = intent.getLongExtra("opportunity_id", -1)
        if (opportunityId == -1L) {
            Toast.makeText(this, "Error loading opportunity", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        loadOpportunity()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        commitmentFab = findViewById(R.id.commitmentFab)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadOpportunity() {
        database.opportunityDao().getOpportunityById(opportunityId).observe(this) { opp ->
            if (opp != null) {
                opportunity = opp
                displayOpportunity(opp)
                checkCommitmentStatus()
            } else {
                Toast.makeText(this, "Opportunity not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayOpportunity(opp: Opportunity) {
        toolbar.title = opp.title

        // Setup ViewPager with tabs
        val adapter = OpportunityPagerAdapter(this, opportunityId)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Details"
                1 -> "Comments"
                else -> ""
            }
        }.attach()
    }

    private fun checkCommitmentStatus() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) return

        lifecycleScope.launch {
            val commitment = database.userDao().getCommitment(userId, opportunityId)
            hasCommitment = commitment != null

            updateCommitmentButton()
        }
    }

    private fun updateCommitmentButton() {
        if (hasCommitment) {
            commitmentFab.text = "Remove from Commitments"
            commitmentFab.setIconResource(android.R.drawable.ic_delete)
            commitmentFab.setOnClickListener { removeFromCommitments() }
        } else {
            commitmentFab.text = "Add to My Commitments"
            commitmentFab.setIconResource(android.R.drawable.ic_input_add)
            commitmentFab.setOnClickListener { addToCommitments() }
        }
    }

    private fun addToCommitments() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val commitment = UserCommitment(
                    userId = userId,
                    opportunityId = opportunityId,
                    status = CommitmentStatus.INTERESTED,
                    hoursPerWeek = 0 // User can update this later
                )

                database.userDao().insertCommitment(commitment)

                hasCommitment = true
                updateCommitmentButton()

                Toast.makeText(
                    this@OpportunityDetailActivity,
                    "Added to your commitments!",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@OpportunityDetailActivity,
                    "Error adding commitment",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun removeFromCommitments() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) return

        lifecycleScope.launch {
            try {
                database.userDao().deleteCommitmentByOpportunity(userId, opportunityId)

                hasCommitment = false
                updateCommitmentButton()

                Toast.makeText(
                    this@OpportunityDetailActivity,
                    "Removed from commitments",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@OpportunityDetailActivity,
                    "Error removing commitment",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * ViewPager adapter for opportunity tabs
     */
    private class OpportunityPagerAdapter(
        activity: FragmentActivity,
        private val opportunityId: Long
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OpportunityDetailsTabFragment.newInstance(opportunityId)
                1 -> OpportunityCommentsTabFragment.newInstance(opportunityId)
                else -> Fragment()
            }
        }
    }
}
