# Revisión de mutación frontend 16

## Dictamen de la campaña original

Gate global superado con limitaciones explícitas. Root confirmó EXIT 0 de la campaña única en `aea3fb`; duración 18 min 55 s y dry run de 820 pruebas GREEN. No se ha demostrado un defecto de la implementación vigente; sí hay huecos diferenciables de sus oráculos, descritos abajo. Este dictamen no cierra por sí solo la feature ni convierte los residuos en equivalentes.

| Fuente | Killed | Survived | NoCoverage | RuntimeError | Total | Score Stryker |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| work-session-reader.tsx | 220 | 60 | 1 | 1 | 282 | 78,29 % |
| work-session-state.tsx | 217 | 28 | 1 | 1 | 247 | 88,21 % |
| App.tsx | 56 | 5 | 0 | 0 | 61 | 91,80 % |
| use-session.ts | 224 | 16 | 0 | 0 | 240 | 93,33 % |
| work-session-state-api.ts | 387 | 60 | 0 | 0 | 447 | 86,58 % |
| Total | 1104 | 169 | 2 | 2 | 1277 | 86,59 % |

Timeout, CompileError e Ignored: cero. Stryker usa 1104/1275 = 86,588235 %, excluyendo los dos RuntimeError de su denominador; la fracción estricta sobre todos los generados es 1104/1277 = 86,452623 %. Ambos superan 80 %. Reader queda por debajo de 80 % individual (78,29 %; estricto 78,01 %); el umbral configurado es global, no por archivo. Ningún error se cuenta como Killed y no se aplica ninguna exclusión adicional.

## Evidencia preservada

`close_work_stryker_original.json` conserva el JSON original completo de `frontend/reports/mutation-close-work-session/mutation.json`, SHA256 `F99173A0C635EFA7D5A2F9D3B9B9653B3A71C5C34B578BB6D35AC993C6EC43A0`. Copia y hash verificados en ab490d. `close_work_stryker_inventory.json` contiene los recuentos; `close_work_stryker_residuals.json` conserva los 173 residuos con archivo, posición, mutador, sustitución y contexto de fuente. Los IDs que siguen son del JSON original, no identificadores estables entre campañas.

Los 90 archivos de `close_work_stryker_before_hashes.json` coinciden con `close_work_stryker_after_hashes.json` (ab490d). Root volvió a preservar el cierre original en `close_work_stryker_root_completion_hashes.json`, 90 idénticos, cc10a4, antes del refuerzo posterior de pruebas. El agente no pudo consumir la sesión root23099 y no inventa su EXIT: lo aporta root. No se modificaron fuentes, pruebas ni configuración durante la medición.

## Residuos prioritarios del lector

- **Privacidad, 611:** `work-session-reader.tsx:331:12–36`, ArrowFunction, `() => controller.abort()` sustituida por `() => undefined`, en OpenSessionAfterClosure. React puede descartar setters del componente desmontado y aun así el observador global de apiRequest procesar un 401 antiguo si no se aborta. Falta distinguir públicamente GET active pendiente tras cierre, navegación a otra sesión y entrega del 401 antiguo. La fuente vigente sí aborta. Root autorizó un único refuerzo y B lo entregó posteriormente, inicialmente GREEN; su medición dirigida queda separada del original.
- **Accesibilidad, 543 y 546–558:** identificación del mensaje y atributos aria-invalid/aria-describedby de las notas. Un caso público con una nota inválida y la otra válida puede exigir el estado inválido sólo en la primera y la descripción accesible enlazada al alert. No basta encontrar el texto visible. La implementación actual contiene esas asociaciones; no se declara fallo funcional actual.
- **Carga y duplicados, 628/630:** guardia `if (loading) return` y `setLoading(true)` del botón Consultar sesión abierta. Propuesta acotada: retry de GET active diferido, estado anunciado y segundo clic sin nueva petición. No requiere otra campaña completa ni cambio de producto.
- **Foco/carga de recuperación, 513/514/516/517:** Consultar estado actual después de 412 elimina el iniciador y renueva GET S. Un oráculo inmediato durante la promesa pendiente distinguiría foco de heading, anuncio de carga y retirada del error anterior. Otros cambios de foco/tabIndex (332/341/386/490/494/512/565) también sobreviven; no se certifica equivalencia.
- **Reintentos posteriores, 519/571/635:** cambiar el incremento de generación por undefined permite el primer refresh pero puede impedir el segundo. Los decrementos 520/572/636 conservan el cambio de identidad requerido por el efecto; se registra esa explicación contextual, sin excluirlos del resultado.
- **Otros grupos:** estado inicial/espera y error (331/346/352/354/390/407/506/590), guardas tras await y desmontaje (363/367/396/397/410/412/441/450/466/478/484/596/601/607), render condicionado (621/622/633), preventDefault del formulario (537), espacios de presentación (538/539/577), acceso opcional a referencia montada y dependencia constante (342/351). La superposición de guardas y el descarte de setters de React pueden ocultar algunas mutaciones; no justifican declarar las 60 equivalentes. La navegación nativa del formulario requiere evidencia de navegador, no inferirla del runner DOM.

