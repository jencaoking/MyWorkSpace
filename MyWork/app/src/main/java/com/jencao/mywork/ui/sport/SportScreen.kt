package com.jencao.mywork.ui.sport

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
fun SportScreen(rootNav: NavHostController, padding: PaddingValues) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = SportRoutes.LIST,
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        composable(SportRoutes.LIST) {
            SportListScreen(nav = nav)
        }
        composable(SportRoutes.EDIT) {
            SportEditScreen(nav = nav)
        }
    }
}
