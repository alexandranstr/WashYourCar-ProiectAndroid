package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.AutoService
import com.example.washyourcar.data.entities.AutoWashService
import com.example.washyourcar.data.entities.AutoWashServiceDetails

@Dao
interface AutoWashServiceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(autoWashService: AutoWashService): Long

    @Query("UPDATE auto_wash_services SET price = :newPrice, duration = :newDuration WHERE autoWashId = :autoWashId AND autoServiceId = :serviceId")
    suspend fun updateOffer(autoWashId: Int, serviceId: Int, newPrice: Float, newDuration: Int)

    @Query("DELETE FROM auto_wash_services WHERE autoWashId = :autoWashId AND autoServiceId = :serviceId")
    suspend fun deactivateService(autoWashId: Int, serviceId: Int)

    @Query("""
        SELECT aws.*, s.name AS serviceName, s.description AS serviceDescription
        FROM auto_wash_services aws
        INNER JOIN auto_services s ON aws.autoServiceId = s.autoServiceId
        WHERE aws.autoWashId = :autoWashId
    """)
    suspend fun getServicesByAutoWashId(autoWashId: Int): List<AutoWashServiceDetails>

    @Query("""
        SELECT * FROM auto_services
        WHERE autoServiceId NOT IN (
        SELECT autoServiceId FROM auto_wash_services WHERE autoWashId = :autoWashId
        ) ORDER BY name ASC
    """)
    suspend fun getUnassociatedServices(autoWashId: Int): List<AutoService>

    @Query("""
        SELECT s.name
        FROM auto_services s
        WHERE s.autoServiceId IN (:serviceIds)
        ORDER BY s.name ASC
    """)
    suspend fun getServiceNamesByIds(serviceIds: List<Int>): List<String>

    @Query("SELECT * FROM auto_services ORDER BY name ASC")
    suspend fun getAllServices(): List<AutoService>
}