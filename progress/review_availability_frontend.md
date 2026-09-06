# Revisión frontend del borrador de disponibilidad

Revisión documental de `progress/contract_availability_draft.feature` y `progress/review_availability_proposal.md`, contrastada con los patrones actuales de edición, navegación y sesión. Ponytail full y Caveman lite. **No aprueba el contrato ni activa feature 10; no se modifica producción.**

El corte de zona y siete presupuestos es viable con controles nativos y el cliente de sesión existente. Las siguientes precisiones evitan implementar dos comportamientos distintos bajo el mismo texto.

## Precisiones necesarias antes del contrato

| Punto | Ambigüedad observada | Opción mínima propuesta |
| --- | --- | --- |
| Recuperación de conflicto | La propuesta ofrece comparar, «Usar versión guardada» y «Guardar mi borrador» con nueva revisión. El borrador s33, en cambio, sustituye formulario y ETag al confirmar un GET activado explícitamente. | Adoptar s33: botón «Recargar versión guardada» con explicación visible de descarte; sólo GET válido reemplaza todo. Eliminar de la propuesta la comparación y sobrescritura con borrador retenido. No se necesita fusión ni una segunda pantalla. |
| Ruta y Cancelar | No se fija ruta UI y s35 dice «vista de origen» incluso para URL directa, pestaña nueva o acceso recuperado. | Fijar `/disponibilidad`, sin query ni sufijos, y un destino determinista. La opción más pequeña es «Cancelar y volver a Proyectos», explicando que descarta cambios. Si se exige volver al origen variable, definir qué rutas internas se conservan y el fallback; no usar `history.back()` sin garantías. |
| Alcance del descarte | La propuesta exige que cualquier descarte sea explícito; s35 sólo regula Cancelar. Sidebar, Atrás, recarga y cierre de pestaña también pueden perder el borrador. | Decidir expresamente si la garantía cubre todas las salidas o sólo las acciones del formulario. Para todas, hace falta tratar navegación interna y `beforeunload`, con límites reales del diálogo nativo; no afirmar esa cobertura reutilizando sin cambios RouteLink. Logout y 401 deben retirar datos de inmediato, sin un aviso que impida la revocación. |
| Presupuesto inválido y total | s40 exige una suma del borrador, pero no define el resumen mientras un día está vacío, incompleto o fuera de rango. | Mostrar total sólo cuando los siete valores sean enteros válidos; en otro caso «Completa los siete presupuestos para calcular el total». No sumar parcialmente ni convertir vacío/NaN en cero. |
| Catálogo y zona guardada ausente | s30 conserva una zona que ya no está en el catálogo; una validación de respuesta que exija pertenencia para GET podría rechazar esa misma preferencia. | Validar la forma del snapshot por separado del catálogo. Mostrar el ID guardado como no disponible, conservándolo en el borrador; bloquear PUT hasta elegir un ID actualmente admitido. No normalizar alias ni sustituir por UTC. |

## Estados y reglas de formulario

Mantener tres cosas separadas en memoria: snapshot confirmado con ETag, borrador textual y catálogo confirmado. Los siete campos numéricos deben conservar texto mientras se edita; convertir a números sólo después de validar. Reutilizar el patrón probado de `validity.badInput`, `Number.isInteger` y rango 0–1440. Un campo vacío es obligatorio, no descanso; `1e` con `value=""` y `badInput=true` sigue siendo inválido. Un decimal no entero no se redondea. `min=0`, `max=1440`, `step=1` orientan al navegador, pero no reemplazan el error accesible propio. Si el contrato quiere prohibir también notaciones válidas que representan un entero, como `1e2`, debe decirlo; s38 sólo prohíbe interpretar una entrada incompleta como cero.

Ausencia confirmada sigue siendo «Sin configurar» aunque exista una sugerencia y siete ceros. Esos valores son borrador inicial, no un snapshot guardado. El primer PUT válido debe poder configurarlos aunque no se haya modificado ningún campo: no bloquearlo por comparar el borrador con los ceros iniciales. La sugerencia de zona se calcula como ayuda opcional, en try/catch, y sólo se incorpora si está en el catálogo backend. Un reintento de catálogo no debe sobrescribir una selección o edición ya hecha por la persona.

