package com.example.washyourcar.data.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

data class AutoWashServiceDetails(
    @Embedded val autoWashServices: AutoWashService,
    val serviceName: String,
    val serviceDescription: String
)