package com.example.washyourcar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.washyourcar.data.entities.SelfWash

@Dao
interface SelfWashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAndReturnId(selfWash: SelfWash): Long

    @Query("SELECT DISTINCT city FROM self_washes ORDER BY city ASC")
    suspend fun getAllCities(): List<String>

    @Query("SELECT * FROM self_washes ORDER BY rating DESC")
    suspend fun getAllSelfWashes(): List<SelfWash>

    @Query("SELECT * FROM self_washes WHERE (:city IS NULL OR city = :city) ORDER BY rating DESC")
    suspend fun getSelfWashesByCity(city: String?): List<SelfWash>

    @Query("SELECT * FROM self_washes WHERE (:city IS NULL OR city = :city) AND name LIKE '%' || :searchQuery || '%' ORDER BY rating DESC")
    suspend fun searchByName(city: String?, searchQuery: String): List<SelfWash>

    @Query("""
        SELECT DISTINCT sw.* FROM self_washes sw
        INNER JOIN self_wash_services sws ON sw.selfWashId = sws.selfWashId
        INNER JOIN self_services s ON sws.selfServiceId = s.selfServiceId
        WHERE (:city IS NULL OR sw.city = :city) AND s.name IN (:serviceNames)
        ORDER BY sw.rating DESC
    """)
    suspend fun filterByServices(city: String?, serviceNames: List<String>): List<SelfWash>

    @Query("SELECT name FROM self_washes WHERE selfWashId = :selfWashId")
    suspend fun getSelfWashNameById(selfWashId: Int): String?

    @Query("SELECT * FROM self_washes WHERE ownerId = :ownerId")
    suspend fun getSelfWashByOwnerId(ownerId: String): SelfWash?

    @Query("SELECT * FROM self_washes WHERE ownerId = :ownerId ORDER BY selfWashId")
    suspend fun getSelfWashesByOwnerId(ownerId: String): List<SelfWash>

    @Query("""
        SELECT s.name FROM self_wash_services sws
        INNER JOIN self_services s ON sws.selfServiceId = s.selfServiceId
        WHERE sws.selfWashId = :selfWashId
        ORDER BY s.name ASC
    """)
    suspend fun getServiceNamesBySelfWashId(selfWashId: Int): List<String>

    @Query("SELECT ownerId FROM self_washes WHERE selfWashId = :selfWashId")
    suspend fun getOwnerIdByWashId(selfWashId: Int): String

    @Query("SELECT * FROM self_washes WHERE selfWashId = :selfWashId")
    suspend fun getSelfWashById(selfWashId: Int): SelfWash?
}