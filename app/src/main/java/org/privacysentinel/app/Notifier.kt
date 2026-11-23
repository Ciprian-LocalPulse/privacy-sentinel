package org.privacysentinel.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object Notifier {
    private const val CHANNEL_MONITOR = "monitor"
    private const val CHANNEL_ALERTS = "alerts"
    const val ID_MONITOR = 1
    private var counter = 0

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                ctx.getString(R.string.channel_monitor),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                ctx.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun openAppIntent(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun monitorNotification(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(ctx.getString(R.string.notif_monitor_title))
            .setContentText(ctx.getString(R.string.notif_monitor_text))
            .setContentIntent(openAppIntent(ctx))
            .setOngoing(true)
            .setSilent(true)
            .build()

    @Synchronized
    fun alert(ctx: Context, title: String, text: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppIntent(ctx))
            .setAutoCancel(true)
            .build()
        nm.notify(100 + (counter++ % 50), notification)
    }
}
