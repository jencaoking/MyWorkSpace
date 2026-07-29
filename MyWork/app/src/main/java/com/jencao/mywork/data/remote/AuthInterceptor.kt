package com.jencao.mywork.data.remote

import com.jencao.mywork.data.settings.UserPreferencesRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 请求拦截器：注入设备标识头 X-Device-ID（方案 V1.1 的设备级鉴权），
 * 以及 App 接口共享令牌头（Authorization: Bearer <token>，与后端 SELFWORK_API_TOKEN 对应）。
 * 令牌来自 DataStore，仅在非空时注入——未配置令牌时留空，兼容后端未启用鉴权的开发模式。
 */
class AuthInterceptor @Inject constructor(
    private val prefs: UserPreferencesRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = runBlocking { prefs.ensureDeviceId() }
        val token = runBlocking { prefs.getApiToken() }
        val requestBuilder = chain.request().newBuilder()
            .addHeader(ApiConfig.HEADER_DEVICE_ID, deviceId)
            .addHeader(ApiConfig.HEADER_ACCEPT, "application/json")
        if (token.isNotBlank()) {
            requestBuilder.addHeader(
                ApiConfig.HEADER_AUTHORIZATION,
                "${ApiConfig.AUTH_SCHEME} $token"
            )
        }
        return chain.proceed(requestBuilder.build())
    }
}
