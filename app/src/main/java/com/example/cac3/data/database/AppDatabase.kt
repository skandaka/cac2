package com.example.cac3.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cac3.data.model.Comment
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.SavedOpportunity
import com.example.cac3.data.model.User
import com.example.cac3.data.model.UserCommitment
import com.example.cac3.data.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room database for the College Opportunity Hub app
 */
@Database(
    entities = [
        Opportunity::class,
        Comment::class,
        SavedOpportunity::class,
        UserPreferences::class,
        User::class,
        UserCommitment::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun opportunityDao(): OpportunityDao
    abstract fun commentDao(): CommentDao
    abstract fun savedOpportunityDao(): SavedOpportunityDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "college_opportunity_hub_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Also populate if database is empty
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val count = database.opportunityDao().getCount()
                        if (count == 0) {
                            populateDatabase(database)
                        }
                    }
                }
            }
        }

        private suspend fun populateDatabase(database: AppDatabase) {
            val opportunityDao = database.opportunityDao()
            val dataPopulator = DataPopulator(database)
            dataPopulator.populateAll()
        }
    }
}
