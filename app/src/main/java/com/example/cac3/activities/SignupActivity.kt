package com.example.cac3.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cac3.MainActivity
import com.example.cac3.R
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.InterestCategory
import com.example.cac3.data.model.User
import com.example.cac3.util.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var database: AppDatabase

    private lateinit var fullNameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var gradeSpinner: Spinner
    private lateinit var interestChipGroup: ChipGroup
    private lateinit var signupButton: MaterialButton
    private lateinit var backToLoginText: com.google.android.material.textview.MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_signup)

        // Handle camera cutout and system bars
        val rootView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        authManager = AuthManager(this)
        database = AppDatabase.getDatabase(this)

        initViews()
        setupGradeSpinner()
        setupInterestChips()
        setupClickListeners()
    }

    private fun initViews() {
        fullNameInput = findViewById(R.id.fullNameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        gradeSpinner = findViewById(R.id.gradeSpinner)
        interestChipGroup = findViewById(R.id.interestChipGroup)
        signupButton = findViewById(R.id.signupButton)
        backToLoginText = findViewById(R.id.backToLoginText)
    }

    private fun setupGradeSpinner() {
        val grades = arrayOf("Grade 9", "Grade 10", "Grade 11", "Grade 12")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, grades)
        gradeSpinner.adapter = adapter
    }

    private fun setupInterestChips() {
        InterestCategory.values().forEach { interest ->
            val chip = Chip(this).apply {
                text = interest.displayName
                isCheckable = true
                setChipBackgroundColorResource(R.color.surface_variant)
                setTextColor(getColor(R.color.text_primary))
            }
            interestChipGroup.addView(chip)
        }
    }

    private fun setupClickListeners() {
        signupButton.setOnClickListener {
            performSignup()
        }

        backToLoginText.setOnClickListener {
            finish() // Go back to LoginActivity
        }
    }

    private fun performSignup() {
        val fullName = fullNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val grade = gradeSpinner.selectedItemPosition + 9 // 9-12

        // Validate inputs
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected interests
        val selectedInterests = mutableListOf<String>()
        for (i in 0 until interestChipGroup.childCount) {
            val chip = interestChipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedInterests.add(InterestCategory.values()[i].name)
            }
        }

        if (selectedInterests.isEmpty()) {
            Toast.makeText(this, "Please select at least one interest", Toast.LENGTH_SHORT).show()
            return
        }

        val interestsString = selectedInterests.joinToString(",")

        lifecycleScope.launch {
            try {
                // Check if user already exists
                val existingUser = database.userDao().getUserByEmail(email)
                if (existingUser != null) {
                    runOnUiThread {
                        Toast.makeText(this@SignupActivity, "Email already registered", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Create new user
                val user = User(
                    email = email,
                    password = password,
                    fullName = fullName,
                    grade = grade,
                    interests = interestsString
                )

                val userId = database.userDao().insertUser(user)

                // Save session
                authManager.login(userId, email, fullName, grade, interestsString)

                // Navigate to main app
                runOnUiThread {
                    val intent = Intent(this@SignupActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SignupActivity, "Signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
