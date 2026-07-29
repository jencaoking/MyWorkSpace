package com.jencao.mywork.dailypending

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jencao.mywork.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.jencao.mywork.data.repository.DailyPendingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 每日作业提醒接收器：
 * - 早间 08:00：有待处理作业时弹「昨日作业未完成」通知；
 * - 周日 20:00：弹本周回顾摘要通知。
 * 触发后自动重排下一次闹钟，形成循环。
 */
class DailyPendingReminderReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailyPendingEntryPoint {
        fun dailyPendingRepo(): DailyPendingRepository
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        scope.launch {
            try {
                val repo = EntryPointAccessors.fromApplication(
                    context.applicationContext, DailyPendingEntryPoint::class.java
                ).dailyPendingRepo()
                when (action) {
                    DailyPendingScheduler.ACTION_MORNING -> {
                        // 先兜底归档一次，保证计数是最新的
                        runCatching { repo.archiveOverdueTasks() }
                        val count = repo.getPendingCount()
                        if (count > 0) {
                            notify(
                                context, 9201,
                                "你有 $count 项作业未完成",
                                "昨日有 $count 个任务未按时完成，点击去处理（补做 / 改期 / 放弃）。"
                            )
                        }
                        DailyPendingScheduler.scheduleMorningReminder(context)
                    }
                    DailyPendingScheduler.ACTION_WEEKLY -> {
                        val review = repo.weeklyReview()
                        if (review.total > 0) {
                            notify(
                                context, 9202,
                                "本周作业回顾",
                                "本周共产生 ${review.total} 项未完成作业：补做 ${review.completed}、改期 ${review.rescheduled}、放弃 ${review.abandoned}、待处理 ${review.pending}。"
                            )
                        }
                        DailyPendingScheduler.scheduleWeeklyReminder(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun notify(context: Context, id: Int, title: String, content: String) {
        DailyPendingScheduler.ensureChannel(context)
        val notification = NotificationCompat.Builder(context, DailyPendingScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(DailyPendingScheduler.contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(id, notification)
    }
}
