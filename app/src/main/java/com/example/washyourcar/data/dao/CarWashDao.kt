package com.example.washyourcar.data.dao

import android.icu.text.StringSearch
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.enitities.CarWash

@Dao
interface CarWashDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAndReturnId(carWash: CarWash): Long

    @Query("SELECT EXISTS(SELECT 1 FROM  car_washes WHERE accessCode = :code)")
    suspend fun doesAccessCodeExist(code: String): Boolean

    @Query("SELECT carWashId FROM car_washes WHERE accessCode = :code")
    suspend fun getCarWashIdByAccessCode(code: String): Int?

    @Query("SELECT DISTINCT city FROM car_washes ORDER BY city ASC")
    suspend fun getAllCities(): List<String>

    @Query("SELECT * FROM car_washes ORDER BY rating DESC")
    suspend fun getAllCarWashes(): List<CarWash>

    @Query("SELECT * FROM car_washes WHERE city = :city ORDER BY rating DESC")
    suspend fun getCarWashesByCity(city: String): List<CarWash>

    @Query("SELECT * FROM car_washes WHERE city = :city AND name LIKE '%' || :searchQuery || '%' ORDER BY rating DESC")
    suspend fun searchByName(city: String, searchQuery: String): List<CarWash>

    @Query("""
        SELECT DISTINCT cw.* FROM car_washes cw
        INNER JOIN car_wash_services cws ON cw.carWashId = cws.carWashId
        INNER JOIN services s ON cws.serviceId = s.serviceId
        WHERE cw.city = :city AND s.name IN (:serviceNames)
        ORDER BY cw.rating DESC
    """)
    suspend fun filterByServices(city: String, serviceNames: List<String>): List<CarWash>

    @Query("SELECT name FROM car_washes WHERE carWashId = :carWashId")
    suspend fun getCarWashNameById(carWashId: Int): String?

    @Query("SELECT * FROM car_washes WHERE ownerId = :ownerId")
    suspend fun getCarWashByOwnerId(ownerId: String): CarWash?

    @Query("""
        SELECT s.name FROM car_wash_services cws
        INNER JOIN services s ON cws.serviceId = s.serviceId
        WHERE cws.carWashId = :carWashId
        ORDER BY s.name ASC
    """)
    suspend fun getServiceNamesByCarWashId(carWashId: Int): List<String>

    @Query("SELECT ownerId FROM car_washes WHERE carWashId = :carWashId")
    suspend fun getOwnerIdByWashId(carWashId: Int): String
}