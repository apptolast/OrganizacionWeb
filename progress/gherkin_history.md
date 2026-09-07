# Handoff Gherkin18 — historial propio

Contrato destilado de la sección18 normativa integrada6384934. Project-spec SHA256 `D8A4C5B274832563B67EDF988B6733760107EDF70563191A73195B6582FE7245`, verificado953c6b. Archivo `features/history.feature` congelado SHA256 `768AA48A5A0495BDC5AA292395F1DC42DADD0BD7702987F50D2FAD4664B72C13`.

**39 escenarios etiquetados @s1–@s39, 142 ejemplos contractuales y un When por escenario.** Conteo estructural a4d800:39tags/39When. Un Scenario cuenta un ejemplo; cada fila Examples cuenta otro. No son142 tests escritos ni ejecutados y no se usa runner BDD nuevo. Sin producción, pruebas ejecutables, migraciones, cambios de estado ni Git; pendiente revisión independiente B/root.

## Familias y fronteras

- @s1–5: cinco fuentes y variantes, identidad compuesta, originales frente a proyecciones, propiedad y nombres actuales. @s2 cuenta diez hechos sin copiar cierre/EXTEND desde proyección ni finalización de proyecto desde outbox.
- @s6–12: envelope/20+1, categorías/contextos y fechasUTC inclusivas/extremas, vacíos y validación. Propiedad de contextos ajenos y ausentes conserva404 incluso sin hechos; lectura global vacía es200.
- @s13–21: orden total/rangos normados, empates sin causalidad, cursor cerrado y normalizado, precedencia sintaxis→propiedad→vínculo y continuación. @s20 fija upper11:00/after09:00/restante08:00 y commits12:00/10:00/08:30 para resultados concretos; no dice «puede aparecer» sin fixture. @s21 refresh sin cursor descubre lo excluido delante de frontera.
- @s22–24: snapshot sólo por petición, read-only/sin Clock, fallos503 incluida finalización/recibo incoherente y recuperación después de reinicio/retirada de transporte. No se prescribe índice, SQL exacto o estructura adicional.
- @s25–27: precisión grande, atribución persistida y validación cliente de envelope/variantes/identidad/filtros/orden/duplicados; una incompatibilidad rechaza la página completa. Sesión muy larga de1000 a1600 permite un acumulado superior a Number seguro sin fixture temporal imposible.
- @s28–39: navegación y enlaces contextuales sin catálogo, una página porURL, filtros explícitos, detalles sin N+1/HTML, carga/vacíos/error, reintentoGET, generaciones/privacidad, foco y matriz UX con límites físicos.

## Reutilización y límites honestos

Se mantienen BlockResponse9, ReceiptResponse13 de7, TaskHistory4, SessionStart7 y recibos P/R6/CLOSE7/EXTEND7. No se vuelven a escribir matrices completas de los decoders14–17, Unicode ni negociación/autenticación; sí conexiones representativas y validación de identidad donde existen campos. TaskHistory4 no inventa projectId/taskId internos.

Los rangos definitivos son los de normativa18: BLOCK_PLANNED0, BLOCK_CHANGED1, TASK_STATUS_CHANGED2, SESSION_STARTED3, SESSION_CHANGED4. La propuesta SQL anterior history_query_design.md era exploratoria y no sustituye esta decisión. La comparación de UUID no presupone secuencia causal de revisiones; la UI debe explicarlo y mostrarlas.

El cursor incluye owner/filtros/upper/after pero nunca actúa como autorización. Base64 no se presenta como secreto/firma. Su coherencia se comprueba después de propiedad de contexto explícito, mientras su sintaxis inválida precede404. No se exige encontrar una fila de frontera ni eliminar datos para probarlo: @s19 permite fronteras válidas arbitrarias sin filas.

Las fechas usan occurredAt UTC y no workDate ni start_at planificado. @s22 exige snapshot coherente con escritor posterior a su inicio, no snapshot persistente entre páginas. @s36 separa401 antes de entregarResponse, JSON200 diferido y clasificación503: no reclama diferir el observador401 después de haber recibidoHTTP.

El contrato no acredita todavía pruebas, estudios, dispositivos/lectores físicos, mutación ni cumplimiento de todas las permutaciones UX. No estadísticas19, exportación, edición histórica ni nuevos consumidores. B fue informado del freeze para review antes de iniciar implementación; root conserva gate y estado.

## Precisión posterior a revisión independiente

Root efb516 y B coincidieron en dos ajustes: @s8 usa proyecto en estado idea; @s13 fija dos SESSION_CHANGED y una de cada otra familia, seis hechos. Root precisa además el título@s25: no recalcular atribución histórica con TZDB, sin prohibir Intl para formato/fallback visual. Sólo texto;39 escenarios/142 ejemplos/39 When se conservan. Snapshot previo SHA755D5CDB2DB109D32ED7FA7903BA541C9DE151E412F82FDD53E4A664F4440A2C queda identificado como corte revisado; no se alteraron sus dictámenes. Nuevo hash superior pendiente de ratificación root/B.
