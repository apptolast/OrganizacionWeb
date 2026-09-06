# Revisión independiente del historial13

**CHANGES_REQUESTED: foco durante carga.** Revisión parcial de BlockChangeHistory y sus tests congelados; no aprueba integración TaskBlocks, E2E, mutación ni feature13 completa. Ponytail full/Caveman lite. Lecturas5546ba/0c7ce9/76bb92, sin ejecutar Vitest, tsc, init ni modificar fuentes/tests.

## Hallazgo concreto

**Foco perdido mientras la página siguiente o el reintento siguen pendientes.** En reschedule-history.tsx33–40, useLayoutEffect sólo enfoca el encabezado cuando `page || failure`. Al activar Más cambios anteriores con foco en su botón, el handler retira page y ese control. El efecto corre con page undefined/failure false y no enfoca nada. Lo mismo ocurre al reintentar y retirar la alerta/botón. El foco queda en BODY hasta resolver la red; una petición indefinida deja ese estado indefinidamente. El test de paginación/foco actual sólo observa el resultado después de resolver su deferred.

Diseño aprobado design_reschedule_ui.md48–49 y docs/ux-requirements.md52 conservan foco visible durante espera y prevén destino cuando desaparece un control. Corrección mínima propuesta: reforzar el caso existente con aserción al entrar en carga, antes de confirmar GET; enfocar el encabezado de la sección si desapareció el control elegido, preservando un foco externo elegido por la persona. No se pide conservar físicamente el botón, mover foco al cargar inicialmente ni repetir suites globales. Hallazgo aceptado por el coordinador para RED/GREEN del autor; sin reproducción ejecutada por este juez.

## Resto del alcance revisado

- Wrapper con key por proyecto/tarea/refreshToken reinicia página y cursor, desmonta recibos previos y aborta lecturas. Refresh empieza en recientes. Paginación reemplaza filas, no concatena contexto antiguo, y reintento conserva cursor de la página fallida.
- Lista y consulta explícita de estado transportan señal y comprueban aborto tras el await de lectura y después de clasificar error. Cleanup cancela solicitudes de lista/fila; pruebas distinguen401 antiguo, clasificación404 con stream pendiente y éxito tardío. El caso de paginación entrega401 dentro del mismo async act y comprueba observer antes de finalizarlo. No se extrapola esa prueba a toda integración de sesión/rutas del padre.
- 401/RESOURCE_NOT_FOUND notifican onAccessFailure; ausencia de recibo o bloque se queda como error local. El retiro global del contexto corresponde al padre. No hay POST ni lectura por cada fila: cada recibo ya es completo y GET de estado requiere acción explícita.
- Hechos históricos mantienen before/after, tipo, objetivo y fecha UTC, separados del estado actual. Consultar de nuevo retira el estado anterior antes de esperar; fallo conserva el recibo y el mensaje de estado sin comprobar. No convierte un recibo antiguo en proyección vigente ni pierde acceso a cancelados cuando no hay reservas activas.
- Consulta de estado usa guarda síncrona y aria-disabled, evitando duplicados y preservando botón enfocable. La reutilización de BlockDetails/BlockTime mantiene presentación temporal. Cursor sigue opaco para esta UI; formato/contexto del cursor y lookahead del servidor no quedan verificados por ella.

## Evidencia y corte

Autor documenta23/23 GREEN40bba6, Prettier835c38, ESLintcfc683 EXIT0 y freeze19bd38. No son reruns de este juez. Los cinco oráculos finales sobre respuestas antiguas constan inicialmente GREEN, sin fabricar RED ni modificar producción por ellos. El mapa de la bitácora atribuye parsing de cursor al cliente; precisión: el cliente validado trata el cursor como token opaco, la validación interna base64url/colección pertenece al servidor.

SHA256 leídos0c7ce9:

- reschedule-history.tsx: 53FFC532B71E14276DD198485C179F8A7C59D1EB4A54C24129C19FABE8987F75.
- reschedule-history.test.tsx: 6CFB4537AD27F48D6DD5F2EC1113842EC3500250720A261DF8025E7F96AEDE30.

Sin otro hallazgo concreto en este corte. Responsive, axe, zoom, motores,30 principios y reinicio real permanecen pendientes de integración; los tests DOM de historial no los acreditan.

## Re-review del delta — APPROVED en alcance historial

Lectura f2e6d2 confirma el ajuste mínimo: useLayoutEffect ya no exige page||failure, conserva interacted y document.activeElement===BODY. Al retirar el botón por paginación o reintento, el encabezado recibe foco durante la carga; montaje inicial no lo roba y un control externo elegido permanece respetado. No cambia la firma ni el flujo de solicitudes/recibos revisado antes.

El nuevo test parametrizado observa Más cambios anteriores y Reintentar cambios con GET deferred: primero exige status de carga y foco en encabezado **antes** de resolver la respuesta; después mueve foco a Otro control y comprueba que la confirmación no lo roba. La bitácora conserva2RED949ac9,25/25 GREEN44d57d, formato9ddcfe, ESLint25fd8d y diff-check663646. Evidencia del autor, sin rerun de este juez. Hallazgo de foco cerrado; no quedan cambios solicitados en este alcance.

Hashes actuales leídos f2e6d2:

- reschedule-history.tsx: 7C2E037F9573395D651C259A7E4BB8B83E1D0F63DC85D17FFA05EE8050C52C56.
- reschedule-history.test.tsx: 28C747DFF7770FC824EFD2742BCED698E325A5B3DE6CE3D247A392ADCC1AFEF0.

Se mantienen los límites del dictamen parcial: integración del padre, E2E/UX completa, validación del servidor y mutación13 no quedan aprobados por esta revisión. Se preserva el hallazgo inicial como historia.
