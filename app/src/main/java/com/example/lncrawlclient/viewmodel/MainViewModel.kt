package com.example.lncrawlclient.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lncrawlclient.worker.CrawlWorker
import com.example.lncrawlclient.data.AppDatabase
import com.example.lncrawlclient.data.NetworkModule
import com.example.lncrawlclient.data.TaskEntity
import com.example.lncrawlclient.data.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class UiState(val tasks: List<TaskEntity> = emptyList())

class MainViewModel(application: Application): AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    var url: String = ""
    var latestLogs: String? = null
    private val downloadRepo = DownloadRepository(OkHttpClient(), application.applicationContext)

    init {
        viewModelScope.launch {
            db.taskDao().getAll().collect { list ->
                _uiState.value = UiState(tasks = list)
            }
        }
    }

    fun startCrawl() {
        val data = Data.Builder().putString("url", url).putString("baseUrl", com.example.lncrawlclient.Prefs.baseUrl).build()
        val req = OneTimeWorkRequestBuilder<CrawlWorker>().setInputData(data).build()
        WorkManager.getInstance(getApplication()).enqueue(req)
    }

    fun refreshTasks() {
        viewModelScope.launch {
            val api = NetworkModule.create(Prefs.baseUrl)
            db.taskDao().getAll().collect { tasks ->
                tasks.forEach { task ->
                    try {
                        val st = api.status(task.id)
                        if (st.isSuccessful) {
                            val body = st.body()!!
                            db.taskDao().insert(TaskEntity(id = task.id, url = task.url, status = body.status, progress = if (body.status=="done") 1f else 0.5f, outputUrl = body.out_dir))
                            latestLogs = body.log_tail
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun downloadOutput(task: TaskEntity) {
        viewModelScope.launch {
            try {
                val outUriString = Prefs.outputUri
                if (outUriString != null) {
                    val destUri = Uri.parse(outUriString)
                    // Use SAF multithreaded download
                    downloadRepo.downloadMultithreadedSaf(task.outputUrl ?: task.url ?: "", destUri, parts = 4) { p ->
                        // update progress in DB
                        db.taskDao().insert(TaskEntity(id = task.id, url = task.url, status = "downloading", progress = p, outputUrl = task.outputUrl))
                    }
                    db.taskDao().insert(TaskEntity(id = task.id, url = task.url, status = "done", progress = 1f, outputUrl = task.outputUrl))
                } else {
                    // no SAF configured - just mark error
                    db.taskDao().insert(TaskEntity(id = task.id, url = task.url, status = "no_output_folder", progress = 0f, outputUrl = task.outputUrl))
                }
            } catch (e: Exception) {
                db.taskDao().insert(TaskEntity(id = task.id, url = task.url, status = "error", progress = 0f, outputUrl = task.outputUrl))
            }
        }
    }
}
