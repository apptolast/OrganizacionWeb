# Diseño UI13: mover y cancelar bloques

Corte de especificación5512b24. Propuesta de implementación tras contrato Gherkin; no cambia decisiones de producto ni activa TDD. Ponytail full/Caveman lite. Sin fuentes/tests/config ni nuevas dependencias.

## Mapa mínimo de responsabilidad

|Pieza|Cambio acotado y reutilización|
|---|---|
|TaskBlocks|Conserva listado/paginación/reload y encabezado enfocable. Añade selección de una acción y sección de cambios inline; no consulta state por cada fila.|
|Panel de acciones de bloque|Una instancia para el bloque seleccionado, con fases cargar estado, editar movimiento o confirmar cancelación, transmitir, recuperar y confirmar. Se desmonta al cambiar contexto.|
|Formulario de movimiento|Reutiliza controles datetime-local, zona, ocurrencias DST, preview/presupuesto y consentimiento de11. Objective sólo lectura; no convertir BlockEditor en motor genérico sin necesidad demostrada.|
|Historial de cambios|Lista semántica paginada por tarea, con before/after, fecha, acción y enlace/botón para consultar estado. Mantiene estado de carga/error/cursor independiente del listado activo.|
|Cliente API13|Funciones por endpoint y tipos cerrados state/recibo/página; reutiliza apiRequest, isBlock, uuid, instant y exact de schedule-block-api. No añade campos a Block9 o Today15.|
|Presentación temporal|Reutiliza BlockDetails/BlockTime; extraer o exportar sólo si otro módulo realmente los necesita. Hoy conserva su lectura y sus fronteras de actualización12.|

La composición exacta de archivos se decide en TDD. Separar el nuevo panel si evita ampliar el editor de creación; no introducir store global, reducer genérico, cache, librería de fechas ni framework de formularios.

## Estado y transiciones observables

|Estado|Presentación y siguiente acción|
|---|---|
|Listado inicial|Vacío confirmado distinto de cargando/error; cada reserva ofrece mover/cancelar. Creación11 sigue disponible según elegibilidad confirmada.|
|Consulta de state|Mostrar contexto seleccionado y carga; no habilitar escritura hasta Block+status+ETag válidos. Cancelled ofrece hecho/estado e historial, sin nueva acción de negocio.|
|Edición|Antes confirmado y campos de destino; modificar horas/zona/ocurrencia invalida preview y consentimiento. Zona histórica no resoluble exige zona/horas explícitas, sin zona implícita del navegador.|
|Preview|Comparación antes/después, duración, zona de presupuesto y días. Error conserva campos y retira revisión vieja. BLOCK_UNCHANGED no produce falsa confirmación.|
|Confirmar cancelación|Texto de objetivo/intervalo y liberación de reserva; no exige disponibilidad ni elegibilidad de tarea/proyecto. Separar «Cancelar edición» de «Confirmar cancelación del bloque».|
|Transmitiendo|Retener tipo, blockId, cuerpo exacto, key y ambos headers aplicables; bloquear edición y doble envío. Anunciar trabajo en curso sin perder foco.|
|Resultado incierto|Conservar intención; comprobar por key. Sólo404 del recibo o recuperación CSRF reconocida habilita reenvío manual según contrato. Ninguna key nueva ni POST automático.|
|Conflicto definitivo|Conservar borrador, retirar preview/consentimiento;412 requiere consultar state antes de revisar otra intención. IDEMPOTENCY_CONFLICT conserva recuperación bloqueada.|
|Recibo confirmado|Mostrar el hecho histórico; recargar listado/cambios y consultar state para vigencia. Si state falla, «Operación confirmada; estado actual sin comprobar». No insertar after histórico en lista activa.|

La confirmación11 conserva comparación con preview original y by-request/replay; sólo se etiqueta como hecho histórico. readBlock actual pertenece a BlockConflict y no sustituye recuperación. Cancelación confirmada puede vaciar la lista sin borrar la sección de cambios.

## Identidad, seguridad y recuperación

- Revisión conservada como ETag textual; BigInt para aritmética necesaria, nunca Number inseguro. Recibo puede ser anterior a state; no asumir monotonía de occurredAt/updatedAt.
- Una acción abierta pertenece a sesión, proyecto, tarea, bloque y generación. Abortar la anterior antes de iniciar otra; comprobar vigencia tras respuesta, JSON, clasificación de error, renovación CSRF y finally.
- Abortar antes de entregar un401 obsoleto al observer compartido, incluidos callbacks de promesa anteriores al cleanup de React; reutilizar el patrón corregido12.
- RESOURCE_NOT_FOUND actual retira contexto; BLOCK_NOT_FOUND y BLOCK_CHANGE_NOT_FOUND no invalidan toda la tarea. No traducir error de lectura en vacío.
- Pérdida de elegibilidad impide movimiento nuevo pero conserva recuperación transmitida; no desmontar la intención incierta al completar tarea/proyecto. Cancelar depende del estado del bloque, no de esa elegibilidad.
- Listado, state, historial y operación tienen peticiones independientes: un error tardío de listado no revoca éxito; un historial antiguo no reemplaza otra tarea.
- No persistir borradores, keys, recibos ni datos privados en navegador. Salir/cerrar descarta estado local y explica que no revoca una petición enviada; logout retira datos inmediatamente.

## Navegación y foco

- Reusar la ruta de detalle de tarea; no nueva pantalla ni deep link de calendario. Desde Hoy se llega por el enlace existente y al volver rigen refresh/privacidad12.
- Un panel activo evita decisiones simultáneas; cerrarlo o cambiar acción no debe disparar POST. No añadir guardia global de navegación.
- Al abrir, ordenar foco y lectura hacia el encabezado/campos del panel; cerrar vuelve al control de origen si sigue conectado y habilitado. Si desapareció el bloque/control, foco al encabezado de Bloques planificados.
- Tras async, conservar el destino elegido por la persona. Para controles temporalmente indisponibles conservar foco con aria-disabled y guarda efectiva si procede; no depender sólo del atributo.
- FormSubmit/Enter y click comparten la misma guarda de envío. Shift+Tab alcanza revisión/guardar y Tab sale sin trampa. Estado con role=status y errores con alert; errores de campo asociados por id.

## Responsive y evidencia prevista

Heredar .task-blocks, .task-list, .block-review, formularios y patrones .task-history. Lista vertical de cambios antes/después en móvil; evitar tabla ancha obligatoria y abreviaturas que oculten fecha/zona. Estilo nuevo sólo por necesidad observada, sin breakpoints inventados preventivamente.

Aplicar las30 filas de docs/ux-requirements.md al flujo: contexto y acción claros, campos próximos a error, consentimiento explícito, cancelación distinguible sin depender de color y texto largo literal. Evidencia futura:320–2560 CSS, lados de breakpoints heredados/tocados, altura reducida, zoom nativo200%, texto ampliado, controles44px, foco/teclado y Chromium/Firefox/WebKit. Axe no acredita facilidad de uso ni dispositivos físicos; registrar límites humanos/táctiles reales.

Priorizar pruebas públicas de mover excluyendo propia reserva, cancelar y recuperar historial,412 con borrador,ACK perdido con replay histórico y state posterior,401 obsoleto y foco tras retirada de fila. Reusar fixtures11/12; no un test por mutador ni matriz duplicada de DTO. E2E usa API/PostgreSQL reales tras freeze coordinado; errores controlados se identifican como fixtures.

Diseño documental listo para revisión. No se ejecutaron suites ni se tocaron rutas protegidas. El contrato Gherkin definitivo prevalece sobre nombres internos sugeridos aquí.
