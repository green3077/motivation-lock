package com.green3077.motivationlock

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.green3077.motivationlock.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: GoalRepository
    private var pickedPhotoUri: Uri? = null

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                pickedPhotoUri = uri
                binding.ivPhotoPreview.setImageURI(uri)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GoalRepository(this)
        loadExistingGoal()

        binding.btnPickPhoto.setOnClickListener {
            pickPhotoLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

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
                requestIgnoreBatteryOptimizations()
                LockScreenTriggerService.start(this)
            } else {
                LockScreenTriggerService.stop(this)
            }
        }
    }

    private fun loadExistingGoal() {
        binding.etGoalText.setText(repository.getGoalText().orEmpty())
        if (repository.photoFile.exists()) {
            binding.ivPhotoPreview.setImageURI(repository.photoFile.toUri())
        }
    }

    private fun saveGoal() {
        val text = binding.etGoalText.text?.toString()?.trim().orEmpty()
        pickedPhotoUri?.let { repository.savePhoto(it) }
        if (text.isNotEmpty()) {
            repository.saveGoalText(text)
        }
        Toast.makeText(this, R.string.toast_goal_saved, Toast.LENGTH_SHORT).show()
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
}
