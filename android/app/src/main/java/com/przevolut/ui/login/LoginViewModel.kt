package com.przevolut.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.TokenManager
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
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val token = response.body()?.accessToken ?: ""
                    tokenManager.saveToken(token)
                    tokenManager.saveUserEmail(email)
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
                when {
                    response.isSuccessful -> login(email, password)
                    response.code() == 409 -> _uiState.value = LoginUiState.Error(
                        "Ten e-mail jest już zarejestrowany. Przełącz się na „Logowanie” i użyj swojego hasła."
                    )
                    else -> _uiState.value = LoginUiState.Error(
                        "Rejestracja nie powiodła się. Spróbuj ponownie."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Brak połączenia z serwerem.")
            }
        }
    }

    fun loginWithBiometric() {
        val token = tokenManager.getToken()
        if (token != null) {
            _uiState.value = LoginUiState.Success(token)
        } else {
            _uiState.value = LoginUiState.Error("Brak zapisanych danych logowania. Zaloguj się hasłem.")
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
