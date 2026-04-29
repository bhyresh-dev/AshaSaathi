package com.ashasaathi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ashasaathi.service.model.ModelDownloadService
import com.ashasaathi.ui.navigation.RootNavGraph
import com.ashasaathi.ui.theme.AshaSaathiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var modelDownloadService: ModelDownloadService

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AshaSaathiTheme {
                RootNavGraph(modelDownloadService = modelDownloadService)
            }
        }
    }
}
