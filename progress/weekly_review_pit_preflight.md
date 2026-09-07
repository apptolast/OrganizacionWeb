# Preparación de PIT19

PREPARADO, sin campaña iniciada. Root ejecuta init integrado; se requiere su resultado y autorización posterior. No se ejecutó Gradle durante esta preparación.

Corte 62c676e869a3009ba5c0f9f138f98c3f3ca5eede, captura UTC 2026-09-07T14:30:00Z. `weekly_review_pit_before_hashes.json` conserva 388 entradas SHA256: todo backend versionado (fuentes, pruebas, recursos, SQL, wrapper y configuración), motor/configuración/schema del arnés, lanzadores y dispatcher con su test. SHA del manifiesto: 1FA459E2E381F0C224623693849954399A49EB9B1CC49800CC94B9936603E67D. No hay fuentes/recursos nuevos sin versionar bajo backend/src o backend/gradle. Se excluyen salidas de build y cachés; los hashes no pretenden inventariar cobertura.

`weekly_review_pit_scope_check.json` registra los siete patrones y nueve fuentes seleccionadas. Coinciden con los nueve archivos Java productivos del diff respecto de main anterior a19, a5d1586 (8ad75a). Los patrones con comodín incluyen records internos. Las interfaces y excepciones no se presuponen mutables: el inventario final depende de PIT.

El target del arnés es `weekly_review-backend`, que despacha a pitest con `-PmutationScope=weekly_review`. Todos los JUnit `com.apptolast.organization.*` son candidatos; cuatro workers, umbral80, timeout constante heredado15000ms, filtros heredados y XML/HTML separados en `backend/build/reports/pitest-weekly-review`. Soporte ya aprobado e integrado; dry-run previo24d0a9 no ejecutó tests ni mutantes.

Antes de iniciar se compararán estas388 entradas y se comprobará que no haya nuevas entradas relevantes. Si cambia alguna, se informará y preservará esta captura sin reemplazarla silenciosamente. Al finalizar se preservarán log, EXIT, XML/HTML originales, inventario por identidad clase/método/línea/mutador/índice y hashes de artefactos y entradas posteriores. KILLED, SURVIVED, NO_COVERAGE, TIMED_OUT y errores se mantendrán separados; no se iniciará replay automáticamente.

## Trazabilidad y límites previos

`progress/tdd_weekly_review_backend.md` contiene mapa de@s1–24 a aplicación/dominio/PG/HTTP y ciclos; `weekly_review_backend_final_bundle.json` conserva el corte funcional aprobado de91 pruebas. El nuevo soporte tiene51 Node verdes; ninguno de esos conteos equivale a los100 ejemplos Gherkin.

La evidencia de@s19 overflow es compuesta (acumulador puro exacto y traducción503 bajo transacción real), sin materializar millones de filas. @s21 incluye writer real posterior al snapshot y anterior al Clock. @s6 conserva la precisión de frontera acreditada con Java y604zonas, sin fabricar la fila imposible. @s24 reinicio de proceso y separación del outbox quedan a integración/E2E; la lectura de Java por sí sola no acredita ese ciclo. Cliente/UI/UX no se deducen de PIT backend. No se encuentra un bloqueo nuevo de trazabilidad para preparar la campaña; faltan los gates que coordina root.
