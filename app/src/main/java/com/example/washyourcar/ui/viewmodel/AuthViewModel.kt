package com.example.washyourcar.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.washyourcar.data.AppDatabase
import com.example.washyourcar.data.entities.*
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

    private val customerDao = AppDatabase.getDatabase(application).customerDao()
    private val carDao = AppDatabase.getDatabase(application).carDao()
    private val ownerDao = AppDatabase.getDatabase(application).ownerDao()
    private val carWashDao = AppDatabase.getDatabase(application).carWashDao()
    private val _authState = MutableStateFlow(AuthState())

    private val employeeDao = AppDatabase.getDatabase(application).employeeDao()
    private val carWashServiceDao = AppDatabase.getDatabase(application).carWashServiceDao()
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

    fun registerWashDetails(
        washName: String,
        address: String,
        city: String,
        phone: String,
        openTime: Long,
        closeTime: Long,
        serviceData: List<Triple<Int, Float, Int>>,
        onAccessCodeGenerated: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        if (washName.isBlank() || address.isBlank() || city.isBlank() || phone.isBlank()) {
            _authState.value = AuthState(errorMessage = "Please fill in all fields.")
            return
        }

        if (serviceData.isEmpty()) {
            _authState.value = AuthState(errorMessage = "Please select at least one service.")
            return
        }

        _authState.value = AuthState(isLoading = true)

        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid == null) {
            _authState.value = AuthState(errorMessage = "No owner session found.")
            return
        }

        viewModelScope.launch {
            try {
                val accessCode = generateUniqueAccessCode(carWashDao)
                Log.d("Auth", "Generated access code: $accessCode")

                val allCarWashes = carWashDao.getAllCarWashes()
                val newCarWashId = if (allCarWashes.isEmpty()) 200 else allCarWashes.maxOf { it.carWashId } + 1

                val carWash = CarWash(
                    carWashId = newCarWashId,
                    ownerId = firebaseUid,
                    name = washName,
                    address = address,
                    city = city,
                    phoneNumber = phone,
                    rating = 0f,
                    accessCode = accessCode,
                    openTime = openTime,
                    closeTime = closeTime
                )

                carWashDao.insertAndReturnId(carWash)
                Log.d("Room", "CarWash saved with id $newCarWashId")

                serviceData.forEach { (serviceId, price, duration) ->
                    val carWashService = CarWashService(
                        carWashId = newCarWashId,
                        serviceId = serviceId,
                        price = price,
                        duration = duration
                    )
                    carWashServiceDao.insert(carWashService)
                    Log.d("Room", "Service $serviceId linked to carWash $newCarWashId")
                }

                onAccessCodeGenerated(accessCode)

                _authState.value = AuthState()
                onSuccess()
            } catch (e: Exception) {
                Log.e("Room", "Error saving car wash: ${e.message}")
                _authState.value = AuthState(errorMessage = "Database failed: ${e.localizedMessage}")
            }
        }
    }

    private fun generateUniqueAccessCode(carWashDao: com.example.washyourcar.data.dao.CarWashDao): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var code: String
        kotlinx.coroutines.runBlocking {
            do {
                code = (1..6).map { chars.random() }.joinToString("")
            } while (carWashDao.doesAccessCodeExist(code))
        }
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