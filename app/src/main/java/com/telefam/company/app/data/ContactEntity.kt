package com.telefam.company.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.telefam.company.app.nativecore.ContactItem

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: String,
    val ownerUserId: String,
    val fullName: String,
    val username: String,
    val countryIso: String,
    val countryName: String,
    val avatarUrl: String,
    val iFollow: Boolean,
    val followsMe: Boolean,
    val isFriend: Boolean,
    val pendingAction: String = PENDING_NONE,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun toContactItem(): ContactItem = ContactItem(
        userId = userId,
        fullName = fullName,
        username = username,
        countryIso = countryIso,
        countryName = countryName,
        avatarUrl = avatarUrl,
        iFollow = iFollow,
        followsMe = followsMe,
        isFriend = iFollow && followsMe
    )

    companion object {
        const val PENDING_NONE = "NONE"
        const val PENDING_FOLLOW = "FOLLOW"
        const val PENDING_UNFOLLOW = "UNFOLLOW"
    }
}
