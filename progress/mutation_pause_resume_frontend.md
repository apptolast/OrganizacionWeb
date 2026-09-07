# Mutación frontend15

Campaña iniciada mediante `node .harness/harness.mjs mutate pause_resume_session-frontend`, configuración aprobada de tres fuentes completas, perTest, ocho workers y umbral80. Baseline: suites globales1666/33 comunicadas por root y lint completo835359; último cambio únicamente formato del test API. No Gradle ni Docker del tester. Ponytail full y Caveman lite.

Snapshot previo: `progress/pause_resume_frontend_mutation_before.json`,89 archivos de fuentes/tests y configuración/runner. Log propio: `progress/pause_resume_frontend_mutation.log`. No existía reporte15 anterior al arranque; salidas fijadas por configuración en `frontend/reports/mutation-pause-resume-session/`. Se conserva ignorePatterns protegido, sin leer ni rastrear su directorio.

## Resultado final observado

Arnés EXIT0 f9dae9, duración13m51s. Dry-run731 tests GREEN; promedio16,18 tests por mutante. JSON contado independientemente057b61:861 total, **741 Killed estrictos**,119 Survived,1 NoCoverage,0 Timeout y0 errores (incluidos RuntimeError/CompileError). Score bruto741/861 = **86,06%**, superior al80 configurado. No se descontó ningún superviviente como equivalente.

| Fuente | Total | Killed | Survived | NoCoverage |
| --- | ---: | ---: | ---: | ---: |
| work-session-state-api.ts |312|265|47|0|
| work-session-state.tsx |225|190|34|1|
| work-session.tsx |324|286|38|0|

89 hashes anteriores/posteriores idénticos396b14. Ambos manifiestos SHA256 `1F041EA2382E0C15F7CBD448436CE839174A666E157930CCD4B0615EFEA52A64`; ninguno de los archivos medidos cambió durante la campaña. Sólo Git/documentación externos al manifiesto podían avanzar por coordinación de root.

Reporte JSON original: `frontend/reports/mutation-pause-resume-session/mutation.json`. Copia raw separada e idéntica: `progress/pause_resume_frontend_mutation_raw.json`; ambos SHA256 `0F0F92DD7FA76E1548CA024CD4904ED766D91877BC08A79938381E3CF8DBA246`. HTML original SHA256 `9B4F03F801EB28ADC7C94DFC53A68CC62E93D954A04455074F7DE991F0B37130`. Log SHA256 `155392DF05F194968B420CF84BB9B237100FA1B6E7C11070D1ACFA3940AD436A`. No errores del runner ni reintentos de campaña.

## Residuos: prioridades observables, sin reclasificar

Inventario completo de los120 no detectados en `progress/pause_resume_frontend_mutation_residuals.json` (549447): ID original, archivo, posición, estado, mutador y reemplazo. Los IDs no forman una secuencia de1 a861; se conservan tal como los entrega Stryker. El raw conserva además código embebido, posiciones completas y pruebas ejecutadas.

1. **Reenvío incierto sólo tras ausencia reconocida.** Panel401/409/410 modifican la autorización de mayResend y sobreviven. Caso mínimo prioritario: POST incierto → Comprobar devuelve503 o problema desconocido → no Reenviar; un404 posterior sí lo ofrece con misma key/revisión. Los tests actuales ejercitan recuperación válida y404, pero no fijan esa negativa intermedia. Es hueco de protección del contrato@s32, no prueba de defecto en la fuente original.
2. **Recuperación por key valida todo el recibo.** API266/267/269 debilitan isChange/acción en recover. Reutilizar un recibo bien formado de acción opuesta y uno con before/after incoherentes, conservando identidad/revisión para que el rechazo no dependa de otra guarda. El caso actual de revisión distinta no cubre esas condiciones independientes.
3. **Tipos y fronteras exactas del estado.** API72/81/117/119/120 cubren guarda de tipo de revisión, máximoBIGINT y decimal; API22–24 y89–99 afectan validación temporal. Priorizar valores canónicos límite aceptados y números/arrays que BigInt puede convertir pero el DTO prohíbe; un instante no válido con inicio anterior a1970 distingue guardas temporales eliminadas. API219 rechaza una transición válida en el mismo microsegundo: un recibo de intervalo cero cubre ese borde sin añadir nuevas reglas. Las regex75/76/123/124 requieren examinar por caller si BigInt ya rechaza la misma entrada; no se llaman equivalentes en bloque.
4. **Problemas cerrados.** API301/305/309/310/316 sobreviven al quitar controles de code/title/type/status. Reforzar reconocimiento con código desconocido, type contradictorio, título vacío y combinación código/status errónea manteniendo los demás campos válidos. La UI debe permanecer incierta ante un problema incompatible, no habilitar decisión definitiva por una coincidencia parcial.
5. **Retiro/consulta y semántica de UI.** Panel366/389/434/450 y418/446 alcanzan guardas de aborto/cleanup; revisar por contexto observable antes de pedir nuevos casos. Ya existen JSON/error tardíos de comando, pero el lookup retirado y su observer merecen distinguirse de un mero setState después de unmount. Panel470/492/507 eliminan incrementos de refresh: puede faltar una segunda consulta fallida y un tercer intento realmente observable. Las cadenas517/537/539/540/549 afectan presentación (unidad, formato temporal, fallback); cualquier equivalencia debe juzgar el contrato de legibilidad, no asumir que todo texto es decorativo.

**Posibles redundancias, aún sin reclasificación:** API36 cambia `<` por`<=` cuando now=since y ambos aportes son0. Panel377/471/493/508 cambian +1 por-1 de un contador usado sólo para cambiar la dependencia, sin exponer su valor. Panel328/356 pueden ser opcionales redundantes si sus referencias siempre están montadas/asignadas en cada caller; exigir prueba de esa condición antes de equivalencia. NoCoverage347 pertenece al lado retenido de la guarda de send: las acciones visibles y el flujo de retención pueden hacerlo inaccesible, pero no se adjudica Killed ni se elimina del total.

Los38 supervivientes de WorkSession pertenecen al archivo14 incluido completo por su composición15. Incluyen fronteras de duración638/641, textos y guardas heredadas. Deben compararse por firma con el dictamen14 si se decide seguimiento; no se inventa correspondencia por número ni se reabre automáticamente toda14. El inventario conserva todos sin exclusiones.

La puerta numérica está superada. Estos grupos sirven a la revisión de riesgo y eventual TDD acotado; no autorizan nuevas campañas ni afirman119 equivalencias. No se modificó producto/test/config para matar residuos durante la medición. Root decide el seguimiento y cierre integrado.
