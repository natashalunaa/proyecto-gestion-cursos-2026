# Iniciar backend de Gestion de Cursos
# Guardar este archivo en la raiz de proyecto-gestion-cursos-2026.

$ErrorActionPreference = 'Stop'

$backend = Join-Path $PSScriptRoot 'CursoFlowable'

if (-not (Test-Path (Join-Path $backend 'pom.xml'))) {
    Write-Error 'No se encontro CursoFlowable\pom.xml.'
    exit 1
}

# Comprobar Maven.

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error 'No se encontro Maven (mvn) en PATH.'
    exit 1
}

# Comprobar Docker.

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error 'No se encontro Docker en PATH.'
    exit 1
}

# Comprobar PostgreSQL.

$postgresRunning = docker inspect --format '{{.State.Running}}' flowable-postgres 2>$null

if ($LASTEXITCODE -ne 0 -or $postgresRunning -ne 'true') {
    throw 'El contenedor flowable-postgres no esta en ejecucion.'
}

# Obtener la configuracion de PostgreSQL.

$dbPassword = docker exec flowable-postgres printenv POSTGRES_PASSWORD

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($dbPassword)) {
    throw 'No se pudo obtener la configuracion de PostgreSQL.'
}

$env:DB_PASSWORD = $dbPassword.Trim()

$env:SERVER_PORT = '8081'
$env:SPRING_LIQUIBASE_ENABLED = 'false'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/institucion_cursos'
$env:SPRING_DATASOURCE_USERNAME = 'flowable'
$env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD

# Detectar el contenedor Flowable en ejecucion.

$contenedores = @(docker ps --format '{{.Names}}')

if ($LASTEXITCODE -ne 0) {
    throw 'No se pudieron consultar los contenedores Docker.'
}

if ($contenedores -contains 'flowable-persistente') {
    $contenedorFlowable = 'flowable-persistente'
} elseif ($contenedores -contains 'flowable-temporal') {
    $contenedorFlowable = 'flowable-temporal'
} else {
    throw 'No se encontro un contenedor Flowable en ejecucion.'
}

Write-Host "Contenedor Flowable detectado: $contenedorFlowable"

# Obtener automaticamente el secreto de integracion.

$secreto = docker exec $contenedorFlowable printenv FLOWABLE_INTEGRATION_SECRET

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($secreto)) {
    throw 'El contenedor no tiene configurado el secreto de integracion.'
}

# Compatibilidad con ambas configuraciones del backend.

$env:FLOWABLE_INTEGRATION_SECRET = $secreto.Trim()
$env:INTEGRACION_FLOWABLE_SECRET = $secreto.Trim()

Write-Host 'Secreto de integracion configurado correctamente.'

# Configuracion de Java y simulacion de credenciales.

$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=UTC -Dsimular.entrega.credenciales=true'

# Iniciar backend.

Write-Host 'Iniciando backend de Gestion de Cursos en el puerto 8081...'

Push-Location $backend

try {
    & mvn spring-boot:run
} finally {
    Pop-Location
}