package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auto_washes")
data class AutoWash(
    @PrimaryKey val autoWashId: Int,
    val ownerId: String,
    val name: String,
    val address: String,
    val city: String,
    val automaticBays: Int,
    val phoneNumber: String,
    val rating: Float,
    val openTime: Long,
    val closeTime: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)