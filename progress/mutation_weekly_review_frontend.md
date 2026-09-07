# Mutación frontend19 — campaña original

Campaña autorizada por root tras revisión/configuración y frontend completo 1.965/42, lint/formato GREEN. Inicio 7 de septiembre de 2026, 16:52:27 Europe/Madrid; sesión `35899`, proceso Stryker 64528. Comando `node scripts/project.mjs mutate weekly_review-frontend`.

Configuración SHA256 `13A67790F5034A697B699B91438A48FA0371F89E2F7726A9C402716FF6C91602`, seis scopes aprobados (cuatro fuentes), ocho workers, perTest, todas las suites Vitest, umbral 80 y mutadores heredados. Sin máscaras/exclusiones nuevas.

Before efectivo `weekly_review_stryker_before.json`: 141 entradas idénticas al gate frontend previo, verificadas antes de iniciar (`dd112a`). Fuentes/tests/config permanecen congelados. Log original `weekly_review_stryker_original.log`.

Instrumentación reporta 766 mutantes (`866e1b`); a las 16:53 seguía el dry-run. **No hay resultado final todavía.** No se clasifican contadores parciales ni se inicia replay. Al cerrar se preservarán JSON original, estados por archivo, hashes after y residuos observables; errores y sin cobertura seguirán separados de Killed.

Dry-run GREEN a las 16:53:58 (2823b6): Stryker reporta 757 tests en 85 segundos. Es su selección efectiva, separada de los 1.965 tests del gate completo; configuración Vitest sin filtro explícito. La campaña de mutantes continúa, sin informe final todavía.

## Resultado final original

EXIT0 `8cd9ed`, fin 17:03:51; duración reportada **11 min 24 s**. JSON original copiado y verificado en `weekly_review_stryker_original.json`, SHA256 `0FC84106ADDF2EB0A31793DA756242CAC037494731E70A276D64625DC36FBF52`. Before/after: **141 entradas idénticas**, cero diferencias (`001719`). La corrección posterior de un fixture heredado split-task se documenta separadamente y no cambia esta campaña.

| Fuente | Killed | Timeout | Survived | NoCoverage | RuntimeError |
| --- | ---: | ---: | ---: | ---: | ---: |
| App | 21 | 3 | 0 | 0 | 0 |
| weekly-review-api | 279 | 0 | 37 | 0 | 0 |
| weekly-review | 310 | 0 | 107 | 2 | 2 |
| workspace | 5 | 0 | 0 | 0 | 0 |
| Total | **615** | **3** | **144** | **2** | **2** |

Total 766. Score Stryker **80,8901 % = (615+3)/764**; Killed estricto sin tratar Timeout como Killed **80,4974 % = 615/764**. Incluyendo también los errores en denominador: **80,2872 % = 615/766**. Los tres cálculos superan80; no se reescribe ningún estado. WeeklyReview aislado reporta73,99 %, mientras el umbral configurado corresponde al scope completo, no a cada archivo.

## Errores y timeouts, sin reinterpretación

- RuntimeError **575/580**, líneas165/167 de WeeklyReview: quitan optional chaining de `link?.getAttribute` y `link?.contains`. El raw acredita fallo del adaptador `errorToString` de Stryker/Vitest al serializar la excepción: `TypeError: Cannot convert object to primitive value`, después de sus dos reinicios internos. Un click que no nace de un enlace permite link=null, pero el diagnóstico terminal es del runner y no una aserción Killed. Misma familia instrumental observada en campañas anteriores; no se modificó el adaptador.
- Timeout **0/1**, App línea15: suprimen ancla inicial/final del reconocimiento de ruta; **9**, App línea33: vacía la etiqueta heredada Proyectos del condicional completo. JSON/log registran Timeout sin statusReason ni duración individual. La causa observable exacta es agotamiento del presupuesto del runner; esos artefactos no permiten atribuir un bucle o un fallo funcional concreto. No se inventa causalidad por carga de CPU ni se reclasifican. No requieren replay para alcanzar80 estricto.

## Clasificación y propuesta acotada para revisión

