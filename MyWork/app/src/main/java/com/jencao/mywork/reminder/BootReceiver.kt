package com.jencao.mywork.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 开机 / 应用重装后恢复所有未来提醒。
 * 通过 Hilt EntryPoint 取得健康仓储读取待提醒记录，再交给 ReminderScheduler 重新排程。
 * 使用 goAsync() 保证协程在接收器生命周期内执行完。
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val isRelevant = action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == "android.intent.action.QUICKBOOT_POWERON"
        if (!isRelevant) return

        val pending = goAsync()
        scope.launch {
            try {
                ReminderScheduler.ensureChannel(context)
                val repo = EntryPointAccessors.fromApplication(
                    context.applicationContext, ReminderEntryPoint::class.java
                ).healthRepo()
                val records = repo.getUpcomingReminders(System.currentTimeMillis())
                ReminderScheduler.rescheduleAll(context.applicationContext, records)
            } finally {
                pending.finish()
            }
        }
    }
}
