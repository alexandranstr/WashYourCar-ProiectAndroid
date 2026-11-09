package com.example.washyourcar.network

import android.util.Log
import com.example.washyourcar.BuildConfig
import com.example.washyourcar.models.NeedService
import com.example.washyourcar.models.NeedWhen
import com.example.washyourcar.models.Priority
import com.example.washyourcar.models.WashNeeds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Turns a free-text request ("quick wash in 30 min under 50 lei") into structured [WashNeeds]
 * using Groq's openai/gpt-oss-120b. The AI only understands the request; ranking happens on-device.
 * Falls back to a keyword parser when there's no key, no network, or a bad answer.
 *
 * Note: the key is compiled into the app (BuildConfig), so it can be extracted from the APK.
 * Fine for a student project; for a public release, call Groq from a small backend instead.
 */
object GroqNeedsParser {
    private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val MODEL = "openai/gpt-oss-120b"

    data class Result(val needs: WashNeeds, val usedAi: Boolean, val error: String? = null)

    private val systemPrompt = """
        You turn a driver's car-wash request into JSON. Answer with ONLY a JSON object:
        {
          "services": array of any of ["EXTERIOR","INTERIOR","DETAILING","SELF_SERVICE","AUTOMATIC"],
          "time": one of ["NOW","IN_ONE_HOUR","LATER_TODAY"],
          "priorities": array (1-2 items) of ["CLOSEST","SHORTEST_WAIT","CHEAPEST","BEST_RATED"],
          "maxBudget": number in RON or null,
          "maxDistanceKm": number or null,
          "summary": short restatement in the user's language, max 12 words
        }
        Rules: "quick/fast/basic wash" -> EXTERIOR + SHORTEST_WAIT. "full/inside/interior/vacuum" -> EXTERIOR + INTERIOR.
        "wax/polish/detailing/shine" -> DETAILING. "I'll wash it myself / jet wash / self" -> SELF_SERVICE.
        "tunnel/automatic/machine" -> AUTOMATIC. "cheap/budget" -> CHEAPEST. "near/close/nearby" -> CLOSEST.
        "good/best/reviews" -> BEST_RATED. Prices in lei/ron/euro: convert euro to RON at x5.
        "in 30 min/now/asap" -> NOW; "in an hour" -> IN_ONE_HOUR; "this evening/later/after work" -> LATER_TODAY.
        If services are unclear use ["EXTERIOR"]. If priorities are unclear use ["SHORTEST_WAIT"].
    """.trimIndent()

    suspend fun parse(text: String): Result = withContext(Dispatchers.IO) {
        val key = BuildConfig.GROQ_API_KEY
        if (key.isBlank()) return@withContext Result(keywordParse(text), usedAi = false, error = "No GROQ_API_KEY set")
        try {
            val body = JSONObject()
                .put("model", MODEL)
                .put("temperature", 0.1)
                .put("response_format", JSONObject().put("type", "json_object"))
                .put(
                    "messages", JSONArray()
                        .put(JSONObject().put("role", "system").put("content", systemPrompt))
                        .put(JSONObject().put("role", "user").put("content", text))
                )
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val response = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) {
                Log.e("GroqNeedsParser", "HTTP $code: $response")
                return@withContext Result(keywordParse(text), usedAi = false, error = "AI unavailable (HTTP $code)")
            }
            val content = JSONObject(response).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            Result(fromJson(JSONObject(content)), usedAi = true)
        } catch (e: Exception) {
            Log.e("GroqNeedsParser", "AI parse failed", e)
            Result(keywordParse(text), usedAi = false, error = "AI unavailable, used quick match")
        }
    }

    private fun fromJson(o: JSONObject): WashNeeds {
        fun <T : Enum<T>> list(name: String, values: Array<T>): Set<T> {
            val arr = o.optJSONArray(name) ?: return emptySet()
            return (0 until arr.length()).mapNotNull { i -> values.firstOrNull { it.name == arr.optString(i) } }.toSet()
        }
        val services = list("services", NeedService.entries.toTypedArray()).ifEmpty { setOf(NeedService.EXTERIOR) }
        val priorities = list("priorities", Priority.entries.toTypedArray()).ifEmpty { setOf(Priority.SHORTEST_WAIT) }
        val time = NeedWhen.entries.firstOrNull { it.name == o.optString("time") } ?: NeedWhen.NOW
        fun num(name: String) = if (o.isNull(name)) null else o.optDouble(name).takeIf { !it.isNaN() && it > 0 }
        return WashNeeds(services, time, priorities, num("maxBudget"), num("maxDistanceKm"), o.optString("summary").ifBlank { null })
    }

    /** Offline fallback: simple keyword matching (English + Romanian). */
    fun keywordParse(text: String): WashNeeds {
        val t = text.lowercase()
        fun has(vararg words: String) = words.any { it in t }
        val services = buildSet {
            if (has("interior", "inside", "vacuum", "aspir", "interi")) { add(NeedService.EXTERIOR); add(NeedService.INTERIOR) }
            if (has("wax", "polish", "detail", "ceara", "lustru")) add(NeedService.DETAILING)
            if (has("myself", "self", "jet", "singur")) add(NeedService.SELF_SERVICE)
            if (has("automatic", "tunnel", "automat")) add(NeedService.AUTOMATIC)
            if (isEmpty()) add(NeedService.EXTERIOR)
        }
        val priorities = buildSet {
            if (has("cheap", "budget", "ieftin")) add(Priority.CHEAPEST)
            if (has("near", "close", "aproape")) add(Priority.CLOSEST)
            if (has("best", "good", "review", "bun")) add(Priority.BEST_RATED)
            if (has("quick", "fast", "rapid", "repede", "now", "acum") || isEmpty()) add(Priority.SHORTEST_WAIT)
        }
        val time = when {
            has("evening", "later", "seara", "after work") -> NeedWhen.LATER_TODAY
            has("hour", "ora") -> NeedWhen.IN_ONE_HOUR
            else -> NeedWhen.NOW
        }
        val budget = Regex("""(\d+)\s*(lei|ron|eur|€)""").find(t)?.let { m ->
            val v = m.groupValues[1].toDouble()
            if (m.groupValues[2] in listOf("eur", "€")) v * 5 else v
        }
        val distance = Regex("""(\d+(?:[.,]\d+)?)\s*km""").find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        return WashNeeds(services, time, priorities, budget, distance)
    }
}
