# Refuerzo dirigido de revisión semanal

La campaña original se conserva: 615 Killed, 144 Survived, 2 NoCoverage, 3 Timeout y 2 RuntimeError. No se ha repetido ni reclasificado. Los seis refuerzos autorizados ejercitan protección existente; ninguno exigió cambiar TS productivo.

| Fila | Oráculo | Primera ejecución |
| --- | --- | --- |
| 1 | Catálogo pendiente, desmontaje, HTTP 401 entregado antes del observador: no revoca acceso | GREEN 100b3a, `weekly_review_refine_1.log` |
| 2 | Primer reintento vuelve a 503; segundo manual consulta la misma selección y obtiene semana | GREEN 4ac38e, `weekly_review_refine_2.log` |
| 3 | Foco voluntario en fecha y después body: la respuesta no lo lleva al encabezado | GREEN e761cc, `weekly_review_refine_3.log` |
| 4 | Semana martes–lunes completamente coherente, salvo requisito de lunes | GREEN 0e544c, `weekly_review_refine_4.log` |
| 5 | Un presupuesto diario null y seis ceros con total conocido: rechaza | GREEN 2ec9e8, `weekly_review_refine_5.log` |
| 6 | Siete presupuestos positivos de 1 con suma 7: acepta | GREEN 8fa0fa, `weekly_review_refine_6.log` |

Root detectó que el refuerzo de foco había sustituido el resultado final del caso anterior. Se conserva ahora el mismo cuerpo parametrizado con dos filas: permanece en el control / pasa a body. Ambas pasan inicialmente, d60265 (`weekly_review_refine_3_preserved.log`). No se duplicó el fixture ni se retiró el destino final original.

Foco final: 69/69 (39 API y 30 UI), `weekly_review_refined_final_focal.log`. Global anterior al añadido de la fila conservada: 1967/1967 en 42 archivos, EXIT 0 6e2f8c, `weekly_review_refined_global.log`; no se atribuye ese global al último delta de una fila. Lint/build previos EXIT 0 950f99, `weekly_review_refined_lint.log` y `weekly_review_outline_build.log`. Lint final en `weekly_review_refined_final_lint.log`.

## Propuesta, todavía sin ejecutar

`weekly_review_refined_replay_proposal.json` contiene siete hashes, siete nodos AST completos mínimos y 19 firmas originales identificadas por archivo, ubicación, mutador y replacement. El generador documental `weekly_review_refined_replay_proposal.mjs` verifica nodos exactos con TypeScript. Las columnas de la ubicación original del reporte son de base uno; se convierten a columnas de base cero del scope, con líneas de base uno. Validación 9159a0. No hay config de replay creada.

Se proponen los siete nodos, heredando configuración original (8 workers, perTest, todas las suites, umbral 80 y mutadores/ignores intactos). Los mutantes extra que se generen se informarán aparte. No se promete que las 19 firmas terminen Killed: algunas variantes de foco podrían conservar observables; se juzgará el resultado sin nuevos refuerzos automáticos.

El ID 157 (sustituir sólo `monday === null` por false) se retira de la lista de objetivos diferenciables del caso martes: ese caso mantiene `monday` no nulo. El nodo completo lo puede generar como extra. No se le atribuye cobertura nueva ni equivalencia universal. Los límites P2, los dos RuntimeError y los tres Timeout originales permanecen como documentados en `mutation_weekly_review_frontend.md`.

Versionar selectivamente los dos tests y documentos/JSON de propuesta. El generador es evidencia reproducible de los rangos, no parte del arnés productivo. Raw original intacto. No nuevos targets, campañas ni cambios de producción TS.
# Ejecución autorizada

Root autorizó los siete rangos exactos. Se creó `frontend/stryker.weekly-review-replay.config.json`, SHA 62159323262319FF9DCCF36A325E9ACB7487879E65BB52D58934184CBA7B49A2. Comparación automática de todos los campos contra la base: sólo cambian mutate y salidas JSON/HTML/temp. Se mantienen 8 workers, perTest, suites, mutadores, ignores y umbral 80.

Global final, incluida la fila original de foco y el fixture A b733619: 1968/1968, 42 archivos, EXIT 0 0d4523 (`weekly_review_refined_final_global.log`, 31,86 s). Este resultado sustituye la reserva del global anterior.

