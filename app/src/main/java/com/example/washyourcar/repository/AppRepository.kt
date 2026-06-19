package com.example.washyourcar.data.repository
import com.example.washyourcar.data.network.ApiService

class AppRepository(private val apiService: ApiService) {
    suspend fun getTips() = apiService.getTips()
}