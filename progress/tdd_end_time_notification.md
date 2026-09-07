# Mapa maestro de evidencia17 — corte provisional

Contrato `features/end_time_notification.feature`, SHA6BC581725DC0FE4C7B548191A842882CE5A62FD0348CF789D1BCCD843ABE4309: **44 escenarios,132 ejemplos**. Esta tabla no convierte ejemplos en tests ni declara done. Lectura documental actual; A está congelado y aprobado parcialmente por revisión independiente; B sigue trabajando. Una fila parcial no acredita todas sus variantes ni sustituye la revisión de sus fuentes finales.

Fuentes: **A** = `progress/tdd_end_time_backend.md` (core/PG final175/175 en13 suites, EXIT0 dc5c90; revisión review_end_time_backend.md y26hashes verificados8bc66b). **B** = `docs/tdd_end_time_frontend.md` (cliente20 nuevos+91 heredados,111 GREEN5d7230 aprobado; panel/coordinador hasta62 casos incluyendo36 heredados y1 hook, WIP). **H** = `progress/tdd_end_time_http.md` (27 nuevos+73 heredados,100 GREEN3b771b). **P** = `progress/tdd_end_time_publisher.md` (226 GREEN7ff7c5, aprobación e integración2089cbd). **I** = `progress/tdd_end_time_integration.md` (HTTP+PG real1 GREEN4bffa6). **E2E** = árbol aislado end-http, `progress/tdd_end_time_e2e.md`, único caso REDc6568d por panel ausente; sus assertions posteriores todavía no alcanzadas.

