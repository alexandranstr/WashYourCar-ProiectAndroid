package com.example.washyourcar.data.enitities

import androidx.room.Embedded
import com.example.washyourcar.models.enitities.Appointment

data class AppointmentWithDetails (
    @Embedded val appointment: Appointment,
    val customerFirstName: String?,
    val customerLastName: String?,
    val customerPhone: String?
)