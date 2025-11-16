package com.example.lncrawlclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lncrawlclient.viewmodel.MainViewModel

@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val logs = viewModel.latestLogs
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Logs", style = androidx.compose.material.MaterialTheme.typography.h6)
        Text(logs ?: "No logs yet", modifier = Modifier.padding(top = 8.dp))
    }
}
