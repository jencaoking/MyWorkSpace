package com.jencao.mywork.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.jencao.mywork.data.settings.ModuleKey

object Routes {
    const val HOME = "home"
    const val TASKS = "tasks"
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    // 阶段4 专项模块（首页卡片进入，不占底部导航）
    const val SPORT = "sport"
    const val ENGLISH = "english"
    const val MEDIA = "media"
    const val HEALTH = "health"
}

/** 任务模块内部嵌套路由（列表 / 详情 / 日历 / 统计 / 分类管理）。 */
object TaskRoutes {
    const val LIST = "task_list"
    const val DETAIL = "task_detail/{taskId}"
    const val CALENDAR = "task_calendar"
    const val STATS = "task_stats"
    const val CATEGORIES = "task_categories"

    fun detail(taskId: String) = "task_detail/$taskId"
}

/** 笔记模块内部嵌套路由（列表 / 编辑 / 搜索）。 */
object NoteRoutes {
    const val LIST = "note_list"
    const val EDIT = "note_edit/{noteId}"
    const val SEARCH = "note_search"

    /** noteId 传 "new" 表示新建 */
    const val NEW_ID = "new"

    fun edit(noteId: String) = "note_edit/$noteId"
}

/** 运动模块内部嵌套路由（列表 / 编辑）。 */
object SportRoutes {
    const val LIST = "sport_list"
    const val EDIT = "sport_edit/{sportId}"
    const val NEW_ID = "new"
    fun edit(sportId: String) = "sport_edit/$sportId"
}

/** 英语模块内部嵌套路由（列表 / 编辑）。 */
object EnglishRoutes {
    const val LIST = "english_list"
    const val EDIT = "english_edit/{wordId}"
    const val NEW_ID = "new"
    fun edit(wordId: String) = "english_edit/$wordId"
}

/** 影音书籍模块内部嵌套路由（列表 / 编辑）。 */
object MediaRoutes {
    const val LIST = "media_list"
    const val EDIT = "media_edit/{mediaId}"
    const val NEW_ID = "new"
    fun edit(mediaId: String) = "media_edit/$mediaId"
}

/** 健康模块内部嵌套路由（列表 / 编辑）。 */
object HealthRoutes {
    const val LIST = "health_list"
    const val EDIT = "health_edit/{healthId}"
    const val NEW_ID = "new"
    fun edit(healthId: String) = "health_edit/$healthId"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    /** 对应功能板块；为 null 表示常驻（首页/设置） */
    val module: ModuleKey? = null
)

/** 底部导航项。P0 板块（任务/笔记）恒显示；其余随板块开关出现。 */
val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "首页", Icons.Filled.Home),
    BottomNavItem(Routes.TASKS, "任务", Icons.Filled.CheckCircle, ModuleKey.TASK),
    BottomNavItem(Routes.NOTES, "笔记", Icons.Filled.Book, ModuleKey.NOTE),
    BottomNavItem(Routes.SETTINGS, "设置", Icons.Filled.Settings)
)
