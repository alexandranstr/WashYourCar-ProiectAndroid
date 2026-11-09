package com.example.washyourcar.models

import AppDatabase
import com.example.washyourcar.data.entities.WashType

/** One of the owner's car washes (an owner can run several, of any type). */
data class OwnerWashRef(val type: WashType, val id: Int, val name: String) {
    fun sameWash(other: OwnerWashRef?) = other != null && other.type == type && other.id == id
}

/** All washes owned by [ownerUid]: classic first, then self-service, then automatic. */
suspend fun AppDatabase.ownerWashes(ownerUid: String): List<OwnerWashRef> =
    carWashDao().getCarWashesByOwnerId(ownerUid).map { OwnerWashRef(WashType.NORMAL, it.carWashId, it.name) } +
        selfWashDao().getSelfWashesByOwnerId(ownerUid).map { OwnerWashRef(WashType.SELF, it.selfWashId, it.name) } +
        autoWashDao().getAutoWashesByOwnerId(ownerUid).map { OwnerWashRef(WashType.AUTOMATIC, it.autoWashId, it.name) }
