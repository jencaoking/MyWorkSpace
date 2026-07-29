package com.jencao.mywork.data.remote

import com.jencao.mywork.data.settings.UserPreferencesRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 请求拦截器：注入设备标识头 X-Device-ID（方案 V1.1 的设备级鉴权）。
 * 设备标识首次访问时由 DataStore 自动生成。
 */
class AuthInterceptor @Inject constructor(
    private val prefs: UserPreferencesRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = runBlocking { prefs.ensureDeviceId() }
        val request = chain.request().newBuilder()
            .addHeader(ApiConfig.HEADER_DEVICE_ID, deviceId)
            .addHeader(ApiConfig.HEADER_ACCEPT, "application/json")
            .build()
        return chain.proceed(request)
    }
}
