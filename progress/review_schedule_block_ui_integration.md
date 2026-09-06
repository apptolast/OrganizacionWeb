# Revisión incremental de integración UI — schedule_block

Estado: IN_PROGRESS. Corte de interfaz en construcción; no constituye freeze, aprobación final ni autorización de mutación. Contrato aprobado a84e42f: 62 escenarios y 325 casos. Ponytail full y Caveman lite.

Alcance leído: task-blocks.tsx, task-reader.tsx, task-state.tsx y task-blocks.test.tsx. Se consultaron las fronteras de schedule-block-api.ts, api-client.ts, SessionGate y el montaje con key de App para evitar atribuir defectos ya resueltos por el contexto real. API cliente considerada aprobada por la revisión del coordinador (190/190), no reeditada ni sometida a otro rediseño. No se ejecutaron tests ni se modificaron producción, pruebas, contrato o informe anterior de dominio.

## Hallazgos del corte, comunicados al autor

### [P2] Los errores de fecha, zona y ocurrencia no se asocian todavía a sus controles

Ubicación: frontend/src/task-blocks.tsx, extracción objectiveError y controles block-start/block-end/block-zone/ocurrencias.

La respuesta VALIDATION_ERROR se conserva en issue, pero sólo objective tiene mensaje de campo, aria-invalid y aria-describedby. Un NONEXISTENT_LOCAL_TIME en startLocal, IN_PAST o OUT_OF_RANGE en endLocal muestra únicamente el fallo genérico de revisión, aunque la API ya entrega el campo y mensaje concreto. Los selectores de ocurrencia reciben opciones cerradas, pero tampoco muestran el mensaje asociado al control.

Incumple los errores asociados del recorrido @s59 y oculta la explicación necesaria para corregir la fecha. Generalizar la asociación existente a los campos afectados, con mensajes seguros y foco coherente; no se necesita una capa nueva.

El autor confirma que es su próximo ciclo TDD de errores y foco. Este informe identifica el tramo pendiente, no una regresión de una entrega declarada terminada.

### [P2] Un solape confirmado se clasifica todavía como resultado incierto

Ubicación: frontend/src/task-blocks.tsx, lista de códigos definitivos de save.

BLOCK_OVERLAP no figura aún entre los rechazos definitivos. Al recibirlo, el flujo entra en setUncertain(true), bloquea el borrador y ofrece consultar una creación que el servidor rechazó. @s49 exige conservar los campos, retirar preview/consentimiento y permitir revisar una intención corregida. El conflicto debe permitir consultar el bloque propio indicado en conflict.

BUDGET_EXCEEDED compartía esta omisión en la primera lectura, pero el autor lo corrigió durante la revisión y añadió la explicación de presupuesto y el requisito de revisión nueva. No se mantiene como hallazgo abierto. BLOCK_OVERLAP queda reconocido por el autor para el ciclo siguiente.

### [P2] La lectura de bloques no retira todavía el contexto ante RESOURCE_NOT_FOUND

Ubicación: frontend/src/task-blocks.tsx, catch del efecto readBlocks.

El catch del listado convierte todos los errores en listFailure. Por tanto una respuesta 404 RESOURCE_NOT_FOUND deja visibles tarea, editor y una confirmación anterior, en lugar de retirar el contexto privado. Las rutas preview/create/check sí llaman a classify y trasladan ese código al TaskReader mediante onAccessFailure. Aplicar la misma distinción al listado sin convertir los fallos de almacenamiento en una lista vacía ni borrar una confirmación por un mero 503.

El autor confirma que este ciclo de privacidad del listado está pendiente. Un 401 no es un defecto equivalente: apiRequest avisa al observador global y SessionGate desmonta App. Hace falta demostrarlo con SessionGate real, no exigir un segundo mecanismo local porque una prueba monte App directamente.

## Comportamientos revisados sin hallazgo adicional

