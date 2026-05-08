package com.example.washyourcar.data.repository
import com.example.washyourcar.data.network.ApiService

class AppRepository(private val apiService: ApiService) {
    suspend fun getServices() = apiService.getServices()
    suspend fun getTips() = apiService.getTips()
}