package com.jencao.mywork.ui.notes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jencao.mywork.ui.navigation.NoteRoutes

/** 笔记模块宿主：嵌套导航（列表 / 编辑 / 搜索），底部导航常驻。 */
@Composable
fun NotesScreen(padding: PaddingValues) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = NoteRoutes.LIST,
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        composable(NoteRoutes.LIST) { NoteListScreen(nav) }
        composable(NoteRoutes.EDIT) { NoteEditScreen(nav) }
        composable(NoteRoutes.SEARCH) { NoteSearchScreen(nav) }
    }
}
