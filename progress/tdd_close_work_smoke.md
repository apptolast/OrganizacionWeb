# Smoke real close_work_session

Extensión del runner único publisher-smoke.mjs tras el flujo15. No fuente Java modificada. Reutiliza relay que comprueba201 upstream antes de destruir respuesta; detiene Rabbit, recupera CLOSE por key, comprueba estado/recibo7/notas, evento11 sin notas, reintento tras recuperación y cola quorum11. Sólo después de comprobar entrega real retira ese outbox published; reinicia API y exige C/K/F y replay200 idénticos, inicio histórico, neto terminal y conteos invariantes de sesiones/cambios/intervalos.

Aserción de neto independiente mediante suma PostgreSQL de intervalos en microsegundos. Puerto dinámico y proyecto Docker propio; cleanup heredado acotado a su stack/scratch. Cambio necesario a helpers previos: allowlist de outbox y esquema esperado de assertMessage incorporan únicamente WorkSessionClosed.v1.

Sintaxis node --check GREEN ee6688; incidente de formato: prettier no está en dependencias raíz, por eso ese comando terminó1 sin ejecutar smoke. Se usó el formateador ya instalado en frontend, GREEN21109a. No se interpreta ese incidente como RED funcional. Snapshot de fuentes y script en close_work_smoke_before_hashes.json; resultado de primer recorrido pendiente.

## Resultado y freeze

Primer recorrido completo inicialmente GREEN: ejecución2318b7, sesión46148, EXIT0 b2d649; log973c56 contiene PASS16 además de regresiones14/15 y eventos anteriores. No RED funcional fabricado ni cambio de producción. Tramo16 confirma upstream201 antes de perder ACK, pending/BROKER_UNAVAILABLE mientras Rabbit está detenido, mismo eventId/payload11 publicado tras retry y recibido en cola quorum durable11/routing correcto. Las notas sólo permanecen en recibo; igualdad completa de payload impide filtrarlas en evento.

Neto contrastado contra suma real de intervalos PostgreSQL; sesiones1, abiertas0, cambios3, intervalos2. Después de comprobar publicación real se elimina únicamente el outbox16 published y se reinicia backend: misma identidad de contenedor, StartedAt diferente, PostgreSQL/montajes sin reinicio. GET C/K/F y replayPOST200 conservan exactamente recibo/notas/revisión; estado terminal, inicio14 y conteos permanecen iguales, events16=0. El cleanup aislado terminó con EXIT0. No se usó18080 ni se tocó Java durante PIT.

323 hashes before/after idénticos fd1815 (backend/src/main, frontend/src y runner). Script SHA256 ACDCD7AD58C290DE9C5504E31A250894ABFCDBD9BB1D10BB5DEA5FAE7893013E. Log SHA256 0D1A3F9BEECEA0921B871CDA3280C65167865400DC85BCC2F3833060C6CE85E5. Formato final Prettier check GREENfd1815. Archivos congelados: scripts/publisher-smoke.mjs; evidencias en tdd_close_work_smoke.md, close_work_publisher_smoke.log y close_work_smoke_before_hashes.json/after_hashes.json.

Límites: un recorrido CLOSE desde running sobre los PAUSE/RESUME reales15; la aritmética paused y otras carreras se acreditan en suite backend, no se duplican aquí. La entrega del broker conserva semántica al menos una vez; no se afirma exactly-once.

Nota de coordinación posterior al afterhash fd1815: root autorizó después un ajuste UX de work-session.scss para tamaño44px de «Volver». La identidad323 corresponde al cierre efectivo de este smoke, anterior a ese cambio; no certifica que el corte futuro tras CSS siga teniendo esos mismos hashes. El ajuste visual queda fuera de publicación/recuperación backend y no obliga a repetir el recorrido.

Corrección de trazabilidad solicitada por root: la línea console PASS del script medido conserva por error las etiquetas @s29/@s41. La evidencia real del recorrido corresponde a @s28 (recuperación tras ACK perdido/reinicio y retirada de outbox publicado) y @s29 (publicación con broker disponible/recuperado). No acredita UX @s41. Se conserva intacto el script/hash medido; esta nota corrige únicamente la atribución documental.
