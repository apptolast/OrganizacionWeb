# Soporte de mutación14

Se conserva el arnés existente con destinos cerrados `start_work_session-frontend` y `start_work_session-backend`. Los tests inyectan un ejecutor y no lanzan campañas, Gradle ni Stryker. Usuario y coordinador autorizaron preparar los gates; la ejecución real espera revisión y freeze comunes.

| Ciclo individual | RED | GREEN |
| --- | --- | --- |
| Despacho frontend a configuración fija | 07eab4, Invalid target | 4f8ec8 |
| Despacho backend a -PmutationScope=start_work_session | ed37c8, Invalid target | 6d8401 |
| Scope frontend de API/UI e integración TaskReader | 2bde30, configuración ausente | 9a3af4 |
| Default frontend incluye las fuentes nuevas | 4de090, API ausente | 06a80d |
| Sufijo de configuración inyectado rechazado antes de ejecutar | inicialmente verde | 6ad92b |
| Target backend usado en tarea test rechazado antes de ejecutar | inicialmente verde | db67d1 |

Formato focal9795c2; regresión completa del archivo de despacho28/28 GREENd03b00. Los destinos anteriores y default siguen pasando sus oráculos existentes. No se reducen umbrales ni se altera selección de tests para aumentar puntuación.

Frontend focal: `work-session-api.ts` y `work-session.tsx` completos; `task-reader.tsx:124:0-135:12` incluye toda la nueva composición y sus guardas de elegibilidad. El test verifica los extremos de la región contra fuente actual. SCSS no genera mutantes Stryker y requiere validación UX independiente. Umbral80, perTest, concurrency8 (decisión ya validada en13 y ratificada por root), exclusión exacta `.stryker-tmp-availability-replay`, informes propios `reports/mutation-start-work-session/mutation.json/html`. No se accedió al destino protegido. Default añade API/UI14; TaskReader ya se medía completo.

Pendiente backend: el despacho está probado, pero `build.gradle.kts` no se ha modificado mientras el autor mantiene Gradle activo. En su freeze se añadirá selección de clases14 y compartidas productivas definitivas, todos los candidatos JUnit, threads4/80/filtros heredados e informe separado. No lanzar todavía el destino backend: antes de ese selector caería en el scope por defecto. Lista preliminar recibida del autor, sin afirmar inventario final ni cobertura.

No campañas ejecutadas en este paquete. Producto frontend permanece congelado según `start_work_frontend_freeze.json`; este documento no declara terminados mutación, E2E ni UX14.

## Selector backend cerrado tras freeze

La espera anterior terminó con freeze core63/63 y cesión explícita de build por autor wiring. RED03ec47: no existía startWorkSessionOnly. Se añadieron10patrones reales (domain.SessionStart/OutboxMessage; application.StartWorkSession*,ReadWorkSessions*,WorkSession*,PublishOutbox; PostgresWorkSessionStore*;WorkSessionController*;ApplicationConfiguration;RabbitBrokerPublisher), incluyendo clases internas por wildcard. GREENebd6ca. Las interfaces/records sin instrucciones mutables no añaden mutantes ficticios: el mutador mide bytecode real. Todos los candidatos JUnit `com.apptolast.organization.*`, threads4/80/FRECORD/excludedMethods y demás filtros heredados intactos, reportdir separado pitest-start-work-session. Default añade los nuevos adaptadores para no omitir14.

El test de selección se añadió DESPUÉS de terminar Stryker y capturar after88/88; no altera la medición original. Formato focal y regresión29/29 EXIT0 5ea30a. Root revisó selector955445 APPROVED. Build cedido estable al autor wiring para su siguiente Gradle, sin ejecutar PIT desde este paquete. Fuentes producto siguen congeladas; arnés/config también quedan estables para commit del coordinador.
