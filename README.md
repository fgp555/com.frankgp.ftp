# FGP FTP - Android FTP Server

An Android application built with **Jetpack Compose** and **Apache FtpServer** that turns your Android phone into a fully functional FTP server running in the background. It allows multiple simultaneous connections from desktop clients like FileZilla, Directory Opus, or Windows Explorer.

---

## Features

1. **Main Dashboard**:
   - ON/OFF Switch to start and stop the FTP server instantly.
   - Live display of the phone's local IP address and port (`2121`).
   - Automatic detection and display of the connected Wi-Fi network name (SSID) or connection type.
   - "Update IP" action button to refresh network information in real-time.
   - Clickable IP address to copy connection strings directly to the clipboard.

2. **Shared Root Directory**:
   - Choose which folder on your phone to share files from (defaults to internal storage root `/storage/emulated/0` or common folders like `/Download`).

3. **Live Status Monitoring**:
   - View currently connected client IP addresses.
   - Track active file transfers (uploads and downloads) in real-time.

4. **Security & Credentials**:
   - Optional Username and Password authentication.
   - Support for Anonymous guest access.

5. **Background & Concurrency**:
   - Runs as a Foreground Service with a persistent notification, ensuring the server stays active even when the app is minimized.
   - Supports multiple simultaneous client connections using Apache MINA / Apache FtpServer.

---

## Required Permissions

- `INTERNET`, `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE` (for networking and IP resolution).
- `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE` (for file sharing and storage access).
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` (for background server execution).
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (required by Android OS to read the connected Wi-Fi SSID name).

---

## How to Connect from Windows (FileZilla / Directory Opus)

1. Open **FGP FTP** on your Android device.
2. Select your shared root folder and configure credentials (or enable anonymous access).
3. Turn on the server using the **ON/OFF Switch**.
4. Note your phone's IP address (e.g., `192.168.18.22`) and port (`2121`).
5. In **FileZilla** or **Directory Opus**:
   - **Host:** `192.168.18.22`
   - **Port:** `2121`
   - **Username / Password:** (Leave blank for Anonymous, or enter your configured credentials).
6. Click **Quickconnect** / **OK** to browse, upload, and download files directly to/from your phone!

---

## License

This project is licensed under the MIT License.
