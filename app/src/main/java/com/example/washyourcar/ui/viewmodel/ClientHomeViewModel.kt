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