# Revisión independiente — backend today

**Dictamen del corte: PENDING evidencia final; sin cambios funcionales requeridos detectados.** Revisión de diseño y cobertura @s1–15 del contrato a127747 (38 escenarios/105 casos), independiente de la autoría backend. No constituye aprobación de frontend, E2E, mutación ni cierre de feature. Sólo lectura; no ejecuté Gradle, init ni pruebas, conforme a la coordinación de autores activos.

## Diseño revisado

- `ReadToday.get` captura Clock.instant una sola vez y trunca a MICROS antes de construir la ventana; no compara createdAt con ese reloj. ZoneCatalog representa la capacidad del servidor, no vuelve a interpretar las zonas históricas de reservas.
- `TodayWindow.at/summarize` usa fecha local e inicios reales de días (DST23/25h), fallback UTC con fuente/motivo y capacidad null, intervalos semiabiertos y suma recortada sólo para capacidad. Actual y próximo tienen fronteras distintas; cierre es el máximo fin real. No se deriva trabajo realizado o estado completed.
- `TodayQueries` contiene preferencia y agenda en una frontera de snapshot. `PostgresTodayQueries.read` crea su TransactionTemplate local readOnly/REPEATABLE_READ; no modifica el template de edición ni usa locks para escribir. El callback calcula el día después del SELECT de preferencia dentro de esa transacción. El catch rodea execute completo, por tanto también alcanza TransactionException al cerrar, y propaga StorageUnavailableException segura.
- SQL: filtro `p.owner_id=?`, JOIN de tarea por `(project_id,task_id)`, intersección `start_at < dayEnd && end_at > dayStart`, ORDER BY inicio/ID y sin LIMIT ni exclusión completed. El resultado SQL ya excluye historial ajeno al día; no hay petición adicional por nombre. No encontré vía de escritura/outbox ni exposición de datos de otro propietario.
- `TodayController` obtiene owner de Principal, rechaza parámetros antes de leer y serializa exactamente objeto15 e item3 reutilizando BlockResponse9. 401 y no-store proceden de los filtros existentes, con cobertura HTTP. Errores StorageUnavailable conservan el formato seguro compartido.
- Compartidos: ApplicationConfiguration añade sólo wiring ReadToday; PostgresBlockStore.MAPPER cambia únicamente visibilidad a paquete. El mapper conserva instantes, offsets y texto de zona guardados. ApplicationWiringTest ejercita el bean por su puerto en contexto fresco; no atribuye resolución a cache de otro test.

## Contrato y oráculos inspeccionados

| Escenario | Test y evidencia observable |
| --- | --- |
| @s1 | TodayWindowTest.s1 y TodayApiTest.s1: objeto JSON entero exacto, capacidad vacía/nulls/no-store y filas sin cambios. |
| @s2 | TodayWindowTest.s2: reserva inferior/igual/superior y presupuesto0, sin trabajo ficticio. |
| @s3 | TodayWindowTest.s3 y TodayApiTest.s3_httpFallback: ausencia/zona retirada, capacidad null, bloque completed con zona histórica y fila intacta. |
| @s4 | TodayWindowTest.s4 y TodayApiTest.s4_sqlLoadsOnlyPositiveIntersections: contactos excluidos/cruces recortados, resultado SQL observado antes del resumen y extremos originales. |
| @s5 | TodayWindowTest.s5: Madrid23/25/24h, límites UTC y presupuesto del weekday. |
| @s6–7 | TodayWindowTest.s6/s7: cruce en ambos días, cierre real, entrada desordenada y cinco posiciones del reloj. |
| @s8–9 | TodayApiTest.s8/s9: cuatro combinaciones de estado, nombres y DTO9 exactos;21 filas insertadas al revés, ninguna truncada y resumen completo. |
| @s10 | TodayApiTest.s10_snapshot: writer real confirma entre SELECTs y siguiente lectura observa todos los cambios; dos casos ausencia/presencia. s10_postgresSnapshotIsActuallyReadOnlyRepeatableRead consulta SHOW de ambos modos y comprueba conexión posterior no read-only. |
| @s11 | TodayApiTest.s11: reloj cambia de día tras primer valor, verificación times(1) y precisión9→6, candidatos y suma del día inicial. |
| @s12–14 | TodayApiTest.s12/s13/s14: ownerB con nombres/capacidad propios no aparece;401 precede query; cinco nombres de parámetros producen400 sin snapshot/escritura. |
| @s15 | TodayApiTest.s15: tabla de preferencia, tabla de reservas y cierre fallan con503 exacto sin detalles internos; restauración de fixture en finally. |

El caso commit simula una TransactionSystemException después de ejecutar commit real, sólo cuando el contexto era read-only. Es un oráculo válido de traducción del fallo al cerrar; no acredita una avería física de disco/red. La bitácora distingue su primer fixture erróneo (interceptaba Spring Session) y el caso corregido inicialmente verde, sin fabricar un RED.

El orden secundario por UUID está en SQL y el orden estable del stream lo conserva. TodayWindow no añade comparación secundaria propia; con reservas válidas sin solape no existen dos inicios iguales. No solicito una prueba artificial de datos inválidos ni un cambio productivo por esa redundancia.

## Evidencia y cierre pendientes

Inspeccionada progress/tdd_today_backend.md: ciclos RED/GREEN por comportamiento, casos heredados inicialmente verdes reconocidos, sin ampliaciones especulativas. Autor comunica49 Today y wiring operativo adicional, contextos21 GREEN; cifras todavía pendientes del XML final postformato. Restan confirmar el diff final de formato, scope `today`, wiring/readOnly en ese corte y regresión conjunta. No hay hallazgo funcional que justifique modificar producción durante COPY. La revisión final se añadirá aquí tras esa evidencia, sin repetir suites.

## Dictamen final del backend congelado

**APPROVED — diseño, cobertura @s1–15 y soporte de medición del backend.** El pendiente del corte anterior queda resuelto; no se detectaron cambios funcionales requeridos. No aprueba frontend ni sustituye init, E2E o la puerta de mutación de la feature.

Inspeccionado corte postformato a157b8: TodayWindow y PostgresTodayQueries conservan el comportamiento revisado; scope today incluye seis patrones de clases productivas y cuatro suites (núcleo, HTTP real y dos contextos), con ApplicationConfiguration completo. Default añade adaptadores y suites nuevas, manteniendo core, umbral80 y políticas previas. Interfaces puras y cambio de visibilidad MAPPER no añaden ramas fuera de campaña; regresión de store explícita en el corte.

Leídos XML existentes, sin ejecutar pruebas: a157b8 fecha de TodayApi/TodayWindow2026-09-06T14:44:04+02:00 y f75aed confirma283 tests,0fallos/errores/omitidos:22 TodayWindow,27 TodayApi,15 Wiring,7 ProjectStateConfiguration,1 Architecture,173 ScheduleBlockApi y38 ScheduleBlockPersistence. Coinciden con ejecución c94da8 y lectura8f8c35 comunicadas por el autor. El wiring operativo y SHOW read-only/RR están presentes en las suites leídas. No se atribuye esta lectura como rerun independiente ni se inventa score de mutación.