Inventario completo de **144 Survived + 2 NoCoverage + 3 Timeout + 2 RuntimeError** en `weekly_review_stryker_residuals.md`, y JSON con contexto/firma/statusReason en `weekly_review_stryker_inventory.json`/`weekly_review_stryker_classified.json`. Cada residuo mantiene ID, archivo, ubicación, mutador y replacement. Las agrupaciones son lectura razonada; no equivalencia certificada para toda una familia.

Prioridades diferenciables propuestas, **sin implementar ni hacer replay**:

| Prioridad | Firma original | Oráculo mínimo público |
| --- | --- | --- |
| P1 privacidad | UI388/389 (cleanup catálogo), secundarias527/532 | Con catálogo GET pendiente, desmontar ruta y entregar HTTP401 antiguo antes del observador: acceso vigente no se revoca. El test de desmontaje actual cubre W, no esa petición de catálogo. |
| P1 recuperación | UI695 | Reforzar retry existente: segundo503 y otro reintento manual deben producir nueva petición y terminar200. `setRefresh(() => undefined)` cambia una vez y luego queda bloqueado. UI708 tiene riesgo análogo en segunda actualización, separado del contador ±1 (696/709), que también cambia generación. |
| P1 foco | UI392–399,409/410/413 | Tras iniciar consulta, mover foco voluntariamente a otro control y después dejar body antes de respuesta; no reenfocar título. El test actual mantiene el otro control enfocado, por lo que la guardia body oculta la pérdida del listener. |
| P1 calendario | API155/156/157/159/163 | Reforzar el caso «no Monday» con semana completa coherente martes–lunes, incluidos siete días/instantes y serverNow. El actual sólo cambia weekStart y también viola secuencia, así que no aísla lunes. |
| P1 integridad capacidad | API271 | Mezclar un día null con seis capacidades conocidas0 y total0; rechazar. El caso actual tiene todos los días null, por lo que every→some también rechaza. |
| P1 cifra positiva | API292 | Una capacidad positiva conocida con suma exacta válida, por ejemplo siete días1 y total7. El negativo existente total6 sigue fallando si sumar cambia por restar; no acredita aceptación de suma válida. |
| P2 zona | API97/99 | Fallback UNCONFIGURED con zona diferente de UTC, resto coherente: rechazar. No recomputar TZDB. |
| P2 límites exactos | API252/253 y240 | serverNow unµs antes de start sin date debe rechazar; igualdad con start debe aceptar. Fecha domingo explícita válida debe aceptar. Son casos separados, no matriz temporal nueva obligatoria. |
| P2 navegación/foco | UI586/588 | Reforzar navegación hacia anterior/siguiente con query y control todavía enfocado; exigir h1 al sustituirlo. El oráculo actual usa Esta semana sin query y toma la otra rama. |
| P2 long/tipo | API302/306,303 | Decimal de20 dígitos comenzado por1 frente a límite long; array JSON `["0"]` en un campo decimal. Los negativos actuales usan números/valores grandes que pueden ser rechazados por otra guardia. No afirmar equivalencia de tipos por compartir regex. |

Para rapidez, propongo limitar la primera decisión a las **seis filas P1** y dejar P2 documentadas salvo juicio contrario. Una fila sólo debe convertirse en tests si root aprueba el alcance; inicialmente GREEN se declarará así. No existe defecto productivo demostrado por esos supervivientes: la fuente actual conserva las protecciones.

Límites restantes explícitos: errores400/409 mal formados, cancelación/clasificación en recorrido A→B→A, segunda actualización, accesibilidad de ayudas/tabIndex y textos de presupuesto/duración pueden distinguir mutantes; no se declaran equivalentes por ser mensajes. Hay evidencia E2E de0 min y capacidad positiva que Stryker no ejecuta. NoCoverage597/604 son los fallback de FormData.get(null) para controles siempre presentes en el formulario actual; no se fabricará DOM amputado sólo para cubrirlos. Dependencias literales constantes391/402/420 y módulo ±7 de API162 tienen justificación algebraica/React acotada. Guardas de null/continuidad y validadores civiles se solapan, sin certificar todas sus variantes como equivalentes.

La observación visual del outline del título queda fuera de este resultado JS y pendiente de medición tras liberar18080. No cambios de CSS/producción mientras los gates integrados siguen activos. Ningún replay ejecutado.
