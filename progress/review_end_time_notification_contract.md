# Revisión de contrato17 — frontend y recorrido

**APPROVED para destilación y revisión de Gherkin.** Las dos concreciones solicitadas quedaron incorporadas en normativa SHA256 `2F44F439F7ED17E19CAE470B911257A3C380321EF20E41E75F09B35FB10ACE02`, verificada719e25. El modelo de fin durable, State6 conservado y recibo EXTEND discriminado es coherente. La solicitud anterior de cambios se conserva explicada abajo como historial de revisión; no queda bloqueo de este dictamen. No acredita implementación ni pruebas17.

Lectura: sección17 de [project-spec.md](../project-spec.md), [propuesta histórica](proposal_end_time_notification.md) y fuentes14–16 vigentes; referencias5a9b08/aba231. No código, tests ni suites ejecutados.

## Concreciones necesarias

1. **El aviso debe encontrarse sin abandonar el recorrido14.** Hoy TaskReader monta WorkSession, que monta WorkSessionStatePanel para la activa, incluida una activa propia de otra tarea. La URL `/proyectos/{p}/tareas/{t}/sesiones/{s}` monta WorkSessionReader, un árbol diferente destinado al cierre16. Añadir sólo un panel al Reader dejaría sin aviso al usuario que inicia y permanece en la tarea. Precisar en norma que aviso/fin y acceso a ampliación aparecen tanto junto al estado de la activa en detalle de tarea como en la URL conocida de esa sesión abierta. El enlace «Cerrar sesión de trabajo» conserva la navegación16 anterior al POST. Puede reutilizarse un único componente local montado en ambos recorridos; no requiere layout global ni router nuevo.
2. **El plazo acumulado puede superar2^31−1ms.** El máximo1440 es por ampliación, no acumulado;25 ampliaciones anticipadas de un día ya superan el rango seguro de una espera nativa. Precisar que se fragmenta la espera y se rearma con el tiempo monotónico restante, sin convertir el plazo completo a un delay que desborde ni consultar repetidamente al instante. Un fragmento agotado antes del fin sólo rearma; al vencimiento efectivo consulta al servidor antes de avisar. Un callback anticipado o resto fraccionario positivo no genera un bucle de delay0. Los instantes/fórmulas se validan en microsegundos exactos; sólo el delay acotado se convierte a milisegundos.

El fundamento del segundo punto es el parámetro `long timeout` de la [interfaz HTML](https://html.spec.whatwg.org/multipage/webappapis.html#windoworworkerglobalscope); los [algoritmos de timers](https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#timers) normalizan retrasos negativos y no garantizan ejecución puntual. La fragmentación es una recomendación de implementación derivada, no una capacidad nueva de producto.

## Coherencia del montaje y consultas

- En detalle de tarea, GETactive sigue siendo global del propietario: la identidad esperada de GETend-time corresponde a la sesión devuelta por active, aunque p/t sean distintos de la tarea mostrada. El contexto p/t/s estricto corresponde a la URL16 y a intenciones conocidas; no rechazar una activa legítima de otra tarea.
- Mostrar original14 como hecho histórico y fin efectivo como actualidad consultada. Un EXTEND confirmado no modifica SessionFacts ni plannedEndAt. Estado/neto15 y end-time17 son snapshots separados: tras una decisión local se refrescan los dependientes y se descartan respuestas iniciadas antes de ella, evitando presentar una revisión anterior como actual.
- Una intención local incierta conserva cantidad/key/token. Coordinar controles de la misma sesión para que PAUSE/RESUME/CLOSE y EXTEND no permitan otra decisión simultánea desde componentes hermanos; no basta deshabilitar únicamente el botón de ampliación. El servidor conserva su arbitraje412 entre pestañas. Navegar sigue permitido y salir no revoca un POST ya transmitido.
- En el Reader16, el formulario de cierre y la ampliación deben ser decisiones explícitas diferenciadas, sin botones submit de una acción dentro del formulario de la otra. Closed conserva recibo/notas y retira aviso/ampliación, incluso si GETactive descubre después otra sesión. No inyectar ese nuevo estado en el recibo antiguo.

## Temporizadores, recuperación y privacidad

La norma ya exige volver a consultar al recuperar visibilidad/suspensión, coalescer GET pendiente, conservar aviso confirmado ante errores y anunciar una vez por fin sin robar foco. Concretar esos oráculos en los recorridos anteriores: snapshot abierto futuro→callback→GET pendiente→respuesta vigente; hidden→visible durante GET sin duplicarlo; consulta anterior al POST que termina tarde sin restaurar el fin previo. Una nueva ampliación vigente rearma y permite un nuevo aviso para el nuevo fin, no un anuncio por render.

El fallo inicial de GETend-time no permite afirmar vencimiento; el fallo de actualización conserva el último aviso como hecho confirmado y muestra error/reintento separado.409 temporal no es ausencia. Un recibo EXTEND recuperado se conserva aunque falle la consulta actual, sin atribuir aquella ampliación a una intención perdida tras recargar sin key.

Reutilizar apiRequest y guardas después de cada await, incluyendo clasificación de error: entregar un Response HTTP401 antiguo después de navegar debe quedar abortado antes del observador de acceso. No simular ese401 dentro de JSON.401/404 de propiedad actuales retiran datos/borradores; un404 reconocido de K habilita sólo el reenvío manual de la misma intención, sin interpretarlo como rollback probado.412 conserva cantidad y requiere consulta abierta válida más nueva confirmación;403 desconocido nunca habilita reenvío.

## Decoder y compatibilidad

GETend-time es objeto exacto3 y exige token canónico acorde a State6. El decoder compartido State6 no necesita relajarse. EXTEND7 exige extension3, cantidad1–1440, identidad original, before/after abiertos iguales salvo revisión+1 y fórmula `max(previousEndAt,occurredAt)+minutos` exacta, incluso diferencia de un microsegundo. Sólo EXTEND permite after.changedAt distinto de occurredAt; P/R/C conservan sus igualdades y shapes actuales. CLOSE sigue closure4 y no acepta extension ni campos null decorativos.

La marca de última decisión es interna, no comprobable íntegramente desde State6: el cliente verifica lo expuesto (occurredAt no anterior a before.changedAt), y el servidor garantiza la marca adicional. No inventar un campo DTO o una falsa validación cliente de un dato ausente. Revision/token/key permanecen internos; mostrar cantidad, fecha/zona, fin y errores en lenguaje normal.

No se detecta otro bloqueo contractual. Tras incorporar las dos concreciones y su integración con las decisiones locales, procede destilar casos medibles y reutilizar explícitamente14–16; no se pide repetir todas las matrices ni se certifica implementación todavía.


## Verificación del delta normativo

Lectura acotada719e25: montaje explícito en detalle14 y URL16; identidad de active de otra tarea frente a ruta estricta; esperas positivas acotadas a2^31−1ms, fracción mínima1ms y rearme sin GET antes de vencer. También incorpora decisiones hermanas coordinadas, invalidación de snapshots anteriores y recibos separados. Las relaciones aditivas del fin preservan paused y State6 sin exponer la marca interna. Las recomendaciones anteriores quedan resueltas por estas precisiones; se retira CHANGES_REQUESTED. Sólo este informe se actualizó, sin init, fuentes, tests ni edición normativa.
