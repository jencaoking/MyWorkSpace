package com.jencao.mywork.data.settings

/**
 * 功能板块标识。工具箱 8 个独立模块（CALC/CONVERTER/QRCODE/COUNTDOWN/FLASHCARD/HABIT/INSPIRATION/EXPRESS）
 * 与既有板块共用同一套「首页磁贴 + 开关 + 同步」体系；sortOrder 用于首页/工具箱排序。
 */
enum class ModuleKey(val sortOrder: Int) {
    TASK(0), NOTE(1), SPORT(2), ENGLISH(3), MEDIA(4), HEALTH(5), ACCOUNT(6), WEATHER(7), POMODORO(8),
    CALC(20), CONVERTER(21), QRCODE(22), COUNTDOWN(23), FLASHCARD(24), HABIT(25), INSPIRATION(26), EXPRESS(27);

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
            POMODORO -> "番茄钟"
            CALC -> "计算器"
            CONVERTER -> "单位换算"
            QRCODE -> "扫码"
            COUNTDOWN -> "倒计时"
            FLASHCARD -> "闪卡"
            HABIT -> "习惯养成"
            INSPIRATION -> "灵感语录"
            EXPRESS -> "快递查询"
        }

    /** 锁定板块恒为开启且不可关闭（P0 核心） */
    val locked: Boolean
        get() = this == TASK || this == NOTE
}
