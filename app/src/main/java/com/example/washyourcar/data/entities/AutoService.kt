package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "auto_services")
data class AutoService(
    @PrimaryKey val autoServiceId: Int,
    val name: String,
    val description: String
)