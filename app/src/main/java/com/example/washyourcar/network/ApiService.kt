package com.example.washyourcar.data.network

import retrofit2.http.GET
import com.example.washyourcar.models.*

interface ApiService {
    @GET("tips.json")
    suspend fun getTips(): List<WashTips>
}
