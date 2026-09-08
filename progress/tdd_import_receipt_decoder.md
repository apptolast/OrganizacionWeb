# TDD del decodificador de recibos de importación 23

Autoría aislada: ImportReceiptDecoder y su prueba. Sin SQL, modelos ni mapper global. Firmas block/session acordadas con A. El retorno es un árbol JSON durable nuevo; el input v1 permanece intacto. Fallos de forma, conversión o invariantes se traducen a ImportInvalidFileException.

| Ciclo | Oráculo | Resultado |
| --- | --- | --- |
| 01 | RESCHEDULED: dos instantáneas y revisión >2^53 exacta | RED compilación → GREEN99b7ef |
| 02 | Rechazo de proyecto sustituido en contexto histórico | RED7e99d1 → GREEN572689 |
| 03 | Extras en raíz/request/time no se omiten | 3 REDa1b9c3 → GREEN917acc |
| 04 | Null/array/objeto incompleto, error seguro | 3 REDa72b7f → GREEN739a54 |
| 05 | PAUSE/RESUME/CLOSE/EXTEND, longs exactos y detalles íntegros | RED compilaciónb021f4 → 4 GREEN485ef2 |
| 06 | Contexto, transición y extra de sesión | 3 RED292d21 → GREEN1d7425 |
| 07 | Forma inválida de raíz/before/after | 3 REDf49ea7 → GREEN6e3a13 |
| 08 | Enteros JSON60.0/15.0 sin endurecimiento léxico | 2 RED953d50 → GREENdc5c5b |

El escritor de exportación valida invariantes y produce una representación v1 cerrada. Comparar el roundtrip completo evita que campos desconocidos queden omitidos silenciosamente. Sólo los números JSON se reducen a su entero matemático exacto; BIGINT textuales pasan a long durable. No se normalizan textos ni fechas históricas ni se consulta TZDB.

Checkpoint:20/20, cero fallos/errores/omitidas, Spotless Java verde, EXIT0 5b0c01. Cuatro huellas en import_receipt_checkpoint_freeze.json. Los logs import_receipt_NN_red/green.log y .exit se preservan; no campaña ni prueba PostgreSQL por C. A integrará los contextos desde el staging del archivo y comparará recibos existentes por su representación v1, no por igualdad textual ciega de JSONB histórico.

Pendientes al checkpoint: oráculos adicionales de BIGINT canónico y fracción ultrafina; no defecto funcional reconocido. No atribuir todavía aplicación integral14/14 ni aceptación.
