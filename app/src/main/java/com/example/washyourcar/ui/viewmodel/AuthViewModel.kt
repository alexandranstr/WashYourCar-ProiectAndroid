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
    val errorMessage: String? = null,
    /** Neutral feedback, e.g. "Password reset email sent". */
    val infoMessage: String? = null
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

    // ---------------- Auth helpers ----------------

    /** Turns Firebase errors into messages a user can act on (Firebase's own text is vague). */
    private fun friendlyAuthError(error: Exception): String = when {
        error is com.google.firebase.FirebaseNetworkException -> "No internet connection. Check it and try again."
        error is com.google.firebase.auth.FirebaseAuthException -> when (error.errorCode) {
            // Newer Firebase projects return INVALID_CREDENTIAL for both a wrong password and an unknown email.
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                "Wrong email or password. Check them, or tap \"Forgot password?\" to reset it."
            "ERROR_USER_NOT_FOUND" -> "No account with this email. Sign up first."
            "ERROR_INVALID_EMAIL" -> "That email address isn't valid."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email already has an account. Log in instead."
            "ERROR_WEAK_PASSWORD" -> "Password is too weak – use at least 6 characters."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a few minutes and try again."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            else -> error.localizedMessage ?: "Sign-in failed."
        }
        error is com.google.firebase.FirebaseTooManyRequestsException -> "Too many attempts. Wait a few minutes and try again."
        else -> error.localizedMessage ?: "Something went wrong."
    }

    /**
     * Creates the Firebase account, or, if the email is already registered (e.g. a previous sign-up
     * created the Firebase account but the profile was never saved on this device), signs in with the
     * same password so the profile can be completed instead of leaving the user stuck.
     */
    private fun createOrRecoverAccount(email: String, password: String, onReady: (uid: String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result -> result.user?.uid?.let(onReady) ?: fail("Could not create the account.") }
            .addOnFailureListener { error ->
                if (error is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            Log.d("Auth", "Email already registered; signed in to finish the profile")
                            viewModelScope.launch {
                                pullFromCloud()
                                result.user?.uid?.let(onReady) ?: fail("Could not sign in.")
                            }
                        }
                        .addOnFailureListener {
                            fail("This email already has an account with a different password. Log in, or use \"Forgot password?\".")
                        }
                } else {
                    Log.e("Auth", "Register error: ${error.message}")
                    fail(friendlyAuthError(error))
                }
            }
    }

    /** Downloads the cloud copy into the local database; gives up after 8 s so login never hangs offline. */
    private suspend fun pullFromCloud() {
        kotlinx.coroutines.withTimeoutOrNull(8_000) { com.example.washyourcar.data.CloudSync.pullAll() }
    }

    private fun fail(message: String) {
        _authState.value = AuthState(errorMessage = message)
    }

    /** Sends Firebase's password reset email. */
    fun sendPasswordReset(email: String) {
        val clean = email.trim()
        if (clean.isBlank()) { fail("Enter your email first, then tap \"Forgot password?\"."); return }
        _authState.value = AuthState(isLoading = true)
        auth.sendPasswordResetEmail(clean)
            .addOnSuccessListener { _authState.value = AuthState(infoMessage = "Password reset email sent to $clean. Check your inbox (and spam).") }
            .addOnFailureListener { fail(friendlyAuthError(it)) }
    }

    fun login(email: String, parola: String, onSuccess: () -> Unit) {
        _authState.value = AuthState(isLoading = true)

        auth.signInWithEmailAndPassword(email.trim(), parola)
            .addOnSuccessListener {
                Log.d("Auth", "Login succesfull")
                viewModelScope.launch {
                    pullFromCloud() // restores this account's washes / profile on a new device
                    _authState.value = AuthState()
                    onSuccess()
                }
            }
            .addOnFailureListener { error ->
                Log.e("Auth", "Login error: ${error.message}")
                fail(friendlyAuthError(error))
            }
    }

    fun loginClient(email: String, parola: String, onSuccess: () -> Unit) {
        _authState.value = AuthState(isLoading = true)

        auth.signInWithEmailAndPassword(email.trim(), parola)
            .addOnSuccessListener { result ->
                val firebaseUid = result.user?.uid
                viewModelScope.launch {
                    try {
                        pullFromCloud() // the profile may only exist in the cloud on this device
                        val hasCustomerProfile =firebaseUid?.let { customerDao.getCustomersById(it) != null } == true
                        val hasRegisteredCar = firebaseUid?.let { carDao.getCarsByCustomerId(it).isNotEmpty() } == true
                        if (hasCustomerProfile && hasRegisteredCar) {
                            Log.d("Auth", "Client login successful with complete profile")
                            _authState.value = AuthState()
                            onSuccess()
                        } else {
                            auth.signOut()
                            // Profiles are stored on each device (not in the cloud yet), so a valid account can
                            // have no profile here. Signing up again with the same email + password restores it.
                            _authState.value = AuthState(
                                errorMessage = "Your password is correct, but this device has no profile for this account. " +
                                    "Tap \"Sign up\" and register again with the same email and password to restore it."
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
                fail(friendlyAuthError(error))
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

        createOrRecoverAccount(email.trim(), password) { firebaseUid ->
            Log.d("Auth", "Owner account ready in Firebase")
            val noulOwner = Owner(
                firebaseUid = firebaseUid,
                firstName = firstName,
                lastName = lastName,
                email = email.trim(),
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
                    fail("Database failed: ${e.localizedMessage}")
                }
            }
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

        createOrRecoverAccount(email.trim(), parola) { firebaseUid ->
            Log.d("Auth", "Client account ready")
            val noulClient = Customer(
                firebaseUid = firebaseUid,
                firstName = prenume,
                lastName = nume,
                email = email.trim(),
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
                    fail("Database failed: ${e.localizedMessage}")
                }
            }
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

        viewModelScope.launch {
            // Check the access code BEFORE creating the Firebase account, so a typo doesn't leave
            // a Firebase account with no employee profile (which then blocks this email).
            val code = accessToken.trim().uppercase()
            val carWashId = try { carWashDao.getCarWashIdByAccessCode(code) } catch (e: Exception) { null }
            if (carWashId == null) {
                fail("Invalid access code! Please check and try again.")
                return@launch
            }
            createOrRecoverAccount(email.trim(), password) { firebaseUid ->
                viewModelScope.launch {
                    try {
                        employeeDao.insert(
                            Employee(
                                firebaseUid = firebaseUid,
                                carWashId = carWashId,
                                firstName = firstName,
                                lastName = lastName,
                                email = email.trim(),
                                phoneNumber = phone,
                                accessToken = code
                            )
                        )
                        Log.d("Auth", "Employee registered and assigned to car wash $carWashId")
                        _authState.value = AuthState()
                        onSuccess()
                    } catch (e: Exception) {
                        Log.e("Auth", "Employee register error: ${e.message}")
                        fail("Database failed: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
}