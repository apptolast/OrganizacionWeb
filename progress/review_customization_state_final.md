# Revisión independiente del estado compartido21

Dictamen provisional: **CHANGES_REQUESTED**, alcance estado/cache/recuperación. Snapshot leído `frontend/src/customization-state.ts`, SHA256 96CB7F2D220A831147C80EFB7D1329401046230147461C81A722BBE92AC54D90. La autora libera después ese archivo únicamente para resolver el hallazgo; este hash describe el corte anterior, no su corrección futura.

Se leen también CustomizationControls, CustomFieldsPanel, App/SessionGate y los oráculos de configuración/valores. Las pruebas y formularios siguen evolucionando bajo propiedad de B; no se atribuye aprobación de UI final, SCSS, UX ni globales. Revisión de código/oráculos, sin ejecutar suites ni modificar producto/tests.

## P2: aceptación tardía de valores con un esquema ya superado

En `customization-state.ts:79` acceptConfig invalida consultas y cache, pero no retira la escritura pendiente. En `:326` saveValues acepta el ACK después del await sin contrastar el esquema/generación vigente. Secuencia válida: PUT de valores con schema0 confirma en servidor y su respuesta se retrasa; otra petición renombra y confirma schema1; llega el ACK anterior con schema0. El estado aplica ese snapshot y retorna true, por lo que el panel puede borrar el borrador y anunciar guardado aunque ya conoce otro esquema. No requiere violar el lock del backend: el desorden está en la entrega de respuestas.

Oráculo acotado comunicado a B/root: diferir ACK del PUT, confirmar cambio de esquema, liberar ACK antiguo; no restaurar representación anterior, no borrar borrador ni anunciar confirmación obsoleta. Root ratifica el hallazgo y autoriza a B el RED y la corrección.

La misma guardia de aceptación debe contemplar la primera lectura: TaskReader monta valores TASK y controles TASK en sus subtareas. Si el GET de valores obtiene schema0 y su respuesta se demora, el primer GET de configuración puede confirmar schema1 cuando valueSchemas aún no contiene esa entidad. El filtro de acceptConfig no la incluye y loadValues (`:272`) acepta después schema0 sin comparar acceptedConfigs. El oráculo147 existente resuelve primero valores y luego configuración; no acredita este orden inverso. Se comunica como variante de la misma frontera, sin pedir una matriz nueva.

## Evidencia favorable y límites

- La incertidumbre y borradores viven en la sesión de App y sobreviven navegación interna; App se retira al cambiar identidad. Los oráculos de desmontaje comprueban aborto antes del observador401.
- Configuración leída y confirmada usa acceptConfig compartido; los casos145/147 cubren recuperación y configuración inicial posterior a valores ya conocidos.
- Los casos dirty/uncertain conservan el borrador y exigen recuperación manual; las consultas limpias se invalidan por generación.
- La aceptación de una escritura retira consultas anteriores; los casos131/132 comprueban que no restauran la confirmación posterior.
- La recuperación de valores descarta el borrador sólo tras lectura válida; la de vista incorpora la misma condición en el delta150.

Pendiente ratificar únicamente el delta de la guardia compartida al recibir el freeze de B. No hay dictamen de feature completa y no se modifica la campaña PIT backend que continúa sobre Java congelado.

## Ratificación del delta154–155

**APPROVED parcial**, limitado al estado compartido revisado. Hash final comprobado:55D3DFDB0382F4FB66FF9C68DFC4BF033BDAECC4940959818CBE2C73389C622D. Se preserva arriba el dictamen del corte anterior.

acceptConfig aborta una escritura afectada, retira su estado de espera y conserva incertidumbre/borrador. Además, rejectOldValues compara la identidad y versión del esquema conocido antes de aceptar tanto GET como PUT; admite versiones posteriores del mismo esquema, evitando rechazar una respuesta más nueva sólo por desigualdad del ETag. La guardia ocurre antes de borrar stale/uncertain o sustituir el snapshot.

Los nuevos oráculos públicos del final de customization.test.tsx reproducen ambos órdenes señalados: ACK retrasado después del rename (RED a685a6, GREEN623451) y GET inicial viejo recibido después de la primera configuración nueva (RED37f366, GREEN542d50). Comprueban no publicar datos anteriores, conservar borrador/bloqueo y recuperación manual, y no añadir GET automáticos. Evidencia de ejecución/formato/lint pertenece a B (143b2d); esta revisión no repite suites. No queda abierto el hallazgo descrito en el alcance revisado. Archivo de estado liberado para la autora; formularios finales, CSS/UX y gates globales quedan fuera de este dictamen.

## Ratificación sobre freeze funcional final de B

El manifiesto customization_frontend_final_freeze.json (ciclos1–162,151/151 acreditados por B) conserva exactamente customization-state.ts SHA55D3DFDB0382F4FB66FF9C68DFC4BF033BDAECC4940959818CBE2C73389C622D; comprobación directa23c4e5. No existe delta del estado respecto de la ratificación154–155. Se mantiene APPROVED parcial de ese archivo y sus guardias, sin extenderlo a formularios/rutas nuevos, CSS, UX, mutación ni regresión global.