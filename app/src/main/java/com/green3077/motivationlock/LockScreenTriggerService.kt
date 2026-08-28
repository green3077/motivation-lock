package com.green3077.motivationlock

import android.app.KeyguardManager
import android.app.Notification
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
 */
class LockScreenTriggerService : Service() {

    private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_ON) return
            if (!keyguardManager.isKeyguardLocked) return
            val repository = GoalRepository(context)
            if (!repository.hasGoal()) return

            val lockScreenIntent = Intent(context, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(lockScreenIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
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

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_content_title))
            .setContentText(getString(R.string.notif_content_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "lock_screen_trigger_channel"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, LockScreenTriggerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenTriggerService::class.java))
        }
    }
}
