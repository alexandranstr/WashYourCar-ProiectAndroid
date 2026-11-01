package com.example.washyourcar.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.AppointmentDao
import com.example.washyourcar.data.dao.CarWashDao
import com.example.washyourcar.data.dao.CarWashServiceDao
import com.example.washyourcar.data.dao.EmployeeDao
import com.example.washyourcar.data.entities.AppointmentWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.washyourcar.data.entities.Employee

class EmployeeHomeViewModel(
    private val employeeDao: EmployeeDao,
    private val appointmentDao: AppointmentDao,
    private val carWashDao: CarWashDao,
    private val carWashServiceDao: CarWashServiceDao
) : ViewModel() {

    private val _appointments = MutableStateFlow<List<AppointmentWithDetails>>(emptyList())
    val appointments: StateFlow<List<AppointmentWithDetails>> = _appointments.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private var currentCarWashId: Int? = null

    private val _currentEmployee = MutableStateFlow<Employee?>(null)
    val currentEmployee: StateFlow<Employee?> = _currentEmployee.asStateFlow()

    private val _carWashName = MutableStateFlow<String?>(null)
    val carWashName: StateFlow<String?> = _carWashName.asStateFlow()

    private val _appointmentServiceNames = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val appointmentServiceNames: StateFlow<Map<String, List<String>>> = _appointmentServiceNames.asStateFlow()

    fun initEmployee(firebaseUid: String) {
        viewModelScope.launch {
            val emp = employeeDao.getById(firebaseUid)
            _currentEmployee.value = emp

            currentCarWashId = emp?.carWashId

            if (currentCarWashId != null) {
                val name = carWashDao.getCarWashNameById(currentCarWashId!!)
                _carWashName.value = name
            }

            loadAppointmentsForSelectedDate()
        }
    }

    fun assignCarWashToEmployee(firebaseUid: String, carWashId: Int) {
        viewModelScope.launch {
            try {
                employeeDao.assignCarWash(firebaseUid, carWashId)
                initEmployee(firebaseUid)
            } catch (e: Exception) {
                Log.e("EmployeeHomeVM", "Eroare la asignare: ${e.message}")
            }
        }
    }

    fun updateSelectedDate(newDate: Calendar) {
        _selectedDate.value = newDate
        loadAppointmentsForSelectedDate()
    }

    fun changeDateByDays(days: Int) {
        val newDate = _selectedDate.value.clone() as Calendar
        newDate.add(Calendar.DAY_OF_YEAR, days)
        updateSelectedDate(newDate)
    }

    private fun loadAppointmentsForSelectedDate() {
        val carWashId = currentCarWashId ?: return

        val calendar = _selectedDate.value.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        viewModelScope.launch {
            try {
                appointmentDao.updateAutomaticStatuses(System.currentTimeMillis())

                val result = appointmentDao.getAppointmentsWithDetailsByDay(carWashId, startOfDay, endOfDay)
                _appointments.value = result

                val serviceNamesMap = mutableMapOf<String, List<String>>()
                for (appointment in result) {
                    if (appointment.appointment.serviceIds.isNotEmpty()) {
                        val names = carWashServiceDao.getServiceNamesByIds(appointment.appointment.serviceIds)
                        serviceNamesMap[appointment.appointment.appointmentId] = names
                    }
                }
                _appointmentServiceNames.value = serviceNamesMap
            } catch (e: Exception) {
                Log.e("EmployeeHomeVM", "Eroare la încărcarea programărilor: ${e.message}")
            }
        }
    }

    fun markAppointmentAsDone(appointmentId: String) {
        viewModelScope.launch {
            try {
                appointmentDao.updateStatus(appointmentId, "DONE")
                loadAppointmentsForSelectedDate()
            } catch (e: Exception) {
                Log.e("EmployeeHomeVM", "Eroare la actualizarea statusului: ${e.message}")
            }
        }
    }
}