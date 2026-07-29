package com.jencao.mywork.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.EmptyHint
import com.jencao.mywork.ui.navigation.AccountRoutes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    nav: NavHostController,
    vm: AccountViewModel = hiltViewModel()
) {
    val items by vm.items.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("记账") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(AccountRoutes.edit(AccountRoutes.NEW_ID)) }) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("收入", style = MaterialTheme.typography.labelMedium)
                        Text("%.2f".format(stats.income), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("支出", style = MaterialTheme.typography.labelMedium)
                        Text("%.2f".format(stats.expense), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("结余", style = MaterialTheme.typography.labelMedium)
                        Text("%.2f".format(stats.balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (items.isEmpty()) {
                EmptyHint("还没有记账记录，点右下角记一笔")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { it ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate(AccountRoutes.edit(it.id)) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(it.category.ifBlank { "未分类" }, style = MaterialTheme.typography.titleSmall)
                                    Text(dateFmt.format(Date(it.recordDate)), style = MaterialTheme.typography.labelSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isIncome = it.type == "income"
                                    Text(
                                        (if (isIncome) "+" else "-") + "%.2f".format(it.amount) + " " + it.currency,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { vm.deleteItem(it.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
