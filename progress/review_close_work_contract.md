# Revisión independiente del contrato de cierre16

Lectura contra sección16 de project-spec.md, propuesta UX aprobada y composición14/15. Sin implementación, ejecución de pruebas ni edición del Gherkin. Ponytail full y Caveman lite.

Corte inicial verificado37c009: SHA2564D7714F246C94DADC12D1CC7F62BFD463D061786C49B8AA997A508A28B34CC91,41 escenarios/113 casos expandidos declarados por autor; no113 tests ejecutados. Lecturas8afeaf/37c009.

Delta puntual solicitado y posteriormente verificado: @s30 debe probar before paused/after closed coherentes salvo acumulado aumentado por descanso, evitando que after paused haga fallar por status antes de la aritmética. Root también pidió que @s23 identifique sin ambigüedad la migración de feature16 posterior aV16, sin reescribirla. No se solicita ampliar escenarios ni repetir matrices14/15.

## Contraste realizado

- Cierre running/paused, cero, precisiónµs y límite UTC; clock antes de escrituras, revisión máxima antes de clock. WorkDate del cierre persistido, zona histórica/fallback y desbordamiento local rechazado.
- Notas opcionales normalizadas sólo ausencia/null,2000 puntos de código, Unicode válido y rechazo NUL/surrogates antes de propiedad/replay. Recibo CLOSE7/closure4 conserva inicio14; PAUSE/RESUME permanecen6. Publicación11 campos excluye notas.
- Terminalidad, replays anteriores tras nueva sesión, comparación de key owner-global ahora alcanzable entre sesiones distintas, cierre/inicio concurrente y rollback sin liberar plaza prematuramente. Lecturas separadas, errores503 y recuperación F por identidad sin reloj.
- URL estable antes del POST, recuperación tras reload sin key mediante S/F, contexto p/t/s verificado, ninguna persistencia privada en almacenamiento web. WorkDate no se recalcula con Intl; fallback visual no modifica atribución.
- Resultado histórico separado de active nueva; incertidumbre conserva intención y notas;412 conserva borrador con decisión manual nueva sólo desde estado válido. CSRF manual, guards después de await y GET anteriores no restauran estado cerrado ni activa previa.
- Foco, feedback y matriz UX30 remiten a evidencia posterior real, sin certificar personas/dispositivos desde Gherkin. No tarea completada, reloj automático, avisos17 ni historial global18.

No se encontró otro bloqueo contractual. La aprobación documental no equivale a implementación ni a aceptación de pruebas; el gate TDD corresponde al coordinador.

## Dictamen final

**APPROVED.** Delta3d6aa4 verificado: @s30 usa before paused/after closed/runningSince null con suma indebida del descanso; @s23 identifica migración de feature16 posterior aV16. SHA256 final3E45F26004E2656E97443451E2D0368CBC9DE734D859725BD07896E206A44285 coincide con entrega d8f57e del autor. Se conserva el alcance41/113; no quedan cambios pedidos por esta revisión.
