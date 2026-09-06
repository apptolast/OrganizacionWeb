# TDD frontend13: reschedule

Contrato d1ff6098a8117 aprobado; frontend único autor, backend independiente. Init78050 vigente según root, sin init concurrente. Ponytail full/Caveman lite. Ownership src frontend/tests y esta bitácora; no config/harness/E2E. Estado: en curso, no veredicto.

## Cliente: ciclos reales

1. @s1/@s4 leer state con ETag BIGINT exacto y petición privada única. REDd97118 import ausente, GREEN1f917e (1test). Mínimo cliente state.
2. @s31 matriz cerrada de estado/ETag incompatible. REDd09a84 aceptación de null, GREEN388061 (2tests). Reutiliza isBlock/exact/instant11 y revisión textual con límite BigInt. Matriz agrupada en un test, sin atribuir13ejecuciones individuales.

Siguientes: status HTTP/privacidad, preview, recibos y recuperación, historial; después UI/composición según design_reschedule_ui.md. Los escenarios de persistencia/eventos pertenecen al autor backend; todavía no hay cobertura completa ni UI13.
3. @s18 estados HTTP fallidos con cuerpo válido: RED12aa01, GREEN847012 (3tests).
4. @s7 preview de movimiento con If-Match y cinco campos: REDcf327a, GREENc7c156 (4tests).
5. @s31 rechazo de preview incompatible: REDfb8d39, GREEN7258a6 (5tests). Exporta isPreview11 sin cambiar su lógica.
6. @s4 ruta UUID mayúscula con respuesta canónica y BIGINT máximo: RED85edad, GREENb3aaf7 (6tests), más formato focal. Exporta/reutiliza sameId11; no relaja ETag canónico.
7. @s12/@s13 cancelación sin disponibilidad, reloj anterior permitido: REDbd2104, GREEN78eb4f (7tests).
8. @s32 esquema/identidad/revisión de recibo cancelado y Location POST: REDe41681, GREENcfa34f (8tests).
9. @s11/@s15 movimiento y replay POST200 con headers exactos: RED262b57, GREEN271638 (9tests).
10. @s29/@s32 preview no coincide con intención retenida: REDaebbdf, GREEN6a3a02 (10tests), rechaza antes de transmitir.
11. @s25/@s34 GET recibo por key sin exigir Location: RED21a34a, GREENeaf272 (11tests). Recuperación compara intención, conserva código HTTP; no consulta detalle ni estado actual en lugar del recibo.
12. @s37 copia estable de intención entre awaits POST/check: RED3ed624, GREEN4f5d2a (12tests); structuredClone de datos retenidos.
13. @s16/@s39 lectura paginada de cambios con cursor codificado: REDf8d590, GREENb1f08c (13tests).
14. @s16/@s31 historial y recibos incompatibles: RED00bcaf, GREEN10d2a6 (14tests); validación cerrada before/after e identidades invariables.
15. @s32 revisión respuestaMAX+1 tras lecturaMAX válida: RED466629, GREEN37188e (15tests). No se invalida lecturaMAX; recibo imposible se rechaza.
16. @s18 GET recibo por id sin Location: RED1b095d, GREENb7210c (16tests).
17. @s33/@s35 cinco errores nuevos con shape/status cerrado: RED517013, GREENfd62c3 (17tests).
18. @s35 errores heredados CSRF/presupuesto: REDf14219, GREEN393251 (18tests), composición readBlockError sin relajar lógica heredada.
19. @s16/@s18 HTTPfallido/duplicados/orden historial: RED5042a7, GREENb2d233 (19tests); comparación de instante conserva microsegundos y desempateUUID.

## Corte cliente para revisión independiente

Formato focal y eslint/tsc sin errores b139b1. Regresión cliente nuevo19 + cliente11 existente250:269/269 GREEN0f2e3a. Sólo exports sameId/isPreview en schedule-block-api.ts; su lógica no cambia. API13 queda estable para revisión mientras sigue TDD UI. No significa feature completa ni cobertura de persistencia/E2E. Las matrices internas son grupos de variantes de un test, no se cuentan como ejecuciones adicionales. Todavía faltan oráculos públicos de UI/guardas y posteriores límites que señale revisión.

## Primer corte UI y pausa coordinada

20. @s28 abrir un panel inline desde una consulta deliberada de state; valores locales de Madrid independientes del navegador y cerrar sin POST. RED2bfc4d, GREENf5ba46 (1testUI).
21. @s31 lectura de state fallida no habilita editor, recuperación explícita. REDac3270 (incluyó rechazo no capturado en montaje aún mínimo), GREEN0f0bc9 (2testsUI).
22. @s12/@s36/@s38 cancelar reserva de tarea/proyecto completed sin disponibilidad; retirar fila, confirmar hecho histórico y enfocar encabezado si desaparece origen. RED7d411c, GREEN7c3ed0 (3testsUI).

Refactor en verde: BlockDetails y BlockTime movidos sin cambiar lógica a block-details.tsx para reutilización. Primera regresión74e138 EXIT1:52fallos/35pass y47errores debidos al import BlockTime omitido en el editor11. Diagnóstico69b221 y corrección de importf7524c. Regresión nueva5a2120 EXIT0:87/87 (84UI11+3UI13),31,93s. No se oculta la ejecución fallida ni se modifica ningún test11 para hacer pasar ese refactor.

Static checks: tsc9c8ee9 EXIT0. ESLint8c56ad señaló sólo import act no usado en testnuevo; retirado, eslint y Prettier focalesf8bf9f EXIT0. No se repiten tests por eliminar import sin uso.

Pausa solicitada por root al detectar otro equipo trabajando backend13. El usuario autorizó paralelo sin pisarse; root prepara rama Codexfrontend. Corte local estable, sin procesos de pruebas vivos al entregarlo. No se han hecho commits/push/cleanup desde este autor. API aprobada queda intacta desde corte269GREEN; nueva UI aún parcial y NO aprobada globalmente.

Archivos UI del corte: src/task-blocks.tsx (acciones y confirmación), src/reschedule-block.tsx (panel inicial/mínima cancelación), src/reschedule-block.test.tsx (3tests), src/block-details.tsx (extracción). No hay SCSS nuevo todavía.

Pendientes reales tras reanudación: preview/formulario completo de movimiento y selector/zonas no reconocidas; recuperación/errores y CSRF de cancelación/movimiento; vigencia state después de recibo y confirmación histórica11; historial inline; elegibilidad/estado cancelled y guardas de generación/ruta/401; foco/teclado/UIUX. En particular cancelación mínima todavía no captura errores ni conserva key ante incertidumbre: no se presenta como final ni debe publicarse producto por este WIP. No queda testRED activo tras el corte; siguiente comportamiento necesita su propio RED.
