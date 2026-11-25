import csv
from pathlib import Path
from datetime import datetime
from getpass import getpass

import requests
import psycopg2


# Configuración
BASE_URL = "http://localhost:8080/flowable-ui/process-api"

PROCESS_KEY = "process-proyecto-curso"

ACTIVIDADES = {
    "EsperarRegistro",
    "CalcularActivos",
    "sid-C279BA20-BEB4-4850-B878-FAC01445ABE0",
    "sid-D4A460A6-6451-48B2-BD5D-F76FDEC3887F",
    "Propuesta",
    "sid-E8F551D6-A49F-44CF-82E5-69F2EF55DBB3",
    "sid-AE90424C-2654-4C64-909C-BD085A34654C",
    "sid-BCDCCC94-87B1-4A9C-AA42-860B0CB20553",
    "sid-2EDEE778-CCB8-4D86-B10B-F0CA6DE47860",
    "sid-B1226B89-72C5-427D-85EF-783C6E99CC5D",
    "Revisar",
    "sid-1F2A59D8-79CE-4E8B-88FF-9FDF0B2CF6EB",
    "sid-DA4178CD-28E4-4E74-A085-B6D7BE829D62",
    "Activity_1eg7zcl",
}

ARCHIVO_SALIDA = Path(__file__).resolve().parent / "eventos.csv"

def consultar_todos(session, recurso, filtros):
    """Recupera todas las pagginas de una consulta REST."""
    inicio = 0

    while True:
        respuesta = session.get(
            f"{BASE_URL}/{recurso}",
            params={
                **filtros,
                "start": inicio,
                "size": 100,
            },
            timeout=30,
        )
        respuesta.raise_for_status()
        pagina = respuesta.json()
        registros = pagina["data"]

        if not registros:
            break

        yield from registros
        inicio += len(registros)

        if inicio >= pagina["total"]:
            break


def fecha_iso(valor):
    return datetime.fromisoformat(valor.replace("Z", "+00:00"))


def obtener_actividades_descartadas():
    clave_db = getpass("Contrasenia de PostgreSQL: ")

    conexion = psycopg2.connect(
        host="127.0.0.1",
        port=5433,
        dbname="flowable",
        user="flowable",
        password=clave_db,
    )

    try:
        with conexion.cursor() as cursor:
            cursor.execute(
                """
                SELECT id_
                FROM act_hi_actinst
                WHERE proc_def_id_ LIKE %s
                  AND delete_reason_ IS NOT NULL
                """,
                (PROCESS_KEY + ":%",),
            )
            return {fila[0] for fila in cursor.fetchall()}
    finally:
        conexion.close()


def main():
    usuario = input("Usuario de Flowable: ")
    clave = getpass("Contrasenia: ")

    descartadas = obtener_actividades_descartadas()
    eventos = []

    with requests.Session() as session:
        session.auth = (usuario, clave)

        instancias = consultar_todos(
            session,
            "history/historic-process-instances",
            {
                "processDefinitionKey": PROCESS_KEY,
                "finished": "true",
            },
        )

        for instancia in instancias:
            # Excluir instancias eliminadas administrativamente.
            if instancia.get("deleteReason"):
                continue

            actividades = consultar_todos(
                session,
                "history/historic-activity-instances",
                {"processInstanceId": instancia["id"]},
            )

            for actividad in actividades:
                if actividad["id"] in descartadas:
                    continue

                # No exportar elementos sin fecha de finalizacion.
                if not actividad.get("endTime"):
                    continue

                if (
                    ACTIVIDADES
                    and actividad["activityId"] not in ACTIVIDADES
                ):
                    continue

                eventos.append({
                    "instancia": instancia["id"],
                    "definicion": actividad["processDefinitionId"],
                    "evento_id": actividad["id"],
                    "actividad_id": actividad["activityId"],
                    "actividad": (
                        actividad.get("activityName")
                        or actividad["activityId"]
                    ),
                    "tipo": actividad["activityType"],
                    "inicio": actividad["startTime"],
                    "fecha_hora": actividad["endTime"],
                    "recurso": actividad.get("assignee") or "",
                })

    # Ordenar por instancia y fecha de finalizacion.
    # El ID da un orden estable si coinciden las marcas temporales.
    eventos.sort(
        key=lambda evento: (
            evento["instancia"],
            fecha_iso(evento["fecha_hora"]),
            fecha_iso(evento["inicio"]),
            evento["evento_id"],
        )
    )

    columnas = [
        "instancia", "definicion", "evento_id",
        "actividad_id", "actividad", "tipo",
        "inicio", "fecha_hora", "recurso",
    ]

    with open(
        ARCHIVO_SALIDA, "w", newline="", encoding="utf-8"
    ) as archivo:
        escritor = csv.DictWriter(archivo, fieldnames=columnas)
        escritor.writeheader()
        escritor.writerows(eventos)

    cantidad_instancias = len({
        evento["instancia"] for evento in eventos
    })

    print(
        f"Generado {ARCHIVO_SALIDA}: "
        f"{len(eventos)} eventos de {cantidad_instancias} instancias."
    )


if __name__ == "__main__":
    main()