| @s | Evidencia localizada | Estado y límite real |
| --- | --- | --- |
| 1 | A `s1_extendsTheEffectiveEndWithoutChangingStateOrWork`, PG `s1_extendCommitsEndRevisionReceiptAndIndependentEvent`; H nominal/min1; I | Nominal real y precisión acreditados en checkpoints. E2E pendiente de mismo caso GREEN. |
| 2 | A `s2_extendsAPausedExpiredSessionFromTheConfirmationClock`; H máximo1440 paused | Parcial: caso tardío paused acreditado; variante running y completed de tarea/proyecto requieren mapa final A o reutilización explícita. |
| 3 | A PG end_s11_sameMicrosecondAllowsAnotherExtensionWithoutACumulativeDayLimit | Final GREEN8b01fe: acumulación2880 y marca igual, sin trabajo nuevo. |
| 4 | A cero/1441 antes de Store; H REQUIRED/INVALID_TYPE/fracción/overflow y límites; B cantidad vacía | Clases de validación y no delegación acreditadas. Null/boolean/array/objeto usan las mismas guardas; no contar cada fila como prueba nueva. |
| 5 | H queries P/E, anónimo E, revisión antes JSON, documentos concatenados, campo extra, CSRF;73 heredados | Conexiones nuevas acreditadas; negociación/UUID/errores comunes se reutilizan de15/16. |
| 6 | A PG `s6_extendRequiresTheTokenSessionIdentityBeforeBusiness`; H token completo y412 | Parcial: identidad preservada; rama propiedad ajena antes de token debe quedar enlazada al mapa final A. |
| 7 | A `s7_replaysAnExtensionAfterClosureAndAnotherStartWithoutClock`; H replay; I después de CLOSE | A acredita estado posterior y Clock sin uso; I confirma201→200 durable. |
| 8 | A `s8_rejectsAnotherQuantityForTheSameExtensionKey`; H409; colisión A cross-session | Cantidad y contexto distintos acreditados. Acción/revisión reutilizan validación de intención; exigir referencia precisa final, no matriz artificial. |
| 9 | A stale antes Clock, reutiliza State.requireClose | Parcial; closed vigente y BIGINT máximo heredados necesitan enlace a pruebas compartidas finales. |
| 10 | A EXTEND/PAUSE/RESUME/CLOSE ante marca; actualización de marca en RESUME | Cuatro decisiones documentadas con PG para P/R/C. Falta únicamente freeze/regresión final A. |
| 11 | A PG end_s11_resumeAtTheExtensionMicrosecondPreservesTheExtendedEnd | Final GREENb3b4eb: paused→RESUME exactamente en marca, trabajo/fin conservados. |
| 12 | A año10000 y desbordamiento de fin; State temporal heredado; H409 | Parcial: año0000 por reutilización requiere referencia final A. P también prueba extremos del evento, que no sustituye core. |
| 13 | A ReadEnd nominal y PG E; H E3/State6/cabecera; I original y closed | Consulta real y forma acreditadas; legacy paused/closed sin proyección deben completarse con upgrade final A. |
| 14 | A E anterior a marca/fuera de rango; H404/409; I propiedad real | Parcial: orden de propiedad y frontera igual a marca dependen de mapa final A. |
| 15 | A declara plantilla read-only RR reutilizada | Observación concurrente de E aún pendiente; el slice y el nominal no acreditan snapshot concurrente. |
| 16 | H E503; cliente B503; A declara fallos de consulta pendientes | Falta prueba real de error/fin de transacción E y vínculo K heredado; no confundir mock HTTP con rollback. |
| 17 | A cinco casos individuales: supresión proyección/recibo/outbox, COMMIT diferido y restricción distinta; H503 | Resultados inicialmente GREEN documentados; pendientes freeze/revisión de A, sin nueva duplicación requerida. |
| 18 | A carrera misma key0a2f75 con espera Lock observable | Documentado GREEN; no freeze final aún. |
| 19 | A dos EXTEND9deac4 y EXTEND/PAUSE8d3183 | Parcial: EXTEND/RESUME y EXTEND/CLOSE todavía pendientes en la bitácora leída. |
| 20 | A `end_s20_crossSessionCollisionRollsBackBeforeFreshIntentLookup` REDbbeb1c→26441f; supresión recibo3f2a2a | Conflicto y ausencia durable acreditados en fixture PostgreSQL controlada. Véase límite explícito debajo para misma intención. |
| 21 | Sin caso17 concreto localizado todavía | Pendiente A: no esperar locks de planificación/otro propietario; no deducir ejecución sólo de lectura de código. |
| 22 | A JSONB P/R literal y regresión CLOSE que detectó extension:null; H73 heredados | Compatibilidad de forma acreditada; upgrade PostgreSQL17 de todos los estados aún pendiente A. |
| 23 | H C/K exactos; I recuperación tras CLOSE y retirada exclusiva outbox17 | Durabilidad sin retención acreditada; ampliación posterior y reinicio real de API siguen pendientes del smoke/E2E acordado. |
| 24 | P Rabbit real bytes/ruta/quorum/durable y reintentos; I evento PG | Componentes acreditados. Falta recorrido conjunto ACK perdido, broker/API reiniciados y recuperación. |
| 25 | P validación de11campos, revisión/status/fracción/fórmula/extra y extremos;226 regresión | Paquete congelado y aprobado. No equivale al smoke@s24. |
| 26 | B cliente20 nuevos y91 heredados, revisión root aprobada | Guardas principales y transporte común acreditados. Integración de incertidumbre del panel sigue WIP. |
| 27 | B positivo paused tardío1440; BigInt/microseconds compartidos14–16 | Parcial explícito B: fixture nueva2026 no supera Number seguro; referencia precisa de época grande heredada o caso focal pendiente de revisión final, sin afirmar test nuevo. |
| 28 | B panel nominal aviso/acciones; montaje real pendiente; E2E RED | No acredita aún ambas superficies ni activa de otra tarea. |
| 29 | B cantidad vacía sinPOST y formulario; E2E preparado | Panel parcial verde; separación del cierre en montaje real pendiente. |
| 30 | B plazo monotónico→GET antes aviso, GREENf0e6f3 | Verde WIP; falta freeze y revisión del panel. |
| 31 | B plazo25d sin overflow, GREENd2f230 | Verde WIP; no evidencia navegador aún. |
| 32 | B1µs/anticipado, GREENf58125 | Verde WIP. Ciclo45 está etiquetado@s32 en bitácora, pero su oráculo de reloj atrasado corresponde a@s34. |
| 33 | B visibility/coalescing GREENf753be | Verde WIP; rama sin consulta pendiente y retirada de contexto se revisarán al freeze. |
| 34 | B E503 inicial/retry00e1b2 y aviso tras reloj atrasado9d4705 | Parcial WIP; conservar aviso tras fallo de actualización/409 requiere mapa final. |
| 35 | B POST explícito+E nuevoa05ccb | Parcial WIP; anuncio único al nuevo vencimiento y renders repetidos aún no localizado. |
| 36 | B ref inmediata8d66ae, composición PAUSE→EXTEND32a830 y dirección inversa e908d6 | Parcial WIP; CLOSE/recuperación y montaje real siguen pendientes reconocidos. |
| 37 | B POST503→K, K404 manual, CSRF manual | Parcial WIP; K503, unknown403 e intención incierta frente a conflicto/dato inválido no aparecen aún como casos explícitos. |
| 38 | B412 conserva cantidad y nueva key/revisión b4e5d8 | Parcial WIP; Eclosed/Efallido tras412 pendientes del mapa final. |
| 39 | Bclosed e53970; I fin durable cerrado | Parcial; recibo confirmado con E fallido, recarga sin key y otra activa requieren integración UI final. |
| 40 | B generaciones PAUSE→E y EXTEND→S,57/58GREEN | Parcial WIP; llegada tardía de E/S/A y montaje real todavía en curso. |
| 41 | B401 previo E/POST y aborto de lecturas hermanas1ecf08/c1a8b1 | Parcial WIP; JSON tardío y clasificación no401 necesitan evidencia final, no se deducen del mero AbortController. |
| 42 | B404 actual9699d7 y401 actualba8ba4 | Parcial WIP; identidad p/t/s del Reader depende de integración y tests16 reutilizados. |
| 43 | B feedback/foco/doble submitc83832 y retry00e1b2 | Parcial WIP; control retirado/control externo y evidencia navegador<400ms pendientes de validación final. |
| 44 | Sin ejecución UX17 | Pendiente de primer E2E GREEN, recorridos reales y matriz30 con límites de motores/dispositivos. No reutilizar PASS16 como ejecución17. |

