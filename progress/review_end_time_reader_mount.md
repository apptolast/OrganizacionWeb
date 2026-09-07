# Revisión independiente del montaje Reader17 y delta End

**APPROVED para el paquete Reader+End congelado**, incluidos reset de carga y encabezado contextual. Los gates globales y la validación UX real siguen separados. Dictamen de lectura sobre `progress/end_time_frontend_reader_freeze.json`, cuatro hashes idénticos en `aa101f`. Evidencia del autor:79/79 GREENe27656 (Reader47 y End32), formato c60f75. Sin ejecución propia, cambios de fuente/tests ni Gradle. El panel/coordinador base ya fue revisado en `review_end_time_frontend_panel.md`; aquí se revisan el montaje y sus deltas.

## Resultado del montaje revisado

La instancia local de decisión se comparte entre CLOSE y End. La adquisición impide decisiones hermanas durante envío/incertidumbre; los rechazos definitivos liberan el dueño, conservando el borrador para nueva confirmación. El éxito EXTEND incrementa generación y obliga a S nueva antes de CLOSE, con guardia real además de aria-disabled. S/F se registran en el coordinador y se abortan antes del observer401 al adquirir otra decisión y al confirmar. El test con HTTP401 difiere la entrega HTTP, no un parseo posterior imposible del observer síncrono.

Reader mantiene la identidad inmutable validada separada del snapshot mutable. Así, limpiar S tras CLOSE412 no desmonta End ni pierde su recibo EXTEND confirmado. Sclosed válida o CLOSE validado propagan knownClosed: se retiran controles y timer inmediatamente sin esperar F/E; se conserva la instancia y una intención retenida aún puede comprobarse. El recibo CLOSE no se relee innecesariamente por F ni se sustituye por otra sesión de GETactive. OpenSessionAfterClosure consulta A aparte y su controller se aborta al desmontar; no hay consulta A del Reader abierta antes de CLOSE que necesite otro coordinador padre.

El test de cierre con E pendiente distingue correctamente la espera legítima de E de una falsa espera S ya resuelta. Otro caso descubre cierre externo mediante S antes de recibir F y retira controles sin afirmar que ese cierre confirma una intención propia incierta. No se recalcula ni se altera el recibo histórico.

## Privacidad y hallazgos corregidos durante el corte

La lectura preliminar detectó que retirar E podía dejar una S pendiente capaz de restaurar session/snapshot. B contrastó la rama: el primer test resultó inicialmente verde porque el remount recibía otro404; la aserción de dos lecturas E frente a tres reveló RED3d85cb. El freeze incorpora lookup.current.abort en inaccessible, junto al aborto de CLOSE y retirada de snapshot, identidad, recibo y notas. El oráculo observa que una S200 tardía no restaura controles ni provoca remount. Se conserva explícitamente el incidente de oráculo; no se fabrica RED anterior.

S válida pero con contexto ajeno también usa retirada completa, no sólo setFailure. End compara el contexto que ya conoce con S/E recibida. Las respuestas se clasifican con guardas de aborto tras await; los datos de otra tarea no se incorporan a la UI. La consulta F y su respuesta permanecen separadas del hecho confirmado por una key propia.

### Único ajuste pendiente al cerrar la primera lectura

`inaccessible` abortaba la propia consulta S pero no limpiaba loading. En un rechazo inicial S (404 o contexto ajeno), el finally abortado omitía el reset y persistía «Consultando sesión de trabajo» junto al error. Se comunicó a B/root y se acordó reforzar únicamente el caso existente de rechazo inicial por otra tarea con ausencia del anuncio, y limpiar loading durante retirada. Este detalle impide aprobar la presentación de estados de carga hasta verificar ese delta. No se piden nuevos escenarios ni una matriz.

## Límites

El dictamen cubre Reader+End y su composición con los componentes ya aprobados, no navegador/UX física, Stryker17 ni smoke completo. La evidencia79 es del autor y no equivale a los132 ejemplos del contrato. Se revisaron guards/intención/foco de los deltas; no se reabre toda la feature16 ni backend. El formulario EXTEND está separado del formulario CLOSE, sin submit anidado ni arquitectura nueva. No se encontró otro bloqueante distinto en este alcance.

## Cierre del detalle de carga y cesión