- La primera creación reúne key, petición exacta y Availability-Revision. Reenvío y recuperación usan la misma intención. Los campos quedan bloqueados durante guardado e incertidumbre; Cancelar permanece disponible.
- readBlockByRequest recibe el preview retenido para comprobar el DTO. Un resultado de key ausente conserva incertidumbre y habilita sólo el reenvío manual de esa intención.
- CSRF_INVALID conserva el editor; la renovación se realiza mediante SessionGate, y el test existente comprueba cuerpo/key conservados y token renovado antes del reenvío manual.
- Editar objetivo, extremos, zona u ocurrencias invalida preview y consentimiento. Los cambios de extremo retiran su offset; cambiar zona retira ambos. Las respuestas de preview abortadas no reactivan revisión.
- Las comprobaciones posteriores a await readBlockError vuelven a comprobar AbortSignal. App monta TaskReader con key de ruta y el desmontaje aborta revisión/escritura: no se requiere un segundo identificador de navegación.
- TaskState comunica el estado confirmado a TaskReader. TaskBlocks conserva el editor abierto cuando pierde elegibilidad, pero impide creación nueva; la intención retenida permite recuperación. No añade otra consulta silenciosa de tarea.
- La carga de disponibilidad y catálogo usa Promise.all y no publica catálogo parcial. El reintento sólo existe después del fallo; no hay camino normal con una zona alternativa ya seleccionable o una revisión válida que esa carga tardía pueda sobrescribir. Se retiró expresamente una sospecha inicial de carrera para no añadir guardas sobre un flujo inexistente.
- No se analizaron como defectos el SCSS incompleto ni el foco aún en autoría. Las verificaciones responsive, de motores y de UX siguen siendo puertas posteriores; este informe no las acredita.

## Evidencia pendiente para la revisión formal

Añadir las pruebas que el autor está desarrollando para los tres hallazgos. Confirmar pérdida de sesión mediante SessionGate y conservar el editor al completar el proyecto mientras existe un resultado incierto. La prueba del estado inicial de proyecto completed no sustituye esa transición durante la recuperación. Mantener también el caso de respuesta obsoleta tras cerrar y volver a abrir, y el de fallo de listado posterior a creación confirmada, ya presentes en el corte leído.

## Revisión posterior del coordinador — 6 de septiembre

Los tres hallazgos P2 anteriores quedan resueltos en el corte actual: los campos de fecha, zona y ocurrencia muestran errores asociados; BLOCK_OVERLAP invalida la revisión como rechazo definitivo y permite consultar el conflicto propio; el listado distingue RESOURCE_NOT_FOUND y retira el contexto después de comprobar la vigencia de la respuesta.

Verificados también los casos de pérdida de sesión con SessionGate, transición de proyecto a completed durante recuperación y restauración de foco tras confirmación mediante creación o consulta. Ejecución independiente: `pnpm exec vitest run src/task-blocks.test.tsx`, 52/52, EXIT 0, salida 3b2608, 09:47:27. Esta ejecución corresponde al archivo focal, no a toda la suite frontend.

El estado global continúa IN_PROGRESS: quedan integración con backend real, comprobaciones visuales y de accesibilidad, regresión completa y mutación tras revisión. No se acredita cumplimiento de los treinta principios mediante pruebas de componentes.

## Corte visual posterior

El autor entrega 53 pruebas focales y una regresión completa anterior de 1117 casos sin fallos, más TypeScript, lint y formato. La prueba adicional de conflicto de idempotencia cubre comportamiento existente, sin cambiar producción. El coordinador inspeccionó las capturas de revisión a 1440 y 320 px y recuperación con zoom nativo, además de mediciones de zoom y Firefox. `ux_schedule_block_frontend.md` conserva las treinta filas y el alcance de los datos simulados.

La evidencia ampliada del autor cubre Chromium, Firefox y WebKit, estados de espera/error/recuperación, altura reducida, texto ampliado, feedback medido y zoom nativo Chromium al 200 %. Los cocientes de ancho interior y DPR registrados acreditan zoom de navegador; no se confunde con reducir sólo el viewport. Esta evidencia de interfaz no acredita persistencia ni dispositivos físicos.

Se comprobó una sospecha de pérdida de foco al abrir/cancelar: aunque activeElement pasa a body al desaparecer el botón, Tab continúa al objetivo o a Planificar bloque en los tres motores. El recorrido secuencial sigue siendo útil; no se exige otro mecanismo de autoenfoque ni se mantiene como defecto. La comparación con el cliente HTTP está en `review_schedule_block_http_client.md`; handlers todavía en autoría no se presentan como fallos de una entrega final.

No se detectan nuevos defectos de interfaz en este corte. La aprobación final continúa pendiente del E2E con backend real y del arnés completo.
