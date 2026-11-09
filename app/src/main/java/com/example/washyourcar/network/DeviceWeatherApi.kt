package com.example.washyourcar.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DeviceWeather(
    val temperatureC: Int,
    val emoji: String,
    val description: String,
    val city: String?
)


object DeviceWeatherApi {

    suspend fun fetch(): DeviceWeather? = withContext(Dispatchers.IO) {
        try {
            val loc = JSONObject(httpGet("https://ipwho.is/"))
            if (!loc.optBoolean("success", false)) return@withContext null

            val lat = loc.getDouble("latitude")
            val lon = loc.getDouble("longitude")
            val city = loc.optString("city").ifBlank { null }

            val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,is_day&timezone=auto"
            val current = JSONObject(httpGet(url)).getJSONObject("current")

            val temp = Math.round(current.getDouble("temperature_2m")).toInt()
            val code = current.getInt("weather_code")
            val isDay = current.optInt("is_day", 1) == 1
            val (emoji, text) = describe(code, isDay)

            DeviceWeather(temp, emoji, text, city)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun httpGet(address: String): String {
        val conn = URL(address).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "WashYourCar/1.0")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("HTTP ${conn.responseCode}")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun describe(code: Int, isDay: Boolean): Pair<String, String> = when (code) {
        0 -> if (isDay) "☀️" to "Sunny" else "🌙" to "Clear"
        1 -> if (isDay) "🌤️" to "Mostly sunny" else "🌙" to "Mostly clear"
        2 -> if (isDay) "⛅" to "Partly cloudy" else "☁️" to "Partly cloudy"
        3 -> "☁️" to "Cloudy"
        45, 48 -> "🌫️" to "Fog"
        51, 53, 55, 56, 57 -> "🌦️" to "Drizzle"
        61, 63, 65, 66, 67 -> "🌧️" to "Rain"
        71, 73, 75, 77 -> "❄️" to "Snow"
        80, 81, 82 -> "🌧️" to "Showers"
        85, 86 -> "❄️" to "Snow showers"
        95, 96, 99 -> "⛈️" to "Thunderstorm"
        else -> "🌡️" to "Weather"
    }
}