package com.frankgp.ftp.service

import com.frankgp.ftp.data.FtpServerManager
import com.frankgp.ftp.data.TransferDirection
import com.frankgp.ftp.data.TransferInfo
import org.apache.ftpserver.ftplet.DefaultFtplet
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.ftplet.FtpSession
import org.apache.ftpserver.ftplet.FtpletResult

class FtpEventTracker : DefaultFtplet() {

    private val activeTransfersMap = mutableMapOf<String, TransferInfo>()

    override fun onConnect(session: FtpSession): FtpletResult {
        val clientIp = session.clientAddress?.address?.hostAddress ?: "Unknown"
        FtpServerManager.updateState { state ->
            val clients = (state.connectedClients + clientIp).distinct()
            state.copy(connectedClients = clients)
        }
        return FtpletResult.DEFAULT
    }

    override fun onDisconnect(session: FtpSession): FtpletResult {
        val clientIp = session.clientAddress?.address?.hostAddress ?: "Unknown"
        FtpServerManager.updateState { state ->
            val clients = state.connectedClients.toMutableList().apply {
                remove(clientIp)
            }
            state.copy(connectedClients = clients)
        }
        activeTransfersMap.remove(session.toString())
        updateTransfersState()
        return FtpletResult.DEFAULT
    }

    override fun onDownloadStart(session: FtpSession, request: FtpRequest): FtpletResult {
        val clientIp = session.clientAddress?.address?.hostAddress ?: "Unknown"
        val fileName = request.argument ?: "unknown"
        val transfer = TransferInfo(
            fileName = fileName,
            clientIp = clientIp,
            direction = TransferDirection.DOWNLOAD,
            startTime = System.currentTimeMillis()
        )
        activeTransfersMap[session.toString() + "_down"] = transfer
        updateTransfersState()
        return FtpletResult.DEFAULT
    }

    override fun onUploadStart(session: FtpSession, request: FtpRequest): FtpletResult {
        val clientIp = session.clientAddress?.address?.hostAddress ?: "Unknown"
        val fileName = request.argument ?: "unknown"
        val transfer = TransferInfo(
            fileName = fileName,
            clientIp = clientIp,
            direction = TransferDirection.UPLOAD,
            startTime = System.currentTimeMillis()
        )
        activeTransfersMap[session.toString() + "_up"] = transfer
        updateTransfersState()
        return FtpletResult.DEFAULT
    }

    override fun onDownloadEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        activeTransfersMap.remove(session.toString() + "_down")
        updateTransfersState()
        return FtpletResult.DEFAULT
    }

    override fun onUploadEnd(session: FtpSession, request: FtpRequest): FtpletResult {
        activeTransfersMap.remove(session.toString() + "_up")
        updateTransfersState()
        return FtpletResult.DEFAULT
    }

    private fun updateTransfersState() {
        val transfers = activeTransfersMap.values.toList()
        FtpServerManager.updateState { it.copy(activeTransfers = transfers) }
    }
}