Para una configuración existente sin cambios, la opción mínima es permitir Guardar y delegar el no-op al servidor. Así se conserva la comprobación de revisión, incluso si otro cliente cambió la fila. No mostrar «Disponibilidad guardada» por una comparación local que no hizo PUT. Un no-op confirmado puede usar el mismo mensaje de confirmación; no debe inventar nueva fecha o revisión. Al editar otra vez se retira el éxito y aparece «Cambios sin guardar».

Durante PUT o recuperación deliberada GET, bloquear otro guardado. El GET de recuperación conserva el borrador visible mientras espera; sólo una respuesta válida sustituye borrador y ETag. Un fallo conserva ambos. Una respuesta de PUT perdida no autoriza reenvío automático. Si se permite seguir editando durante PUT, hay que definir cómo se evita que su respuesta borre nuevas teclas; la opción más pequeña es deshabilitar temporalmente los campos durante el envío.

## Recuperaciones, sesión y respuestas antiguas

Los fallos de catálogo y preferencia deben distinguirse. Se puede mostrar un presupuesto confirmado aunque falle el catálogo, manteniendo el guardado bloqueado y «Reintentar zonas». Ausencia sólo existe tras GET de preferencia válido; ni 503 ni JSON inválido significan sin configurar. Una respuesta de catálogo inválida no se sustituye por `Intl.supportedValuesOf` ni por una lista local.

Completar s36/s37 para incluir **401 de GET zones** y respuestas tardías de esa consulta. Las tres peticiones —preferencia, catálogo y PUT— deben usar `apiRequest` con cancelación, de modo que un 401 antiguo no active el observador global contra una sesión nueva. Deben ignorarse tanto éxito como rechazo tardíos. La secuencia pública StrictMode (A abortado, B confirmado, PUT y llegada de A) ya tiene un patrón de prueba válido; no hacen falta controles adicionales para fabricar carreras.

El nuevo path tiene que entrar en la lista permitida de retorno privado de `use-session.ts`, con coincidencia exacta y sin aceptar query extra. La recuperación deliberada de CSRF conserva borrador, obtiene token y nunca repite PUT; al perder sesión se desmonta la vista y se retiran datos. Añadir la ruta sólo al render de App dejaría incoherente la recuperación tras login.

Los errores `dailyMinutes.MONDAY` deben asociarse al campo Lunes y enfocar el primer campo reconocido; `zoneId` al selector. Errores generales de `body`, `dailyMinutes` o `If-Match` necesitan un aviso general recuperable, sin intentar enfocar un control inexistente. Mensajes de servidor se representan como texto, no HTML. Tras una recuperación que elimina el botón enfocado, restaurar un destino local si el foco quedó en body; conservar el foco elegido durante la espera.

## Reutilización concreta y límites

Usar el cliente CSRF/sesión actual, el shell, SCSS y los patrones de campos, errores y foco. El hook `useEditProject` está ligado a rutas y campos del proyecto: conviene reutilizar su comportamiento probado, no parametrizarlo ahora para convertirlo en un editor universal. Un pequeño hook de disponibilidad y un módulo API bastan; no hace falta librería de formulario, router nuevo, autosave o almacenamiento local.

`RouteLink` actualmente implementa su propio onClick y no ofrece un mecanismo de bloqueo de borrador. Si se decide cubrir todo descarte, esa modificación compartida requiere contrato y pruebas de navegación explícitos; no prometerla mediante un onClick del formulario que el link sobrescribe. Un selector nativo con IDs largos debe conservar texto y ancho del contenedor, y siete campos ordenados lunes–domingo pueden usar la cuadrícula responsive existente. El catálogo backend mantiene el orden contractual; las etiquetas de día visibles pueden ser españolas sin cambiar las claves del JSON.

