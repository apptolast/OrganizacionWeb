# Evidencia contractual de iniciar trabajo — feature14

Corte de lectura: backend core37c86d1, HTTP7186fc3 y wiring congelado104b16; frontend congelado y campaña inicial terminada. Este mapa referencia ejecuciones existentes, no ejecuta ni modifica pruebas. Las42etiquetas no equivalen a 42 pruebas ni a todas las filas de Examples ejecutadas de extremo a extremo. La evidencia compuesta combina un contrato de puerto/HTTP con comportamiento real PostgreSQL; se indica cuando falta una fixture literal. No se infiere un defecto por esa diferencia.

## Fuentes de evidencia

- **C/P/R/M**: [bitácora backend](tdd_start_work_backend.md), GREEN1e3110/XML5b65c8: StartWorkSessionTest17, WorkSessionStoreTest41, ReadWorkSessionsTest4 y WorkSessionPersistenceTest1;63 sin fallos/errores/omitidos, Spotless real. Tests en backend/src/test/java/com/apptolast/organization/application y adapter/persistence.
- **H/W**: [HTTP](tdd_start_work_http.md) y [wiring](tdd_start_work_wiring.md), GREEN104b16/XML4bfe94: WorkSessionApiTest49, WorkSessionIntegrationTest3, ApplicationWiringTest16 y ProjectStateConfigurationTest7. Las 75 incluyen H, no se suman como suites independientes. H usa puertos controlados; W atraviesa Spring, seguridad y PostgreSQL reales.
- **PUB**: [publicador](tdd_start_work_publisher.md), GREEN2249c5/XML2626ab:185 pruebas en PublishOutboxTest, RabbitBrokerPublisherTest, RabbitBrokerFailuresTest y PublisherConfigurationTest; incluye 19 casos nuevos. Ocho rutas previas conservadas por regresión, novena comprobada con Rabbit real.
- **F**: [frontend](tdd_start_work_frontend.md), GREEN4a7565:73 propias (32API,39UI,2integración), más regresión heredada250/250ff75d9. work-session-api.test.ts, work-session.test.tsx y work-session-integration.test.tsx; JSDOM no mide layout ni motores.
- **S**: [smoke real](tdd_start_work_publisher_smoke.md), EXIT0/3e2f05: API, PostgreSQL y Rabbit, pérdida controlada de respuesta HTTP, retry y reinicio. Los seis eventos históricos del script también pasaron. No se atribuye al smoke una repetición de las otras dos rutas de11/13.
- **E**: [navegador](tdd_start_work_e2e.md): nominal1/1 GREEN5d91ac; recuperación14 inicialmente GREENe53689 (esa invocación también ejecutó un caso11, no se cuenta como segundo caso14); otra tarea/logout1/1 GREEN2450d0. UX56081 sigue en ejecución y no se cuenta como pasado.

## Mapa de las 42 etiquetas

