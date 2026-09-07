# Propuesta UI19 — revisión semanal

7 de septiembre de 2026. Propuesta documental, pendiente del contrato revisado; sin código ni pruebas nuevas. Se aplican Ponytail full/Caveman lite y `docs/ux-requirements.md`. A redacta `proposal_weekly_review.md`; esta propuesta se alinea con sus datos y no añade métricas.

## Una vista, un objetivo

Ruta propuesta `/revision-semanal`, enlace «Revisión semanal» en Principal y h1 del mismo nombre. Reutilizar App, Workspace y RouteLink; no crear un router, calendario interactivo ni pantalla de configuración. La navegación móvil existente ya permite envolver enlaces; comprobar el quinto enlace a 320 px, sin ocultarlo en un menú nuevo.

Orden: título, semana y zona aplicadas; controles de navegación; actualización; resumen; siete días lunes–domingo. Resumen con un `dl`: «Plan vigente», «Trabajo registrado» y «Presupuesto actual». Cada día repite esos tres conceptos bajo su fecha y encabezado. Una agenda vertical semántica funciona también en escritorio; no hace falta un gráfico, barras de cumplimiento ni siete columnas estrechas. La comparación debe poder leerse sin depender del color.

«Plan vigente» suma reservas actuales: mover o cancelar puede cambiar una semana pasada. «Trabajo registrado» suma intervalos efectivos, incluidas sesiones abiertas sólo hasta la hora del snapshot. No equivale a tareas terminadas ni a todo el tiempo transcurrido. Mostrar «Actualizado a…» y botón «Actualizar revisión»; ningún contador local ni autoavance de duración.

Presupuesto cero: «Sin tiempo presupuestado actualmente», con ayuda común que permite contemplar días de descanso sin afirmar descanso real. Pausa, ausencia de reservas o cero trabajo no acreditan descanso. Presupuesto null: «No disponible», con la causa de preferencia/zona; no convertirlo en cero. Nunca mostrar deuda, puntuación, racha o porcentajes de productividad. Si hay sesiones antiguas no cuantificables, conservar su aviso junto al trabajo registrado; cero cuantificable no significa ausencia demostrada de todo trabajo.

## Fechas, zona y navegación

Enlaces «Semana anterior», «Semana actual» y «Semana siguiente», más un formulario nativo con «Fecha de la semana» y «Zona horaria»/«Usar zona de disponibilidad», según catálogo existente. Botón «Mostrar semana» aplica ambos juntos; editar campos no dispara consultas. La URL conserva date/zoneId para Back, enlaces y recarga. El servidor determina lunes, siete fechas y fronteras; el cliente no reconstruye días sumando 24 horas ni redistribuye intervalos con su TZDB.

La zona y las fechas visibles pertenecen a la respuesta aplicada, no al borrador. Cambiar zona puede cambiar reparto diario y disponibilidad del presupuesto. Mantener la fecha ancla al cambiar sólo zona; «Semana actual» conserva la selección de zona y omite date para que el servidor elija el día actual. En extremos de rango, no generar un enlace fuera de contrato. Error de fecha/zona corregible junto al campo; no ocultarlo tras un botón que repite la misma consulta inválida.

## Estados, privacidad y foco

Carga inicial anunciada sin ceros provisionales. Al cambiar semana/zona, retirar la respuesta anterior para no atribuirla a criterios nuevos. En actualización de la misma URL puede conservarse el snapshot con «Actualizando…»/«Sin actualizar», nunca como resultado nuevo. Un fallo 503 ofrece reintento manual; 409 temporal explica que no se pudo calcular la semana y permite consultar otra fecha o actualizar. Semana válida sin reservas/trabajo conserva sus siete días y presupuesto conocido; no necesita inventar una llamada a planificar ni reprochar inactividad.

Reutilizar apiRequest/AbortController y guardas después de cada await: respuesta de una URL anterior no restaura datos ni su 401 revoca acceso actual. Un 401 vigente retira datos privados. Ningún caché persistente, almacenamiento local o consulta por cada día.

Navegación mediante enlaces conserva modificadores, apertura en pestaña y Back. Aplicar/reintentar no roba foco a quien ya salió del control. Si una navegación deliberada retira el iniciador todavía enfocado, mover foco al encabezado de la semana recibida; reutilizar el patrón corregido de History, no la bandera permanente de interacción. Error de campo queda descrito por su ayuda/alerta y visible al recibir foco. Carga anuncia pronto sin fingir porcentaje.

## Archivos y reutilización mínima

- Nuevos `frontend/src/weekly-review-api.ts` y `.test.ts`: consulta/decoder del DTO aprobado, exactitud BigInt, fechas/identidad de criterios y sumas; reutilizar validadores existentes mediante export mínimo sólo si hace falta.
- Nuevos `frontend/src/weekly-review.tsx` y `.test.tsx`: formulario, URL, snapshot, resumen y agenda de siete días. Reutilizar formateo de duraciones exactas y fallback UTC de sesiones; no copiar Today como otra máquina de estados ni sus sumas en segundos.
- `frontend/src/App.tsx`, `workspace.tsx` y sus pruebas pertinentes: una ruta y un enlace accesible, siguiendo Historial. `navigation.tsx` permanece sin cambio previsto.
- `frontend/src/styles.scss`: sección local de revisión, controles nativos y reflow; reutilizar superficies, formularios y navegación existentes. No cambio previsto en History, TaskReader, ProjectReader ni paneles de sesión.
- E2E/UX propios se definirán después del contrato y del freeze; no se crean en esta propuesta.

## Riesgos y evidencia que faltará

Prioridades observables: presupuesto null frente a cero; datos incompletos legados; semana futura; días de 23/25/0 horas; cambio de zona; misma fecha con snapshot distinto; respuesta vieja/401 tardío; error de campo, reintento y navegación por teclado. El cliente presenta días calculados por el servidor y no recalcula workDate histórico de cierre. No se solicitan matrices duplicadas de validadores heredados.

Las treinta filas de `docs/ux-requirements.md` se revisarán con evidencia de esta pantalla: jerarquía y agrupación (título, resumen, días), decisiones limitadas (fecha/zona), contexto visible y recuperación, métricas neutrales y descanso, controles coherentes y semántica nativa. Fluir/Parkinson se preservan porque consultar la revisión no altera sesiones ni su fin; no se añade una acción de escritura. No se promete cumplimiento psicológico por diseño.

Pendiente tras implementación: mediciones de áreas principales 44 × 44, foco y feedback, contenido largo, vacío/carga/error/éxito, anchos y bordes de breakpoints, altura reducida, texto y zoom nativo 200 %, Chromium/Firefox/WebKit y matriz explícita de treinta principios. Dispositivos físicos, teclado virtual y evaluación humana se declararán según evidencia real; los resultados de Historial no certifican esta nueva pantalla.
