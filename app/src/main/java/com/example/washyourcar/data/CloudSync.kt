package com.example.washyourcar.data

import AppDatabase
import android.util.Log
import androidx.room.InvalidationTracker
import com.example.washyourcar.data.entities.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Keeps a permanent copy of the app's data in Cloud Firestore, so car washes, profiles, bookings,
 * reviews and loyalty cards survive a reinstall, a wiped local database or a new device.
 *
 * The app keeps reading/writing the local Room database exactly as before:
 *  - push: whenever a synced table changes, only the rows that actually changed are uploaded,
 *          and rows this device had synced that were deleted locally are deleted in the cloud;
 *  - pull: on app start and after login everything is downloaded and merged into Room.
 * One Firestore collection per table, one document per row (document id = primary key).
 * Requires a signed-in Firebase user (see Firestore security rules).
 */
object CloudSync {
    private const val TAG = "CloudSync"
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private lateinit var db: AppDatabase
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /** table -> (document id -> hash of the row as last synced). */
    private val synced = mutableMapOf<String, MutableMap<String, Int>>()
    @Volatile private var pulling = false
    private var pushJob: Job? = null

    private class Table<T : Any>(
        val name: String,
        val type: Class<T>,
        val id: (T) -> String,
        val readAll: suspend () -> List<T>,
        val upsert: suspend (List<T>) -> Unit
    )

    private val tables: List<Table<*>> by lazy {
        val d = db.syncDao()
        listOf(
            Table("owners", Owner::class.java, { it.firebaseUid }, d::owners, d::upsertOwners),
            Table("customers", Customer::class.java, { it.firebaseUid }, d::customers, d::upsertCustomers),
            Table("cars", Car::class.java, { it.licensePlate }, d::cars, d::upsertCars),
            Table("employees", Employee::class.java, { it.firebaseUid }, d::employees, d::upsertEmployees),
            Table("car_washes", CarWash::class.java, { it.carWashId.toString() }, d::carWashes, d::upsertCarWashes),
            Table("self_washes", SelfWash::class.java, { it.selfWashId.toString() }, d::selfWashes, d::upsertSelfWashes),
            Table("auto_washes", AutoWash::class.java, { it.autoWashId.toString() }, d::autoWashes, d::upsertAutoWashes),
            Table("car_wash_services", CarWashService::class.java, { "${it.carWashId}_${it.serviceId}" }, d::carWashServices, d::upsertCarWashServices),
            Table("self_wash_services", SelfWashService::class.java, { "${it.selfWashId}_${it.selfServiceId}" }, d::selfWashServices, d::upsertSelfWashServices),
            Table("auto_wash_services", AutoWashService::class.java, { "${it.autoWashId}_${it.autoServiceId}" }, d::autoWashServices, d::upsertAutoWashServices),
            Table("appointments", Appointment::class.java, { it.appointmentId }, d::appointments, d::upsertAppointments),
            Table("reviews", Review::class.java, { it.reviewId }, d::reviews, d::upsertReviews),
            Table("loyalty_cards", LoyaltyCard::class.java, { it.customerUid }, d::loyaltyCards, d::upsertLoyaltyCards)
        )
    }

    private fun signedIn() = FirebaseAuth.getInstance().currentUser != null

    /** Call once (MainActivity). Starts watching local tables and pushing changes. */
    fun start(database: AppDatabase) {
        if (::db.isInitialized) return
        db = database
        db.invalidationTracker.addObserver(object : InvalidationTracker.Observer(tables.map { it.name }.toTypedArray()) {
            override fun onInvalidated(tables: Set<String>) {
                if (!pulling) schedulePush()
            }
        })
    }

    /** Debounced so a burst of writes becomes one upload. */
    fun schedulePush() {
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(1_500)
            pushChanges()
        }
    }

    /** Downloads every synced collection and merges it into Room. Returns false if offline/not allowed. */
    suspend fun pullAll(): Boolean {
        if (!::db.isInitialized || !signedIn()) return false
        return mutex.withLock {
            pulling = true
            try {
                for (table in tables) pullTable(table)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Pull failed (is Firestore enabled with rules allowing signed-in users?)", e)
                false
            } finally {
                pulling = false
            }
        }.also { if (it) schedulePush() } // upload anything created locally while offline
    }

    private suspend fun <T : Any> pullTable(table: Table<T>) {
        val snapshot = firestore.collection(table.name).get().await()
        val rows = snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            try { gson.fromJson(gson.toJson(data), table.type) } catch (e: Exception) {
                Log.w(TAG, "Skipping bad ${table.name}/${doc.id}", e); null
            }
        }
        if (rows.isNotEmpty()) table.upsert(rows)
        // Remember what the cloud has, so the next push only uploads real changes.
        synced[table.name] = rows.associate { table.id(it) to gson.toJson(it).hashCode() }.toMutableMap()
        Log.d(TAG, "Pulled ${rows.size} ${table.name}")
    }

    /** Uploads rows that changed since the last sync and deletes rows removed locally. */
    suspend fun pushChanges() {
        if (!::db.isInitialized || !signedIn()) return
        mutex.withLock {
            try {
                for (table in tables) pushTable(table)
            } catch (e: Exception) {
                Log.e(TAG, "Push failed (is Firestore enabled with rules allowing signed-in users?)", e)
            }
        }
    }

    private suspend fun <T : Any> pushTable(table: Table<T>) {
        val known = synced.getOrPut(table.name) { mutableMapOf() }
        val local = table.readAll().associateBy { table.id(it) }
        val changed = local.filter { (id, row) -> known[id] != gson.toJson(row).hashCode() }
        // Only rows this device had synced can be "deleted"; rows it never saw are left alone.
        val deleted = known.keys - local.keys
        if (changed.isEmpty() && deleted.isEmpty()) return

        val collection = firestore.collection(table.name)
        val ops = changed.map { (id, row) -> { b: com.google.firebase.firestore.WriteBatch -> b.set(collection.document(id), toFirestoreMap(row)); Unit } } +
            deleted.map { id -> { b: com.google.firebase.firestore.WriteBatch -> b.delete(collection.document(id)); Unit } }
        ops.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { it(batch) }
            batch.commit().await()
        }
        changed.forEach { (id, row) -> known[id] = gson.toJson(row).hashCode() }
        deleted.forEach { known.remove(it) }
        Log.d(TAG, "Pushed ${changed.size} / deleted ${deleted.size} ${table.name}")
    }

    /** Entity -> Firestore map with proper number types (whole numbers as Long). */
    private fun toFirestoreMap(row: Any): Map<String, Any?> =
        JsonParser.parseString(gson.toJson(row)).asJsonObject.entrySet().associate { (k, v) -> k to toValue(v) }

    private fun toValue(e: JsonElement): Any? = when {
        e.isJsonNull -> null
        e.isJsonArray -> e.asJsonArray.map { toValue(it) }
        e.isJsonObject -> e.asJsonObject.entrySet().associate { (k, v) -> k to toValue(v) }
        e.asJsonPrimitive.isBoolean -> e.asBoolean
        e.asJsonPrimitive.isNumber -> e.asDouble.let { d -> if (d % 1.0 == 0.0 && kotlin.math.abs(d) < 9e15) d.toLong() else d }
        else -> e.asString
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}
