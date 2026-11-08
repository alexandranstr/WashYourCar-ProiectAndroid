package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.SelfService
import com.example.washyourcar.data.entities.SelfWashService
import com.example.washyourcar.data.entities.SelfWashServiceDetails

@Dao
interface SelfWashServiceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(selfWashService: SelfWashService): Long

    @Query("UPDATE self_wash_services SET price = :newPrice, duration = :newDuration WHERE selfWashId = :selfWashId AND selfServiceId = :serviceId")
    suspend fun updateOffer(selfWashId: Int, serviceId: Int, newPrice: Float, newDuration: Int)

    @Query("DELETE FROM self_wash_services WHERE selfWashId = :selfWashId AND selfServiceId = :serviceId")
    suspend fun deactivateService(selfWashId: Int, serviceId: Int)

    @Query("""
        SELECT sws.*, s.name AS serviceName, s.description AS serviceDescription
        FROM self_wash_services sws
        INNER JOIN self_services s ON sws.selfServiceId = s.selfServiceId
        WHERE sws.selfWashId = :selfWashId
    """)
    suspend fun getServicesBySelfWashId(selfWashId: Int): List<SelfWashServiceDetails>

    @Query("""
        SELECT * FROM self_services
        WHERE selfServiceId NOT IN (
        SELECT selfServiceId FROM self_wash_services WHERE selfWashId = :selfWashId
        ) ORDER BY name ASC
    """)
    suspend fun getUnassociatedServices(selfWashId: Int): List<SelfService>

    @Query("""
        SELECT s.name
        FROM self_services s
        WHERE s.selfServiceId IN (:serviceIds)
        ORDER BY s.name ASC
    """)
    suspend fun getServiceNamesByIds(serviceIds: List<Int>): List<String>

    @Query("SELECT * FROM self_services ORDER BY name ASC")
    suspend fun getAllServices(): List<SelfService>
}