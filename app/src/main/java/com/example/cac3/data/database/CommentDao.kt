package com.example.cac3.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.cac3.data.model.Comment
import com.example.cac3.data.model.InsightType

/**
 * DAO for Comment operations
 */
@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE opportunityId = :opportunityId AND parentCommentId IS NULL ORDER BY createdAt DESC")
    fun getCommentsForOpportunity(opportunityId: Long): LiveData<List<Comment>>

    @Query("SELECT * FROM comments WHERE parentCommentId = :parentCommentId ORDER BY createdAt ASC")
    fun getRepliesForComment(parentCommentId: Long): LiveData<List<Comment>>

    @Query("SELECT COUNT(*) FROM comments WHERE parentCommentId = :parentCommentId")
    suspend fun getReplyCount(parentCommentId: Long): Int

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND parentCommentId IS NULL
        AND insightType = :insightType
        ORDER BY helpfulCount DESC, createdAt DESC
    """)
    fun getCommentsByType(opportunityId: Long, insightType: InsightType): LiveData<List<Comment>>

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND parentCommentId IS NULL
        AND isVerifiedParticipant = 1
        ORDER BY helpfulCount DESC, createdAt DESC
    """)
    fun getVerifiedComments(opportunityId: Long): LiveData<List<Comment>>

    @Query("SELECT * FROM comments WHERE id = :id")
    fun getCommentById(id: Long): LiveData<Comment?>

    @Query("SELECT COUNT(*) FROM comments WHERE opportunityId = :opportunityId")
    suspend fun getCommentCount(opportunityId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment): Long

    @Update
    suspend fun update(comment: Comment)

    @Query("""
        UPDATE comments
        SET upvotes = upvotes + 1,
            helpfulCount = (upvotes + 1) - downvotes
        WHERE id = :commentId
    """)
    suspend fun upvote(commentId: Long)

    @Query("""
        UPDATE comments
        SET downvotes = downvotes + 1,
            helpfulCount = upvotes - (downvotes + 1)
        WHERE id = :commentId
    """)
    suspend fun downvote(commentId: Long)

    @Query("UPDATE comments SET isFlagged = 1 WHERE id = :commentId")
    suspend fun flagComment(commentId: Long)

    @Delete
    suspend fun delete(comment: Comment)

    @Query("DELETE FROM comments WHERE opportunityId = :opportunityId")
    suspend fun deleteCommentsForOpportunity(opportunityId: Long)

    // ADVANCED FILTERING QUERIES

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND parentCommentId IS NULL
        ORDER BY
            CASE :sortBy
                WHEN 'newest' THEN createdAt
                WHEN 'oldest' THEN -createdAt
                WHEN 'helpful' THEN -helpfulCount
                WHEN 'rating' THEN -COALESCE(rating, 0)
                ELSE createdAt
            END DESC
    """)
    fun getCommentsSorted(opportunityId: Long, sortBy: String): LiveData<List<Comment>>

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND parentCommentId IS NULL
        AND (:filterType IS NULL OR insightType = :filterType)
        AND (:minRating IS NULL OR rating >= :minRating)
        AND (:verifiedOnly = 0 OR isVerifiedParticipant = 1)
        AND (:withResourcesOnly = 0 OR hasResources = 1)
        ORDER BY
            isPinned DESC,
            CASE :sortBy
                WHEN 'newest' THEN createdAt
                WHEN 'oldest' THEN -createdAt
                WHEN 'helpful' THEN -helpfulCount
                WHEN 'rating' THEN -COALESCE(rating, 0)
                ELSE createdAt
            END DESC
    """)
    fun getFilteredComments(
        opportunityId: Long,
        filterType: InsightType?,
        minRating: Int?,
        verifiedOnly: Boolean,
        withResourcesOnly: Boolean,
        sortBy: String
    ): LiveData<List<Comment>>

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND parentCommentId IS NULL
        AND insightType IN (:types)
        ORDER BY isPinned DESC, helpfulCount DESC, createdAt DESC
    """)
    fun getCommentsByTypes(opportunityId: Long, types: List<InsightType>): LiveData<List<Comment>>

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND parentCommentId IS NULL
        AND hasResources = 1
        ORDER BY helpfulCount DESC, createdAt DESC
    """)
    fun getCommentsWithResources(opportunityId: Long): LiveData<List<Comment>>

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND parentCommentId IS NULL
        AND isPinned = 1
        ORDER BY createdAt DESC
    """)
    fun getPinnedComments(opportunityId: Long): LiveData<List<Comment>>

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND parentCommentId IS NULL
        AND rating >= :minRating
        ORDER BY rating DESC, helpfulCount DESC
    """)
    fun getHighRatedComments(opportunityId: Long, minRating: Int): LiveData<List<Comment>>

    @Query("""
        SELECT AVG(rating) FROM comments
        WHERE opportunityId = :opportunityId
        AND rating IS NOT NULL
    """)
    suspend fun getAverageRating(opportunityId: Long): Double?

    @Query("""
        SELECT COUNT(*) FROM comments
        WHERE opportunityId = :opportunityId
        AND insightType = :type
    """)
    suspend fun getCommentCountByType(opportunityId: Long, type: InsightType): Int

    @Query("""
        UPDATE comments
        SET reportCount = reportCount + 1,
            isFlagged = CASE WHEN reportCount + 1 >= 3 THEN 1 ELSE isFlagged END
        WHERE id = :commentId
    """)
    suspend fun reportComment(commentId: Long)

    @Query("UPDATE comments SET isPinned = :pinned WHERE id = :commentId")
    suspend fun setPinned(commentId: Long, pinned: Boolean)
}
