package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.CarWashServiceDao
import com.example.washyourcar.data.dao.OwnerDao
import com.example.washyourcar.data.entities.CarWashServiceDetails
import com.example.washyourcar.data.entities.Owner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OwnerHomeViewModel(
    private val ownerDao: OwnerDao,
    private val carWashServiceDao: CarWashServiceDao
) : ViewModel() {

    private val _currentOwner = MutableStateFlow<Owner?>(null)
    val currentOwner: StateFlow<Owner?> = _currentOwner.asStateFlow()

    private val _services = MutableStateFlow<List<CarWashServiceDetails>>(emptyList())
    val services: StateFlow<List<CarWashServiceDetails>> = _services.asStateFlow()

    fun initOwner(firebaseUid: String) {
        viewModelScope.launch {
            val carWashId = 1
            _services.value = carWashServiceDao.getServicesByCarWashId(carWashId)
        }
    }
}