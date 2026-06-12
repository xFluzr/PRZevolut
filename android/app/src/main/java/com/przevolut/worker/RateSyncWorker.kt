package com.przevolut.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.przevolut.domain.repository.RateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker — synchronizacja kursów walut w tle.
 * Uruchamiany co 1 godzinę gdy jest dostępne połączenie sieciowe.
 */
@HiltWorker
class RateSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val rateRepository: RateRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return rateRepository.refreshRates().fold(
            onSuccess = {
                Log.d(TAG, "Kursy zsynchronizowane pomyślnie")
                Result.success()
            },
            onFailure = { e ->
                Log.e(TAG, "Synchronizacja nieudana (próba $runAttemptCount)", e)
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        )
    }

    companion object {
        private const val TAG = "RateSyncWorker"
        private const val WORK_NAME = "rate_sync_periodic"
        private const val MAX_RETRIES = 3

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<RateSyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
