package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.LoyaltyCard
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {
    @Query("SELECT * FROM loyalty_cards WHERE customerUid = :uid")
    fun observeCard(uid: String): Flow<LoyaltyCard?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: LoyaltyCard)

    // Each top-up adds 1 point for every 10 RON.
    @Query("UPDATE loyalty_cards SET balance = balance + :amount, points = points + CAST(:amount / 10 AS INTEGER) WHERE customerUid = :uid")
    suspend fun topUp(uid: String, amount: Double)
}
