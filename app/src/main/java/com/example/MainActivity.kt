package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.model.DesktopApp
import com.example.ui.MainViewModel
import com.example.ui.desktop.DesktopScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.views.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()

            MyApplicationTheme(darkTheme = settings.isDarkMode) {
                DesktopScreen(
                    viewModel = viewModel,
                    windowContents = { app ->
                        when (app) {
                            DesktopApp.DASHBOARD -> DashboardView(viewModel)
                            DesktopApp.DEVICE_MANAGER -> DeviceManagerView(viewModel)
                            DesktopApp.ADB_FASTBOOT -> AdbFastbootView(viewModel)
                            DesktopApp.FILE_EXPLORER -> FileExplorerView(viewModel)
                            DesktopApp.APK_MANAGER -> ApkManagerView(viewModel)
                            DesktopApp.REMOTE_DESKTOP -> RemoteDesktopView(viewModel)
                            DesktopApp.AI_ASSISTANT -> AiAssistantView(viewModel)
                            DesktopApp.MEDIA_PLAYER -> MediaSuiteView(viewModel)
                            DesktopApp.UTILITIES -> UtilitiesView(viewModel)
                            DesktopApp.SETTINGS -> SettingsView(viewModel)
                        }
                    }
                )
            }
        }
    }
}
