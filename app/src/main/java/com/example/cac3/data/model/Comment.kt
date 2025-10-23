package com.example.cac3.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Comment entity for peer insights and feedback on opportunities
 */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = Opportunity::class,
            parentColumns = ["id"],
            childColumns = ["opportunityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("opportunityId")]
)
@Parcelize
data class Comment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val opportunityId: Long,

    // Comment Content
    val comment: String,
    val insightType: InsightType,

    // User Info (anonymous or with username)
    val userName: String = "Anonymous",
    val userGrade: Int? = null,
    val isVerifiedParticipant: Boolean = false,

    // Engagement
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val helpfulCount: Int = 0,

    // Moderation
    val isFlagged: Boolean = false,
    val isModeratorApproved: Boolean = false,

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class InsightType {
    TIME_REALITY_CHECK,      // "Actually takes 15 hrs/week, not 5"
    HIDDEN_COSTS,            // "$300 in travel expenses not mentioned"
    APPLICATION_TIP,         // "Essay matters more than GPA"
    IMPACT_STORY,           // "Got me into Northwestern"
    WARNING,                // "Advisor is disorganized"
    SOCIAL_INFO,            // "Great group, very welcoming"
    GENERAL_REVIEW          // General feedback
}