| Contrato | Evidencia existente y límite |
| --- | --- |
| @s1 | C.s1 exactitud de inicio/evento y lectura única de Clock; M.s1 upgrade y persistencia sin alterar planificación; H.s1 DTO7/Location; W nominal real201 yµs; E nominal y S. |
| @s2 | C.s2 uno y1440min, ambos extremos ejecutados individualmente; H.s6_oneMinute/maximumMinutes conectan valores al puerto. No se duplican como dos E2E. |
| @s3 | C.s3_fixedDurationAcrossMidnight/SpringDst/AutumnDst ejecuta los tres instantes exactos del contrato. F muestra inicio/fin/zona; presentación de las tres transiciones en navegador aún no acreditada por este corte. |
| @s4 | C.s4 cubre las tres condiciones de fallback; P nominal sin preferencia y M zona válida. P.s20 separa fallo SQL de ausencia. Evidencia compuesta: no hay tres fixtures HTTP/PG de zonas inválidas. |
| @s5 | P.s5 idea/paused y M nominal active preservan estados y planificación; C no depende del presupuesto. Las fixtures idea/paused no tienen disponibilidad, por lo que no equivalen literalmente a una preferencia con presupuesto0. |
| @s6 | H cubre ausente,null,string,boolean,fracción,0,1441 y entero grande; C cubre rango antes del puerto. Siete filas observadas, además del refuerzo de overflow. |
| @s7 | H empty/truncated/duplicate/trailing/rootArray/extraField: lector estricto y cierre lexical antes del puerto/replay. Seis formas verificadas. |
| @s8 | H query POST/active repetida/ID/key, sin invocar puertos; seguridad, origen, CSRF y415 tienen conexiones representativas. |
| @s9 | H anonymousRead, detailQuery, byRequestQuery, queryPrecedesInvalidPathAndMissingKey y missingKeyPrecedesMalformedBody; además ambos IDs y key repetida/malformada. Cinco filas conectadas. |
| @s10 | P.s10_foreignContextIsNotFoundBeforeReplay es real; H.s10_contextNotFoundDoesNotExposeSession verifica404 cerrado. Contexto inexistente y tarea de otro proyecto comparten consultas filtradas, pero no tienen aquí una fixture PG independiente previa a replay. La FK deV15 prueba integridad, no sustituye esas peticiones. |
| @s11 | C.s11 tres combinaciones y prioridad proyecto; H títulos propios de inicio; P.s11 completed ante fallo de consulta de zona verifica ambas entidades. |
| @s12 | P.s12 activa antes de completed; H.s12 problema cerrado con sólo sessionId adicional; F ofrece consulta propia. |
| @s13 | C.s13 tres límites ejecutados; H.s13 título y forma409 sin culpar campo; F rechazo definitivo diferenciado. |
| @s14 | P.s14 cambia ambos estados y demuestra cero interacción con Clock/catálogo; W replay real conserva DTO/Location/filas; H y F200 histórico. La fixture P no cambia una preferencia existente: demuestra replay sin disponibilidad y sin negocio actual. |
| @s15 | P.s15 duración, tarea y proyecto/tarea ejecutados; H forma409; F mantiene intención incierta ante conflicto. |
| @s16 | P.s16 misma key en creación11 y cancelación13 válidas, nueva sesión14 y registros anteriores intactos. Fixture SQL coherente, no POST11/13 repetidos. |
| @s17 | P.s17 tres carreras: ambos intentos de INSERT alcanzados, una sesión/evento y resolución tras rollback en transacción nueva. H/W traducen creación/replay/conflictos; no son tres carreras HTTP. |
| @s18 | P.s18 dos propietarios concurrentes con misma key, dos confirmaciones y activas propias. H/F mantienen consultas por principal, sin restringir a tarea abierta. |
| @s19 | P.s19 cuatro órdenes reales frente a completar proyecto/tarea; pg_stat_activity Lock y futuro pendiente antes de liberar. Inicio ganador queda activo/recuperable después de completed. |
| @s20 | P.s20 cinco fallos contractuales: consulta, supresión sesión, supresión outbox, fallo outbox y fallo diferido de commit; rollback y conteos exactos. Refuerzo23505 distingue fallo outbox de colisión de intención. H traduce503 cerrado. |
| @s21 | P.s21 ID/key después de completed y sin outbox; H ambas rutas sinLocation; W recuperación real; F valida identidad/intención. |
| @s22 | P.s22 cuatro identidades ausentes/ajenas, ID/key; H mismo404 cerrado; F preserva respuesta sin inventar éxito. |
| @s23 | P.s23 activa propia y P.s18 aislamiento entre propietarios; H null explícito/activa sin ruta de tarea; F otra tarea y ausencia. Falta fixture PG literal con sólo activa ajena y consulta del propietario vacío; evidencia compuesta, no afirmación de esa fila ejecutada en PG. |
| @s24 | P.s24 seis fallos SQL/cierre reales y observación readOnly/READ_COMMITTED sin locks; H tres traducciones503; F no convierte error en ausencia. |
| @s25 | S corta respuesta tras upstream201, recupera key, verifica publicación real, retira evento publicado y reinicia backend; recibo/activa idénticos, sesiones1/eventos0. P/W aportan persistencia, pero no se cuentan como reinicio adicional. |
| @s26 | PUB nominal/histórico y Rabbit real,11 campos/identidades/microsegundos/routing/cola; regresión 185 preserva ocho rutas. S conecta recibo con payload y novena cola en stack real. |
| @s27 | PUB cinco condiciones: broker unavailable, confirm timeout, extra, fin incompatible y zona histórica; misma identidad/JSON al reintentar. S añade caída y recuperación real del broker; pérdida de confirm del broker se acredita en tests del protocolo/adaptador, no se atribuye al relay HTTP. |
| @s28 | F montaje/ausencia/duración explícita/elegibilidad e integración TaskReader; E confirma inicio deliberado sin duplicación. Sin controles15–18. |
| @s29 | F consulta pendiente/ausencia/error/reintento/otra tarea y bloqueo por ausencia obsoleta. E otra tarea GREEN2450d0; UX real pendiente al corte. |
| @s30 | F API cierra DTO, identidad/contexto, duración, instante/calendario, fin exacto incluso1µs, zona yLocation; UI conserva incertidumbre ante éxito incompatible. Reutiliza validación temporal de DTO existente, sin afirmar un nuevo E2E por cada vector. |
| @s31 | F fallback Intl con zona histórica visible y UTC explícito; exactitud antes de1970. Navegador con zona no soportada pendiente de entrega UX. |
| @s32 | F pending/red/503/desconocido/conflicto, key/cuerpo retenidos, bloqueo y no reenvío automático. |
| @s33 | F comprobación válida/404/503 y repetición conservan intención; W/S respaldan GET durable. E recuperación GREENe53689. |
| @s34 | F useSession/SessionGate real en JSDOM: renovación manual separada del reenvío y token vigente; no segundo flujo CSRF. |
| @s35 | F cinco rechazos y recuperación contextual; H problemas cerrados y P reglas reales. |
| @s36 | F aviso de salida, regreso sin key, descubrimiento de activa y lookup anterior retirado; E recarga nominal. S confirma que cortar respuesta no revoca el commit. |
| @s37 | F recibo histórico conserva fin ante fallo posterior; no temporizador que lo amplíe/cierre. W/S recuperan mismo hecho. |
| @s38 | F cambio de tarea, JSON tardío, clasificación tardía, desmontaje y aborto previo a401; pruebas no invocan funciones privadas. Evidencia de UI/cliente, no caída física de red. |
| @s39 | F401 actual y SessionGate logout retiran datos antes de terminar petición; lookup tardío no restaura. |
| @s40 | F anuncios de envío/check/consulta, foco al desaparecer iniciador, control externo y Enter/Tab/ShiftTab. Foco visible y geometría reales siguen sujetos a UX navegador. |
| @s41 | ux_start_work_session.md contiene30 criterios con límites. Gate final pendiente de responsive/zoom/texto/motores/mediciones y dictamen independiente; no se certifican dispositivos físicos ni estudios humanos. |
| @s42 | F API envelope/SessionStart inválidos y UI error sin habilitar inicio; activa válida de otra tarea aceptada. |

