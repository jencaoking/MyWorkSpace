package com.jencao.mywork.ui.health

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.HealthRoutes

/** 健康模块嵌套导航宿主：列表 <-> 编辑。 */
@Composable
fun HealthScreen(rootNav: NavHostController) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HealthRoutes.LIST) {
        composable(HealthRoutes.LIST) { HealthListScreen(nav = nav) }
        composable(HealthRoutes.EDIT) { HealthEditScreen(nav = nav) }
    }
}