La matriz de 30 principios, 22 anchos, 44×44 y zoom pertenece a la posterior implementación y revisión real. Esta revisión no afirma compatibilidad universal con dispositivos físicos, ni resuelve ventanas, DST, reservas, tiempo trabajado o conectores.

**Resultado:** propuesta viable, pendiente de resolver la divergencia de conflicto, el alcance de descarte/ruta y las precisiones de catálogo, suma inválida y petición zones en privacidad. No se ejecutaron pruebas ni se aprobaron escenarios nuevos.

## Adición tras revisión del coordinador: confirmaciones incompatibles

El borrador no cubre aún un GET de disponibilidad HTTP 200 cuya forma o ETag sea incoherente: s30 se refiere al catálogo y s32 al PUT. Añadir ejemplos iniciales para ambas variantes del snapshot. Ausencia exige configured false, los tres null y el tag literal de ausencia; configuración exige configured true, zona textual, siete números válidos, fecha UTC válida y ETag configurado fuerte. Casos mínimos: campo ausente/extra, tipo incorrecto, mapa incompleto, fecha inválida, ETag ausente/débil/mal formado y tag de ausencia con cuerpo configurado o viceversa. Ninguno habilita Guardar ni se presenta como ausencia o preferencia guardada; mostrar error y reintento de lectura.

Esta validación de forma sigue separada de la pertenencia al catálogo: una zona guardada que desapareció del runtime sigue siendo un snapshot visible y no disponible, como exige s30. Rechazarla por catálogo al validar GET contradiría ese escenario.

Un PUT HTTP 200 estructuralmente válido tampoco debe confirmar si devuelve otra zona o presupuestos diferentes de la intención enviada. La API cliente debe comparar zona exacta y los siete números con el payload efectivamente enviado, no con un borrador que pudo cambiar durante la espera. Su respuesta debe estar configurada y llevar ETag configurado válido; una ausencia de forma válida no es confirmación del PUT. Conservar el borrador y ofrecer consulta deliberada, igual que las demás respuestas inciertas. Añadir explícitamente estos casos a s32 o a un escenario de validación de confirmación; no requieren asumir la hora del servidor ni calcular versiones en el navegador.

## Resolución documental del coordinador

Las decisiones siguientes concretan el corte propuesto; no son aprobación de implementación y todavía deben reflejarse en el contrato definitivo.

- Ruta UI exacta `/disponibilidad`, sin query ni sufijos. La acción será **Cancelar y volver a Proyectos**, con destino `/proyectos`, también al abrir directamente o volver del acceso.
- El descarte explícito se garantiza en las acciones del formulario: Cancelar y Recargar versión guardada. La pantalla avisa permanentemente **«Los cambios sin guardar se pierden al salir»**. No se añade guardia global de navegación, router, diálogo de cierre ni `beforeunload`; no se promete evitar pérdida al usar Atrás, recargar o cerrar la pestaña. Las revocaciones de sesión siguen retirando datos inmediatamente.
- El conflicto usa la recarga de s33: la acción explica que descarta cambios; GET válido sustituye borrador y ETag juntos. Se elimina de la propuesta el flujo de comparar/fusionar o conservar el borrador sobre una revisión nueva para sobrescribirla.
- Durante PUT y durante esa recuperación GET, campos y Guardar quedan deshabilitados. La respuesta no puede sobrescribir nuevas teclas porque el formulario no admite edición en ese intervalo. Un fallo de recuperación conserva el borrador.
- Después de PUT 412, 503, pérdida de conexión o HTTP 200 inválido, se exige confirmar la recarga antes de otro PUT. La consulta fallida no levanta esa condición. Un HTTP 400 con errores de campo permite corregirlos y enviar de nuevo deliberadamente; CSRF conserva el patrón actual de recuperación sin reenvío automático.
- El total semanal sólo se calcula con los siete números válidos. El vacío o la entrada incompleta no son cero ni un total parcial; siete ceros válidos sí representan descanso completo.
- La privacidad y cancelación incluyen GET zones. El retorno privado tras login admite la ruta exacta nueva. No se reintroduce un borrador por respuestas de una sesión o montaje antiguos.
- El contrato añadirá los GET HTTP 200 incompatibles descritos arriba y el PUT con cuerpo válido pero valores distintos de la intención enviada. Ninguno constituye una confirmación ni permite inventar ausencia, zona, fechas o presupuesto.

