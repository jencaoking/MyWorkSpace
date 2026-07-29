package com.jencao.mywork

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jencao.mywork.data.settings.UserPreferencesRepository
import com.jencao.mywork.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyWorkApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var prefs: UserPreferencesRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 注册后台周期同步（幂等），并据开关在启动时触发一次即时同步
        SyncScheduler.schedule(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            if (prefs.autoSyncEnabled.first()) {
                SyncScheduler.syncNow(this@MyWorkApplication)
            }
        }
    }
}
