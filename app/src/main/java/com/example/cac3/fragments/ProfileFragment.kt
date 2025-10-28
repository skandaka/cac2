package com.example.cac3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cac3.R
import com.example.cac3.activities.LoginActivity
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.cac3.ai.AIManager
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.InterestCategory
import com.example.cac3.util.AuthManager
import com.example.cac3.util.PortfolioGenerator
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/**
 * Profile Fragment - Display user profile and stats
 */
class ProfileFragment : Fragment() {

    private lateinit var authManager: AuthManager
    private lateinit var database: AppDatabase
    private lateinit var aiManager: AIManager
    private lateinit var portfolioGenerator: PortfolioGenerator

    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userGradeTextView: TextView
    private lateinit var totalCommitmentsTextView: TextView
    private lateinit var totalHoursTextView: TextView
    private lateinit var interestsChipGroup: ChipGroup
    private lateinit var generatePdfButton: MaterialButton
    private lateinit var generateTextButton: MaterialButton
    private lateinit var apiKeyStatusTextView: TextView
    private lateinit var configureAIButton: MaterialButton
    private lateinit var viewNotesButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager(requireContext())
        database = AppDatabase.getDatabase(requireContext())
        aiManager = AIManager(requireContext())
        portfolioGenerator = PortfolioGenerator(requireContext())

