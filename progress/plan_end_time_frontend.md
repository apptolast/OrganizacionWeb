# Plan mínimo de integración frontend17

Plan previo a TDD, basado en normativa2F44…ACE02 y lectura de fuentes b4c0dd; no implementación ni pruebas ejecutadas.

## Montaje y coordinación local

- Detalle de tarea: `work-session.tsx` conserva inicio14/GETactive y monta el panel17 junto al `WorkSessionStatePanel` existente. Un pequeño contenedor local identificado por active.id posee la coordinación de ambos; cambiar de sesión desmonta y aborta su contexto anterior. No condicionar la identidad de active a la tarea alojadora.
- URL16: `work-session-reader.tsx` conserva carga S/F, notas y cierre. Monta el mismo panel17 para esa identidad, fuera del formulario de cierre, y posee la coordinación con CLOSE. No duplicar Reader ni StatePanel ni crear otra ruta.
- Compartir únicamente coordinación mediante un hook local específico `use-work-session-decision.ts`: propietario de decisión, bloqueo inmediato en ref y generación de lecturas observable por los hijos. Una instancia por superficie/sesión, sin singleton, contexto global ni dependencia nueva.
- Cada componente conserva su intención exacta y su recibo: PAUSE/RESUME en StatePanel, CLOSE en Reader, EXTEND en panel17. El hook no reescribe cuerpos ni centraliza todos los protocolos en una máquina genérica.
- Adquirir el bloqueo de forma síncrona antes del POST, no sólo mediante un setState propagado después del render. El propietario puede comprobar/reintentar su misma intención; los hermanos no pueden iniciar otra decisión durante envío, comprobación o incertidumbre. Consultas y navegación siguen disponibles.
- Mantener bloqueo en resultados inciertos/CSRF/ausencia de K hasta resolver o retirar legítimamente la intención; liberar ante rechazo definitivo o confirmación.412 conserva el borrador pero retira la intención y exige snapshot nuevo y otra confirmación. Un desmontaje descarta sólo memoria local, no revoca el POST.
- Al iniciar una decisión, invalidar/abortar lecturas anteriores de estado/neto/fin. Al confirmarla, invalidar otra vez las lecturas que pudieron empezar durante el envío y refrescar dependientes. No desmontar paneles por cada generación: se perderían recibos, borradores y foco.

## Archivos y propiedad prevista

- Frontend: nuevos `work-session-end-api.ts` y `work-session-end.tsx`, sus pruebas focales y el hook local con pruebas públicas de composición; `work-session.scss` sólo para controles/estados propios que lo necesiten.
- Compartido necesario: `work-session-state-api.ts` para unión discriminada EXTEND7 y recuperación C/K; conservar State6, snapshot3 y contratos P/R/C. Reutilizar validadores exactos, apiRequest, SnapshotTime y problemas existentes, sin editar api-client salvo defecto demostrado.
- Integración: `work-session-state.tsx`, `work-session-reader.tsx`, `work-session.tsx` y sus pruebas. TaskReader/App/use-session mantienen rutas y contrato: no se prevé cambiarlos si las props se resuelven dentro de esos tres propietarios.
- Backend/HTTP/publicador/migración pertenecen a A/C; este plan no les asigna ediciones. Configuración de mutación/E2E se inventaría al freeze real, no ahora.

## Resultados que deben descartarse

- GETactive14 del padre anterior a una confirmación/navegación no puede restaurar otra sesión o un estado previo; conservar su guardia actual y conectar cualquier actualización posterior necesaria sin tocar el recibo original.
- GETstate15, GETend-time17 y GETclosure16 capturan identidad/generación; tras cada await de respuesta, JSON o clasificación comprueban vigencia. El aborto ocurre antes de que apiRequest propague un HTTP401 al observador, no sólo antes de setState.
- POST/K anteriores a cambio de ruta no alteran datos, errores, bloqueo ni foco del contexto nuevo. Una consulta actual fallida conserva recibo histórico y aviso previamente confirmado; no convierte fallo en ausencia/cierre.
- El panel17 posee un único timer y lectura coalescida: cancelación al invalidar/cerrar/desmontar, fragmentos positivos hasta2^31−1ms y rearme monotónico; visible dispara consulta sin duplicar la pendiente. Un callback antiguo no rearma ni anuncia.
- Cada superficie coordina sus snapshots; no promete sincronía entre pestañas. La revisión del servidor decide sus carreras. Los recibos históricos siguen visibles aunque una consulta vigente cambie status o falle.

Primero cliente nominal/discriminación, después panel y coordinación por oráculos públicos individuales; finalmente composición en ambas superficies. El Gherkin aprobado fijará los casos, sin añadir store global, router, biblioteca ni duplicación de componentes15/16.
