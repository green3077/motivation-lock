package com.green3077.motivationlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 재부팅 후에는 실행 중이던 포그라운드 서비스가 당연히 다 죽어있으므로, 사용자가 "자동으로
 * 뜨게 하기"를 켜뒀던 경우에만 다시 시작해준다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repository = GoalRepository(context)
        if (repository.isAutoEnabled() && repository.hasGoal()) {
            LockScreenTriggerService.start(context)
        }
    }
}
