package com.example.washyourcar.models

import com.example.washyourcar.data.entities.Appointment
import kotlin.random.Random

enum class BayKind(val label: String, val minutesPerCar: Int, val bookable: Boolean) {
    SELF_NORMAL("Normal", 10, false),
    SELF_PREMIUM("Premium", 15, true),
    AUTOMATIC("Automatic", 8, true)
}

/**
 * State of one wash bay right now.
 * [number] is the bay number shown on screen; [carsInLine] counts cars waiting or washing;
 * [waitMinutes] is how long a car arriving now would wait.
 */
data class BayStatus(
    val number: Int,
    val kind: BayKind,
    val carsInLine: Int,
    val waitMinutes: Int,
    val bookedNow: Boolean,
    val nextBookingStart: Long?
) {
    val isFree: Boolean get() = carsInLine == 0 && !bookedNow
}

/** Summary used on the client list card and detail screen. */
data class BayAvailability(val freeBays: Int, val totalBays: Int, val shortestWaitMinutes: Int)

/**
 * The app has no sensors, so walk-in queues are simulated: numbers are deterministic per wash, bay and
 * 5-minute window (owner and client see the same values). Real bookings are layered on top.
 *
 * Bookings store the bay number within its bookable group: premium self-service bay 1, 2… or automatic bay 1, 2…
 * On screen, premium bays are numbered after the normal bays.
 */
object BayQueueSimulator {
    private const val WINDOW_MS = 5 * 60_000L
    private val walkInWeights = intArrayOf(0, 0, 0, 1, 1, 2, 3) // mostly short queues

    fun statuses(
        washKey: Int,
        normalBays: Int,
        premiumBays: Int,
        automaticBays: Int,
        bookings: List<Appointment>,
        now: Long = System.currentTimeMillis()
    ): List<BayStatus> {
        val result = mutableListOf<BayStatus>()
        repeat(normalBays) { i -> result += bay(washKey, i + 1, null, BayKind.SELF_NORMAL, bookings, now) }
        repeat(premiumBays) { i -> result += bay(washKey, normalBays + i + 1, i + 1, BayKind.SELF_PREMIUM, bookings, now) }
        repeat(automaticBays) { i -> result += bay(washKey, i + 1, i + 1, BayKind.AUTOMATIC, bookings, now) }
        return result
    }

    private fun bay(
        washKey: Int,
        displayNumber: Int,
        bookingNumber: Int?,
        kind: BayKind,
        bookings: List<Appointment>,
        now: Long
    ): BayStatus {
        val bayBookings = if (bookingNumber == null) emptyList() else bookings.filter { it.bayNumber == bookingNumber }
        val running = bayBookings.firstOrNull { it.startTime <= now && it.endTime > now }
        val next = bayBookings.filter { it.startTime > now }.minOfOrNull { it.startTime }
        val bookedRemaining = running?.let { ((it.endTime - now + 59_999) / 60_000).toInt() } ?: 0

        // Premium self-service bays are booking-only, so they have no walk-in queue.
        val walkIns = if (kind == BayKind.SELF_PREMIUM) 0 else {
            val seed = washKey * 31L + displayNumber * 7L + kind.ordinal * 1_000L + now / WINDOW_MS
            walkInWeights[Random(seed).nextInt(walkInWeights.size)]
        }
        val cars = walkIns + if (running != null) 1 else 0
        val wait = bookedRemaining + walkIns * kind.minutesPerCar
        return BayStatus(displayNumber, kind, cars, wait, running != null, next)
    }

    fun availability(statuses: List<BayStatus>): BayAvailability = BayAvailability(
        freeBays = statuses.count { it.isFree },
        totalBays = statuses.size,
        shortestWaitMinutes = statuses.minOfOrNull { it.waitMinutes } ?: 0
    )
}
