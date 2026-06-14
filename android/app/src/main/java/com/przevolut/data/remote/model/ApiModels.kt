package com.przevolut.data.remote.model

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────

data class RegisterRequest(
    val email: String,
    val password: String
)

data class RegisterResponse(
    val id: Int,
    val email: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class UserResponse(
    val id: Int,
    val email: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class PasswordChangeRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

// ── Rates ─────────────────────────────────────────────────────────────────

data class RatesResponse(
    @SerializedName("fetched_at") val fetchedAt: String,
    val base: String? = "PLN",
    val rates: List<RateResponse>
)

data class RateResponse(
    @SerializedName("code") val currency: String,
    val name: String?,
    val rate: Double?,
    val mid: Double?,
    val bid: Double?,
    val ask: Double?,
    @SerializedName("effective_date") val effectiveDate: String?
)

data class RateHistoryResponse(
    val code: String,
    val days: Int,
    val history: List<RateHistoryPoint>
)

data class RateHistoryPoint(
    val rate: Double,
    @SerializedName("fetched_at") val fetchedAt: String
)

// ── Alerts (zgodne z backendem: currency_code, threshold) ───────────────

data class AlertRequest(
    @SerializedName("currency_code") val currency: String,
    val direction: String,
    val threshold: Double
)

data class AlertUpdateRequest(
    @SerializedName("currency_code") val currency: String? = null,
    val direction: String? = null,
    val threshold: Double? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

data class AlertResponse(
    val id: Int,
    @SerializedName("currency_code") val currency: String,
    val direction: String,
    val threshold: Double,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("last_triggered_at") val triggeredAt: String?,
    @SerializedName("created_at") val createdAt: String
) {
    val isTriggered: Boolean get() = triggeredAt != null
    val targetRate: Double get() = threshold
}
