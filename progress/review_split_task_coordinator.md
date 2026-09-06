# Revisión del coordinador — split_task

Estado: backend aprobado para mutación tras congelación, revisión independiente y pruebas normales del alcance. Interfaz en revisión; no se autoriza el cierre global. El cliente API está liberado para revisión.

## Cliente API

Revisión favorable de `tasks-api.ts` y `split-task-api.test.ts`, contrastada con `tdd_split_task_api.md`. Las rutas nuevas reutilizan transporte, cookies, señales, DTO8 y validación existentes. La relación exige HTTP 200 y un objeto cerrado; un fallo no se convierte en raíz. Las identidades UUID se comparan sin distinguir mayúsculas, sin alterar el contenido confirmado. Se rechazan autorrelaciones en lectura y creación. Las llamadas históricas conservan su firma y colección por defecto.

El autor aporta 100 pruebas verdes en dos archivos, TypeScript y ESLint con salida 0. El coordinador ha leído fuente y pruebas; no atribuye esa ejecución a una ejecución propia. La recuperación de sesión y los borradores todavía requieren las pruebas de interfaz. El autor de este módulo no es su juez independiente.

## Backend leído durante desarrollo

La transacción compartida bloquea primero el proyecto del propietario y comprueba después el padre dentro del proyecto. Inserta tarea con relación y un solo evento, comprobando una fila por escritura. La migración usa clave foránea compuesta y prohíbe una autorrelación. La consulta del padre distingue tarea inexistente de raíz mediante LEFT JOIN. Las listas de hijos conservan el contexto del padre y excluyen nietos. El cursor añade el padre a su contexto cerrado; la colección plana mantiene el contrato anterior.

El evento de subtarea comparte únicamente los campos reales comunes con TaskCreated. Su validación compara identidades mediante UUID.equals, también cuando cambia la caja. Pendientes la trazabilidad completa, los resultados normales y la revisión final del corte formateado. No se extrapola esta lectura estática a carreras, rollback ni transporte real.

Actualización del corte congelado: leídos el informe final `judge_split_task_backend.md` y la trazabilidad del autor; no hay hallazgos abiertos. El coordinador inspeccionó los XML de la sesión normal 29329: 370 pruebas, cero fallos, errores y omisiones. El alcance incluye regresiones afectadas y 105 pruebas del nuevo adaptador HTTP. Queda superada la puerta previa de backend y se autoriza PIT con el perfil split_task. La verificación completa del monorepo, integración y mutación frontend permanecen pendientes; no se atribuyen los 370 casos a toda la suite backend.

## Interfaz pendiente

Se ha pedido al autor verificar que recargar el contexto del proyecto ante un conflicto no desmonte el editor ni pierda el borrador de subtarea. También debe comprobar el foco del enlace de salto al contenido y medir enlaces con títulos muy cortos: el área de la tarjeta no demuestra el tamaño interactivo del enlace.

La revisión posterior de TaskReader confirma que la recarga del proyecto conserva el componente editor. Las pruebas nuevas cubren 412 seguido de recarga satisfactoria, recarga fallida con reintento y 409 seguido de reapertura con el ETag confirmado. El autor comunica 24 casos focalizados verdes; la suite completa y la comprobación en navegador siguen pendientes. La ruta de tarea también se ha incorporado al conjunto permitido por la recuperación de sesión, para conservar la URL después del acceso.

Faltan congelación conjunta, pruebas completas, integración, matriz UX y mutación posterior a la puerta de revisión.

Actualización de interfaz congelada: el autor comunica 455/455 pruebas en 14 archivos, lint y build verdes. La revisión de producción, pruebas de recuperación y alcance Stryker es favorable. El enlace al padre y los títulos de lista usan mínimo 44 × 44 en SCSS; la medida en navegador queda pendiente. Se corrigió además la composición con UUID de URL en mayúsculas: TaskReader usa las identidades canónicas del Task confirmado para consultar proyecto y pasar contexto a relación/hijos. El RED/GREEN del recorrido 409 con reapertura cubre este borde sin modificar readProjects histórico. Integración debe construir desde este último corte.

El perfil Stryker cubre completos los archivos de API, TaskReader, TaskParent y ProjectTasks, y las líneas modificadas de hook, selección de ruta y ruta privada de sesión. El perfil global conserva su lógica histórica y añade los nuevos componentes. La ejecución queda coordinada con la carga de PIT e integración; no se registra todavía puntuación ni cierre global.

## Puerta conjunta previa a mutación frontend

El corte definitivo también corrige la recuperación de un 404: reintentar retira task/snapshot antiguos hasta confirmar la nueva lectura. La recuperación independiente de 412/503 conserva el borrador. La relación anuncia su resultado y recupera foco local sólo cuando el usuario no ha elegido otro destino; la recarga del proyecto aplica la misma precaución. El coordinador leyó ambos componentes finales.

Ejecución independiente del coordinador: `node .harness/harness.mjs init`, sesión 9396, salida 0. XML backend: **622 pruebas, cero fallos, errores y omisiones**. Frontend: **462 pruebas en 14 archivos**, todas verdes. Lint global verde. No es una suma de ejecuciones focalizadas.

El coordinador leyó el XML PIT completo: 235 KILLED y un SURVIVED, 236 en total. Contrastó TaskController.string con normalización de Task: el retorno null/cadena vacía es equivalente en sus dos usos actuales. Aceptado el resultado **235/236 (99,58 %)** sin replay ni cambios posteriores. No se atribuye a toda la lógica del monorepo.

Queda autorizada la ejecución del perfil Stryker split_task. Integración y revisión de sus resultados siguen pendientes; esta puerta no permite marcar la feature done.

## Limitación de limpieza heredada

La revisión automática rechazó eliminar `.e2e-work/read-review-state.json` y `.e2e-work/read-review-stop` con «blocked by policy». Ambos siguen ignorados por Git. No se expone su contenido ni se elude el bloqueo.

Cierre aprobado en judge_split_task.md. Original Stryker 558/601 y replay separado 56/58 inspeccionados: 24 objetivos originales Killed; 12 equivalencias y 7 variantes permitidas restantes justificadas. Suite frontend final independiente 475/475, 14 archivos, 6,15 s; producción sin cambios desde init e integración.
