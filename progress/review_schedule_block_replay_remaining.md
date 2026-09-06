# Revisión independiente del resto del replay frontend

2026-09-06. resume_review, sólo lectura. Ponytail full/Caveman lite. Leídos mutation_schedule_block_frontend_replay.md, fuente TaskBlocks completa en ramas afectadas, tests relevantes, contrato @s39–43/@s51/@s56/@s58–59, docs/mutation-testing.md y docs/verification.md. No pruebas, mutación, workaround de runtime ni cambios de fuentes/tests/config. Excluidos746,931,1070,1303/1304,1401/1403: ya asignados al autor.

## Puerta de mutación y límite del dictamen

El umbral medido está superado: el informe conserva362Killed/41Survived/1RuntimeError,89,60% bruto y89,83% evaluable, ambos>80. Es campaña de replay separada, no reemplaza score original.

**El umbral y la transparencia del error no bastan para declarar done mientras queden supervivientes observables sin resolver.** docs/verification.md, nivel5, dice: «Todo mutante sobreviviente se mata con un test nuevo o se justifica como equivalente» y prohíbe done si sobreviven sin justificar. Esta regla es más estricta que alcanzar80 de docs/mutation-testing.md. No se propone perseguir100 ni cambiar denominadores: los equivalentes se documentan; los comportamientos restantes se agrupan en pocos oráculos públicos. Una diferencia cosmética observable no se convierte en equivalencia por ser pequeña.

## Equivalencias que se conservan

- API516: hours<=18 implicado por total<=64800 y componentes no negativos.
- API552/553: instant(local+":00Z") mantiene formato completo y fecha válida aunque el regex local pierda un anchor.
- API575/576: days vacío no supera some(exceso)>0 en problema ni suma de segundos positiva de preview.
- API716: el formato validado por instant ya obliga al Z terminal.
- UI744: parser puro antes de guarda746 posterior al await; resolución root anterior.
- UI886: orígenes productivos HTML conservan isConnected y :disabled; resolución root anterior.
- UI1108: contador de invalidación, +1/-1 cambian identidad en cada reintento; resolución anterior.

Son9 supervivientes justificados individualmente, conservando su estadoS y denominador.

## Tres nuevas equivalencias contextuales propuestas

| ID / línea TaskBlocks | Evidencia de código | Propuesta |
| --- | --- | --- |
|834 /177|Es allowOverBudget inicial false→true, NO configurationFailure. Campos objetivo/inicio/fin arrancan vacíos162–164. Obtener preview válido requiere editarlos; todos sus onChange llaman invalidate(), que restablece false331 antes de guardar. No hay camino de carga automática de un borrador válido.|EQ contextual del editor privado sin prefill. No añadir test espejo del valor inicial. Reabrir monta campos vacíos de nuevo.|
|1087 /387|catch de Promise.all sólo cambia setter local configurationFailure. Reintentar configuración sólo existe después del rechazo de ese mismo Promise.all; una promesa ya rechazada no vuelve a ejecutar catch cuando acaba el otro miembro. Otra limpieza posible es desmontar BlockEditor y entonces el setter pertenece a instancia retirada.|EQ contextual para este efecto, sin callback global. No confundir con cleanup de peticiones apiRequest o con el lector padre, que sí tienen efectos externos.|
|1184 /541|Fallback de value de select null→"Stryker was here!" no cambia startOffset/endOffset. Opciones siempre comienzan por opción vacía habilitada550. ReactDOM instalado updateOptions (react-dom-client.development.js1775–1796) selecciona la primera opción habilitada cuando value no coincide; ambos valores muestran esa misma opción vacía. onChange y payload continúan usando estado real null/valor elegido.|EQ contextual con opciones cerradas y primer placeholder habilitado. Es diferente de829: éste sí cambia el estado zoneId enviado. No basarse sólo en supervivencia ni en inspección del prop React.|

Requieren ratificación de root y anotación en bitácora de mutación; este documento no cambia inventario ni exclusiones.

## Diferencias observables restantes:22 IDs

