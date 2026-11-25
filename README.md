# Sistema de Gestión de Cursos de Capacitación

Proyecto de modelado, automatización y ejecución de procesos de negocio para gestionar la solicitud, evaluación, preparación y desarrollo de cursos de capacitación destinados a empresas.

**Materia:** Gestión y Tecnologías de Procesos de Negocio  
**Universidad Nacional de Río Cuarto**  
**Integrantes:** Jennifer Pari y Natasha Luna.

## 1. Descripción del proyecto

El sistema permite gestionar el ciclo de vida de una solicitud de capacitación, desde su recepción hasta la finalización del curso y la entrega del informe de resultados.

El proceso de negocio se encuentra modelado mediante BPMN 2.0 y utiliza Flowable como motor de ejecución.

El proyecto integra un sitio web, servicios desarrollados en Java, una base de datos PostgreSQL, aplicaciones auxiliares y herramientas de análisis de procesos.

## 2. Funcionalidades

### Gestión de solicitudes y propuestas

- Registro de solicitudes de capacitación mediante un formulario web.
- Identificación de empresas mediante CUIT.
- Evaluación automática de viabilidad mediante DMN.
- Elaboración y revisión de propuestas.
- Gestión de aceptación, rechazo y solicitud de modificaciones.
- Control de plazos y envío simulado de recordatorios.

### Preparación y desarrollo de cursos

- Recepción de la nómina de participantes.
- Confección de la lista definitiva.
- Preparación de materiales.
- Evaluación y gestión de recursos.
- Asignación automática de tareas administrativas.
- Selección del docente responsable.
- Desarrollo del curso.
- Elaboración del informe de resultados.

### Gestión de situaciones excepcionales

El modelo BPMN contempla:

- Cancelación durante la preparación del curso.
- Falta de disponibilidad de aulas.
- Compensación de reservas realizadas.
- Liberación de recursos.
- Cierre de solicitudes por falta de respuesta.

### Sitio web

- Formulario público de solicitud de cursos.
- Autenticación de empresas y personal de la institución.
- Panel de seguimiento para empresas.
- Panel de tareas administrativas.
- Panel de tareas docentes.
- Consulta del estado y avance de los cursos.

### Aplicaciones auxiliares y análisis

- Aplicación de escritorio para gestionar la preparación de materiales mediante la API Java de Flowable.
- Simulador de comunicaciones de empresas mediante la API REST.
- Consultas históricas mediante HistoryService.
- Exportación de registros de eventos.
- Minería de procesos mediante PM4Py.

## 3. Tecnologías utilizadas

| Tecnología | Utilización |
|---|---|
| BPMN 2.0 | Modelado del proceso de negocio. |
| DMN | Evaluación de reglas de negocio. |
| Flowable 6.8.0 | Motor de ejecución de procesos. |
| Java | Implementación de la lógica de negocio. |
| Groovy | Tareas de script dentro del BPMN. |
| Spring Boot | Backend y servicios REST. |
| Maven | Gestión de dependencias y compilación. |
| PostgreSQL 15 | Persistencia de datos. |
| pgAdmin | Administración de bases de datos. |
| Docker | Ejecución de los servicios. |
| HTML, CSS y JavaScript | Desarrollo del sitio web. |
| Java Swing | Aplicación auxiliar de materiales. |
| Python | Procesamiento de registros históricos. |
| PM4Py | Minería de procesos. |
| Graphviz | Visualización de modelos descubiertos. |
| Git y GitHub | Control de versiones y trabajo colaborativo. |

## 4. Arquitectura del sistema

El sistema se organiza en diferentes componentes que trabajan sobre un mismo proceso de negocio.

- **Flowable:** ejecuta el proceso BPMN, evalúa decisiones DMN, administra tareas y registra el historial de ejecución.
- **Spring Boot:** implementa los servicios necesarios para comunicar el sitio web con Flowable y PostgreSQL.
- **PostgreSQL:** almacena tanto la información del motor de procesos como los datos propios de las empresas y los cursos.
- **Sitio web:** proporciona las interfaces de interacción para empresas, administrativos y docentes.
- **Aplicaciones auxiliares:** permiten interactuar con Flowable mediante su API Java y su API REST.
- **Minería de procesos:** analiza los registros históricos para identificar los recorridos ejecutados.

## 5. Estructura del repositorio

```text
proyecto-gestion-cursos-2026/
│
├── CursoFlowable/
│   ├── database/
│   │   └── schema.sql
│   ├── docs/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   │           ├── processes/
│   │           └── static/
│   └── pom.xml
│
├── aplicacionMateriales/
│   ├── src/
│   ├── pom.xml
│   └── README.md
│
├── simuladorEmpresa/
│   ├── src/
│   ├── pom.xml
│   └── README.md
│
├── consultasHistoricas/
│   ├── src/
│   └── pom.xml
│
├── mineriaProcesos/
│   ├── exportar_history.py
│   ├── analizar.py
│   ├── requirements.txt
│   └── README.md
│
├── docker/
│   └── Dockerfile
│
├── docker-compose.yml
├── iniciar-proyecto.ps1
└── README.md
```

