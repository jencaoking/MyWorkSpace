package com.jencao.mywork.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jencao.mywork.MainActivity
import com.jencao.mywork.data.local.entity.HealthRecordEntity

/**
 * 复诊 / 用药提醒调度器（本地优先，不依赖云端）。
 *
 * 使用 AlarmManager.setAlarmClock 而非 setExactAndAllowWhileIdle：
 * - setAlarmClock 在已授予 SCHEDULE_EXACT_ALARM 时不受限制；未授予时降级为 setAndAllowWhileIdle（见 schedule）；
 * - 在断电 / 低电模式下仍能可靠触发，适合健康提醒这种"到点必须提醒"的场景；
 * - 状态栏会显示闹钟指示，语义上即"复诊闹钟 / 用药闹钟"。
 *
 * 密钥 / 网络均不涉及，纯本地调度。
 */
object ReminderScheduler {
    const val CHANNEL_ID = "health_reminder"
    const val CHANNEL_NAME = "复诊 / 用药提醒"
    private const val ACTION = "com.jencao.mywork.reminder.FIRE"

    /** 创建通知渠道（Android 8+ 必须，否则通知不显示）。幂等。 */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "健康复诊与用药提醒"
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    /** 提醒标题：用药用"用药提醒"，其余（复诊等）用"复诊提醒"。 */
    fun reminderTitle(rec: HealthRecordEntity): String =
        if (rec.type == "medication") "用药提醒" else "复诊提醒"

    /** 提醒正文：优先备注，否则用数值+单位，再否则兜底文案。 */
    fun reminderContent(rec: HealthRecordEntity): String =
        if (rec.note.isNotBlank()) rec.note
        else if (rec.value != 0f) "${rec.value}${if (rec.unit.isNotBlank()) " ${rec.unit}" else ""}"
        else "复诊 / 用药时间到了，请留意。"

    /** 构造广播 PendingIntent（AlarmManager 到点触发 ReminderReceiver）。 */
    private fun fireIntent(context: Context, recordId: String, title: String, content: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION
            putExtra("record_id", recordId)
            putExtra("title", title)
            putExtra("content", content)
        }
        return PendingIntent.getBroadcast(
            context, recordId.hashCode(),
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 点击通知 / 闹钟后打开 App 并定位到该健康记录。 */
    fun contentIntent(context: Context, recordId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("open_health_id", recordId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 安排单条提醒。trigger <= now 时直接跳过（不再补发过去的时间）。 */
    fun schedule(context: Context, rec: HealthRecordEntity, title: String, content: String) {
        val trigger = rec.reminderTime ?: return
        if (trigger <= System.currentTimeMillis()) return
        ensureChannel(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = fireIntent(context, rec.id, title, content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } else {
            am.setAlarmClock(
                AlarmManager.AlarmClockInfo(trigger, contentIntent(context, rec.id)),
                pi
            )
        }
    }

    /** 取消单条提醒（删除 / 修改时间时调用）。无对应闹钟时静默。 */
    fun cancel(context: Context, recordId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, recordId.hashCode(),
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION
                putExtra("record_id", recordId)
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { am.cancel(it) }
    }

    /** 重新安排（先取消旧的再按需安排新的），用于保存记录时保证状态一致。 */
    fun reschedule(context: Context, rec: HealthRecordEntity) {
        cancel(context, rec.id)
        if (rec.reminderTime != null && rec.reminderTime!! > System.currentTimeMillis()) {
            schedule(context, rec, reminderTitle(rec), reminderContent(rec))
        }
    }

    /** 开机 / 应用启动时批量恢复所有未来提醒。 */
    fun rescheduleAll(context: Context, records: List<HealthRecordEntity>) {
        val now = System.currentTimeMillis()
        records.filter { it.reminderTime != null && it.reminderTime!! > now }
            .forEach { schedule(context, it, reminderTitle(it), reminderContent(it)) }
    }
}
