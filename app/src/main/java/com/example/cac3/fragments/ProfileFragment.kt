package com.example.cac3.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cac3.R
import com.example.cac3.activities.LoginActivity
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.InterestCategory
import com.example.cac3.util.AuthManager
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

    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userGradeTextView: TextView
    private lateinit var totalCommitmentsTextView: TextView
    private lateinit var totalHoursTextView: TextView
    private lateinit var interestsChipGroup: ChipGroup
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

        initializeViews(view)
        loadUserProfile()
        setupLogoutButton()
    }

    private fun initializeViews(view: View) {
        userNameTextView = view.findViewById(R.id.userNameTextView)
        userEmailTextView = view.findViewById(R.id.userEmailTextView)
        userGradeTextView = view.findViewById(R.id.userGradeTextView)
        totalCommitmentsTextView = view.findViewById(R.id.totalCommitmentsTextView)
        totalHoursTextView = view.findViewById(R.id.totalHoursTextView)
        interestsChipGroup = view.findViewById(R.id.interestsChipGroup)
        logoutButton = view.findViewById(R.id.logoutButton)
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
    }
}
