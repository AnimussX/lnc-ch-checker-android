package com.example.lncrawlclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lncrawlclient.viewmodel.MainViewModel

@Composable
fun QueueScreen(viewModel: MainViewModel) {
    val ui by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = viewModel.url, onValueChange = { viewModel.url = it }, label = { Text("Novel URL") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.startCrawl() }) { Text("Start Crawl") }
            Button(onClick = { viewModel.refreshTasks() }) { Text("Refresh") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tasks:")
        ui.tasks.forEach { task ->
            DownloadItemSimple(task.id, task.url ?: "-", task.status ?: "-", task.progress ?: 0f)
        }
    }
}

@Composable
fun DownloadItemSimple(id: String, url: String, status: String, progress: Float) {
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(url)
            LinearProgressIndicator(progress = progress)
            Text("Status: $status  ${(progress*100).toInt()}%" )
        }
    }
}
