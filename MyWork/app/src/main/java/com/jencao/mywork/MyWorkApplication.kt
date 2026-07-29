package com.jencao.mywork

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jencao.mywork.data.repository.HealthRecordRepository
import com.jencao.mywork.data.settings.UserPreferencesRepository
import com.jencao.mywork.data.sync.SyncScheduler
import com.jencao.mywork.reminder.ReminderScheduler
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

    @Inject
    lateinit var healthRepo: HealthRecordRepository

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
        // 健康提醒：确保通知渠道存在，并恢复所有未来提醒（首次安装也会创建渠道）
        ReminderScheduler.ensureChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val upcoming = healthRepo.getUpcomingReminders(System.currentTimeMillis())
            ReminderScheduler.rescheduleAll(this@MyWorkApplication, upcoming)
        }
    }
}
