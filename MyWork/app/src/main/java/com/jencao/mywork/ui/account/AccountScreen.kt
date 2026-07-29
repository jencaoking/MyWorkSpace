package com.jencao.mywork.ui.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.AccountRoutes

@Composable
fun AccountScreen(rootNav: NavHostController) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = AccountRoutes.LIST) {
        composable(AccountRoutes.LIST) { AccountListScreen(nav = nav) }
        composable(AccountRoutes.EDIT) { AccountEditScreen(nav = nav) }
    }
}
