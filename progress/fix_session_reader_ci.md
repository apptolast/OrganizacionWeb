# Corrección del fixture de Reader después del CI de main

Corte base d0fb20e, rama codex/session-reader-ci creada por root. PreCI34115700428 verde y postmain34117519068 falló1/1899 en `@s36 feature17 releases a definitively rejected closure so the user can choose extension after refresh`; log original history_main_ci_failure.log. No se modificó producto por el fallo.

## Causa y reproducción

El test esperaba el campo de notas, que sólo acredita GETstate(S). Enseguida pulsaba CLOSE mientras GETend-time(E) podía seguir pendiente. La adquisición de CLOSE aborta lecturas hermanas, correctamente. Tras412 se libera la decisión; «Consultar estado actual» refresca S, no promete refrescar E automáticamente. Cuando E fue abortado antes de almacenar snapshot, el panel muestra «Actualizar fin acordado» y no «Ampliar tiempo». Ese es el DOM exacto del CI. El contrato no obliga al botón S a ejecutar también E.

El caso original aislado fue inicialmente GREEN7e6609 (`session_reader_ci_initial.log`), no reproducción por azar. Se hizo E explícitamente diferido en el único fixture y se entregó después de pulsar CLOSE: RED determinista5d171d (`session_reader_ci_deferred_red.log`), mismo botón ausente después del refreshS. No timeout mayor, retries de test ni supresión de aserciones.

## Cambio acotado

Se conserva E diferido y se resuelve/observa «Ampliar tiempo» ANTES de CLOSE. Así la precondición de fin conocido es explícita y el oráculo aísla su responsabilidad: liberar la decisión definitivamente rechazada para que otra acción pueda adquirirse. Se conservan todas las aserciones finales de envío único de ampliación y borrador de notas. No se transforma la lectura abortada en éxito ni se asume que consultar S consulta E. Ningún cambio de producción, otros tests, políticas de error o temporizadores.

## Validación y freeze

Reader completo47/47 EXIT0 5bb54d (`session_reader_ci_green.log`). Tras formato focal, frontend completo una vez1899/1899 en40suites,22,78s, y lint completo EXIT0 ca633c (`session_reader_ci_full.log`, `session_reader_ci_lint.log`, cada uno con EXIT_CODE=0). No build, Java, E2E ni mutación adicionales, conforme autorización de root.

Manifiestos `session_reader_ci_before.json` y `session_reader_ci_after.json` cubren134entradas frontend de código, tests y configuración:0diferencias durante la regresión. El único archivo fuente del cambio es frontend/src/work-session-reader.test.tsx; esta nota y los manifiestos preservan evidencia. WIP de metadatos V14 ajeno no se tocó. Root gestiona revisión/Git/CI; paquete congelado sin más expansión.