## Límite de@s20 y pendientes operativos

El caso de colisión cross-session oculta la primera búsqueda de key y después observa una violación UNIQUE real y dos txid; A está cerrada y B es la única abierta. No representa una carrera natural de dos abiertas. La carrera misma intención sobre una sesión se serializa por su fila y ya acredita201/200 en@s18; eso no prueba por sí solo la rama excepcional de recuperación exitosa posterior a UNIQUE. El mapa final debe mantener esa distinción y justificar reutilización/inalcanzabilidad si procede, sin fabricar un fixture incompatible ni exigir100% de mutación.

Pendientes ya reconocidos: A completa atomicidad/carreras/upgrade/lecturas; B completa integración/coordinación/privacidad y freeze; C conserva único E2E RED y todavía no tiene ownership de smoke completo. No se ha demostrado un defecto nuevo mediante esta lectura documental. Dos precisiones de trazabilidad para no perder al cierre: ubicar@s3/@s11 y la época grande de@s27; corregir el enlace documental del ciclo45 B de@s32 a@s34. Son solicitudes de evidencia acotada, no autorización para añadir matrices ni duplicar suites. Gates globales, campañas17 y validación UX permanecen pendientes.

## Actualización tras freeze final A

Revisión independiente `review_end_time_backend.md` APPROVED para core/PG/migración.175/175,13 XML,26 hashes idénticos8bc66b. Las filasA anteriores marcadas parciales describen el checkpoint previo y quedan completadas por el mapa final@s1–23 de `tdd_end_time_backend.md`: cuatro carreras@s19, RR@s15, fallos@s16–17, independencia de locks@s21, upgrade@s22 y referencias precisas a guardas heredadas@s9/@s12. @s3/@s11 ya actualizadas en tabla. No se suman ejecuciones ni se crean casos por referencia.

@s20 ahora añade `end_s20_sameIntentRecoveryUsesTheDurableWinnerAfterRollback` GREENa5b62e: snapshot/lookup controlados, lock y UNIQUE reales, dos txid y efectos intactos. Se acepta como inyección controlada de la recuperación excepcional, sin atribuir carrera natural. La observación previa de falta de esa rama queda resuelta; se conserva el límite metodológico. Sólo siguen pendientesB, E2E/smoke y gates integrados; en@s23 todavía falta reinicio real y ampliación posterior.