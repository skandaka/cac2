package com.example.cac3.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User preferences for personalization
 */
@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1, // Single row

    // User Profile
    val currentGrade: Int? = null, // 9-12
    val schoolName: String = "Conant High School",
    val gpa: Double? = null,
    val zipCode: String? = null,
    val familyIncome: String? = null, // For need-based filters

    // Transportation Access
    val hasCar: Boolean = false,
    val hasBike: Boolean = false,
    val paceRoutesAccess: String? = null, // Comma-separated accessible routes

    // Interests (for recommendations)
    val interests: String? = null, // Comma-separated: STEM, Arts, Leadership, Service, etc.

    // Notification Preferences
    val deadlineNotifications: Boolean = true,
    val newOpportunitiesNotifications: Boolean = true,
    val commentNotifications: Boolean = false,

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