## Puertas y pendientes al corte

No hay una nueva ejecución por este mapa. Init global46298 estaba activo según coordinador; no se le atribuye todavía resultado. E2E/UX final siguen en curso. Stryker inicial sí terminó PASS452/533=84,8030%, EXIT0, con81 Survived pendientes de dictamen: véase mutation_start_work_frontend.md; no se ocultan ni se declaran todos equivalentes. PIT backend todavía pendiente de puerta/campaña y revisión del resultado.

Los huecos literales señalados en @s5/@s10/@s14/@s23 son límites de evidencia, no bugs demostrados ni una exigencia automática de repetir cada variante. El coordinador decide refuerzos sólo si hay un comportamiento diferenciable no acreditado. Este mapa no declara14 terminada ni habilita estados15–18.

## Actualización de gates posterior

Init438035 y XMLd0b518:1751Java,1571frontend y29scripts verdes; después del refuerzo/fixf745c94, regresiónfrontend1585 y build verdes4393bc/4c4a3d. UX aprobado en judge_start_work_ux.md:412medidas/28axe sin violaciones, JSON independiente7ea3d5. PITbackend2804 y Strykerfinal85286 siguen en curso; PR12 head3930123 tiene CI34068314181 en ejecución. El mapa no declara cierre antes de esos resultados.