Before efectivo: `weekly_review_replay_before.json`, 142 entradas, SHA FC2A58512B400D7B56EF921D3E074F77DE654D4980DB27BFF04AB453F663D7E3. Las siete fuentes/config de la propuesta y las 142 entradas se verificaron antes de lanzar. Comando directo autorizado sin target nuevo: `pnpm --dir frontend exec stryker run stryker.weekly-review-replay.config.json`. Sesión 37993; log `weekly_review_stryker_replay.log`, metadatos horarios en `weekly_review_replay_started.json` y salida en `weekly_review_replay_exit.json`. Fuentes/tests/config congelados hasta after.
# Resultado final del único replay

EXIT 0 f42c55, 2 min 45 s. Global estricto del replay: **38/46 Killed = 82,6087 %**, 8 Survived y cero Timeout, NoCoverage, RuntimeError o CompileError. API 21 K/2 S; UI 17 K/6 S. La campaña original permanece intacta y no se recalcula su porcentaje con este subconjunto.

Mapping exacto 149444: **19 firmas originales = 17 Killed + 2 Survived**; **27 extras = 21 Killed + 6 Survived**. Los 142 hashes before/after son idénticos, cero diferencias. Raw preservado `weekly_review_stryker_replay.json`, SHA E04E82F1B84A07739D26618707E56428E03767BBC58DF4B562F56FF57C239032. Inventario completo, ubicaciones/replacements y separación objetivo/extra en `weekly_review_replay_mapping.json`; after en `weekly_review_replay_after.json`. El original sigue con SHA 0FC84106ADDF2EB0A31793DA756242CAC037494731E70A276D64625DC36FBF52.

## Ocho supervivientes, sin otra campaña solicitada

| Original / replay | Residuo | Lectura acotada |
| --- | --- | --- |
| 395 / 29 | Listener borra iniciador siempre | Los handlers capturan el iniciador después del evento de foco. El foco dirigido a otro control sí lo borra. La diferencia exige un evento de foco posterior dirigido al mismo iniciador retenido; los recorridos observados no la producen. No se acredita equivalencia universal. |
| 413 / 43 | Omite comprobar que activeElement sea body | La retirada del iniciador y el listener de foco conservados bloquean los recorridos control/body probados; son defensas solapadas. No se demostró robo de foco sobre fuente actual. |
| extra 3 (original 157) | Omite monday===null | El caso martes no aísla null, como se anticipó. La validación civil anterior permanece; no se reclama nuevo oráculo para este mutante. |
| extra 8 | Resta 7 en normalización modular | Cambiar +7 por −7 conserva la divisibilidad por 7 utilizada para comprobar lunes, incluidos restos negativos de BigInt. Equivalencia aritmética limitada a esta comparación con cero. |
| extras 34/35 | No retira listener focusin / retira otro nombre | Higiene de desmontaje: listener residual que sólo conserva y vacía una ref local. No se reclama que esté probado el retiro ni que sea equivalente; no hay callback privado ni restauración de datos en esa función. Límite P2, sin refuerzo adicional para perseguir score. |
| extra 36 | Dependencia constante de efecto en vez de [] | Ambos efectos se montan una vez por instancia con esa constante; no demuestra fallo de actualización. |
| extra 46 | Generación de retry decrementa en vez de incrementar | Cambia el valor en cada decisión manual y provoca nueva consulta; la prueba del segundo retry distingue la constante, no exige dirección del contador interno. |

Las familias prioritarias diferenciables quedan acreditadas: retiro de catálogo antes de HTTP 401 (388/389 K), listener y guardas de foco (392/393/394/396/397/399/409/410 K), segundo retry (695 K), semana no lunes coherente (155/156/159/163 K), nulidad mezclada (271 K) y suma positiva (292 K). No cambia producción TS ni se solicitan nuevas matrices/replays.

Entrega selectiva: config `frontend/stryker.weekly-review-replay.config.json`; dos tests; `weekly_review_refined_replay.md`, propuesta JSON/generador, mapping JSON y manifiestos before/after/start/exit. `weekly_review_map_replay.mjs` es herramienta documental reproducible; los raw permanecen locales y separados de informes. Root gestiona ignore y Git. Las fuentes/tests/config quedan liberadas tras after, sujetas a coordinación root.
