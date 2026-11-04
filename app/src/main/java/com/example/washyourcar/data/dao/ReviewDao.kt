package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.Review
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review): Long

    @Query("SELECT * FROM reviews WHERE carWashId = :carWashId ORDER BY timestamp DESC")
    fun getReviewsByCarWashId(carWashId: Int): Flow<List<Review>>

    @Query("SELECT * FROM reviews WHERE carWashId = :carWashId ORDER BY timestamp DESC")
    suspend fun getReviewsByCarWashIdOnce(carWashId: Int): List<Review>

    @Query("SELECT AVG(rating) FROM reviews WHERE carWashId = :carWashId")
    suspend fun getAverageRating(carWashId: Int): Float?

    @Query("SELECT COUNT(*) FROM reviews WHERE carWashId = :carWashId")
    suspend fun getReviewCount(carWashId: Int): Int

    @Query("SELECT * FROM reviews WHERE carWashId = :carWashId AND customerId = :customerId LIMIT 1")
    suspend fun getCustomerReviewForCarWash(carWashId: Int, customerId: String): Review?

    @Query("DELETE FROM reviews WHERE reviewId = :reviewId")
    suspend fun deleteReview(reviewId: String)
}
