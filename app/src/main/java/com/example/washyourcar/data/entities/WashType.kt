package com.example.washyourcar.data.entities

enum class WashType(val label: String) {
    NORMAL("Normal"),
    AUTOMATIC("Automatic"),
    SELF("Self-service")
}

/**
 * Classic, self and automatic washes each number their ids from 200, so the same id can exist
 * in all three tables. Reviews share one table keyed by an Int, so self/auto ids are offset.
 */
fun WashType.reviewKey(washId: Int): Int = when (this) {
    WashType.NORMAL -> washId
    WashType.SELF -> 100_000 + washId
    WashType.AUTOMATIC -> 200_000 + washId
}