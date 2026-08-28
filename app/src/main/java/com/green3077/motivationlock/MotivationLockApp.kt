package com.green3077.motivationlock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MotivationLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            LockScreenTriggerService.CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.notif_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }
}
