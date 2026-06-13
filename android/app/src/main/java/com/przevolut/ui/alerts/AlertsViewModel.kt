package com.przevolut.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.TokenManager
import com.przevolut.data.remote.ApiService
import com.przevolut.data.remote.model.AlertRequest
import com.przevolut.data.remote.model.AlertResponse
import com.przevolut.data.remote.model.AlertUpdateRequest
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

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        if (tokenManager.getToken() == null) {
            _uiState.value = AlertsUiState.Error("Zaloguj się, aby zobaczyć alerty.")
            return
        }
        _uiState.value = AlertsUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getAlerts()
                if (response.isSuccessful) {
                    _uiState.value = AlertsUiState.Success(response.body() ?: emptyList())
                } else {
                    _uiState.value = AlertsUiState.Error(
                        "Błąd pobierania alertów (${response.code()})."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AlertsUiState.Error("Brak połączenia z serwerem.")
            }
        }
    }

    fun deleteAlert(alertId: Int) {
        if (tokenManager.getToken() == null) return
        viewModelScope.launch {
            try {
                val response = apiService.deleteAlert(alertId)
                if (response.isSuccessful) {
                    loadAlerts()
                } else {
                    _uiState.value = AlertsUiState.Error("Nie udało się usunąć alertu.")
                }
            } catch (e: Exception) {
                _uiState.value = AlertsUiState.Error("Nie udało się usunąć alertu.")
            }
        }
    }

    fun createAlert(currency: String, direction: String, threshold: Double) {
        if (tokenManager.getToken() == null) {
            _uiState.value = AlertsUiState.Error("Zaloguj się, aby dodać alert.")
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.createAlert(
                    AlertRequest(currency = currency, direction = direction, threshold = threshold)
                )
                if (response.isSuccessful) {
                    loadAlerts()
                } else {
                    _uiState.value = AlertsUiState.Error(
                        "Nie udało się utworzyć alertu (${response.code()})."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AlertsUiState.Error("Brak połączenia z serwerem.")
            }
        }
    }

    fun updateAlert(alertId: Int, direction: String, threshold: Double) {
        if (tokenManager.getToken() == null) return
        viewModelScope.launch {
            try {
                val response = apiService.updateAlert(
                    alertId,
                    AlertUpdateRequest(direction = direction, threshold = threshold)
                )
                if (response.isSuccessful) {
                    loadAlerts()
                } else {
                    _uiState.value = AlertsUiState.Error("Nie udało się zaktualizować alertu.")
                }
            } catch (e: Exception) {
                _uiState.value = AlertsUiState.Error("Brak połączenia z serwerem.")
            }
        }
    }
}
