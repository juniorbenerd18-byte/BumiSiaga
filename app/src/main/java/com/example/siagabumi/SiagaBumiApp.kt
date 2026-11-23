package com.example.siagabumi

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SiagaBumiApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Setup Notification Channel & Background WorkManager for Gempa Alerts
        com.example.siagabumi.worker.NotificationHelper.createNotificationChannel(this)
        com.example.siagabumi.worker.GempaCheckWorker.schedule(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
