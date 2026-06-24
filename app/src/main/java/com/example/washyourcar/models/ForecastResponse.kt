package com.example.washyourcar.models

import com.google.gson.annotations.SerializedName

data class ForecastResponse (
    val list: List<HourlyWeather>
    )

data class HourlyWeather(
    val main: MainData,
    val dt_txt: String
)

data class MainData(
    val temp: Double
)