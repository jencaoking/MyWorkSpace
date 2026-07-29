package com.jencao.mywork.ui.english

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.EnglishRoutes

/** 英语模块嵌套导航宿主：列表 <-> 编辑。 */
@Composable
fun EnglishScreen(rootNav: NavHostController) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = EnglishRoutes.LIST) {
        composable(EnglishRoutes.LIST) { EnglishListScreen(nav = nav) }
        composable(EnglishRoutes.EDIT) { EnglishEditScreen(nav = nav) }
    }
}
