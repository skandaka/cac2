package com.example.cac3.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Main Opportunity entity representing all types of opportunities
 * (Competitions, Employment, Volunteering, Colleges, Clubs, Honor Societies, etc.)
 */
@Entity(tableName = "opportunities")
@Parcelize
data class Opportunity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Basic Info
    val title: String,
    val description: String,
    val category: OpportunityCategory,
    val type: String, // Specific type within category

    // Contact & Location
    val organizationName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val address: String? = null,
    val website: String? = null,

    // Deadlines & Timeline
    val deadline: Long? = null, // Timestamp
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isRolling: Boolean = false,

    // Eligibility
    val minGrade: Int? = null, // 9-12
    val maxGrade: Int? = null,
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val minGPA: Double? = null,
    val maxIncome: Int? = null, // For need-based programs

    // Costs & Financial
    val cost: String? = null,
    val costMin: Double? = null,
    val costMax: Double? = null,
    val scholarshipAvailable: Boolean = false,
    val scholarshipAmount: String? = null,
    val wage: String? = null, // For jobs
    val wageMin: Double? = null,
    val wageMax: Double? = null,

    // Time Commitment
    val hoursPerWeek: String? = null,
    val hoursPerWeekMin: Int? = null,
    val hoursPerWeekMax: Int? = null,
    val totalHoursRequired: Int? = null,

    // Requirements
    val requirements: String? = null,
    val workPermitRequired: Boolean = false,
    val applicationComponents: String? = null,

    // Transportation
    val transitAccessible: Boolean = false,
    val paceRoutes: String? = null, // Comma-separated route numbers
    val walkingDistance: String? = null,
    val requiresCar: Boolean = false,
    val isVirtual: Boolean = false,

    // Benefits
    val benefits: String? = null,
    val collegeCredit: Boolean = false,
    val serviceHours: Boolean = false,
    val graduationCord: Boolean = false,

    // Club-specific
    val meetingDay: String? = null,
    val meetingTime: String? = null,
    val meetingRoom: String? = null,
    val schoologyCode: String? = null,
    val sponsor: String? = null,
    val sponsorEmail: String? = null,

    // Metadata
    val tags: String? = null, // Comma-separated tags
    val priority: Int = 0, // For sorting opportunities
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class OpportunityCategory {
    COMPETITION,
    EMPLOYMENT,
    VOLUNTEERING,
    COLLEGE,
    CLUB,
    HONOR_SOCIETY,
    SUMMER_PROGRAM,
    TEST_PREP,
    INTERNSHIP,
    OTHER
}
