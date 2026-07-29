package com.jencao.mywork.ui.task

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.PaddingValues
import com.jencao.mywork.ui.navigation.TaskRoutes

@Composable
fun TaskScreen(padding: PaddingValues) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = TaskRoutes.LIST,
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        composable(TaskRoutes.LIST) { TaskListScreen(nav) }
        composable(TaskRoutes.DETAIL) { backStack ->
            val id = backStack.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(nav, id)
        }
        composable(TaskRoutes.CALENDAR) { TaskCalendarScreen(nav) }
        composable(TaskRoutes.STATS) { TaskStatsScreen(nav) }
        composable(TaskRoutes.CATEGORIES) { CategoryManageScreen(nav) }
    }
}
