package com.example.washyourcar.data.entities

import androidx.room.Embedded

data class AppointmentWithFullDetails(
    @Embedded val appointment: Appointment,
    val employeeName: String?,
    val customerFirstName: String?,
    val customerLastName: String?,
    val customerPhone: String?,
    val serviceNames: String
)
