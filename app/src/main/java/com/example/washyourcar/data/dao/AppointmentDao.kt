package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.Appointment
import com.example.washyourcar.data.entities.AppointmentWithDetails
import com.example.washyourcar.data.entities.AppointmentWithEmployeeName

@Dao
interface AppointmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: Appointment)

    @Query("UPDATE appointments SET status = :newStatus WHERE appointmentId = :appointmentId")
    suspend fun updateStatus(appointmentId: String, newStatus: String)

    @Query("""
        SELECT * FROM appointments
        WHERE customerId = :customerId
        AND startTime > :currentTime
        AND status = 'CONFIRMED'
        ORDER BY startTime ASC
        LIMIT 1
    """)
    suspend fun getNextAppointmentForCustomer(customerId: String, currentTime: Long): Appointment?

    @Query("""
        SELECT a.*, c.firstName AS customerFirstName, c.lastName AS customerLastName, c.phoneNumber AS customerPhone
        FROM appointments a
        INNER JOIN customers c ON a.customerId = c.firebaseUid
        WHERE a.carWashId = :carWashId
        AND a.startTime >= :startOfDay
        AND a.startTime <= :endOfDay
        ORDER BY a.startTime ASC
    """)
    suspend fun getAppointmentsWithDetailsByDay(carWashId: Int, startOfDay: Long, endOfDay: Long): List<AppointmentWithDetails>

    @Query("""
        SELECT a.*, (e.firstName || ' ' || e.lastName) AS employeeName
        FROM appointments a
        INNER JOIN employees e ON a.employeeId = e.firebaseUid
        WHERE a.carWashId = :carWashId
        AND a.startTime >= :startOfDay
        AND a.startTime <= :endOfDay
        ORDER BY a.startTime ASC
    """)
    suspend fun getAppointmentsWithEmployeeByDay(carWashId: Int, startOfDay: Long, endOfDay: Long): List <AppointmentWithEmployeeName>

    @Query("""
        SELECT firebaseUid From employees
        WHERE carWashId = :carWashId
        AND firebaseUid NOT IN (
            SELECT employeeId FROM appointments 
            WHERE status != 'DONE' AND status != 'CANCELED'
            AND startTime < :endTime
            AND endTime > :startTime
        )
    """)
    suspend fun getAvailableEmployees(carWashId: Int, startTime: Long, endTime: Long): List<String>

    @Query("""
        UPDATE appointments
        SET status = CASE 
            WHEN :currentTime BETWEEN startTime AND endTime THEN 'IN_PROGRESS'
            WHEN :currentTime >= endTime THEN 'DONE'
            ELSE status
        END
        WHERE status IN ('CONFIRMED', 'IN_PROGRESS')
    """)
    suspend fun updateAutomaticStatuses(currentTime: Long)
}