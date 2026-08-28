package com.green3077.motivationlock

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.io.File
import java.util.UUID

/**
 * 목표(보물지도 보드)를 저장/조회한다: 손글씨로 써내려가는 메인 목표 문구 1개 + 보드에 흩뿌려질
 * 사진 여러 장 + 짧은 문구(포스트잇) 여러 개.
 *
 * 사진은 Photo Picker가 주는 content:// URI를 그대로 들고 있지 않고 앱 내부 저장소로 복사해서
 * 보관한다 - content:// URI에 대한 읽기 권한은 기기 재부팅이나 원본 파일 삭제로 사라질 수 있는데,
 * 잠금화면은 오래도록(재부팅 이후에도) 계속 같은 사진을 보여줘야 하기 때문이다.
 */
class GoalRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val boardPhotosDir: File
        get() = File(context.filesDir, BOARD_PHOTOS_DIR_NAME).apply { mkdirs() }

    fun hasGoal(): Boolean = !getGoalText().isNullOrBlank()

    fun getGoalText(): String? = prefs.getString(KEY_GOAL_TEXT, null)

    fun saveGoalText(text: String) {
        prefs.edit().putString(KEY_GOAL_TEXT, text).apply()
    }

    /** 보드에 흩뿌려질 사진 파일 목록. 순서가 화면마다 흔들리지 않도록 파일명(추가된 순서) 기준 정렬. */
    fun getBoardPhotoFiles(): List<File> =
        boardPhotosDir.listFiles()?.sortedBy { it.name } ?: emptyList()

    fun addBoardPhoto(sourceUri: Uri) {
        val outFile = File(boardPhotosDir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
    }

    fun removeBoardPhoto(file: File) {
        file.delete()
    }

    /** 보드에 흩뿌려질 짧은 문구(포스트잇/말풍선 캡션). 손글씨 애니메이션은 메인 목표 문구에만 적용되고, 이건 정적으로 표시된다. */
    fun getNotes(): List<String> {
        val json = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).map { array.getString(it) }
    }

    fun saveNotes(notes: List<String>) {
        val array = JSONArray()
        notes.forEach { array.put(it) }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    fun isAutoEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_ENABLED, false)

    fun setAutoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "motivation_lock_prefs"
        private const val KEY_GOAL_TEXT = "goal_text"
        private const val KEY_NOTES = "notes"
        private const val KEY_AUTO_ENABLED = "auto_enabled"
        private const val BOARD_PHOTOS_DIR_NAME = "board_photos"
    }
}
