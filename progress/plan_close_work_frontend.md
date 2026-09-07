# Plan mínimo frontend16

Mapa de lectura091008/ae15d6, sujeto al contrato Gherkin16 y su aprobación. Sin implementación, pruebas nuevas ni cambios a fuentes. Se reutilizan arquitectura14/15, SCSS nativo, apiRequest y UX30.

| Archivo | Cambio previsto |
| --- | --- |
| work-session-state-api.ts y prueba | Ampliar WorkSessionState con closed y unión de recibos CLOSE7/closure4, conservando PAUSE/RESUME6. POST cierre con notas normalizadas en intención; recuperación por key valida también notas. GET closure por sessionId sin Location. Reutilizar exact, UUID, microseconds/BigInt y problemas cerrados. |
| work-session-reader.tsx y prueba, nuevos | Recurso de ruta /proyectos/{p}/tareas/{t}/sesiones/{s}: main/heading y regreso a tarea, consulta state por ID, verifica proyecto/tarea/sesión antes de mostrar, y consulta closure cuando closed. Reutiliza panel15; no remonta todas las colecciones de TaskReader. |
| work-session-state.tsx y prueba | Cierre inline desde running/paused en URL estable, notas opcionales, envío y recuperación de intención, borrador conservado tras412. Estado closed sin Pausar/Reanudar; recibo histórico separado de consulta actual. Extraer formulario CloseWorkSession sólo si el refactor GREEN lo justifica; no crear máquina genérica ni framework de comandos. |
| work-session.tsx y prueba | Enlace a recurso de sesión antes del POST cierre. Mantener confirmación histórica y nueva consulta active por generación independiente; otra sesión activa se presenta aparte. Callback mínimo al confirmar cierre si la composición necesita refrescar active. |
| App.tsx y prueba | Reconocer ruta exacta de sesión antes de la ruta de tarea, montar lector con key de contexto. Conservar rutas existentes y404. |
| use-session.ts y prueba | Admitir retorno privado a la ruta de sesión: el regex actual sólo permite proyecto/editar/tarea. Conservar reglas de login, query y descarte de rutas desconocidas. |
| work-session.scss, sólo por necesidad observable | Reusar .field/.task-form y controles existentes; notas como texto, saltos preservados y palabras largas con reflow. Sin CSS global preventivo. |

TaskReader ya pasa contexto a WorkSession y previsiblemente no necesita cambios. Workspace reconoce Proyectos por prefijo y RouteLink ya maneja navegación: conservarlos. SessionStart14, api-client y validadores ajenos permanecen intactos salvo necesidad contractual demostrada mediante test.

## Riesgos y oráculos de implementación

- El formulario debe abrir en la URL estable **antes** de transmitir el cierre. Navegar después del éxito no permite recuperar una respuesta perdida al recargar. No almacenar notas/key/recibos en URL ni almacenamiento web.
- Consultar state closed acredita terminalidad, no la confirmación de la intención retenida ni sus notas. Con key en memoria se comprueba recibo por key; sin ella, GETclosure permite mostrar el cierre histórico real y sus notas.
- GETclosure404 por sesión todavía abierta no es ausencia de sesión activa ni habilita un nuevo inicio. Un503 tampoco acredita ausencia o rollback.
- Abortar/descartar GET antiguos al confirmar cierre y antes de propagar401 obsoleto. Un active nuevo puede pertenecer a otra sesión propia; no sustituye el recibo cerrado. Active=null habilita inicio sólo cuando la consulta es válida y se conserva elegibilidad14.
- WorkDate y closeZoneId son atribución persistida. Validar forma/fecha sin exigir equivalencia con TZDB/Intl del cliente; fallback visual UTC no cambia la atribución.
- Notas plaintext hasta2000 puntos de código según contrato, con NUL/surrogates rechazados; preservar espacios/saltos y borrador tras412. Sólo una nueva confirmación manual crea key/revisión nuevas después de consultar estado.
- Guardas busy reales con aria-disabled, anuncio de espera y foco al encabezado cuando desaparece iniciador, respetando otro control. Sin cierre automático, cambios del fin previsto, completar tarea ni funcionalidades17/18.
- Revalidar PAUSE/RESUME y recibos14 históricos después de cerrar. Fixtures de composición por URL: el GET nuevo no debe consumir errores destinados a active, como ocurrió en CI15.

UX30 requiere evidencia posterior por estado/modalidad, no aprobación por este plan. No se presupone nueva dependencia, historial global, segundero ni gestor de estado compartido.
