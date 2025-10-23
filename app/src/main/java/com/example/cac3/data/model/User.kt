package com.example.cac3.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User entity for authentication and personalization
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val email: String,
    val password: String, // In production, this should be hashed
    val fullName: String,
    val schoolName: String = "Conant High School",
    val grade: Int, // 9-12
    val gpa: Double? = null,

    // Interests for personalization
    val interests: String, // Comma-separated: STEM,Arts,Leadership,Service,etc.

    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Interest categories for profiling
 */
enum class InterestCategory(val displayName: String) {
    STEM("STEM (Science, Technology, Engineering, Math)"),
    ARTS("Arts & Creative"),
    LEADERSHIP("Leadership & Government"),
    SERVICE("Community Service"),
    ATHLETICS("Sports & Athletics"),
    BUSINESS("Business & Entrepreneurship"),
    HEALTHCARE("Healthcare & Medicine"),
    EDUCATION("Education & Teaching")
}

/**
 * Subject tags for filtering opportunities
 */
enum class SubjectTag(val displayName: String) {
    // STEM
    COMPUTER_SCIENCE("Computer Science"),
    ENGINEERING("Engineering"),
    MATHEMATICS("Mathematics"),
    SCIENCE("Science"),
    ROBOTICS("Robotics"),

    // Arts
    VISUAL_ARTS("Visual Arts"),
    PERFORMING_ARTS("Performing Arts"),
    MUSIC("Music"),
    THEATER("Theater"),
    FILM("Film"),

    // Service
    COMMUNITY_SERVICE("Community Service"),
    VOLUNTEERING("Volunteering"),
    SOCIAL_IMPACT("Social Impact"),

    // Leadership
    GOVERNMENT("Government"),
    DEBATE("Debate & Speech"),
    MODEL_UN("Model UN"),
    STUDENT_GOVERNMENT("Student Government"),

    // Other
    ATHLETICS("Athletics"),
    CULTURAL("Cultural"),
    ACADEMIC("Academic"),
    BUSINESS("Business"),
    HEALTHCARE("Healthcare")
}

/**
 * Activity commitment tracking
 */
@Entity(tableName = "user_commitments")
data class UserCommitment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Long,
    val opportunityId: Long,

    val status: CommitmentStatus,
    val hoursPerWeek: Int = 0,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val notes: String? = null,

    val addedAt: Long = System.currentTimeMillis()
)

enum class CommitmentStatus {
    INTERESTED,
    APPLIED,
    ACCEPTED,
    PARTICIPATING,
    COMPLETED
}
