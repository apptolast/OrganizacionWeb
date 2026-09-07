# Replay dirigido de apariencia

Resultado: EXIT0 0a8f27, BUILD SUCCESSFUL en 5m19s. Quince mutantes:
13 KILLED, 2 SURVIVED, 0 NO_COVERAGE, 0 TIMED_OUT y 0 errores.
Denominador estricto 15; 13/15 = 86.6666666666667 %. No se suman a los
159 mutantes de la campaña original, que permanece en 153 K + 6 S.

392 entradas antes/después idénticas, comprobación b4dd29. XML separado en
progress/appearance_pit_replay_original/mutations.xml, SHA256
51E7FA671084073A1B95D8B04C44870DA939EFC86923F7383DDDE3A1D7817E8D.
La copia original conserva SHA256
293593AC0AFB54D77BDDC2B624AC92503C904764E970BF598998BB466E2EDA5E.
Los raw/logs permanecen locales y no se incluyen en el commit de entrega.

## Comparación completa

appearance_pit_replay_inventory.json conserva las quince firmas y su estado
original/replay. Cada firma tiene correspondencia única por clase, método,
descriptor, línea, mutador, index y block; comprobación f9c66c.

| Objetivo | Firma | Resultado |
| --- | --- | --- |
| Canal lineal | AppearanceValues.channel:62, MATH index23 | SURVIVED a KILLED |
| Offset de contraste | AppearanceValues.contrast:53, MATH index16 | SURVIVED a KILLED |
| Umbral exacto 4.5 | AppearanceValues.color:40, boundary index44 | SURVIVED |
| Lectura año9999 | PostgresAppearanceStore.lambda$select$0:45, boundary index88 | SURVIVED a KILLED |
| Escritura año9999 | SaveAppearance.timestamp:66, boundary index23 | SURVIVED a KILLED |

Diez acompañantes: los nueve KILLED originales permanecen KILLED; el boundary
AppearanceValues.channel:62 index20 permanece SURVIVED. Ningún nuevo residuo.
El canal byte/255 nunca es exactamente 0.04045: requeriría byte10.31475.
El boundary de 4.5 queda como limitación; no se ha demostrado un color público
exacto ni equivalencia finita, y no se inventa un fixture para eliminarlo.

## Dictamen acotado

Los tres oráculos autorizados acreditan los cuatro cambios observables sin
modificar producción. Ambas campañas superan el umbral80 conservado. No hay
hallazgo productivo demostrado ni justificación para otra campaña o matriz.
La revisión/UI/E2E de la feature se evalúa por separado; esto no declara done.