## 6. Requisitos previos

Para ejecutar el proyecto localmente se requiere:

- Git.
- Docker Desktop con Docker Compose.
- JDK 17 o superior. Se recomienda JDK 21.
- Apache Maven.
- Python 3, compatible con las dependencias del módulo de minería.
- Graphviz, para generar las representaciones gráficas.
- Un navegador web.

Los comandos de instalación y ejecución de esta guía están orientados a Windows PowerShell.

## 7. Instalación y configuración

### 7.1. Clonar el repositorio

```powershell
git clone https://github.com/natashalunaa/proyecto-gestion-cursos-2026.git

cd proyecto-gestion-cursos-2026
```

### 7.2. Iniciar los servicios

Con Docker Desktop en ejecución, desde la raíz del repositorio ejecutar:

```powershell
docker compose up -d --build
```

Comprobar el estado de los contenedores:

```powershell
docker compose ps
```

Docker Compose permite iniciar los servicios correspondientes a Flowable, PostgreSQL y pgAdmin.

### 7.3. Configurar la base de datos

El proyecto utiliza dos bases de datos:

| Base de datos | Finalidad |
|---|---|
| `flowable` | Almacenar las instancias, tareas, usuarios e historial del motor. |
| `institucion_cursos` | Almacenar los datos propios de las empresas y los cursos. |

La estructura de la base de datos de negocio se encuentra definida en:

```text
CursoFlowable/database/schema.sql
```

Para realizar la configuración, consultar la siguiente guía:

[Configuración de la base de datos PostgreSQL](./CursoFlowable/docs/base-de-datos.md)

### 7.4. Configurar usuarios y grupos

Mediante Flowable IDM se deben configurar los usuarios, grupos y privilegios necesarios para la ejecución del proceso.

El modelo utiliza responsables administrativos, docentes y jefes de área.

Entre los elementos utilizados se encuentran:

| Usuario o grupo | Responsabilidad |
|---|---|
| `jefe_admin` | Jefe del área administrativa. |
| `jefe_docente` | Jefe del equipo docente. |
| `Administracion` | Grupo de administrativos. |
| `Docentes` | Grupo de docentes. |

Los usuarios que interactúan con Flowable mediante su API REST deben contar con los privilegios correspondientes.

[Configuración de usuarios y grupos de Flowable IDM](./CursoFlowable/docs/flowable-idm.md)

### 7.5. Desplegar el proceso BPMN y la decisión DMN

El modelo definitivo utiliza el identificador de proceso:

```text
process-proyecto-curso
```

La decisión DMN utilizada para evaluar la viabilidad se identifica mediante:

```text
evaluarViabilidad
```

Antes de ejecutar el proceso, se debe comprobar que:

- El modelo BPMN definitivo se encuentre desplegado.
- La decisión DMN esté disponible.
- Las clases Java referenciadas por el BPMN estén compiladas y disponibles para el motor Flowable.
- Los usuarios y grupos coincidan con las asignaciones del modelo.
- Los mensajes y variables utilizados por las aplicaciones coincidan con los definidos en el BPMN.

### 7.6. Iniciar el backend

Desde la raíz del repositorio ejecutar:

```powershell
.\iniciar-proyecto.ps1
```

El script configura las variables de entorno necesarias, obtiene la configuración de los contenedores y ejecuta el backend mediante Maven.

La terminal debe permanecer abierta mientras se utiliza el sitio web.

## 8. Acceso a los servicios

| Servicio | Dirección |
|---|---|
| Sitio web | http://localhost:8081 |
| Flowable UI | http://localhost:8080/flowable-ui |
| Flowable Modeler | http://localhost:8080/flowable-ui/modeler |
| Flowable IDM | http://localhost:8080/flowable-ui/idm |
| API REST de procesos | http://localhost:8080/flowable-ui/process-api |
| pgAdmin | http://localhost:5050 |
| PostgreSQL | localhost:5433 |

Las credenciales y los parámetros de conexión deben coincidir con la configuración del entorno local.

## 9. Modelo del proceso de negocio

El proceso comprende las siguientes etapas:

1. Recepción de la solicitud.
2. Evaluación de viabilidad.
3. Elaboración y negociación de la propuesta.
4. Preparación del curso.
5. Confirmación y selección del docente.
6. Desarrollo de la capacitación.
7. Elaboración del informe de resultados.
8. Archivo, liberación de recursos y cierre.

Durante la preparación se ejecutan en paralelo las actividades de inscripción, preparación de materiales y gestión de recursos.

El modelo utiliza compuertas exclusivas, paralelas, inclusivas y basadas en eventos, además de temporizadores, eventos de mensaje, eventos de error y mecanismos de compensación.

### Evaluación de viabilidad

La decisión DMN establece el siguiente criterio:

| Cantidad de cursos activos | Resultado |
|---|---|
| Menor que 5 | VIABLE |
| Mayor o igual que 5 | NO_VIABLE |

### Resultados del proceso

La variable `resultado` permite clasificar las instancias finalizadas.

- `no_viable`
- `rechazado`
- `cerrado_por_falta_de_respuesta`
- `cancelado`
- `finalizado`

