package com.frankgp.ftp.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.frankgp.ftp.MainActivity
import com.frankgp.ftp.data.FtpServerManager
import org.apache.ftpserver.DataConnectionConfigurationFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

class FtpServerService : Service() {

    private var ftpServer: FtpServer? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_ROOT_DIR = "EXTRA_ROOT_DIR"
        const val EXTRA_USERNAME = "EXTRA_USERNAME"
        const val EXTRA_PASSWORD = "EXTRA_PASSWORD"
        const val EXTRA_ANONYMOUS = "EXTRA_ANONYMOUS"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "FtpServerChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, 2121)
                val rootDir = intent.getStringExtra(EXTRA_ROOT_DIR) ?: Environment.getExternalStorageDirectory().absolutePath
                val username = intent.getStringExtra(EXTRA_USERNAME) ?: "ftpuser"
                val password = intent.getStringExtra(EXTRA_PASSWORD) ?: "ftppass"
                val allowAnon = intent.getBooleanExtra(EXTRA_ANONYMOUS, true)
                startFtpServer(port, rootDir, username, password, allowAnon)
            }
            ACTION_STOP -> {
                stopFtpServer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startFtpServer(port: Int, rootDirPath: String, username: String, pass: String, allowAnon: Boolean) {
        if (ftpServer != null) return

        try {
            createNotificationChannel()
            val notification = createNotification("Servidor FTP activo en puerto $port")
            startForeground(NOTIFICATION_ID, notification)

            val rootDir = File(rootDirPath)
            if (!rootDir.exists()) {
                rootDir.mkdirs()
            }

            val serverFactory = FtpServerFactory()
            val localIp = getPhoneIpAddress()
            val listenerFactory = ListenerFactory().apply {
                this.port = port
                if (localIp != "127.0.0.1") {
                    val dataConfigFactory = DataConnectionConfigurationFactory().apply {
                        setPassiveExternalAddress(localIp)
                    }
                    dataConnectionConfiguration = dataConfigFactory.createDataConnectionConfiguration()
                }
            }
            serverFactory.addListener("default", listenerFactory.createListener())

            val userManager = serverFactory.userManager
            
            val user = BaseUser().apply {
                name = username
                password = pass
                homeDirectory = rootDir.absolutePath
                authorities = listOf<Authority>(WritePermission())
            }
            userManager.save(user)

            if (allowAnon) {
                val anonUser = BaseUser().apply {
                    name = "anonymous"
                    homeDirectory = rootDir.absolutePath
                    authorities = listOf<Authority>(WritePermission())
                }
                userManager.save(anonUser)
            }

            serverFactory.ftplets["tracker"] = FtpEventTracker()

            ftpServer = serverFactory.createServer()
            ftpServer?.start()

            FtpServerManager.updateState {
                it.copy(
                    isRunning = true,
                    port = port,
                    rootDirectory = rootDirPath,
                    username = username,
                    password = pass,
                    allowAnonymous = allowAnon,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = if (port < 1024) {
                "Error en puerto $port: Los puertos menores a 1024 requieren permisos de root en Android. Usa el puerto 2121."
            } else {
                "Error al iniciar servidor: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
            stopFtpServerWithError(errorMsg)
        }
    }

    private fun stopFtpServer() {
        try {
            ftpServer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ftpServer = null
        FtpServerManager.updateState {
            it.copy(
                isRunning = false,
                connectedClients = emptyList(),
                activeTransfers = emptyList()
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopFtpServerWithError(errorMsg: String) {
        try {
            ftpServer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ftpServer = null
        FtpServerManager.updateState {
            it.copy(
                isRunning = false,
                connectedClients = emptyList(),
                activeTransfers = emptyList(),
                errorMessage = errorMsg
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servidor FTP Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el servidor FTP corriendo en segundo plano"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Servidor FTP")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_menu_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun getPhoneIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress
                        if (hostAddress != null && !hostAddress.startsWith("127.")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    override fun onDestroy() {
        stopFtpServer()
        super.onDestroy()
    }
}
