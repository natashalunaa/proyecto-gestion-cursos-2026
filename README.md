# Sistema de Gestión de Cursos de Capacitación

**Materia:** Gestión y Tecnologías de Procesos de Negocio  
**Universidad Nacional de Río Cuarto**  
**Integrantes:** Jennifer Pari y Natasha Luna.

## Descripción

El proyecto consiste en la implementación de un sistema de gestión de cursos de capacitación para empresas, desde la recepción de la solicitud hasta la finalización del curso.

El proceso de negocio se encuentra modelado mediante BPMN 2.0 y se ejecuta en Flowable 6.8.0. El sistema cuenta con un sitio web, aplicaciones auxiliares, consultas históricas y un módulo de minería de procesos.

## Requisitos previos

- Git.
- Docker Desktop con Docker Compose.
- JDK 17 o superior (se recomienda JDK 21).
- Apache Maven.
- Python 3 y Graphviz para minería de procesos.

Los comandos indicados a continuación corresponden a Windows PowerShell.

## Instalación y configuración

### 1. Clonar el repositorio

```powershell
git clone https://github.com/natashalunaa/proyecto-gestion-cursos-2026.git
cd proyecto-gestion-cursos-2026
```

### 2. Iniciar los contenedores

Con Docker Desktop en ejecución, ejecutar desde la raíz del repositorio:

```powershell
docker compose up -d --build
```

Verificar el estado de los contenedores:

```powershell
docker compose ps
```

### 3. Configurar las bases de datos

Se utilizan dos bases de datos PostgreSQL:

- `flowable`: almacena los datos del motor de procesos.
- `institucion_cursos`: almacena los datos propios de la institución.

Seguir las instrucciones de [configuración de la base de datos](./CursoFlowable/docs/base-de-datos.md).

### 4. Configurar Flowable

Crear los usuarios, grupos y privilegios según la [guía de configuración de Flowable IDM](./CursoFlowable/docs/flowable-idm.md).

Desplegar el modelo BPMN y la decisión DMN del proyecto.

- ID del proceso: `process-proyecto-curso`.
- ID de la decisión: `evaluarViabilidad`.

Las clases Java utilizadas por el modelo BPMN deben estar disponibles en el motor Flowable.

### 5. Iniciar el sitio web

Desde la raíz del repositorio ejecutar:

```powershell
.\iniciar-proyecto.ps1
```

La terminal debe permanecer abierta durante la ejecución del sitio web.

## Acceso a los servicios

| Servicio | Dirección |
|---|---|
| Sitio web | http://localhost:8081 |
| Flowable UI | http://localhost:8080/flowable-ui |
| Flowable IDM | http://localhost:8080/flowable-ui/idm |
| pgAdmin | http://localhost:5050 |
| PostgreSQL | localhost:5433 |

## Aplicaciones auxiliares

Los siguientes comandos deben ejecutarse en terminales independientes, partiendo de la raíz del repositorio.

### Aplicación de preparación de materiales

Permite consultar, reclamar y completar tareas de preparación de materiales mediante la API Java de Flowable.

```powershell
cd aplicacionMateriales
mvn compile exec:java "-Dexec.mainClass=materiales.App"
```

[Documentación de la aplicación](./aplicacionMateriales/README.md).

### Simulador de empresa

Permite simular las comunicaciones de las empresas mediante la API REST de Flowable.

```powershell
cd simuladorEmpresa
mvn compile exec:java "-Dexec.mainClass=simulador.App"
```

[Documentación del simulador](./simuladorEmpresa/README.md).

### Consultas históricas

Permite consultar información de las instancias y actividades ejecutadas mediante HistoryService.

```powershell
cd consultasHistoricas
mvn compile exec:java "-Dexec.mainClass=historico.App"
```

## Minería de procesos

Se utiliza Python y PM4Py para exportar y analizar el historial de ejecución correspondiente a la etapa de negociación.

```powershell
cd mineriaProcesos
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python exportar_history.py
python analizar.py
```

Para realizar las consultas históricas y el análisis de minería de procesos es necesario contar con instancias previamente ejecutadas.

[Documentación de minería de procesos](./mineriaProcesos/README.md).

## Estructura del repositorio

| Directorio | Contenido |
|---|---|
| `CursoFlowable/` | Backend, sitio web, modelos de procesos, componentes Java y scripts SQL. |
| `aplicacionMateriales/` | Aplicación de preparación de materiales. |
| `simuladorEmpresa/` | Simulador de comunicaciones de empresas. |
| `consultasHistoricas/` | Consultas al historial de Flowable. |
| `mineriaProcesos/` | Scripts de exportación y análisis de eventos. |
| `docker/` | Configuración de la imagen Docker. |

## Detener los servicios

Para detener los contenedores:

```powershell
docker compose stop
```

Para iniciarlos nuevamente:

```powershell
docker compose start
```

No eliminar los volúmenes de PostgreSQL si se desea conservar los usuarios, las instancias y el historial de ejecución.
