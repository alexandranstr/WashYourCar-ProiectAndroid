package com.example.washyourcar.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.washyourcar.models.ForecastResponse
import com.example.washyourcar.models.HourlyWeather
import com.example.washyourcar.repository.WeatherRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()
    val weatherList = mutableStateListOf<HourlyWeather>()

    var errorMessage = ""

    fun fetchWeather(city: String, apiKey: String){
        repository.fetchWeatherForCity(city, apiKey, object: Callback<ForecastResponse>{
            override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>){
                if(response.isSuccessful && response.body() != null){
                    weatherList.clear()
                    weatherList.addAll(response.body()!!.list)
                    println("VREME SUCCES: Am primit ${weatherList.size} prognoze!")
                }else{
                    errorMessage = "Eroare de la server"
                    println("VREME EROARE SERVER: Cod ${response.code()}, Mesaj: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ForecastResponse>, t: Throwable){
                errorMessage = "Eroare conexiune: ${t.message}"
                println("VREME CRASH/FAIL: ${t.localizedMessage}")
                t.printStackTrace()
            }
        })
    }
}