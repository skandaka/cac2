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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import com.example.cac3.data.model.OpportunityCategory
import com.example.cac3.data.model.Team
import com.example.cac3.data.model.TeamRequest
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

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_opportunity_detail)

        // Handle camera cutout and system bars
        val rootView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
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
            R.id.action_get_guidance -> {
                showGuidanceAndChecklist()
                true
            }
            R.id.action_application_help -> {
                showApplicationHelp()
                true
            }
            R.id.action_predict_deadline -> {
                showPredictDeadline()
                true
            }
            R.id.action_export_pdf -> {
                exportCommitmentsToPDF()
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
                    else -> {
                        // Provide reasonable defaults based on category when hours not specified
                        when (currentOpp?.category) {
                            OpportunityCategory.CLUB -> 2
                            OpportunityCategory.HONOR_SOCIETY -> 1
                            OpportunityCategory.VOLUNTEERING -> 3
                            OpportunityCategory.EMPLOYMENT -> 10
                            OpportunityCategory.INTERNSHIP -> 15
                            OpportunityCategory.COMPETITION -> 5
                            OpportunityCategory.SUMMER_PROGRAM -> 30
                            OpportunityCategory.TEST_PREP -> 4
                            else -> 3 // General default
                        }
                    }
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

    private fun showGuidanceAndChecklist() {
        val currentOpp = opportunity ?: return
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.ai_guidance_title))
            .setMessage(getString(R.string.ai_loading))
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                // Get user profile
                val user = database.userDao().getUserById(userId)
                if (user == null) {
                    loadingDialog.dismiss()
                    // Show helpful message instead of just "User not found"
                    AlertDialog.Builder(this@OpportunityDetailActivity)
                        .setTitle(getString(R.string.error))
                        .setMessage("Please complete your profile first to get personalized guidance.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                // Get user's current activities
                val commitments = database.userDao().getUserCommitmentsSync(userId)

                // Check if user has enough history for accurate analysis
                if (commitments.size < 2) {
                    loadingDialog.dismiss()
                    AlertDialog.Builder(this@OpportunityDetailActivity)
                        .setTitle(getString(R.string.ai_guidance_title))
                        .setMessage(getString(R.string.ai_not_enough_data))
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                val activities = mutableListOf<String>()
                for (commitment in commitments) {
                    val opp = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
                    if (opp != null) {
                        activities.add("${opp.title} (${opp.category})")
                    }
                }

                // Get both guidance and checklist
                val guidanceResult = aiManager.calculateSuccessProbability(user, currentOpp, activities)
                val checklistResult = aiManager.generateApplicationChecklist(currentOpp)

                loadingDialog.dismiss()

                // Check if both succeeded
                val guidance = guidanceResult.getOrNull()
                val checklist = checklistResult.getOrNull()

                if (guidance != null || checklist != null) {
                    showGuidanceAndChecklistDialog(guidance, checklist)
                } else {
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        getString(R.string.ai_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(
                    this@OpportunityDetailActivity,
                    getString(R.string.ai_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showGuidanceAndChecklistDialog(
        guidance: com.example.cac3.ai.SuccessProbability?,
        checklist: List<ChecklistItem>?
    ) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        // Show guidance section (positive framing)
        if (guidance != null) {
            // Your Strengths
            if (guidance.strengths.isNotEmpty()) {
                val strengthsTitle = TextView(this).apply {
                    text = "✓ Your Strengths:"
                    textSize = 18f
                    setTextColor(getColor(R.color.success))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 10, 0, 10)
                }
                dialogView.addView(strengthsTitle)

                guidance.strengths.forEach { strength ->
                    val strengthItem = TextView(this).apply {
                        text = "• $strength"
                        textSize = 14f
                        setPadding(20, 5, 0, 5)
                    }
                    dialogView.addView(strengthItem)
                }
            }

            // How to Improve Your Chances
            if (guidance.recommendations.isNotEmpty()) {
                val recsTitle = TextView(this).apply {
                    text = "→ How to Improve Your Chances:"
                    textSize = 18f
                    setTextColor(getColor(R.color.primary))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 20, 0, 10)
                }
                dialogView.addView(recsTitle)

                guidance.recommendations.forEach { rec ->
                    val recItem = TextView(this).apply {
                        text = "• $rec"
                        textSize = 14f
                        setPadding(20, 5, 0, 5)
                    }
                    dialogView.addView(recItem)
                }
            }
        }

        // Show checklist section
        if (checklist != null && checklist.isNotEmpty()) {
            val checklistTitle = TextView(this).apply {
                text = "☑ Application Checklist:"
                textSize = 18f
                setTextColor(getColor(R.color.info))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 20, 0, 10)
            }
            dialogView.addView(checklistTitle)

            checklist.take(5).forEach { item ->
                val taskText = TextView(this).apply {
                    text = "• ${item.task}"
                    textSize = 14f
                    setPadding(20, 5, 0, 2)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                dialogView.addView(taskText)

                val detailsText = TextView(this).apply {
                    text = "  ${item.description}"
                    textSize = 12f
                    setTextColor(getColor(R.color.text_secondary))
                    setPadding(40, 2, 0, 8)
                }
                dialogView.addView(detailsText)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.ai_guidance_title))
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
                val allOpportunities = database.opportunityDao().getAllOpportunitiesSync()
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

    private fun exportCommitmentsToPDF() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.export_pdf))
            .setMessage(getString(R.string.export_generating))
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val user = database.userDao().getUserById(userId)
                val commitments = database.userDao().getUserCommitmentsSync(userId)

                if (commitments.isEmpty()) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        "No commitments to export",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Create PDF
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageWidth = 595 // A4 width in points
                val pageHeight = 842 // A4 height in points

                var pageNumber = 1
                var yPosition = 100f
                val lineHeight = 20f
                val margin = 50f

                var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas
                val paint = android.graphics.Paint()

                // Title
                paint.textSize = 24f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText(getString(R.string.export_title), margin, yPosition, paint)
                yPosition += 40f

                // User info
                paint.textSize = 14f
                paint.typeface = android.graphics.Typeface.DEFAULT
                if (user != null) {
                    canvas.drawText("Student: ${user.fullName}", margin, yPosition, paint)
                    yPosition += lineHeight
                    canvas.drawText("Grade: ${user.grade}", margin, yPosition, paint)
                    yPosition += lineHeight
                    if (user.gpa != null) {
                        canvas.drawText("GPA: ${user.gpa}", margin, yPosition, paint)
                        yPosition += lineHeight
                    }
                }
                yPosition += 20f

                // Total hours
                val totalHours = commitments.sumOf { it.hoursPerWeek }
                paint.textSize = 16f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("Total Time Commitment: $totalHours hrs/week", margin, yPosition, paint)
                yPosition += 30f

                // Commitments list
                paint.textSize = 14f
                paint.typeface = android.graphics.Typeface.DEFAULT

                for ((index, commitment) in commitments.withIndex()) {
                    val opp = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
                    if (opp != null) {
                        // Check if we need a new page
                        if (yPosition > pageHeight - 150) {
                            pdfDocument.finishPage(page)
                            pageNumber++
                            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            yPosition = 50f
                        }

                        // Opportunity title
                        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        canvas.drawText("${index + 1}. ${opp.title}", margin, yPosition, paint)
                        yPosition += lineHeight

                        // Category
                        paint.typeface = android.graphics.Typeface.DEFAULT
                        canvas.drawText("   Category: ${opp.category}", margin, yPosition, paint)
                        yPosition += lineHeight

                        // Time commitment
                        canvas.drawText("   Time: ${commitment.hoursPerWeek} hrs/week", margin, yPosition, paint)
                        yPosition += lineHeight

                        // Dates if available
                        if (commitment.startDate != null || commitment.endDate != null) {
                            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                            val dateStr = if (commitment.startDate != null && commitment.endDate != null) {
                                "${dateFormat.format(java.util.Date(commitment.startDate!!))} - ${dateFormat.format(java.util.Date(commitment.endDate!!))}"
                            } else if (commitment.startDate != null) {
                                "Starts: ${dateFormat.format(java.util.Date(commitment.startDate!!))}"
                            } else {
                                "Ends: ${dateFormat.format(java.util.Date(commitment.endDate!!))}"
                            }
                            canvas.drawText("   Duration: $dateStr", margin, yPosition, paint)
                            yPosition += lineHeight
                        }

                        // Status
                        canvas.drawText("   Status: ${commitment.status}", margin, yPosition, paint)
                        yPosition += lineHeight

                        // Organization if available
                        if (!opp.organizationName.isNullOrEmpty()) {
                            canvas.drawText("   Organization: ${opp.organizationName}", margin, yPosition, paint)
                            yPosition += lineHeight
                        }

                        yPosition += 10f
                    }
                }

                pdfDocument.finishPage(page)

                // Save PDF
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val fileName = "OpportunityPortfolio_${System.currentTimeMillis()}.pdf"
                val file = java.io.File(downloadsDir, fileName)

                try {
                    pdfDocument.writeTo(java.io.FileOutputStream(file))
                    loadingDialog.dismiss()

                    AlertDialog.Builder(this@OpportunityDetailActivity)
                        .setTitle(getString(R.string.export_success))
                        .setMessage("PDF saved to Downloads/$fileName")
                        .setPositiveButton("OK", null)
                        .show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        getString(R.string.export_error),
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    pdfDocument.close()
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(
                    this@OpportunityDetailActivity,
                    getString(R.string.export_error),
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
