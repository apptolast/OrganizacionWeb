# Mutación frontend: disponibilidad

Estado: original finalizado con EXIT 0, sesión 68808, duración 17 minutos y 42 segundos. Resultado: **517 Killed / 635 evaluables = 81,42 %**. Se instrumentaron 637: 115 Survived, 3 NoCoverage y 2 RuntimeError, sin Timeout. Los dos RuntimeError no son detecciones y están fuera del denominador del score. Se conserva `frontend/reports/mutation-availability/mutation-original.json` y `stryker-original.log`. Refuerzos y replays cerrados; no se sumarán al porcentaje original.

Dry run: 577 pruebas en 51 segundos, frente a 841 de la suite normal. El runner instalado selecciona archivos relacionados (`related=true`); el JSON conserva los 47 registros UI y 147 API. Sus identidades aparecen en killedBy de 287 y 228 mutantes, respectivamente; estos conjuntos pueden solaparse. Esta evidencia demuestra participación, sin equiparar contadores ni sumar detecciones. Durante la campaña sólo cambió SCSS 760→700 bajo autorización: ningún archivo instrumentado ni prueba.

Los RuntimeError originales 431 y 446 provocaron un fallo de serialización del runner (`Cannot convert object to primitive value`) después de errores del mutante. Se diagnosticarán separadamente, sin convertirlos en Killed ni modificar el manejo global de errores. NoCoverage 333 corresponde a una guarda del encabezado; 356/357 a la recuperación de Intl. Su clasificación conservará explícitamente el estado original.


Alcance de `frontend/stryker.availability.config.json`:

- `src/availability.tsx`, completo: estado local, validación, lectura, guardado, recuperaciones, cancelación, foco y UI.
- `src/availability-api.ts`, completo, autor integration_craftsman: DTO, ETag, catálogo, intención y transporte.
- `src/workspace.tsx:27:0-41:200` y `72:0-72:200`: navegación activa y breadcrumb.
- `src/use-session.ts:187:0-187:200`: aceptación exacta de la ruta de disponibilidad.
- `src/App.tsx:11:0-11:200` y `14:0-16:200`: selección de ruta y composición con shell.

Umbral 80, concurrency 2, coverageAnalysis perTest y runner Vitest existente. No exclusiones nuevas de lógica, cambios de timeout ni dependencia adicional. El perfil global conserva módulos históricos y agrega disponibilidad/API/navegación; el rango histórico App se ajusta a 10–18 por los cambios de líneas. No se modifica scripts/project.mjs.

Puerta de pruebas del autor: lint y build finales verdes, 82/82 focales después de corregir la única regresión de la primera corrida 840/841. Init independiente de raíz 8318 EXIT 0: 984 backend y 841/841 frontend, lint global verde. API: 147 pruebas entregadas y revisión independiente en `review_availability_api.md`. Revisión frontend aprobada en `review_availability_frontend.md`, sección de implementación. Los resultados se registrarán por ejecución sin sumar porcentajes de replays.


## Refuerzos posteriores al original

Revisión independiente UI: `review_availability_ui_mutants.md`; API: `review_availability_api.md`. API 31 fue detectado en replay separado 3/3, realizado por integración. Las otras seis equivalencias API no se presentan como detecciones.

La revisión descubrió dos defectos funcionales, corregidos por RED/GREEN y aprobados por raíz: mensajes 400 vacíos/blancos dejan ahora una alerta recuperable; Enter conserva como origen el control nativo que envía. La prueba de foco modela BODY en jsdom porque éste conserva foco al deshabilitar un input, mientras Chromium real lo pierde. Integración confirmó ambos arreglos en 2/2 casos sobre bundle CpU8JHCd, incluyendo foco externo elegido; CSS permanece intacto.

Refuerzos por comportamientos: parsing HTTP, segundo ETag, sugerencia/avisos, errores asociados, opciones de zona, cancelación StrictMode, reintentos repetidos, navegación y foco. Corte congelado: 80/80 pruebas UI, Prettier y ESLint verdes. La regresión global final queda a cargo de raíz. Fuente adicional autorizada para replay: guarda de mensaje y captura/restauración de origen del submit.


## Revisión del alcance público

Los IDs originales 417–422 son guardas defensivas redundantes bajo el montaje actual: el formulario sólo existe con snapshot y draft; los campos y Guardar quedan inhabilitados durante PUT/recarga de preferencias, y Guardar sigue inhabilitado cuando el resultado exige recarga. La reentrada requeriría despachar submit artificialmente a un formulario bloqueado. No se añade ese evento imposible como supuesto defecto de producto. Las pruebas y Chromium verifican el bloqueo y la recuperación manual. Las guardas permanecen en producción y en el perfil global.

Las cinco guardas PUT 456/464/466/480/536 sólo continúan tras desmontar la instancia; sus setters no tienen efectos externos, e isCsrfFailure sólo clasifica. Esto no se extiende a GET/catálogo: 372/380/402/408 tienen recorridos StrictMode alcanzables y ahora pruebas explícitas de éxito/rechazo antiguo. La premisa anterior de que apiRequest suprimía Response abortado era incorrecta y se rectifica en la revisión independiente.

