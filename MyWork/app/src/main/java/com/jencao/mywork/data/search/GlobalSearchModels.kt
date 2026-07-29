package com.jencao.mywork.data.search

import com.jencao.mywork.data.settings.ModuleKey

/**
 * 跨模块全局搜索覆盖的功能板块。
 *
 * @param label 板块中文名，用于搜索结果分组标题
 * @param moduleKey 对应的 [ModuleKey]，结果点击时据此定位目标路由
 */
enum class SearchModule(
    val label: String,
    val moduleKey: ModuleKey
) {
    TASK("任务", ModuleKey.TASK),
    NOTE("笔记", ModuleKey.NOTE),
    MOVIE_BOOK("影音书籍", ModuleKey.MEDIA),
    SPORT("运动", ModuleKey.SPORT),
    ENGLISH("英语", ModuleKey.ENGLISH),
    HEALTH("健康", ModuleKey.HEALTH),
    ACCOUNT("记账", ModuleKey.ACCOUNT),
    POMODORO("番茄钟", ModuleKey.POMODORO)
}

/** 一条全局搜索结果。 */
data class GlobalSearchResult(
    val id: String,
    val module: SearchModule,
    val title: String,
    val snippet: String
)
