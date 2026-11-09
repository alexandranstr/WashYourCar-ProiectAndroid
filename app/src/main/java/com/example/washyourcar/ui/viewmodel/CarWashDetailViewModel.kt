package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.AppointmentDao
import com.example.washyourcar.data.dao.*
import com.example.washyourcar.data.entities.Appointment
import com.example.washyourcar.data.entities.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import com.example.washyourcar.models.*
import android.content.Context
import java.util.UUID

class CarWashDetailViewModel(
    private val carWashDao: CarWashDao,
    private val appointmentDao: AppointmentDao,
    private val carDao: com.example.washyourcar.data.dao.CarDao,
    private val carWashServiceDao: com.example.washyourcar.data.dao.CarWashServiceDao,
    private val reviewDao: ReviewDao,
    private val customerDao: com.example.washyourcar.data.dao.CustomerDao,
    private val selfWashDao: SelfWashDao? = null,
    private val selfWashServiceDao: SelfWashServiceDao? = null,
    private val autoWashDao: AutoWashDao? = null,
    private val autoWashServiceDao: AutoWashServiceDao? = null
) : ViewModel() {

    private val _washType = MutableStateFlow(WashType.NORMAL)
    val washType: StateFlow<WashType> = _washType

    /** Extra line for self/auto washes (number of bays). */
    private val _washInfo = MutableStateFlow<String?>(null)
    val washInfo: StateFlow<String?> = _washInfo

    private val _carWash = MutableStateFlow<CarWash?>(null)
    val carWash: StateFlow<CarWash?> = _carWash

    private var washId = -1
    private var normalBays = 0
    private var premiumBays = 0
    private var automaticBays = 0

    /** Bays clients can book: premium self-service bays or automatic bays (0 for classic washes). */
    val bookableBays: Int get() = if (_washType.value == WashType.SELF) premiumBays else automaticBays

    private val _bayStatuses = MutableStateFlow<List<BayStatus>>(emptyList())
    val bayStatuses: StateFlow<List<BayStatus>> = _bayStatuses

    /** Recomputes the (simulated) queues; call periodically from the screen. */
    fun refreshBays() {
        val type = _washType.value
        if (type == WashType.NORMAL || washId == -1) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val key = type.washKey(washId)
            val bookings = appointmentDao.getOpenBookings(key, now, now + 24 * 3_600_000L)
            _bayStatuses.value = BayQueueSimulator.statuses(key, normalBays, premiumBays, automaticBays, bookings, now)
        }
    }

    private val _customerCar = MutableStateFlow<com.example.washyourcar.data.entities.Car?>(null)
    val customerCar: StateFlow<com.example.washyourcar.data.entities.Car?> = _customerCar

    private val _availableSlots = MutableStateFlow<List<Long>>(emptyList())
    val availableSlots: StateFlow<List<Long>> = _availableSlots

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    private val _randomTip = MutableStateFlow<WashTips?>(null)
    val randomTip: StateFlow<WashTips?> = _randomTip
    private val _carWashServiceNames = MutableStateFlow<List<String>>(emptyList())
    val carWashServiceNames: StateFlow<List<String>> = _carWashServiceNames

    private val _availableServices = MutableStateFlow<List<CarWashServiceDetails>>(emptyList())
    val availableServices: StateFlow<List<CarWashServiceDetails>> = _availableServices

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _computedRating = MutableStateFlow<Float?>(null)
    val computedRating: StateFlow<Float?> = _computedRating

    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount

    private val _customerExistingReview = MutableStateFlow<Review?>(null)
    val customerExistingReview: StateFlow<Review?> = _customerExistingReview

    fun loadCarWashDetails(carWashId: Int, customerUid: String, type: WashType = WashType.NORMAL) {
        _washType.value = type
        washId = carWashId
        viewModelScope.launch {
            when (type) {
                WashType.NORMAL -> {
                    _carWash.value = carWashDao.getCarWashById(carWashId)
                    _carWashServiceNames.value = carWashDao.getServiceNamesByCarWashId(carWashId)
                    _availableServices.value = carWashServiceDao.getServicesByCarWashId(carWashId)
                }
                // Self/auto washes are mapped onto CarWash so the existing detail screen can show them.
                WashType.SELF -> {
                    val wash = selfWashDao?.getSelfWashById(carWashId)
                    _carWash.value = wash?.let {
                        CarWash(it.selfWashId, it.ownerId, it.name, it.address, it.city, it.phoneNumber, it.rating, "", it.openTime, it.closeTime, it.latitude, it.longitude)
                    }
                    normalBays = wash?.normalSelfWashBays ?: 0
                    premiumBays = wash?.premiumSelfWashBays ?: 0
                    _washInfo.value = wash?.let { "${it.normalSelfWashBays} normal / ${it.premiumSelfWashBays} premium self-service bays" }
                    _carWashServiceNames.value = selfWashDao?.getServiceNamesBySelfWashId(carWashId).orEmpty()
                    _availableServices.value = selfWashServiceDao?.getServicesBySelfWashId(carWashId).orEmpty().map {
                        CarWashServiceDetails(CarWashService(carWashId, it.selfWashService.selfServiceId, it.selfWashService.price, it.selfWashService.duration), it.serviceName, it.serviceDescription)
                    }
                }
                WashType.AUTOMATIC -> {
                    val wash = autoWashDao?.getAutoWashById(carWashId)
                    _carWash.value = wash?.let {
                        CarWash(it.autoWashId, it.ownerId, it.name, it.address, it.city, it.phoneNumber, it.rating, "", it.openTime, it.closeTime, it.latitude, it.longitude)
                    }
                    automaticBays = wash?.automaticBays ?: 0
                    _washInfo.value = wash?.let { "${it.automaticBays} automatic wash bays" }
                    _carWashServiceNames.value = autoWashDao?.getServiceNamesByAutoWashId(carWashId).orEmpty()
                    _availableServices.value = autoWashServiceDao?.getServicesByAutoWashId(carWashId).orEmpty().map {
                        CarWashServiceDetails(CarWashService(carWashId, it.autoWashServices.autoServiceId, it.autoWashServices.price, it.autoWashServices.duration), it.serviceName, it.serviceDescription)
                    }
                }
            }
            refreshBays()
            val carList = carDao.getCarsByCustomerId(customerUid)
            _customerCar.value = carList.firstOrNull()

            try {
                val tips = com.example.washyourcar.data.network.RetrofitClient.apiService.getTips()
                if (tips.isNotEmpty()) {
                    _randomTip.value = tips.random()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _randomTip.value = WashTips(0, "Wash Tip", "Keep your car clean for a shiny day!")
            }

            loadReviews(carWashId, customerUid)
        }
    }

    private fun loadReviews(washId: Int, customerUid: String) {
        val carWashId = _washType.value.reviewKey(washId)
        viewModelScope.launch {
            _reviews.value = reviewDao.getReviewsByCarWashIdOnce(carWashId)
            _reviewCount.value = reviewDao.getReviewCount(carWashId)
            _computedRating.value = reviewDao.getAverageRating(carWashId)
            _customerExistingReview.value = reviewDao.getCustomerReviewForCarWash(carWashId, customerUid)
        }
    }

    fun submitReview(washId: Int, customerId: String, rating: Int, comment: String) {
        val carWashId = _washType.value.reviewKey(washId)
        viewModelScope.launch {
            val customerName = customerDao.getCustomerFullName(customerId) ?: "Anonymous"
            val existingReview = reviewDao.getCustomerReviewForCarWash(carWashId, customerId)

            val review = if (existingReview != null) {
                Review(
                    reviewId = existingReview.reviewId,
                    carWashId = carWashId,
                    customerId = customerId,
                    customerName = customerName,
                    rating = rating,
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                Review(
                    reviewId = UUID.randomUUID().toString(),
                    carWashId = carWashId,
                    customerId = customerId,
                    customerName = customerName,
                    rating = rating,
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )
            }

            reviewDao.insertReview(review)
            loadReviews(washId, customerId)
        }
    }

    private var slotRequest = 0

    fun clearAvailableSlots() {
        slotRequest++
        _availableSlots.value = emptyList()
        _errorMessage.value = null
    }

    fun generateSlots(carWashId: Int, dateMillis: Long, durationMinutes: Int, timeRange: String) {
        val request = ++slotRequest
        viewModelScope.launch {
            _availableSlots.value = emptyList()
            _errorMessage.value = null

            val currentCarWash = _carWash.value ?: return@launch

            val baseCalendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val startHour = when (timeRange) {
                "Morning" -> (currentCarWash.openTime / 100).toInt()
                "Afternoon" -> ((currentCarWash.openTime / 100 + currentCarWash.closeTime / 100) / 2).toInt()
                else -> (((currentCarWash.openTime / 100 + currentCarWash.closeTime / 100) / 2) + 1).toInt()
            }

            val endHour = when (timeRange) {
                "Morning" -> ((currentCarWash.openTime / 100 + currentCarWash.closeTime / 100) / 2).toInt()
                "Afternoon" -> (((currentCarWash.openTime / 100 + currentCarWash.closeTime / 100) / 2) + 1).toInt()
                else -> (currentCarWash.closeTime / 100).toInt()
            }

            val slots = mutableListOf<Long>()
            val durationMillis = durationMinutes * 60 * 1000L
            val earliestStart = System.currentTimeMillis() + minimumLeadMillis()

            for (hour in startHour until endHour) {
                for (minute in listOf(0, 15, 30, 45)) {
                    val slotCal = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val slotStart = slotCal.timeInMillis
                    val slotEnd = slotStart + durationMillis

                    if (slotStart < earliestStart) continue
                    if (isSlotAvailable(carWashId, slotStart, slotEnd)) slots.add(slotStart)
                }
            }

            if (request != slotRequest) return@launch
            if (slots.isEmpty()) {
                val noEmployees = _washType.value == WashType.NORMAL && appointmentDao.countEmployees(carWashId) == 0
                _errorMessage.value = if (noEmployees)
                    "This car wash has no employees registered yet, so it can't take bookings. Please try again later."
                else if (_washType.value == WashType.SELF)
                    "No free premium bay in this period. Self-service bays must be booked at least 1 hour ahead."
                else "No available slots in this period."
            } else {
                _availableSlots.value = slots
            }
        }
    }

    /** Self-service premium bays must be booked at least one hour ahead; others just in the future. */
    private fun minimumLeadMillis(): Long = if (_washType.value == WashType.SELF) 60 * 60_000L else 0L

    private suspend fun isSlotAvailable(carWashId: Int, start: Long, end: Long): Boolean =
        if (_washType.value == WashType.NORMAL) {
            appointmentDao.getAvailableEmployees(carWashId, start, end).isNotEmpty()
        } else {
            freeBay(start, end) != null
        }

    /** First bookable bay (1-based within premium/automatic bays) that is free for the whole interval. */
    private suspend fun freeBay(start: Long, end: Long): Int? {
        val taken = appointmentDao.getBookedBays(_washType.value.washKey(washId), start, end).toSet()
        return (1..bookableBays).firstOrNull { it !in taken }
    }

    private suspend fun confirmBayBooking(
        context: Context,
        customerId: String,
        serviceIds: List<Int>,
        licensePlate: String,
        startTime: Long,
        durationMinutes: Int
    ) {
        val endTime = startTime + durationMinutes * 60_000L
        if (startTime < System.currentTimeMillis() + minimumLeadMillis()) {
            _errorMessage.value = "Self-service bays must be booked at least 1 hour ahead."
            return
        }
        val bay = freeBay(startTime, endTime)
        if (bay == null) {
            _errorMessage.value = "That bay was just taken. Please refresh slots."
            return
        }
        appointmentDao.insert(
            Appointment(
                appointmentId = UUID.randomUUID().toString(),
                customerId = customerId,
                carWashId = _washType.value.washKey(washId),
                employeeId = null,
                serviceIds = serviceIds,
                licensePlate = licensePlate,
                startTime = startTime,
                endTime = endTime,
                status = "CONFIRMED",
                bayNumber = bay
            )
        )
        context.getSharedPreferences("WashYourCarPrefs", Context.MODE_PRIVATE).edit()
            .putString("last_booked_wash_name", _carWash.value?.name ?: "Car wash")
            .putLong("last_booked_time", System.currentTimeMillis())
            .apply()
        refreshBays()
        _isSuccess.value = true
    }

    fun confirmAppointment(
        context: Context,
        customerId: String,
        carWashId: Int,
        serviceId: Int,
        licensePlate: String,
        startTime: Long,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val endTime = startTime + (durationMinutes * 60 * 1000L)

            val availableEmployees = appointmentDao.getAvailableEmployees(carWashId, startTime, endTime)

            if (availableEmployees.isEmpty()) {
                _errorMessage.value = "Employee no longer available. Please refresh slots."
                return@launch
            }

            val newAppointment = Appointment(
                appointmentId = UUID.randomUUID().toString(),
                customerId = customerId,
                carWashId = carWashId,
                employeeId = availableEmployees.first(),
                serviceIds = listOf(serviceId),
                licensePlate = licensePlate,
                startTime = startTime,
                endTime = endTime,
                status = "CONFIRMED"
            )

            appointmentDao.insert(newAppointment)

            val sharedPrefs = context.getSharedPreferences("WashYourCarPrefs", android.content.Context.MODE_PRIVATE)
            val currentWashName = _carWash.value?.name ?: "Unknown Car Wash"

            sharedPrefs.edit().apply {
                putString("last_booked_wash_name", currentWashName)
                putLong("last_booked_time", System.currentTimeMillis())
            }.apply()

            _isSuccess.value = true
        }
    }

    fun confirmAppointmentWithServices(
        context: Context,
        customerId: String,
        carWashId: Int,
        serviceIds: List<Int>,
        licensePlate: String,
        startTime: Long,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            if (_washType.value != WashType.NORMAL) {
                confirmBayBooking(context, customerId, serviceIds, licensePlate, startTime, durationMinutes)
                return@launch
            }
            val endTime = startTime + (durationMinutes * 60 * 1000L)

            val availableEmployees = appointmentDao.getAvailableEmployees(carWashId, startTime, endTime)

            if (availableEmployees.isEmpty()) {
                _errorMessage.value = "Employee no longer available. Please refresh slots."
                return@launch
            }

            val newAppointment = Appointment(
                appointmentId = UUID.randomUUID().toString(),
                customerId = customerId,
                carWashId = carWashId,
                employeeId = availableEmployees.first(),
                serviceIds = serviceIds,
                licensePlate = licensePlate,
                startTime = startTime,
                endTime = endTime,
                status = "CONFIRMED"
            )

            appointmentDao.insert(newAppointment)

            val sharedPrefs = context.getSharedPreferences("WashYourCarPrefs", android.content.Context.MODE_PRIVATE)
            val currentWashName = _carWash.value?.name ?: "Unknown Car Wash"

            sharedPrefs.edit().apply {
                putString("last_booked_wash_name", currentWashName)
                putLong("last_booked_time", System.currentTimeMillis())
            }.apply()

            _isSuccess.value = true
        }
    }

    fun resetSuccess() {
        _isSuccess.value = false
    }
}