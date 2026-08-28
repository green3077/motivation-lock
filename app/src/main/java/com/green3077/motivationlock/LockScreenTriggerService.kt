package com.green3077.motivationlock

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * 화면 켜짐(ACTION_SCREEN_ON)을 감지해서, 기기가 잠긴 상태라면 LockScreenActivity를 띄우는
 * 포그라운드 서비스. ACTION_SCREEN_ON은 API 26+부터 매니페스트에 정적 리시버로 등록해도
 * 시스템이 무시하므로, 반드시 실행 중인 컴포넌트(이 서비스)에서 런타임 등록해야 한다.
 *
 * 화면을 띄우는 방법: BroadcastReceiver 안에서 context.startActivity()를 바로 호출하면
 * 안 된다 - Android 10(API 29)부터는 "백그라운드에서 Activity 시작 금지" 제한이 강화되어,
 * 포그라운드 서비스라 해도 이렇게 직접 startActivity를 부르면 시스템이 조용히 무시해버린다
 * (예외/에러 없이 그냥 안 뜸 - "저장해도 잠금화면에 안 뜬다"의 실제 원인). 전화/알람 앱이
 * 잠금화면 위에 뜨는 것과 같은 공식적으로 허용된 방법은 Notification.setFullScreenIntent()
 * 다 - 이 알림을 IMPORTANCE_HIGH 채널로 올리면 시스템이 백그라운드 제한을 우회해서
 * PendingIntent가 가리키는 Activity를 직접 띄워준다.
 */
class LockScreenTriggerService : Service() {

    private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_ON) return
            if (!keyguardManager.isKeyguardLocked) return
            val repository = GoalRepository(context)
            if (!repository.hasGoal()) return
            showFullScreenAlert(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(screenOnReceiver) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_content_title))
            .setContentText(getString(R.string.notif_content_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun showFullScreenAlert(context: Context) {
        val lockScreenIntent = Intent(context, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            lockScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_content_title))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "lock_screen_trigger_channel"
        const val ALERT_CHANNEL_ID = "lock_screen_alert_channel"
        private const val NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 2

        fun start(context: Context) {
            val intent = Intent(context, LockScreenTriggerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenTriggerService::class.java))
        }
    }
}
