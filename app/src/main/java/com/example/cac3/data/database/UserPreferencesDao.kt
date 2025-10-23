package com.example.cac3.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.cac3.data.model.UserPreferences

/**
 * DAO for UserPreferences operations
 */
@Dao
interface UserPreferencesDao {

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getUserPreferences(): LiveData<UserPreferences?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getUserPreferencesSync(): UserPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userPreferences: UserPreferences)

    @Update
    suspend fun update(userPreferences: UserPreferences)

    @Query("""
        UPDATE user_preferences
        SET currentGrade = :grade,
            updatedAt = :timestamp
        WHERE id = 1
    """)
    suspend fun updateGrade(grade: Int, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE user_preferences
        SET gpa = :gpa,
            updatedAt = :timestamp
        WHERE id = 1
    """)
    suspend fun updateGPA(gpa: Double, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE user_preferences
        SET interests = :interests,
            updatedAt = :timestamp
        WHERE id = 1
    """)
    suspend fun updateInterests(interests: String, timestamp: Long = System.currentTimeMillis())
}
