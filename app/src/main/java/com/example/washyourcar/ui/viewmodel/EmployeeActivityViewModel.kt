package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.AppointmentDao
import com.example.washyourcar.data.dao.CarWashDao
import com.example.washyourcar.data.entities.AppointmentWithFullDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class EmployeeActivityViewModel(
    private val appointmentDao: AppointmentDao,
    private val carWashDao: CarWashDao
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private val _appointments = MutableStateFlow<List<AppointmentWithFullDetails>>(emptyList())
    val appointments: StateFlow<List<AppointmentWithFullDetails>> = _appointments.asStateFlow()

    private var carWashId: Int = -1

    fun initOwner(firebaseUid: String) {
        viewModelScope.launch {
            val carWash = carWashDao.getCarWashByOwnerId(firebaseUid)
            if (carWash != null) {
                carWashId = carWash.carWashId
                loadAppointments()
            }
        }
    }

    fun changeDateByDays(days: Int) {
        val newDate = _selectedDate.value.clone() as Calendar
        newDate.add(Calendar.DAY_OF_YEAR, days)
        _selectedDate.value = newDate
        loadAppointments()
    }

    private fun loadAppointments() {
        if (carWashId == -1) return

        val calendar = _selectedDate.value.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endOfDay = calendar.timeInMillis

        viewModelScope.launch {
            _appointments.value = appointmentDao.getAppointmentsWithFullDetailsByDay(carWashId, startOfDay, endOfDay)
        }
    }
}