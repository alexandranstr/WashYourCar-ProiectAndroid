package com.example.washyourcar.data.entities

import androidx.room.Entity

@Entity(
    tableName = "self_wash_services",
    primaryKeys = ["selfWashId", "selfServiceId"]
)
data class SelfWashService(
    val selfWashId: Int,
    val selfServiceId: Int,
    val price: Float,
    val duration: Int
)