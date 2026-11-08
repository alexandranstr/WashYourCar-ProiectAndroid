package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_wash_services",
    primaryKeys = ["autoWashId", "autoServiceId"]
)
data class AutoWashService(
    val autoWashId: Int,
    val autoServiceId: Int,
    val price: Float,
    val duration: Int
)