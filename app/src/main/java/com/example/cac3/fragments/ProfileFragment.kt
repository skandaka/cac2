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
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
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

    private fun showAPIKeyDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_1, null)

        val inputEditText = EditText(requireContext()).apply {
            hint = "Enter your OpenAI API key"
            setText(aiManager.getApiKey())
            setSingleLine()
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Configure OpenAI API Key")
            .setMessage("Enter your OpenAI API key to enable AI-powered features like smart recommendations, application assistant, and success probability calculator.\n\nGet your API key from: https://platform.openai.com/api-keys")
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
