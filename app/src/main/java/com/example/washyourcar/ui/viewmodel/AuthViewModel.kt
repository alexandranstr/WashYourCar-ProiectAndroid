package com.example.washyourcar.viewmodel

import AppDatabase
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.entities.AutoWash
import com.example.washyourcar.data.entities.AutoWashService
import com.example.washyourcar.data.entities.Car
import com.example.washyourcar.data.entities.CarWash
import com.example.washyourcar.data.entities.CarWashService
import com.example.washyourcar.data.entities.Customer
import com.example.washyourcar.data.entities.Employee
import com.example.washyourcar.data.entities.Owner
import com.example.washyourcar.data.entities.SelfWash
import com.example.washyourcar.data.entities.SelfWashService
import com.example.washyourcar.data.entities.WashType
import com.example.washyourcar.models.ServiceOption
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = AppDatabase.getDatabase(application)

    private val customerDao = database.customerDao()
    private val carDao = database.carDao()
    private val ownerDao = database.ownerDao()
    private val employeeDao = database.employeeDao()

    private val carWashDao = database.carWashDao()
    private val autoWashDao = database.autoWashDao()
    private val selfWashDao = database.selfWashDao()

    private val carWashServiceDao = database.carWashServiceDao()
    private val autoWashServiceDao = database.autoWashServiceDao()
    private val selfWashServiceDao = database.selfWashServiceDao()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    sealed class UserRole {
        object Client : UserRole()
        object Owner : UserRole()
        object Employee : UserRole()
        object Unknown : UserRole()
    }

    suspend fun getUserRole(): UserRole {
        val firebaseUid = auth.currentUser?.uid ?: return UserRole.Unknown
        return when {
            customerDao.getCustomersById(firebaseUid) != null -> UserRole.Client
            ownerDao.getById(firebaseUid) != null -> UserRole.Owner
            employeeDao.getById(firebaseUid) != null -> UserRole.Employee
            else -> UserRole.Unknown
        }
    }

    fun login(email: String, parola: String, onSuccess: () -> Unit) {
        _authState.value = AuthState(isLoading = true)

        auth.signInWithEmailAndPassword(email, parola)
            .addOnSuccessListener {
                Log.d("Auth", "Login succesfull")
                _authState.value = AuthState()
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e("Auth", "Login error: ${error.message}")
                _authState.value = AuthState(errorMessage = error.message)
            }
    }

    fun loginClient(email: String, parola: String, onSuccess: () -> Unit) {
        _authState.value = AuthState(isLoading = true)

        auth.signInWithEmailAndPassword(email, parola)
            .addOnSuccessListener { result ->
                val firebaseUid = result.user?.uid
                viewModelScope.launch {
                    try {
                        val hasCustomerProfile = firebaseUid?.let { customerDao.getCustomersById(it) != null } == true
                        val hasRegisteredCar = firebaseUid?.let { carDao.getCarsByCustomerId(it).isNotEmpty() } == true
                        if (hasCustomerProfile && hasRegisteredCar) {
                            Log.d("Auth", "Client login successful with complete profile")
                            _authState.value = AuthState()
                            onSuccess()
                        } else {
                            auth.signOut()
                            _authState.value = AuthState(
                                errorMessage = "This client account does not have a complete profile. Please register again."
                            )
                        }
                    } catch (error: Exception) {
                        auth.signOut()
                        _authState.value = AuthState(
                            errorMessage = "Could not load your profile. Please try again."
                        )
                        Log.e("Auth", "Client profile validation error: ${error.message}")
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.e("Auth", "Client login error: ${error.message}")
                _authState.value = AuthState(errorMessage = error.message)
            }
    }

    fun registerOwner(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
            _authState.value = AuthState(errorMessage = "Complete all fields!")
            return
        }

        _authState.value = AuthState(isLoading = true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Log.d("Auth", "Owner account created in Firebase")

                val firebaseUid = authResult.user?.uid

                if (firebaseUid != null) {
                    val noulOwner = Owner(
                        firebaseUid = firebaseUid,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phoneNumber = phone
                    )

                    viewModelScope.launch {
                        try {
                            ownerDao.insertOwner(noulOwner)
                            Log.d("Room", "Owner saved to local database")

                            _authState.value = AuthState()
                            onSuccess()
                        } catch (e: Exception) {
                            Log.e("Room", "Error saving owner: ${e.message}")
                            _authState.value = AuthState(
                                errorMessage = "Database failed: ${e.localizedMessage}"
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.e("Auth", "Owner register error: ${error.message}")
                _authState.value = AuthState(errorMessage = error.message)
            }
    }

    suspend fun getServiceOptions(type: WashType): List<ServiceOption> = when (type) {
        WashType.NORMAL -> carWashServiceDao.getAllServices()
            .map { ServiceOption(it.serviceId, it.name, it.description) }
        WashType.AUTOMATIC -> autoWashServiceDao.getAllServices()
            .map { ServiceOption(it.autoServiceId, it.name, it.description) }
        WashType.SELF -> selfWashServiceDao.getAllServices()
            .map { ServiceOption(it.selfServiceId, it.name, it.description) }
    }

    fun registerWashBasics(
        washType: WashType,
        washName: String,
        address: String,
        city: String,
        phone: String,
        openTime: Long,
        closeTime: Long,
        normalBays: Int,
        premiumBays: Int,
        automaticBays: Int,
        latitude: Double? = null,
        longitude: Double? = null,
        onSuccess: (washId: Int) -> Unit
    ) {
        if (washName.isBlank() || address.isBlank() || city.isBlank() || phone.isBlank()) {
            _authState.value = AuthState(errorMessage = "Please fill in all fields.")
            return
        }

        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid == null) {
            _authState.value = AuthState(errorMessage = "No owner session found.")
            return
        }

        _authState.value = AuthState(isLoading = true)

        viewModelScope.launch {
            try {
                val newId = when (washType) {
                    WashType.NORMAL -> {
                        val accessCode = generateUniqueAccessCode()
                        Log.d("Auth", "Generated access code: $accessCode")

                        val id = (carWashDao.getAllCarWashes().maxOfOrNull { it.carWashId } ?: 199) + 1
                        carWashDao.insertAndReturnId(
                            CarWash(
                                carWashId = id,
                                ownerId = firebaseUid,
                                name = washName,
                                address = address,
                                city = city,
                                phoneNumber = phone,
                                rating = 0f,
                                accessCode = accessCode,
                                openTime = openTime,
                                closeTime = closeTime,
                                latitude = latitude,
                                longitude = longitude
                            )
                        )
                        id
                    }

                    WashType.AUTOMATIC -> {
                        val id = (autoWashDao.getAllAutoWashes().maxOfOrNull { it.autoWashId } ?: 199) + 1
                        autoWashDao.insertAndReturnId(
                            AutoWash(
                                autoWashId = id,
                                ownerId = firebaseUid,
                                name = washName,
                                address = address,
                                city = city,
                                phoneNumber = phone,
                                rating = 0f,
                                openTime = openTime,
                                closeTime = closeTime,
                                automaticBays = automaticBays,
                                latitude = latitude,
                                longitude = longitude
                            )
                        )
                        id
                    }

                    WashType.SELF -> {
                        val id = (selfWashDao.getAllSelfWashes().maxOfOrNull { it.selfWashId } ?: 199) + 1
                        selfWashDao.insertAndReturnId(
                            SelfWash(
                                selfWashId = id,
                                ownerId = firebaseUid,
                                name = washName,
                                address = address,
                                city = city,
                                phoneNumber = phone,
                                rating = 0f,
                                openTime = openTime,
                                closeTime = closeTime,
                                premiumSelfWashBays = premiumBays,
                                normalSelfWashBays = normalBays,
                                latitude = latitude,
                                longitude = longitude
                            )
                        )
                        id
                    }
                }

                Log.d("Room", "${washType.name} wash saved with id $newId")
                _authState.value = AuthState()
                onSuccess(newId)
            } catch (e: Exception) {
                Log.e("Room", "Error saving wash: ${e.message}")
                _authState.value = AuthState(errorMessage = "Database failed: ${e.localizedMessage}")
            }
        }
    }

    fun saveWashServices(
        washType: WashType,
        washId: Int,
        serviceData: List<Triple<Int, Float, Int>>,
        onSuccess: () -> Unit
    ) {
        if (serviceData.isEmpty()) {
            _authState.value = AuthState(errorMessage = "Please select at least one service.")
            return
        }

        _authState.value = AuthState(isLoading = true)

        viewModelScope.launch {
            try {
                serviceData.forEach { (serviceId, price, duration) ->
                    when (washType) {
                        WashType.NORMAL -> carWashServiceDao.insert(
                            CarWashService(carWashId = washId, serviceId = serviceId, price = price, duration = duration)
                        )
                        WashType.AUTOMATIC -> autoWashServiceDao.insert(
                            AutoWashService(autoWashId = washId, autoServiceId = serviceId, price = price, duration = duration)
                        )
                        WashType.SELF -> selfWashServiceDao.insert(
                            SelfWashService(selfWashId = washId, selfServiceId = serviceId, price = price, duration = duration)
                        )
                    }
                    Log.d("Room", "Service $serviceId linked to ${washType.name} wash $washId")
                }

                _authState.value = AuthState()
                onSuccess()
            } catch (e: Exception) {
                Log.e("Room", "Error saving services: ${e.message}")
                _authState.value = AuthState(errorMessage = "Database failed: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun generateUniqueAccessCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var code: String
        do {
            code = (1..6).map { chars.random() }.joinToString("")
        } while (carWashDao.doesAccessCodeExist(code))
        return code
    }

    fun register(
        nume: String,
        prenume: String,
        email: String,
        telefon: String,
        parola: String,
        numarInmatriculare: String,
        modelMasina: String,
        onSuccess: () -> Unit
    ) {
        if (nume.isBlank() || prenume.isBlank() || email.isBlank() || telefon.isBlank() || parola.isBlank() || numarInmatriculare.isBlank() || modelMasina.isBlank()) {
            _authState.value = AuthState(errorMessage = "Complete all fields!")
            return
        }

        _authState.value = AuthState(isLoading = true)

        auth.createUserWithEmailAndPassword(email, parola)
            .addOnSuccessListener { authResult ->
                Log.d("Auth", "Account created")

                val firebaseUid = authResult.user?.uid

                if (firebaseUid != null) {
                    val noulClient = Customer(
                        firebaseUid = firebaseUid,
                        firstName = prenume,
                        lastName = nume,
                        email = email,
                        phoneNumber = telefon
                    )

                    val masinaClient = Car(
                        licensePlate = numarInmatriculare,
                        customerId = firebaseUid,
                        model = modelMasina,
                        status = "Active"
                    )

                    viewModelScope.launch {
                        try {
                            customerDao.insert(noulClient)
                            carDao.insert(masinaClient)
                            Log.d("Room", "Customer and car saved")

                            _authState.value = AuthState()
                            onSuccess()
                        } catch (e: Exception) {
                            Log.e("Room", "Error: ${e.message}")
                            _authState.value = AuthState(
                                errorMessage = "Database failed: ${e.localizedMessage}"
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.e("Auth", "Register error: ${error.message}")
                _authState.value = AuthState(errorMessage = error.message)
            }
    }

    fun registerEmployee(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        accessToken: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || phone.isBlank() || accessToken.isBlank() || password.isBlank()) {
            _authState.value = AuthState(errorMessage = "Complete all fields!")
            return
        }

        _authState.value = AuthState(isLoading = true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUid = authResult.user?.uid

                if (firebaseUid != null) {
                    viewModelScope.launch {
                        try {
                            val carWashId = carWashDao.getCarWashIdByAccessCode(accessToken)
                            if (carWashId == null) {
                                _authState.value = AuthState(errorMessage = "Invalid access code! Please check and try again.")
                                return@launch
                            }

                            val newEmployee = Employee(
                                firebaseUid = firebaseUid,
                                carWashId = carWashId,
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phoneNumber = phone,
                                accessToken = accessToken
                            )

                            employeeDao.insert(newEmployee)
                            Log.d("Auth", "Employee registered and assigned to car wash $carWashId")
                            _authState.value = AuthState()
                            onSuccess()
                        } catch (e: Exception) {
                            Log.e("Auth", "Employee register error: ${e.message}")
                            _authState.value = AuthState(errorMessage = "Database failed: ${e.localizedMessage}")
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                _authState.value = AuthState(errorMessage = error.message)
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
}