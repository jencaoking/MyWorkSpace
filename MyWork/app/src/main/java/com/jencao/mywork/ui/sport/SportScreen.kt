package com.jencao.mywork.ui.sport

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.SportRoutes

/** 运动模块嵌套导航宿主：列表 <-> 编辑。 */
@Composable
fun SportScreen(rootNav: NavHostController) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = SportRoutes.LIST) {
        composable(SportRoutes.LIST) {
            SportListScreen(nav = nav)
        }
        composable(SportRoutes.EDIT) {
            SportEditScreen(nav = nav)
        }
    }
}
