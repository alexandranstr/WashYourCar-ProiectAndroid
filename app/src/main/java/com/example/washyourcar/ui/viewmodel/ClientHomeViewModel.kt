package com.example.washyourcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.AppDatabase
import com.example.washyourcar.data.entities.Car
import com.example.washyourcar.data.entities.CarWash
import com.example.washyourcar.data.entities.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClientHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val carWashDao = database.carWashDao()
    private val customerDao = database.customerDao()
    private val carDao = database.carDao()
    private val appointmentDao = database.appointmentDao()

    private val _carWashes = MutableStateFlow<List<CarWash>>(emptyList())
    val carWashes: StateFlow<List<CarWash>> = _carWashes.asStateFlow()

    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()

    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilters: StateFlow<Set<String>> = _selectedFilters.asStateFlow()

    private val _currentCustomer = MutableStateFlow<Customer?>(null)
    val currentCustomer: StateFlow<Customer?> = _currentCustomer.asStateFlow()

    private val _customerCar = MutableStateFlow<Car?>(null)
    val customerCar: StateFlow<Car?> = _customerCar.asStateFlow()

    private val _activeAppointment = MutableStateFlow<com.example.washyourcar.data.entities.Appointment?>(null)
    val activeAppointment: StateFlow<com.example.washyourcar.data.entities.Appointment?> = _activeAppointment

    init {
        loadCities()
        loadAllCarWashes()
    }

    fun loadCustomerData(firebaseUid: String) {
        viewModelScope.launch {
            val clientGasit = customerDao.getCustomersById(firebaseUid)
            _currentCustomer.value = clientGasit

            val masinaGasita = carDao.getCarsByCustomerId(firebaseUid).firstOrNull()
            _customerCar.value = masinaGasita
        }
    }

    private fun loadCities() {
        viewModelScope.launch {
            _cities.value = carWashDao.getAllCities()
        }
    }

    fun loadAllCarWashes() {
        viewModelScope.launch {
            _selectedCity.value = null
            _carWashes.value = carWashDao.getAllCarWashes()
        }
    }

    fun selectCity(city: String) {
        viewModelScope.launch {
            _selectedCity.value = city
            _searchQuery.value = ""
            _selectedFilters.value = emptySet()
            _carWashes.value = carWashDao.getCarWashesByCity(city)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            _selectedFilters.value = emptySet()
        }
        applySearchAndFilters()
    }

    fun toggleFilter(serviceName: String) {
        val current = _selectedFilters.value.toMutableSet()
        if (current.contains(serviceName)) {
            current.remove(serviceName)
        } else {
            current.add(serviceName)
        }
        _selectedFilters.value = current

        if (current.isNotEmpty()) {
            _searchQuery.value = ""
        }
        applySearchAndFilters()
    }

    private fun applySearchAndFilters() {
        viewModelScope.launch {
            val city = _selectedCity.value
            val query = _searchQuery.value.trim()
            val filters = _selectedFilters.value

            when {
                filters.isNotEmpty() -> {
                    _carWashes.value = carWashDao.filterByServices(city, filters.toList())
                }
                query.isNotEmpty() -> {
                    _carWashes.value = carWashDao.searchByName(city, query)
                }
                else -> {
                    _carWashes.value = carWashDao.getCarWashesByCity(city)
                }
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