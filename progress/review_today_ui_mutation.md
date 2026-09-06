# Revisión independiente — refuerzos UI/App/Workspace de Hoy

Rol judge readonly, posterior a campaña original521mutantes. Autoría de estos refuerzos: resume_review; revisión no ejecuta tests ni cambia fuentes/config. API35 tiene autor/revisor distintos y no se autoaprueba en este documento. Alcance today.test.tsx y progress/tdd_today_ui_mutation.md, original preservado.

## Corte preliminar27e210 — sin hallazgos, pendiente freeze final

Leídos ciclos1–11 y diff completo presente. Fixtures de presupuesto conocido/fallback conservan coherencia; aserciones se dirigen al dd de exceso y aviso correspondiente. Etiquetas de actual/próximo se comprueban dentro del item correcto, con ausencias en el otro. Intervalos positivos sin solapes, nombres/IDs/createdAt permanecen válidos.

Obsolescencia: respuesta503 antigua llega después de sustituir generación mientras la nueva permanece pendiente; el test comprueba aborto, ausencia de alerta, loading y coalescing de focus, antes de confirmar respuesta vigente. No difiere artificialmente un observador401 síncrono. Respuesta recibida oculta exige cero timers y ninguna lectura hasta recuperación. Reintento confirma retirada del error anterior y cierre vacío explícito.

Navegación: rutas con prefijos/sufijos no autorizados deben permanecer404 sin GET; se verifican recuperación, tabindex-1 y breadcrumb/aria-current cuando corresponde. No se fuerzan nombres de entidad inválidos como datos válidos ni se exige secciónnull a rutas bajo el espacio/proyectos que antes lo conservaban.

Liberación de recursos: captura listeners propios realmente registrados para visibility/focus y exige su retirada al desmontar. No fija cantidad de hooks ni estructura de callbacks; es un oráculo de propiedad de suscripción, no prueba de getter.

Tiempo fraccionario399/412: el test controla performance.now monotónico con avance0,5ms entre captura/programación y adapta setTimeout a truncamiento entero del delay. No llama refresh ni inspecciona refs; observa agenda antes/después, carga nueva y número de peticiones por las dos entradas boundary/visibility. Se presenta explícitamente como modelo de conversión temporal de plataforma, no como medida de latencia ni ejecución de mutante. El borde exacto329 reaparece sin ejecutar timer de deadline y exige aborto/retirada inmediata.

Los separadores de tiempo/zona y enlaces se prueban como composición legible; no se equipara toda decoración a equivalencia ni se inspecciona HTMLinterno para identificar mutantes. Pendiente cierre del autor con regresión, formato y argumentos individuales antes de emitir aprobación final.

## Revisión de freeze56 — observación puntual pendiente

Corte final informado b7ba6a:56/56 tests, formato y tsc/ESLint ae1ea7; relectura91aa76/e201a8 confirma16casos/11ciclos y ajuste exclusivamente de tipo DOM/Node del retorno setTimeout, sin cambiar temporización. Argumentos29E de UI/Workspace se remiten al dictamen independiente review_today_mutation_candidates.md ya ratificado por root: se distinguen equivalencia lógica y aceptación contractual de diferencias visuales redundantes; no se afirma render idéntico ni se ajusta score.

Hueco concreto de oráculo292: ciclo5 recibe un rechazo inmediato y espera alerta; para entonces failure=true tanto con inicializaciónfalse como conmutantetrue. El caso inicial @s21 tiene GETdiferido, pero sólo comprueba ausencia de alerta después de confirmar snapshot, cuando setFailure(false) ya borró la diferencia. Falta comprobar ausencia de fallo mientras primera lectura está pendiente. Se propone reforzar ese caso existente, sin crear testporID ni reclamar muerte. Comunicado autor/root antes de aprobación final; no se ejecutó prueba desde judge.

## Dictamen final — APPROVED

Hueco292 resuelto mediante una aserción pública en @s21 existente: mientras Response sigue diferida, carga no muestra ninguna alerta; después conserva los mismos oráculos de vacío confirmado. Autor documenta inicialmenteGREEN2f0a3a, regresión56/56GREEN98844f y ESLint/tsc/diff-checkd7d862. Bitácora ciclo12 corrige expresamente la atribución anterior:292 queda en@s21, ciclo5 sólo347/426/471. No prueba nueva porID ni producción adicional.

Aprobados los16casos nuevos y el refuerzo del caso existente, sobre fuentes productivas intactas. Tiempo fraccionario399/412, recuperación aldeadline329, generación obsoleta y recursos tienen oráculos observables adecuados; no se exige muerte anticipada de cada mutante. Las29E UI/Workspace mantienen condiciones/documentación del juez independiente y ratificaciónroot; esta revisión no reduce denominadores ni altera el reporte original. No quedan cambios requeridos en este alcance. Coordinador conserva puerta global y autorización de replay separado; no se ejecutó medición desde esta revisión.
