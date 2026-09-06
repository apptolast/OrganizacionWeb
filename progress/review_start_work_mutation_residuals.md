# Revisión independiente de residuos frontend14

**Dictamen: umbral aprobado; refuerzo contractual todavía necesario.** Lectura de los 81 Survived del inventario original, fuentes y pruebas, sin ejecutar suites ni editar producto durante init46298. Ponytail full y Caveman lite. La campaña conserva 452/533 (84,8030 %), cero errores y timeouts. Este documento no cambia estados del reporter ni atribuye detecciones futuras.

Propuesta para root: 21 equivalencias contextuales justificadas abajo y 60 diferencias observables que requieren cobertura o decisión contractual explícita. No son 60 tests nuevos. Las prioridades siguientes agrupan comportamientos y reutilizan pruebas existentes. Los IDs son globales del JSON original; por ejemplo, duración POST es **141, en línea 134**, no ID134.

## Prioridades y oráculos mínimos

| Prioridad | IDs | Evidencia y refuerzo |
| --- | --- | --- |
| P1 | API141 | work-session-api.ts:134 exige la duración enviada además de coherencia interna. El test de recuperación:111 sí cambia duración/fin juntos, pero falta hacerlo en POST. Responder 26 minutos y fin +1560 s a una intención de 25; el cliente rechaza y la UI conserva incertidumbre. |
| P1 | API29,32,35,37,54,58,61,65,66,69,72,73,76 | readWorkSessionError:31–57 reconoce códigos cerrados. Añadir una pequeña matriz de un defecto por fila: active con code/type/status corporal/status HTTP incoherentes; problemas simples con type/status incoherentes, código desconocido y pares NOT_FOUND/409 o TIME_OUT/404. Debe resolver null, preservando el body original. Un problema conocido mal tipado puede autorizar reenvío o convertir incertidumbre en rechazo. No aceptar un objeto porque conserva un title legible. |
| P1 | UI367,369,370 | work-session.tsx:108–109 retira ausencia tras ALREADY_ACTIVE. El test:702 espera el aviso y pulsa Actualizar inmediatamente; no intenta empezar otra vez entre ambas acciones. Antes de actualizar, comprobar que no se afirma ausencia ni se envía otro POST. |
| P1 | UI257,258,261; Reader1,3 | Elegibilidad:50–52 y task-reader.tsx:130. El test UI:565 empieza con ambos estados undefined, por lo que no aísla las dos guardas. Dos estados independientes: tarea completed con proyecto active; tarea pending con proyecto desconocido. En composición, recargar proyecto antes conocido y dejarlo pendiente o fallido; no reutilizar su elegibilidad anterior ni enviar POST. |
| P1 | UI429,440 | Lookup:146/152. Un GET active viejo puede rechazar después de un refresco nuevo: con 401 no debe llamar al padre actual; su finally no debe retirar “Consultando” mientras el GET nuevo sigue pendiente. El test:685 entrega éxito viejo, no cubre el catch; el test:640 cubre POST, no este GET. Usar padre vivo y respuesta diferida, no inspeccionar refs. |
| P1 | UI504,502 | Submit:213–214. Durante incertidumbre, Enter no debe emitir POST ni navegación nativa. Reutilizar el formulario visible y observar solicitudes/evento cancelado. send() permite reenvío con retained, por lo que la guarda del submit aporta protección real. |
| P2 | UI294,296,297 | Duración local:62–63: 1 y 1440 deben iniciar; 1441 no. Los límites HTTP/API no acreditan esta interacción. Extender el caso accesible de duración, una frontera por ciclo. |
| P2 | API190,191,192,201 | Precisión:167–169. Una pareja válida con fracciones equivalentes escritas como .1Z y .100000Z detecta representaciones mal escaladas (190/201). Para191, inicio .000000 y fin un segundo antes con .500000 puede adquirir indebidamente el segundo perdido al sumar dos veces milisegundos. 192 elimina Z y vuelve Date.parse dependiente de la zona local; verificar con zona no UTC y cruce DST controlado, sin simular éxito del parser. |
| P2 | UI384,386 | Error de campo:118. VALIDATION_ERROR sin plannedMinutes debe usar title; con error ajeno primero y plannedMinutes después debe elegir el mensaje del campo correcto. El test:834 contiene sólo el campo coincidente. |
| P2 | UI236,240,249,305,316,318,322,375,393,467,469 | Estados visibles: carga inicial sin incertidumbre y duración vacía; corrección retira el error previo; confirmación no conserva carga/error/incertidumbre; rechazo definitivo permite nueva intención; dos actualizaciones consecutivas sí consultan y retiran el fallo durante la carga. No comprobar directamente estados React: usar anuncios, input, botones y número/identidad de solicitudes. |
| P2 | UI446,447 | La sección y su heading deben conservar nombre accesible asociado (159/161). Un único oráculo getByRole(region, nombre Sesión de trabajo) acredita la asociación; no fijar el ID generado por React. |
| P2 | UI494,495,496,497,498,499,500 | Texto de inelegibilidad:201. Observar ayuda cuando los estados no permiten iniciar y su ausencia en contexto elegible o intención incierta. Son diferencias de información, no equivalencias por ser texto. Reutilizar la matriz de elegibilidad. |
| P2 | UI541,543,545,546 | SessionFacts:297–303. Para zona soportada, mostrar hora/fecha locales y no anunciar respaldo UTC. Locale vacío y estilos vacíos provocan fallback o cambian presentación; el test:488 sólo verifica la zona retirada. Añadir un nominal legible con fecha, hora y zona reales, sin afirmar todo UX por comparar opciones de Intl. |
| P3 | UI532 | Aviso de salida:269. Mutarlo a true anuncia un “inicio transmitido” antes de enviar nada. Distinguir antes/después de transmisión en el test existente de cierre, sin exigir otro flujo completo. |

