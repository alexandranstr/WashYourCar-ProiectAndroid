package com.example.washyourcar.data.enitities

import androidx.room.Embedded
import com.example.washyourcar.models.enitities.Appointment

data class AppointmentWithEmployeeName(
    @Embedded val appointment: Appointment,
    val employeeName: String?
)