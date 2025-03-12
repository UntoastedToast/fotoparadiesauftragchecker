package com.app.fotoparadiesauftragchecker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
    
    // Setzt das Ergebnis, das an die MainActivity zurückgegeben wird
    private var settingsChanged = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        if (isGranted) {
            prefs.edit { putBoolean("notifications_enabled", true) }
            binding.notificationSwitch.isChecked = true
            scheduleWork(binding.updateIntervalInput.text.toString().toIntOrNull() ?: 30)
            Snackbar.make(binding.root, getString(R.string.notifications_enabled), Snackbar.LENGTH_SHORT).show()
            settingsChanged = true
        } else {
            prefs.edit { putBoolean("notifications_enabled", false) }
            binding.notificationSwitch.isChecked = false
            WorkManager.getInstance(this).cancelUniqueWork("order_check_work")
            Snackbar.make(binding.root, getString(R.string.notifications_permission_denied), Snackbar.LENGTH_SHORT).show()
            settingsChanged = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-Edge aktivieren
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Insets für das Layout verwalten
        setupInsets()

        notificationService = NotificationService(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        // Load saved settings
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        binding.notificationSwitch.isChecked = prefs.getBoolean("notifications_enabled", true)
        binding.updateIntervalInput.setText(prefs.getInt("update_interval", 30).toString())
        
        // Theme-Einstellung laden
        val currentNightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> binding.themeSystemRadio.isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> binding.themeLightRadio.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.themeDarkRadio.isChecked = true
        }

        // Save settings when notification switch changes
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestNotificationPermission()
            } else {
                prefs.edit { putBoolean("notifications_enabled", false) }
                WorkManager.getInstance(this).cancelUniqueWork("order_check_work")
                Snackbar.make(binding.root, getString(R.string.notifications_disabled), Snackbar.LENGTH_SHORT).show()
                settingsChanged = true
            }
        }

        // Test notification button
        binding.testNotificationButton.setOnClickListener {
            if (binding.notificationSwitch.isChecked) {
                checkNotificationPermission {
                    notificationService.showOrderReadyNotification("304480", "2334")
                    Snackbar.make(binding.root, getString(R.string.test_notification_sent), Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(binding.root, getString(R.string.enable_notifications_first), Snackbar.LENGTH_SHORT).show()
            }
        }

        // Save settings when update interval changes
        binding.updateIntervalInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val interval = binding.updateIntervalInput.text.toString().toIntOrNull() ?: 30
                val validInterval = interval.coerceIn(15, 180) // Zwischen 15 Minuten und 3 Stunden
                binding.updateIntervalInput.setText(validInterval.toString())
                
                // Wenn sich das Intervall geändert hat
                if (prefs.getInt("update_interval", 30) != validInterval) {
                    prefs.edit { putInt("update_interval", validInterval) }
                    settingsChanged = true
                    
                    if (binding.notificationSwitch.isChecked) {
                        scheduleWork(validInterval)
                        Snackbar.make(
                            binding.root,
                            getString(R.string.update_interval_set, validInterval),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        
        // Theme RadioGroup Listener
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val nightMode = when (checkedId) {
                R.id.themeSystemRadio -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                R.id.themeLightRadio -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.themeDarkRadio -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            
            // Aktuelle Einstellung abrufen
            val currentNightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            
            // Nur wenn sich die Einstellung geändert hat, aktualisieren
            if (currentNightMode != nightMode) {
                prefs.edit { putInt("night_mode", nightMode) }
                AppCompatDelegate.setDefaultNightMode(nightMode)
                settingsChanged = true
            }
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // AppBar mit Statusbar-Insets ausrichten
            binding.appBarLayout.updatePadding(top = insets.top)
            
            windowInsets
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
                    Snackbar.make(binding.root, getString(R.string.notifications_permission_required), Snackbar.LENGTH_SHORT).show()
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
                    getSharedPreferences("settings", MODE_PRIVATE)
                        .edit {
                            putBoolean("notifications_enabled", true)
                        }
                    scheduleWork(binding.updateIntervalInput.text.toString().toIntOrNull() ?: 30)
                    Snackbar.make(binding.root, getString(R.string.notifications_enabled), Snackbar.LENGTH_SHORT).show()
                    settingsChanged = true
                }
                else -> {
                    // Request the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android versions below 13, no runtime permission is needed
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit {
                    putBoolean("notifications_enabled", true)
                }
            scheduleWork(binding.updateIntervalInput.text.toString().toIntOrNull() ?: 30)
            Snackbar.make(binding.root, getString(R.string.notifications_enabled), Snackbar.LENGTH_SHORT).show()
            settingsChanged = true
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

    override fun finish() {
        // Setze das Ergebnis und signalisiere der MainActivity, dass ein Refresh erforderlich ist,
        // wenn Einstellungen geändert wurden
        if (settingsChanged) {
            val resultIntent = Intent()
            resultIntent.putExtra(SETTINGS_CHANGED, true)
            setResult(RESULT_OK, resultIntent)
        }
        super.finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    companion object {
        const val SETTINGS_CHANGED = "settings_changed"
    }
}