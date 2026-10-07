package com.example.webview

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.components.LoadingOverlay
import com.example.ui.components.OfflineScreen
import com.example.ui.components.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VvtNavy
import com.example.ui.theme.VvtOrange
import com.example.util.NetworkMonitor

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var fileChooserHandler: FileChooserHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        networkMonitor = NetworkMonitor(applicationContext)
        fileChooserHandler = FileChooserHandler(this)

        val viewModel = MainViewModel(networkMonitor)

        setContent {
            MyApplicationTheme {
                MainScreen(
                    activity = this,
                    viewModel = viewModel,
                    fileChooserHandler = fileChooserHandler,
                    networkMonitor = networkMonitor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    activity: Activity,
    viewModel: MainViewModel,
    fileChooserHandler: FileChooserHandler,
    networkMonitor: NetworkMonitor
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ActivityResultLauncher for HTML file chooser upload (gallery, camera, docs)
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileChooserHandler.onActivityResult(result.resultCode, result.data)
    }

    // WebViewManager setup (remembered across recompositions)
    val webViewManager = remember {
        VvtWebViewManager(
            context = context,
            onPageStarted = { url ->
                viewModel.onPageStarted(url)
            },
            onPageFinished = { url ->
                viewModel.onPageFinished(url, false)
            },
            onProgressChanged = { progress ->
                viewModel.onProgressChanged(progress)
            },
            onErrorReceived = { errorDesc ->
                viewModel.onErrorReceived(errorDesc)
            },
            onOpenFileChooser = { filePathCallback, fileChooserParams ->
                fileChooserHandler.startFileChooser(
                    fileChooserLauncher = fileChooserLauncher,
                    callback = filePathCallback,
                    params = fileChooserParams
                )
            }
        )
    }

    // Update canGoBack on page finished
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            viewModel.onPageFinished(uiState.currentUrl, webViewManager.canGoBack())
        }
    }

    // Clean up WebView on disposal
    DisposableEffect(Unit) {
        onDispose {
            webViewManager.destroy()
        }
    }

    // Back button handling
    BackHandler(enabled = !uiState.isSplashActive) {
        if (webViewManager.canGoBack()) {
            webViewManager.goBack()
        } else {
            viewModel.setShowExitDialog(true)
        }
    }

    // Exit confirmation dialog
    if (uiState.showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                viewModel.setShowExitDialog(false)
                activity.finish()
            },
            onDismiss = {
                viewModel.setShowExitDialog(false)
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VvtNavy
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (uiState.isSplashActive) {
                // Splash Screen
                SplashScreen(
                    onSplashFinished = {
                        viewModel.onSplashFinished()
                        // Initial load
                        if (uiState.isOnline) {
                            webViewManager.loadUrl(uiState.initialUrl)
                        }
                    }
                )
            } else {
                // Main content: Offline Screen or WebView
                if (!uiState.isOnline) {
                    OfflineScreen(
                        title = stringResource(id = R.string.no_internet_title),
                        description = stringResource(id = R.string.no_internet_desc),
                        buttonText = stringResource(id = R.string.retry_button),
                        onRetry = {
                            viewModel.onRetryClicked()
                            if (networkMonitor.isOnline()) {
                                webViewManager.loadUrl(uiState.initialUrl)
                            }
                        }
                    )
                } else if (uiState.hasLoadError) {
                    OfflineScreen(
                        title = stringResource(id = R.string.error_loading_title),
                        description = stringResource(id = R.string.error_loading_desc),
                        buttonText = stringResource(id = R.string.try_again_button),
                        onRetry = {
                            viewModel.onRetryClicked()
                            webViewManager.reload()
                        }
                    )
                } else {
                    // Full screen Pull-to-refresh & WebView
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Smooth progress indicator at the top
                        if (uiState.isLoading && uiState.progress in 1..99) {
                            LinearProgressIndicator(
                                progress = { uiState.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .testTag("webview_progress_bar"),
                                color = VvtOrange,
                                trackColor = Color.Transparent
                            )
                        }

                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = {
                                viewModel.onRefreshTriggered()
                                webViewManager.reload()
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("webview_pull_refresh")
                        ) {
                            AndroidView(
                                factory = {
                                    webViewManager.webView
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("main_webview")
                            )
                        }
                    }

                    // First-time or heavy-page loading overlay
                    LoadingOverlay(
                        visible = uiState.isLoading && uiState.progress < 70 && !uiState.isRefreshing
                    )
                }
            }
        }
    }
}
