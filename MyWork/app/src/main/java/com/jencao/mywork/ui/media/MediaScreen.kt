package com.jencao.mywork.ui.media

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.MediaRoutes

/** 影音模块嵌套导航宿主：列表 <-> 编辑。 */
@Composable
fun MediaScreen(rootNav: NavHostController, padding: PaddingValues) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = MediaRoutes.LIST,
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        composable(MediaRoutes.LIST) { MediaListScreen(nav = nav) }
        composable(MediaRoutes.EDIT) { MediaEditScreen(nav = nav) }
    }
}
