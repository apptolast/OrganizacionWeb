# Revisión — create_task

**Dictamen final: APPROVED.** El coordinador revisó el contrato de 35 escenarios, fuentes backend/frontend, pruebas, integración y ambas campañas de mutación. La revisión backend independiente consta en judge_create_task_backend.md. Las secciones siguientes conservan la secuencia de puertas y hallazgos; el cierre final al pie sustituye sus estados pendientes históricos.

## Verificación independiente

Init 74002 terminó con salida 0: entorno, estructura, estados, lint, 484 pruebas backend y 331 frontend. Los XML backend contienen cero fallos, errores y omisiones. La primera regresión completa 84796 detectó tres fallos en ProjectStateConfigurationTest por los nuevos puertos ausentes en su contexto aislado; el autor ajustó únicamente el fixture y conservó las comprobaciones de capacidad. La ejecución conjunta posterior confirma la corrección.

## Diseño y hallazgos resueltos

La entidad hija conserva referencia al proyecto; la transacción bloquea su fila propia, comprueba estado y confirma una fila de tarea y una de outbox antes de devolver éxito. No modifica estado, fechas, capacidad o ETag del proyecto. La consulta limita propietario y proyecto mediante JOIN; su cursor cerrado incorpora proyecto, fecha e id y mantiene orden descendente estable. TaskCreated conserva el agregado proyecto y añade taskId, sin publicar criterio o estimación.

La interfaz mantiene carga y error de tareas separados del detalle del proyecto. Conserva el borrador ante errores, cancela solicitudes al desmontar, usa el cliente de sesión existente y no repite escrituras automáticamente. Una creación confirmada se conserva aunque falle la lectura posterior; vuelve a la página reciente. La revisión deliberada de un conflicto actualiza proyecto y ETag juntos. Controles nativos, SCSS y una extracción local del estado evitan dependencias o abstracciones genéricas nuevas.

Se corrigieron antes de esta puerta: invariantes de identidad/estado/fechas de Task; clasificación de projectId inválido antes de cursor; confirmación de tarea tras GET fallido; recuperación de error al cambiar de página; entrada numérica badInput distinta de vacío voluntario; y devolución de foco al desaparecer paginación sin desplazar un campo elegido. Las pruebas reproducen los casos y conservan regresión de autenticación mediante composición real con SessionGate.

## Trazabilidad revisada

- s1–s7: TaskTest, CreateTaskTest y TaskApiTest; límites, Unicode, normalización, opcionales, confirmación y valores persistidos. tasks-api.test valida respuestas; create-task.test cubre Unicode y estimación nativa.
- s8–s12: TaskApiTest, cuerpos estrictos, sesión/CSRF/origen, recursos indistinguibles e identificadores. El mapa backend identifica cada método.
- s13–s15: conservación del proyecto/ETag, ambas carreras de cierre y triggers reales que fallan o suprimen cada INSERT; sin éxito parcial.
- s16–s18: TaskEventTest y RabbitBrokerPublisherTest, esquema y ruta reales. El smoke preparado comprueba broker detenido, identidad y recuperación; falta resultado final.
- s19–s25: ReadTasksTest y TaskApiTest, colección vacía, 20/21 elementos, continuación con inserción nueva, cursor inválido, detalle y no-store/503.
- s26–s30: create-task.test, pruebas «guarda una tarea», «mantiene tarea confirmada aunque refrescar ... falle», «conserva el borrador exacto», «bloquea doble envío», carga/recuperación independientes y «un proyecto terminado conserva tareas».
- s31–s32: create-task.test, «aborta una lectura antigua», «cancela POST al salir», «un 401 vigente retira lista y borrador» y recuperación CSRF sin reenvío.
- s33: errores de campo asociados, badInput, foco al paginar y conservación del foco elegido. El recorrido de teclado real está preparado en E2E.
- s34: reglas SCSS revisadas; prueba de 22 anchos y controles/axe preparada. Matriz completa, zoom real y revisión visual pendientes; no se atribuyen a tests unitarios.
- s35: paginación opaca en cliente y recarga real en el primer smoke; suite final de 32 E2E pendiente.

