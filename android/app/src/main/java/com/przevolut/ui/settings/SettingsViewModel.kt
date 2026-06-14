package com.przevolut.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.TokenManager
import com.przevolut.data.remote.ApiService
import com.przevolut.data.remote.model.PasswordChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettings(
    val userEmail: String? = null,
    val isLoggedIn: Boolean = false,
    val defaultCurrency: String = "EUR",
    val biometricEnabled: Boolean = false,
    val themeMode: String = "system",
    val refreshIntervalMinutes: Int = 60,
    val fontScale: Float = 1.0f,
)

sealed class SettingsEvent {
    data class Message(val text: String) : SettingsEvent()
    object LoggedOut : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val apiService: ApiService
) : ViewModel() {

    private val prefs = context.getSharedPreferences("przevolut_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events

    fun refreshProfile() {
        val loggedIn = tokenManager.isLoggedIn()
        if (!loggedIn) {
            _settings.value = _settings.value.copy(
                isLoggedIn = false,
                userEmail = null,
                biometricEnabled = prefs.getBoolean("biometric_enabled", false)
            )
            return
        }

        viewModelScope.launch {
            var email = tokenManager.getUserEmail()
            try {
                val response = apiService.getMe()
                if (response.isSuccessful) {
                    email = response.body()?.email ?: email
                    email?.let { tokenManager.saveUserEmail(it) }
                }
            } catch (_: Exception) {
                // pokaż zapisany e-mail offline
            }
            _settings.value = _settings.value.copy(
                isLoggedIn = true,
                userEmail = email
            )
        }
    }

    private fun loadSettings(): AppSettings {
        val loggedIn = tokenManager.isLoggedIn()
        return AppSettings(
            userEmail = if (loggedIn) tokenManager.getUserEmail() else null,
            isLoggedIn = loggedIn,
            defaultCurrency = prefs.getString("default_currency", "EUR") ?: "EUR",
            biometricEnabled = loggedIn && prefs.getBoolean("biometric_enabled", false),
            themeMode = prefs.getString("theme_mode", "system") ?: "system",
            refreshIntervalMinutes = prefs.getInt("refresh_interval_minutes", 60),
            fontScale = prefs.getFloat("a11y_font_scale", 1.0f),
        )
    }

    fun setDefaultCurrency(currency: String) {
        prefs.edit().putString("default_currency", currency).apply()
        _settings.value = _settings.value.copy(defaultCurrency = currency)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        if (!tokenManager.isLoggedIn()) {
            _events.tryEmit(SettingsEvent.Message("Włącz biometrię po zalogowaniu hasłem."))
            _settings.value = _settings.value.copy(biometricEnabled = false)
            return
        }
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _settings.value = _settings.value.copy(biometricEnabled = enabled)
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setRefreshIntervalMinutes(minutes: Int) {
        prefs.edit().putInt("refresh_interval_minutes", minutes).apply()
        _settings.value = _settings.value.copy(refreshIntervalMinutes = minutes)
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat("a11y_font_scale", scale).apply()
        _settings.value = _settings.value.copy(fontScale = scale)
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (!tokenManager.isLoggedIn()) return
        viewModelScope.launch {
            try {
                val response = apiService.changePassword(
                    PasswordChangeRequest(currentPassword, newPassword)
                )
                if (response.isSuccessful) {
                    _events.emit(SettingsEvent.Message("Hasło zostało zmienione."))
                } else {
                    _events.emit(SettingsEvent.Message("Nie udało się zmienić hasła."))
                }
            } catch (_: Exception) {
                _events.emit(SettingsEvent.Message("Brak połączenia z serwerem."))
            }
        }
    }

    fun logout() {
        tokenManager.softLogout()
        _settings.value = loadSettings()
        viewModelScope.launch {
            _events.emit(SettingsEvent.LoggedOut)
        }
    }
}
