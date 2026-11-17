## Configuración de la Base de Datos (PostgreSQL + pgAdmin 4)

El sistema utiliza una base de datos independiente de Flowable llamada `institucion_cursos` dentro de la misma instancia de PostgreSQL.

### 1. Conexión en pgAdmin 4
Registrar un servidor (**Servers -> Register -> Server...**) con los siguientes parámetros:
* **Host name/address:** `flowable-postgres`
* **Port:** `5432`
* **Maintenance database:** `flowable`
* **Username:** `flowable`
* **Password:** `flow10`

### 2. Creación de la base de datos de negocio
1. Dentro del servidor conectado, hacer clic derecho sobre **Databases -> Create -> Database...**.
2. Completar **Database:** `institucion_cursos` y **Owner:** `flowable`.
3. Guardar los cambios.

### 3. Creación de tablas
1. Hacer clic derecho sobre la base `institucion_cursos` -> **Query Tool**.
2. Abrir o pegar el contenido del archivo `database/schema.sql` de este repositorio.
3. Ejecutar el script (`F5`) y verificar en **Schemas -> public -> Tables** que se hayan creado las tablas `empresas` y `cursos`.