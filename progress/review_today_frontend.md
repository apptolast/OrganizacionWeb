# Revisión independiente — frontend Hoy

Rol judge readonly, alcance @s16–37 y SCSS básico. Revisión de today-api.ts, today.tsx, App.tsx, workspace.tsx, use-session.ts, project-reader.tsx, exports de schedule-block-api.ts y suites relacionadas. Sin ejecutar pruebas ni editar fuentes; gate global corresponde al coordinador. Ponytail/Caveman y límites de rutas protegidas conservados.

## Corte preliminar: CHANGES_REQUESTED

Hallazgo concreto @s28: después de consumir la frontera de bloque (por ejemplo serverNow12:59:59, fin13:00), si el GET iniciado permanece pendiente hasta medianoche, no queda timeout que retire el día anterior. El efecto depende de snapshot/failure, ambos intactos mientras la petición sigue pendiente. El arreglo `failure ? dayEndAt : siguiente` cubre rechazo, pero no este intercalado. Hace falta mantener una sola frontera de retirada del día aun con GET pendiente, abortar esa generación al vencer y consultar el nuevo día una vez. Comunicado al autor y coordinador; esperar ciclo y evidencia final. La expectativa de cero timers en @s25 tras disparar una lectura pendiente necesita distinguir ausencia de repetición del bloque de la frontera legítima del día.

## Cobertura y observaciones del resto del corte

- @s16–17: today-api.test.ts cubre esquema15/item3/block9, campos extra/ausentes, tipos, enum sin coerción, orden, IDs, presupuesto/fallback, suma/intersección y candidatos. Valida antes de mostrar. Reutiliza isBlock/instant/uuid/exact/text/integer: el diff de schedule-block-api.ts sólo exporta funciones existentes, sin cambiar sus cuerpos ni DTO11. No compara createdAt con serverNow ni usa Intl para aceptar datos históricos.
- @s18–20: tests de texto seguro, enlace de tarea, cierre con fecha del día siguiente, fallback UNCONFIGURED/UNAVAILABLE e Intl no disponible efectivo/histórico. Render React conserva texto como texto y fallback presenta UTC etiquetado más zona original.
- @s21–23: distingue carga/vacío/error; red y JSON inválido permiten reintento sin Storage.setItem; actualización manual conserva snapshot fechado y foco elegido por teclado.
- @s24–27: pruebas de coalescer idle/manual/initial, fronteras estrictamente futuras, cambio del reloj del dispositivo y ocultación/recuperación. pending se fija síncronamente antes de incrementar generación; controller.abort y guard después de await impiden confirmar resultados de la generación cancelada. La espera usa performance.now desde recepción, no Date.now.
- @s28–30: existe cobertura de rollover con JSON antiguo diferido, nuevo día éxito/fallo, retorno visible después de medianoche y resultados JSON/401 después de desmontaje. El observador401 recibe Response después de invalidar contexto, sin inventar una ventana async posterior a headers. Pendiente la secuencia específica descrita arriba; no se atribuye a esos tests una demostración del caso no cubierto.
- @s31: SessionGate real en test retira agenda mientras logout permanece pendiente y la nueva sesión carga sin datos previos. Aborto al desmontar también cubre la frontera de lectura obsoleta; no se introdujo persistencia privada.
- @s32–35: captura exacta /proyectos/nuevo precede al patrón de detalle; sufijos desconocidos no caen al formulario. Ambos enlaces de creación migrados. Tests directos y suites históricas de autenticación conservan rutas; test nuevo distingue sesión inicialmente autenticada (404 local) de login desde anónimo (normalización histórica a raíz). El guard isPrivateRoute se conserva y sólo añade captura explícita.
- @s36–37: Workspace deriva una sección, aria-current único y breadcrumb; main enfocable mantiene destino del skip link. Tests operan Actualizar mediante Enter y mantienen enlace elegido tras éxito/fallo; no hay efectos que fuercen foco tras respuesta.

SCSS básico: wrap anywhere, columnas minmax(0,1fr), items min-width0 y enlaces/botones mínimo44px de altura, sin ancho rígido nuevo. Esto es revisión estática: dimensiones, motores, zoom nativo y matriz30 pertenecen a evidencia E2E/UX independiente @s38; no acredita layout real ni lector de pantalla.

Evidencia previa recibida del autor:71 API+35 UI; regresión1315 verdes996e1e, build c9040d. Es anterior al arreglo temporal en curso; no sustituye la entrega final ni init del coordinador. Ningún otro defecto funcional observado en este corte.

## Revisión del corte corregido — APPROVED (alcance frontend @s16–37)

Relectura readonly fb0ef5 de today.tsx, ambos tests nuevos y bitácora ciclos31/32. El hallazgo de este informe queda resuelto: el efecto incluye revision y failure, usa elapsed monotónico para no rearmar el bloque vencido y mantiene siguiente frontera conocida mientras una lectura espera; después de fallo sólo conserva dayEnd. Al vencer el día refresh detecta deadline, aborta generación anterior y retira snapshot antes de iniciar nueva lectura. No hay polling ni más de un timeout vigente.

