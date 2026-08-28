package com.green3077.motivationlock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.green3077.motivationlock.databinding.ActivityLockScreenBinding

/**
 * 화면이 켜질 때 진짜 시스템 잠금화면(PIN/패턴/지문)보다 먼저 뜨는 화면. setShowWhenLocked +
 * setTurnScreenOn으로 "잠긴 상태에서도 이 Activity가 화면에 보이게" 만든다 - 오버레이 창
 * (SYSTEM_ALERT_WINDOW)이 아니라 평범한 Activity라 별도 "다른 앱 위에 표시" 권한이 필요 없다.
 *
 * 화면 아무 곳이나 누르면 finish()로 닫히고, 그 뒤에 있던 진짜 잠금화면(잠금 설정이 없으면 홈
 * 화면)이 그대로 이어서 보인다 - 우리가 잠금 자체를 대신 처리하는 게 아니다.
 */
class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.lockScreenRoot.setOnClickListener { finish() }

        val repository = GoalRepository(this)
        if (repository.photoFile.exists()) {
            binding.ivGoalPhoto.setImageURI(repository.photoFile.toUri())
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면을 켤 때마다(=이 Activity가 다시 보일 때마다) 처음부터 다시 써내려가는 느낌을 준다.
        val repository = GoalRepository(this)
        val goalText = repository.getGoalText().orEmpty()
        binding.tvGoalTextHandwriting.playHandwriting(goalText)
    }
}
