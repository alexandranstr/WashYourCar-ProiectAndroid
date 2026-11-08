package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loyalty_cards")
data class LoyaltyCard(
    @PrimaryKey val customerUid: String,
    val cardNumber: String,
    val balance: Double = 0.0,
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
