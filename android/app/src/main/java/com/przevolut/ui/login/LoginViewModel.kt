package com.przevolut.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.remote.ApiService
import com.przevolut.data.remote.model.LoginRequest
import com.przevolut.data.remote.model.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val token: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    // W prawdziwej aplikacji token jest zapisywany w EncryptedSharedPreferences
    private var savedToken: String? = null

    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val token = response.body()?.accessToken ?: ""
                    savedToken = token
                    _uiState.value = LoginUiState.Success(token)
                } else {
                    _uiState.value = LoginUiState.Error("Nieprawidłowy e-mail lub hasło.")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Brak połączenia z serwerem. Sprawdź internet.")
            }
        }
    }

    fun register(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.register(RegisterRequest(email, password))
                if (response.isSuccessful) {
                    // Po rejestracji automatycznie logujemy
                    login(email, password)
                } else {
                    _uiState.value = LoginUiState.Error("Rejestracja nie powiodła się. E-mail może już być zajęty.")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Brak połączenia z serwerem.")
            }
        }
    }

    fun loginWithBiometric() {
        // W produkcji: pobierz zapisany token z EncryptedSharedPreferences
        savedToken?.let {
            _uiState.value = LoginUiState.Success(it)
        } ?: run {
            _uiState.value = LoginUiState.Error("Brak zapisanych danych logowania. Zaloguj się hasłem.")
        }
    }
}
