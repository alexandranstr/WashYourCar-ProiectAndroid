package com.example.washyourcar.models.enitities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey val licensePlate: String,
    val customerId: String,
    val model: String,
    val status: String
)