## Puerta siguiente

Se autoriza PIT focal create_task y Stryker de tasks-api.ts, task-validation.ts, use-project-tasks.ts y project-tasks.tsx completos, con umbral 80. Se mantienen los alcances predeterminados para CI y se documentarán supervivientes, cobertura real y cualquier repetición por separado. No se atribuye a este corte únicamente la puntuación histórica.

C1–C3 y verificación unitaria de C4: conformes. C4 de navegador/UX, C5 de cierre, verificación final de C6 y C7 continúan pendientes. No hay autorización para marcar done hasta completar esas pruebas y la revisión de sus resultados. Ponytail full y Caveman lite se aplican sin reducir arquitectura, seguridad ni accesibilidad.

## Integración y UX posteriores

Sobre imágenes congeladas: 32/32 E2E en 2,4 minutos. La primera pasada 30/32 sólo reveló dos selectores ambiguos de status; se precisaron sin alterar producción. El caso de feedback se reforzó después y pasó por separado (1/1, 15,5 segundos), midiendo 2 ms hasta Guardando tarea mediante submit y MutationObserver antes de liberar POST. No se mezclan esos resultados en un denominador nuevo.

Firefox/WebKit completaron el recorrido de tareas (2/2, 10 segundos). El smoke de publicador terminó con salida 0: creación durante caída, evento pendiente, recuperación con identidad conservada y retención del mismo TaskCreated tras reiniciar Rabbit con backend detenido. Las tres rutas anteriores permanecen verificadas.

El coordinador inspeccionó las capturas de escritorio, móvil y zoom, junto al JSON de medidas. No observó recortes; el zoom nativo de Chromium es 200 %, innerWidth 320 y scrollWidth/clientWidth 312. Controles de 240 CSS de ancho y 52/112/52/45 de alto. La matriz de 22 anchos, errores, teclado y axe pasó; ux_create_task.md revisa las treinta filas con sus límites. No se declaran dispositivos físicos, teclado virtual, lector de pantalla real ni una certificación universal de usabilidad o WCAG.

C4 de integración y la evidencia funcional de C6 quedan revisados. Persisten C7, revisión de supervivientes y cierre de lifecycle antes de done.

## Limpieza acotada

Integración retiró sus contenedores, volúmenes y navegador de prueba. La revisión automática rechazó eliminar `.e2e-work/read-review-state.json` y `.e2e-work/read-review-stop` con el motivo literal «blocked by policy». Permanecen ignorados por Git; no se imprimió el contenido del archivo de estado ni se intentó otra vía para eludir el bloqueo. Esta limitación de limpieza se conserva explícita y no representa un bloqueo de la aplicación.

## Hallazgo posterior durante revisión de mutantes

La primera campaña frontend produjo 402/504 (79,76 %), por debajo del umbral. La revisión de sus supervivientes detectó una carrera observable: tras fallar GET de tareas, un POST retenido puede coincidir con un reintento de lectura. Si el reintento incrementa la revisión antes de confirmar POST, la actualización basada en el valor capturado puede reutilizar el contador y dejar la lista en carga sin una nueva consulta. El autor reprodujo el caso en rojo.

Se autoriza exclusivamente sustituir esa actualización por una función del estado vigente. La puerta de mutación del archivo afectado se reabrirá después de comprobar prueba verde, lint, build y revisión del cambio. Integración verificará el recorrido con una imagen nueva; la evidencia visual previa conserva su alcance porque la corrección no cambia estructura ni estilos. El cierre global sigue pendiente.

## Mutación backend revisada

