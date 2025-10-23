package com.example.cac3.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cac3.MainActivity
import com.example.cac3.R
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.User
import com.example.cac3.util.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var database: AppDatabase

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var switchToSignupText: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if already logged in
        authManager = AuthManager(this)
        if (authManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        // Handle edge-to-edge display for camera cutout
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(0, insets.systemWindowInsetTop, 0, 0)
            insets
        }

        database = AppDatabase.getDatabase(this)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        switchToSignupText = findViewById(R.id.switchToSignupText)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            performLogin()
        }

        switchToSignupText.setOnClickListener {
            // Go to signup screen
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val user = database.userDao().login(email, password)
            if (user != null) {
                // Save session
                authManager.login(
                    user.id,
                    user.email,
                    user.fullName,
                    user.grade,
                    user.interests
                )

                // Navigate to main app
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } else {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
