package com.example.washyourcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.dao.AutoWashDao
import com.example.washyourcar.data.dao.AutoWashServiceDao
import com.example.washyourcar.data.dao.CarWashDao
import com.example.washyourcar.data.dao.CarWashServiceDao
import com.example.washyourcar.data.dao.OwnerDao
import com.example.washyourcar.data.dao.ReviewDao
import com.example.washyourcar.data.dao.SelfWashDao
import com.example.washyourcar.data.dao.SelfWashServiceDao
import com.example.washyourcar.data.entities.AutoWash
import com.example.washyourcar.data.entities.AutoWashServiceDetails
import com.example.washyourcar.data.entities.CarWash
import com.example.washyourcar.data.entities.CarWashServiceDetails
import com.example.washyourcar.data.entities.Owner
import com.example.washyourcar.data.entities.Review
import com.example.washyourcar.data.entities.SelfWash
import com.example.washyourcar.data.entities.SelfWashServiceDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OwnerHomeViewModel(
    private val ownerDao: OwnerDao,
    private val carWashDao: CarWashDao,
    private val autoWashDao: AutoWashDao,
    private val selfWashDao: SelfWashDao,
    private val carWashServiceDao: CarWashServiceDao,
    private val autoWashServiceDao: AutoWashServiceDao,
    private val selfWashServiceDao: SelfWashServiceDao,
    private val reviewDao: ReviewDao
) : ViewModel() {

    private val _currentOwner = MutableStateFlow<Owner?>(null)
    val currentOwner: StateFlow<Owner?> = _currentOwner.asStateFlow()

    private val _carWash = MutableStateFlow<CarWash?>(null)
    val carWash: StateFlow<CarWash?> = _carWash.asStateFlow()

    private val _selfWash = MutableStateFlow<SelfWash?>(null)
    val selfWash: StateFlow<SelfWash?> = _selfWash.asStateFlow()

    private val _autoWash = MutableStateFlow<AutoWash?>(null)
    val autoWash: StateFlow<AutoWash?> = _autoWash.asStateFlow()

    private val _services = MutableStateFlow<List<CarWashServiceDetails>>(emptyList())
    val services: StateFlow<List<CarWashServiceDetails>> = _services.asStateFlow()

    private val _selfServices = MutableStateFlow<List<SelfWashServiceDetails>>(emptyList())
    val selfServices: StateFlow<List<SelfWashServiceDetails>> = _selfServices.asStateFlow()

    private val _autoServices = MutableStateFlow<List<AutoWashServiceDetails>>(emptyList())
    val autoServices: StateFlow<List<AutoWashServiceDetails>> = _autoServices.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _computedRating = MutableStateFlow<Float?>(null)
    val computedRating: StateFlow<Float?> = _computedRating.asStateFlow()

    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount.asStateFlow()

    fun initOwner(firebaseUid: String) {
        viewModelScope.launch {
            _currentOwner.value = ownerDao.getById(firebaseUid)

            // 1. Verificăm dacă e spălătorie Normală (Clasică)
            val normal = carWashDao.getCarWashByOwnerId(firebaseUid)
            if (normal != null) {
                _carWash.value = normal
                _services.value = carWashServiceDao.getServicesByCarWashId(normal.carWashId)
                loadReviews(normal.carWashId)
                return@launch
            }

            // 2. Verificăm dacă e Self Wash
            val self = selfWashDao.getSelfWashByOwnerId(firebaseUid)
            if (self != null) {
                _selfWash.value = self
                _selfServices.value = selfWashServiceDao.getServicesBySelfWashId(self.selfWashId)
                loadReviews(self.selfWashId)
                return@launch
            }

            // 3. Verificăm dacă e Auto Wash (Automată)
            val auto = autoWashDao.getAutoWashByOwnerId(firebaseUid)
            if (auto != null) {
                _autoWash.value = auto
                _autoServices.value = autoWashServiceDao.getServicesByAutoWashId(auto.autoWashId)
                loadReviews(auto.autoWashId)
                return@launch
            }
        }
    }

    private suspend fun loadReviews(washId: Int) {
        _reviews.value = reviewDao.getReviewsByCarWashIdOnce(washId)
        _reviewCount.value = reviewDao.getReviewCount(washId)
        _computedRating.value = reviewDao.getAverageRating(washId)
    }
}