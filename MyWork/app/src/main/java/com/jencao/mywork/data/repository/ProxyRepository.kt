package com.jencao.mywork.data.repository

import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.TranslateData
import com.jencao.mywork.data.remote.model.WordLookupData

/**
 * 第三方 API 代理仓储：App 不持有任何密钥，所有翻译 / 词典请求都经过
 * 服务端代理（/api/proxy 路径），密钥统一在后台管理中填写。
 * 由调用方（ViewModel）以 ApiService 构造，不纳入 Hilt 绑定图。
 */
class ProxyRepository(private val api: ApiService) {

    suspend fun translate(
        text: String,
        from: String = "auto",
        to: String = "zh-CHS"
    ): Result<TranslateData> = runCatching {
        val resp = api.translate(text, from, to)
        if (resp.code == 0 && resp.data != null) resp.data
        else throw Exception(resp.message.ifBlank { "翻译失败" })
    }

    suspend fun lookupWord(text: String): Result<WordLookupData> = runCatching {
        val resp = api.lookupWord(text)
        if (resp.code == 0 && resp.data != null) resp.data
        else throw Exception(resp.message.ifBlank { "查询失败" })
    }
}
