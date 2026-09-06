# Revisión de regresión heredada11 — corte UI parcial13

**APPROVED en el alcance de extracción y regresión legacy.** Sin hallazgos bloqueantes introducidos en11. Revisión independiente sólo lectura de los objetos Git cf5e3e3..a74a812 (2c8828/215a1c), sin leer fuentes en TDD, ejecutar suites ni modificar tests. Ponytail full/Caveman lite. No aprueba la UI13 todavía parcial.

BlockDetails y BlockTime pasan a block-details.tsx con la misma estructura, textos, atributos time/dateTime, opciones Intl, zona y fallback UTC con ID original. Sólo cambian export/import y formato de firma. Ambos nombres están importados en task-blocks.tsx, incluido BlockTime usado por el preview de creación; el fallo previo por import omitido no está presente en este commit.

La comparación textual de los cuerpos completos confirma BlockEditor idéntico (18.534 caracteres) y BlockConflict idéntico (1.600), salvo separación exterior de funciones. Se conservan gestión de foco, preview/consentimiento, key retenida, recuperación de creación por by-request, clasificación y guardas tras awaits. BlockConflict sigue consultando detalle deliberadamente; no sustituye recuperación de ACK.

La composición mantiene el artículo de confirmación11, recarga independiente del listado y paginación. Las condiciones heredadas task pending/proyecto conocido distinto de completed y el prop eligible no cambian. Las acciones nuevas se ocultan durante edición de creación, y Planificar bloque se oculta mientras hay acción13 seleccionada: evitan dos editores simultáneos sin cancelar la recuperación11 ya abierta. No hay cambios SCSS ni modificaciones de los84 tests legacy en este diff.

Evidencia del autor conservada en el commit: el primer intento de refactor74e138 falló por import BlockTime ausente; se corrigió y la regresión5a2120 terminó87/87 (84UI11+3UI13), tipos9c8ee9 y lint/formato f8bf9f verdes. Este juez no los vuelve a ejecutar ni convierte lectura de código en evidencia nueva de navegador.

El corte declara pendientes movimiento completo, incertidumbre/CSRF de cambios, vigencia posterior, historial y guardas UI13. No se catalogan como regresiones de11 por estar sin implementar en este WIP; requieren sus ciclos y revisión posteriores. La etiqueta histórica de confirmación11 exigida por13 también figura como pendiente del autor y no queda aprobada por este dictamen acotado.
