## Configuración de Usuarios y Grupos en Flowable IDM

Para ejecutar el proceso correctamente y permitir la autenticación en el portal web y aplicaciones auxiliares, se deben dar de alta los siguientes grupos, usuarios y permisos en **Flowable IDM** (`http://localhost:8080/flowable-ui` -> módulo **IDM**, ingresando con el usuario por defecto `admin` / `test`).

### 1. Grupos
Ingresar a la pestaña **Groups** -> **Create group** y crear los siguientes dos grupos (sin tildes en los IDs):

| ID del Grupo | Nombre | Descripción |
| :--- | :--- | :--- |
| `Administracion` | Administracion | Equipo administrativo encargado de gestión, presupuestos y seguimiento. |
| `Docentes` | Docentes | Equipo docente encargado de evaluación académica, dictado e informes. |

---

### 2. Usuarios
Ingresar a la pestaña **Users** -> **Create user**, crear los siguientes 5 usuarios y asignarlos a su grupo correspondiente:

| ID de Usuario | Nombre | Apellido | Contraseña | Grupo Asignado | Rol en el Proceso |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `jefe_admin` | Jefe | Administrativo | `1234` | `Administracion` | Jefe del equipo de Administración |
| `admin2` | Administrativo | Dos | `1234` | `Administracion` | Integrante del equipo de Administración |
| `jefe_docente` | Jefe | Docente | `1234` | `Docentes` | Jefe del equipo Docente |
| `docente2` | Docente | Dos | `1234` | `Docentes` | Integrante del equipo Docente |
| `docente3` | Docente | Tres | `1234` | `Docentes` | Integrante del equipo Docente |

---

### 3. Permisos (Privileges)
Para que los usuarios puedan iniciar sesión en Flowable UI y gestionar sus bandejas de tareas:

1. Ingresar a la pestaña **Privileges** dentro de **IDM**.
2. Seleccionar el privilegio **Access the workflow application** (o *Access the task application* según la versión).
3. Agregar a los 5 usuarios creados (`jefe_admin`, `admin2`, `jefe_docente`, `docente2`, `docente3`) o vincular directamente los grupos `Administracion` y `Docentes`.
