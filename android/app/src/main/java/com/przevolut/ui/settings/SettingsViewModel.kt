package com.przevolut.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettings(
    val defaultCurrency: String = "EUR",
    val biometricEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager
) : ViewModel() {

    // W produkcji użyj DataStore lub EncryptedSharedPreferences
    private val prefs = context.getSharedPreferences("przevolut_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    private fun loadSettings(): AppSettings {
        return AppSettings(
            defaultCurrency = prefs.getString("default_currency", "EUR") ?: "EUR",
            biometricEnabled = prefs.getBoolean("biometric_enabled", false),
        )
    }

    fun setDefaultCurrency(currency: String) {
        prefs.edit().putString("default_currency", currency).apply()
        _settings.value = _settings.value.copy(defaultCurrency = currency)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _settings.value = _settings.value.copy(biometricEnabled = enabled)
    }

    fun logout() {
        viewModelScope.launch {
            // Poprawiony logout — czyści token przez TokenManager (auth_prefs / access_token)
            tokenManager.clearToken()
            // TODO: Nawiguj do LoginFragment przez NavController
        }
    }
}

