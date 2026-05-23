package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "car_washes")
data class CarWash(
    @PrimaryKey val carWashId: Int,
    val ownerId: String,
    val name: String,
    val address: String,
    val city: String,
    val phoneNumber: String,
    val rating: Float,
    val accessCode: String,
    val openTime: Long,
    val closeTime: Long
)