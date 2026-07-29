package com.jencao.mywork.ui.weather

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jencao.mywork.data.remote.model.QweatherCity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerSheet(
    state: WeatherUiState,
    searchResults: List<QweatherCity>,
    searching: Boolean,
    onDismiss: () -> Unit,
    onToggleAuto: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onSelect: (QweatherCity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var keyword by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
        ) {
            Text("天气设置", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("自动定位", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = state.auto, onCheckedChange = onToggleAuto)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = keyword,
                onValueChange = {
                    keyword = it
                    onSearch(it)
                },
                label = { Text("搜索城市") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            if (searching) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                items(searchResults) { city ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(city) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(city.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${city.adm1} ${city.adm2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Divider()
                }
            }
        }
    }
}
