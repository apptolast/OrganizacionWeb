# Revisión independiente de la revisión semanal

Estado: revisión funcional aprobada; cierre de la feature pendiente de mutación frontend, refuerzos backend y validación integrada final. No acredita despliegue.

El contrato conserva 34 escenarios y 100 ejemplos. La precisión del límite superior del calendario está razonada en `review_weekly_review_calendar_bound.md`; no reduce el rango público ni modifica el contrato de persistencia.

## Código y comportamiento

Se revisaron HTTP, aplicación, dominio, consulta PostgreSQL, cliente, navegación y vista. Los checkpoints integrados son `7ecf043`, `3020f30`, `aea633a` y `1301fa8`. La consulta establece el snapshot antes de leer el reloj, usa intervalos efectivos y separa plan vigente, trabajo registrado y presupuesto actual. No escribe hechos ni eventos. El cliente conserva microsegundos exactos y descarta respuestas obsoletas. La vista conserva la selección aplicada en la URL y distingue datos anteriores de una consulta completada.

La revisión detectó y exigió correcciones de duplicados HTTP, estados temporales futuros ocultos por otra fecha, intervalos futuros, áreas interactivas pequeñas y foco perdido al reintentar. El último ajuste `88674a4` captura el botón sólo cuando conserva el foco y reutiliza la recuperación existente; no roba el foco después de una navegación deliberada. Los hashes de fuente y prueba coinciden con la evidencia RED/GREEN de `tdd_weekly_review_frontend.md`.

## Evidencia contrastada

- Init integrado sobre `62c676e`: 2.286 pruebas Java, 1.965 frontend y 51 del arnés, todas verdes. Se preservan XML y manifiesto; los cambios posteriores necesitan su validación correspondiente.
- CI del checkpoint `1301fa8`: ejecuciones 34132468713 y 34132467354 completadas con éxito. No se extrapola a commits posteriores.
- E2E y UX integrados en `fd64920`: los nueve hashes del manifiesto coinciden. La preparación histórica traslada fechas de hechos creados mediante HTTP en la base aislada; no se presenta como creación de reservas pasadas mediante la API. Los oráculos exigen 1 h planificada y 30 min trabajados antes y después del reinicio, conservando hechos y retirando sólo eventos propios.
- Se revisaron las pruebas de selección, Back, recuperación, geometría y zoom, y la captura actual a 320 px. El informe UX conserva 124 medidas, 25 análisis axe sin infracciones, zoom nativo Chromium al 200 % y resultados separados por motor. Los límites de dispositivos físicos, lectores de pantalla y evaluación humana permanecen explícitos.
- PIT original: XML contrastado con 178 KILLED, 6 SURVIVED y 6 NO_COVERAGE, total 190, sin errores. Los 20 archivos del manifiesto de resultados coinciden. El 93,6842 % supera el umbral; los seis supervivientes tienen oráculos de frontera diferenciables y se han autorizado refuerzos. Los seis accessors internos sin consumidor permanecen en el denominador.

## Cierre pendiente

Revisar refuerzos y replay backend conservando la campaña original; ejecutar y revisar Stryker sobre las fuentes finales; completar los gates finales y CI antes de marcar `done`. El MVP publicado desde `a5d1586` y la preparación de infraestructura son entregas distintas: todavía no acreditan esta feature en producción.
