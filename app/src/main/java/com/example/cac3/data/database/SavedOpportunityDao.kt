package com.example.cac3.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.cac3.data.model.SavedOpportunity
import com.example.cac3.data.model.SavedStatus

/**
 * DAO for SavedOpportunity operations
 */
@Dao
interface SavedOpportunityDao {

    @Query("""
        SELECT * FROM saved_opportunities
        ORDER BY savedAt DESC
    """)
    fun getAllSavedOpportunities(): LiveData<List<SavedOpportunity>>

    @Query("""
        SELECT * FROM saved_opportunities
        WHERE status = :status
        ORDER BY statusChangedAt DESC
    """)
    fun getSavedOpportunitiesByStatus(status: SavedStatus): LiveData<List<SavedOpportunity>>

    @Query("SELECT * FROM saved_opportunities WHERE opportunityId = :opportunityId")
    fun getSavedOpportunity(opportunityId: Long): LiveData<SavedOpportunity?>

    @Query("SELECT * FROM saved_opportunities WHERE opportunityId = :opportunityId")
    suspend fun getSavedOpportunitySync(opportunityId: Long): SavedOpportunity?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM saved_opportunities
            WHERE opportunityId = :opportunityId
        )
    """)
    suspend fun isOpportunitySaved(opportunityId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedOpportunity: SavedOpportunity): Long

    @Update
    suspend fun update(savedOpportunity: SavedOpportunity)

    @Query("""
        UPDATE saved_opportunities
        SET status = :status,
            statusChangedAt = :timestamp
        WHERE opportunityId = :opportunityId
    """)
    suspend fun updateStatus(
        opportunityId: Long,
        status: SavedStatus,
        timestamp: Long = System.currentTimeMillis()
    )

    @Delete
    suspend fun delete(savedOpportunity: SavedOpportunity)

    @Query("DELETE FROM saved_opportunities WHERE opportunityId = :opportunityId")
    suspend fun deleteByOpportunityId(opportunityId: Long)

    @Query("SELECT COUNT(*) FROM saved_opportunities")
    suspend fun getCount(): Int
}
