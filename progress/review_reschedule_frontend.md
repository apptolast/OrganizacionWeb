# Judge del frontend13 congelado — 2026-09-06

**CHANGES_REQUESTED.** Revisión independiente de `reschedule-block.tsx`, sus tests y composición `TaskBlocks`, con lectura de los cruces reales con ChangeSubmit/BlockConfirmation/API. Contrato41/156 de `reschedule.feature`, sección13 y requisitos UX. No se ejecutaron suites ni se editaron fuentes/tests. Los dictámenes parciales de API, historial y recibos conservan su alcance; no acreditan automáticamente esta composición.

## Revisión posterior de las correcciones

Root, sin autoría de fuentes/tests, revisó la implementación corregida y sus oráculos en29f836/8bbfd3/1c16ce. R1–R6 resueltos en el alcance de composición: clasificación protegida por aborto en recuperación, consulta deliberada tras preview412, foco lógico durante carga sin robar el foco externo, explicación del cierre sin prometer rollback, retirada de errores de campos corregidos y anuncio de preview pendiente. Evidencia del autor:129/129 GREENa67b37, lint/tipos verdes. El último refuerzo del mismo estado pendiente añade anuncio de la consulta actual (RED99bc0b, GREENb5c560); usa currentLoading existente y no modifica el protocolo de red.

Dictamen posterior: **APPROVED para la composición frontend revisada**, condicionado al init del corte integrado y a las puertas de mutación/E2E/UX pendientes. Los hallazgos siguientes se conservan como historial, no se borran ni se atribuyen a la versión corregida. Esta revisión no aprueba un backend todavía incompleto ni certifica cierre de feature13.

## Hallazgos

### R1 — P1: pérdida de contexto en lectura de recuperación no retira datos

`reschedule-block.tsx:204–222`, `MoveFields.loadCurrent()`: después de un412 del envío, «Consultar estado actual» captura cualquier rechazo como `currentFailed`. Un404 `RESOURCE_NOT_FOUND` del contexto nunca llega a `onAccessFailure(404)`, aunque el GET inicial y preview sí propagan ese caso. Permanecen el objetivo y borrador privados en un contexto que el servidor acaba de declarar inaccesible. Un401 actual tiene además el observer global de `apiRequest`; el hallazgo concreto es404, no ausencia universal de gestión401.

Mínimo: clasificar el error de esta lectura y propagar pérdida de contexto, comprobando aborto antes y después del await. Extender el flujo412 existente (`reschedule-block.test.tsx:1234`, hoy sólo503 y posterior éxito) con GET de recuperación404; observar retirada mediante el padre y ausencia de restauración tardía. Contrato@s37 y herencia de privacidad11.

### R2 — P2: preview412 no permite obtener la revisión vigente

`reschedule-block.tsx:254–267,387`: si el bloque cambia después de abrir las acciones y **antes** de revisar, preview devuelve412 `BLOCK_CONFLICT`. El catch sólo muestra «No se pudo revisar el movimiento». No ofrece consultar estado; repetir Revisar usa el mismo `state.revision` obsoleto y vuelve a412. La recuperación implementada en ChangeSubmit sólo cubre un412 posterior al POST de movimiento.

Mínimo: ofrecer la consulta deliberada de estado también tras412 del preview, conservando fechas y retirando revisión/consentimiento. Probar GET revisión1, preview412, consulta revisión2 y preview posterior con If-Match2, sin POST de movimiento automático. Sección13: «Tras412 ofrecer consultar estado nuevo»; @s35 no debe quedar implementado sólo en un punto del recorrido.

### R3 — P2: desaparece el control de recuperación sin destino de foco

`reschedule-block.tsx:198–207,40–42`: activar «Consultar estado actual» tras412 llama `invalidate()`, desmonta preview y su ChangeSubmit, y elimina el botón que tenía el foco. El único efecto de foco de BlockActions se ejecuta al montar (`[]`), no durante esta transición. No hay destino en MoveFields para ese GET pendiente. La misma retirada ocurre al reintentar el GET inicial tras error. El test412 actual usa `fireEvent.click` sin enfocar ese botón, por lo que no observa el estado de teclado.

Mínimo: conservar foco lógico cuando el control elegido desaparece, usando encabezado/control visible existente sólo si el foco terminó en BODY; no robarlo si la persona lo movió. Extender el test de recuperación con botón enfocado y respuesta diferida, comprobando el foco **durante** carga y con otro control elegido. Contrato@s38. Es una conclusión del flujo DOM; no se presenta como medición de navegador realizada en esta revisión.

### R4 — P2: falta aviso de que cerrar no revoca la petición

