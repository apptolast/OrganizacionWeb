# create_task — integración

Contrato de 35 escenarios y sección 7 de project-spec.md revisados. Ponytail full y Caveman lite activos. Este agente mantiene e2e, herramientas de integración/publicador y evidencia UX; no implementa producción backend/frontend.

Preparados dos recorridos iniciales: tarea confirmada desde formulario, lectura por Location, recarga, identidad de evento, proyecto y ETag intactos, cierre/reapertura; y 21 tareas con empates, paginación y creación posterior sin duplicados. Labels acordados con frontend: región Tareas, Título de la tarea, Criterio de finalización, Estimación en minutos, Crear tarea, lista Tareas guardadas, Más tareas antiguas, Volver a tareas recientes y Reintentar tareas.

El backend confirmó tabla tasks con referencia al proyecto y sin propietario redundante. Los reseteos de fixtures enumeran tasks junto a projects/outbox_events; no se utiliza borrado de recursos fuera del proyecto de prueba. El smoke conserva las tres rutas previas y añade una tarea con broker detenido, pendiente/reintento y recepción real de TaskCreated en su cola quorum; aggregateId conserva proyecto y taskId identifica la entidad hija.

`pnpm exec playwright test --list` descubre 29 casos: 27 anteriores y dos preparados. No se ha levantado stack ni ejecutado comportamiento del corte 7; se espera el primer build de los autores. No se afirma RED sobre una prueba no ejecutada. Restan privacidad/errores, matriz de 22 anchos, zoom real, ejecución y revisión independiente del backend congelado.
Preparación ampliada a cuatro casos nuevos (31 en total): privacidad con cuerpos 404 idénticos, recuperación de lista independiente, tres errores de creación con borrador conservado, revocación real de sesión, 22 anchos y teclado/doble envío. El smoke selecciona TaskCreated por proyecto y taskId validado, sin suponer un solo evento del tipo por agregado. Aún no se ejecuta la aplicación de este corte.

## Primer corte ejecutado

Ambos autores confirmaron un corte compilable de POST/lista/detalle y formulario. Se levantó el fixture aislado `organizationweb-e2e-59400` con PostgreSQL real, migración V7 y sesión de formulario. `node .e2e-work/task-check.mjs --grep confirmed.task` pasó: **1/1 en 5,2 segundos**.

La prueba creó desde la interfaz una tarea con Unicode y markup literal, criterio conservado y estimación de 25 minutos. Verificó ocho campos exactos, Location consultable, fila visible, texto que distingue estimación de tiempo trabajado, recarga sin POST repetido, evento TaskCreated de ocho campos sin criterio/estimación, y proyecto/ETag intactos. No se probó aún publicación al broker: ese adaptador sigue en construcción.

Se separó cierre/reapertura en su propio caso para no exigir al primer smoke una interfaz que el autor aún estaba implementando. El conjunto preparado contiene cinco pruebas nuevas y 27 históricas, 32 en total. Paginación, completed, errores, matriz y publicación siguen pendientes de su corte estable. Este GREEN de integración sobre código probado por los autores no se presenta como un ciclo RED de producción.

## Coordinación de cortes intermedios

Dos intentos de reconstrucción coincidieron con TDD aún activo: el primero encontró imports incompletos en TaskController y el segundo TS2769 en create-task.test.tsx. Ambos abortaron antes de reemplazar el stack; no representan fallos funcionales de la aplicación ni una suite ejecutada. Los autores corrigieron compilación, y el coordinador pidió esperar freeze conjunto para la siguiente reconstrucción.

La revisión previa de backend quedó en `progress/judge_create_task_backend.md`, favorable en diseño/cobertura con pendientes de cierre. El hallazgo projectId/cursor se reprodujo y corrigió en backend. Se añadió a la matriz de teclado el exponente incompleto «1e», verificando badInput y cero POST, a petición de frontend/coordinador; aún pendiente de ejecutar sobre el corte final.

## Corte congelado

