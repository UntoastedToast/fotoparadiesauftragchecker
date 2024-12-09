package com.app.fotoparadiesauftragchecker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.fotoparadiesauftragchecker.databinding.ActivitySettingsBinding
import com.app.fotoparadiesauftragchecker.notification.NotificationService
import com.app.fotoparadiesauftragchecker.worker.OrderCheckWorker
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var notificationService: NotificationService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (isGranted) {
            prefs.edit().putBoolean("notifications_enabled", true).apply()
            binding.notificationSwitch.isChecked = true
            scheduleWork(binding.updateIntervalInput.text.toString().toIntOrNull() ?: 30)
            Snackbar.make(binding.root, "Benachrichtigungen aktiviert", Snackbar.LENGTH_SHORT).show()
        } else {
            prefs.edit().putBoolean("notifications_enabled", false).apply()
            binding.notificationSwitch.isChecked = false
            WorkManager.getInstance(this).cancelUniqueWork("order_check_work")
            Snackbar.make(binding.root, "Benachrichtigungsberechtigung verweigert", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationService = NotificationService(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Einstellungen"

        // Load saved settings
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        binding.notificationSwitch.isChecked = prefs.getBoolean("notifications_enabled", true)
        binding.updateIntervalInput.setText(prefs.getInt("update_interval", 30).toString())

        // Save settings when notification switch changes
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestNotificationPermission()
            } else {
                prefs.edit().putBoolean("notifications_enabled", false).apply()
                WorkManager.getInstance(this).cancelUniqueWork("order_check_work")
                Snackbar.make(binding.root, "Benachrichtigungen deaktiviert", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Test notification button
        binding.testNotificationButton.setOnClickListener {
            if (binding.notificationSwitch.isChecked) {
                checkNotificationPermission {
                    notificationService.showOrderReadyNotification("304480", "2334")
                    Snackbar.make(binding.root, "Test-Benachrichtigung gesendet", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(binding.root, "Bitte aktiviere zuerst die Benachrichtigungen", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Save settings when update interval changes
        binding.updateIntervalInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val interval = binding.updateIntervalInput.text.toString().toIntOrNull() ?: 30
                val validInterval = interval.coerceIn(15, 180) // Zwischen 15 Minuten und 3 Stunden
                binding.updateIntervalInput.setText(validInterval.toString())
                prefs.edit().putInt("update_interval", validInterval).apply()
                
                if (binding.notificationSwitch.isChecked) {
                    scheduleWork(validInterval)
                    Snackbar.make(
                        binding.root,
                        "Aktualisierungsintervall auf $validInterval Minuten gesetzt",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkNotificationPermission(onPermissionGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    onPermissionGranted()
                }
                else -> {
                    Snackbar.make(binding.root, "Benachrichtigungsberechtigung erforderlich", Snackbar.LENGTH_SHORT).show()
                }
            }
        } else {
            onPermissionGranted()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted, enable notifications
                    getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("notifications_enabled", true)
                        .apply()
                    scheduleWork(binding.updateIntervalInput.text.toString().toIntOrNull() ?: 30)
                    Snackbar.make(binding.root, "Benachrichtigungen aktiviert", Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    // Request the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android versions below 13, no runtime permission is needed
            getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("notifications_enabled", true)
                .apply()
            scheduleWork(binding.updateIntervalInput.text.toString().toIntOrNull() ?: 30)
            Snackbar.make(binding.root, "Benachrichtigungen aktiviert", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun scheduleWork(intervalMinutes: Int) {
        val orderCheckRequest = PeriodicWorkRequestBuilder<OrderCheckWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES,
            (intervalMinutes / 2).toLong(), TimeUnit.MINUTES // Flex interval für Batterie-Optimierung
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "order_check_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            orderCheckRequest
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
