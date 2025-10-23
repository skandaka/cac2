package com.example.cac3.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for tracking opportunities saved by the user
 */
@Entity(
    tableName = "saved_opportunities",
    foreignKeys = [
        ForeignKey(
            entity = Opportunity::class,
            parentColumns = ["id"],
            childColumns = ["opportunityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("opportunityId")]
)
data class SavedOpportunity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val opportunityId: Long,

    // Status tracking
    val status: SavedStatus = SavedStatus.SAVED,
    val notes: String? = null,

    // Timestamps
    val savedAt: Long = System.currentTimeMillis(),
    val statusChangedAt: Long = System.currentTimeMillis()
)

enum class SavedStatus {
    SAVED,           // Bookmarked for later
    INTERESTED,      // Actively interested
    APPLIED,         // Application submitted
    PARTICIPATING,   // Currently participating
    COMPLETED        // Finished
}