El coordinador confirmó freeze de ambos autores. La reconstrucción final de backend y web completó correctamente. La primera pasada conjunta dio 30/32: dos selectores de `role=status` eran ambiguos porque coexistían la confirmación «Tarea guardada» y la recarga «Cargando tareas». No era un fallo de producción. Se acotaron las aserciones al status de confirmación y se inició otra pasada sobre las mismas imágenes, sin reconstrucción adicional.

La matriz de 22 anchos, axe, controles de 44 CSS y teclado llegó verde antes de ese selector final. El exponente incompleto «1e» produjo `validity.badInput`, recuperó el foco y no envió ningún POST; la estimación corregida permitió un único envío. El smoke añade comprobación del TaskCreated original después del reinicio de Rabbit ya previsto, con el backend detenido, sin añadir otro reinicio.

La segunda pasada conjunta terminó **32/32 en 2,4 minutos**, sobre las mismas imágenes congeladas. El recorrido de tareas en Firefox y WebKit pasó **2/2 en 10 segundos**. Zoom nativo 200 % y ancho interior 320 completaron guardado real, con capturas y medidas en outputs; la matriz de 30 principios queda en `progress/ux_create_task.md`.

Por petición del coordinador, el techo `timeout-minutes` de CI pasa de 30 a 60. La última validación de feature 6 duró 25 min 39 s y el corte 7 amplía mutación. Es un techo de ejecución, no una espera añadida; todos los pasos y umbrales permanecen iguales.

El smoke `pnpm test:publisher` terminó con salida 0. Con worker habilitado y Rabbit detenido, POST tarea devolvió 201, persistió evento pendiente y registró reintento. Al recuperar Rabbit recibió el JSON original de ocho campos, con aggregateId de proyecto y taskId separado, en la nueva cola quorum. Tras el mismo reinicio de Rabbit previsto para la regresión, con backend detenido, el mensaje conservó eventId/payload y la fila de outbox publicada permaneció idéntica. Las tres rutas históricas también pasaron. El runner eliminó sólo sus contenedores, volúmenes y directorio temporal propios.

El coordinador pidió medir feedback, además de comprobar su presencia. Se añadió submit + MutationObserver al caso de POST retenido. Ejecución adicional sobre la misma imagen: **1/1 en 15,5 segundos**, con **2 ms hasta Guardando tarea**, antes de liberar la respuesta. No se atribuye esa medición al resultado anterior de 32/32 ni se repite toda la suite.

## Regresión de reintento durante creación pendiente

La revisión posterior encontró una carrera real en el contador de recarga: un reintento GET que terminaba durante un POST retenido podía consumir la revisión que el cierre antiguo de submit pretendía establecer. El autor reprodujo RED, corrigió únicamente la actualización funcional `setRevision(value => value + 1)` y liberó lint, 366 pruebas y build verdes. El coordinador revisó ese cambio antes del nuevo build de integración.

Se añadió `retry completed during pending save still refreshes confirmed tasks` a la suite de CI. El caso fuerza sólo el primer GET a 503 y retiene el POST antes de enviarlo al servidor; el reintento GET real confirma colección vacía mientras PostgreSQL sigue sin tareas. Después libera el POST real, exige el tercer GET, una fila visible en la lista, ausencia de «Cargando tareas», un único POST y una única tarea persistida.

Comando: `pnpm test:e2e -- e2e/create-task.spec.mjs --grep "retry completed during pending save"`. Resultado sobre imagen reconstruida y liberada: **1/1 en 4,0 segundos**, salida 0. El proyecto aislado organizationweb-e2e-62592, sus volúmenes y directorio temporal se limpiaron mediante el runner estándar. No se tocaron los dos archivos bloqueados de otro fixture. No se repitió matriz visual ni zoom porque el cambio no modifica layout. La suite contiene ahora 33 casos, pero no se atribuye un resultado conjunto 33/33: se conservan 32/32 del corte previo y esta regresión focal del corte corregido como evidencias distintas.
