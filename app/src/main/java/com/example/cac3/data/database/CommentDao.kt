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

    @Query("SELECT * FROM comments WHERE opportunityId = :opportunityId ORDER BY createdAt DESC")
    fun getCommentsForOpportunity(opportunityId: Long): LiveData<List<Comment>>

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
        AND insightType = :insightType
        ORDER BY helpfulCount DESC, createdAt DESC
    """)
    fun getCommentsByType(opportunityId: Long, insightType: InsightType): LiveData<List<Comment>>

    @Query("""
        SELECT * FROM comments
        WHERE opportunityId = :opportunityId
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
}