| IDs / líneas | Riesgo y oráculo público mínimo |
| --- | --- |
|750 /55|Problema no clasificable del listado deja problem=null; quitar ?. rechaza el catch asincrónico antes de setListFailure. Comprobar alerta y Reintentar bloques tras error de red/JSON desconocido, sin rechazo no manejado. Es conducta de recuperación, no comparación de excepción interna.|
|757 /62|tabIndex -1→+1 añade encabezado al principio del recorrido Tab. Es orden de teclado observable @s59, no cosmética equivalente. Un recorrido de teclado por la sección comprueba orden lógico y que el encabezado sigue disponible para foco programático.|
|814/816 /128|Esta guarda calcula eligible de un editor YA ABIERTO. El botón inicial tiene otra guarda119: probar sólo carga inicial completed enmascara estos mutantes. Abrir borrador con proyecto elegible, confirmar completed mientras sigue abierto y exigir Revisar/Guardar inhabilitados y cero creación. @s56.|
|829 /170|Zona inicial ficticia puede enviarse antes de resolver configuración, aunque el select muestre placeholder. Mantener configuración pendiente, rellenar campos públicos y revisar: payload no inventa zona. Diferencia de protocolo real, de riesgo menor por rechazo backend; no EQ sólo por que la configuración final lo sobrescriba.|
|887 /210|OR puede intentar foco en origen retirado. Sin embargo, el test existente Reenviar→rechazo ya afirma heading y sobrevivió: el origen retirado puede conservar disabled=true, de modo que guarda212 aún conduce al fallback. Antes de escribir otro test idéntico, demostrar recorrido público con origen desconectado y NO disabled, manteniendo editor vivo. No forzar refs/setters. Sigue pendiente de prueba o justificación, no declarar EQ sin cerrar ese alcance.|
|894/897/898 /220|Guardas de confirmación: siempre fallback roba foco elegido fuera; nunca fallback con body activo deja foco perdido. Un test parametrizado de confirmación diferida con destino externo deliberado o body distingue resultados de @s58. Esperar confirmación final y comprobar foco vivo, no referencia desmontada.|
|996 /303|Es setUncertain(false)→true DESPUÉS de CSRF_INVALID, no un reset de csrfRejected. Añade Comprobar guardado a la UI de CSRF aunque el aviso prioritario siga siendo de CSRF. No cambia creación automática ni identidad, pero sí acciones expuestas. Extender flujoCSRF existente para distinguir estado rechazado/estado incierto y acción manual correcta; diferencia de estado visible, no EQ cosmética.|
|1124 /454|Cambiar inicio debe retirar su offset elegido antes de siguiente preview. Elegir inicio no nulo, editar startLocal y observar payload startOffset:null. @s42.|
|1143 /483|Eliminar spread borra también opciones de inicio al cambiar fin. El test actual conserva referencia start y consulta su valor aunque ya pueda estar desmontado. Reconsultar selector por label tras cambiar fin, exigir presencia/conexión y selección preservada; comprobar nuevo payload. @s41–42.|
|1154/1157 /506/509|Cambiar zona después de revisión debe retirar review/consentimiento y ambos offsets. El test actual cambia zona con endOffset ya null y puede ocultar1157. Partir de ambos offsets no nulos, revisión válida/aceptada, cambiar zona y comprobar estado/payload después de revisión nueva. @s42.|
|1186 /544|Cambiar ocurrencia sin invalidate conserva preview y permite enviar offsets de preview anterior (save usa review.startOffset/endOffset272–273). Riesgo de intención real. Con revisión vigente y consentimiento, cambiar ocurrencia; review desaparece, Guardar no permite enviar y siguiente revisión usa elección nueva. @s42.|
|1241/1268/1269 /600/643–644|Quitar espacios genera texto unido (Inicio:fecha, reservado0,3600segundos). Diferencia visible menor, NO equivalencia. Una aserción de frase legible completa por región de revisión/error cubre separación semántica; no snapshot global ni test por literal JSX.|
|1248/1250 /617|Añaden checkbox en días sin exceso. El test existente espera ausencia después de disparar preview y puede pasar durante setReview(undefined), antes del nuevo resultado. Esperar región de revisión y cifra0 FINAL, luego ausencia de checkbox y Guardar habilitado. @s43.|
|1267 /641|Desaparecen cifras de días del BUDGET_EXCEEDED, aunque aviso general permanece. Comprobar fecha/presupuesto/reservado/solicitado/exceso del rechazo vigente, agrupado con legibilidad. @s16/@s49.|
|1273 /650|Enlace de disponibilidad aparece ante errores ajenos o sin error; puede llevar a perder borrador por una recomendación incorrecta. Comprobar ausencia del enlace en rechazo de negocio ajeno y presencia sólo cuando disponibilidad requerida/inválida. No equivalente.|

