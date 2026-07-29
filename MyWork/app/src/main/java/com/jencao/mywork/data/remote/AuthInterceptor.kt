package com.jencao.mywork.data.remote

import com.jencao.mywork.data.settings.UserPreferencesRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 请求拦截器：注入设备标识头 X-Device-ID（方案 V1.1 的无登录设备级鉴权）。
 * 后端以设备 ID 做数据隔离，不强制共享令牌，保持本地/自用部署的开箱即用。
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
