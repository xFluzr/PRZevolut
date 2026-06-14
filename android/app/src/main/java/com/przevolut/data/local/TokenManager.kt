package com.przevolut.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit { putString(KEY_TOKEN, token) }

    fun saveUserEmail(email: String) = prefs.edit { putString(KEY_EMAIL, email) }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun clearToken() = prefs.edit {
        remove(KEY_TOKEN)
        remove(KEY_EMAIL)
    }

    fun isLoggedIn(): Boolean = getToken() != null

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_EMAIL = "user_email"
    }
}