R1 carga cerrado: el caso existente de rechazo inicial por contexto ajeno ahora exige ausencia de «Consultando sesión de trabajo», REDc2d731 → Reader47/47 GREENbf0fe9, formato fa36fc y lint509c12 del autor. Lectura9cba73 confirma `setLoading(false)` en inaccessible antes de retirar datos. Manifiesto renovado458fa8: Reader01D4C642A06833B5200B24A75FD273A8796D30A838B3DAE991DDEB2159ED6994; test87BF7723DD4B6D47899FB86813374E492B7E4A96CC76A939CD14BD13CEF791DB. End y su test siguen el freeze anterior.

No queda bloqueante funcional en el montaje revisado. Se liberaron explícitamente Reader y End para el ajuste que root señaló: h2 del panel bajo h1 del Reader, conservando h3 bajo h2 en tarea. Es un delta semántico contextual observable con un oráculo DOM; no se atribuye un fallo de axe ni se condiciona a una nueva matriz UX. El dictamen de este corte es parcial mientras ese delta está en curso, sin retener fuentes ni pedir repetición de las suites ya acreditadas.
## Freeze final del encabezado — APPROVED

Delta semántico verificado `e08416`: End recibe `headingLevel?:2|3`, default3; Reader pasa2. El mismo elemento conserva ref/tabIndex para el foco; no cambia timers, red, DTO ni coordinación. El oráculo nominal existente exige heading «Fin de la sesión» level2 en Reader, REDbe8b9d. B acredita regresión8 suites248/248 GREENdf8548 y formato e51a79/lint e0cfe4/types6823fd; esta revisión no ejecutó esos comandos.

Cuatro hashes finales de `end_time_frontend_reader_freeze.json` (5df5ff) comprobados idénticos en e08416: Reader6B0B0DDF00A55D047F035210687AD35A8E7EDD4B9D98CB7F3D9D4D57CB53E9D5, test593AB44C7121E998ACCAF0879114F589D4564BF168F04AF2D0743A101E6F88AC, End667FA2EEBA44225BD2CB19A9E7DA02BFE664AB158F7212EF106CE1872CEDFBFA, testDEF535E69DA0DBD1C8F4513468B4C404A2BE687475903469EB3C1F9690DE9D1B.

No quedan cambios solicitados en este paquete. Reader y End quedan liberados para integración/gates coordinados; esta aprobación no certifica aún la feature17 completa ni afirma pruebas físicas, axe o campañas no realizadas.
## Delta posterior: identidad ajena de F — APPROVED

Se conserva el dictamen anterior como revisión del snapshot 5df5ff. Root detectó después una rama diferenciable: S closed incorpora identidad y snapshot antes de esperar F, por lo que un rechazo de contexto de F que sólo mostraba el error dejaba visible el panel End. El delta revisado cambia esa rama a `inaccessible()` (`work-session-reader.tsx:178`), que aborta lecturas/comando, limpia carga, identidad, snapshot, recibo y borradores. No se incorpora el cierre ajeno ni queda el panel montado.

El caso existente de F con otra tarea ahora difiere F y exige que el fin propio de E sea visible antes de resolver el recibo incompatible. Después exige error y ausencia del fin, heading End, cierre/notas y anuncio de carga. Así observa la retirada de información que sí estaba montada, sin depender de que F llegue antes de E. B documenta RED c10ff6 → Reader 47/47 GREEN a63304, formato 9f1835 y lint b62ae3 en ciclo 81; esta revisión no ejecutó suites.

Manifiesto 02d9dd comprobado contra los cuatro archivos en lectura 2f89d5: Reader FD83DD0B37EC1B3CDDED8857B4D8E512EDDD4A53C27A4817BEE4A4B0E8F54153; test 4893F2F542CE15462659D9A33AFCB2F6C1B0A1FF80EE6DFA1B13F48AF805DE14. End y su test mantienen exactamente los hashes del apartado anterior. Lecturas b777de y d9463d verifican rama y retirada compartida.

APPROVED para este delta puntual de privacidad @s42. Sin cambios solicitados; se liberan los archivos para los gates coordinados. No se amplía la revisión a otras matrices de 16 ni se atribuye una nueva ejecución de las 248 pruebas al cambio posterior.