        initializeViews(view)
        loadUserProfile()
        setupPortfolioButtons()
        updateAIStatus()
        setupConfigureAIButton()
        setupViewNotesButton()
        setupLogoutButton()
    }

    private fun initializeViews(view: View) {
        userNameTextView = view.findViewById(R.id.userNameTextView)
        userEmailTextView = view.findViewById(R.id.userEmailTextView)
        userGradeTextView = view.findViewById(R.id.userGradeTextView)
        totalCommitmentsTextView = view.findViewById(R.id.totalCommitmentsTextView)
        totalHoursTextView = view.findViewById(R.id.totalHoursTextView)
        interestsChipGroup = view.findViewById(R.id.interestsChipGroup)
        generatePdfButton = view.findViewById(R.id.generatePdfButton)
        generateTextButton = view.findViewById(R.id.generateTextButton)
        apiKeyStatusTextView = view.findViewById(R.id.apiKeyStatusTextView)
        configureAIButton = view.findViewById(R.id.configureAIButton)
        viewNotesButton = view.findViewById(R.id.viewNotesButton)
        logoutButton = view.findViewById(R.id.logoutButton)
    }

    private fun setupPortfolioButtons() {
        generatePdfButton.setOnClickListener {
            generatePortfolioPDF()
        }

        generateTextButton.setOnClickListener {
            generatePortfolioText()
        }
    }

    private fun generatePortfolioPDF() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) return

        // Show loading
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Generating Portfolio")
            .setMessage("Creating your PDF resume...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val user = database.userDao().getUserById(userId)
                if (user == null) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                    // Clear invalid session and redirect to login
                    authManager.logout()
                    val intent = Intent(requireContext(), com.example.cac3.activities.LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                    return@launch
                }

                // Get all commitments with opportunities
                val commitments = database.userDao().getUserCommitmentsSync(userId)
                val activities = mutableListOf<PortfolioGenerator.ActivityEntry>()

                for (commitment in commitments) {
                    val opportunity = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
                    if (opportunity != null) {
                        activities.add(PortfolioGenerator.ActivityEntry(commitment, opportunity))
                    }
                }

                if (activities.isEmpty()) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Add some activities to your commitments first",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Generate PDF
                val result = portfolioGenerator.generatePortfolio(user, activities)

                loadingDialog.dismiss()

                result.onSuccess { file ->
                    // Share the PDF
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Activities Resume - ${user.fullName}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(shareIntent, "Share Portfolio"))

                    Toast.makeText(
                        requireContext(),
                        "PDF generated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                }.onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        "Error generating PDF: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                loadingDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error generating portfolio",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun generatePortfolioText() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) return

        lifecycleScope.launch {
            try {
                val user = database.userDao().getUserById(userId)
                if (user == null) {
                    Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                    // Clear invalid session and redirect to login
                    authManager.logout()
                    val intent = Intent(requireContext(), com.example.cac3.activities.LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                    return@launch
                }

                // Get all commitments with opportunities
                val commitments = database.userDao().getUserCommitmentsSync(userId)
                val activities = mutableListOf<PortfolioGenerator.ActivityEntry>()

                for (commitment in commitments) {
                    val opportunity = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
                    if (opportunity != null) {
                        activities.add(PortfolioGenerator.ActivityEntry(commitment, opportunity))
                    }
                }

                if (activities.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Add some activities to your commitments first",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Generate text
                val portfolioText = portfolioGenerator.generateTextPortfolio(user, activities)

                // Share as text
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, portfolioText)
                    putExtra(Intent.EXTRA_SUBJECT, "Activities Resume - ${user.fullName}")
                }

                startActivity(Intent.createChooser(shareIntent, "Share Portfolio"))

                Toast.makeText(
                    requireContext(),
                    "Text portfolio generated!",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error generating portfolio",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadUserProfile() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            return
        }

        // Load basic user info from AuthManager
        userNameTextView.text = authManager.getCurrentUserName() ?: "Student"
        userEmailTextView.text = authManager.getCurrentUserEmail() ?: ""
        val grade = authManager.getCurrentUserGrade()
        userGradeTextView.text = "Grade $grade"

        // Load interests
        val interests = authManager.getCurrentUserInterests() ?: ""
        displayInterests(interests)

        // Load stats
        loadStats(userId)
    }

    private fun displayInterests(interests: String) {
        interestsChipGroup.removeAllViews()

        if (interests.isEmpty()) {
            val chip = Chip(requireContext()).apply {
                text = "No interests selected"
                isClickable = false
                isCheckable = false
            }
            interestsChipGroup.addView(chip)
            return
        }

        val interestList = interests.split(",").map { it.trim() }
        for (interest in interestList) {
            val chip = Chip(requireContext()).apply {
                text = interest
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.primary)
                setTextColor(resources.getColor(android.R.color.white, null))
            }
            interestsChipGroup.addView(chip)
        }
    }

    private fun loadStats(userId: Long) {
        lifecycleScope.launch {
            try {
                // Get commitment count
                val commitmentCount = database.userDao().getCommitmentCount(userId)
                totalCommitmentsTextView.text = commitmentCount.toString()

                // Get total hours per week
                val totalHours = database.userDao().getTotalHoursPerWeek(userId) ?: 0
                totalHoursTextView.text = totalHours.toString()

            } catch (e: Exception) {
                e.printStackTrace()
                totalCommitmentsTextView.text = "0"
                totalHoursTextView.text = "0"
            }
        }
    }

    private fun updateAIStatus() {
        if (aiManager.isApiKeyConfigured()) {
            val key = aiManager.getApiKey()
            val maskedKey = "sk-..." + key.takeLast(4)
            apiKeyStatusTextView.text = "Status: Configured ($maskedKey)"
            apiKeyStatusTextView.setTextColor(resources.getColor(R.color.success, null))
        } else {
            apiKeyStatusTextView.text = "Status: Not configured"
            apiKeyStatusTextView.setTextColor(resources.getColor(R.color.text_secondary, null))
        }
    }

    private fun setupConfigureAIButton() {
        configureAIButton.setOnClickListener {
            showAPIKeyDialog()
        }
    }

    private fun setupViewNotesButton() {
        viewNotesButton.setOnClickListener {
            showNotesDialog()
        }
    }

    private fun showNotesDialog() {
        val prefs = requireContext().getSharedPreferences("ai_notes", android.content.Context.MODE_PRIVATE)
        val allNotes = prefs.all.entries
            .filter { it.key.startsWith("note_") }
            .mapNotNull { entry ->
                val parts = (entry.value as? String)?.split("|")
                if (parts != null && parts.size >= 4) {
                    AINote(
                        id = entry.key,
                        opportunityTitle = parts[0],
                        noteType = parts[1],
                        content = parts[2],
                        timestamp = parts[3].toLongOrNull() ?: 0L
                    )
                } else null
            }
            .sortedByDescending { it.timestamp }

        if (allNotes.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Notes Yet")
                .setMessage("You haven't saved any AI responses yet. Use the AI features and click 'Save to Notes' to build your collection!")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Create notes list view
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_1, null)

        val scrollView = android.widget.ScrollView(requireContext())
        val notesContainer = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
        }

        // Header
        val headerText = android.widget.TextView(requireContext()).apply {
            text = "📝 My AI Notes (${allNotes.size})"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(R.color.text_primary, null))
            setPadding(0, 0, 0, 16)
        }
        notesContainer.addView(headerText)

        // Display each note
        allNotes.forEach { note ->
            val noteCard = createNoteCard(note, prefs)
            notesContainer.addView(noteCard)
        }

        scrollView.addView(notesContainer)

        AlertDialog.Builder(requireContext())
            .setTitle("My AI Notes")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Clear All") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Clear All Notes?")
                    .setMessage("This will permanently delete all ${allNotes.size} saved notes. This cannot be undone.")
                    .setPositiveButton("Delete All") { _, _ ->
                        prefs.edit().clear().apply()
                        Toast.makeText(requireContext(), "All notes deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .show()
    }

    private fun createNoteCard(note: AINote, prefs: android.content.SharedPreferences): View {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(resources.getColor(R.color.surface, null))
        }

        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Note type badge
        val typeBadge = android.widget.TextView(requireContext()).apply {
            text = note.noteType
            textSize = 10f
            setTextColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(resources.getColor(R.color.accent, null))
                cornerRadius = 12f
            }
            setPadding(12, 4, 12, 4)
        }
        container.addView(typeBadge)

        // Opportunity title
        val oppTitle = android.widget.TextView(requireContext()).apply {
            text = note.opportunityTitle
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(R.color.text_primary, null))
            setPadding(0, 8, 0, 4)
        }
        container.addView(oppTitle)

        // Timestamp
        val timestamp = android.widget.TextView(requireContext()).apply {
            text = formatTimestamp(note.timestamp)
            textSize = 11f
            setTextColor(resources.getColor(R.color.text_hint, null))
            setPadding(0, 0, 0, 8)
        }
        container.addView(timestamp)

        // Content preview (first 150 chars)
        val contentPreview = note.content.take(150) + if (note.content.length > 150) "..." else ""
        val content = android.widget.TextView(requireContext()).apply {
            text = contentPreview
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        container.addView(content)

        // Action buttons
        val buttonLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 0)
        }

        val viewButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "View Full"
            textSize = 12f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                showFullNoteDialog(note)
            }
        }
        buttonLayout.addView(viewButton)

        val deleteButton = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "Delete"
            textSize = 12f
            setTextColor(resources.getColor(R.color.error, null))
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 8
            }
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Note?")
                    .setMessage("Delete this note from ${note.opportunityTitle}?")
                    .setPositiveButton("Delete") { _, _ ->
                        prefs.edit().remove(note.id).apply()
                        Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show()
                        showNotesDialog() // Refresh
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        buttonLayout.addView(deleteButton)

        container.addView(buttonLayout)
        card.addView(container)

        return card
    }

    private fun showFullNoteDialog(note: AINote) {
        val scrollView = android.widget.ScrollView(requireContext())
        val contentView = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
        }

        val contentText = android.widget.TextView(requireContext()).apply {
            text = note.content
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_primary, null))
        }
        contentView.addView(contentText)

        scrollView.addView(contentView)
        val maxHeightValue = (resources.displayMetrics.heightPixels * 0.7).toInt()
        scrollView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            maxHeightValue
        )

        AlertDialog.Builder(requireContext())
            .setTitle("${note.noteType} - ${note.opportunityTitle}")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff)
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            hours < 1 -> "Just now"
            hours < 24 -> "$hours hours ago"
            days < 7 -> "$days days ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                sdf.format(java.util.Date(timestamp))
            }
        }
    }

    data class AINote(
        val id: String,
        val opportunityTitle: String,
        val noteType: String,
        val content: String,
        val timestamp: Long
    )

    private fun showAPIKeyDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_1, null)

        val inputEditText = EditText(requireContext()).apply {
            hint = "Enter your OpenAI API key"
            setText(aiManager.getApiKey())
            setSingleLine()
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            // Enable pasting
            isFocusable = true
            isFocusableInTouchMode = true
            isLongClickable = true
            setTextIsSelectable(true)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Configure OpenAI API Key")
            .setMessage("Enter your OpenAI API key to enable AI-powered features like smart recommendations, application assistant, and personalized path to success guidance.\n\nGet your API key from: https://platform.openai.com/api-keys")
            .setView(inputEditText)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = inputEditText.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    aiManager.saveApiKey(apiKey)
                    updateAIStatus()
                    Toast.makeText(
                        requireContext(),
                        "API key saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "API key cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Remove") { _, _ ->
                aiManager.saveApiKey("")
                updateAIStatus()
                Toast.makeText(
                    requireContext(),
                    "API key removed",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun setupLogoutButton() {
        logoutButton.setOnClickListener {
            authManager.logout()

            // Navigate to login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload stats when fragment becomes visible
        loadUserProfile()
        updateAIStatus()
    }
}
