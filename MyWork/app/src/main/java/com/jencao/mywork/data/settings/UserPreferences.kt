package com.jencao.mywork.data.settings

/**
 * 主题模式：跟随系统 / 浅色 / 深色。
 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromOrdinal(value: Int) = entries.getOrElse(value) { SYSTEM }
    }
}

/**
 * 可开关的功能板块（对应首页卡片与底部导航是否展示）。
 */
enum class ModuleKey {
    TASK,       // 任务（P0，始终开启、不可关闭）
    NOTE,       // 笔记（P0，始终开启、不可关闭）
    SPORT,      // 运动
    ENGLISH,    // 英语
    MEDIA,      // 影音书籍
    HEALTH,     // 健康
    ACCOUNT,    // 记账
    WEATHER;    // 天气

    val displayName: String
        get() = when (this) {
            TASK -> "任务"
            NOTE -> "笔记"
            SPORT -> "运动"
            ENGLISH -> "英语"
            MEDIA -> "影音书籍"
            HEALTH -> "健康"
            ACCOUNT -> "记账"
            WEATHER -> "天气"
        }

    /** P0 板块固定开启、不允许用户关闭 */
    val locked: Boolean
        get() = this == TASK || this == NOTE
}
