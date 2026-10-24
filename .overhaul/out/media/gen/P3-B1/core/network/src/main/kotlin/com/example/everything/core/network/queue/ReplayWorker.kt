package com.example.everything.core.network.queue

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.everything.core.common.log.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** Runs a replay pass when the device has a network, and again, further apart, while entries remain. */
internal class ReplayWorker(
    context: Context,
    parameters: WorkerParameters,
    private val replayer: QueuedRequestReplayer,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = try {
        if (replayer.replayPending() == 0) Result.success() else Result.retry()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        // A storage error or a row that no longer parses: try again later rather than give up on
        // everything queued behind it.
        Log.e("Replaying the offline queue failed", failure, TAG)
        Result.retry()
    }

    private companion object {
        const val TAG = "OfflineQueue"
    }
}

/** Starts a replay as soon as the device has a network. */
@Singleton
class ReplayScheduler @Inject internal constructor(
    @ApplicationContext private val context: Context,
) {
    fun replaySoon() {
        val request = OneTimeWorkRequestBuilder<ReplayWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, FIRST_RETRY_SECONDS, TimeUnit.SECONDS)
            .build()
        // Appended rather than kept: a pass already running may have read the queue before this
        // entry arrived, and would finish without sending it.
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "offline-queue"
        const val FIRST_RETRY_SECONDS = 30L
    }
}

/**
 * Builds [ReplayWorker] with its dependencies. The app hands it to WorkManager through
 * `Configuration.Provider`, which also keeps WorkManager from starting at every launch: it starts
 * the first time something is queued.
 */
@Singleton
class OfflineQueueWorkerFactory @Inject internal constructor(
    private val replayer: Provider<QueuedRequestReplayer>,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        ReplayWorker::class.java.name -> ReplayWorker(appContext, workerParameters, replayer.get())
        else -> null
    }
}
