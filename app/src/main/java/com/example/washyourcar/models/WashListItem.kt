package com.example.washyourcar.models

import com.example.washyourcar.data.entities.CarWash;
import com.example.washyourcar.data.entities.AutoWash;
import com.example.washyourcar.data.entities.SelfWash;
import com.example.washyourcar.data.entities.WashType;

data class WashListItem(
    val id: Int,
    val type: WashType,
    val name: String,
    val address: String,
    val city: String,
    val rating: Float,
    val openTime: Long,
    val closeTime: Long
) {
    val key: String get() = "${type.name}-$id"
}

fun CarWash.toListItem() = WashListItem(carWashId, WashType.NORMAL, name, address, city, rating, openTime, closeTime)
fun AutoWash.toListItem() = WashListItem(autoWashId, WashType.AUTOMATIC, name, address, city, rating, openTime, closeTime)
fun SelfWash.toListItem() = WashListItem(selfWashId, WashType.SELF, name, address, city, rating, openTime, closeTime)