# Plan mínimo E2E y UX17

Plan documental, no pruebas ejecutadas. El primer E2E está congelado RED por ausencia del montaje17. No añadir otro caso hasta repetirlo GREEN con bundle B aprobado. Smoke final@s23/@s24 está aprobado por raíz (932596/fb1437); su pérdida de ACK, reinicios y recuperación real no necesitan duplicarse en cada caso UX.

## Secuencia propuesta

1. Repetir sin ampliar el único `e2e/end-time-notification.spec.mjs`: inicio desde tarea, cantidad vacía al abrir, EXTEND explícito15min, fin exacto SQL y navegación al detalle16 con E/C. Resolver cualquier fallo demostrado con su autor antes de continuar. Comando focal: `node scripts/e2e.mjs e2e/end-time-notification.spec.mjs`.
2. Un recorrido de aviso real@s28/@s30/@s35: sesión con fin alcanzado confirmado por E; aviso no escribe ni mueve foco; ampliar y retirar aviso sólo tras E nuevo. Si requiere vencimiento, usar reloj/fixture coherente ya permitido por infraestructura, sin falsificar un éxito17; evitar una espera larga arbitraria. Registrar criterio del fixture antes de implementarlo.
3. Un recorrido de recuperación/composición UI@s36–40: ACK perdido tras POST real201, comprobar K, consulta E posterior distinta y recarga URL sin key. Intercalar sólo la decisión hermana que la revisión final B identifique como diferenciable (PAUSE/RESUME/CLOSE); no repetir todas las combinaciones ya probadas por UI y PG. Sesión paused de otra tarea propia mostrada en detalle debe incluirse en esta composición si aún no la acredita el primer recorrido o B.
4. UX@s43–44: un caso por ciclo, reutilizando el patrón `e2e/close-work-session-ux.spec.mjs`. Recorrer ambas superficies con estados aviso/formulario, espera, incertidumbre, confirmación y fallo de actualización. Una matriz de geometría31anchos320–2560/bordes y altura400, no una matriz de datos nueva. Revisar orden Tab/Enter, foco visible,44×44 de área efectiva, nooverflow y axe por estado. Retener una respuesta real para medir anuncio<400ms sin afirmar latencia de servidor.
5. Después del primer caso UX GREEN: caso separado texto200%; después zoom nativo200% Chromium con evidencia real del factor/viewport, no sólo ancho reducido. Ejecutar los recorridos existentes pertinentes en Firefox/WebKit; documentar disponibilidad y límites por motor. Capturas de viewport además de fullPage evitan interpretar skiplink fuera de pantalla como un defecto sin medirlo.

No repetir por navegador los casos de reloj largo/fragmento1µs/aborto que B ya valida con timers deterministas salvo fallo material de integración. Visibility sí merece conexión observable en el recorrido real si no existe aún, sin prometer alarmas durante suspensión. Root coordina18080 y paquetes; todos los runners deben retirar sólo su stack. No iniciar ahora servicios, campañas o tests.

## Matriz30: aplicación y evidencia requerida

Todas las filas están **pendientes de UX17**; referencias16 sólo indican método reutilizable, no PASS17. La inspección visual/humana complementa geometría y axe.

