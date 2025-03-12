package com.app.fotoparadiesauftragchecker

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Anwendungsklasse für die Fotoparadies-App
 * 
 * Diese Klasse wird beim App-Start initialisiert und wendet globale Einstellungen an,
 * wie z.B. das vom Benutzer gewählte Design-Theme (hell/dunkel)
 */
class FotoparadiesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Theme-Einstellung beim App-Start anwenden
        applyUserTheme()
    }
    
    /**
     * Wendet die vom Benutzer gespeicherte Theme-Einstellung an
     * 
     * Die Funktion liest die gespeicherte Einstellung aus den SharedPreferences
     * und setzt den entsprechenden Nachtmodus für die gesamte App
     */
    private fun applyUserTheme() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val nightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