No son22 bugs demostrados de producción: son cambios introducidos por mutantes que las pruebas actuales no distinguen. La autoría de producción sigue sin hallazgos nuevos aquí.

## Agrupación mínima de seguimiento

1. Recuperación de listado desconocido y estadoCSRF: ampliar sus flujos existentes con resultado visible (750/996); no inspeccionar variables internas.
2. Elegibilidad y carga inicial: borrador abierto→proyecto completed y configuración pendiente sin zona inventada (814/816/829).
3. Foco y teclado: confirmación en dos destinos, orden Tab y sólo si existe recorrido legítimo el origen retirado habilitado (757/887/894/897/898).
4. Edición DST: reutilizar flujo con ambos offsets elegidos; cambios inicio/fin/zona/ocurrencia con revisión vigente, verificando DOM actual y payload (1124/1143/1154/1157/1186).
5. Presentación semántica del presupuesto y acciones: revisión final sin exceso; rechazoBUDGET con cifras y frases legibles; enlaceconfig ausente en otro problema (1241/1248/1250/1267/1268/1269/1273).

Esta agrupación satisface la política más estricta sin campañas por cada implementación. Los siete IDs ya asignados quedan fuera; no duplicar sus tests. Los IDs cosméticos pueden resolverse en las mismas aserciones de presentación, pero no justificarse falsamente como equivalentes.

## RuntimeError945 / replay286

Se mantiene como RuntimeError, no Killed ni Survived inferido. El error literal demuestra fallo de serialización del error en @stryker-mutator/util.errorToString durante VitestTestRunner, tras dos reinicios internos; no prueba por sí solo el resultado del mutante. La retirada de ?. en check259 sí tiene un vector semántico posible con problema desconocido/null, pero no se deduce un kill de esa intuición.

No hay regla explícita en docs/mutation-testing.md que exija0RuntimeError ni que obligue a modificar el runner. El gate numérico80 pasa incluso contando el error en total404. Por tanto, **el error transparente aislado no invalida por sí solo ese PASS de umbral**; debe quedar como límite de medición en el judge, sin cambiar score ni tratarlo como equivalencia. La prohibición de done de verification.md se aplica ahora por supervivientes no justificados independientemente de945. Cerrar éstos no convierte automáticamente945 en Killed: el juez deberá aceptar explícitamente esta limitación de herramienta si decide cerrar con ella. No recomiendo workaround, reinstalación o replay reiterado sólo por este error; tampoco afirmar que el error está resuelto.

## Revisión independiente del segundo refuerzo UI — corte previo a correcciones

Revisor backend, sólo lectura de fuentes/pruebas; diff completo task-blocks.test.tsx frente56ced31 leído en e44257 y recorridos focales10422a. Los once casos netos nuevos (nueve tests y dos filas de matriz), más los ajustes de oráculos existentes, mantienen operaciones públicas y fixtures de DTO coherentes. Las respuestas JSON diferidas atraviesan los parsers reales; no contienen detección de mutantes ni llamadas a handlers/refs privados. El recorrido de foco887 observa el nodo real retirado y no modifica su estado disabled.

