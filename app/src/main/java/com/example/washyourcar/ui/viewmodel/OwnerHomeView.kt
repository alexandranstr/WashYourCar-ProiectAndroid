package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.CarWashDao
import com.example.washyourcar.data.dao.CarWashServiceDao
import com.example.washyourcar.data.dao.OwnerDao
import com.example.washyourcar.data.dao.ReviewDao
import com.example.washyourcar.data.entities.CarWash
import com.example.washyourcar.data.entities.CarWashServiceDetails
import com.example.washyourcar.data.entities.Owner
import com.example.washyourcar.data.entities.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OwnerHomeViewModel(
    private val ownerDao: OwnerDao,
    private val carWashServiceDao: CarWashServiceDao,
    private val carWashDao: CarWashDao,
    private val reviewDao: ReviewDao
) : ViewModel() {

    private val _currentOwner = MutableStateFlow<Owner?>(null)
    val currentOwner: StateFlow<Owner?> = _currentOwner.asStateFlow()

    private val _carWash = MutableStateFlow<CarWash?>(null)
    val carWash: StateFlow<CarWash?> = _carWash.asStateFlow()

    private val _services = MutableStateFlow<List<CarWashServiceDetails>>(emptyList())
    val services: StateFlow<List<CarWashServiceDetails>> = _services.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _computedRating = MutableStateFlow<Float?>(null)
    val computedRating: StateFlow<Float?> = _computedRating.asStateFlow()

    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount.asStateFlow()

    fun initOwner(firebaseUid: String) {
        viewModelScope.launch {
            _currentOwner.value = ownerDao.getById(firebaseUid)
            val carWash = carWashDao.getCarWashByOwnerId(firebaseUid)
            _carWash.value = carWash
            carWash?.let {
                _services.value = carWashServiceDao.getServicesByCarWashId(it.carWashId)
                _reviews.value = reviewDao.getReviewsByCarWashIdOnce(it.carWashId)
                _reviewCount.value = reviewDao.getReviewCount(it.carWashId)
                _computedRating.value = reviewDao.getAverageRating(it.carWashId)
            }
        }
    }
}