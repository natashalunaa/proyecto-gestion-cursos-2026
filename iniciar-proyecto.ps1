# Iniciar backend de Gestion de Cursos
# Guardar este archivo en la raiz de proyecto-gestion-cursos-2026.
$ErrorActionPreference = 'Stop'

$backend = Join-Path $PSScriptRoot 'CursoFlowable'
if (-not (Test-Path (Join-Path $backend 'pom.xml'))) {
    Write-Error 'No se encontro CursoFlowable\pom.xml. Coloca este script en la raiz del proyecto.'
    exit 1
}

# Variables de entorno solo para esta sesion de PowerShell y el proceso Java hijo.
$env:DB_PASSWORD = 'flow10'
$env:SERVER_PORT = '8081'
$env:SPRING_LIQUIBASE_ENABLED = 'false'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/institucion_cursos'
$env:SPRING_DATASOURCE_USERNAME = 'flowable'
$env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
$config = docker inspect flowable-temporal | ConvertFrom-Json

$entrada = $config.Config.Env | Where-Object {
    $_ -like 'FLOWABLE_INTEGRATION_SECRET=*'
} | Select-Object -First 1

if (-not $entrada) {
    throw "El contenedor no tiene configurado el secreto de integracion."
}

$env:INTEGRACION_FLOWABLE_SECRET = $entrada.Substring('FLOWABLE_INTEGRATION_SECRET='.Length)

if ([string]::IsNullOrWhiteSpace($env:INTEGRACION_FLOWABLE_SECRET)) {
    throw "El secreto de integracion esta vacio."
}

Write-Host "Secreto de integracion configurado correctamente."
$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=UTC'

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
