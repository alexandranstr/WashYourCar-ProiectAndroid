package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "services")
data class Service (
    @PrimaryKey val serviceId: Int,
    val name: String,
    val description: String
)