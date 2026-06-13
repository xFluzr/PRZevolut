package com.przevolut.data.remote

import com.przevolut.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    private val noAuthPaths = setOf("auth/login", "auth/register")

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
        if (response.code == 401 && token != null) {
            tokenManager.clearToken()
        }
        return response
    }
}
