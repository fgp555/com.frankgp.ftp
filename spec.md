# Software Design Document (SDD) / App Specification Template
# Project: Android Embedded FTP Server (FGP FTP)

---

## 1. Executive Summary
This specification defines the architecture, components, and design patterns for an Android application that embeds a network server (such as Apache FtpServer) running as a Foreground Service, managed through a Jetpack Compose user interface. This pattern can be reused for other local server applications (e.g., HTTP, WebDAV, MQTT brokers on Android).

---

## 2. Architecture Overview

### 2.1 Technology Stack
- **UI Framework:** Jetpack Compose (Material 3, Edge-to-Edge).
- **Architecture Pattern:** Unidirectional Data Flow (UDF) using Kotlin `StateFlow` and Singleton State Managers (`Manager` object).
- **Background Execution:** Android `Service` running as a `FOREGROUND_SERVICE` with type `dataSync` (or appropriate service type).
- **Server Engine:** Apache FtpServer Core (`org.apache.ftpserver:ftpserver-core`) powered by Apache MINA.

### 2.2 Component Hierarchy
```
com.frankgp.ftp/
├── data/              # State models and Singleton State Managers
├── service/           # Background Foreground Services & Event Trackers (Ftplet)
├── ui/                # Jetpack Compose screens and themes
└── MainActivity.kt    # Entry point, permission coordination, network callback
```

---

## 3. Core Modules & Specifications

### 3.1 State Management (`FtpServerState.kt`)
- Encapsulates UI state in an immutable data class (`UiState`).
- Exposes a read-only `StateFlow<UiState>` to the UI layer.
- Provides a thread-safe update mechanism (`Manager.updateState { ... }`).

### 3.2 Background Server Service (`FtpServerService.kt`)
- **Lifecycle:** Started via `startForegroundService` to prevent OS termination.
- **Notification:** Maintains an ongoing persistent notification (`NotificationCompat.Builder`) linked back to `MainActivity`.
- **Server Factory:** Configures `FtpServerFactory`, `ListenerFactory`, and `UserManager`.
- **Passive Mode Configuration:** Crucial for desktop clients (FileZilla, Directory Opus) using PASV mode:
  ```kotlin
  val dataConfigFactory = DataConnectionConfigurationFactory().apply {
      setPassiveExternalAddress(localIp)
  }
  listenerFactory.dataConnectionConfiguration = dataConfigFactory.createDataConnectionConfiguration()
  ```

### 3.3 Event Tracking & Interception (`DefaultFtplet`)
- Subclasses `DefaultFtplet` to intercept server events:
  - `onConnect` / `onDisconnect`: Track connected client IP addresses.
  - `onDownloadStart` / `onUploadStart` / `onDownloadEnd` / `onUploadEnd`: Track active file transfers and volumes.

### 3.4 Network & Connectivity (`MainActivity.kt`)
- **IP Resolution:** Enumerates network interfaces (`NetworkInterface`) to find active IPv4 addresses.
- **Network Callback:** Registers `ConnectivityManager.NetworkCallback` to instantly update IP address and Wi-Fi SSID upon network changes.
- **SSID Detection:** Reads Wi-Fi network name using `WifiManager` / `NetworkCapabilities.transportInfo` (requires `ACCESS_FINE_LOCATION` permission).

---

## 4. Permissions & Storage Strategy

- **Storage:**
  - `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` (Legacy & Scoped storage).
  - `MANAGE_EXTERNAL_STORAGE` (`ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` for Android 11+ full folder access).
- **Network:**
  - `INTERNET`, `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`.
- **Foreground Service:**
  - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`.
- **Location (for Wi-Fi SSID):**
  - `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.

---

## 5. Reusable Guidelines for Other Server Apps
1. **Always wrap background servers in a Foreground Service** with valid notification channels to avoid Android background execution limits.
2. **Handle Passive Mode IPs explicitly** for network protocols that require data channels (FTP, WebDAV) to prevent client connection timeouts.
3. **Decouple server state from UI lifecycle** using singletons or ViewModels backed by `StateFlow`.
