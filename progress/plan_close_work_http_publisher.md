# Plan acotado HTTP y publicación16

Sólo preparación. Contrato leído: sección16 de `project-spec.md` y `features/close_work_session.feature`, SHA256 `3E45F26004E2656E97443451E2D0368CBC9DE734D859725BD07896E206A44285`. No hay código, stubs ni aprobación de implementación implícita en este plan. Root dará la señal; Jason entrega las firmas reales compilables.

## Propiedad y orden

1. Publicación: `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java`, `adapter/broker/RabbitBrokerPublisher.java`, y sus pruebas existentes `application/PublishOutboxTest.java` y `adapter/broker/RabbitBrokerPublisherTest.java` bajo `backend/src/test/java/com/apptolast/organization/`. Bitácora propia `progress/tdd_close_work_publisher.md`. Puede avanzar sin Store ni puertos de lectura cuando exista el evento real congelado; no crear un record paralelo.
2. HTTP: `backend/src/main/java/com/apptolast/organization/adapter/http/WorkSessionStateController.java` y la suite nueva `backend/src/test/java/com/apptolast/organization/adapter/CloseWorkSessionApiTest.java`. Conservar la suite15 `WorkSessionStateApiTest.java`; sólo adaptar su construcción si las dependencias reales del controller lo requieren, sin debilitar sus oráculos. Bitácora `progress/tdd_close_work_http.md`.
3. Jason conserva core, evento, recibos, Store, migración, puertos y wiring. No tocar `ApplicationConfiguration`, `ApiErrors`, configuración de mutación ni smoke mientras no se asignen expresamente. Root ya creó `../OrganizacionWeb-close-http`, rama `codex/close-work-http`, desde `5cbe0b6`: todo Java de este paquete se escribirá allí tras la señal de inicio. El plan permanece en COMMON; no se fusionará la rama antigua pause-http.

## Publicación independiente

Primer ciclo: evento real de cierre válido produce entrega con aggregateId de sesión y once campos exactos, sin notas. Continuar un caso por ciclo con validación de campos/valores nuevas: estado de origen, revisión/acumulado canónicos, instante UTC en microsegundos, fecha válida y zona histórica textual. No resolver TZDB ni recalcular la atribución en el publicador. Reutilizar validadores existentes cuando preserven estos límites.

Después comprobar Rabbit real: ruta `work-session.closed.v1`, cola quorum durable `organization.work-session-closed.v1`, persistencia y protocolo existente de confirms, reintentos y blocked. Mantener las diez rutas anteriores y el esquema12 de `WorkSessionStateChanged.v1`. No añadir worker ni cambiar configuración. El smoke de ACK perdido/reinicio/retirada del outbox pertenece a la integración posterior, no a esta prueba de adaptador.

## Dependencias y HTTP

Esperar del handoff las firmas definitivas del comando de cierre, notas normalizadas, unión de recibos y consulta por sesión. El controller transmitirá owner, sessionId, key y token completo UUID/revisión; no hará una consulta previa para sustituir la precedencia transaccional del core.

Primer caso HTTP con mocks de los puertos reales: POST close válido entrega201, Location del cambio y CLOSE7 con closure4. Añadir individualmente replay200, normalización ausencia/null, preservación Unicode y rechazo de tipos, exceso, NUL y surrogates aislados; sintaxis JSON antes de datos. Reutilizar precedencia query/UUID/key/token antes del cuerpo, seguridad y problemas15. Los mocks prueban delegación y representación, no locks ni rollback.

Completar GET closure con recibo original, distinción404 sesión/ausencia de cierre, prohibición de query, no-store y503; comprobar C/K con CLOSE7 y estado closed con State6/snapshot3. Los errores temporales, revisión, estado e idempotencia conservan títulos15. Regresión focal HTTP15 y14, formato real y evidencia XML al freeze; no campaña global propia.

## Compatibilidad que debe conservar el handoff

Actualmente `PostgresWorkSessionStore` escribe el recibo completo con Jackson y lo lee directamente como `WorkSessionTransitionReceipt`; el DTO HTTP `ReceiptResponse` tiene seis campos. La nueva representación debe leer JSONB15 sin `closure` y volver a servir PAUSE/RESUME con exactamente seis campos, nunca `closure:null`. CLOSE sirve siete campos y exige closure4. No reescribir recibos existentes ni alterar SessionStart7/State6. La implementación concreta de la unión corresponde a Jason; HTTP adapta su tipo real sin inventar otra persistencia. Este punto se verificará en regresión tanto por serialización HTTP como en pruebas PG del propietario del Store.
