package com.frankgp.ftp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.frankgp.ftp.data.FtpServerManager
import com.frankgp.ftp.service.FtpServerService
import com.frankgp.ftp.ui.MainScreen
import com.frankgp.ftp.ui.theme.FGPFTPTheme
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshIpAddress()
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermissions()
        registerNetworkCallback()

        setContent {
            FGPFTPTheme {
                val uiState by FtpServerManager.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    refreshIpAddress()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onToggleServer = { start, port, rootDir, username, password, allowAnon ->
                            if (start) {
                                startFtpServerService(port, rootDir, username, password, allowAnon)
                            } else {
                                stopFtpServerService()
                            }
                        },
                        onRequestManageStorage = {
                            requestManageStoragePermission()
                        },
                        onRefreshIp = {
                            refreshIpAddress()
                        },
                        onRequestLocationPermission = {
                            requestLocationPermission()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshIpAddress()
    }

    private fun refreshIpAddress() {
        val ip = getPhoneIpAddress()
        val netName = getNetworkInfoName()
        FtpServerManager.updateState { it.copy(ipAddress = ip, networkName = netName) }
    }

    private fun getNetworkInfoName(): String {
        try {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return "Sin conexión"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Sin conexión"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    var ssid = ""
                    try {
                        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                        val info = wifiManager.connectionInfo
                        if (info != null) {
                            val rawSsid = info.ssid
                            if (rawSsid != null && rawSsid != "<unknown ssid>") {
                                ssid = rawSsid.removeSurrounding("\"")
                            }
                        }
                    } catch (_: Exception) {}

                    if (ssid.isEmpty() || ssid == "<unknown ssid>") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                val transportInfo = capabilities.transportInfo
                                if (transportInfo is WifiInfo) {
                                    val s = transportInfo.ssid
                                    if (s != null && s != "<unknown ssid>") {
                                        ssid = s.removeSurrounding("\"")
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (ssid.isNotEmpty() && ssid != "<unknown ssid>") {
                        return "Wi-Fi: $ssid"
                    } else {
                        val hasLocationPermission = ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        return if (!hasLocationPermission) {
                            "Wi-Fi (Toca para dar permiso de ubicación)"
                        } else {
                            "Wi-Fi (Activa Ubicación GPS)"
                        }
                    }
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    return "Datos Móviles"
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    return "Ethernet"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Desconocida"
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    refreshIpAddress()
                }
                override fun onLost(network: Network) {
                    refreshIpAddress()
                }
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    refreshIpAddress()
                }
            }
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (networkCallback != null) {
                val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(networkCallback!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun startFtpServerService(port: Int, rootDir: String, username: String, pass: String, allowAnon: Boolean) {
        val intent = Intent(this, FtpServerService::class.java).apply {
            action = FtpServerService.ACTION_START
            putExtra(FtpServerService.EXTRA_PORT, port)
            putExtra(FtpServerService.EXTRA_ROOT_DIR, rootDir)
            putExtra(FtpServerService.EXTRA_USERNAME, username)
            putExtra(FtpServerService.EXTRA_PASSWORD, pass)
            putExtra(FtpServerService.EXTRA_ANONYMOUS, allowAnon)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopFtpServerService() {
        val intent = Intent(this, FtpServerService::class.java).apply {
            action = FtpServerService.ACTION_STOP
        }
        startService(intent)
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
}
