package com.telefam.company.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnerProfileDao {

    @Query("SELECT * FROM owner_profile WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<OwnerProfileEntity?>

    @Query("SELECT * FROM owner_profile WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): OwnerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OwnerProfileEntity)

    @Query("UPDATE owner_profile SET pendingProfileSync = 0, updatedAtMs = :nowMs WHERE userId = :userId")
    suspend fun clearPendingFlag(userId: String, nowMs: Long)

    @Query("DELETE FROM owner_profile")
    suspend fun clearAll()
}
