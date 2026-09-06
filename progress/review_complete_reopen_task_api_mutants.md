# Revisión independiente de mutantes del cliente API

Coordinador, original mutation-original.json de complete_reopen_task. El autor de API fue integración; los refuerzos los ejecuta frontend. Se revisan doce supervivientes de task-status-api.ts, sin atribuir resultados de replay todavía.

| ID original | Dictamen | Evidencia y acción |
| --- | --- | --- |
| 282 | Equivalente | Suprimir typeof object conserva rechazo de null y arrays; primitivas JSON carecen de los campos exigidos y fallan longitud o validación posterior con el mismo error controlado. |
| 296 | Brecha | Al suprimir typeof string, una fecha representada por array u objeto con toString nulo puede lanzar TypeError en coerción o replace. Añadir respuesta JSON incompatible y exigir el error controlado del cliente. |
| 299 | Equivalente | Sin ancla inicial de fecha, cualquier prefijo permanece en el lado derecho del cotejo exacto con ISO canónico; no se acepta como fecha válida. |
| 300 | Equivalente | Sin ancla final, el sufijo permanece en el cotejo ISO exacto. Un Z intermedio tampoco desaparece por la sustitución anclada al final. |
| 303, 304, 305 | Brecha | Rechazan años expandidos válidos que el validador original admite. Verificar representación ISO expandida, sin ampliar precisión ni prometer que todos los Instant de Java caben en Date del navegador. |
| 322 | Equivalente | La expresión completa previa admite un solo Z final; quitar el ancla en su sustitución no cambia qué sufijo se elimina. |
| 411 | Brecha | Quitar typeof string permite cursor array no vacío u objeto con length positivo. El contrato exige string no vacío o null. |
| 437 | Brecha | Quitar la guarda textual del UUID puede provocar coerción fallida para un objeto JSON; debe conservar error controlado de historial. |
| 440, 441 | Brecha | Quitar cada ancla permite prefijo o sufijo alrededor de un UUID válido. Añadir ambos límites sin alterar producción. |

El timeout 475 pertenece a tasks-api.ts y requiere replay independiente. El 89,46 % original incluye un Timeout: no se describe como 416 fallos detectados por aserción. Contadores originales: 415 Killed, 1 Timeout, 49 Survived y cero NoCoverage. Los resultados selectivos posteriores tendrán su propio denominador.

Replay principal leído por el coordinador: correspondencia exacta sensible a mayúsculas entre archivo, ubicación, mutador y replacement. Los ocho objetivos API 296/303/304/305/411/437/440/441 figuran Killed. En particular 305 corresponde a replay 42: se distingue correctamente `\D` de `\d`. La misma comprobación confirma los quince objetivos UI lógicos como Killed. Son 23 identidades originales resueltas; no un recálculo de la campaña original. Los espacios 67/212 y timeout 475 siguen en replay separado.
