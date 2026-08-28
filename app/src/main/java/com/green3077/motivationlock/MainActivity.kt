package com.green3077.motivationlock

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.setMargins
import com.google.android.material.chip.Chip
import com.green3077.motivationlock.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: GoalRepository

    private val pickPhotosLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_BOARD_PHOTOS)) { uris ->
            uris.forEach { repository.addBoardPhoto(it) }
            refreshPhotoThumbnails()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GoalRepository(this)
        loadExistingGoal()

        binding.btnAddBoardPhoto.setOnClickListener {
            pickPhotosLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnAddNote.setOnClickListener { addNote() }

        binding.btnSaveGoal.setOnClickListener { saveGoal() }

        binding.btnPreviewLockScreen.setOnClickListener {
            if (!repository.hasGoal()) {
                Toast.makeText(this, R.string.toast_goal_required, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, LockScreenActivity::class.java))
            }
        }

        binding.switchAutoEnable.isChecked = repository.isAutoEnabled()
        binding.switchAutoEnable.setOnCheckedChangeListener { _, isChecked ->
            repository.setAutoEnabled(isChecked)
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
                requestFullScreenIntentPermissionIfNeeded()
                requestIgnoreBatteryOptimizations()
                LockScreenTriggerService.start(this)
            } else {
                LockScreenTriggerService.stop(this)
            }
        }
    }

    private fun loadExistingGoal() {
        binding.etGoalText.setText(repository.getGoalText().orEmpty())
        refreshPhotoThumbnails()
        refreshNotesChips()
    }

    private fun saveGoal() {
        val text = binding.etGoalText.text?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) {
            repository.saveGoalText(text)
        }
        Toast.makeText(this, R.string.toast_goal_saved, Toast.LENGTH_SHORT).show()
    }

    private fun addNote() {
        val note = binding.etNoteInput.text?.toString()?.trim().orEmpty()
        if (note.isEmpty()) return
        repository.saveNotes(repository.getNotes() + note)
        binding.etNoteInput.setText("")
        refreshNotesChips()
    }

    /** 사진/문구는 추가·삭제하는 즉시 저장한다 - "저장" 버튼은 메인 목표 문구에만 해당된다. */
    private fun refreshPhotoThumbnails() {
        val container = binding.photoThumbnailContainer
        container.removeAllViews()
        val thumbnailSizePx = (THUMBNAIL_SIZE_DP * resources.displayMetrics.density).toInt()
        val marginPx = (THUMBNAIL_MARGIN_DP * resources.displayMetrics.density).toInt()

        repository.getBoardPhotoFiles().forEach { file ->
            val frame = FrameLayout(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(thumbnailSizePx, thumbnailSizePx).apply {
                    setMargins(marginPx)
                }
            }
            val bitmap = ImageUtils.decodeSampledBitmap(file.absolutePath, thumbnailSizePx)
            frame.addView(ImageView(this).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })
            frame.addView(TextView(this).apply {
                text = "✕"
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x99000000.toInt())
                gravity = Gravity.CENTER
                val closeSizePx = (CLOSE_BUTTON_SIZE_DP * resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(closeSizePx, closeSizePx, Gravity.TOP or Gravity.END)
                setOnClickListener {
                    repository.removeBoardPhoto(file)
                    refreshPhotoThumbnails()
                }
            })
            container.addView(frame)
        }
    }

    private fun refreshNotesChips() {
        val group = binding.notesChipGroup
        group.removeAllViews()
        repository.getNotes().forEach { note ->
            val chip = Chip(this).apply {
                text = note
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    repository.saveNotes(repository.getNotes() - note)
                    refreshNotesChips()
                }
            }
            group.addView(chip)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestFullScreenIntentPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 34) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.canUseFullScreenIntent()) return

        AlertDialog.Builder(this)
            .setTitle(R.string.full_screen_intent_title)
            .setMessage(R.string.full_screen_intent_message)
            .setPositiveButton(R.string.full_screen_intent_confirm) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
            }
            .setNegativeButton(R.string.full_screen_intent_cancel, null)
            .show()
    }

    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        AlertDialog.Builder(this)
            .setTitle(R.string.battery_optimization_title)
            .setMessage(R.string.battery_optimization_message)
            .setPositiveButton(R.string.battery_optimization_confirm) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
            }
            .setNegativeButton(R.string.battery_optimization_cancel, null)
            .show()
    }

    companion object {
        private const val MAX_BOARD_PHOTOS = 9
        private const val THUMBNAIL_SIZE_DP = 72
        private const val THUMBNAIL_MARGIN_DP = 4
        private const val CLOSE_BUTTON_SIZE_DP = 22
    }
}
