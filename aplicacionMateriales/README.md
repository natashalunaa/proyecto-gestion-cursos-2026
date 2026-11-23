# Aplicación de preparación de materiales

Aplicación auxiliar del proyecto **Gestión de Cursos**, desarrollada en Java 17 con interfaz gráfica Swing e integración mediante la API Java de Flowable 6.8.0.

## Funcionalidades

- Identificar al usuario administrativo mediante su identificador.
- Consultar tareas de **Preparar materiales** asignadas al usuario.
- Consultar tareas de **Preparar materiales** disponibles para reclamar.
- Reclamar tareas mediante `TaskService.claim()`.
- Completar tareas asignadas mediante `TaskService.complete()`.
- Actualizar la tabla después de reclamar o completar una tarea.

## Tecnologías

- Java 17
- Maven
- Java Swing
- Flowable Engine 6.8.0
- PostgreSQL

## Requisitos

- Java 17 o superior y Maven instalados.
- Flowable y PostgreSQL en ejecución.
- Acceso a la base de datos PostgreSQL utilizada por Flowable.

La configuración actual utiliza:

- Servidor: `127.0.0.1`
- Puerto: `5433`
- Base de datos: `flowable`
- Usuario: `flowable`

La contraseña de la base de datos está configurada actualmente en `FlowableService.java`. Debe coincidir con la configuración local de PostgreSQL.

## Ejecución

Desde la carpeta `aplicacionMateriales`:

```powershell
mvn clean compile exec:java "-Dexec.mainClass=materiales.App"
```

## Uso

1. Ingresar el identificador del administrativo.
2. Presionar **Consultar tareas**.
3. Seleccionar una tarea disponible y presionar **Reclamar tarea**.
4. Seleccionar una tarea asignada y presionar **Completar tarea**.
5. Confirmar la operación.

## Pruebas realizadas

Se desplegó el proceso `PruebaMateriales` para verificar la integración con Flowable.

Se comprobó que la aplicación permite consultar una tarea disponible, reclamarla y completarla.

El proceso utilizado para las pruebas se encuentra en `pruebas/prueba-materiales.bpmn20.xml`.

## Pendiente de validación

- Probar la aplicación con una instancia del BPMN principal del proyecto.
- Verificar los usuarios y grupos administrativos reales.
- Confirmar el comportamiento de la aplicación al trabajar con las tareas del proceso completo.
