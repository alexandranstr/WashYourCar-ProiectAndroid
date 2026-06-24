package com.example.washyourcar.models

import com.google.gson.annotations.SerializedName

data class ForecastResponse (
    @SerializedName("list")
    val hourlyForecasts : List<HourlyWeather>
    )

data class HourlyWeather(
    @SerializedName("main") val mainData: MainData,
    @SerializedName("dt_txt") val dateTime: String
)

data class MainData(
    @SerializedName("temp") val temperature: Double
)