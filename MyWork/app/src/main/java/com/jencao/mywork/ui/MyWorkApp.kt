package com.jencao.mywork.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.data.settings.ThemeMode
import com.jencao.mywork.ui.home.HomeScreen
import com.jencao.mywork.ui.english.EnglishScreen
import com.jencao.mywork.ui.health.HealthScreen
import com.jencao.mywork.ui.media.MediaScreen
import com.jencao.mywork.ui.navigation.Routes
import com.jencao.mywork.ui.navigation.bottomNavItems
import com.jencao.mywork.ui.notes.NotesScreen
import com.jencao.mywork.ui.settings.SettingsScreen
import com.jencao.mywork.ui.sport.SportScreen
import com.jencao.mywork.ui.task.TaskScreen
import com.jencao.mywork.ui.theme.MyWorkTheme

@Composable
fun MyWorkApp(appVm: AppViewModel) {
    val themeMode by appVm.themeMode.collectAsStateWithLifecycle()
    val toggles by appVm.moduleToggles.collectAsStateWithLifecycle()

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MyWorkTheme(darkTheme = darkTheme) {
        val navController: NavHostController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val items = bottomNavItems.filter { it.module == null || (toggles[it.module] ?: it.module.locked) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.HOME) { HomeScreen(appVm, padding, navController) }
                composable(Routes.TASKS) { TaskScreen(padding) }
                composable(Routes.NOTES) { NotesScreen(padding) }
                composable(Routes.SETTINGS) { SettingsScreen(appVm, padding) }
                // 阶段4 专项模块（由首页卡片进入）
                composable(Routes.SPORT) { SportScreen(navController) }
                composable(Routes.ENGLISH) { EnglishScreen(navController) }
                composable(Routes.MEDIA) { MediaScreen(navController) }
                composable(Routes.HEALTH) { HealthScreen(navController) }
            }
        }
    }
}
