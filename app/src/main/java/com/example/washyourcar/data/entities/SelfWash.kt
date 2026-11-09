package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "self_washes")
data class SelfWash(
    @PrimaryKey val selfWashId: Int,
    val ownerId: String,
    val name: String,
    val address: String,
    val city: String,
    val phoneNumber: String,
    val rating: Float,
    val openTime: Long,
    val closeTime: Long,
    val normalSelfWashBays: Int,
    val premiumSelfWashBays: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
)