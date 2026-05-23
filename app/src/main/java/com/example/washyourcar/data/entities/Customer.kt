package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val firebaseUid: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String
)