`reschedule-block.tsx:105–118`: «Cancelar edición» cierra siempre mediante `onClose`, pero no hay aviso en BlockActions/MoveFields/ChangeSubmit de que salir no cancela una operación transmitida. @s38 y sección13 exigen explicarlo antes de que la persona elija salir. La intención sí se descarta localmente y el cleanup aborta la espera; eso no prueba rollback del servidor.

Mínimo: mensaje visible de esta consecuencia cuando corresponda (sin nuevo modal ni guardia global). Un POST diferido permite observar aviso, cierre sin POST adicional y ausencia de confirmación local tardía. Compartir el estado necesario con el padre o usar una explicación incondicional precisa, sin inventar una segunda máquina de recuperación.

### R5 — P2: errores de inicio/zona quedan junto a una revisión válida

`reschedule-block.tsx:198–202,252,303,335`: editar inicio/zona no retira sus `fieldErrors`, y el éxito de preview tampoco los limpia. Fin y selectores sí filtran algunos errores. Tras un inicio inexistente, corregirlo y recibir preview200 conserva `aria-invalid` y «Esta hora local no existe». La revisión de markup ya señalaba esta parte; se resolvió la asociación a más campos, pero no todo el ciclo de corrección.

Mínimo: retirar errores obsoletos de campos afectados y/o al validar la nueva revisión, sin ocultar errores aún vigentes. Extender el caso existente de inicio inválido (`test:1185`) hasta corrección y preview válido; un caso de zona cubre el otro camino de edición. Contrato@s35, errores claros y actuales según UX.

### R6 — P2: revisión pendiente carece de feedback visible de carga

`reschedule-block.tsx:140,224–274,375–387`: `reviewing` sólo cambia una guarda y `aria-disabled`; no se representa como texto/status de revisión en curso. El label «Revisar movimiento» permanece igual y no hay estilo `[aria-disabled]` que aporte señal visual de espera. La primera revisión con red pendiente no comunica visualmente si empezó. El test de coalescing (`test:1426`) comprueba foco y atributo, no feedback visible. El POST final sí tiene «Procesando cambio» en ChangeSubmit.

Mínimo: anuncio de revisión pendiente conservando el control y su foco; extender ese mismo test diferido para verificar estado de carga y retirada al terminar. La medición<400ms queda para E2E, no se inventa desde este análisis. Contrato@s40 y principio Doherty.

## Aspectos comprobados sin nuevos hallazgos

- Los wrappers `.field` y labels asociados ya reutilizan presentación compartida. Cancelación muestra el intervalo de `state.block`, no sólo la fila anterior: dos hallazgos de markup resueltos.
- Preview/información recibida se valida mediante el cliente ya revisado; sólo el submit explícito envía movimiento. Ediciones invalidan preview/consentimiento; pérdida de elegibilidad aborta preview y no restaura permiso al volver a pending.
- Intención transmitida permanece bloqueada y recuperable pese a inelegibilidad. Campos, offsets y checkbox no alteran el cuerpo retenido. Rechazos definitivos desmontan revisión y exigen otra antes de enviar.
- Después de confirmar, TaskBlocks refresca listado por separado y conserva únicamente el artículo histórico más reciente; no inserta el DTO del recibo en la lista. Historial queda disponible aunque no haya reservas, con refreshToken tras cambio y sin GET por fila.
- App remonta TaskReader por ruta; las esperas de preview/clasificación y catálogo comprobadas conservan guardas de aborto. No se exige rediseñar APIs aprobadas para corregir R1.

## Evidencia y límites

Autor reporta124/124 panel39+TaskBlocks85 GREEN `5ecf30`, formato y ESLint `335e9e`, tsc `74969b`; el refactor posterior de aborto en useLayoutEffect tiene foco1PASS `799cbf`, no una repetición124. Esta revisión no convierte esos datos en nuevo rerun ni en aprobación global/E2E/mutación.

Lecturas `624375`, `e83836`, `21bc5a`, `dfaad4`, `b73f68`. SHA256 del corte: panel `25532C60BD0B76C49238190BAA2BA472B95FEF0183DF76D03EA1D44FAC3CDBA2`; test panel `C393632DE00FE5827B518981DBA0612C1E25A44652025C3AD581A53AAAD659BC`; TaskBlocks `EC2D7AC254DD1D96410FC4618C7D5AEDCAD51B02CE049160113EF8206D5FD5A3`; test TaskBlocks `77F5719EB3590C8CAB75CCB74A6EC3D7325D53573462F335E5737AF8DED6A098`; ChangeSubmit `E12B6C1F5ABB4777976EF3792520D4365D53B8A4F5C8E3F06D1AE057A692FB02`.

Se solicita TDD de estos flujos concretos, preferentemente extendiendo casos existentes, y re-review del delta. Geometría real/30principios, motores, zoom y gate de mutación siguen pendientes de su validación correspondiente; no son PASS implícitos ni defectos inventados de dispositivos no examinados.
