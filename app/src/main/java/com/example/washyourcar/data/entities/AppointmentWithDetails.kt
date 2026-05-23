package com.example.washyourcar.data.entities

import androidx.room.Embedded

data class AppointmentWithDetails (
    @Embedded val appointment: Appointment,
    val customerFirstName: String?,
    val customerLastName: String?,
    val customerPhone: String?
)