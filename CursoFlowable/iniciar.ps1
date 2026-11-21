# Colocar este archivo en la raíz del proyecto CursoFlowable.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$archivoClave = Join-Path $PSScriptRoot '.flowable-password.dpapi'
if (-not (Test-Path $archivoClave)) {
    Write-Host 'Primero ejecutá .\guardar-clave.ps1 para guardar la contraseña.'
    exit 1
}
$segura = Get-Content -Path $archivoClave -Raw | ConvertTo-SecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
try {
    $env:FLOWABLE_USER = 'admin'
    $env:FLOWABLE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
try {
    mvn spring-boot:run
} finally {
    Remove-Item Env:FLOWABLE_PASSWORD -ErrorAction SilentlyContinue
}
