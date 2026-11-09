package com.example.washyourcar.models

import com.example.washyourcar.data.entities.WashType
import kotlin.math.max
import kotlin.math.roundToInt

/** What the client wants done to the car. */
enum class NeedService(val label: String, val keywords: List<String>) {
    EXTERIOR("Exterior", listOf("exterior", "express", "basic", "classic", "shampoo", "foam", "warm water", "rinse")),
    INTERIOR("Interior", listOf("interior", "vacuum")),
    DETAILING("Detailing / wax", listOf("detailing", "polish", "wax", "deluxe", "ultimate", "protection")),
    SELF_SERVICE("Self-service", emptyList()),
    AUTOMATIC("Automatic", emptyList());
}

enum class NeedWhen(val label: String, val minutesFromNow: Int) {
    NOW("Right now", 0),
    IN_ONE_HOUR("In ~1 hour", 60),
    LATER_TODAY("Later today", 180)
}

enum class Priority(val label: String) {
    CLOSEST("Closest"),
    SHORTEST_WAIT("Shortest wait"),
    CHEAPEST("Cheapest"),
    BEST_RATED("Best rated")
}

/** The client's needs, from the chips or parsed by the AI from free text. */
data class WashNeeds(
    val services: Set<NeedService> = setOf(NeedService.EXTERIOR),
    val time: NeedWhen = NeedWhen.NOW,
    val priorities: Set<Priority> = setOf(Priority.SHORTEST_WAIT),
    val maxBudget: Double? = null,
    val maxDistanceKm: Double? = null,
    /** One-line restatement shown back to the user ("Quick exterior wash now, under 50 RON"). */
    val summary: String? = null
)

/** A service a wash offers, with its price and duration. */
data class OfferedService(val name: String, val price: Double, val durationMinutes: Int)

/** Everything known about one wash at the moment of ranking. */
data class WashCandidate(
    val item: WashListItem,
    val distanceKm: Double?,
    /** Minutes until the client could start washing at the requested time; null = can't take the car. */
    val waitMinutes: Int?,
    val rating: Float?,
    val reviewCount: Int,
    val services: List<OfferedService>,
    val unavailableReason: String? = null
)

data class WashMatch(
    val candidate: WashCandidate,
    val score: Int,
    val price: Double?,
    val reasons: List<String>,
    /** For alternatives: how this one compares to the best match. */
    var tradeOff: String? = null
)

/**
 * Ranks car washes for a set of needs. Hard filters (type, budget, distance, availability) first,
 * then a weighted score where the client's priorities count extra. Runs fully on-device.
 */
object WashMatcher {