Hallazgo pendiente1143: la reconsulta getByLabelText de inicio se añadió antes de editar Fin del bloque; después de editar fin aún aparece expect(start).toHaveValue sobre la referencia anterior. Esa referencia puede conservar el valor aunque desaparezca el selector del DOM. Corregir esa segunda aserción para consultar el selector vigente después del cambio. La frase de la bitácora que afirma reconsulta posterior todavía no corresponde al corte leído. Comunicado al autor y al coordinador; no se modificaron tests desde esta revisión.

También se espera la corrección@s40 ya solicitada por root: contar dos llamadas preview y comprobar la última petición después de resolver configuración, conservando el oráculo1082 del flujo previo. El dictamen final queda pendiente de ambas correcciones y su evidencia focal. No se atribuyen kills de los29 IDs propuestos por pasar pruebas sin mutación.

## Dictamen independiente final del segundo refuerzo UI

**APPROVED diseño y cobertura del refuerzo** sobre task-blocks.test.tsx frente56ced31, después de las dos correcciones. Lectura6af6c0 confirma que el selector de inicio se consulta desde el DOM después de editar fin (1143), y que disponibilidad espera dos peticiones y comprueba la última después de resolver configuración, preservando1082 además del nuevo oráculo829. El autor corrigió la afirmación prematura de su bitácora. Sin otros hallazgos en el diff completo.

Los29 IDs propuestos se atienden mediante cinco entrelazados de asincronía y grupos de recuperación, elegibilidad, teclado/foco, edición DST y presentación. Se conservan los casos anteriores de preview/error al ampliar la matriz de foco con confirmación; la recuperación CSRF mantiene sus comprobaciones de reenvío manual e identidad. Las aserciones de presupuesto esperan el resultado final y exigen texto y acciones concretas; las de edición comprueban DOM actual y payload. Los fixtures temporales retienen fetch o JSON según la frontera necesaria, con DTO coherentes al liberarlos. No hay detección de mutantes, fixtures que eviten los parsers productivos ni cambios de producción. No se exige test por ID ni se confunde equivalencia con falta de aserción.

Evidencia del autor:84/84 verdes fee3b5 antes de las dos correcciones, complementados por focales e892db y4f8899 GREEN; formato91cda6, ESLint54bccd y TypeScriptc13610 verdes después. Diff --check6af6c0 limpio. Esta revisión no ejecutó pruebas ni editó fuentes/tests. La regresión global del corte final corresponde al coordinador; no se presenta la regresión84 previa como repetida tras las correcciones. Los29 resultados de mutación continúan pendientes de medición autorizada;945 permanece bajo el dictamen separado de limitación del adaptador. No se declara done.

## Revisión independiente del ajuste de sincronización create-task

**APPROVED**, diff finalf78766. El test «una revisión tardía no restaura proyecto retirado por un404 de sus acciones» conserva el aborto obligatorio y las comprobaciones públicas después de liberar la respuesta antigua: no reaparecen Tareas ni el encabezado privado. Captura la señal del GET identificado por URL y método, espera el mensaje terminal404 y luego la cancelación del efecto. No aumenta timeouts, elimina aserciones ni fuerza la cancelación desde el test.

Lectura productiva readonly d62bfe confirma que useProjectTasks cancela review.current en cleanup de useEffect; la ausencia de región en DOM no certifica que ese cleanup ya haya terminado. El diagnóstico del autor d1fcb9 observó esa ventana; fue instrumentación con fallo deliberado, no RED de producción. La instrumentación está retirada del diff final. No se atribuye el incidente a un índice de llamada equivocado ni a un defecto productivo.

Evidencia recibida: focal ajustado364a22 PASS, suite create-task55/55 PASS7c3c42, formato32bf5d, ESLint31b060, TypeScript79b6ea y diff --check9cb74d verdes. Bitácora del incidente revisada al final de tdd_schedule_block_frontend.md. Este revisor no ejecutó suites ni modificó fuentes/tests. El init global final sigue correspondiendo al coordinador; esta aprobación no adelanta su resultado ni autoriza mutación por sí sola.
