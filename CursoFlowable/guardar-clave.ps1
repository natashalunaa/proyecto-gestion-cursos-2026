# Ejecutar una sola vez, o cuando cambie la contraseña de Flowable.
$ErrorActionPreference = 'Stop'
$archivoClave = Join-Path $PSScriptRoot '.flowable-password.dpapi'
$clave = Read-Host 'Contraseña del usuario admin de Flowable' -AsSecureString
$clave | ConvertFrom-SecureString | Set-Content -Path $archivoClave -Encoding UTF8
Write-Host 'Contraseña guardada cifrada para tu usuario de Windows.'
