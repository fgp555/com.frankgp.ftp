package com.frankgp.ftp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frankgp.ftp.data.FtpServerManager
import com.frankgp.ftp.data.FtpServerUiState
import com.frankgp.ftp.data.TransferDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    uiState: FtpServerUiState,
    onToggleServer: (Boolean, Int, String, String, String, Boolean) -> Unit,
    onRequestManageStorage: () -> Unit,
    onRefreshIp: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    val context = LocalContext.current

    var portTextW by remember(uiState.port) { mutableStateOf(uiState.port.toString()) }
    var rootDirW by remember(uiState.rootDirectory) { mutableStateOf(uiState.rootDirectory) }
    var usernameW by remember(uiState.username) { mutableStateOf(uiState.username) }
    var passwordW by remember(uiState.password) { mutableStateOf(uiState.password) }
    var allowAnonW by remember(uiState.allowAnonymous) { mutableStateOf(uiState.allowAnonymous) }

    var showFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Servidor FTP para Android") },
                actions = {
                    TextButton(onClick = { onRefreshIp() }) {
                        Text(
                            "Actualizar IP",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Switch & Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (uiState.isRunning) "Servidor Activo" else "Servidor Detenido",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Red: ${uiState.networkName}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "IP: ${uiState.ipAddress} | Puerto: ${uiState.port}",
                                fontSize = 14.sp,
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("FTP Address", "${uiState.ipAddress}:${uiState.port}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copiado: ${uiState.ipAddress}:${uiState.port}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Switch(
                            checked = uiState.isRunning,
                            onCheckedChange = { checked ->
                                val port = portTextW.toIntOrNull() ?: 2121
                                onToggleServer(checked, port, rootDirW, usernameW, passwordW, allowAnonW)
                            }
                        )
                    }
                }
            }

            // Folder Selection Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Carpeta Raíz Compartida", fontWeight = FontWeight.Bold)
                    Text(text = rootDirW, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showFolderDialog = true },
                            enabled = !uiState.isRunning
                        ) {
                            Text("Cambiar Carpeta")
                        }
                        OutlinedButton(
                            onClick = { onRequestManageStorage() }
                        ) {
                            Text("Permisos de Archivos")
                        }
                    }
                }
            }

            // Credentials Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Credenciales de Acceso", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = portTextW,
                        onValueChange = { portTextW = it },
                        label = { Text("Puerto") },
                        enabled = !uiState.isRunning,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Permitir Acceso Anónimo")
                        Switch(
                            checked = allowAnonW,
                            onCheckedChange = { allowAnonW = it },
                            enabled = !uiState.isRunning
                        )
                    }

                    if (!allowAnonW) {
                        OutlinedTextField(
                            value = usernameW,
                            onValueChange = { usernameW = it },
                            label = { Text("Usuario") },
                            enabled = !uiState.isRunning,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = passwordW,
                            onValueChange = { passwordW = it },
                            label = { Text("Contraseña") },
                            enabled = !uiState.isRunning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Status / Connected Clients & Transfers Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Estado del Servidor y Clientes", fontWeight = FontWeight.Bold)
                    
                    Text(text = "Clientes conectados: ${uiState.connectedClients.size}", fontWeight = FontWeight.Medium)
                    if (uiState.connectedClients.isNotEmpty()) {
                        uiState.connectedClients.forEach { ip ->
                            Text(text = " • $ip", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Transferencias activas: ${uiState.activeTransfers.size}", fontWeight = FontWeight.Medium)
                    if (uiState.activeTransfers.isNotEmpty()) {
                        uiState.activeTransfers.forEach { transfer ->
                            val dirText = if (transfer.direction == TransferDirection.DOWNLOAD) "Descargando" else "Subiendo"
                            Text(
                                text = " • $dirText: ${transfer.fileName} (${transfer.clientIp})",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Text(text = "Ninguna transferencia en curso", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = {
                FtpServerManager.updateState { it.copy(errorMessage = null) }
            },
            title = { Text("Error al Iniciar Servidor") },
            text = { Text(uiState.errorMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        FtpServerManager.updateState { it.copy(errorMessage = null) }
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (showFolderDialog) {
        val defaultDirs = listOf(
            Environment.getExternalStorageDirectory().absolutePath,
            Environment.getExternalStorageDirectory().absolutePath + "/Download",
            Environment.getExternalStorageDirectory().absolutePath + "/Documents",
            Environment.getExternalStorageDirectory().absolutePath + "/DCIM"
        )

        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Seleccionar Carpeta Raíz") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rootDirW,
                        onValueChange = { rootDirW = it },
                        label = { Text("Ruta personalizada") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("O elige una carpeta común:", fontWeight = FontWeight.Bold)
                    defaultDirs.forEach { dir ->
                        TextButton(
                            onClick = {
                                rootDirW = dir
                                showFolderDialog = false
                            }
                        ) {
                            Text(dir)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }
}
