package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface AccountDao {

    @Query("""
        SELECT EXISTS(
        SELECT 1 FROM customers WHERE email = :email AND phoneNumber = :phoneNumber
        UNION ALL 
        SELECT 1 FROM employees WHERE email = :email AND phoneNumber = :phoneNumber
        UNION ALL
        SELECT 1 FROM owners WHERE email = :email AND phoneNumber = :phoneNumber
        )
    """)
    suspend fun  checkUniversalIdentity(email: String, phoneNumber: String): Boolean
}