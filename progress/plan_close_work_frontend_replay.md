# Preparación de replay dirigido frontend 16

Configuración dedicada: `frontend/stryker.close-work-session.replay.config.json`. Preparada por autorización root después del cierre original; no ejecutada todavía. No añade target global ni altera la configuración original. Conserva candidatos Vitest completos mediante vite.config.ts, perTest, ocho workers, umbral global 80 y el ignorePatterns protegido exacto. JSON, HTML y directorio temporal tienen nombres propios de replay.

La selección contiene ocho rangos sobre el Reader original SHA256 C7FB07A1B05B47938ADD0B46EE41E5728687DC92BA7D608970D5C22B9097E4F2:

| Rango línea:columnas | IDs originales objetivo | Conducta |
| --- | --- | --- |
| 232:20–39 | 543 | ID de la descripción de error |
| 242:29–66 | 546,547,548,549 | aria-invalid de progressNote |
| 243:45–64 | 550 | descripción accesible de progressNote |
| 255:29–62 | 554,556 | aria-invalid de nextStep |
| 256:45–64 | 558 | descripción accesible de nextStep |
| 331:12–36 | 611 | abort de GET active al desmontar |
| 357:15–22 | 628 | guardia de retry mientras GET pendiente |
| 358:22–26 | 630 | anuncio y estado de carga del retry |

`close_work_stryker_replay_signatures.json` fija las doce firmas originales por archivo, posición, mutador y sustitución. La correspondencia entre campañas se hará por esos cuatro campos, nunca por ordinal: los IDs locales nuevos pueden cambiar. Esto es correspondencia de identidad, no declaración de equivalencia semántica. Los rangos pueden generar mutantes adicionales dentro de esas expresiones; se reportarán todos y se comprobará expresamente la presencia de las doce firmas. No se anticipa el número generado ni el score.

El original1277 y sus169S/2NC/2RuntimeError permanecen intactos. Este replay no acredita API, State ni los errores de runner del original. El nuevo test de privacidad de B fue revisado por lectura c8bc60/41123e y B informó30GREEN1ac7d7; aún faltan su entrega final de los dos refuerzos y autorización root de ejecución. Configuración creada c27fa5 y formateada405be7, sin campañas ni pruebas ejecutadas por esta preparación.

## Incidente de selección del primer intento

Intento autorizado037327, EXIT0 real379c3b, duración1min11s: sólo2mutantes generados y2Killed. Coinciden con originales549 y557; sólo549 pertenece a las doce firmas objetivo. Faltan las otras once, incluido611: este100% parcial no acredita el paquete solicitado.

Causa confirmada leyendo implementación local (9448ba/0d9c39): project-reader.js filterMutatePattern resta1 a líneas pero conserva columnas del selector, que son base0. reportPositionToStrykerPosition resta1 a las columnas del JSON, base1. La preparación copió indebidamente columnas del JSON al selector. unionFileDescriptions concatena los rangos del mismo archivo; no los sobrescribe. Corrección propuesta: restar1 a ambas columnas de cada rango, manteniendo líneas y los ocho rangos. No aplicada ni repetida sin autorización root.

Original del intento preservado en close_work_stryker_replay_attempt1.json, SHA3931731175486A78BFE89F9EDB3812341AE4F498E6A1B3FBD3801F5ED49E5EF8, con attempt1_config.json y attempt1.log. Sus91hashes before/after permanecen idénticos083689. Este incidente no modifica ni reclasifica la campaña original1277.
