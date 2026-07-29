package com.jencao.mywork.data.remote.model

/**
 * 笔记图片上传响应：{ code, message, data{ url, path, name, size } }
 * 与后端 NoteImageController::upload 的返回结构保持一致。
 */
data class NoteImageUploadResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: NoteImageData? = null
)

data class NoteImageData(
    val url: String,
    val path: String,
    val name: String? = null,
    val size: Long? = null
)
