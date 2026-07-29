package com.jencao.mywork.ui.media

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.MediaRoutes

/** 影音模块嵌套导航宿主：列表 <-> 编辑。 */
@Composable
fun MediaScreen(rootNav: NavHostController) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = MediaRoutes.LIST) {
        composable(MediaRoutes.LIST) { MediaListScreen(nav = nav) }
        composable(MediaRoutes.EDIT) { MediaEditScreen(nav = nav) }
    }
}