Oráculos concretos: `a failed block boundary still retires the old agenda at midnight without focus` comprueba Sin actualizar después del rechazo y retirada/carga nueva al dayEnd; `a pending block-boundary request is replaced at midnight` exige tercera petición, abort de la segunda y no restaurar datos cuando ésta finalmente responde. Las expectativas @s25/@s29 se ajustan para permitir la frontera legítima del día conservando contador de peticiones y ausencia de repetición de la frontera vencida; no debilitan el comportamiento requerido.

Evidencia del autor: RED14e63f→GREEN9541d4 y RED934607→GREENdc9f61; foco108 API/UI (71+37) GREEN8cb4bb, formato/lint/build/types GREEN294fcd. No ejecuté pruebas ni edité código. No quedan cambios funcionales requeridos en este alcance. Aprobación independiente de diseño/cobertura frontend; la puerta global de init y evidencia UX/E2E @s38 siguen siendo responsabilidad del coordinador y no se anticipa aprobación de mutación o done.

## Reapertura acotada @s24–28 — CHANGES_REQUESTED

Consulta adicional del coordinador confirmada por lectura estática a4834e, sin ejecutar pruebas ni editar fuentes. En today.tsx:102 ocultarse cancela timeout. Con snapshotD confirmado y GET manual pendiente, volver visible antes de dayEnd llama refresh (:71), pero retorna en :58 por pending sin modificar revision ni otra dependencia del efecto (:109). El timer cancelado no se reconstruye; si GET continúa pendiente al día siguiente, agendaD permanece como Hoy. El deadline sólo se comprueba al llamar refresh, no por sí mismo. La aprobación anterior queda reabierta exclusivamente para esta secuencia.

Debe conservarse la coalescencia @s24 (no duplicar GET manual por visible/focus) y la regla @s27 de esperar snapshot nuevo para reconstruir fronteras de negocio; aun así @s28 exige retirar el día anterior al deadline, cancelar la generación pendiente y consultar una vez el nuevo día. Solicitud de ciclo RED exacto al autor resume_review y notificación al coordinador. No se modificó producción durante init35422/COPY activo. Este hallazgo es análisis de flujo, no resultado de prueba ya ejecutada.

## Revisión final de recuperación visible — APPROVED (@s16–37)

Relectura independiente8ec664 del freeze7cf936: resueltos tanto el hallazgo manualpending→hidden→visible como la variante idle→visible→GET pendiente. El listener de visibilidad conserva únicamente dayDeadline y cancela su timer anterior. awaitingVisibleSnapshot impide que el efecto reconstruido por revision use fronteras de negocio del snapshot anterior; se libera sólo al aceptar respuesta vigente. Se conserva coalescing de GET y el deadline se cancela al ocultarse. No se añadió infraestructura ni persistencia ni polling.

Tests nuevos concretos: `returning visible with a manual request pending keeps only the day deadline` mantiene2peticiones después de visible/focus y al horario viejo, exige tercera al dayEnd, retiro, abort y no restauración tardía. `visibility recovery waits for its new snapshot before rearming a block boundary` inicia GET desde idle, cruza horario viejo sin tercera petición y conserva un timer hasta confirmar el nuevo snapshot. No son asserts espejo del ref: observan peticiones, tiempo y pantalla. Las secuencias temporales anteriores siguen en la suite.

Evidencia entregada: RED388bcb→GREENda5ae1; RED36601f→GREENfba8b4,110API/UI (71+39); formato/lint/types/build7cf936. Ningún hallazgo funcional pendiente en este alcance. Init35422 (1317frontend/1415backend) se identifica correctamente como corte anterior; el coordinador completa gate global y @s38. Esta revisión no ejecutó suites ni mutación, ni modificó fuentes/tests. El manifest de mutación requiere hash actualizado antes de medir; aprobación de diseño/cobertura no autoriza por sí sola esa ejecución.

## Corrección de foco posterior a medición — APPROVED (@s37)

Diff readonly dec458: único cambio productivo disabled={loading}→aria-disabled={loading} en Actualizar. Conserva semántica de indisponibilidad sin retirar el botón del foco; pending.current ya impide acciones repetidas, por lo que no se añade envío ni focus() programático. Test nuevo opera Enter, conserva foco/aria-disabled, repite Enter sin tercera petición y confirma foco después de503. No debilita guards ni tests previos.

Evidencia autor: RED3aac94 (atributo accesible)→111API/UI GREENeeb6ae; build/types9f4c89. El blur real fue reproducido por E2Ecc49ad, no por JSDOM; root comunica Chromium4/4GREEN6b8014 y posteriormente12/12motores GREEN. La corrección ocurrió después de campaña originale5ccd3 y no hereda una muerte de disabled inexistente. Revisión funcional aprobada; init15059 en curso a cargo del coordinador antes de nuevo TDD o medición. No ejecuté pruebas ni edité fuentes/tests.
