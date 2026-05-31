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

// ── Rates ─────────────────────────────────────────────────────────────────

data class RatesResponse(
    @SerializedName("fetched_at") val fetchedAt: String,
    val base: String,
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

// ── Alerts ────────────────────────────────────────────────────────────────

data class AlertRequest(
    val currency: String,
    val direction: String,  // "below" | "above"
    @SerializedName("target_rate") val targetRate: Double
)

data class AlertResponse(
    val id: Int,
    val currency: String,
    val direction: String,
    @SerializedName("target_rate") val targetRate: Double,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_triggered") val isTriggered: Boolean,
    @SerializedName("triggered_at") val triggeredAt: String?,
    @SerializedName("created_at") val createdAt: String
)
