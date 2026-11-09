package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.AutoWashDao
import com.example.washyourcar.data.dao.AutoWashServiceDao
import com.example.washyourcar.data.dao.CarWashDao
import com.example.washyourcar.data.dao.CarWashServiceDao
import com.example.washyourcar.data.dao.OwnerDao
import com.example.washyourcar.data.dao.ReviewDao
import com.example.washyourcar.data.dao.SelfWashDao
import com.example.washyourcar.data.dao.SelfWashServiceDao
import com.example.washyourcar.data.entities.AutoWash
import com.example.washyourcar.data.entities.AutoWashServiceDetails
import com.example.washyourcar.data.entities.CarWash
import com.example.washyourcar.data.entities.CarWashServiceDetails
import com.example.washyourcar.data.entities.Owner
import com.example.washyourcar.data.entities.Review
import com.example.washyourcar.data.entities.SelfWash
import com.example.washyourcar.data.entities.SelfWashServiceDetails
import com.example.washyourcar.data.entities.WashType
import com.example.washyourcar.data.entities.reviewKey
import com.example.washyourcar.data.entities.washKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OwnerHomeViewModel(
    private val ownerDao: OwnerDao,
    private val carWashDao: CarWashDao,
    private val autoWashDao: AutoWashDao,
    private val selfWashDao: SelfWashDao,
    private val carWashServiceDao: CarWashServiceDao,
    private val autoWashServiceDao: AutoWashServiceDao,
    private val selfWashServiceDao: SelfWashServiceDao,
    private val reviewDao: ReviewDao,
    private val appointmentDao: com.example.washyourcar.data.dao.AppointmentDao? = null
) : ViewModel() {

    private val _currentOwner = MutableStateFlow<Owner?>(null)
    val currentOwner: StateFlow<Owner?> = _currentOwner.asStateFlow()

    private val _carWash = MutableStateFlow<CarWash?>(null)
    val carWash: StateFlow<CarWash?> = _carWash.asStateFlow()

    private val _selfWash = MutableStateFlow<SelfWash?>(null)
    val selfWash: StateFlow<SelfWash?> = _selfWash.asStateFlow()

    private val _autoWash = MutableStateFlow<AutoWash?>(null)
    val autoWash: StateFlow<AutoWash?> = _autoWash.asStateFlow()

    private val _services = MutableStateFlow<List<CarWashServiceDetails>>(emptyList())
    val services: StateFlow<List<CarWashServiceDetails>> = _services.asStateFlow()

    private val _selfServices = MutableStateFlow<List<SelfWashServiceDetails>>(emptyList())
    val selfServices: StateFlow<List<SelfWashServiceDetails>> = _selfServices.asStateFlow()

    private val _autoServices = MutableStateFlow<List<AutoWashServiceDetails>>(emptyList())
    val autoServices: StateFlow<List<AutoWashServiceDetails>> = _autoServices.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _computedRating = MutableStateFlow<Float?>(null)
    val computedRating: StateFlow<Float?> = _computedRating.asStateFlow()

    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount.asStateFlow()

    /** Employees registered at a classic wash (null for self-service/automatic). */
    private val _employeeCount = MutableStateFlow<Int?>(null)
    val employeeCount: StateFlow<Int?> = _employeeCount.asStateFlow()

    private val _bayStatuses = MutableStateFlow<List<com.example.washyourcar.models.BayStatus>>(emptyList())
    val bayStatuses: StateFlow<List<com.example.washyourcar.models.BayStatus>> = _bayStatuses.asStateFlow()

    /** Upcoming bookings (next 7 days) for premium self-service or automatic bays. */
    private val _bookings = MutableStateFlow<List<com.example.washyourcar.data.entities.AppointmentWithDetails>>(emptyList())
    val bookings: StateFlow<List<com.example.washyourcar.data.entities.AppointmentWithDetails>> = _bookings.asStateFlow()

    /** Recomputes simulated queues and reloads bookings; the screen calls this periodically. */
    fun refreshBays() {
        val dao = appointmentDao ?: return
        val self = _selfWash.value
        val auto = _autoWash.value
        val key = when {
            self != null -> WashType.SELF.washKey(self.selfWashId)
            auto != null -> WashType.AUTOMATIC.washKey(auto.autoWashId)
            else -> return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val open = dao.getOpenBookings(key, now, now + 24 * 3_600_000L)
            _bayStatuses.value = com.example.washyourcar.models.BayQueueSimulator.statuses(
                key, self?.normalSelfWashBays ?: 0, self?.premiumSelfWashBays ?: 0, auto?.automaticBays ?: 0, open, now
            )
            _bookings.value = dao.getBookingsWithCustomer(key, now, now + 7 * 24 * 3_600_000L)
        }
    }

    /** Loads the owner and one of their washes: [wash] if given, otherwise their first one. */
    fun initOwner(firebaseUid: String, wash: com.example.washyourcar.models.OwnerWashRef? = null) {
        viewModelScope.launch {
            _currentOwner.value = ownerDao.getById(firebaseUid)

            // Switching washes: clear what the previous one showed.
            _carWash.value = null; _selfWash.value = null; _autoWash.value = null
            _services.value = emptyList(); _selfServices.value = emptyList(); _autoServices.value = emptyList()
            _employeeCount.value = null; _bayStatuses.value = emptyList(); _bookings.value = emptyList()
            _reviews.value = emptyList(); _reviewCount.value = 0; _computedRating.value = null

            // 1. Verificăm dacă e spălătorie Normală (Clasică)
            val normal = when (wash?.type) {
                null -> carWashDao.getCarWashByOwnerId(firebaseUid)
                WashType.NORMAL -> carWashDao.getCarWashById(wash.id)
                else -> null
            }
            if (normal != null) {
                _carWash.value = normal
                _services.value = carWashServiceDao.getServicesByCarWashId(normal.carWashId)
                _employeeCount.value = appointmentDao?.countEmployees(normal.carWashId)
                loadReviews(normal.carWashId)
                return@launch
            }

            // 2. Verificăm dacă e Self Wash
            val self = when (wash?.type) {
                null -> selfWashDao.getSelfWashByOwnerId(firebaseUid)
                WashType.SELF -> selfWashDao.getSelfWashById(wash.id)
                else -> null
            }
            if (self != null) {
                _selfWash.value = self
                _selfServices.value = selfWashServiceDao.getServicesBySelfWashId(self.selfWashId)
                loadReviews(WashType.SELF.reviewKey(self.selfWashId))
                refreshBays()
                return@launch
            }

            // 3. Verificăm dacă e Auto Wash (Automată)
            val auto = when (wash?.type) {
                null -> autoWashDao.getAutoWashByOwnerId(firebaseUid)
                WashType.AUTOMATIC -> autoWashDao.getAutoWashById(wash.id)
                else -> null
            }
            if (auto != null) {
                _autoWash.value = auto
                _autoServices.value = autoWashServiceDao.getServicesByAutoWashId(auto.autoWashId)
                loadReviews(WashType.AUTOMATIC.reviewKey(auto.autoWashId))
                refreshBays()
                return@launch
            }
        }
    }

    private suspend fun loadReviews(washId: Int) {
        _reviews.value = reviewDao.getReviewsByCarWashIdOnce(washId)
        _reviewCount.value = reviewDao.getReviewCount(washId)
        _computedRating.value = reviewDao.getAverageRating(washId)
    }
}