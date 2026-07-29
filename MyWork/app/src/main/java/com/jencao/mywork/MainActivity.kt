package com.jencao.mywork

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jencao.mywork.ui.AppViewModel
import com.jencao.mywork.ui.MyWorkApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // 复诊 / 用药提醒通知点击后带入的记录 id，用于直达编辑页（覆盖 onCreate 与 onNewIntent 两种启动方式）
    private var deepLinkHealthId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkHealthId = intent.getStringExtra("open_health_id")
        intent.removeExtra("open_health_id")
        setContent {
            val appVm: AppViewModel = hiltViewModel()
            MyWorkApp(appVm, deepLinkHealthId = deepLinkHealthId) { deepLinkHealthId = null }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkHealthId = intent.getStringExtra("open_health_id")
        intent.removeExtra("open_health_id")
    }
}
