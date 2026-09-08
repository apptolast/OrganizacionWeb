# Mutación de HTTP y recibos de exportación

## Corte y resultados

Checkout aislado `codex/export-data-http`, c024dab, con wiring real 401a77f. Regresión backend `test spotlessCheck`: EXIT 0, 4m9s, 2818 pruebas / 121 XML, sin fallos, errores ni omisiones (459ab0). XML preservados en `export_http_receipt_backend_gate_xml` y resumen en `export_http_receipt_backend_gate_results.json`.

PIT completo de `ExportDataController*`, `ExportHeadersFilter*` y `ExportReceiptWriter*`, incluidos internos, mediante `export_http_receipt_pit_wired.init.gradle`. Todos los candidatos JUnit, cuatro workers, umbral 80 y configuración oficial restante intacta. EXIT 0 b73a51 en 6m42s: 259 mutantes, 216 KILLED, 43 SURVIVED; cero NO_COVERAGE, timeout o errores. Proporción 216/259 = 83,3977 %. PIT examinó 446 clases de tests y ejecutó 1838 pruebas durante mutación. No confundir esas cifras con las 2818 pruebas del gate.

448 entradas antes/después idénticas (9169d1). Raw XML/HTML íntegro en `export_http_receipt_pit_wired_raw`; inventario conserva estado, clase, método, descriptor, línea, mutador, índices, bloques y descripción para los 259. Los IDs del inventario son ordinales documentales, no IDs propios del motor. `export_http_receipt_pit_wired_residues.json` conserva los 43 estados originales. El primer intento fallido por bean ausente queda separado y no constituye una campaña con puntuación.

## Lectura de residuos y refuerzo selectivo

Tres huecos materiales del oráculo, con producción ya correcta:

- `ExportReceiptWriter.block` línea 214 / índice 128 y `session` línea 42 / índice 93: retirada de validación del contexto anterior. Un nuevo caso parametrizado altera únicamente el proyecto del snapshot `before` de bloque/sesión y exige rechazo sin bytes escritos. Inicialmente GREEN, 2/2, c5ed2d.
- `state` línea 188 / índice 129: omisión de `runningSince` no detectada por el nominal. Los tres recorridos existentes PAUSE/RESUME/EXTEND ahora comparan el campo exacto o null en ambos estados. Inicialmente GREEN junto al siguiente refuerzo, 3/3, 6ec94e.
- `session` líneas 55/62, índices 207/254: el cierre automático del generator individual ocultaba un objeto sin terminar. Los casos existentes CLOSE y EXTEND se escriben dentro de un array con un elemento posterior, verificando ambos elementos. CLOSE inicialmente GREEN 8b735a; los demás 6ec94e. No se simula una confirmación HTTP ni se equipara esto con socket.

Después de esos tres ciclos, sólo cambia `ExportReceiptWriterTest.java`: 58/58 y spotlessCheck EXIT 0 ec34e3, XML `export_receipt_refined_final.xml`. Las tres fuentes productivas y la configuración de campaña siguen iguales. No se ejecuta replay por mejora numérica; los cinco mutantes objetivo conservan su estado SURVIVED original, sin atribuirles detección no medida.

## Límites restantes

Se conservan como limitaciones las fronteras inclusivas de minutos, años y revisiones, las variantes de transición y corruptelas del snapshot `after` que no tienen oráculo individual. Los guardas existen; no se ha demostrado un defecto productivo ni equivalencia general. La revisión no convierte los residuos en cobertura completa de toda corrupción posible.

Hay redundancias observables: las cabeceras del controller también las fija el filtro de ruta; varias comprobaciones `isObject`/presencia preceden accesos tipados que rechazan el mismo árbol. Estas son explicaciones locales de supervivencia, no exclusiones ni cambios de denominador. La omisión de `reset` del wrapper y la selección de wildcard/elementos vacíos de Accept-Encoding permanecen como límites del test unitario actual; requieren contraste HTTP de comportamiento, no un parser alternativo ni cambios preventivos.

Este corte supera el umbral del alcance C y preserva todos los residuos. No acredita el adaptador PostgreSQL completo de A, UI, socket, descarga por navegador ni despliegue. La siguiente validación independiente será HTTP real con puerto aleatorio y PostgreSQL, conservando el universo anterior como evidencia histórica.
