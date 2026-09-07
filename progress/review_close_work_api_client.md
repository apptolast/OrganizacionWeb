# Revisión del cliente API16

APPROVED como paquete API, sin aprobar todavía UI ni cierre completo. Root65cbc9 revisó el delta y los18 oráculos nuevos;109 pruebas API14/15/16 verdes según999bdf y12b5ab, con tsc/formato/ESLint focales. Root6b0a5d verificó fuente B009E16A92D6A5C5250032F1F0A71D7BFF9F78FCCF656429726ECDA4D5711663 y prueba B688D28B23C54604B65C2E64D507D4170FE2532773B41F648D6192B864B3B039.

La unión añade closed y CLOSE7/closure4 manteniendo PAUSE/RESUME6. Reutiliza validación exacta de objetos, identidad, revisión y aritmética BigInt. Cierre desde running añade sólo el último tramo; desde paused conserva el acumulado. Las notas son strings de hasta2000 puntos de código válidos, con NUL y surrogates aislados rechazados. Fecha persistida válida sin recalcular con Intl; recuperación F verifica sessionId y K compara también las notas retenidas. Un503 se conserva como fallo, no ausencia.

Se pidió fortalecer cuatro negativos de notas porque sameNotes podía hacerlos pasar por diferencia de intención. El autor los trasladó individualmente a GETclosure200, que no compara intención; fueron inicialmente verdes y la fuente no cambió. Root verificó que ahora aíslan el validador de recurso. Los ciclos RED originales quedan registrados sin inventar otros.

Quedan por revisar lector, URL antes del POST, formulario, incertidumbre/412/CSRF, generaciones y foco, composición y E2E/mutación. No se atribuye cobertura completa de41 escenarios ni pruebas reales de navegador a este paquete.
