package com.example.cac3.activities

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.cac3.R
import com.example.cac3.ai.AIManager
import com.example.cac3.ai.ChecklistItem
import com.example.cac3.ai.DeadlinePrediction
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.CommitmentStatus
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.Team
import com.example.cac3.data.model.TeamRequest
import com.example.cac3.data.model.UserCommitment
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
    private lateinit var aiManager: AIManager

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
        aiManager = AIManager(this)

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Only show AI menu if API key is configured
        if (aiManager.isApiKeyConfigured()) {
            menuInflater.inflate(R.menu.opportunity_detail_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_success_probability -> {
                showSuccessProbability()
                true
            }
            R.id.action_application_help -> {
                showApplicationHelp()
                true
            }
            R.id.action_generate_checklist -> {
                showGenerateChecklist()
                true
            }
            R.id.action_predict_deadline -> {
                showPredictDeadline()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                // Calculate hours per week from opportunity data
                val currentOpp = opportunity
                val hours = when {
                    currentOpp?.hoursPerWeekMin != null && currentOpp.hoursPerWeekMax != null -> {
                        // Use average of min and max
                        (currentOpp.hoursPerWeekMin + currentOpp.hoursPerWeekMax) / 2
                    }
                    currentOpp?.hoursPerWeekMin != null -> {
                        currentOpp.hoursPerWeekMin
                    }
                    currentOpp?.hoursPerWeekMax != null -> {
                        currentOpp.hoursPerWeekMax
                    }
                    else -> 0 // Default if no hours specified
                }

                val commitment = UserCommitment(
                    userId = userId,
                    opportunityId = opportunityId,
                    status = CommitmentStatus.INTERESTED,
                    hoursPerWeek = hours,
                    startDate = currentOpp?.startDate,
                    endDate = currentOpp?.endDate
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

    private fun showSuccessProbability() {
        val currentOpp = opportunity ?: return
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Calculating Success Probability")
            .setMessage("Analyzing your profile and this opportunity...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val user = database.userDao().getUserById(userId)
                if (user == null) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@OpportunityDetailActivity, "User not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Get user's current activities
                val commitments = database.userDao().getUserCommitmentsSync(userId)
                val activities = mutableListOf<String>()
                for (commitment in commitments) {
                    val opp = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
                    if (opp != null) {
                        activities.add("${opp.title} (${opp.category})")
                    }
                }

                val result = aiManager.calculateSuccessProbability(user, currentOpp, activities)

                loadingDialog.dismiss()

                result.onSuccess { probability ->
                    showSuccessProbabilityDialog(probability)
                }.onFailure { error ->
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(
                    this@OpportunityDetailActivity,
                    "Error calculating probability",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showSuccessProbabilityDialog(probability: com.example.cac3.ai.SuccessProbability) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        // Probability score
        val scoreText = TextView(this).apply {
            text = "Success Probability: ${probability.probability}%"
            textSize = 20f
            setTextColor(getColor(R.color.primary))
            setPadding(0, 0, 0, 20)
        }
        dialogView.addView(scoreText)

        // Confidence
        val confidenceText = TextView(this).apply {
            text = "Confidence: ${probability.confidence.uppercase()}"
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }
        dialogView.addView(confidenceText)

        // Strengths
        if (probability.strengths.isNotEmpty()) {
            val strengthsTitle = TextView(this).apply {
                text = "Strengths:"
                textSize = 16f
                setTextColor(getColor(R.color.success))
                setPadding(0, 10, 0, 5)
            }
            dialogView.addView(strengthsTitle)

            probability.strengths.forEach { strength ->
                val strengthItem = TextView(this).apply {
                    text = "• $strength"
                    textSize = 14f
                    setPadding(20, 5, 0, 5)
                }
                dialogView.addView(strengthItem)
            }
        }

        // Weaknesses
        if (probability.weaknesses.isNotEmpty()) {
            val weaknessesTitle = TextView(this).apply {
                text = "Areas to Improve:"
                textSize = 16f
                setTextColor(getColor(R.color.warning))
                setPadding(0, 15, 0, 5)
            }
            dialogView.addView(weaknessesTitle)

            probability.weaknesses.forEach { weakness ->
                val weaknessItem = TextView(this).apply {
                    text = "• $weakness"
                    textSize = 14f
                    setPadding(20, 5, 0, 5)
                }
                dialogView.addView(weaknessItem)
            }
        }

        // Recommendations
        if (probability.recommendations.isNotEmpty()) {
            val recsTitle = TextView(this).apply {
                text = "Recommendations:"
                textSize = 16f
                setTextColor(getColor(R.color.info))
                setPadding(0, 15, 0, 5)
            }
            dialogView.addView(recsTitle)

            probability.recommendations.forEach { rec ->
                val recItem = TextView(this).apply {
                    text = "• $rec"
                    textSize = 14f
                    setPadding(20, 5, 0, 5)
                }
                dialogView.addView(recItem)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Success Analysis")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showApplicationHelp() {
        val currentOpp = opportunity ?: return
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Input dialog for user's question
        val input = EditText(this).apply {
            hint = "What do you need help with?"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 5
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("AI Application Assistant")
            .setMessage("Ask for help with your application, essay, or any questions about this opportunity.")
            .setView(input)
            .setPositiveButton("Get Help") { _, _ ->
                val question = input.text.toString().trim()
                if (question.isNotEmpty()) {
                    getAIApplicationHelp(currentOpp, question)
                } else {
                    Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getAIApplicationHelp(opp: Opportunity, question: String) {
        val userId = authManager.getCurrentUserId()

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Getting AI Assistance")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val user = database.userDao().getUserById(userId)
                val userProfile = if (user != null) {
                    """
                    Name: ${user.fullName}
                    Grade: ${user.grade}
                    Interests: ${user.interests}
                    GPA: ${user.gpa ?: "Not specified"}
                    """.trimIndent()
                } else {
                    "High school student"
                }

                val result = aiManager.generateApplicationHelp(opp, userProfile, question)

                loadingDialog.dismiss()

                result.onSuccess { response ->
                    showApplicationHelpResponse(response)
                }.onFailure { error ->
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(
                    this@OpportunityDetailActivity,
                    "Error getting AI help",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showApplicationHelpResponse(response: String) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val responseText = TextView(this).apply {
            text = response
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        dialogView.addView(responseText)

        AlertDialog.Builder(this)
            .setTitle("AI Application Assistance")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .setNeutralButton("Ask Another Question") { _, _ ->
                showApplicationHelp()
            }
            .show()
    }

    private fun showGenerateChecklist() {
        val currentOpp = opportunity ?: return

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Generating Checklist")
            .setMessage("Creating your personalized application checklist...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val result = aiManager.generateApplicationChecklist(currentOpp)

                loadingDialog.dismiss()

                result.onSuccess { checklist ->
                    showChecklistDialog(checklist)
                }.onFailure { error ->
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(
                    this@OpportunityDetailActivity,
                    "Error generating checklist",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showChecklistDialog(checklist: List<ChecklistItem>) {
        if (checklist.isEmpty()) {
            Toast.makeText(this, "No checklist items generated", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val headerText = TextView(this).apply {
            text = "Application Checklist"
            textSize = 18f
            setTextColor(getColor(R.color.primary))
            setPadding(0, 0, 0, 20)
        }
        dialogView.addView(headerText)

        checklist.forEachIndexed { index, item ->
            // Task number and priority
            val taskHeader = TextView(this).apply {
                text = "${index + 1}. ${item.task} [${item.priority.uppercase()}]"
                textSize = 16f
                setTextColor(when (item.priority) {
                    "high" -> getColor(R.color.error)
                    "medium" -> getColor(R.color.warning)
                    else -> getColor(R.color.info)
                })
                setPadding(0, 15, 0, 5)
            }
            dialogView.addView(taskHeader)

            // Description
            val descText = TextView(this).apply {
                text = item.description
                textSize = 14f
                setPadding(20, 5, 0, 5)
            }
            dialogView.addView(descText)

            // Time and deadline info
            val infoText = TextView(this).apply {
                text = "Estimated: ${item.estimatedHours} hours • Start ${item.daysBeforeDeadline} days before deadline"
                textSize = 12f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(20, 5, 0, 10)
            }
            dialogView.addView(infoText)
        }

        AlertDialog.Builder(this)
            .setTitle("Application Checklist")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPredictDeadline() {
        val currentOpp = opportunity ?: return

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Predicting Deadline")
            .setMessage("Analyzing similar opportunities and typical cycles...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                // Get similar opportunities (same category)
                val allOpportunities = database.opportunityDao().getAllOpportunities().value ?: emptyList()
                val similarOpportunities = allOpportunities.filter {
                    it.category == currentOpp.category && it.id != currentOpp.id
                }

                val result = aiManager.predictDeadline(currentOpp, similarOpportunities)

                loadingDialog.dismiss()

                result.onSuccess { prediction ->
                    showDeadlinePredictionDialog(prediction)
                }.onFailure { error ->
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(
                    this@OpportunityDetailActivity,
                    "Error predicting deadline",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeadlinePredictionDialog(prediction: DeadlinePrediction) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        // Prediction
        val predictionText = TextView(this).apply {
            text = "Predicted Deadline: ${prediction.predictedMonth} ${prediction.predictedYear}"
            textSize = 18f
            setTextColor(getColor(R.color.primary))
            setPadding(0, 0, 0, 15)
        }
        dialogView.addView(predictionText)

        // Confidence
        val confidenceText = TextView(this).apply {
            text = "Confidence: ${prediction.confidence.uppercase()}"
            textSize = 14f
            setTextColor(when (prediction.confidence) {
                "high" -> getColor(R.color.success)
                "medium" -> getColor(R.color.warning)
                else -> getColor(R.color.error)
            })
            setPadding(0, 0, 0, 20)
        }
        dialogView.addView(confidenceText)

        // Reasoning
        val reasoningTitle = TextView(this).apply {
            text = "Reasoning:"
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, 10, 0, 5)
        }
        dialogView.addView(reasoningTitle)

        val reasoningText = TextView(this).apply {
            text = prediction.reasoning
            textSize = 14f
            setPadding(0, 5, 0, 20)
        }
        dialogView.addView(reasoningText)

        // Suggested check date
        val checkDateTitle = TextView(this).apply {
            text = "Suggested Check Date:"
            textSize = 16f
            setTextColor(getColor(R.color.info))
            setPadding(0, 10, 0, 5)
        }
        dialogView.addView(checkDateTitle)

        val checkDateText = TextView(this).apply {
            text = prediction.suggestedCheckDate
            textSize = 14f
            setPadding(0, 5, 0, 10)
        }
        dialogView.addView(checkDateText)

        AlertDialog.Builder(this)
            .setTitle("Deadline Prediction")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
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

        override fun getItemCount(): Int = 1

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OpportunityDetailsTabFragment.newInstance(opportunityId)
                else -> Fragment()
            }
        }
    }
}
