# Iniciar backend de Gestion de Cursos
# Guardar este archivo en la raiz de proyecto-gestion-cursos-2026.
$ErrorActionPreference = 'Stop'

$backend = Join-Path $PSScriptRoot 'CursoFlowable'
if (-not (Test-Path (Join-Path $backend 'pom.xml'))) {
    Write-Error 'No se encontro CursoFlowable\pom.xml. Coloca este script en la raiz del proyecto.'
    exit 1
}

# Variables solo para esta sesion de PowerShell y el proceso Java hijo.
$env:DB_PASSWORD = 'flowable'
$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=America/Argentina/Buenos_Aires -Dsimular.entrega.credenciales=true'

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error 'No se encontro Maven (mvn) en PATH.'
    exit 1
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    $postgresRunning = docker inspect --format '{{.State.Running}}' flowable-postgres 2>$null
    if ($LASTEXITCODE -ne 0 -or $postgresRunning -ne 'true') {
        Write-Warning 'El contenedor flowable-postgres no esta en ejecucion. Inicia tu Docker Compose antes de continuar.'
        exit 1
    }
} else {
    Write-Warning 'No se encontro Docker en PATH; no se pudo comprobar PostgreSQL.'
}

Write-Host 'Iniciando backend de Gestion de Cursos en el puerto 8081...'
Push-Location $backend
try {
    & mvn spring-boot:run
} finally {
    Pop-Location
}
