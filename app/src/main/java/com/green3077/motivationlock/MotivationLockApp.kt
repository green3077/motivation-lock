package com.green3077.motivationlock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MotivationLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            LockScreenTriggerService.CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.notif_channel_desc)
        }
        manager.createNotificationChannel(serviceChannel)

        // 잠금화면 위에 실제로 화면을 띄우는 트리거는 setFullScreenIntent()로 동작하는데,
        // 이건 채널 중요도가 HIGH 이상이어야 시스템이 실제로 전체화면으로 띄워준다
        // (낮은 중요도 채널이면 그냥 조용한 일반 알림으로만 취급되고 화면이 안 뜬다).
        val alertChannel = NotificationChannel(
            LockScreenTriggerService.ALERT_CHANNEL_ID,
            getString(R.string.notif_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notif_alert_channel_desc)
            setSound(null, null)
        }
        manager.createNotificationChannel(alertChannel)
    }
}
