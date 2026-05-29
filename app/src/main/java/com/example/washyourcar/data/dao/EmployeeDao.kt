package com.example.washyourcar.data.dao

import android.provider.ContactsContract
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.Employee

@Dao
interface EmployeeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: Employee)

    @Query("SELECT firebaseUid FROM employees WHERE email = :email AND phoneNumber = :phoneNumber")
    suspend fun getEmployeeIdByContact(email: String, phoneNumber: String) : String?

    @Query("SELECT carWashId FROM employees WHERE firebaseUid = :uid")
    suspend fun getCarWashIdByEmployee(uid: String): Int?

    @Query("SELECT * FROM employees WHERE firebaseUid = :uid")
    suspend fun getById(uid: String): Employee?

    @Query("UPDATE employees SET carWashId = NULL WHERE firebaseUid = :uid")
    suspend fun removeEmployeeFromCarWash(uid: String)

    @Query("SELECT * FROM employees WHERE email = :email AND carWashId IS NOT NULL")
    suspend fun getActivieEmployee(email: String): Employee?
}