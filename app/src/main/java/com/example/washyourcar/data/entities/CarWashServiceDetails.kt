package com.example.washyourcar.data.entities

import androidx.room.Embedded
import com.example.washyourcar.data.entities.CarWashService

data class CarWashServiceDetails (
    @Embedded val carWashService: CarWashService,
    val serviceName: String,
    val serviceDescription: String
)