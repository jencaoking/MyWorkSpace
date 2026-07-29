package com.jencao.mywork.ui.health

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.HealthRoutes

/** 健康模块嵌套导航宿主：列表 <-> 编辑。 */
@Composable
fun HealthScreen(rootNav: NavHostController, padding: PaddingValues) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = HealthRoutes.LIST,
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        composable(HealthRoutes.LIST) { HealthListScreen(nav = nav) }
        composable(HealthRoutes.EDIT) { HealthEditScreen(nav = nav) }
    }
}
