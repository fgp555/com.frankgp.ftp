```sh
ftp -n 192.168.18.22

# Dentro del prompt:
open 192.168.18.22 2121
user anonymous
# [Press Enter sin ingresar contraseña]
ls
quit

```

# powershell

```sh
# Desactiva firewall completamente
netsh advfirewall set allprofiles state off

# Ver que se desactivó:
Get-NetFirewallProfile | Select-Object Name, Enabled
# Debería mostrar: Enabled = False para todos


# Después de probar Directory Opus, vuelve a activar:
netsh advfirewall set allprofiles state on

# Verificar:
Get-NetFirewallProfile | Select-Object Name, Enabled
# Debería mostrar: Enabled = True para todos


# Agregar regla para Directory Opus (salida)
netsh advfirewall firewall add rule name="Directory Opus FTP Out" `
  dir=out action=allow program="C:\Program Files\GPSoftware\Directory Opus\dopus.exe" `
  protocol=tcp remoteport=2121

# Agregar regla para Directory Opus (entrada)
netsh advfirewall firewall add rule name="Directory Opus FTP In" `
  dir=in action=allow program="C:\Program Files\GPSoftware\Directory Opus\dopus.exe" `
  protocol=tcp localport=2121

# Ver todas las reglas:
Get-NetFirewallRule -DisplayName "*Directory*" | Select-Object DisplayName, Enabled, Direction

# Ver reglas por puerto:
Get-NetFirewallRule | Where-Object {$_.LocalPort -eq 2121 -or $_.RemotePort -eq 2121}

```
