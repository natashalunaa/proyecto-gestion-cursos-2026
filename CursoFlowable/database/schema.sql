CREATE TABLE empresas (
    id SERIAL PRIMARY KEY,
    razon_social VARCHAR(150) NOT NULL,
    cuit VARCHAR(20) UNIQUE NOT NULL,
    email_contacto VARCHAR(150) NOT NULL,
    nombre_contacto VARCHAR(100) NOT NULL,
    telefono VARCHAR(50),
    usuario_acceso VARCHAR(100) UNIQUE,
    password_acceso VARCHAR(100)
);

CREATE TABLE cursos (
    id SERIAL PRIMARY KEY,
    empresa_id INTEGER REFERENCES empresas(id),
    process_instance_id VARCHAR(64) UNIQUE NOT NULL,
    tematica VARCHAR(200) NOT NULL,
    descripcion TEXT,
    cantidad_participantes INTEGER,
    fechas_preferidas VARCHAR(150),
    programa TEXT,
    presupuesto NUMERIC(12, 2),
    fecha_inicio_acordada TIMESTAMP,
    estado VARCHAR(50) NOT NULL DEFAULT 'en_evaluacion',
    informe_resultados_pdf TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);