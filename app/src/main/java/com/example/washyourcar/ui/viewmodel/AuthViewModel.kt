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
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

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

    fun logout() {
        auth.signOut()
    }
}