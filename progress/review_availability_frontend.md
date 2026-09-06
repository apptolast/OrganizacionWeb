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
