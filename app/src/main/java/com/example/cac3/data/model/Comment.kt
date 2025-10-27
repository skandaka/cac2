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
    val userId: Long? = null, // Link to user who posted
    val parentCommentId: Long? = null, // For nested replies (null = top-level comment)

    // Comment Content
    val comment: String,
    val insightType: InsightType,
    val rating: Int? = null, // 1-5 star rating for the opportunity

    // User Info (anonymous or with username)
    val userName: String = "Anonymous",
    val userGrade: Int? = null,
    val isVerifiedParticipant: Boolean = false,

    // Engagement & Voting
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val helpfulCount: Int = 0,
    val reportCount: Int = 0,

    // Advanced Features
    val hasResources: Boolean = false, // Does this comment include study resources/links?
    val resourceLinks: String? = null, // Comma-separated links to resources
    val tags: String? = null, // Comma-separated tags for filtering

    // Moderation
    val isFlagged: Boolean = false,
    val isModeratorApproved: Boolean = false,
    val isPinned: Boolean = false, // Moderators can pin important comments

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastEditedAt: Long? = null
) : Parcelable

enum class InsightType(val displayName: String, val description: String) {
    HELP("Help & Advice", "Ask for or provide help with applications, requirements, or processes"),
    OPINION("Opinion & Review", "Share your honest opinion and overall review of the opportunity"),
    STUDY_RESOURCES("Study Resources", "Share or request study materials, guides, and learning resources"),
    TIPS("Tips & Tricks", "Share application tips, insider knowledge, and success strategies"),
    EXPERIENCE("My Experience", "Share your personal experience participating in this opportunity"),
    QUESTION("Question", "Ask specific questions about the opportunity"),
    TIME_REALITY_CHECK("Time Reality Check", "Share the actual time commitment vs. what's advertised"),
    HIDDEN_COSTS("Hidden Costs", "Warn about unexpected expenses or financial requirements"),
    APPLICATION_TIP("Application Tip", "Specific advice for applying successfully"),
    IMPACT_STORY("Impact Story", "Share how this opportunity impacted your college/career path"),
    WARNING("Warning & Concerns", "Important warnings or concerns others should know"),
    SOCIAL_INFO("Social & Culture", "Information about the people, environment, and social aspects")
}
