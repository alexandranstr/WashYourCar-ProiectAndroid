package com.example.washyourcar.repository

import com.example.washyourcar.models.ForecastResponse
import com.example.washyourcar.network.WeatherApiService
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {
    private val apiService: WeatherApiService

    init{
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(WeatherApiService::class.java)
    }

    fun fetchWeatherForCity(city: String, apiKey: String, callback: Callback<ForecastResponse>){
        val call = apiService.getHourlyForecast(city, apiKey)
        call.enqueue(callback)
    }
}