    fun rank(needs: WashNeeds, candidates: List<WashCandidate>): List<WashMatch> {
        val wantSelf = NeedService.SELF_SERVICE in needs.services
        val wantAuto = NeedService.AUTOMATIC in needs.services
        val categories = needs.services - NeedService.SELF_SERVICE - NeedService.AUTOMATIC

        val eligible = candidates.mapNotNull { c ->
            if (c.waitMinutes == null) return@mapNotNull null
            // Type preference: if they asked for self-service and/or automatic, only those types.
            if ((wantSelf || wantAuto) &&
                !((wantSelf && c.item.type == WashType.SELF) || (wantAuto && c.item.type == WashType.AUTOMATIC))
            ) return@mapNotNull null
            val matched = categories.associateWith { cat -> cheapestFor(cat, c.services) }
            val price = when {
                matched.isEmpty() -> c.services.minOfOrNull { it.price }
                else -> matched.values.filterNotNull().sumOf { it.price }.takeIf { matched.values.any { s -> s != null } }
            }
            val fit = if (categories.isEmpty()) 1.0 else matched.values.count { it != null }.toDouble() / categories.size
            if (fit == 0.0 && categories.isNotEmpty()) return@mapNotNull null
            if (needs.maxBudget != null && price != null && price > needs.maxBudget) return@mapNotNull null
            if (needs.maxDistanceKm != null && c.distanceKm != null && c.distanceKm > needs.maxDistanceKm) return@mapNotNull null
            Triple(c, price, fit to matched)
        }
        if (eligible.isEmpty()) return emptyList()

        val prices = eligible.mapNotNull { it.second }
        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 0.0
        val distanceRef = needs.maxDistanceKm ?: max(10.0, eligible.mapNotNull { it.first.distanceKm }.maxOrNull() ?: 10.0)

        fun weight(p: Priority, base: Double) = base + if (p in needs.priorities) 0.6 else 0.0
        val wDistance = weight(Priority.CLOSEST, 0.25)
        val wWait = weight(Priority.SHORTEST_WAIT, 0.25)
        val wPrice = weight(Priority.CHEAPEST, 0.2)
        val wRating = weight(Priority.BEST_RATED, 0.15)
        val wFit = 0.3

        val matches = eligible.map { (c, price, fitInfo) ->
            val (fit, matched) = fitInfo
            val sDistance = c.distanceKm?.let { 1.0 - (it / distanceRef).coerceIn(0.0, 1.0) } ?: 0.4
            val sWait = 1.0 - ((c.waitMinutes ?: 60) / 60.0).coerceIn(0.0, 1.0)
            val sPrice = if (price == null || maxPrice <= minPrice) 0.7 else 1.0 - (price - minPrice) / (maxPrice - minPrice)
            val sRating = (c.rating ?: 3.5f) / 5.0
            val total = wDistance * sDistance + wWait * sWait + wPrice * sPrice + wRating * sRating + wFit * fit
            val score = (100 * total / (wDistance + wWait + wPrice + wRating + wFit)).roundToInt()

            val reasons = buildList {
                c.distanceKm?.let { add(formatDistance(it) + " away") }
                add(
                    when {
                        c.waitMinutes == 0 && needs.time == NeedWhen.NOW -> "free now"
                        c.waitMinutes == 0 -> "free at your time"
                        else -> "~${c.waitMinutes} min wait"
                    }
                )
                price?.let { add("${it.roundToInt()} RON") }
                c.rating?.takeIf { c.reviewCount > 0 }?.let { add("%.1f★".format(it)) }
                matched.filterValues { it != null }.keys.forEach { add("has ${it.label.lowercase()}") }
                matched.filterValues { it == null }.keys.forEach { add("no ${it.label.lowercase()}") }
            }
            WashMatch(c, score, price, reasons)
        }.sortedByDescending { it.score }

        // Explain alternatives relative to the winner.
        val best = matches.first()
        matches.drop(1).forEach { it.tradeOff = tradeOff(best, it) }
        return matches
    }

    private fun cheapestFor(cat: NeedService, services: List<OfferedService>): OfferedService? =
        services.filter { s -> cat.keywords.any { s.name.contains(it, ignoreCase = true) } }.minByOrNull { it.price }

    private fun tradeOff(best: WashMatch, alt: WashMatch): String {
        val parts = mutableListOf<String>()
        val bp = best.price; val ap = alt.price
        if (bp != null && ap != null && ap < bp - 0.5) parts += "${(bp - ap).roundToInt()} RON cheaper"
        if (bp != null && ap != null && ap > bp + 0.5) parts += "${(ap - bp).roundToInt()} RON more"
        val bw = best.candidate.waitMinutes ?: 0; val aw = alt.candidate.waitMinutes ?: 0
        if (aw < bw) parts += "${bw - aw} min shorter wait" else if (aw > bw) parts += "${aw - bw} min longer wait"
        val bd = best.candidate.distanceKm; val ad = alt.candidate.distanceKm
        if (bd != null && ad != null && ad < bd - 0.1) parts += "closer" else if (bd != null && ad != null && ad > bd + 0.1) parts += "${formatDistance(ad - bd)} farther"
        val br = best.candidate.rating; val ar = alt.candidate.rating
        if (br != null && ar != null && ar > br + 0.1) parts += "better rated"
        return parts.joinToString(", ").ifEmpty { "similar option" }.replaceFirstChar { it.uppercase() }
    }

    fun formatDistance(km: Double): String = if (km < 1) "${(km * 1000).roundToInt()} m" else "%.1f km".format(km)
}
