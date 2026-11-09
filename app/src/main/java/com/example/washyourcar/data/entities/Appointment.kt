package com.example.washyourcar.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey val appointmentId: String,
    val customerId: String,
    val carWashId: Int,
    val employeeId: String?,
    val serviceIds: List<Int>,
    val licensePlate: String,
    val startTime: Long,
    val endTime: Long,
    val status: String,
    // Bay booked at self-service (premium) or automatic washes; null for classic (employee) bookings.
    val bayNumber: Int? = null,
)