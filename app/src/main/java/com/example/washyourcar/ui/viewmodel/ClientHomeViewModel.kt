package com.example.washyourcar.ui.viewmodel

import AppDatabase
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.entities.Appointment
import com.example.washyourcar.data.entities.Car
import com.example.washyourcar.data.entities.Customer
import com.example.washyourcar.data.entities.WashType
import com.example.washyourcar.data.entities.reviewKey
import com.example.washyourcar.data.entities.washKey
import com.example.washyourcar.models.WashListItem
import com.example.washyourcar.models.toListItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ClientHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val carWashDao = database.carWashDao()
    private val autoWashDao = database.autoWashDao()
    private val selfWashDao = database.selfWashDao()
    private val customerDao = database.customerDao()
    private val carDao = database.carDao()
    private val appointmentDao = database.appointmentDao()
    private val reviewDao = database.reviewDao()

    private val _washes = MutableStateFlow<List<WashListItem>>(emptyList())
    val washes: StateFlow<List<WashListItem>> = _washes.asStateFlow()

    // ---------------- "Find my wash" (needs -> best matches) ----------------

    private val prefs = application.getSharedPreferences("find_my_wash", android.content.Context.MODE_PRIVATE)

    private val _needs = MutableStateFlow(loadSavedNeeds())
    val needs: StateFlow<com.example.washyourcar.models.WashNeeds> = _needs.asStateFlow()

    private val _matches = MutableStateFlow<List<com.example.washyourcar.models.WashMatch>?>(null)
    /** null = not searched yet; empty = nothing fits. */
    val matches: StateFlow<List<com.example.washyourcar.models.WashMatch>?> = _matches.asStateFlow()

    private val _matchBusy = MutableStateFlow(false)
    val matchBusy: StateFlow<Boolean> = _matchBusy.asStateFlow()

    private val _aiNote = MutableStateFlow<String?>(null)
    /** What the AI understood (or why it fell back to keywords). */
    val aiNote: StateFlow<String?> = _aiNote.asStateFlow()

    fun updateNeeds(needs: com.example.washyourcar.models.WashNeeds) { _needs.value = needs }

    fun clearMatches() { _matches.value = null }

    /** Sends the typed request to the AI and fills the needs chips with what it understood. */
    fun understandRequest(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _matchBusy.value = true
            val result = com.example.washyourcar.network.GroqNeedsParser.parse(text)
            _needs.value = result.needs
            _aiNote.value = result.error ?: result.needs.summary?.let { "Got it: $it" }
            _matchBusy.value = false
        }
    }

    /** Ranks every wash placed on the map for the current needs, from the client's position. */
    fun findMatches(latitude: Double?, longitude: Double?) {
        viewModelScope.launch {
            _matchBusy.value = true
            val needs = _needs.value
            saveNeeds(needs)
            val candidates = buildCandidates(needs, latitude, longitude)
            _matches.value = com.example.washyourcar.models.WashMatcher.rank(needs, candidates)
            _matchBusy.value = false
        }
    }

    private val carWashServiceDao = database.carWashServiceDao()
    private val selfWashServiceDao = database.selfWashServiceDao()
    private val autoWashServiceDao = database.autoWashServiceDao()

    private suspend fun buildCandidates(
        needs: com.example.washyourcar.models.WashNeeds,
        lat: Double?,
        lng: Double?
    ): List<com.example.washyourcar.models.WashCandidate> {
        val now = System.currentTimeMillis()
        val target = now + needs.time.minutesFromNow * 60_000L
        val items = (carWashDao.getAllCarWashes().map { it.toListItem() } +
            selfWashDao.getAllSelfWashes().map { it.toListItem() } +
            autoWashDao.getAllAutoWashes().map { it.toListItem() })
            .filter { it.latitude != null && it.longitude != null } // only washes placed on the map

        return items.map { item ->
            val key = item.type.washKey(item.id)
            val services = when (item.type) {
                WashType.NORMAL -> carWashServiceDao.getServicesByCarWashId(item.id).map {
                    com.example.washyourcar.models.OfferedService(it.serviceName, it.carWashService.price.toDouble(), it.carWashService.duration)
                }
                WashType.SELF -> selfWashServiceDao.getServicesBySelfWashId(item.id).map {
                    com.example.washyourcar.models.OfferedService(it.serviceName, it.selfWashService.price.toDouble(), it.selfWashService.duration)
                }
                WashType.AUTOMATIC -> autoWashServiceDao.getServicesByAutoWashId(item.id).map {
                    com.example.washyourcar.models.OfferedService(it.serviceName, it.autoWashServices.price.toDouble(), it.autoWashServices.duration)
                }
            }
            val (wait, reason) = waitFor(item, key, target, now)
            com.example.washyourcar.models.WashCandidate(
                item = item,
                distanceKm = distanceKm(lat, lng, item.latitude, item.longitude),
                waitMinutes = wait,
                rating = reviewDao.getAverageRating(key),
                reviewCount = reviewDao.getReviewCount(key),
                services = services,
                unavailableReason = reason
            )
        }
    }

    /** Minutes from [target] until the car can be washed, or null with a reason. */
    private suspend fun waitFor(item: WashListItem, key: Int, target: Long, now: Long): Pair<Int?, String?> {
        if (!isOpenAt(item, target)) return null to "closed at that time"
        return when (item.type) {
            WashType.NORMAL -> {
                if (appointmentDao.countEmployees(item.id) == 0) return null to "no employees yet"
                // Next 15-minute step (up to 4 h) where an employee is free for 30 minutes.
                for (step in 0..16) {
                    val t = target + step * 15 * 60_000L
                    if (isOpenAt(item, t) && appointmentDao.getAvailableEmployees(item.id, t, t + 30 * 60_000L).isNotEmpty()) {
                        return step * 15 to null
                    }
                }
                null to "fully booked"
            }
            else -> {
                if (target - now > 30 * 60_000L) return 0 to null // queues further ahead aren't predictable
                val bookings = appointmentDao.getOpenBookings(key, now, now + 24 * 3_600_000L)
                val statuses = com.example.washyourcar.models.BayQueueSimulator.statuses(
                    key, item.normalBays, if (item.type == WashType.SELF) item.premiumBays else 0, item.automaticBays, bookings, now
                ).filter { it.kind != com.example.washyourcar.models.BayKind.SELF_PREMIUM } // premium bays need booking ahead
                (statuses.minOfOrNull { it.waitMinutes } ?: 0) to null
            }
        }
    }

    private fun isOpenAt(item: WashListItem, time: Long): Boolean {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = time }
        val hhmm = c.get(java.util.Calendar.HOUR_OF_DAY) * 100 + c.get(java.util.Calendar.MINUTE)
        return hhmm >= item.openTime && hhmm < item.closeTime.coerceAtLeast(item.openTime + 1)
    }

    private fun distanceKm(lat: Double?, lng: Double?, toLat: Double?, toLng: Double?): Double? {
        if (lat == null || lng == null || toLat == null || toLng == null) return null
        val out = FloatArray(1)
        android.location.Location.distanceBetween(lat, lng, toLat, toLng, out)
        return out[0] / 1000.0
    }

    private fun saveNeeds(n: com.example.washyourcar.models.WashNeeds) {
        prefs.edit()
            .putStringSet("services", n.services.map { it.name }.toSet())
            .putString("time", n.time.name)
            .putStringSet("priorities", n.priorities.map { it.name }.toSet())
            .putFloat("budget", n.maxBudget?.toFloat() ?: -1f)
            .putFloat("distance", n.maxDistanceKm?.toFloat() ?: -1f)
            .apply()
    }

    /** Last needs are remembered so a repeat client is two taps from a booking. */
    private fun loadSavedNeeds(): com.example.washyourcar.models.WashNeeds {
        val d = com.example.washyourcar.models.WashNeeds()
        val services = prefs.getStringSet("services", null)?.mapNotNull { s ->
            com.example.washyourcar.models.NeedService.entries.firstOrNull { it.name == s }
        }?.toSet()
        val priorities = prefs.getStringSet("priorities", null)?.mapNotNull { s ->
            com.example.washyourcar.models.Priority.entries.firstOrNull { it.name == s }
        }?.toSet()
        return d.copy(
            services = services?.ifEmpty { null } ?: d.services,
            time = com.example.washyourcar.models.NeedWhen.entries.firstOrNull { it.name == prefs.getString("time", null) } ?: d.time,
            priorities = priorities?.ifEmpty { null } ?: d.priorities,
            maxBudget = prefs.getFloat("budget", -1f).takeIf { it > 0 }?.toDouble(),
            maxDistanceKm = prefs.getFloat("distance", -1f).takeIf { it > 0 }?.toDouble()
        )
    }

    private val _selectedTypes = MutableStateFlow<Set<WashType>>(emptySet())
    val selectedTypes: StateFlow<Set<WashType>> = _selectedTypes.asStateFlow()

    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()

    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _carWashRatings = MutableStateFlow<Map<String, Pair<Float, Int>>>(emptyMap())
    val carWashRatings: StateFlow<Map<String, Pair<Float, Int>>> = _carWashRatings.asStateFlow()

    /** Bay bookings running right now, grouped by wash key (for the availability line on each card). */
    private val _runningBookings = MutableStateFlow<Map<Int, List<Appointment>>>(emptyMap())
    val runningBookings: StateFlow<Map<Int, List<Appointment>>> = _runningBookings.asStateFlow()

    fun refreshAvailability() {
        viewModelScope.launch {
            _runningBookings.value = appointmentDao.getRunningBookings(System.currentTimeMillis()).groupBy { it.carWashId }
        }
    }

    private val _currentCustomer = MutableStateFlow<Customer?>(null)
    val currentCustomer: StateFlow<Customer?> = _currentCustomer.asStateFlow()

    private val _customerCar = MutableStateFlow<Car?>(null)
    val customerCar: StateFlow<Car?> = _customerCar.asStateFlow()

    private val _activeAppointment = MutableStateFlow<Appointment?>(null)
    val activeAppointment: StateFlow<Appointment?> = _activeAppointment.asStateFlow()

    private var profileJob: Job? = null
    private val _profileLoading = MutableStateFlow(false)
    val profileLoading = _profileLoading.asStateFlow()
    private val _profileMessage = MutableStateFlow<String?>(null)
    val profileMessage = _profileMessage.asStateFlow()
    private val _signedInEmail = MutableStateFlow<String?>(null)
    val signedInEmail = _signedInEmail.asStateFlow()

    init {
        loadCities()
        refreshWashes()
    }

    private fun loadCities() {
        viewModelScope.launch {
            _cities.value = (carWashDao.getAllCities() +
                    autoWashDao.getAllCities() +
                    selfWashDao.getAllCities()).distinct().sorted()
        }
    }

    private fun refreshWashes() {
        viewModelScope.launch {
            val city = _selectedCity.value
            val query = _searchQuery.value.trim()
            val types = _selectedTypes.value.ifEmpty { WashType.entries.toSet() }
            val result = mutableListOf<WashListItem>()

            if (WashType.NORMAL in types) {
                val list = if (query.isEmpty()) carWashDao.getCarWashesByCity(city)
                else carWashDao.searchByName(city, query)
                result += list.map { it.toListItem() }
            }
            if (WashType.AUTOMATIC in types) {
                val list = if (query.isEmpty()) autoWashDao.getAutoWashesByCity(city)
                else autoWashDao.searchByName(city, query)
                result += list.map { it.toListItem() }
            }
            if (WashType.SELF in types) {
                val list = if (query.isEmpty()) selfWashDao.getSelfWashesByCity(city)
                else selfWashDao.searchByName(city, query)
                result += list.map { it.toListItem() }
            }

            _washes.value = result.sortedByDescending { it.rating }
            loadRatings(_washes.value)
        }
    }

    private fun loadRatings(items: List<WashListItem>) {
        viewModelScope.launch {
            val map = mutableMapOf<String, Pair<Float, Int>>()
            for (item in items) {
                val avg = reviewDao.getAverageRating(item.type.reviewKey(item.id))
                val count = reviewDao.getReviewCount(item.type.reviewKey(item.id))
                if (avg != null && count > 0) map[item.key] = avg to count
            }
            _carWashRatings.value = map
        }
    }

    fun selectAllCities() {
        _selectedCity.value = null
        refreshWashes()
    }

    fun selectCity(city: String) {
        _selectedCity.value = city
        refreshWashes()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        refreshWashes()
    }

    fun toggleType(type: WashType) {
        _selectedTypes.value = _selectedTypes.value.toMutableSet().apply {
            if (!add(type)) remove(type)
        }
        refreshWashes()
    }

    fun refreshRatings() {
        if (_washes.value.isNotEmpty()) loadRatings(_washes.value)
        refreshAvailability()
    }

    fun loadCustomerData(firebaseUid: String) {
        profileJob?.cancel()
        _currentCustomer.value = null
        _customerCar.value = null
        _profileMessage.value = null
        val user = FirebaseAuth.getInstance().currentUser
        _signedInEmail.value = user?.takeIf { it.uid == firebaseUid }?.email
        if (firebaseUid.isBlank() || user?.uid != firebaseUid) {
            _profileLoading.value = false
            _profileMessage.value = "Your session is unavailable. Please sign out and sign in again."
            return
        }
        _profileLoading.value = true
        profileJob = viewModelScope.launch {
            combine(customerDao.observeCustomer(firebaseUid), carDao.observeCars(firebaseUid)) { customer, cars ->
                customer to cars.firstOrNull()
            }.catch { error ->
                if (error is CancellationException) throw error
                _profileLoading.value = false
                _profileMessage.value = "Couldn’t load account details. Please try again."
            }.collect { (customer, car) ->
                _currentCustomer.value = customer
                _customerCar.value = car
                _profileLoading.value = false
                _profileMessage.value = if (customer == null)
                    "You are signed in, but no customer profile is stored on this device. Name, phone and vehicle details are stored locally and are not restored by signing in."
                else null
            }
        }
    }

    fun loadActiveAppointment(customerId: String) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            _activeAppointment.value = appointmentDao.getNextAppointmentForCustomer(customerId, currentTime)
        }
    }

    fun cancelAppointment(appointmentId: String, customerId: String) {
        viewModelScope.launch {
            appointmentDao.updateStatus(appointmentId, "CANCELED")
            loadActiveAppointment(customerId)
        }
    }
}