Con estas resoluciones desaparece la divergencia de los dos flujos de conflicto y queda acotado el coste de navegación. Permanecen las precisiones de forma/catálogo, controles nativos y evidencia UX de esta revisión. **No se modifica el borrador aprobado de otra feature, no se activan escenarios ni se inicia producción de disponibilidad.**

---

# Revisión de disponibilidad frontend

Estado: revisión parcial, pendiente de freeze y comprobación final. No autoriza todavía Stryker ni cierre.

El coordinador leyó la vista, cambios de navegación/sesión, cliente común y casos UI de guardado, recuperación y cancelación. El cliente específico tiene aprobación independiente en `review_availability_api.md`. Las fuentes UI siguen en TDD; no se presentan sus estados transitorios como producto terminado.

## Hallazgos enviados antes del freeze

1. Recuperar acceso tras un rechazo conocido `403 CSRF_INVALID` debe permitir un segundo envío manual de los mismos valores, usando el token renovado y sin descartar el borrador. El corte observado agrupaba ese rechazo con resultados inciertos y exigía recargar preferencias; la prueba incluso fijaba Guardar deshabilitado después de recuperar acceso. Se pidió corregir esa expectativa y el comportamiento mediante TDD. Se reutiliza la recuperación de sesión existente; no se reenvía automáticamente el PUT. `412`, `503`, red y confirmación inválida mantienen su recarga obligatoria.
2. La navegación incorpora una segunda sección, pero heredaba el fondo activo en todos los enlaces y el punto fijo en Proyectos. Se pidió distinguir la sección actual mediante `aria-current` y un indicador que no dependa sólo del color, dentro del acabado SCSS ya pendiente.
3. La configuración global de Stryker aún necesita incorporar disponibilidad y revisar el rango fijo de App al desplazarse las líneas. El perfil focal y el global deben cubrir los cambios nuevos sin perder los anteriores. Es preparación de la verificación, no una ejecución autorizada.

La revisión final comprobará las correcciones, el código congelado, trazabilidad completa, resultados de pruebas y evidencia de navegador. No se atribuye un resultado global a la lectura parcial.

## Seguimiento de correcciones

La lectura posterior confirma que el rechazo CSRF usa `isCsrfFailure`, conserva las guardas de aborto después del await y permite un segundo envío manual. La prueba exige mismo cuerpo, mismo ETag, token renovado, sólo dos escrituras iniciadas por el usuario y ninguna lectura adicional de preferencias. El autor registra RED/GREEN; el coordinador aún no reejecuta la vista completa, pendiente del freeze. La navegación ya añade borde e indicador a la sección activa y retira el punto de la sección inactiva; queda su comprobación visual en navegador.

## Dictamen sobre el corte congelado

APPROVED para verificación por mutación e integración. El coordinador revisó la vista congelada, cliente, cambios compartidos, pruebas y mapas, además de corregir los hallazgos anteriores. Init global 8318 terminó EXIT 0: lint verde, 984 pruebas backend y 841 frontend en 19 archivos. El total backend se sumó de los XML, sin fallos, errores ni omisiones. La ejecución incluye las 47 pruebas UI y 147 del cliente específico; no se suman de nuevo al total global.

Los perfiles Stryker focal y global incluyen disponibilidad y las líneas finales de App, Workspace y retorno privado. Se conserva el único observador de ruta en App. El formulario separa snapshot, borrador y catálogo; bloquea envíos inciertos hasta recarga válida, conserva cambios frente a fallos y cancela peticiones al desmontarse. La recuperación de CSRF queda diferenciada y el foco se restaura sin desplazar el destino elegido durante la espera.

