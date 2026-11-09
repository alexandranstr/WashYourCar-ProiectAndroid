package com.example.washyourcar.models

import com.example.washyourcar.data.entities.AutoWash
import com.example.washyourcar.data.entities.CarWash
import com.example.washyourcar.data.entities.SelfWash
import com.example.washyourcar.data.entities.WashType

data class WashListItem(
    val id: Int,
    val type: WashType,
    val name: String,
    val address: String,
    val city: String,
    val rating: Float,
    val openTime: Long,
    val closeTime: Long,
    val normalBays: Int = 0,
    val premiumBays: Int = 0,
    val automaticBays: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val key: String get() = "${type.name}-$id"
}

fun CarWash.toListItem() = WashListItem(carWashId, WashType.NORMAL, name, address, city, rating, openTime, closeTime,
    latitude = latitude, longitude = longitude)
fun AutoWash.toListItem() = WashListItem(autoWashId, WashType.AUTOMATIC, name, address, city, rating, openTime, closeTime,
    automaticBays = automaticBays, latitude = latitude, longitude = longitude)
fun SelfWash.toListItem() = WashListItem(selfWashId, WashType.SELF, name, address, city, rating, openTime, closeTime,
    normalBays = normalSelfWashBays, premiumBays = premiumSelfWashBays, latitude = latitude, longitude = longitude)
