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

    /** App 接口共享令牌头（与后端 SELFWORK_API_TOKEN 对应；后端优先读 Authorization，其次 X-Api-Token）。 */
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_API_TOKEN = "X-Api-Token"
    /** Authorization 头的值前缀，采用标准 Bearer 方案。 */
    const val AUTH_SCHEME = "Bearer"
}
