package com.example.lncrawlclient

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lncrawlclient.ui.MainApp
import com.example.lncrawlclient.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val pickDir = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            Prefs.outputUri = it.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {\n        // Try to register HMS Push as fallback for EMUI/Huawei devices\n        if (HuaweiPushHelper.isHuaweiDevice() && HuaweiPushHelper.isHmsAvailable(this)) {\n            HuaweiPushHelper.registerPush(this)\n        }
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            Surface(color = MaterialTheme.colors.background) {
                MainApp(vm,
                    onPickDirectory = { pickDir.launch(null) },
                    onOpenDownload = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                )
            }
        }
    }
}
