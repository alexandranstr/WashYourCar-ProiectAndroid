package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.AutoWashDao
import com.example.washyourcar.data.dao.AutoWashServiceDao
import com.example.washyourcar.data.dao.CarWashDao
import com.example.washyourcar.data.dao.CarWashServiceDao
import com.example.washyourcar.data.dao.SelfWashDao
import com.example.washyourcar.data.dao.SelfWashServiceDao
import com.example.washyourcar.data.entities.AutoWashService
import com.example.washyourcar.data.entities.CarWashService
import com.example.washyourcar.data.entities.CarWashServiceDetails
import com.example.washyourcar.data.entities.SelfWashService
import com.example.washyourcar.data.entities.Service
import com.example.washyourcar.data.entities.WashType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the owner's services for any wash type. Self/automatic services are mapped onto
 * [CarWashServiceDetails] / [Service] so the existing monitoring screen works unchanged.
 */
class CarWashMonitoringViewModel(
    private val carWashServiceDao: CarWashServiceDao,
    private val carWashDao: CarWashDao,
    private val selfWashDao: SelfWashDao,
    private val selfWashServiceDao: SelfWashServiceDao,
    private val autoWashDao: AutoWashDao,
    private val autoWashServiceDao: AutoWashServiceDao
) : ViewModel() {

    private val _activeServices = MutableStateFlow<List<CarWashServiceDetails>>(emptyList())
    val activeServices: StateFlow<List<CarWashServiceDetails>> = _activeServices.asStateFlow()

    private val _availableServices = MutableStateFlow<List<Service>>(emptyList())
    val availableServices: StateFlow<List<Service>> = _availableServices.asStateFlow()

    private var currentWashId: Int = -1
    private var currentType: WashType = WashType.NORMAL

    fun initOwner(firebaseUid: String) {
        viewModelScope.launch {
            val normal = carWashDao.getCarWashByOwnerId(firebaseUid)
            val self = if (normal == null) selfWashDao.getSelfWashByOwnerId(firebaseUid) else null
            val auto = if (normal == null && self == null) autoWashDao.getAutoWashByOwnerId(firebaseUid) else null
            when {
                normal != null -> { currentType = WashType.NORMAL; currentWashId = normal.carWashId }
                self != null -> { currentType = WashType.SELF; currentWashId = self.selfWashId }
                auto != null -> { currentType = WashType.AUTOMATIC; currentWashId = auto.autoWashId }
                else -> return@launch
            }
            loadServices()
        }
    }

    private fun loadServices() {
        if (currentWashId == -1) return
        viewModelScope.launch {
            when (currentType) {
                WashType.NORMAL -> {
                    _activeServices.value = carWashServiceDao.getServicesByCarWashId(currentWashId)
                    _availableServices.value = carWashServiceDao.getUnassociatedServices(currentWashId)
                }
                WashType.SELF -> {
                    _activeServices.value = selfWashServiceDao.getServicesBySelfWashId(currentWashId).map {
                        CarWashServiceDetails(
                            CarWashService(currentWashId, it.selfWashService.selfServiceId, it.selfWashService.price, it.selfWashService.duration),
                            it.serviceName, it.serviceDescription
                        )
                    }
                    _availableServices.value = selfWashServiceDao.getUnassociatedServices(currentWashId)
                        .map { Service(it.selfServiceId, it.name, it.description) }
                }
                WashType.AUTOMATIC -> {
                    _activeServices.value = autoWashServiceDao.getServicesByAutoWashId(currentWashId).map {
                        CarWashServiceDetails(
                            CarWashService(currentWashId, it.autoWashServices.autoServiceId, it.autoWashServices.price, it.autoWashServices.duration),
                            it.serviceName, it.serviceDescription
                        )
                    }
                    _availableServices.value = autoWashServiceDao.getUnassociatedServices(currentWashId)
                        .map { Service(it.autoServiceId, it.name, it.description) }
                }
            }
        }
    }

    fun updateService(serviceId: Int, newPrice: Float, newDuration: Int) {
        viewModelScope.launch {
            when (currentType) {
                WashType.NORMAL -> carWashServiceDao.updateOffer(currentWashId, serviceId, newPrice, newDuration)
                WashType.SELF -> selfWashServiceDao.updateOffer(currentWashId, serviceId, newPrice, newDuration)
                WashType.AUTOMATIC -> autoWashServiceDao.updateOffer(currentWashId, serviceId, newPrice, newDuration)
            }
            loadServices()
        }
    }

    fun removeService(serviceId: Int) {
        viewModelScope.launch {
            when (currentType) {
                WashType.NORMAL -> carWashServiceDao.deactivateService(currentWashId, serviceId)
                WashType.SELF -> selfWashServiceDao.deactivateService(currentWashId, serviceId)
                WashType.AUTOMATIC -> autoWashServiceDao.deactivateService(currentWashId, serviceId)
            }
            loadServices()
        }
    }

    fun addService(serviceId: Int, price: Float, duration: Int) {
        viewModelScope.launch {
            when (currentType) {
                WashType.NORMAL -> carWashServiceDao.insert(CarWashService(currentWashId, serviceId, price, duration))
                WashType.SELF -> selfWashServiceDao.insert(SelfWashService(currentWashId, serviceId, price, duration))
                WashType.AUTOMATIC -> autoWashServiceDao.insert(AutoWashService(currentWashId, serviceId, price, duration))
            }
            loadServices()
        }
    }
}
