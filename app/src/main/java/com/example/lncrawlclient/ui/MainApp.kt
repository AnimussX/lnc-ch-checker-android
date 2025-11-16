package com.example.lncrawlclient.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.lncrawlclient.viewmodel.MainViewModel

@Composable
fun MainApp(viewModel: MainViewModel, onPickDirectory: ()->Unit, onOpenDownload: (String)->Unit) {
    val tabs = listOf("Queue", "Downloads", "Logs", "Settings")
    var selected = 0
    Scaffold(topBar = { TopAppBar(title = { Text("LNCrawl Client") }) }) {
        Column {
            TabRow(selectedTabIndex = selected) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selected==index, onClick = { selected = index }) {
                        Text(title, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            when (selected) {
                0 -> QueueScreen(viewModel)
                1 -> DownloadsScreen(viewModel, onOpenDownload)
                2 -> LogsScreen(viewModel)
                3 -> SettingsScreen(viewModel, onPickDirectory)
            }
        }
    }
}
