package com.jencao.mywork.data.util

import com.jencao.mywork.data.local.BaseEntity

/**
 * 标记本地变更待同步：刷新 lastModified 并置 needsSync。
 * 所有扩展模块仓库（运动/英语/影音/健康）共用。
 */
fun BaseEntity.touch() {
    lastModified = System.currentTimeMillis()
    needsSync = true
}
