package com.telefam.company.app.data

import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val ownerProfileDao: OwnerProfileDao) {

    fun observeProfile(userId: String): Flow<OwnerProfileEntity?> =
        ownerProfileDao.observeProfile(userId)

    suspend fun getCachedProfile(userId: String): OwnerProfileEntity? =
        ownerProfileDao.getProfile(userId)

    suspend fun saveLocalEdit(userId: String, profileJson: String) {
        if (userId.isBlank() || profileJson.isBlank()) return
        ownerProfileDao.upsert(
            OwnerProfileEntity(
                userId = userId,
                profileJson = profileJson,
                pendingProfileSync = true,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveServerProfile(userId: String, serverProfileJson: String) {
        if (userId.isBlank() || serverProfileJson.isBlank()) return
        val existing = ownerProfileDao.getProfile(userId)
        if (existing != null && existing.pendingProfileSync) {
            return
        }
        ownerProfileDao.upsert(
            OwnerProfileEntity(
                userId = userId,
                profileJson = serverProfileJson,
                pendingProfileSync = false,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun markSyncCompleted(userId: String, serverProfileJson: String) {
        if (userId.isBlank()) return
        val existing = ownerProfileDao.getProfile(userId)
        ownerProfileDao.upsert(
            OwnerProfileEntity(
                userId = userId,
                profileJson = serverProfileJson.ifBlank { existing?.profileJson.orEmpty() },
                pendingProfileSync = false,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearPendingFlag(userId: String) {
        if (userId.isBlank()) return
        ownerProfileDao.clearPendingFlag(userId, System.currentTimeMillis())
    }

    suspend fun clearAll() = ownerProfileDao.clearAll()
}
