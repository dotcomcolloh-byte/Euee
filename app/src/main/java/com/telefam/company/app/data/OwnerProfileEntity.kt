package com.telefam.company.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owner_profile")
data class OwnerProfileEntity(
    @PrimaryKey
    val userId: String,
    val profileJson: String,
    val pendingProfileSync: Boolean = false,
    val updatedAtMs: Long = 0L
)
