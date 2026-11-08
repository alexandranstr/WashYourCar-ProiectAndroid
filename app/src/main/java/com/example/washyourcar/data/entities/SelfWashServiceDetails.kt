package com.example.washyourcar.data.entities

import androidx.room.Embedded

data class SelfWashServiceDetails(
    @Embedded val selfWashService: SelfWashService,
    val serviceName: String,
    val serviceDescription: String
)