package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.AppointmentDao
import com.example.washyourcar.data.dao.*
import com.example.washyourcar.data.entities.Appointment
import com.example.washyourcar.data.entities.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import com.example.washyourcar.models.*
import android.content.Context
import java.util.UUID

class CarWashDetailViewModel(
    private val carWashDao: CarWashDao,
    private val appointmentDao: AppointmentDao,
    private val carDao: com.example.washyourcar.data.dao.CarDao
) : ViewModel() {

    private val _carWash = MutableStateFlow<CarWash?>(null)
    val carWash: StateFlow<CarWash?> = _carWash

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

    fun loadCarWashDetails(carWashId: Int, customerUid: String) {
        viewModelScope.launch {
            _carWash.value = carWashDao.getCarWashById(carWashId)
            val carList = carDao.getCarsByCustomerId(customerUid)
            _customerCar.value = carList.firstOrNull()
            _carWashServiceNames.value = carWashDao.getServiceNamesByCarWashId(carWashId)

            try {
                val tips = com.example.washyourcar.data.network.RetrofitClient.apiService.getTips()
                if (tips.isNotEmpty()) {
                    _randomTip.value = tips.random()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _randomTip.value = WashTips(0, "Wash Tip", "Keep your car clean for a shiny day!")
            }
        }
    }

    fun generateSlots(carWashId: Int, dateMillis: Long, durationMinutes: Int, timeRange: String) {
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

                    val availableEmployees = appointmentDao.getAvailableEmployees(carWashId, slotStart, slotEnd)
                    if (availableEmployees.isNotEmpty()) {
                        slots.add(slotStart)
                    }
                }
            }

            if (slots.isEmpty()) {
                _errorMessage.value = "No available slots in this period."
            } else {
                _availableSlots.value = slots
            }
        }
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
                serviceIds = serviceId,
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