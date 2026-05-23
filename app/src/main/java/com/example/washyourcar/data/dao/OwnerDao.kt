package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.Owner

@Dao
interface OwnerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwner(owner: Owner)

    @Query("SELECT * FROM owners WHERE email = :email AND phoneNumber = :phoneNumber")
    suspend fun getOwnerByContact(email: String, phoneNumber: String): Owner?

    @Query("SELECT firebaseUid FROM owners WHERE email = :email AND phoneNumber = :phoneNumber")
    suspend fun getOwnerId(email: String, phoneNumber: String): String?

    @Query("SELECT * FROM owners WHERE firebaseUid = :uid")
    suspend fun getById(uid: String): Owner?
}