package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.Car

@Dao
interface CarDao {
    @Query("SELECT EXISTS(SELECT 1 FROM cars WHERE UPPER(licensePlate) = :plate)")
    suspend fun licensePlateExists(plate: String): Boolean

    @Query("SELECT * FROM cars WHERE customerId = :customerId")
    fun observeCars(customerId: String): kotlinx.coroutines.flow.Flow<List<Car>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(car: Car)

    @Query("SELECT * FROM cars WHERE customerId = :customerId")
    suspend fun getCarsByCustomerId(customerId: String): List<Car>

    @Query("DELETE FROM cars WHERE licensePlate = :licensePlate")
    suspend fun deleteCar(licensePlate: String)
}