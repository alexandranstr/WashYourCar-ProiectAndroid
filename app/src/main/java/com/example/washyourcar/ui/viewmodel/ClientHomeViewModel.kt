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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import com.google.firebase.auth.FirebaseAuth

class ClientHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val carWashDao = database.carWashDao()
    private val customerDao = database.customerDao()
    private val carDao = database.carDao()
    private val appointmentDao = database.appointmentDao()
    private val carWashServiceDao = database.carWashServiceDao()

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

    private val _allServiceNames = MutableStateFlow<List<String>>(emptyList())
    val allServiceNames: StateFlow<List<String>> = _allServiceNames.asStateFlow()

    private val _currentCustomer = MutableStateFlow<Customer?>(null)
    val currentCustomer: StateFlow<Customer?> = _currentCustomer.asStateFlow()

    private val _customerCar = MutableStateFlow<Car?>(null)
    val customerCar: StateFlow<Car?> = _customerCar.asStateFlow()

    private val _activeAppointment = MutableStateFlow<com.example.washyourcar.data.entities.Appointment?>(null)
    val activeAppointment: StateFlow<com.example.washyourcar.data.entities.Appointment?> = _activeAppointment

    init {
        loadCities()
        loadAllCarWashes()
        loadAllServices()
    }

    private fun loadAllServices() {
        viewModelScope.launch {
            val allServices = mutableSetOf<String>()
            for (carWash in carWashDao.getAllCarWashes()) {
                val names = carWashDao.getServiceNamesByCarWashId(carWash.carWashId)
                allServices.addAll(names)
            }
            _allServiceNames.value = allServices.sorted()
        }
    }

    private var profileJob: Job? = null
    private val _profileLoading = MutableStateFlow(false)
    val profileLoading = _profileLoading.asStateFlow()
    private val _profileMessage = MutableStateFlow<String?>(null)
    val profileMessage = _profileMessage.asStateFlow()
    private val _signedInEmail = MutableStateFlow<String?>(null)
    val signedInEmail = _signedInEmail.asStateFlow()

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