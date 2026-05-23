package com.example.washyourcar.data.entities

import androidx.room.Embedded

data class AppointmentWithEmployeeName(
    @Embedded val appointment: Appointment,
    val employeeName: String?
)