package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey val reviewId: String,
    val carWashId: Int,
    val customerId: String,
    val customerName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long
)
