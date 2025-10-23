package com.example.cac3.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.*
import com.example.cac3.repository.OpportunityRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing opportunity data and UI state
 */
class OpportunityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OpportunityRepository

    // LiveData for UI
    val allOpportunities: LiveData<List<Opportunity>>
    val featuredOpportunities: LiveData<List<Opportunity>>
    val hiddenGems: LiveData<List<Opportunity>>
    val upcomingDeadlines: LiveData<List<Opportunity>>
    val savedOpportunities: LiveData<List<SavedOpportunity>>
    val userPreferences: LiveData<UserPreferences?>

    // Search and filter state
    private val _searchResults = MutableLiveData<List<Opportunity>>()
    val searchResults: LiveData<List<Opportunity>> = _searchResults

    private val _selectedOpportunity = MutableLiveData<Opportunity?>()
    val selectedOpportunity: LiveData<Opportunity?> = _selectedOpportunity

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    init {
        val database = AppDatabase.getDatabase(application)
        repository = OpportunityRepository(database)

        allOpportunities = repository.getAllOpportunities()
        featuredOpportunities = repository.getFeaturedOpportunities()
        hiddenGems = repository.getHiddenGems()
        upcomingDeadlines = repository.getUpcomingDeadlines()
        savedOpportunities = repository.getAllSavedOpportunities()
        userPreferences = repository.getUserPreferences()
    }

    // Opportunity operations
    fun selectOpportunity(opportunity: Opportunity) {
        _selectedOpportunity.value = opportunity
        loadCommentsForOpportunity(opportunity.id)
    }

    fun getOpportunityById(id: Long): LiveData<Opportunity?> {
        return repository.getOpportunityById(id)
    }

    fun searchOpportunities(query: String) {
        viewModelScope.launch {
            _searchResults.value = repository.searchOpportunities(query).value ?: emptyList()
        }
    }

    fun filterOpportunities(
        category: OpportunityCategory?,
        minGrade: Int?,
        maxGrade: Int?,
        maxCost: Double?,
        transitOnly: Boolean,
        virtualOnly: Boolean
    ) {
        viewModelScope.launch {
            _searchResults.value = repository.getFilteredOpportunities(
                category, minGrade, maxGrade, maxCost, transitOnly, virtualOnly
            ).value ?: emptyList()
        }
    }

    fun getOpportunitiesByCategory(category: OpportunityCategory): LiveData<List<Opportunity>> {
        return repository.getOpportunitiesByCategory(category)
    }

    // Comment operations
    private fun loadCommentsForOpportunity(opportunityId: Long) {
        viewModelScope.launch {
            _comments.value = repository.getCommentsForOpportunity(opportunityId).value ?: emptyList()
        }
    }

    fun getCommentsForOpportunity(opportunityId: Long): LiveData<List<Comment>> {
        return repository.getCommentsForOpportunity(opportunityId)
    }

    fun addComment(
        opportunityId: Long,
        commentText: String,
        insightType: InsightType,
        userName: String = "Anonymous",
        userGrade: Int? = null
    ) {
        viewModelScope.launch {
            val comment = Comment(
                opportunityId = opportunityId,
                comment = commentText,
                insightType = insightType,
                userName = userName,
                userGrade = userGrade
            )
            repository.insertComment(comment)
            loadCommentsForOpportunity(opportunityId)
        }
    }

    fun upvoteComment(commentId: Long, opportunityId: Long) {
        viewModelScope.launch {
            repository.upvoteComment(commentId)
            loadCommentsForOpportunity(opportunityId)
        }
    }

    fun downvoteComment(commentId: Long, opportunityId: Long) {
        viewModelScope.launch {
            repository.downvoteComment(commentId)
            loadCommentsForOpportunity(opportunityId)
        }
    }

    fun flagComment(commentId: Long) {
        viewModelScope.launch {
            repository.flagComment(commentId)
        }
    }

    // Saved opportunity operations
    fun saveOpportunity(opportunityId: Long, status: SavedStatus = SavedStatus.SAVED) {
        viewModelScope.launch {
            val savedOpportunity = SavedOpportunity(
                opportunityId = opportunityId,
                status = status
            )
            repository.saveOpportunity(savedOpportunity)
        }
    }

    fun unsaveOpportunity(opportunityId: Long) {
        viewModelScope.launch {
            repository.unsaveOpportunity(opportunityId)
        }
    }

    fun updateSavedOpportunityStatus(opportunityId: Long, status: SavedStatus) {
        viewModelScope.launch {
            repository.updateSavedOpportunityStatus(opportunityId, status)
        }
    }

    suspend fun isOpportunitySaved(opportunityId: Long): Boolean {
        return repository.isOpportunitySaved(opportunityId)
    }

    // User preferences operations
    fun updateUserPreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            val existing = repository.getUserPreferences().value
            if (existing != null) {
                repository.updateUserPreferences(preferences)
            } else {
                repository.insertUserPreferences(preferences)
            }
        }
    }
}
