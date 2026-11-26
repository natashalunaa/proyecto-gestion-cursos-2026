# Simulador Empresa

Aplicación Java independiente, con su propio `pom.xml`, que representa a la empresa externa y permite simular sus comunicaciones con la institución de capacitación mediante la API REST de Flowable.

## 1. Estructura

| Archivo o directorio | Descripción |
|---|---|
| `pom.xml` | Configuración de Maven y dependencias de la aplicación. |
| `src/main/java/simulador/App.java` | Clase principal. Presenta el menú de opciones y permite interactuar con el proceso desde la consola. |
| `src/main/java/simulador/FlowableClient.java` | Gestiona la comunicación con la API REST de Flowable, consulta las ejecuciones disponibles y envía los mensajes correspondientes. |

## 2. Funcionalidades

La aplicación permite simular las siguientes acciones de una empresa:

| Opción | Acción | Mensaje BPMN |
|---|---|---|
| 1 | Aceptar propuesta | `Msg_acepta` |
| 2 | Rechazar propuesta | `Msg_rechaza` |
| 3 | Solicitar modificaciones | `Msg_mod` |
| 4 | Enviar nómina de participantes | `Msg_nom` |
| 5 | Cancelar curso | `Msg_cancel` |
| 0 | Salir de la aplicación | — |

El simulador consulta las ejecuciones que se encuentran esperando el mensaje seleccionado, permitiendo elegir la instancia del proceso sobre la cual se desea realizar la acción.

## 3. Comunicación con Flowable

La comunicación se realiza mediante la API REST de Flowable.

La aplicación identifica las ejecuciones que poseen una suscripción al mensaje correspondiente y envía dicho mensaje a la ejecución seleccionada.

De esta manera, el proceso BPMN puede continuar su ejecución según la respuesta de la empresa, sin necesidad de intervenir manualmente desde Flowable.

## 4. Habilitación del acceso de la empresa

Como parte de la integración entre el simulador, el proceso BPMN y el sitio web, se incorporó un mecanismo de habilitación de acceso para las empresas.

Cuando una empresa acepta una propuesta:

1. El simulador envía el mensaje `Msg_acepta`.
2. Flowable continúa el proceso y ejecuta la actividad **Habilitar acceso de la empresa**.
3. En Java `CreateCourseOnAcceptance` se comunica con el backend.
4. El backend verifica si la empresa dispone de una cuenta de acceso.
5. Si no posee una cuenta, se generan las credenciales correspondientes.
6. Se almacenan los datos de acceso en PostgreSQL, protegiendo la contraseña mediante BCrypt.
7. El backend realiza un envío simulado de las credenciales mediante la consola.

El formato del envío simulado es el siguiente:

```text
========================================
ENVÍO SIMULADO DE CREDENCIALES

Empresa: [Razón social]
Usuario: empresa_[id]
Contraseña: [Contraseña generada]
Portal: http://localhost:8081/
========================================
```

Si la empresa ya dispone de una cuenta habilitada, se reutiliza la existente, evitando generar nuevas credenciales cada vez que acepta una propuesta.

Con estas credenciales, la empresa puede ingresar al sitio web y consultar sus cursos y sus respectivos estados.

## 5. Decisiones de implementación

La utilización de mensajes REST permite respetar la comunicación entre participantes definida en el modelo BPMN.

La generación de credenciales es responsabilidad del backend y no del simulador, manteniendo centralizada la gestión de usuarios y el acceso al sistema. Se optó por simular el envío de credenciales mediante la consola, evitando la necesidad de configurar un servidor de correo electrónico para las pruebas del proyecto.

## 6. Tecnologías utilizadas

- Java.
- Maven.
- API REST de Flowable.
- HTTP.
- JSON.
- BPMN 2.0.

## 7. Ejecución

Con Flowable y el backend del proyecto en funcionamiento, ingresar desde la raíz del repositorio al directorio del simulador:

```powershell
cd simuladorEmpresa
```

Ejecutar la aplicación:

```powershell
mvn compile exec:java "-Dexec.mainClass=simulador.App"
```

Se mostrará el siguiente menú:

```text
1. Aceptar propuesta
2. Rechazar propuesta
3. Solicitar modificaciones
4. Enviar nómina de participantes
5. Cancelar curso
0. Salir
```

Seleccionar la opción correspondiente y la ejecución del proceso sobre la cual se desea realizar la acción.

## 8. Conclusión

El simulador permite reproducir las interacciones de una empresa externa con la institución de capacitación y comprobar el comportamiento del proceso BPMN ante diferentes respuestas.

Su integración con Flowable y el backend permite probar tanto la comunicación mediante mensajes como la habilitación del acceso de las empresas al sitio web, sin depender de sistemas externos de comunicación.
