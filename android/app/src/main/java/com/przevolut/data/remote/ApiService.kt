package com.przevolut.data.remote

import com.przevolut.data.remote.model.AlertRequest
import com.przevolut.data.remote.model.AlertResponse
import com.przevolut.data.remote.model.AlertUpdateRequest
import com.przevolut.data.remote.model.LoginRequest
import com.przevolut.data.remote.model.LoginResponse
import com.przevolut.data.remote.model.RateHistoryResponse
import com.przevolut.data.remote.model.RateResponse
import com.przevolut.data.remote.model.RatesResponse
import com.przevolut.data.remote.model.RegisterRequest
import com.przevolut.data.remote.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("rates")
    suspend fun getRates(): Response<RatesResponse>

    @GET("rates/history")
    suspend fun getRateHistory(
        @Query("code") currency: String,
        @Query("days") days: Int = 14
    ): Response<RateHistoryResponse>

    @GET("rates/{currency}")
    suspend fun getRate(@Path("currency") currency: String): Response<RateResponse>

    @GET("alerts")
    suspend fun getAlerts(): Response<List<AlertResponse>>

    @POST("alerts")
    suspend fun createAlert(@Body request: AlertRequest): Response<AlertResponse>

    @PATCH("alerts/{id}")
    suspend fun updateAlert(
        @Path("id") alertId: Int,
        @Body request: AlertUpdateRequest
    ): Response<AlertResponse>

    @DELETE("alerts/{id}")
    suspend fun deleteAlert(@Path("id") alertId: Int): Response<Unit>
}
