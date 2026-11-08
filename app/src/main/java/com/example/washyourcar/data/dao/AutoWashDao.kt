package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.AutoWash

@Dao
interface AutoWashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAndReturnId(autoWash: AutoWash): Long

    @Query("SELECT EXISTS(SELECT 1 FROM auto_washes WHERE accessCode = :code)")
    suspend fun doesAccessCodeExist(code: String): Boolean

    @Query("SELECT autoWashId FROM auto_washes WHERE accessCode = :code")
    suspend fun getAutoWashIdByAccessCode(code: String): Int?

    @Query("SELECT DISTINCT city FROM auto_washes ORDER BY city ASC")
    suspend fun getAllCities(): List<String>

    @Query("SELECT * FROM auto_washes ORDER BY rating DESC")
    suspend fun getAllAutoWashes(): List<AutoWash>

    @Query("SELECT * FROM auto_washes WHERE (:city IS NULL OR city = :city) ORDER BY rating DESC")
    suspend fun getAutoWashesByCity(city: String?): List<AutoWash>

    @Query("SELECT * FROM auto_washes WHERE (:city IS NULL OR city = :city) AND name LIKE '%' || :searchQuery || '%' ORDER BY rating DESC")
    suspend fun searchByName(city: String?, searchQuery: String): List<AutoWash>

    @Query("""
        SELECT DISTINCT aw.* FROM auto_washes aw
        INNER JOIN auto_wash_services aws ON aw.autoWashId = aws.autoWashId
        INNER JOIN auto_services s ON aws.autoServiceId = s.autoServiceId
        WHERE (:city IS NULL OR aw.city = :city) AND s.name IN (:serviceNames)
        ORDER BY aw.rating DESC
    """)
    suspend fun filterByServices(city: String?, serviceNames: List<String>): List<AutoWash>

    @Query("SELECT name FROM auto_washes WHERE autoWashId = :autoWashId")
    suspend fun getAutoWashNameById(autoWashId: Int): String?

    @Query("SELECT * FROM auto_washes WHERE ownerId = :ownerId")
    suspend fun getAutoWashByOwnerId(ownerId: String): AutoWash?

    @Query("""
        SELECT s.name FROM auto_wash_services aws
        INNER JOIN auto_services s ON aws.autoServiceId = s.autoServiceId
        WHERE aws.autoWashId = :autoWashId
        ORDER BY s.name ASC
    """)
    suspend fun getServiceNamesByAutoWashId(autoWashId: Int): List<String>

    @Query("SELECT ownerId FROM auto_washes WHERE autoWashId = :autoWashId")
    suspend fun getOwnerIdByWashId(autoWashId: Int): String

    @Query("SELECT * FROM auto_washes WHERE autoWashId = :autoWashId")
    suspend fun getAutoWashById(autoWashId: Int): AutoWash?
}