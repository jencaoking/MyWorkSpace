package com.jencao.mywork.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.NeuIconButton
import com.jencao.mywork.ui.navigation.ModuleTile
import com.jencao.mywork.ui.navigation.moduleMeta
import com.jencao.mywork.ui.navigation.moduleRoute

@Composable
fun ToolsScreen(rootNav: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeuIconButton(onClick = { rootNav.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, "返回")
            }
            Spacer(Modifier.width(12.dp))
            Text("全部工具", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            "所有功能板块，点击直接进入",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val modules = moduleMeta.keys.toList()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            modules.chunked(2).forEach { rowKeys ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowKeys.forEach { key ->
                        ModuleTile(
                            key,
                            onClick = { rootNav.navigate(moduleRoute(key)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowKeys.size == 1) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