Estos grupos cubren los 60 IDs no equivalentes propuestos. Priorizar P1 y fronteras temporales; no lanzar otra campaña completa sólo para perseguir el 100 %. Si quedan observables sin test, conservarlos pendientes según docs/verification.md; el umbral no permite llamarlos equivalentes.

## Equivalencias contextuales propuestas, caso a caso

| ID | Justificación en el código actual y límite |
| --- | --- |
| API14 | catch JSON devuelve undefined en lugar de null; ambos fallan exact() y readWorkSessionError retorna null. No escapa el valor intermedio. |
| API174 | Sin ambas guardas de null, uno inválido provoca TypeError de bigint, o ambos inválidos dan false; los cuatro lectores públicos siguen rechazando. Sus consumidores no usan la clase/mensaje del Error de DTO. |
| API175 | El OR permite la resta con un null y provoca rechazo; con ambos null permanece false. Misma limitación contractual que174. |
| API176 | start null con end bigint provoca rechazo; no puede producir un DTO válido. |
| API178 | end null con start bigint provoca rechazo; no puede producir un DTO válido. |
| API188 | instant() ya exige un único Z terminal y ninguna basura. Quitar esta segunda ancla no cambia los strings alcanzables. |
| API189 | Sin fracción, no reemplazar Z por Z conserva exactamente el string; con fracción sigue retirándola. |
| API195 | La captura de fracción sólo recibe strings aprobados por instant() y acabados en Z; el ancla final es redundante aquí. |
| API199 | Cada fracción f está en [0,999999]. El único uso del valor es end-start = entero de minutos en microsegundos. Tanto sumar como restar f exige f_final=f_inicial y la misma diferencia de segundos: una diferencia no nula de fracciones no alcanza 1.000.000. El bigint calculado no se devuelve; los strings originales sí. No sería equivalente si se expusiera el timestamp numérico. |
| Reader6 | En el branch !projectLoading && !projectFailure, readProjects de ruta de detalle sólo puede devolver ProjectSnapshot validado o rechazar. El primero establece snapshot antes de finalizar carga; el segundo conserva failure o retira el padre. El retry reinicia loading junto con snapshot. No existe snapshot ausente alcanzable en ese branch mediante el cliente real. |
| UI233 | El heading siempre se renderiza y su ref se asigna antes del layout effect; no hay branch que quite ese h2 manteniendo Session montado. |
| UI238 | checking inicial sólo aparece bajo busy, inicialmente false. Cada send asigna checking antes de presentar busy; no se observa el valor inicial cambiado. |
| UI273 | Sustituir [] por un array con string constante mantiene las dependencias iguales en cada render y el cleanup sólo al desmontar. |
| UI313 | El comando sólo se aborta al desmontar Session. Después de ese await exitoso, todas las acciones son setters y refs de esa instancia retirada; no hay callback al padre ni navegación. WorkSession usa key por proyecto/tarea, por lo que no alcanza las refs de la nueva instancia. No extrapolar a los catches con onAccessFailure. |
| UI314 | El efecto inicial asigna lookup.current antes de que el usuario pueda enviar; ningún camino vuelve esa ref a null. El resultado exitoso siempre encuentra un controller, incluso si ya terminó. |
| UI411 | La guarda completa del finally sólo evita setters y limpiar la ref de un comando abortado en una instancia desmontada. Mientras está montada, command.current impide otro send concurrente. No hay callback externo. |
| UI413 | Cambiar AND por OR añade el mismo trabajo local tras desmontaje descrito en411; no habilita un comando nuevo en la instancia viva. |
| UI415 | Durante un send vivo no se reemplaza command.current: sólo este send la establece y su finally la borra. La identidad adicional es redundante mientras siga ese protocolo. |
| UI470 | refresh sólo es una dependencia; aumentar o disminuir produce un valor distinto en cada clic, sin mostrarlo ni compararlo con límites. |
| UI554 | Quitar el espacio después de “Inicio:” conserva etiqueta, dos puntos y elemento time con toda la fecha/hora. Equivalencia restringida al contrato de información/legibilidad, no igualdad DOM ni de píxeles. No se concatenan dos valores sin separador. |
| UI555 | Igual argumento para “Fin previsto:”; conserva los dos puntos, fecha/hora y dateTime íntegros. |

Las 21 propuestas requieren aceptación de root; ninguna reescribe el estado Survived ni el score bruto. Las guardas se revisaron en composición, no se propone eliminarlas de producción.

## Alcance y coordinación

Fuentes leídas: work-session-api.ts completo, work-session.tsx completo, TaskReader y readProjects en los branches afectados; pruebas API/UI y api-client/readBlockError/instant compartidos. Evidencias de lectura 3d1cf1, 6c1440, 3d701b, c0c932 y 1fc51f. El inventario es el original de 81 entradas; no se han generado nuevos resultados.

Autor frontend informado de prioridades mientras termina E2E. Siguiente paso propuesto: asignar pocos ciclos de comportamiento y después seleccionar un replay trazable, conservando reportes originales. Ninguna fuente, prueba, configuración ni archivo protegido se modificó en esta revisión.

Auditoría de IDs 4e261a: 81 entradas, 21 propuestas E y 60 observables, sin omisiones ni IDs ajenos. Hashes de fuentes del corte: API B50C97B6309B68389257D4A02BE132789F3A1B91E082696736166786BC1D87A9; UI 433D5A00A5A2C233DAB2B69C0FFC71B709F7B1A7E85CE71EFEAC8FBF3F29AB2F; TaskReader 0CC6ED956F083FE7262F512DA2830F11FB46596191DDD4E6C2DA5945E187402A.
