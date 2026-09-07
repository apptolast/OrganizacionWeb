# Sesión actual — implementación de pausa/reanudación

Feature 14 está done y fusionada en main 353c9d444b2af8bfeca63b8b166b559848d42963 mediante PR13. CI34070183747 SUCCESS sobre deca259 exacto antes del merge; fetch384a27 confirmó árboles idénticos. Informe ci_start_work_final.md. La CI del nuevo main34070995379 terminó SUCCESS, verificada por root5378c7 sobre el SHA completo. Watch6677 acabó EXIT0; el log queda en OrganizacionWeb-start-work-final/progress/ci_start_work_final_main.log.

Cierre14: judge_start_work_final.md APPROVED; 104E2E, smoke real de respuesta perdida/Rabbit/reinicio sin outbox publicado, UX412 medidas y28axe con límites físicos. PIT original340/347 KILLED estrictos (97,9827%),5S/1NC/1TIMEOUT; Stryker483/539 (89,6104%),56S y cero errores. Replay adicional11/11 KILLED, EXIT0,289 hashes idénticos, cinco supervivientes detectados por el oráculo JSON independiente. No se mezclan denominadores ni se ocultan residuos.

Feature15 está in_progress. Especificación e3b61ef/eb1a9bb, contrato a5c556f:39 escenarios y105 casos declarados, aprobación independiente review_pause_resume_contract.md y root. Work-Session-Revision es cabecera propia con id/revisión, sin comillas; no ETag fuerte del snapshot que cambia con el reloj. Recibo14 permanece inmutable; paused ocupa la única plaza; neto provisional exacto en microsegundos, fin previsto fijo. No iniciar16 antes de cerrar15.

## Reparto y árboles

- OrganizacionWeb-backend, rama codex/pause-resume-session. HEAD514c996 conserva un checkpoint parcial compilable de núcleo, tipos y tres tests nominales; no declara formato/gates ni función completa. Backend A+B escribe núcleo, consultas, Store y V16; frontend trabaja cliente/panel15 e integración14. Root controla Git, revisión y documentación de sesión.
- OrganizacionWeb-pause-http, rama codex/pause-resume-http desde514c996. Autor C HTTP/publicación trabaja aisladamente para evitar esperas de compilación. Tiene tipos/evento reales de ese checkpoint; nuevos puertos de lectura se coordinarán al congelarse. No fusionar toda la rama si incorpora snapshots de dependencias posteriores: seleccionar sólo adaptadores/tests/bitácoras revisados.
- OrganizacionWeb-start-work-final conserva la rama publicada de cierre14 y su seguimiento CI; no se usa para escribir15.

El traslado C preservó sus tres archivos iniciales y hashes3178a6 en backend/build/handoff/publisher-15 del aislado. Una comparación inicial377b59 se detuvo sin restaurar porque C había avanzado allí el siguiente test. Root028179 verificó copias inmutables y sólo después restauró OutboxMessage/PublishOutboxTest comunes a14 y retiró la bitácora común ya preservada. Los originales también están en el trabajo aislado; no integrar archivos build/handoff. C no escribe common.

Avance parcial: publicador integrado selectivamente en4b5a254 (6ddee0), cliente revisado enab6b4f7. Backend acredita51 casos propios (9 núcleo,5 snapshot,4 recibos y33 PG), incluidos replay, atomicidad, privacidad y concurrencia con locks observados; upgrade real V15→V16 pasa7e0c8b. Sigue restricciones acotadas, colisión esperada, wiring y formato. Root948927 verificó22fixtures: sólo agregan dos tablas hijas a TRUNCATE, ninguna aserción cambia; V14/V15 sin diff. Primer intento f9ed3b falló por resolver rutas relativas desde raíz incorrecta, sin escritura.

HTTP15 aprobado en review_pause_resume_http.md: rootb47e61 verifica XML97 (48 nuevos+49 HTTP14), cero fallos/errores/omitidas y hashes. Commit aislado5c4841e contiene sólo2Java+2docs; está pendiente de cherry-pick en frontera A. Los cinco snapshots de puertos reales quedan excluidos. C pasa a COMMON: scripts/project.mjs/.test.mjs para dos targets de mutación15 (32 Node verdes acreditados, reviewb3ceb1) y después smoke/E2E @s25/@s26. No ejecutar Docker ni campaña sobre build incompleto. A posee build.gradle.kts, frontend su configuración Stryker. Root revisa e integra.

Frontend acredita23 pruebas de panel y43 de composición14 verdes b458e6; siguen guardas tras await y presentación antes del freeze. Root sólo ha aprobado el cliente, no UI ni integración15 completa. No lanzar suites globales por microcheckpoint ni usar el selector de mutación14 como cobertura15.

## Próximos pasos

Completar núcleo/consultas/PG, HTTP/publicación y cliente/panel con TDD individual; integrar wiring en frontera compilable, revisar código, recorrer API/PG/Rabbit/UI reales y ejecutar gates finales15. Las firmas nuevas se comunican antes de editar consumidores. No reescribir migraciones publicadas V14/V15. V16 añadirá hijos y requiere actualizar fixtures TRUNCATE explícitos; no limpieza global ni CASCADE como atajo.

Para el MVP siguen pendientes15–18 y despliegue: pausa/reanudación, cierre con tiempo neto, aviso e historial. El usuario pide hoy; se trabaja hacia ese objetivo sin garantía ni una cifra menor inventada. El intervalo30–60 horas sigue siendo hipótesis anterior de baja confianza, no estimación nueva por dividir entre agentes. Acceso SSH rechazado publickey y dominio siguen pendientes de respuestas ya solicitadas; no repetir preguntas. No habilitar uso habitual antes del cierre16 ni afirmar despliegue.

Ponytail full/Caveman lite; baseline init ya pasó en esta sesión y no se repite por cada fase documental/traslado. Nunca leer ni limpiar .e2e-work/read-review-state.json, .e2e-work/read-review-stop, frontend/.stryker-tmp-availability-replay o progress/proposal_schedule_block_time.md, ni borrar/mover sus ascendientes. No force-push, limpieza global o intervención en el stack existente del puerto8080. La marca M de V14 sin diff de contenido es ruido de metadatos; no reescribirla. El historial14 detallado permanece en progress/history.md y sus bitácoras.
