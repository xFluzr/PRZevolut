package com.przevolut.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.remote.ApiService
import com.przevolut.data.remote.model.AlertResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AlertsUiState {
    object Loading : AlertsUiState()
    data class Success(val alerts: List<AlertResponse>) : AlertsUiState()
    data class Error(val message: String) : AlertsUiState()
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState

    // TODO: Token pobierać z SessionManager/EncryptedSharedPreferences
    private var token: String = ""

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        if (token.isBlank()) {
            _uiState.value = AlertsUiState.Error("Zaloguj się, aby zobaczyć alerty.")
            return
        }
        _uiState.value = AlertsUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getAlerts("Bearer $token")
                if (response.isSuccessful) {
                    _uiState.value = AlertsUiState.Success(response.body() ?: emptyList())
                } else {
                    _uiState.value = AlertsUiState.Error("Błąd pobierania alertów.")
                }
            } catch (e: Exception) {
                _uiState.value = AlertsUiState.Error("Brak połączenia z serwerem.")
            }
        }
    }

    fun deleteAlert(alertId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteAlert("Bearer $token", alertId)
                if (response.isSuccessful) {
                    loadAlerts() // Odśwież listę
                }
            } catch (e: Exception) {
                _uiState.value = AlertsUiState.Error("Nie udało się usunąć alertu.")
            }
        }
    }

    fun setToken(jwtToken: String) {
        token = jwtToken
        loadAlerts()
    }
}
