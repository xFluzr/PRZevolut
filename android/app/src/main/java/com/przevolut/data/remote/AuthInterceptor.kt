package com.przevolut.data.remote

import com.przevolut.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    private val noAuthPaths = setOf("auth/login", "auth/register", "auth/refresh")

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath.trimStart('/')

        if (noAuthPaths.any { path.startsWith(it) }) {
            return chain.proceed(original)
        }

        val token = tokenManager.getToken()
        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // If 401 and we have a refresh token, try to refresh automatically
        if (response.code == 401 && token != null) {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                response.close()

                val newAccessToken = tryRefreshToken(chain, refreshToken)
                if (newAccessToken != null) {
                    tokenManager.saveToken(newAccessToken)
                    // Retry the original request with the new token
                    val retryRequest = original.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                    return chain.proceed(retryRequest)
                }
            }
            // Refresh failed — clear everything
            tokenManager.clearToken()
        }

        return response
    }

    /**
     * Synchronously call /auth/refresh to get a new access_token.
     * Returns the new token on success, null on failure.
     */
    private fun tryRefreshToken(chain: Interceptor.Chain, refreshToken: String): String? {
        return try {
            val json = JSONObject().put("refresh_token", refreshToken).toString()
            val body = json.toRequestBody("application/json".toMediaType())

            val baseUrl = chain.request().url.newBuilder()
                .encodedPath("/auth/refresh")
                .query(null)
                .build()

            val refreshRequest = Request.Builder()
                .url(baseUrl)
                .post(body)
                .build()

            val refreshResponse = chain.proceed(refreshRequest)
            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                refreshResponse.close()
                responseBody?.let {
                    JSONObject(it).optString("access_token", null)
                }
            } else {
                refreshResponse.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
