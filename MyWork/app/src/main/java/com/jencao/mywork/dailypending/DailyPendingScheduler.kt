package com.jencao.mywork.dailypending

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jencao.mywork.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 每日未完成作业调度器：
 * - 归档：WorkManager 周期任务（24h，对齐每天 00:05）+ 应用启动兜底一次；
 * - 早间提醒：AlarmManager 每天 08:00（有待处理作业才弹通知）；
 * - 周回顾提醒：AlarmManager 每周日 20:00。
 * 与健康提醒一致使用 setAlarmClock，规避 Android 12+ 精确闹钟权限问题。
 */
object DailyPendingScheduler {
    const val CHANNEL_ID = "daily_pending_reminder"
    private const val CHANNEL_NAME = "每日作业提醒"
    private const val ARCHIVE_PERIODIC_ID = "daily_pending_archive"
    private const val ARCHIVE_ONETIME_ID = "daily_pending_archive_now"
    const val ACTION_MORNING = "com.jencao.mywork.dailypending.MORNING"
    const val ACTION_WEEKLY = "com.jencao.mywork.dailypending.WEEKLY"
    private const val REQ_MORNING = 9101
    private const val REQ_WEEKLY = 9102

    /** 幂等注册所有调度（应用启动 / 开机时调用） */
    fun scheduleAll(context: Context) {
        ensureChannel(context)
        scheduleArchiveWorker(context)
        archiveNow(context)
        scheduleMorningReminder(context)
        scheduleWeeklyReminder(context)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "昨日未完成作业与每周回顾提醒"
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    /** 周期归档：24h 一次，初始延迟对齐下一个 00:05 */
    private fun scheduleArchiveWorker(context: Context) {
        val req = PeriodicWorkRequestBuilder<DailyArchiveWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntil(0, 5), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(ARCHIVE_PERIODIC_ID, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** 启动兜底：立即归档一次（幂等），覆盖凌晨设备关机错过调度的情况 */
    fun archiveNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<DailyArchiveWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ARCHIVE_ONETIME_ID, ExistingWorkPolicy.REPLACE, req)
    }

    /** 每天 08:00 早间提醒（触发后由 Receiver 重新排下一天） */
    fun scheduleMorningReminder(context: Context) {
        val trigger = System.currentTimeMillis() + millisUntil(8, 0)
        setAlarm(context, ACTION_MORNING, REQ_MORNING, trigger)
    }

    /** 每周日 20:00 周回顾提醒（触发后由 Receiver 重新排下一周） */
    fun scheduleWeeklyReminder(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY || timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        setAlarm(context, ACTION_WEEKLY, REQ_WEEKLY, cal.timeInMillis)
    }

    /** 点击通知打开 App 并跳转每日作业页 */
    fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("open_daily_pending", true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context, REQ_MORNING, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAlarm(context: Context, action: String, requestCode: Int, trigger: Long) {
        ensureChannel(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, DailyPendingReminderReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, contentIntent(context)), pi)
    }

    /** 距下一个 hour:minute 的毫秒数（若今天已过则取明天） */
    private fun millisUntil(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis - System.currentTimeMillis()
    }
}
