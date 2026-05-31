package com.przevolut.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.dao.RateDao
import com.przevolut.data.local.entity.RateEntity
import com.przevolut.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val rates: List<RateEntity>,
        val isOffline: Boolean = false,
        val lastUpdated: String = ""
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val rateDao: RateDao,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        // Obserwuj lokalne kursy z Room (automatyczna aktualizacja)
        viewModelScope.launch {
            rateDao.getLatestRates().collect { localRates ->
                if (localRates.isNotEmpty()) {
                    val lastFetch = localRates.maxOfOrNull { it.fetchedAt } ?: 0L
                    val dateStr = formatTimestamp(lastFetch)
                    _uiState.value = DashboardUiState.Success(
                        rates = localRates,
                        isOffline = false,
                        lastUpdated = dateStr
                    )
                }
            }
        }

        // Przy inicjalizacji pobierz świeże kursy z API
        refreshRates()
    }

    fun refreshRates() {
        viewModelScope.launch {
            try {
                val response = apiService.getRates()
                if (response.isSuccessful) {
                    val remoteRates = response.body()?.rates ?: return@launch
                    val now = System.currentTimeMillis()
                    val entities = remoteRates.map { r ->
                        RateEntity(
                            currency = r.currency,
                            rate = r.mid ?: r.rate ?: 0.0,
                            mid = r.mid ?: r.rate ?: 0.0,
                            effectiveDate = r.effectiveDate ?: "",
                            fetchedAt = now
                        )
                    }
                    rateDao.upsertRates(entities)
                    // Room Flow automatycznie zaktualizuje UI
                } else {
                    showOfflineIfNoData()
                }
            } catch (e: Exception) {
                showOfflineIfNoData()
            }
        }
    }

    private suspend fun showOfflineIfNoData() {
        val localRates = rateDao.getLatestRates().firstOrNull()
        if (localRates.isNullOrEmpty()) {
            _uiState.value = DashboardUiState.Error(
                "Brak połączenia z internetem i brak lokalnych danych. Połącz się z siecią."
            )
        } else {
            val lastFetch = localRates.maxOfOrNull { it.fetchedAt } ?: 0L
            _uiState.value = DashboardUiState.Success(
                rates = localRates,
                isOffline = true,
                lastUpdated = formatTimestamp(lastFetch)
            )
        }
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
