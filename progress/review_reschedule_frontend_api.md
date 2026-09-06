# Revisión independiente — cliente API de reschedule

**APPROVED en el alcance del cliente API congelado.** Sin hallazgos bloqueantes. No aprueba UI, persistencia, publicación, mutación ni cierre de feature13. Rol judge independiente de esta autoría; Ponytail full/Caveman lite. Sólo lectura de fuentes/tests, sin Vitest, Gradle, init ni cambios de implementación.

## Corte y evidencia

Contrato d1ff609,41 escenarios/156 casos. Lecturas4437d2/a86d32/f579ef: reschedule-api.ts, sus19 tests, los dos exports de schedule-block-api.ts y ciclos1–19 de tdd_reschedule_frontend.md. Evidencia del autor: formato/ESLint/tsc b139b1, regresión269/269 (19 nuevos+250 API11) 0f2e3a, diff-check1c4608. Son ejecuciones comunicadas y documentadas por el autor, no un rerun de este juez. Las matrices dentro de cada test no se cuentan como ejecuciones separadas.

Hashes SHA256 leídos86959f:

- reschedule-api.ts: D4EA806C882F8372B1E5BCD10BF0992836E9D578FBC22FB75B3A533056CFAAB7.
- reschedule-api.test.ts: E423582880F1DE6FDE4F2258488738E654110393B0CD9DD5CEDE1D04542648AF.
- schedule-block-api.ts: 6CCA4B6A14620B2F30483CB2774292C784F7DCCB0EF2090EFAF16C426DA26D0C.

## Contraste contractual

- **Estado y revisión (@s4/@s18/@s31, líneas24–64).** Estado cerrado de tres campos de red, Block9 contextual y UUID case-insensitive heredado, enum por identidad sin coerción de valores, instante validado y ETag fuerte canónico. El cliente añade revision sólo a su modelo local. BigInt compara el máximo exacto y conserva texto; no utiliza Number. La ruta mayúscula acepta el ID canónico de respuesta sin relajar sintaxis del ETag.
- **Preview (@s7/@s31, líneas66–102).** Petición sólo de cinco campos y If-Match. Copia los valores escalares de propuesta/objetivo y revisión esperados antes del primer await. Reutiliza isPreview11 y exige el mismo ETag; una modificación posterior del objeto de entrada no cambia la comparación. No introduce catálogo TZDB de navegador ni reglas distintas de duración/DST.
- **Confirmación y recuperación (@s11–s15/@s25/@s32–s34, líneas120–219).** StructuredClone captura estado, intención y preview antes de transmitir o consultar por key. POST valida coherencia preview/input antes de enviar; cabeceras y cuerpo corresponden al tipo, sin Availability-Revision en cancelar. matchesChange comprueba siete campos, contexto, kind, before completo, after exacto según preview o null y revisión sucesora mediante BigInt. No exige reloj creciente. La validación de rango del recibo impide aceptar MAX+1: el ciclo15 conserva RED466629/GREEN37188e. Location se exige sólo en POST201/200; recuperación GET usa la key y valida el recibo sin exigir ese header ni sustituirla por estado/detalle actual.
- **Historial y lectura de recibo (@s16–s18/@s39, líneas225–303).** Lista cerrada con máximo20, recibos cerrados y before/after con contexto e identidad invariable. Desempate UUID y duplicados se comparan normalizados; timeKey conserva microsegundos al ordenar, sin pérdida por Date.parse. No ordena por revisión ni exige monotonía del reloj. GET por id además exige ese changeId. El cursor se trata como token opaco no vacío y se codifica al transmitir; la validación base64url/colección/contexto del cursor, lookahead21 y terminal20 son obligaciones del servidor, no se atribuyen a este cliente.
- **Errores (@s19/@s33/@s35, líneas305–341).** readBlockError se reutiliza primero sin cambiarlo. Los cinco códigos nuevos requieren objeto cerrado, URN, status de cuerpo/HTTP correspondiente y texto; desconocidos o incompatibles producen null y preservan incertidumbre para el caller. La lectura usa clone y no consume el cuerpo original ni relaja CSRF/presupuesto heredados. Los estados HTTP incorrectos no se convierten en éxito por llevar un cuerpo con apariencia válida.
- **Superficie compartida.** El diff de schedule-block-api.ts sólo exporta sameId/isPreview y formatea la firma; su lógica permanece intacta. No hay nuevo validador paralelo ni dependencia.

## Límites del dictamen

Los métodos transportan AbortSignal y usan apiRequest, credentials same-origin y cache no-store. La preservación de intención entre awaits queda cubierta aquí; no demuestra por sí sola que el componente aborte antes de un401 antiguo, retire datos al logout o ignore resultados tras navegación. Esos oráculos @s37, el manejo visual de errores/CSRF, confirmación histórica separada de estado vigente y foco pertenecen a la UI todavía en TDD y requieren revisión posterior.

No se acredita comportamiento real del servidor, carreras, paginación SQL, E2E ni muertes de mutantes con esta lectura. No se solicita repetir la suite durante trabajo concurrente; coordinador realizará la integración en su frontera común. Este archivo conserva un dictamen parcial del corte descrito.
