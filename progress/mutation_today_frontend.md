# Mutación — Hoy frontend

Estado: medición iniciada, sin score ni veredicto anticipado. Rol mutation_tester independiente de autoría UI; Ponytail full/Caveman lite vigentes. Autorización root tras judge frontend4ffcf5, regresión1319/1319 4aecda, lint9698e6, foco110/build7cf936 y13script38e58c. Una campaña focal por arnés; sin replay automático, cambio de fuentes/tests/config ni reducción de umbral80.

## Snapshot y comando

HEAD al iniciar5cc80a99e3951a755e0e1331622113ed29e2e2c2; cambios UI finales autorizados aún identificados por manifest, no se afirma que HEAD solo los contenga. Comprobación f5954a: los seis hashes del manifest progress/today_frontend_mutation_scope.json coinciden. Today.tsx SHA2560bcc57577073014f76eee41110121b03413e9731a72544d13d0cf1432a9ef789. Manifest contiene demás hashes y nueve selectores (Today API/UI completos; regiones modificadas App/Workspace/ProjectReader/use-session). Archivo destino original frontend/reports/mutation-today/mutation.json no existía antes de iniciar.

Comando exacto: `node .harness/harness.mjs mutate today-frontend`. Stryker configuración dedicada, concurrency2, perTest, threshold80; salidas separadas frontend/reports/mutation-today/mutation.json y mutation.html. No se lee ni limpia la ruta protegida excluida por configuración. Se conservará informe original y se separarán K/S/Timeout/RuntimeError/CompileError/NoCoverage; score bruto K/total junto a score del runner si difiere.

Inicio ed316f/session83950: Stryker instrumentó6archivos y521mutantes. Dryrun empezó523ed8 con2workers y cobertura perTest; aún sin resultado. Coordinador publicó checkpointf568f6e (push43e507) durante medición, declarando mismos bytes de fuente/tests/config; esa referencia complementa el HEAD inicial sin reiniciar campaña.

Baseline a1102c: Initial test run succeeded,657 tests en60s (net35400.55ms/overhead24653.45ms). Esta cifra es la selección del runner durante dryrun, no reemplaza la regresión global1319 del coordinador. Medición521mutantes en curso; sin score parcial atribuido.

## Resultado final — PASS de umbral; seguimiento pendiente

Campaña cerrada e5ccd3 EXIT0 a15:23:50 Europe/Madrid, duración25min52s. **418 Killed /521 total =80,2303262956%** (umbral80%). Stryker80,23%;102Survived,1NoCoverage,0Timeout,0RuntimeError,0CompileError. Sin exclusiones ni score ajustado. Superar umbral no resuelve automáticamente supervivientes ni el defecto de foco hallado por E2E; feature no se declara done aquí.

Informe original JSON SHA256 **5cc335b97919aaa1bcd3cf4cf956af54ee55db3a02fdb23a060c77508f5c47a3**; HTML **a7e2bb6b54ed19dcb36ae2ce4f4f0d4a8c6eef49708ba76d305612f24036db8e** (978945). Ambos en frontend/reports/mutation-today; copia original preservada en frontend/reports/mutation-today-initial, hashJSONidéntico f1cc4c. Checkpoint de campaña f568f6e, hashes previos f5954a/manifest. Freeze liberado inmediatamente al cerrar antes del inventario; correcciones posteriores del autor no pertenecen a esta medición.

Inventario exacto de103 resultados noKilled: progress/mutation_today_frontend_inventory.json (f1cc4c), con archivo,ID,operador,ubicación completa, expresión original sensible a mayúsculas, replacement ystatus.521IDs únicos. Distribución: TodayAPI203K/34S/1NC (238); TodayUI157K/42S (199); App35K/6S (41); Workspace16K/20S (36); use-session7K (7). ProjectReader estaba seleccionado en6archivos instrumentados, pero el catálogo no generó mutantes en sus href: no se inventa denominador para ese archivo.

Botón Actualizar (fuente original línea124): **ningún mutante directo de disabled={loading}** fue generado. ID423 ArrowFunction `() => refresh()`→`() => undefined`, línea124columnas57–72, Killed; ID289 BlockStatement del cuerpo completo de Today→{}, Killed. Evidencia3b2aa9. El defecto de foco observado en navegador no se atribuye a un superviviente inexistente ni se considera cubierto por haber matado el handler. Clasificación de103entradas en seguimiento, sin nueva campaña ni modificación de fuentes/tests/config por este medidor.

## Clasificación inicial para el artesano y judge (no exclusiones)

Todos los IDs siguientes mantienen su status original. «Candidato equivalente» es una hipótesis de flujo para revisión independiente, no una exclusión aplicada ni un kill. El inventario JSON conserva las103 identidades completas y expresiones exactas.

