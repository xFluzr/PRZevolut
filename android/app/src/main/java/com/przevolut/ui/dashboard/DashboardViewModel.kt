package com.przevolut.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.DashboardWatchlistStore
import com.przevolut.data.local.dao.RateDao
import com.przevolut.data.local.entity.RateEntity
import com.przevolut.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import com.przevolut.MainActivity
import com.przevolut.R

data class ChartPoint(val timestamp: Long, val ratePln: Float)

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val rates: List<RateEntity>,
        val watchedCurrencies: Set<String>,
        val chartSeries: Map<String, List<ChartPoint>>,
        val isOffline: Boolean = false,
        val lastUpdated: String = ""
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rateDao: RateDao,
    private val apiService: ApiService,
    private val watchlistStore: DashboardWatchlistStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState

    private var watchedCurrencies = watchlistStore.getWatchedCurrencies()
    private var latestRates: List<RateEntity> = emptyList()
    private var isOffline = false
    private var lastUpdated = ""

    init {
        viewModelScope.launch {
            rateDao.getLatestRates().collect { localRates ->
                if (localRates.isNotEmpty()) {
                    latestRates = localRates
                    val lastFetch = localRates.maxOfOrNull { it.fetchedAt } ?: 0L
                    lastUpdated = formatTimestamp(lastFetch)
                    if (!isOffline) {
                        publishSuccess()
                        loadChartData()
                    }
                }
            }
        }
        refreshRates()
    }

    fun reloadWatchlist() {
        val latestWatchlist = watchlistStore.getWatchedCurrencies()
        if (latestWatchlist != watchedCurrencies) {
            watchedCurrencies = latestWatchlist.toMutableSet()
            publishSuccess()
            loadChartData()
        }
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
                    isOffline = false
                    loadChartData()
                    checkAlertsLocally(entities)
                } else {
                    showOfflineIfNoData()
                }
            } catch (e: Exception) {
                showOfflineIfNoData()
            }
        }
    }

    fun toggleCurrency(currency: String) {
        if (currency == watchlistStore.getDefaultCurrency()) return

        watchedCurrencies = watchedCurrencies.toMutableSet().apply {
            if (contains(currency)) {
                remove(currency)
            } else {
                add(currency)
            }
        }
        watchlistStore.saveWatchedCurrencies(watchedCurrencies)
        publishSuccess()
        loadChartData()
    }

    fun setWatchedCurrencies(currencies: Set<String>) {
        if (currencies.isNotEmpty()) {
            watchedCurrencies = currencies.toMutableSet()
            watchlistStore.saveWatchedCurrencies(watchedCurrencies)
            publishSuccess()
            loadChartData()
        }
    }

    fun getDefaultCurrency(): String = watchlistStore.getDefaultCurrency()

    fun isCurrencyWatched(currency: String): Boolean = currency in watchedCurrencies

    private fun publishSuccess() {
        val filtered = latestRates
            .filter { it.currency in watchedCurrencies }
            .sortedWith(compareBy({ it.currency != watchlistStore.getDefaultCurrency() }, { it.currency }))
        val current = _uiState.value
        val chartSeries = if (current is DashboardUiState.Success) current.chartSeries else emptyMap()
        _uiState.value = DashboardUiState.Success(
            rates = filtered,
            watchedCurrencies = watchedCurrencies,
            chartSeries = chartSeries,
            isOffline = isOffline,
            lastUpdated = lastUpdated
        )
    }

    private fun loadChartData() {
        viewModelScope.launch {
            val series = mutableMapOf<String, List<ChartPoint>>()
            for (code in watchedCurrencies) {
                series[code] = fetchHistory(code)
            }
            val filtered = latestRates
                .filter { it.currency in watchedCurrencies }
                .sortedWith(compareBy({ it.currency != watchlistStore.getDefaultCurrency() }, { it.currency }))
            _uiState.value = DashboardUiState.Success(
                rates = filtered,
                watchedCurrencies = watchedCurrencies,
                chartSeries = series,
                isOffline = isOffline,
                lastUpdated = lastUpdated
            )
        }
    }

    private suspend fun fetchHistory(currency: String): List<ChartPoint> {
        val fromApi = fetchDailyHistoryFromApi(currency)
        if (fromApi.size >= 2) return fromApi

        val fromLocal = aggregateToDailyPoints(
            rateDao.getHistoryForCurrency(currency).map {
                ChartPoint(it.fetchedAt, it.mid.toFloat())
            }
        ).takeLast(14)

        return if (fromLocal.size >= 2) fromLocal else emptyList()
    }

    private suspend fun fetchDailyHistoryFromApi(currency: String): List<ChartPoint> {
        try {
            val response = apiService.getRateHistory(currency, 14)
            if (response.isSuccessful) {
                return response.body()?.history.orEmpty()
                    .mapNotNull { point ->
                        parseDayTimestamp(point.fetchedAt)?.let { dayMillis ->
                            ChartPoint(dayMillis, point.rate.toFloat())
                        }
                    }
                    .distinctBy { it.timestamp }
                    .sortedBy { it.timestamp }
                    .takeLast(14)
            }
        } catch (_: Exception) {
            // brak API — fallback do lokalnej agregacji dziennej
        }
        return emptyList()
    }

    private fun aggregateToDailyPoints(points: List<ChartPoint>): List<ChartPoint> {
        if (points.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        return points
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .toSortedMap()
            .map { (date, dayPoints) ->
                ChartPoint(toDayMillis(date, zone), dayPoints.last().ratePln)
            }
    }

    private fun parseDayTimestamp(raw: String): Long? {
        val zone = ZoneId.systemDefault()
        val localDate = try {
            Instant.parse(raw).atZone(zone).toLocalDate()
        } catch (_: Exception) {
            try {
                LocalDate.parse(raw.substring(0, 10))
            } catch (_: Exception) {
                return null
            }
        }
        return toDayMillis(localDate, zone)
    }

    private fun toDayMillis(date: LocalDate, zone: ZoneId): Long {
        return date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    }

    private suspend fun showOfflineIfNoData() {
        val localRates = rateDao.getLatestRates().firstOrNull()
        if (localRates.isNullOrEmpty()) {
            _uiState.value = DashboardUiState.Error(
                "Brak połączenia z internetem i brak lokalnych danych. Połącz się z siecią."
            )
        } else {
            latestRates = localRates
            isOffline = true
            val lastFetch = localRates.maxOfOrNull { it.fetchedAt } ?: 0L
            lastUpdated = formatTimestamp(lastFetch)
            publishSuccess()
            loadChartData()
        }
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun checkAlertsLocally(rates: List<RateEntity>) {
        viewModelScope.launch {
            try {
                val response = apiService.getAlerts()
                if (response.isSuccessful) {
                    val alerts = response.body() ?: emptyList()
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel("przevolut_main", "Powiadomienia PRZevolut", NotificationManager.IMPORTANCE_DEFAULT)
                        manager.createNotificationChannel(channel)
                    }

                    for (alert in alerts) {
                        if (!alert.isActive) continue // Alert wyłączony
                        
                        val rate = rates.find { it.currency == alert.currency } ?: continue
                        var triggered = false
                        var title = ""
                        var body = ""

                        if (alert.direction == "above" && rate.mid >= alert.targetRate) {
                            triggered = true
                            title = "Alert osiągnięty: ${alert.currency} rośnie!"
                            body = "Kurs ${alert.currency} wynosi aktualnie %.4f PLN (ustawiony próg to %.4f)".format(rate.mid, alert.targetRate)
                        } else if (alert.direction == "below" && rate.mid <= alert.targetRate) {
                            triggered = true
                            title = "Alert osiągnięty: ${alert.currency} spada!"
                            body = "Kurs ${alert.currency} wynosi aktualnie %.4f PLN (ustawiony próg to %.4f)".format(rate.mid, alert.targetRate)
                        }

                        if (triggered) {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            val pendingIntent = PendingIntent.getActivity(
                                context, alert.id, intent,
                                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                            )

                            val notification = NotificationCompat.Builder(context, "przevolut_main")
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(title)
                                .setContentText(body)
                                .setAutoCancel(true)
                                .setContentIntent(pendingIntent)
                                .build()

                            manager.notify(alert.id, notification)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignoruj błędy przy sprawdzaniu
            }
        }
    }
}
