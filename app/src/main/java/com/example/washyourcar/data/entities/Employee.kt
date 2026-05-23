package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey val firebaseUid: String,
    val carWashId: Int?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val accessToken: String
)