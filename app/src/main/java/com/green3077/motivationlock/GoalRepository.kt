package com.green3077.motivationlock

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 목표(사진+문구)를 저장/조회한다. 지금은 목표를 1개만 다루지만, 나중에 여러 개로 확장할 때
 * 이 클래스 뒤로 숨겨서 호출부(MainActivity/LockScreenActivity)는 그대로 두고 내부만 바꿀 수
 * 있게 분리해둔다.
 *
 * 사진은 Photo Picker가 주는 content:// URI를 그대로 들고 있지 않고 앱 내부 저장소로 복사해서
 * 보관한다 - content:// URI에 대한 읽기 권한은 기기 재부팅이나 원본 파일 삭제로 사라질 수 있는데,
 * 잠금화면은 오래도록(재부팅 이후에도) 계속 같은 사진을 보여줘야 하기 때문이다.
 */
class GoalRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val photoFile: File
        get() = File(context.filesDir, PHOTO_FILE_NAME)

    fun hasGoal(): Boolean = photoFile.exists() && !getGoalText().isNullOrBlank()

    fun getGoalText(): String? = prefs.getString(KEY_GOAL_TEXT, null)

    fun saveGoalText(text: String) {
        prefs.edit().putString(KEY_GOAL_TEXT, text).apply()
    }

    fun savePhoto(sourceUri: Uri) {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            photoFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun isAutoEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_ENABLED, false)

    fun setAutoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "motivation_lock_prefs"
        private const val KEY_GOAL_TEXT = "goal_text"
        private const val KEY_AUTO_ENABLED = "auto_enabled"
        private const val PHOTO_FILE_NAME = "goal_photo.jpg"
    }
}
