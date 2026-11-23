package com.example.siagabumi.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.siagabumi.data.mapper.GempaMapper
import com.example.siagabumi.data.remote.GempaApi
import com.example.siagabumi.domain.model.GempaCategory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class GempaCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GempaWorkerEntryPoint {
        fun gempaApi(): GempaApi
    }

    override suspend fun doWork(): Result {
        return try {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                GempaWorkerEntryPoint::class.java
            )
            val gempaApi = hiltEntryPoint.gempaApi()

            val response = gempaApi.getAutoGempa()
            val dto = response.infoGempa?.gempa

            if (dto != null) {
                val gempa = GempaMapper.toDomain(dto, GempaCategory.TERBARU)
                val prefs = applicationContext.getSharedPreferences("gempa_alerts_pref", Context.MODE_PRIVATE)
                val lastNotifiedId = prefs.getString("last_notified_gempa_id", null)

                if (gempa.magnitude >= 5.0 && gempa.id != lastNotifiedId) {
                    NotificationHelper.showGempaNotification(applicationContext, gempa)
                    prefs.edit().putString("last_notified_gempa_id", gempa.id).apply()
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "gempa_check_periodic_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<GempaCheckWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
