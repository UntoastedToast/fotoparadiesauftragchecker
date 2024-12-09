package com.app.fotoparadiesauftragchecker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.fotoparadiesauftragchecker.worker.OrderCheckWorker
import java.util.concurrent.TimeUnit

class FotoparadiesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupPeriodicOrderCheck()
    }

    private fun setupPeriodicOrderCheck() {
        val orderCheckRequest = PeriodicWorkRequestBuilder<OrderCheckWorker>(
            30, TimeUnit.MINUTES, // Minimum interval ist 15 Minuten
            15, TimeUnit.MINUTES  // Flex interval für Batterie-Optimierung
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "order_check_work",
            ExistingPeriodicWorkPolicy.KEEP, // Behält existierende Arbeit bei
            orderCheckRequest
        )
    }
}
