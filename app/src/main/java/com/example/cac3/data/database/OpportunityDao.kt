package com.example.cac3.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.OpportunityCategory

/**
 * DAO for Opportunity operations
 */
@Dao
interface OpportunityDao {

    @Query("SELECT * FROM opportunities ORDER BY priority DESC, deadline ASC")
    fun getAllOpportunities(): LiveData<List<Opportunity>>

    @Query("SELECT * FROM opportunities ORDER BY priority DESC, deadline ASC")
    suspend fun getAllOpportunitiesSync(): List<Opportunity>

    @Query("SELECT * FROM opportunities WHERE id = :id")
    fun getOpportunityById(id: Long): LiveData<Opportunity?>

    @Query("SELECT * FROM opportunities WHERE id = :id")
    suspend fun getOpportunityByIdSync(id: Long): Opportunity?

    @Query("SELECT * FROM opportunities WHERE category = :category ORDER BY deadline ASC")
    fun getOpportunitiesByCategory(category: OpportunityCategory): LiveData<List<Opportunity>>

    @Query("""
        SELECT * FROM opportunities
        WHERE title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        OR organizationName LIKE '%' || :query || '%'
        OR tags LIKE '%' || :query || '%'
        ORDER BY priority DESC
    """)
    fun searchOpportunities(query: String): LiveData<List<Opportunity>>

    @Query("""
        SELECT * FROM opportunities
        WHERE deadline IS NOT NULL
        AND deadline > :currentTime
        AND deadline < :thresholdTime
        ORDER BY deadline ASC
    """)
    fun getUpcomingDeadlines(
        currentTime: Long = System.currentTimeMillis(),
        thresholdTime: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L) // 30 days
    ): LiveData<List<Opportunity>>

    @Query("""
        SELECT * FROM opportunities
        WHERE (:category IS NULL OR category = :category)
        AND (:minGrade IS NULL OR minGrade IS NULL OR minGrade <= :minGrade)
        AND (:maxGrade IS NULL OR maxGrade IS NULL OR maxGrade >= :maxGrade)
        AND (:maxCost IS NULL OR costMax IS NULL OR costMax <= :maxCost)
        AND (:transitOnly = 0 OR transitAccessible = 1)
        AND (:virtualOnly = 0 OR isVirtual = 1)
        ORDER BY priority DESC, deadline ASC
    """)
    fun getFilteredOpportunities(
        category: OpportunityCategory?,
        minGrade: Int?,
        maxGrade: Int?,
        maxCost: Double?,
        transitOnly: Boolean,
        virtualOnly: Boolean
    ): LiveData<List<Opportunity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(opportunity: Opportunity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(opportunities: List<Opportunity>)

    @Update
    suspend fun update(opportunity: Opportunity)

    @Delete
    suspend fun delete(opportunity: Opportunity)

    @Query("DELETE FROM opportunities")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM opportunities")
    suspend fun getCount(): Int
}