Este dictamen no cierra la función. La revisión de navegador debe comprobar los nuevos breakpoints 420, 760 y 1000 además de la matriz anterior. Se señaló un riesgo concreto entre 701 y 760: la navegación pasa a fila mientras el sidebar aún conserva su ancho lateral. Integración comprobará texto y posibles solapes, además de cajas, axe y desbordamiento. Si aparece el fallo, deberá corregirse y verificarse antes del cierre. Reinicio, zoom, matriz UX y mutación siguen pendientes de sus resultados reales.

## Hallazgos durante la verificación, pendientes del cierre

- Integración confirmó el solape entre 701 y 760. El coordinador inspeccionó las capturas de 701 y 761: en la primera, Disponibilidad sale del enlace e invade el contenido principal; en la segunda, ambos enlaces son legibles dentro del sidebar. Se autorizó una corrección exclusiva de SCSS tras una comprobación focal RED, sin modificar los archivos TypeScript ni pruebas que Stryker está usando. El bundle visual final tendrá su propia verificación.
- Un error HTTP 400 con `dailyMinutes.MONDAY` y mensaje vacío crea una clave en fieldErrors y evita el aviso general, pero el JSX tampoco muestra el error vacío. Se solicitó tratar mensajes vacíos o de espacios como error general recuperable o sustituirlos por texto local útil. Este cambio de lógica y su test esperan al informe original de Stryker para conservar la evidencia de esa campaña. Debe resolverse antes del cierre, aunque no lo detecte una mutación.

La corrección de navegación queda verificada: SCSS usa ahora 700 como punto de cambio a fila. Integración registra matriz y navegación 2/2 en el bundle final; el coordinador inspeccionó además la captura corregida de 701 píxeles, con ambos enlaces dentro del sidebar y sin invadir el contenido. Las capturas finales de escritorio, móvil y zoom también se revisaron visualmente. El hallazgo del mensaje 400 continúa abierto; esta comprobación visual no lo sustituye.

Corrección 400 revisada y aprobada: la única modificación lógica exige `entry.message.trim().length > 0` antes de reconocer el mensaje, conservando intacto cualquier texto válido. Dos casos RED/GREEN verifican vacío y espacios, alerta útil, borrador intacto y ausencia de reenvío. Integración ejecutó además un recorrido Chromium con ambos casos contra backend real, recuperación deliberada y nuevo guardado válido: 1/1, bundle CIX_-ttO y CSS Codz1mIb. El coordinador leyó ese escenario y su evidencia; la navegación principal permanece en cero al enviar. El comportamiento queda corregido; su replay de mutación y los refuerzos generales aún deben completar el cierre.

Nuevo hallazgo de teclado, antes del cierre: el autor señaló que submitFocus sólo conserva botones. Integración confirmó mediante Enter real desde minutes-MONDAY que una respuesta de éxito y una 503 dejan el foco en BODY; un error 400 de campo sí lo devuelve al input. Evidencia original en `evidence_availability_enter_focus.json`, sesión 96423, imagen CIX_-ttO. El coordinador leyó la evidencia y autorizó TDD y una corrección mínima para restaurar el control de origen sin robar el foco elegido fuera del formulario. Este hallazgo permanece abierto hasta review y verificación de la nueva fuente; no se considera resuelto por las capturas anteriores.

Corrección de Enter aprobada: submitFocus conserva ahora button/input/select, los tres controles nativos del formulario; reutiliza los efectos de restauración y no añade otra abstracción. El coordinador revisó fuente y pruebas. La simulación de foco BODY en jsdom reproduce el comportamiento observado en Chromium, no pretende demostrar por sí sola el comportamiento nativo. Integración verificó después Enter real en éxito, 503, 400 y cambio deliberado de foco por Tab: 2/2 focales, incluida regresión del mensaje 400 vacío, sesión 5043 y bundle CpU8JHCd. CSS Codz1mIb intacto. Los hallazgos de producción están resueltos; quedan revisión de refuerzos y replay de mutación para la aprobación final.