| Principio | Aplicación17 / evidencia que debe quedar |
| --- | --- |
| Atención selectiva | Aviso del fin distinguible, una decisión deliberada y secundarios discretos; capturas de ambas superficies. |
| Carga cognitiva | Cantidad solicitada sólo al ampliar; fin/contexto visibles sin memorizar otra pantalla. |
| Estética-usabilidad | Texto legible y estilos coherentes en éxito/error, revisión visual de capturas. |
| Posición en serie | Cerrar/ampliar/comprobar y orden Tab previsibles al cambiar ancho. |
| Tendencia a la meta | EXTEND no añade tiempo trabajado ni progreso; contraste con recibo y SQL del funcional. |
| Von Restorff | Aviso y error con texto/semántica además de color; axe y lectura visual. |
| Zeigarnik | Salir no revoca POST; recibo/fin durable recuperables, sin presión para continuar. |
| Fluir | Fin respetado, aviso sin modal/foco robado y opciones controlables. |
| Fragmentación | Fin original/acordado, formulario y recibo separados por significado. |
| Memoria de trabajo | Cantidad retenida en412/incertidumbre; comprobar no obliga a reconstruir intención. |
| Navaja de Occam | Sin controles adicionales a ampliar/cerrar/consultar/comprobar pertinentes. |
| Conectividad uniforme | Enlaces reflejan tarea/sesión reales; no diagrama decorativo requerido. |
| Fitts | Medir44×44 efectivos y separación de botones/enlaces; teclado sin hover. |
| Hick | Revelar cantidad al ampliar, sin confirmación preaceptada ni menús innecesarios. |
| Jakob | Input/formularios/enlaces nativos, Enter y regreso conservan convenciones. |
| Semejanza | Estados y controles equivalentes coherentes entre tarea y URL16. |
| Miller | Agrupación de contexto/decisión/resultado; no regla artificial de siete opciones. |
| Parkinson | Vencimiento nunca amplía solo; cantidad vacía y POST únicamente explícito. |
| Postel | Límites enteros1–1440 claros; no coerción permisiva ni error técnico expuesto. |
| Proximidad | Label, ayuda y error asociados al input y juntos a320px. |
| Prägnanz | Abierta/paused/closed/incertidumbre se explican con texto, sin iconos únicos. |
| Región común | Panel17 es unidad funcional; formulario no anidado con cierre ni otros submits. |
| Tesler | Keys/revisiones internas, fechas y zona legibles; conservar fin original sin recalcularlo conIntl. |
| Modelo mental | Ampliar fin no reanuda ni completa tarea y no añade neto; aviso no es cierre. |
| Usuario activo | Ayuda contextual suficiente para primera ampliación sin manual. |
| Pareto | Acciones frecuentes directas, recuperación disponible; sin porcentajes de uso inventados. |
| Fin de pico | Confirmación sólo cierta por recibo válido; errores no pierden cantidad ni fabrican éxito. |
| Sesgo cognitivo | Descanso no penalizado y fin elegido separado de trabajo real. |
| Sobrecarga de opciones | Sin personalización/alarma externa extra; cantidad revelada progresivamente. |
| Doherty | Medición real de feedback<400ms, estado de espera honesto y foco sin robo. |

## Entrega y límites

Guardar por ejecución fuente/hash/log, JSON de geometría/axe/feedback/zoom y capturas revisables; anotar RED funcional frente a defecto de fixture. El informe final completará las30filas con evidencia o límite explícito, sin extrapolar a móviles físicos, teclado virtual, áreas seguras, lectores de pantalla ni facilidad de uso universal. No añadir historial18 ni notificaciones externas. Global E2E y campañas17 sólo cuando root autorice freeze integrado; este plan no cambia ese gate.

## Decisión del vencimiento real (lectura c6ec06/3dcffa)

No hay control remoto del Clock de negocio en el runner existente: `ApplicationConfiguration.clock()` devuelve `Clock.systemUTC()`; `scripts/e2e.mjs` sólo configura credenciales, origen, límite de proyectos y puerto. El helperToday desplaza una reserva SQL y authentication desplaza timestamps de expiración, pero ninguno mantiene conjuntamente el hecho de inicio, recibos, proyección y eventos de una sesión14–17. No se reutilizará ese desplazamiento parcial para fabricar un vencimiento.

Método elegido: **inicio real de1minuto por UI y espera del GET E que dispara el panel al vencer**. Es el mínimo permitido por contrato y cuesta aproximadamente60s; no requiere infraestructura nueva ni tocar datos históricos. Caso con timeout explícito120s y espera de respuesta hasta90s, limitada a esa ruta y sesión. Esos límites permiten ejecutar el comportamiento real, no relajan su oráculo ni añaden un sleep fijo.

Tras POST201, capturar E inicial200 y comprobar con SQL `serverNow::timestamptz < effectiveEndAt::timestamptz`. Registrar inicio original, fin, revisión y conteos. Mantener la página visible y un control ajeno enfocado; esperar la siguiente respuesta E del panel, sin sondear desde el test, avanzar relojbrowser ni usar Date.now como autoridad. Para afirmar vencimiento exigir HTTP200 real y comparación PostgreSQL `serverNow::timestamptz >= effectiveEndAt::timestamptz`; conservar payload/instantes en la evidencia. El tiempo monotónico del runner puede documentar duración observada, pero no decide si llegó el fin.

A continuación comprobar aviso, foco conservado y conteos sin nueva decisión. Abrir Ampliar tiempo, elegir1minuto y confirmar POST201 real; esperar E posterior con fin futuro, exigir revisión nueva, fórmula SQL exacta y retirada del aviso. No es necesario esperar otro minuto: el rearme/segundo aviso y unicidad por renders se cubren con los timers deterministas de B; si la revisión final detecta un hueco real se acotará entonces. Un único recorrido suficiente conecta el timer de navegador con el Clock real del servidor y la ampliación deliberada.

Si el E inicial ya aparece vencido por una demora externa superior a un minuto, el caso debe fallar con su evidencia de tiempos para diagnóstico; no ajustar fechas, reintentar silenciosamente ni declarar probado el timer. Si no llega la consulta del panel dentro del límite, también es fallo explícito. Esta decisión sólo actualiza el plan: todavía no hay test nuevo ni ejecución.
