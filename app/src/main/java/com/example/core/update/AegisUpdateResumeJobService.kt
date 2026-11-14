package com.example.core.update

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log

/**
 * Native Android JobService that keeps silent 100 KB/s update pre-downloading alive
 * and resumable even after the user closes Sheildlenz from Recents or battery recovers > 20%.
 */
class AegisUpdateResumeJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "Resuming persisted update pre-download job.")
        UpdateChecker.resumePersistedPreloadFromJob(applicationContext) {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Reschedule automatically if OS pauses due to battery/network change
        return UpdateChecker.hasIncompletePersistedDownload(applicationContext)
    }

    companion object {
        private const val TAG = "AegisUpdateResumeJob"
        private const val JOB_ID = 7741

        fun schedule(context: Context) {
            try {
                val appCtx = context.applicationContext
                val scheduler = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(appCtx, AegisUpdateResumeJobService::class.java)
                val jobInfo = JobInfo.Builder(JOB_ID, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresBatteryNotLow(true)
                    .setPersisted(true)
                    .setMinimumLatency(3_000L)
                    .build()
                scheduler.schedule(jobInfo)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to schedule resume job: ${e.message}")
            }
        }

        fun cancel(context: Context) {
            try {
                val scheduler = context.applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                scheduler?.cancel(JOB_ID)
            } catch (_: Exception) {
            }
        }
    }
}
