package com.capstone.houseviewingapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.capstone.houseviewingapp.MainActivity
import com.capstone.houseviewingapp.R

class RegistryNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
            .ifEmpty { extras.getCharSequence("android.title")?.toString().orEmpty() }
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
            .ifEmpty { extras.getCharSequence("android.text")?.toString().orEmpty() }
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val combined = "$title $text $bigText $subText"

        if (!combined.contains("등기부등본") || !combined.contains("변경")) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ensureChannel(this)

        val notifTitle = "등기부등본 변경 알림"
        val contentText = "등기부등본 변경이 감지되었습니다. 탭하여 분석을 진행하세요."

        val ourNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.houseviewingalarmicon)
            .setContentTitle(notifTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText).setBigContentTitle(notifTitle))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent())
            .build()
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, ourNotification)
        } catch (e: SecurityException) {
            // 알림 권한 거부 시 무시
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP // -> 기존 액티비티 재사용, 새로 시작하지 않음
            putExtra(MainActivity.EXTRA_SHOW_REGISTRY_CHANGE_DIALOG, true)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "등기부등본 변경 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setDescription("등기부등본 변경이 감지되었을 때 알림을 표시하는 채널입니다.")
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "registry_change_channel"
        private const val NOTIFICATION_ID = 1001
    }
}