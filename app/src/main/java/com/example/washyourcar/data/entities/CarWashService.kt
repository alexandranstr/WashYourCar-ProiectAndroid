package com.example.washyourcar.data.entities

import androidx.room.Entity

@Entity(
    tableName = "car_wash_services",
    primaryKeys = ["carWashId", "serviceId"]
)
data class CarWashService(
    val carWashId: Int,
    val serviceId: Int,
    val price: Float,
    val duration: Int
)