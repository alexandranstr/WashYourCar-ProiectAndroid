package com.example.washyourcar.data.network
import retrofit2.http.GET
import com.example.washyourcar.models.*

interface ApiService {
    @GET("services.json")
    suspend fun getServices(): List<CarService>

    @GET("tips.json")
    suspend fun getTips(): List<WashTips>
}