# TDD publish_outbox backend

Usuario aprobó expresamente publish_outbox; root init exit0 antes de producción. Alcance23 escenarios/36 casos en features/publish_outbox.feature. No commit/push ni done desde este agente sin cierre coordinado.

## Ciclos reales

1. @s1 PublishOutboxTest.s1_recordsPublishedOnlyAfterAcceptedOriginalMessage: ROJO tipos/puertos ausentes. Implementación mínima del primer intento y logging después de confirmación del puerto transaccional.

## Referencias

RabbitMQ Java API Guide y confirms oficiales consultados: confirms no equivalen a consumo; mandatory return precede confirm del mismo mensaje; conexiones largas con recuperación propia. Cliente com.rabbitmq:amqp-client5.25.0 gestionado por BOM oficial SpringBoot3.5.11, validado en Maven Central. RabbitMQ4.3.5-management-alpine verificado por agente de integración.
2. @s5 parámetro cuatro fallos: ROJO enum sin códigos y resultado incorrecto; VERDE retry conserva evento, attempt1 y fecha+1s.
3. @s7 cinco intervalos: ROJO cuatro fechas incorrectas; VERDE desplazamiento limitado antes de overflow (1,2,32,60,60).
4. @s17 lote21: ROJO solo1 procesado; VERDE máximo20 y21.º pendiente.
5. @s5 ciclo largo: ROJO mismo evento enviado20veces; VERDE conjunto de eventId ya procesados transmitido al puerto de reclamación.
6. @s15 tipo/versión: ROJO dos inválidos enviados; VERDE blocked sin intento de envío, siguiente válido se publica.
Cada ciclo ejecutó gradlew test --tests *PublishOutboxTest; siguiente test se añadió tras verde. Sin bloqueos de herramientas.
7. @s15 payload: ROJO14 defectos publicados; VERDE validación pura de7campos exactos, tipos/identidades/instante/nombre normalizado. 8. @s5 reloj que avanza: ROJO next_attempt_at usaba segunda lectura; VERDE una lectura del instante de finalización para estado y retraso.
9. @s18 almacenamiento: ROJO excepción escapaba; VERDE worker_error STORAGE_UNAVAILABLE y ningún envío.
10. @s1 PostgreSQL: ROJO adaptador ausente; VERDE migración V2 aditiva, fila original/payload conservados y published tras resultado de callback confirmado.
11. @s5 PostgreSQL: ROJO fallo de broker marcado published; VERDE estado pending,attempt1,last_error_code,next_attempt_at y exclusión de mismo evento dentro del ciclo.
12. @s8 PostgreSQL: ROJO evento futuro publicado; VERDE next_attempt_at<=now y frontera exacta.
13. @s17 PostgreSQL: ROJO orden de heap en vez de ocurrido/id; VERDE ORDER BY occurred_at,event_id y máximo20.
14. @s15 PostgreSQL: ROJO JSONBnull/array/string/number abortaban ciclo; VERDE snapshot inválido conservado blocked,attempts3 intactos,siguiente válido publicado yblockedno reseleccionado.
15. @s2 regresión verde: transacción de creación no confirmada invisible a publicador en otro hilo; rollback original conserva aislamiento.
16. @s12 ROJO falloSQLreal al UPDATE escapaba; VERDE rollback completo,sin auditpublished,worker_error STORAGE_UNAVAILABLE. Trigger retirado siempre en finally.
17. @s4/@s13 regresión de FOR UPDATE SKIP LOCKED: dos réplicas reales JDBC, latch acotado5s y liberaciónfinally; fila reclamada siguepending y otra réplica procesa otrafila sin interferir.
18. @s19 configuración: ROJO clase ausente; VERDE publicador deshabilitado no crea worker/outbox/broker.
19. @s23 auditoría: ROJO adaptador ausente; VERDE solo eventId/outcome/attempt/code, blocked registra0 aunque attemptsalmacenado3.
20. @s22 secreto ausente: ROJO scheduler ausente; VERDE CONFIGURATION_ERROR sin reclamar.
21. Disparador: ROJO configuración válida no ejecutaba ciclos; VERDE @Scheduled fixedDelay1s/initialDelay1s y wiring real puertos/JDBC/Rabbit. Habilitado por OUTBOX_PUBLISHER_ENABLED, defecto false.
22. @s22 config inválida: ROJO host/usuario/vhost/puerto inválidos permitidos o contexto caído por conversión; VERDE parseo interno y worker inactivo manteniendo API viable.
23. @s18 auditoría worker: ROJO no registraba; VERDE mensaje estructurado con código sin excepción/secretos.
24. @s21 ROJO excepción de topología escapaba; VERDE worker_error TOPOLOGY_MISMATCH y ningún resultado de evento confirmado.
25. Snapshot: ROJO mutación externa cambiaba validación; VERDE copia defensiva no modificable, preservando nullvalues inválidos.
26. Instantes: ROJO dos resultados conservaban nanos no persistibles; VERDE MICROS coherente para resultado/reintento.
27. Apagado: ROJO hilo interrumpido reclamaba segundo evento; VERDE termina lote preservando interruptflag.
28. Mutación: PIT inicial 86/90 detectó cuatro carencias reales (JSON entregado, versión entregada, nombre válido de120 puntos de código y fecha parseable distinta de columna). Aserciones de comportamiento añadidas; tests core37 verdes y PIT90/90 (100%), ningún superviviente ni NO_COVERAGE. No se modificó producción para estos refuerzos.
29. Regresiones de frontera: PostgreSQL15/configuración11 tests verdes (ejecución39348). Vacío sin escrituras, published no se reenvía ni modifica timestamp, conexión PostgreSQL realmente rechazada no llama al broker, y scheduler real espera al menos1segundo entre dos fallos de almacenamiento. Producción ya satisfacía estas aserciones; no se declara un rojo inexistente.
30. Verificación backend completa74662 exit0: spotlessApply, test (147 casos /12 suites,0fallos), pitest (90/90,105/105 líneas,0supervivientes/NO_COVERAGE), bootJar. Incluye recuperación real de procesos y PostgreSQL/Rabbit de OutboxRecoveryTest (3 casos), incorporada por integration_craftsman; no hay backdoors de fallo en producción. Fuentes/tests congelados para revisión independiente; no se marca feature done ni se despliega.
31. Verificación final local del coordinador 6887: exit 0, lint, 147 tests backend, 38 frontend, PIT 90/90 y Stryker 143/148 (96,62 %) correctos. E2E 49506 completó ocho pruebas base y tres etapas de smoke del publicador. Juez APPROVED. Código publicado en 1a3737758c655462fc3814f6af8d0f87138eb1a8; cierre pendiente de CI remoto 33993262637, actualmente en curso. No se cambia el estado a done ni se despliega en servidor.
32. Cierre autorizado por el coordinador tras CI 33993262637 completed SUCCESS para 1a3737758c655462fc3814f6af8d0f87138eb1a8, incluidos verify/build/E2E/publisher smoke. Feature 2 done; feature 3 spec_ready y aprobación pendiente. Sin cambios de producción ni despliegue.
