package com.example.cac3.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.cac3.data.model.Team
import com.example.cac3.data.model.TeamMember
import com.example.cac3.data.model.TeamRequest

/**
 * DAO for Team operations
 */
@Dao
interface TeamDao {

    // Teams
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: Team): Long

    @Update
    suspend fun updateTeam(team: Team)

    @Delete
    suspend fun deleteTeam(team: Team)

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: Long): Team?

    @Query("SELECT * FROM teams WHERE opportunityId = :opportunityId AND isOpen = 1 ORDER BY createdAt DESC")
    fun getTeamsForOpportunity(opportunityId: Long): LiveData<List<Team>>

    @Query("SELECT * FROM teams WHERE leaderUserId = :userId ORDER BY createdAt DESC")
    fun getTeamsLeadByUser(userId: Long): LiveData<List<Team>>

    @Query("SELECT * FROM teams WHERE isOpen = 1 ORDER BY createdAt DESC LIMIT 20")
    fun getAllOpenTeams(): LiveData<List<Team>>

    @Query("SELECT * FROM teams ORDER BY createdAt DESC")
    suspend fun getAllTeamsSync(): List<Team>

    // Team Members
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMember(member: TeamMember): Long

    @Update
    suspend fun updateTeamMember(member: TeamMember)

    @Delete
    suspend fun deleteTeamMember(member: TeamMember)

    @Query("SELECT * FROM team_members WHERE teamId = :teamId")
    suspend fun getTeamMembers(teamId: Long): List<TeamMember>

    @Query("SELECT * FROM team_members WHERE userId = :userId")
    suspend fun getUserTeams(userId: Long): List<TeamMember>

    @Query("SELECT COUNT(*) FROM team_members WHERE teamId = :teamId AND status = 'ACCEPTED'")
    suspend fun getTeamMemberCount(teamId: Long): Int

    // Team Requests
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamRequest(request: TeamRequest): Long

    @Update
    suspend fun updateTeamRequest(request: TeamRequest)

    @Query("SELECT * FROM team_requests WHERE teamId = :teamId AND status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingRequestsForTeam(teamId: Long): List<TeamRequest>

    @Query("SELECT * FROM team_requests WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getUserRequests(userId: Long): List<TeamRequest>

    @Query("SELECT * FROM team_requests WHERE userId = :userId AND teamId = :teamId")
    suspend fun getExistingRequest(userId: Long, teamId: Long): TeamRequest?
}
