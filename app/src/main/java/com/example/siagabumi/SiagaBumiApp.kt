package com.example.siagabumi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class SiagaBumiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Osmdroid configuration for map tiles user agent
        Configuration.getInstance().userAgentValue = packageName
    }
}
