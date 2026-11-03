package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.CarWashDao
import com.example.washyourcar.data.dao.CarWashServiceDao
import com.example.washyourcar.data.entities.CarWashService
import com.example.washyourcar.data.entities.CarWashServiceDetails
import com.example.washyourcar.data.entities.Service
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CarWashMonitoringViewModel(
    private val carWashServiceDao: CarWashServiceDao,
    private val carWashDao: CarWashDao
) : ViewModel() {

    private val _activeServices = MutableStateFlow<List<CarWashServiceDetails>>(emptyList())
    val activeServices: StateFlow<List<CarWashServiceDetails>> = _activeServices.asStateFlow()

    private val _availableServices = MutableStateFlow<List<Service>>(emptyList())
    val availableServices: StateFlow<List<Service>> = _availableServices.asStateFlow()

    private var currentCarWashId: Int = -1

    fun initOwner(firebaseUid: String) {
        viewModelScope.launch {
            val carWash = carWashDao.getCarWashByOwnerId(firebaseUid)
            if (carWash != null) {
                currentCarWashId = carWash.carWashId
                loadServices()
            }
        }
    }

    private fun loadServices() {
        if (currentCarWashId == -1) return
        viewModelScope.launch {
            _activeServices.value = carWashServiceDao.getServicesByCarWashId(currentCarWashId)
            _availableServices.value = carWashServiceDao.getUnassociatedServices(currentCarWashId)
        }
    }

    fun updateService(serviceId: Int, newPrice: Float, newDuration: Int) {
        viewModelScope.launch {
            carWashServiceDao.updateOffer(currentCarWashId, serviceId, newPrice, newDuration)
            loadServices()
        }
    }

    fun removeService(serviceId: Int) {
        viewModelScope.launch {
            carWashServiceDao.deactivateService(currentCarWashId, serviceId)
            loadServices()
        }
    }

    fun addService(serviceId: Int, price: Float, duration: Int) {
        viewModelScope.launch {
            val newService = CarWashService(
                carWashId = currentCarWashId,
                serviceId = serviceId,
                price = price,
                duration = duration
            )
            carWashServiceDao.insert(newService)
            loadServices()
        }
    }
}