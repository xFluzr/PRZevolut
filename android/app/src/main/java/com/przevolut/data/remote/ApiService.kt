package com.przevolut.data.remote

import com.przevolut.data.remote.model.AlertRequest
import com.przevolut.data.remote.model.AlertResponse
import com.przevolut.data.remote.model.LoginRequest
import com.przevolut.data.remote.model.LoginResponse
import com.przevolut.data.remote.model.RateResponse
import com.przevolut.data.remote.model.RegisterRequest
import com.przevolut.data.remote.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface — mapowanie endpointów backendu PRZevolut API.
 */
interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ── Rates ─────────────────────────────────────────────────────────────

    @GET("rates")
    suspend fun getRates(): Response<List<RateResponse>>

    @GET("rates/{currency}")
    suspend fun getRate(@Path("currency") currency: String): Response<RateResponse>

    // ── Alerts ────────────────────────────────────────────────────────────

    @GET("alerts")
    suspend fun getAlerts(@Header("Authorization") token: String): Response<List<AlertResponse>>

    @POST("alerts")
    suspend fun createAlert(
        @Header("Authorization") token: String,
        @Body request: AlertRequest
    ): Response<AlertResponse>

    @DELETE("alerts/{id}")
    suspend fun deleteAlert(
        @Header("Authorization") token: String,
        @Path("id") alertId: Int
    ): Response<Unit>
}
