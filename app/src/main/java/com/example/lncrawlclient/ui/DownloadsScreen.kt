package com.example.lncrawlclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lncrawlclient.viewmodel.MainViewModel

@Composable
fun DownloadsScreen(viewModel: MainViewModel, onOpenDownload: (String)->Unit) {
    val ui by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Downloads", style = MaterialTheme.typography.h6)
        LazyColumn {
            items(ui.tasks) { task ->
                Card(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(task.url ?: "-")
                        LinearProgressIndicator(progress = task.progress ?: 0f, modifier = Modifier.fillMaxWidth().height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(task.status ?: "-")
                            Row {
                                if (task.outputUrl != null) {
                                    Button(onClick = { onOpenDownload(task.outputUrl!!) }) { Text("Open") }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.downloadOutput(task) }) { Text("Download") }
                            }
                        }
                    }
                }
            }
        }
    }
}
