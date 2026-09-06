# Revisión parcial — confirmación histórica de bloque

**APPROVED para este componente**, no para el cierre de feature13. Revisión independiente por el coordinador, sin autoría de producción/tests. Lectura completa f561c5 de fuente, tests y bitácora; corte del autor con16 pruebas verdes d09716, formato03cbbd y ESLint7d610b. No se repiten suites mientras continúa la autoría del panel; integración, tipos globales, UX real y mutación siguen pendientes.

## Contrato y evidencia

- @s36: creación original y recibos de movimiento/cancelación permanecen identificados como hechos históricos. GET state independiente, sin POST ni inserción en lista; estado posterior se presenta en región distinta. Fallo conserva confirmación y ofrece reintento manual. La API13 ya revisada valida el DTO y su contexto antes de entregarlos.
- @s37: reemplazar recibo, proyecto, tarea o bloque desmonta la instancia anterior; los tests de respuesta tardía comprueban aborto, ausencia de callback y que401 no llega al observer. La clasificación404 usa ReadableStream demorado y comprueba la guarda después de JSON. Estos tests llevan etiqueta textual @s38 en el archivo; su trazabilidad contractual corresponde aquí a @s37.
- @s38/@s40: al desaparecer el botón de reintento durante la consulta, el foco pasa al encabezado si BODY sigue activo. El control exterior conserva su foco antes y después de la respuesta. Los hechos confirmados conservan role=status; carga y error tienen estados distintos. Esto acredita oráculos de componentes, no lector de pantalla físico ni la matriz responsive completa.
- No se habilitan acciones de negocio desde este componente. TaskBlocks debe consultar state al abrir una nueva acción y conservar las listas separadas; esa composición no está aprobada por este dictamen.

## Disciplina y alcance

La bitácora identifica RED/GREEN y reconoce los casos inicialmente verdes y los errores de fixture sin atribuirlos a producción. La corrección del anuncio accesible tuvo RED49dd3d y GREENd09716. Se reutilizan API y presentación temporal; sin dependencia, almacenamiento privado ni abstracción adicional de formulario.

Fuente congelada SHA256 `6A882F6A46FF3B718D30676BA39E0838C820806C0A8C14664E94A876DCC8F73E`; test `C69C5C8C24DADECCD55EF1372D6C29E0FCF260945362905D029157C45694856F`. El informe no convierte init12f6fdd ni sus fallos E2E en validación de este componente. Feature13 permanece in_progress.
