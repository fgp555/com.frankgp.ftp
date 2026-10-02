package com.frankgp.ftp.data

import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FtpServerUiState(
    val isRunning: Boolean = false,
    val ipAddress: String = "127.0.0.1",
    val port: Int = 2121,
    val networkName: String = "Desconocida",
    val rootDirectory: String = Environment.getExternalStorageDirectory().absolutePath,
    val username: String = "ftpuser",
    val password: String = "ftppass",
    val allowAnonymous: Boolean = true,
    val connectedClients: List<String> = emptyList(),
    val activeTransfers: List<TransferInfo> = emptyList(),
    val errorMessage: String? = null
)

data class TransferInfo(
    val fileName: String,
    val clientIp: String,
    val direction: TransferDirection,
    val startTime: Long = System.currentTimeMillis(),
    val bytesTransferred: Long = 0L
)

enum class TransferDirection {
    UPLOAD, DOWNLOAD
}

object FtpServerManager {
    private val _uiState = MutableStateFlow(FtpServerUiState())
    val uiState: StateFlow<FtpServerUiState> = _uiState.asStateFlow()

    fun updateState(update: (FtpServerUiState) -> FtpServerUiState) {
        _uiState.update(update)
    }
}
