package com.example.lncrawlclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lncrawlclient.Prefs
import com.example.lncrawlclient.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onPickDirectory: ()->Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(value = Prefs.baseUrl, onValueChange = { Prefs.baseUrl = it }, label = { Text("API base URL") })
        Text("Selected folder: ${Prefs.outputUri ?: "(none)"}", modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onPickDirectory, modifier = Modifier.padding(top = 8.dp)) { Text("Choose download folder") }
    }
}
