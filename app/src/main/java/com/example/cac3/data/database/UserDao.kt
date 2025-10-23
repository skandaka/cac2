package com.example.cac3.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.cac3.data.model.User
import com.example.cac3.data.model.UserCommitment

/**
 * DAO for User operations
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    suspend fun login(email: String, password: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    // User Commitments
    @Query("SELECT * FROM user_commitments WHERE userId = :userId ORDER BY addedAt DESC")
    fun getUserCommitments(userId: Long): LiveData<List<UserCommitment>>

    @Query("SELECT SUM(hoursPerWeek) FROM user_commitments WHERE userId = :userId")
    suspend fun getTotalHoursPerWeek(userId: Long): Int?

    @Query("SELECT COUNT(*) FROM user_commitments WHERE userId = :userId")
    suspend fun getCommitmentCount(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: UserCommitment): Long

    @Update
    suspend fun updateCommitment(commitment: UserCommitment)

    @Delete
    suspend fun deleteCommitment(commitment: UserCommitment)

    @Query("SELECT * FROM user_commitments WHERE userId = :userId AND opportunityId = :opportunityId LIMIT 1")
    suspend fun getCommitment(userId: Long, opportunityId: Long): UserCommitment?

    @Query("DELETE FROM user_commitments WHERE userId = :userId AND opportunityId = :opportunityId")
    suspend fun deleteCommitmentByOpportunity(userId: Long, opportunityId: Long)
}
