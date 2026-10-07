package com.example.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WebViewState(
    val initialUrl: String = "https://vvt.org.in/",
    val currentUrl: String = "https://vvt.org.in/",
    val isSplashActive: Boolean = true,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val hasLoadError: Boolean = false,
    val errorMessage: String? = null,
    val canGoBack: Boolean = false,
    val showExitDialog: Boolean = false,
    val progress: Int = 0
)

class MainViewModel(
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WebViewState(
            isOnline = networkMonitor.isOnline()
        )
    )
    val uiState: StateFlow<WebViewState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnlineFlow.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    fun onSplashFinished() {
        _uiState.update { it.copy(isSplashActive = false) }
    }

    fun onPageStarted(url: String?) {
        _uiState.update {
            it.copy(
                isLoading = true,
                currentUrl = url ?: it.currentUrl,
                hasLoadError = false,
                errorMessage = null
            )
        }
    }

    fun onPageFinished(url: String?, canGoBack: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                currentUrl = url ?: it.currentUrl,
                canGoBack = canGoBack
            )
        }
    }

    fun onProgressChanged(progress: Int) {
        _uiState.update {
            it.copy(
                progress = progress,
                isLoading = progress < 100
            )
        }
    }

    fun onErrorReceived(description: String?) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                hasLoadError = true,
                errorMessage = description
            )
        }
    }

    fun onRefreshTriggered() {
        _uiState.update { it.copy(isRefreshing = true, hasLoadError = false) }
    }

    fun onRetryClicked() {
        val online = networkMonitor.isOnline()
        _uiState.update {
            it.copy(
                isOnline = online,
                hasLoadError = false,
                errorMessage = null,
                isLoading = true
            )
        }
    }

    fun setShowExitDialog(show: Boolean) {
        _uiState.update { it.copy(showExitDialog = show) }
    }
}
