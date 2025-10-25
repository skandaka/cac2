package com.example.cac3.repository

import androidx.lifecycle.LiveData
import com.example.cac3.data.database.*
import com.example.cac3.data.model.*

/**
 * Repository for accessing opportunity data
 */
class OpportunityRepository(private val database: AppDatabase) {

    private val opportunityDao = database.opportunityDao()
    private val commentDao = database.commentDao()
    private val savedOpportunityDao = database.savedOpportunityDao()
    private val userPreferencesDao = database.userPreferencesDao()

    // Opportunity operations
    fun getAllOpportunities(): LiveData<List<Opportunity>> = opportunityDao.getAllOpportunities()

    fun getOpportunityById(id: Long): LiveData<Opportunity?> = opportunityDao.getOpportunityById(id)

    fun getOpportunitiesByCategory(category: OpportunityCategory): LiveData<List<Opportunity>> =
        opportunityDao.getOpportunitiesByCategory(category)

    fun searchOpportunities(query: String): LiveData<List<Opportunity>> =
        opportunityDao.searchOpportunities(query)

    fun getUpcomingDeadlines(): LiveData<List<Opportunity>> =
        opportunityDao.getUpcomingDeadlines()

    fun getFilteredOpportunities(
        category: OpportunityCategory?,
        minGrade: Int?,
        maxGrade: Int?,
        maxCost: Double?,
        transitOnly: Boolean,
        virtualOnly: Boolean
    ): LiveData<List<Opportunity>> =
        opportunityDao.getFilteredOpportunities(
            category, minGrade, maxGrade, maxCost, transitOnly, virtualOnly
        )

    suspend fun insertOpportunity(opportunity: Opportunity) = opportunityDao.insert(opportunity)

    // Comment operations
    fun getCommentsForOpportunity(opportunityId: Long): LiveData<List<Comment>> =
        commentDao.getCommentsForOpportunity(opportunityId)

    fun getCommentsByType(opportunityId: Long, insightType: InsightType): LiveData<List<Comment>> =
        commentDao.getCommentsByType(opportunityId, insightType)

    fun getVerifiedComments(opportunityId: Long): LiveData<List<Comment>> =
        commentDao.getVerifiedComments(opportunityId)

    suspend fun getCommentCount(opportunityId: Long): Int =
        commentDao.getCommentCount(opportunityId)

    suspend fun insertComment(comment: Comment) = commentDao.insert(comment)

    suspend fun upvoteComment(commentId: Long) = commentDao.upvote(commentId)

    suspend fun downvoteComment(commentId: Long) = commentDao.downvote(commentId)

    suspend fun flagComment(commentId: Long) = commentDao.flagComment(commentId)

    // Saved opportunity operations
    fun getAllSavedOpportunities(): LiveData<List<SavedOpportunity>> =
        savedOpportunityDao.getAllSavedOpportunities()

    fun getSavedOpportunitiesByStatus(status: SavedStatus): LiveData<List<SavedOpportunity>> =
        savedOpportunityDao.getSavedOpportunitiesByStatus(status)

    fun getSavedOpportunity(opportunityId: Long): LiveData<SavedOpportunity?> =
        savedOpportunityDao.getSavedOpportunity(opportunityId)

    suspend fun isOpportunitySaved(opportunityId: Long): Boolean =
        savedOpportunityDao.isOpportunitySaved(opportunityId)

    suspend fun saveOpportunity(savedOpportunity: SavedOpportunity) =
        savedOpportunityDao.insert(savedOpportunity)

    suspend fun updateSavedOpportunityStatus(opportunityId: Long, status: SavedStatus) =
        savedOpportunityDao.updateStatus(opportunityId, status)

    suspend fun unsaveOpportunity(opportunityId: Long) =
        savedOpportunityDao.deleteByOpportunityId(opportunityId)

    // User preferences operations
    fun getUserPreferences(): LiveData<UserPreferences?> = userPreferencesDao.getUserPreferences()

    suspend fun updateUserPreferences(userPreferences: UserPreferences) =
        userPreferencesDao.update(userPreferences)

    suspend fun insertUserPreferences(userPreferences: UserPreferences) =
        userPreferencesDao.insert(userPreferences)
}
