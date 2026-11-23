package com.telefam.company.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE ownerUserId = :ownerUserId ORDER BY fullName COLLATE NOCASE ASC")
    fun observeContacts(ownerUserId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE ownerUserId = :ownerUserId")
    suspend fun getContactsSnapshot(ownerUserId: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE ownerUserId = :ownerUserId AND pendingAction <> 'NONE'")
    suspend fun getPendingActions(ownerUserId: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE ownerUserId = :ownerUserId AND userId = :targetUserId LIMIT 1")
    suspend fun getById(ownerUserId: String, targetUserId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE ownerUserId = :ownerUserId AND pendingAction = 'NONE' AND userId NOT IN (:keepIds)")
    suspend fun deleteStaleSynced(ownerUserId: String, keepIds: List<String>)

    @Query("DELETE FROM contacts")
    suspend fun clearAll()
}
