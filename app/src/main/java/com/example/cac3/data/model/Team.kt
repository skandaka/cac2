package com.example.cac3.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Team entity for collaborative opportunities
 */
@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val opportunityId: Long,
    val teamName: String,
    val description: String,
    val leaderUserId: Long,
    val maxMembers: Int = 4,
    val currentMembers: Int = 1,

    // Type: COMPETITION_TEAM or STUDY_GROUP
    val teamType: TeamType = TeamType.COMPETITION_TEAM,

    // Requirements
    val requiredSkills: String? = null, // Comma-separated
    val preferredGrade: String? = null, // e.g., "10-12"

    // For study groups
    val meetingSchedule: String? = null, // e.g., "Tuesdays 3pm"
    val meetingLocation: String? = null, // e.g., "Library", "Virtual"

    // Status
    val isOpen: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TeamType {
    COMPETITION_TEAM,  // For team competitions
    STUDY_GROUP,       // For application prep, study sessions
    INTEREST_GROUP     // General interest/hobby groups
}

/**
 * Team member entity
 */
@Entity(tableName = "team_members")
data class TeamMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val teamId: Long,
    val userId: Long,
    val role: String? = null, // e.g., "Coder", "Designer", "Writer"
    val status: TeamMemberStatus = TeamMemberStatus.PENDING,

    val joinedAt: Long = System.currentTimeMillis()
)

enum class TeamMemberStatus {
    PENDING,    // Invited but not accepted
    ACCEPTED,   // Active team member
    DECLINED,   // Declined invitation
    LEFT        // Left the team
}

/**
 * Team join request entity
 */
@Entity(tableName = "team_requests")
data class TeamRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val teamId: Long,
    val userId: Long,
    val message: String? = null,
    val status: RequestStatus = RequestStatus.PENDING,

    val createdAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null
)

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
