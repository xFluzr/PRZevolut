package com.przevolut.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.TokenManager
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
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState

    private fun authHeader(): String? {
        return tokenManager.getToken()?.let { "Bearer $it" }
    }

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        val header = authHeader()
        if (header == null) {
            _uiState.value = AlertsUiState.Error("Zaloguj się, aby zobaczyć alerty.")
            return
        }
        _uiState.value = AlertsUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getAlerts(header)
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
        val header = authHeader() ?: return
        viewModelScope.launch {
            try {
                val response = apiService.deleteAlert(header, alertId)
                if (response.isSuccessful) {
                    loadAlerts()
                }
            } catch (e: Exception) {
                _uiState.value = AlertsUiState.Error("Nie udało się usunąć alertu.")
            }
        }
    }

    fun createAlert(currency: String, direction: String, targetRate: Double) {
        val header = authHeader()
        if (header == null) {
            _uiState.value = AlertsUiState.Error("Zaloguj się, aby dodać alert.")
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.createAlert(
                    header,
                    com.przevolut.data.remote.model.AlertRequest(
                        currency = currency,
                        direction = direction,
                        targetRate = targetRate
                    )
                )
                if (response.isSuccessful) {
                    loadAlerts()
                } else {
                    _uiState.value = AlertsUiState.Error("Nie udało się utworzyć alertu.")
                }
            } catch (e: Exception) {
                _uiState.value = AlertsUiState.Error("Brak połączenia z serwerem.")
            }
        }
    }
}
