package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.*

/** Read-all / upsert for every table that CloudSync mirrors to Firestore. */
@Dao
interface SyncDao {
    @Query("SELECT * FROM owners") suspend fun owners(): List<Owner>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertOwners(items: List<Owner>)

    @Query("SELECT * FROM customers") suspend fun customers(): List<Customer>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCustomers(items: List<Customer>)

    @Query("SELECT * FROM cars") suspend fun cars(): List<Car>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCars(items: List<Car>)

    @Query("SELECT * FROM employees") suspend fun employees(): List<Employee>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertEmployees(items: List<Employee>)

    @Query("SELECT * FROM car_washes") suspend fun carWashes(): List<CarWash>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCarWashes(items: List<CarWash>)

    @Query("SELECT * FROM self_washes") suspend fun selfWashes(): List<SelfWash>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSelfWashes(items: List<SelfWash>)

    @Query("SELECT * FROM auto_washes") suspend fun autoWashes(): List<AutoWash>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAutoWashes(items: List<AutoWash>)

    @Query("SELECT * FROM car_wash_services") suspend fun carWashServices(): List<CarWashService>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCarWashServices(items: List<CarWashService>)

    @Query("SELECT * FROM self_wash_services") suspend fun selfWashServices(): List<SelfWashService>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSelfWashServices(items: List<SelfWashService>)

    @Query("SELECT * FROM auto_wash_services") suspend fun autoWashServices(): List<AutoWashService>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAutoWashServices(items: List<AutoWashService>)

    @Query("SELECT * FROM appointments") suspend fun appointments(): List<Appointment>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAppointments(items: List<Appointment>)

    @Query("SELECT * FROM reviews") suspend fun reviews(): List<Review>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertReviews(items: List<Review>)

    @Query("SELECT * FROM loyalty_cards") suspend fun loyaltyCards(): List<LoyaltyCard>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLoyaltyCards(items: List<LoyaltyCard>)
}
