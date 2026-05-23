package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.Customer

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer)

    @Query("SELECT * FROM customers WHERE email= :email AND phoneNumber = :phoneNumber")
    suspend fun getCustomerByContact(email: String, phoneNumber: String): Customer?

    @Query("SELECT firebaseUid FROM customers WHERE email = :email AND phoneNumber = :phoneNumber")
    suspend fun getCustomerId(email: String, phoneNumber: String): String?

    @Query("SELECT * FROM customers WHERE firebaseUid = :uid")
    suspend fun getCustomersById(uid: String): Customer?

    @Query("SELECT (firstName || ' ' || lastName) FROM customers WHERE firebaseUid = :uid")
    suspend fun getCustomerFullName(uid: String): String?
}