Perfil completo: 182/186 (97,85 %), sin timeouts ni falta de cobertura. El coordinador comprobó el XML del replay separado: 15/15 eliminados. Las identidades de los tres supervivientes no equivalentes originales aparecen eliminadas por las pruebas nuevas de cursor con microsegundos válidos y destino Rabbit configurado (host y virtual host). El cuarto superviviente sustituye null por cadena vacía en el helper de lectura: ambos valores producen el mismo rechazo de título o la misma normalización del criterio. La equivalencia se limita al contrato actual.

No se suman el perfil completo y el replay ni se presenta una puntuación global inventada. La puerta de mutación backend queda aprobada; el cierre mantiene pendientes la regresión normal posterior y el frontend.

## Nueva puerta previa tras corregir la carrera

El coordinador revisó `use-project-tasks.ts:81`: la actualización funcional incrementa el valor vigente y evita reutilizar el contador capturado antes del reintento. La prueba conserva POST retenido, un GET fallido y otro correcto antes de confirmar; exige la consulta posterior y la tarea visible. La suite real de navegador verifica además un único POST y una única fila persistida (1/1, 4 segundos, imagen reconstruida).

Init 73511 terminó con salida 0: lint, 486 pruebas backend sin fallos/errores/omisiones y 366 pruebas frontend. Build frontend verde tras la corrección. Se vuelve a aprobar el diseño y la cobertura previos a mutación. Queda autorizado el análisis incremental de los cuatro archivos completos, conservando el informe original y documentando qué resultados reutiliza; cualquier cambio de identidad o denominador debe quedar explícito. El cierre global sigue esperando esa puerta.

## Segunda campaña frontend

La segunda invocación, aunque configurada como incremental, reutilizó cero resultados y ejecutó los 505 mutantes actuales de los cuatro archivos. Terminó con salida 0: 480 eliminados (95,05 %), 25 supervivientes y ningún timeout, error o falta de cobertura. El coordinador comprobó el JSON final. El cambio funcional explica el denominador diferente; no se suma con 402/504 ni se atribuye ahorro de ejecución inexistente.

La revisión descubrió también una asociación incorrecta en el primer informe independiente: 304 corresponde al rechazo de HTTP distinto de 200 y 323 a la guarda de tipo objeto. El revisor corrigió explícitamente el informe y revalidó las 21 identidades de su ámbito contra fuente y localización. El caso 323 fue eliminado por la prueba de JSON primitivo; no era equivalente. Siguen bajo comprobación focal la confirmación al paginar, HTTP inesperado y el intercalado de las dos señales de recarga. Las variantes de foco que sólo restauran el encabezado antes, sin desplazar un foco elegido, se evalúan contra el contrato y no se confunden con identidad semántica absoluta.

## Cierre final

El replay frontend terminó con 16/16 eliminados. El coordinador emparejó fuente, localización, mutador y sustitución de las cuatro identidades actuales 110, 304, 428 y 521; todas aparecen eliminadas en el informe separado. Los contadores no eran equivalentes bajo el lote comprobado: confirmación y clic público todavía conectado podían cancelar una señal con signos opuestos. La prueba utiliza DOM y transporte, sin invocar el hook ni modificar estado interno; no se presenta como una nueva reproducción en navegador. No hubo más cambios de producción.

Quedan 21 variantes revisadas: 19 equivalentes dentro de las entradas y consumidores actuales y dos variantes de restauración de foco admitidas por el contrato. Se conservan las guardas defensivas. No quedan huecos reales conocidos abiertos en este alcance. Los resultados publicados permanecen 480/505 global y 16/16 replay por separado, igual que 182/186 y 15/15 en backend.

La suite frontend posterior a los últimos refuerzos termina con 371 pruebas y lint verdes. La regresión conjunta previa permanece documentada con 486/366, y las evidencias de navegador conservan sus cortes exactos (32/32 original, feedback 1/1 y carrera corregida 1/1). Se cumplen las puertas de revisión, prueba, mutación e interfaz aplicables. Se autoriza al autor a marcar create_task como done y cerrar su bitácora; el coordinador publicará el commit. CI remota y despliegue no se atribuyen a esta verificación local. La limitación de limpieza indicada arriba permanece explícita.
