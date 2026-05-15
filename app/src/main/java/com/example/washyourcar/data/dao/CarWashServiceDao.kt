package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.CarWashServiceDetails
import com.example.washyourcar.data.entities.Service
import com.example.washyourcar.data.enitities.CarWashService

@Dao
interface CarWashServiceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(carWashService: CarWashService): Long

    @Query("UPDATE car_wash_services SET price = :newPrice, duration = :newDuration WHERE carWashId = :carWashId AND serviceId = :serviceId")
    suspend fun updateOffer(carWashId: Int, serviceId: Int, newPrice: Float, newDuration: Int)

    @Query("DELETE FROM car_wash_services WHERE carWashId = :carWashId AND serviceId = :serviceId")
    suspend fun deactivateService(CarWashId: Int, serviceId: Int)

    @Query("""
        SELECT cws.*, s.name AS serviceName, s.description AS serviceDescription
        FROM car_wash_services cws
        INNER JOIN services s ON cws.serviceId = s.serviceId
        WHERE cws.carWashId = :carWashId
    """)
    suspend fun getServicesByCarWashId(carWashId: Int): List<CarWashServiceDetails>

    @Query("""
        SELECT * FROM services
        WHERE serviceId NOT IN (
        SELECT serviceId FROM car_wash_services WHERE carWashId = :carWashId
        ) ORDER BY name ASC
    """)
    suspend fun getUnassociatedServices(carWashId: Int): List<Service>
}