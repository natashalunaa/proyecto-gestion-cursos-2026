from pathlib import Path
import pandas as pd
import pm4py

# Leer el CSV. Mantener los identificadores como texto.
CARPETA = Path(__file__).resolve().parent
df = pd.read_csv(CARPETA / "eventos.csv", dtype=str)

if df.empty:
    print("No hay eventos históricos para analizar.")
    print("Primero deben ejecutarse instancias de process-proyecto-curso.")
    raise SystemExit(0)

# Conservar las actividades relevantes de la negociación.
TIPOS_ANALIZADOS = {
    "userTask",
    "serviceTask",
    "scriptTask",
    "sendTask",
    "receiveTask",
    "businessRuleTask",
    "intermediateCatchEvent",
}

df = df[df["tipo"].isin(TIPOS_ANALIZADOS)].copy()

# Convertir fechas y conservar el orden del CSV ante empates.
df["fecha_hora"] = pd.to_datetime(df["fecha_hora"], utc=True, format="mixed")
df = df.sort_values(
    ["instancia", "fecha_hora"], kind="stable"
)

# Normalizar nombres mediante los identificadores BPMN.
NOMBRES_ACTIVIDADES = {
    "Aceptar": "Aceptaci\u00f3n",
    "Plazo": "5 d\u00edas",
}

df["actividad"] = df["actividad_id"].map(
    NOMBRES_ACTIVIDADES
).fillna(df["actividad"])

# Adaptar las columnas al formato utilizado por PM4Py.
log = pm4py.format_dataframe(
    df,
    case_id="instancia",
    activity_key="actividad",
    timestamp_key="fecha_hora",
)

# 1. Recorridos o variantes mÃ¡s frecuentes.
variantes = pm4py.get_variants_as_tuples(log)
total = log["case:concept:name"].nunique()

print(f"\nInstancias analizadas: {total}")
print(f"Recorridos diferentes: {len(variantes)}")

print("\nLos tres recorridos mas frecuentes:")
for recorrido, cantidad in sorted(
    variantes.items(),
    key=lambda elemento: elemento[1],
    reverse=True,
)[:3]:
    print(f"\n{cantidad} casos ({100 * cantidad / total:.1f} %)")
    print(" -> ".join(recorrido))

# 2. Grafo de sucesiÃ³n directa:
# cada flecha indica cuÃ¡ntas veces una tarea siguiÃ³ a otra.
dfg, inicios, finales = pm4py.discover_dfg(log)
pm4py.save_vis_dfg(
    dfg, inicios, finales, str(CARPETA / "recorridos.png")
)

# 3. Modelo BPMN descubierto a partir de las ejecuciones.
modelo = pm4py.discover_bpmn_inductive(log)
pm4py.save_vis_bpmn(modelo, str(CARPETA / "modelo_descubierto.png"))
pm4py.write_bpmn(modelo, str(CARPETA / "modelo_descubierto.bpmn"))

print("\nArchivos generados:")
print("- recorridos.png")
print("- modelo_descubierto.png")
print("- modelo_descubierto.bpmn")