## Otros residuos y precisión del cliente

Los 60 residuos del decoder incluyen tipos/gramática decimal y revisión (714/717/718/759/761/762/765/766), enums/forma de estados y recibos (701/853–857/888–926), tiempos y límites (659–673/723/731–741/932), F limitado a CLOSE (1025), igualdad de intención (1040–1045) y clasificación de errores (1053–1089). Son fuentes completas compartidas con 15; el inventario no atribuye todos esos casos a nueva lógica 16.

Un hueco especialmente acotado es 1045: retirar sólo la comparación de progressNote puede quedar oculto si el caso incompatible cambia también nextStep. Un recibo con únicamente progressNote distinto distinguiría la obligación de conservar la intención. También son diferenciables tipos numéricos en lugar de strings, status desconocido o un PAUSE coherente recibido por F; no se propone desplegar otra matriz completa para perseguir puntuación. La comparación estricta de un intervalo cero (673) y algunos checks repetidos por validadores anteriores admiten razonamiento contextual, pero no se han eliminado ni declarado equivalentes formalmente.

Los 28 residuos de State incluyen limpieza/guardas de consultas y comandos (1139/1162/1191/1207/1219/1223), estados e invalidación de snapshot (1102/1111/1136/1143/1172), reintento/foco (1225/1236/1237/1243/1265/1280), referencia montada/dependencia constante y generación alternativa (1101/1109/1117/1129/1150/1244/1266/1281), y presentación (1251/1295/1344). En particular, la eliminación del abort 1223 no se acredita mediante el futuro replay del abort 611: son firmas diferentes.

Los cinco de App incluyen anclas de ruta (11/12) y presentación heredada (51/56/58). Los 16 de use-session incluyen navegación tras acceso (93/96/285/287), limpieza de token/observer (112/129/155/156), guardas de aborto y doble cierre (186/209/244), estado inicial (70), referencias opcionales y dependencia constante (154/157/160/240). Permanecen explícitos; esta revisión no reabre toda la autenticación heredada sin un caso diferenciable aprobado.

## NoCoverage y errores de ejecución

Reader 366 (línea 53) y State 1120 (línea 42) afectan la guardia `!snapshot && !retained.current`. Los controles actuales requieren snapshot o intención retenida para invocar la acción. Esa defensa no se alcanza públicamente por las pruebas instrumentadas; no se propone invocar funciones privadas para fabricar cobertura. Se conservan como NoCoverage.

Reader 435, línea 103, y State 1185, línea 86, OptionalChaining, cambian `problem?.code` por `problem.code`. Ambos RuntimeError contienen fallo del runner tras dos intentos: `TypeError: Cannot convert object to primitive value`, en `@stryker-mutator/util` errorToString y VitestTestRunner.run. La desreferencia alterada de un resultado ausente es una causa plausible del rechazo que el runner no logra presentar; el reporte no permite convertir ese incidente en Killed ni probar una equivalencia. Son dos errores de medición residuales, no bugs demostrados del producto actual.

## Seguimiento dirigido, separado del resultado original

Preparar sólo replay de la firma Reader 611 tras revisión del refuerzo GREEN: rango original `src/work-session-reader.tsx:331:12-331:36`, ArrowFunction y reemplazo `() => undefined`. Reutilizar configuración Vitest y candidatos completos, perTest, umbral 80, ignorePatterns protegido exacto y reportes/tempDir separados de la campaña original. Verificar posición y hash antes de ejecutar y casar la firma por archivo/posición/mutador/reemplazo, nunca por ordinal de otra campaña. Un rango puede generar más de un mutante; informar todos los generados y la firma objetivo. Este documento sólo prepara esa medición: no la ejecuta ni presupone su resultado. Otros refuerzos requieren coordinación expresa y no alterarán retroactivamente los 1277 resultados originales.
