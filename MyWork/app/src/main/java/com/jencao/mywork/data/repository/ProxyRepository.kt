package com.jencao.mywork.data.repository

import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.TranslateData
import com.jencao.mywork.data.remote.model.WordLookupData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 第三方 API 代理仓储：App 不持有任何密钥，所有翻译 / 词典请求都经过
 * 服务端代理（/api/proxy/*），密钥统一在后台管理中填写。
 */
@Singleton
class ProxyRepository @Inject constructor(private val api: ApiService) {

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