Los ocho cambios decorativos 660/661/662/663/664/666/667/668 son variantes visuales permitidas por el coordinador, no equivalencias DOM. Los enlaces mantienen texto, destino, aria-current y borde activo. No se persigue 100 % a costa de pruebas de decoración.

Replay focal terminado, sesión 90278: 126 mutantes en availability.tsx y workspace.tsx, 36 rangos. `frontend/reports/mutation-availability/replay-targets.json` conserva 64 identidades originales con localización, operador y reemplazo, más correspondencia de líneas actuales. La guarda trim y captura de foco nuevas están incluidas. Los RuntimeError 431/446 quedan en diagnóstico independiente coordinado; no se retiran del alcance original/global.

Regresión final independiente: init 11298 EXIT 0, 875/875 frontend en 19 archivos y lint verde. Backend se conserva en 984, sin cambios y con tareas UP-TO-DATE. No se atribuye una segunda ejecución efectiva de sus pruebas.


Diagnóstico independiente RuntimeError cerrado por raíz: `review_availability_runtime_mutants.md` y evidencia JSON. En copias aisladas, baseline Vitest normal 6 pruebas EXIT 0; variante 431 EXIT 1 por dos rechazos asíncronos de undefined.includes, y 446 EXIT 1 por cuatro SyntaxError del selector vacío. Las aserciones individuales pasan; el proceso falla por errores no manejados. Esto demuestra el efecto dañino sin renombrar los RuntimeError originales como Killed ni generar otro score. Hash de fuente viva idéntico, sin cambios del manejo global de errores.


## Resultado del replay focal

Sesión 90278 EXIT 0, 4 minutos y 37 segundos: **115/126 Killed = 91,27 %**, 11 Survived, cero NoCoverage/RuntimeError/Timeout. Dry run relacionado: 463 pruebas. Este porcentaje pertenece sólo a 36 rangos de dos archivos, no es una nueva campaña global ni sustituye 517/635. Perfil: frontend/stryker.availability-replay.config.json; raw: reports/mutation-availability/replay.json; log: stryker-replay.log.

El mapa replay-targets.json empareja 64 identidades por archivo, líneas ajustadas, columnas, mutador y reemplazo: 57 ahora Killed. Los siete objetivos restantes son 278→0, 325→5, 326→6, 344→15, 345→16, 346→17 y 347→18. Cambian el momento/destino de foco cuando BODY está activo; mantienen el respeto al foco externo. Raíz aceptó estas variantes contractuales, separadas de equivalencia estricta.

Otros cuatro supervivientes del replay: 10 elimina optional chaining de h1 montado; 107/133 decrementan contadores usados únicamente como disparadores; 43 captura cualquier activeElement en vez de restringirlo a los tres tipos nativos que pueden originar el submit público. No se inventa una llamada privada para originarlo desde un elemento inexistente en el formulario.

Balance final de identidades originales: 58 detectadas posteriormente (57 UI + 1 API en ejecución separada), 2 RuntimeError diagnosticados fuera de Stryker, 45 equivalencias y 15 variantes permitidas. Las 45 equivalencias incluyen los tres NoCoverage originales, que siguen sin contarse como detectados. IDs EQ: 80,103,148,151,152,174,269,281,282,287,290,306,311,312,314,316,317,333,337,352,356,357,397,398,399,417,418,419,420,421,422,423,424,445,456,459,464,466,478,480,536,544,564,636,655. Razones por grupos en las dos revisiones independientes y la sección de alcance público anterior.

El directorio temporal propio .stryker-tmp-availability-replay permanece sin seguimiento: la herramienta rechazó su borrado automáticamente. No es un entregable ni debe agregarse al commit. Los perfiles y reportes finales no dependen de ese directorio.

## Dictamen final y limitación operativa

Raíz acepta las siete variantes de foco bajo s39: encabezado y control son destinos significativos si se preservan foco externo y error asociado. Los siete casos son variantes permitidas, no equivalencias ni detecciones. La fuente conserva la devolución al origen nativo, demostrada en Chromium. Queda resuelta la propuesta pendiente del apartado anterior: 58 identidades originales detectadas posteriormente, 2 diagnosticadas por error asíncrono, 45 equivalencias y 15 variantes permitidas; no se suman scores.

Ruta del temporal bloqueado: C:/Users/vhurt/Documents/Codex/2026-09-05/a-ver-necesito-o-me-gustar/work/OrganizacionWeb/frontend/.stryker-tmp-availability-replay. El comando solicitado fue `Remove-Item -LiteralPath $replayScratch -Recurse -Force`, precedido de GetFullPath y comparación exacta con esa ruta. Rechazo previo a ejecución: «rejected: blocked by policy». No se borró un ancestro ni se intentó otra herramienta. El coordinador informó al usuario. También se rechazó un comando Python que intentaba registrar ese texto; este registro usa edición documental, sin ejecutar el borrado.

Fuentes, pruebas y perfiles liberados al coordinador. Este informe no marca done ni modifica metadatos SDD. Ponytail full y Caveman lite aplicadas, con documentación legible y sin nuevas dependencias.
