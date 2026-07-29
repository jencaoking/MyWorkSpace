package com.jencao.mywork.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.util.RepeatRule
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TaskCalendarScreen(
    nav: NavHostController,
    vm: TaskCalendarViewModel = hiltViewModel()
) {
    val year by vm.year.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val checkinDays by vm.checkinDays.collectAsStateWithLifecycle()
    val scheduledDays by vm.scheduledDays.collectAsStateWithLifecycle()

    val ym = remember(year, month) { YearMonth.of(year, month) }
    val firstDayWeekday = remember(ym) { (ym.atDay(1).dayOfWeek.value % 7) } // 周日=0
    val daysInMonth = remember(ym) { ym.lengthOfMonth() }
    val cells = remember(firstDayWeekday, daysInMonth) {
        List(firstDayWeekday) { "" } + (1..daysInMonth).map { it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${year}年${month}月") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.prevMonth() }) { Icon(Icons.Filled.ChevronLeft, "上月") }
                    IconButton(onClick = { vm.nextMonth() }) { Icon(Icons.Filled.ChevronRight, "下月") }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendDot(MaterialTheme.colorScheme.primary, "已打卡")
                LegendDot(MaterialTheme.colorScheme.tertiary, "计划日")
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cells) { day ->
                    if (day.isEmpty()) {
                        Box(Modifier.aspectRatio(1f))
                    } else {
                        val dateStr = RepeatRule.dateStr(LocalDate.of(year, month, day.toInt()))
                        val checked = checkinDays.contains(dateStr)
                        val scheduled = scheduledDays.contains(dateStr)
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        checked -> MaterialTheme.colorScheme.primary
                                        scheduled -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day,
                                color = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
