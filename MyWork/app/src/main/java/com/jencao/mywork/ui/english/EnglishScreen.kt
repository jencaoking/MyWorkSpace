package com.jencao.mywork.ui.english

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.EnglishRoutes

/** 英语模块嵌套导航宿主：列表 <-> 编辑。 */
@Composable
fun EnglishScreen(rootNav: NavHostController, padding: PaddingValues) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = EnglishRoutes.LIST,
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        composable(EnglishRoutes.LIST) { EnglishListScreen(nav = nav) }
        composable(EnglishRoutes.EDIT) { EnglishEditScreen(nav = nav) }
    }
}
