package com.example.cac3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.cac3.activities.LoginActivity
import com.example.cac3.fragments.AddOpportunityFragment
import com.example.cac3.fragments.BrowseFragment
import com.example.cac3.fragments.DashboardFragment
import com.example.cac3.fragments.ProfileFragment
import com.example.cac3.util.AuthManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication
        authManager = AuthManager(this)
        if (!authManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main_new)

        // Handle edge-to-edge display for camera cutout
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(0, insets.systemWindowInsetTop, 0, 0)
            insets
        }

        bottomNav = findViewById(R.id.bottomNavigation)
        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_browse -> {
                    loadFragment(BrowseFragment())
                    true
                }
                R.id.nav_add -> {
                    loadFragment(AddOpportunityFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
