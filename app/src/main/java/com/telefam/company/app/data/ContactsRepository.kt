package com.telefam.company.app.data

import com.telefam.company.app.nativecore.ContactItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactsRepository(private val contactDao: ContactDao) {

    fun observeContacts(ownerUserId: String): Flow<List<ContactItem>> =
        contactDao.observeContacts(ownerUserId).map { list ->
            list.map { it.toContactItem() }
        }

    suspend fun setOptimisticFollowState(
        ownerUserId: String,
        targetUserId: String,
        follow: Boolean
    ): ContactEntity? {
        val existing = contactDao.getById(ownerUserId, targetUserId) ?: return null
        val updated = existing.copy(
            iFollow = follow,
            isFriend = follow && existing.followsMe,
            pendingAction = if (follow) ContactEntity.PENDING_FOLLOW else ContactEntity.PENDING_UNFOLLOW,
            updatedAtMs = System.currentTimeMillis()
        )
        contactDao.upsert(updated)
        return updated
    }

    suspend fun getPendingMutations(ownerUserId: String): List<ContactEntity> =
        contactDao.getPendingActions(ownerUserId)

    suspend fun markMutationSynced(
        ownerUserId: String,
        targetUserId: String,
        iFollow: Boolean,
        followsMe: Boolean
    ) {
        val existing = contactDao.getById(ownerUserId, targetUserId) ?: return
        contactDao.upsert(
            existing.copy(
                iFollow = iFollow,
                followsMe = followsMe,
                isFriend = iFollow && followsMe,
                pendingAction = ContactEntity.PENDING_NONE,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun revertPendingFollowOnRateLimit(
        ownerUserId: String,
        targetUserId: String
    ) {
        val existing = contactDao.getById(ownerUserId, targetUserId) ?: return
        val revertedFollow = existing.pendingAction == ContactEntity.PENDING_UNFOLLOW
        contactDao.upsert(
            existing.copy(
                iFollow = revertedFollow,
                isFriend = revertedFollow && existing.followsMe,
                pendingAction = ContactEntity.PENDING_NONE,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun mergeServerDirectory(ownerUserId: String, remoteItems: List<ContactItem>) {
        val existingById = contactDao.getContactsSnapshot(ownerUserId).associateBy { it.userId }
        val now = System.currentTimeMillis()
        val merged = remoteItems.map { item ->
            val local = existingById[item.userId]
            if (local != null && local.pendingAction != ContactEntity.PENDING_NONE) {
                val localFollow = local.pendingAction == ContactEntity.PENDING_FOLLOW
                local.copy(
                    fullName = item.fullName,
                    username = item.username,
                    countryIso = item.countryIso,
                    countryName = item.countryName,
                    avatarUrl = item.avatarUrl,
                    followsMe = item.followsMe,
                    iFollow = localFollow,
                    isFriend = localFollow && item.followsMe,
                    updatedAtMs = now
                )
            } else {
                ContactEntity(
                    userId = item.userId,
                    ownerUserId = ownerUserId,
                    fullName = item.fullName,
                    username = item.username,
                    countryIso = item.countryIso,
                    countryName = item.countryName,
                    avatarUrl = item.avatarUrl,
                    iFollow = item.iFollow,
                    followsMe = item.followsMe,
                    isFriend = item.iFollow && item.followsMe,
                    pendingAction = ContactEntity.PENDING_NONE,
                    updatedAtMs = now
                )
            }
        }
        if (merged.isNotEmpty()) {
            contactDao.upsertAll(merged)
            contactDao.deleteStaleSynced(ownerUserId, remoteItems.map { it.userId })
        }
    }

    suspend fun clearAll() = contactDao.clearAll()
}
