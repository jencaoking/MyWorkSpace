package com.jencao.mywork.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.data.settings.ThemeMode
import com.jencao.mywork.ui.account.AccountScreen
import com.jencao.mywork.ui.components.NeuFab
import com.jencao.mywork.ui.theme.NeuRadiusLarge
import com.jencao.mywork.ui.english.EnglishScreen
import com.jencao.mywork.ui.english.EnglishReviewScreen
import com.jencao.mywork.ui.health.HealthScreen
import com.jencao.mywork.ui.home.HomeScreen
import com.jencao.mywork.ui.media.MediaScreen
import com.jencao.mywork.ui.navigation.BottomNavItem
import com.jencao.mywork.ui.navigation.Routes
import com.jencao.mywork.ui.navigation.bottomNavItems
import com.jencao.mywork.ui.notes.NotesScreen
import com.jencao.mywork.ui.pomodoro.PomodoroScreen
import com.jencao.mywork.ui.settings.SettingsScreen
import com.jencao.mywork.ui.sport.SportScreen
import com.jencao.mywork.ui.search.GlobalSearchScreen
import com.jencao.mywork.ui.task.TaskScreen
import com.jencao.mywork.ui.theme.MyWorkTheme
import com.jencao.mywork.ui.theme.neumorphic
import com.jencao.mywork.ui.tools.ToolsScreen

@Composable
fun MyWorkApp(appVm: AppViewModel, deepLinkHealthId: String? = null, onDeepLinkConsumed: () -> Unit = {}) {
    val themeMode by appVm.themeMode.collectAsStateWithLifecycle()

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

        MyWorkTheme(darkTheme = darkTheme) {
            val navController: NavHostController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            var pendingHealthId by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(deepLinkHealthId) {
                if (!deepLinkHealthId.isNullOrBlank()) pendingHealthId = deepLinkHealthId
            }

        Scaffold(
            bottomBar = { NeumorphNavBar(currentRoute, navController) }
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
                composable(Routes.TOOLS) { ToolsScreen(navController, padding) }
                // 阶段4 专项模块
                composable(Routes.SPORT) { SportScreen(navController, padding) }
                composable(Routes.ENGLISH) { EnglishScreen(navController, padding) }
                composable(Routes.MEDIA) { MediaScreen(navController, padding) }
                composable(Routes.HEALTH) { HealthScreen(navController, padding, pendingHealthId) {
                    pendingHealthId = null
                    onDeepLinkConsumed()
                } }
                // 阶段5
                composable(Routes.ACCOUNT) { AccountScreen(navController, padding) }
                composable(Routes.POMODORO) { PomodoroScreen(navController, padding) }
                composable(Routes.ENGLISH_REVIEW) { EnglishReviewScreen(navController, padding) }
                composable(Routes.GLOBAL_SEARCH) { GlobalSearchScreen(navController, padding) }
            }
        }
    }
}

/** 新拟物底部导航：四项常驻入口 + 中心悬浮专注按钮，确保入口稳定可发现。 */
@Composable
fun NeumorphNavBar(currentRoute: String?, navController: NavHostController) {
    val bg = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .neumorphic(NeuRadiusLarge, 8.dp, backgroundColor = bg)
            .padding(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.take(2).forEach { item ->
                NavBarItem(item, currentRoute, navController, Modifier.weight(1f))
            }
            NeuFab(onClick = {
                navController.navigate(Routes.POMODORO) { launchSingleTop = true }
            }) {
                Icon(Icons.Filled.Timer, "专注", tint = MaterialTheme.colorScheme.primary)
            }
            bottomNavItems.takeLast(2).forEach { item ->
                NavBarItem(item, currentRoute, navController, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.NavBarItem(
    item: BottomNavItem,
    currentRoute: String?,
    navController: NavHostController,
    modifier: Modifier
) {
    val selected = currentRoute == item.route
    val color =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable {
                navController.navigate(item.route) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            .padding(8.dp)
    ) {
        Icon(item.icon, contentDescription = item.label, tint = color)
        Spacer(Modifier.height(4.dp))
        Text(item.label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
