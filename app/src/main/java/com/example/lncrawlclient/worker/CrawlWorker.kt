package com.example.lncrawlclient.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lncrawlclient.data.NetworkModule
import com.example.lncrawlclient.data.StartRequest
import com.example.lncrawlclient.data.TaskEntity
import com.example.lncrawlclient.data.AppDatabase
import kotlinx.coroutines.delay

class CrawlWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val base = inputData.getString("baseUrl") ?: com.example.lncrawlclient.Prefs.baseUrl
        val api = NetworkModule.create(base)
        try {
            val resp = api.start(StartRequest(url))
            if (!resp.isSuccessful) return Result.failure()
            val taskId = resp.body()?.task_id ?: return Result.failure()

            // Persist initial Task
            val db = AppDatabase.getInstance(applicationContext)
            db.taskDao().insert(TaskEntity(id = taskId, url = url, status = "started", progress = 0f, outputUrl = null))

            // Poll status until done/error
            var done = false
            while (!done) {
                delay(2000)
                val st = api.status(taskId)
                if (!st.isSuccessful) continue
                val body = st.body()!!
                val status = body.status
                val progress = when(status) {
                    "running" -> 0.3f
                    "done" -> 1.0f
                    "error" -> 0f
                    else -> 0.0f
                }
                db.taskDao().insert(TaskEntity(id = taskId, url = url, status = status, progress = progress, outputUrl = body.out_dir))
                if (status == "done" || status == "error") done = true
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
