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


