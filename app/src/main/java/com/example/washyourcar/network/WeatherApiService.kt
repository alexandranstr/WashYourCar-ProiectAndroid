package com.example.washyourcar.network

import com.example.washyourcar.models.ForecastResponse
import retrofit2.http.GET
import retrofit2.Call
import retrofit2.http.Query

interface WeatherApiService {
    @GET("data/2.5/forecast")
    fun getHourlyForecast(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Call<ForecastResponse>
}