| Archivo / IDs | Observación y comprobación propuesta |
| --- | --- |
| today-api 127 | Observable: date array de un string ISO puede coercionarse al concatenar; caso JSON con único defecto de tipo. |
| today-api 171–178,181,183,185,192 | Observables: fallback coherente base con un único campo de capacidad no-null, zona efectiva distinta deUTC o availabilityZoneId no-null enUNCONFIGURED. Evitar fixtures que ya fallen por un segundo campo. |
| today-api 224,225(NC),226 | Observables: ID repetido con intervalos distintos/ordenados, preferiblemente noadyacente y resúmenes coherentes; actual duplicado idéntico puede ser rechazado antes por orden y no alcanza Set. |
| today-api 209,211–219 | Revisar orden cronológico con UUID inversos, mayúsculas y empates en payloads defectuosos. El contrato válido prohíbe solapes, por lo que no inventar reservas válidas empatadas para forzar muertes.216 es candidato redundante: igualdad deUUID se rechaza luego porSet aun relajando >= a >. Los otrosIDs requieren revisar tanto aceptación válida como rechazo del defecto aislado. |
| today-api 232,233 | Observables: bloque que sólo toca frontera, con suma0 coherente y candidatos ajustados; el caso actual fuera del día puede caer antes por resumen incoherente. |
| today-api 78,85,89,93 | Candidatos redundantes: includes valida enum por identidad y validación posterior exige presupuesto entero o null, restantes/exceso numéricos exactos o null. Verificar recorrido completo antes de decidir equivalencia. |
| today-api 222 | Candidato equivalente: convertir todos los IDs delSet aupper en vez delower conserva igualdad case-insensitive paraUUIDhex. |
| today-api 253,259 | Candidatos contextuales: en reservas válidas sin solapes y ordenadas porinicio, finmáximo es el último; empates de fin tampoco son válidos. No atribuir equivalencia universal para intervalos arbitrarios. |
| today.tsx 292,347,426 | Observables: anuncio/alerta de carga inicial y de refresco pendiente después de fallo, antes de confirmación. |
| today.tsx 309,315 | Observables por secuencia: error/finally antiguo entregado tras sustituir generación; verificar que no cambia loading/error de la lectura vigente. |
| today.tsx 329,331,337,397 | Revisar frontera exacta y generación forzada con GET vigente, incluyendo ventana de aborto inmediato y entrega tardía antes de nueva confirmación. Los tests existentes no distinguen todos los valoresfalse de estos guards. |
| today.tsx 366,368,370 | Limpieza de listeners: comprobar ciclo mount/unmount y restitución de suscripciones propias; no asumir equivalencia sólo porque React ignore setState desmontado. La fuga de listeners es observable en recursos, aunque una pantalla posterior no cambie. |
| today.tsx 378,380 | Observables: respuesta aceptada mientras pestaña permanece oculta no debe armar frontera; regreso debe consultar/coalescer y conservar deadline. |
| today.tsx 443,445 | Observable: snapshotAVAILABILITY no debe anunciar fallback de zona/capacidad desconocida. |
| today.tsx 465–470 | Observables: exceso conocido positivo y desconocido en fallback, tanto valor/minutos como texto; otros resúmenes correctos no cubren este dd. |
| today.tsx 471 | Observable: cierre vacío anuncia Sin bloques; no basta probar ausencia de items. |
| today.tsx 479,481,484,486 | Observables: etiquetas actual/próximo dentro de su item concreto y ausentes en otros; buscar etiqueta global permite que se intercambien. |
| today.tsx 491 | Observable: aviso de zona original no debe duplicarse cuando coincide con efectiva. |
| today.tsx 286,435,452,487,493 | Separadores visuales/espacios: conservar como observables hasta revisión de composición/render; no son equivalentes por ser texto corto. |
| today.tsx 291,336,338,350,351,371,399,411,412 | Candidatos contextuales: refinicial se reinicia antes del primer snapshot; abort adicional de petición ya completada; active inicializado antes de refreshforzado; cualquier cambio numérico activa revision; depsconstantes/refreshestable; rollover se recomputa desde deadline y fuerza abort aunque argumento redundante sea false. Requieren dictamen individual, sin descuento ahora. |
| App18 | Observable: sección activa de ruta desconocida (startsWith vacío clasifica todo comoProyectos). |
| App28,29,32 | Observables: rutas con prefijo/sufijo extra para probar anchors sin lecturas accidentales. |
| App39 | Observable: main de404 no debe entrar antes del orden natural deTab por tabindex+1. |
| App40 | Separador visual; evaluar efecto, no excluir automáticamente. |
| Workspace506–511,518–523,529–534 | Indicador visual de sección: mutaciones muestran múltiplesdots, ninguno o sección opuesta. aria-current correcto no elimina por sísolo el cambio visual. Judge puede valorar redundancia contractual con border/background, documentando efecto y decisión. |
| Workspace517 | Espacio de composición visual; no exclusión automática. |
| Workspace536 | Observable: breadcrumb de ruta desconocida conserva Página no encontrada. |

No se ejecutaron replays ni se modificaron tests para esta clasificación. La reparación de foco requiere ciclo independiente con navegador y nueva línea medida: el catálogo original no generó mutante de disabled.
