# Curso Flowable

Backend Spring Boot + BPMN + servicios + base de datos. Ejecuta la lógica del negocio.

## 1. Estructura

Módulo principal del sistema de gestión de cursos de capacitación. Integra el backend desarrollado en Java con Spring Boot, el motor de procesos Flowable, las reglas de negocio DMN, la base de datos PostgreSQL y la interfaz web.

| Archivo o directorio | Descripción |
|---|---|
| `pom.xml` | Configuración de Maven y dependencias del proyecto. |
| `database/` | Scripts SQL para la estructura de la base de datos. |
| `docs/` | Documentación técnica de la base de datos y Flowable IDM. |
| `src/main/java/` | Código Java del backend, servicios, controladores, repositorios y listeners. |
| `src/main/resources/processes/` | Modelos BPMN del proceso de gestión de cursos. |
| `src/main/resources/dmn/` | Tablas de decisión para la evaluación de viabilidad. |
| `src/main/resources/static/` | Interfaz web del sistema. |
| `src/test/` | Pruebas de componentes Java. |

## 2. Backend

El backend permite integrar la interfaz web, la base de datos y el motor de procesos Flowable.

El código Java se organiza en los siguientes componentes:

- **Controladores:** reciben las solicitudes realizadas desde la interfaz web.
- **Servicios:** implementan la lógica de negocio y la comunicación con Flowable.
- **Repositorios:** permiten consultar y actualizar la información almacenada en PostgreSQL.
- **DTO:** representan los datos intercambiados entre los componentes del sistema.
- **Listeners:** ejecutan acciones asociadas a determinados eventos del proceso.
- **Delegados Java:** permiten ejecutar lógica específica durante el proceso BPMN.

## 3. Modelo BPMN

El modelo BPMN representa el ciclo de vida de una solicitud de capacitación, desde su recepción hasta la finalización o cancelación del curso.

Comprende las siguientes etapas:

- Recepción de la solicitud.
- Evaluación de viabilidad.
- Elaboración y negociación de la propuesta.
- Preparación del curso.
- Gestión y reserva de recursos.
- Asignación del docente.
- Desarrollo del curso.
- Elaboración del informe de resultados.
- Cierre del proceso.

El modelo utiliza tareas de usuario, tareas de servicio, tareas de script, eventos, compuertas, subprocesos y mecanismos de compensación.

## 4. Reglas de negocio DMN

La tabla de decisión `Evaluar viabilidad.dmn` permite automatizar la evaluación de las solicitudes de capacitación.

La utilización de DMN permite separar las reglas de negocio del flujo principal del proceso, facilitando su mantenimiento y modificación.

## 5. Base de datos

Se utiliza PostgreSQL para almacenar la información del sistema.

Se distinguen dos bases de datos:

- `flowable`: almacena las definiciones, instancias, tareas, variables, usuarios y registros históricos del motor de procesos.
- `institucion_cursos`: almacena la información propia del negocio, como los cursos y las empresas.

Los scripts de definición de la estructura de la base de datos se encuentran en el directorio `database/`.

## 6. Interfaz web

La interfaz web permite interactuar con el proceso de gestión de cursos según el rol del usuario.

Sus principales funcionalidades comprenden la presentación de solicitudes, la gestión administrativa, el seguimiento de cursos y la interacción con las tareas del proceso.

Los archivos HTML, CSS y demás recursos de la interfaz se encuentran en `src/main/resources/static/`.

## 7. Tecnologías utilizadas

- Java.
- Spring Boot.
- Maven.
- Flowable 6.8.0.
- BPMN 2.0.
- DMN.
- PostgreSQL.
- HTML, CSS y JavaScript.
- Docker y Docker Compose.

## 8. Ejecución

Desde la raíz del repositorio, iniciar los servicios de Docker:

```powershell
docker compose up -d --build
```

A continuación, ejecutar el script de inicio del proyecto:

```powershell
.\iniciar-proyecto.ps1
```

Una vez iniciados los servicios, se puede acceder a:

| Servicio | Dirección |
|---|---|
| Sitio web | http://localhost:8081 |
| Flowable UI | http://localhost:8080/flowable-ui |
| pgAdmin | http://localhost:5050 |

Para la configuración inicial de la infraestructura y la base de datos, consultar el README principal del repositorio.

## 9. Integración con otros módulos

El módulo principal se complementa con:

- `aplicacionMateriales`: aplicación auxiliar para la preparación de materiales.
- `simuladorEmpresa`: simulación de las comunicaciones de las empresas con el proceso.
- `consultasHistoricas`: recuperación y análisis de información histórica mediante la API Java de Flowable.
- `mineriaProcesos`: extracción y análisis de eventos mediante técnicas de minería de procesos.

Estos componentes permiten complementar la ejecución, interacción y evaluación del proceso de negocio.
