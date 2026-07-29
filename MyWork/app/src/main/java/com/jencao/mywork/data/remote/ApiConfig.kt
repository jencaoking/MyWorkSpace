package com.jencao.mywork.data.remote

import com.jencao.mywork.BuildConfig

/**
 * 网络层全局配置。BASE_URL 来自 app/build.gradle.kts 的 buildConfigField，
 * Release 时替换为自己的域名（必须 HTTPS，否则 OkHttp 默认拒绝明文）。
 */
object ApiConfig {
    val BASE_URL: String get() = BuildConfig.API_BASE_URL
    val DEBUG: Boolean get() = BuildConfig.DEBUG_NETWORK

    const val HEADER_DEVICE_ID = "X-Device-ID"
    const val HEADER_ACCEPT = "Accept"
}
