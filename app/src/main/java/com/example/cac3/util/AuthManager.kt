package com.example.cac3.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple authentication manager using SharedPreferences
 */
class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "OpportunityHubAuth"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_GRADE = "user_grade"
        private const val KEY_USER_INTERESTS = "user_interests"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun login(userId: Long, email: String, name: String, grade: Int, interests: String) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putInt(KEY_USER_GRADE, grade)
            putString(KEY_USER_INTERESTS, interests)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getCurrentUserId(): Long = prefs.getLong(KEY_USER_ID, -1)

    fun getCurrentUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun getCurrentUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun getCurrentUserGrade(): Int = prefs.getInt(KEY_USER_GRADE, 9)

    fun getCurrentUserInterests(): String? = prefs.getString(KEY_USER_INTERESTS, null)

    fun updateInterests(interests: String) {
        prefs.edit().putString(KEY_USER_INTERESTS, interests).apply()
    }
}
