# Destilación import_data23

Fuente normativa aprobada: project-spec.md de bd34881, SHA256
F77B05F11547314A0DDD9600217CB84C4E7BBCBDD210921350472CEAF916ACAB.
Leídos gherkin_author, docs/gherkin, convenciones y revisión técnica import23.
Ponytail full/Caveman lite; no producto, pruebas ni nueva semántica.

42 escenarios estables con una línea When por escenario:

- s1–s7: ausencia, fusión integral, igualdad durable, preferencias y propiedad.
- s8–s15: JSON cerrado, tipos exactos, integridad, recibos, legados e inactivos.
- s16–s23: límites, snapshot, recibos idempotentes, respuesta perdida y rollback.
- s24–s27: writers reales, dos carreras de advisory compartido y timeouts.
- s28–s32: seguridad, precedencia HTTP y Nginx real, chunked y alcance exacto.
- s33–s42: preview/confirmación, recuperación de pestaña, privacidad y UX.

El archivo remite a los campos cerrados14 y recibos aprobados, evitando copiar
sus matrices de campos. Locks de tabla no sustituyen los advisory compartidos;
s25/s26 conservan el hallazgo de pérdida de datos corregido por root. Los límites
SQL son por adquisición/sentencia, nunca SLA global. Pruebas de confirmación
sólo en cuentas locales efímeras; live permite únicamente preview.

Verificación documental:42 tags únicos consecutivos y42 When; JSON de catálogo
válido, únicamente ficha23 modificada; fuente normativa conserva su hash.
No hay parser Cucumber instalado: conteo e inspección léxicos, no ejecución
Gherkin ni tests. Diffcheck limpio. Estado23 spec_ready, pendiente revisiónroot
antes de in_progress/TDD;24–30 y todos los cierres anteriores se conservan.

Revisión final de root: s6 usa registro distinto sin exigir UUID a intervalos; s17 parte de prefijo todavía válido; s23 exige rechazo comprobable del commit, distinto de resultado incierto de red cubierto por s37/s38. Sin cambio de alcance ni nuevos escenarios.