## 10. Ejecución de las aplicaciones auxiliares

Para ejecutar cada aplicación, abrir una terminal de PowerShell ubicada inicialmente en la raíz del repositorio.

### 10.1. Aplicación de preparación de materiales

Aplicación de escritorio desarrollada mediante Java Swing que utiliza la API Java de Flowable.

Permite consultar, reclamar y completar tareas de preparación de materiales.

```powershell
cd aplicacionMateriales

mvn compile exec:java "-Dexec.mainClass=materiales.App"
```

[Documentación de la aplicación de materiales](./aplicacionMateriales/README.md)

### 10.2. Simulador de comunicaciones de la empresa

Aplicación Java que permite simular las comunicaciones de la empresa mediante la API REST de Flowable.

Contempla las operaciones de aceptación, rechazo, solicitud de modificaciones, envío de nómina y cancelación.

```powershell
cd simuladorEmpresa

mvn compile exec:java "-Dexec.mainClass=simulador.App"
```

[Documentación del simulador de empresa](./simuladorEmpresa/README.md)

### 10.3. Consultas históricas

Aplicación Java que utiliza HistoryService para consultar las instancias y actividades registradas por Flowable.

Permite obtener información sobre:

- Tareas realizadas por los administrativos.
- Duración promedio de las ramas de preparación.
- Distribución de solicitudes según su resultado final.

```powershell
cd consultasHistoricas

mvn compile exec:java "-Dexec.mainClass=historico.App"
```

Para obtener resultados, deben existir instancias previamente ejecutadas en Flowable.

## 11. Minería de procesos

El módulo de minería de procesos permite analizar los registros históricos mediante Python y PM4Py.

### 11.1. Preparar el entorno

Desde la raíz del repositorio ejecutar:

```powershell
cd mineriaProcesos

python -m venv .venv

.\.venv\Scripts\Activate.ps1

python -m pip install -r requirements.txt
```

Comprobar que Graphviz esté instalado:

```powershell
dot -V
```

### 11.2. Exportar los registros históricos

```powershell
python exportar_history.py
```

Este script obtiene los registros históricos y genera un archivo CSV con los identificadores de las instancias, las actividades y sus marcas temporales.

### 11.3. Analizar los registros

```powershell
python analizar.py
```

El análisis permite identificar los recorridos más frecuentes y generar un modelo de proceso descubierto.

### Archivos generados

| Archivo | Contenido |
|---|---|
| `eventos.csv` | Registro de eventos exportados. |
| `recorridos.png` | Visualización de los recorridos. |
| `modelo_descubierto.png` | Representación gráfica del modelo descubierto. |
| `modelo_descubierto.bpmn` | Modelo BPMN obtenido mediante minería. |

Los resultados dependen de las instancias ejecutadas y de los filtros aplicados durante la exportación.

[Documentación de minería de procesos](./mineriaProcesos/README.md)

## 12. Pruebas del sistema

Para verificar el funcionamiento del proyecto se contemplan los siguientes escenarios:

- Solicitud viable.
- Solicitud no viable.
- Aceptación de propuesta.
- Rechazo de propuesta.
- Solicitud de modificaciones.
- Cierre por falta de respuesta.
- Preparación completa del curso.
- Cancelación con recursos reservados.
- Cancelación con reservas parciales.
- Falta de disponibilidad de aulas.
- Desarrollo y finalización normal del curso.
- Consulta de tareas mediante las aplicaciones auxiliares.
- Consultas históricas y minería de procesos.

Las pruebas de compensación deben comprobar que solamente se liberen los recursos cuyas reservas se hayan completado.

## 13. Configuración y seguridad

El proyecto utiliza variables de entorno para configurar la comunicación entre sus componentes.

Entre ellas se encuentran:

| Variable | Finalidad |
|---|---|
| `DB_PASSWORD` | Contraseña de conexión a PostgreSQL. |
| `FLOWABLE_INTEGRATION_SECRET` | Autorización de comunicaciones internas. |
| `SPRING_DATASOURCE_URL` | Dirección de la base de datos del backend. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos. |
| `SERVER_PORT` | Puerto del backend. |

Las credenciales utilizadas en el entorno académico son de desarrollo y deben reemplazarse para cualquier entorno de producción.

## 14. Detener los servicios

Para detener los contenedores sin eliminar los datos almacenados:

```powershell
docker compose stop
```

Para volver a iniciarlos:

```powershell
docker compose start
```

No se deben eliminar los volúmenes de PostgreSQL si se desea conservar las instancias, los usuarios y los registros históricos utilizados durante las pruebas.

## 15. Documentación

- [Configuración de la base de datos](./CursoFlowable/docs/base-de-datos.md)
- [Configuración de usuarios y grupos](./CursoFlowable/docs/flowable-idm.md)
- [Aplicación de preparación de materiales](./aplicacionMateriales/README.md)
- [Simulador de empresa](./simuladorEmpresa/README.md)
- [Minería de procesos](./mineriaProcesos/README.md)

El proyecto se desarrolla con fines académicos para la materia Gestión y Tecnologías de Procesos de Negocio de la Universidad Nacional de Río Cuarto.
