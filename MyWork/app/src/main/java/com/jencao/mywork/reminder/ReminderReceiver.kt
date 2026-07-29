package com.jencao.mywork.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jencao.mywork.R

/**
 * 由 AlarmManager 到点触发的广播接收器，负责弹出通知。
 * 标题 / 正文通过 Intent extras 传入，避免接收器再去读数据库，触发更可靠。
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val recordId = intent.getStringExtra("record_id") ?: return
        val title = intent.getStringExtra("title") ?: "健康提醒"
        val content = intent.getStringExtra("content") ?: ""
        ReminderScheduler.ensureChannel(context)

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(ReminderScheduler.contentIntent(context, recordId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(recordId.hashCode(), notification)
    }
}
