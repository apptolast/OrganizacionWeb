# TDD frontend: disponibilidad

Contrato aprobado: features/availability.feature, 47 escenarios y 237 casos, sección 10. Leídos Ponytail full, Caveman lite y contrato completo. Init compartido 27065 verde: 798 backend, 647 frontend y lint; no repetido por este autor.

Ownership: UI, hook, shell/ruta privada, SCSS, pruebas UI y perfiles de mutación. API y sus pruebas pertenecen a integration_craftsman, ya liberadas con 147 pruebas verdes y aprobación de raíz.

Ciclos RED/GREEN focales consecutivos: navegación exacta y enlace activo; retorno después del login mediante SessionGate; ausencia confirmada con siete ceros editables; sugerencia perteneciente al catálogo y selección conservada; zona histórica fuera del catálogo; catálogo pendiente sin falsa indisponibilidad. Séptimo ciclo: RED por falta de Guardar disponibilidad; GREEN con PUT/If-Match, bloqueo de campos y confirmación posterior a respuesta válida. Resultado focal: 1 pasada, 6 omitidas por filtro.

Sin mutación antes de juez ni E2E antes de freeze conjunto.

Siguientes ciclos RED/GREEN: presupuestos inválidos (vacío, negativo, exceso y fracción); total completo del borrador sin suma parcial; catálogo fallido o malformado y recuperación conservando valores; GET inicial fallido o sin ETag sin inventar ausencia; PUT 412/503/red/200 inválido con recarga obligatoria; recarga pendiente y fallida que conserva borrador, seguida de GET válido que lo sustituye; errores 400 de día y zona corregibles sin recarga; cancelación de PUT pendiente al navegar. En este último, el RED demostró una segunda consulta de sesión provocada por un 401 tardío; el GREEN transmite AbortSignal y guarda respuesta/catch/finally. El transporte de SessionGate no está sustituido por un mock de sus callbacks.

Corte intermedio de regresión de esta vista: 23/23 verdes; después, el caso nuevo de cancelación pasó focalmente (1 pasada, 23 omitidas por filtro).

Últimos ciclos RED/GREEN: retirar confirmación al editar después de un no-op; encabezado enfocable al entrar; mensaje específico de recarga fallida; restauración de contexto al reintentar; restauración del botón tras pérdida de foco durante PUT, sin desplazar un foco elegido; error de campo que tampoco roba ese foco; anuncio de catálogo pendiente. La pérdida de foco nativa al deshabilitar se modela colocando el foco en body en la prueba unitaria: el navegador real corresponde a integración.

La revisión del coordinador corrigió una interpretación: CSRF_INVALID es rechazo conocido y no exige descartar el borrador. El test ampliado falló al intentar el segundo envío manual; GREEN con isCsrfFailure, comprobación de aborto tras await y conservación de cuerpo/ETag. Recuperar acceso no envía PUT ni recarga preferencias; el siguiente clic usa token renovado. El resto de errores inciertos conserva la recarga obligatoria.

Regresiones adicionales, verdes desde su primera ejecución porque ejercitan mecanismos ya existentes: 401 de preferencias, catálogo y PUT mediante SessionGate; cierre por BroadcastChannel con PUT tardío; GET antiguo en StrictMode tras confirmar PUT; GET 401 tardío tras navegar; variantes inválidas de ruta después del login; cancelar sin PUT y volver con datos confirmados. No se presentan como ciclos RED inventados. No se desactivaron pruebas históricas ni se añadieron dependencias.

Freeze comunicado a raíz: 47 pruebas UI, API externa con 147 pruebas. Primera corrida global: 840/841; el único fallo histórico descubrió una segunda suscripción a navegación en Workspace. Se corrigió pasando desde App la sección activa ya calculada. GREEN afectado: 82/82 en read-projects y availability, conservando la prueba histórica sin cambios. Lint final EXIT 0 y build final EXIT 0. La siguiente corrida global es responsabilidad de raíz; no se infiere 841/841 de resultados parciales.

Mapa del contrato (los escenarios de servidor no se duplican artificialmente en UI):

| Escenarios | Evidencia y responsabilidad |
| --- | --- |
| s1–s8 | DTO, ausencia, catálogo, alias, presupuestos y no-op: API 147; UI ausencia editable, selección y no-op confirmado. Persistencia/versiones: backend. |
| s9–s17 | Fechas, precondiciones, formas y validación HTTP: backend y API. UI localiza errores de día/zona y conserva borrador. |
| s18–s22 | Aislamiento, ausencia de eventos, concurrencia y propietario: backend/integración; no afirmación basada en mocks UI. |
| s23–s27 | Sesión, CSRF, fallos, endpoints cerrados y no-store: API/backend; composición real de SessionGate cubierta en UI. |
| s28 | Reinicio real: integración, pendiente de su evidencia independiente. |
| s29–s30 | Ausencia con siete ceros, sugerencia validada contra catálogo, catálogo pendiente/fallido y reintento sin perder valores. |
| s31 | PUT retenido, bloqueo, doble envío, confirmación válida y no-op. Latencia real: integración. |
| s32–s34 | Errores recuperables, campo corregible, recarga explícita bloqueada mientras espera, borrador intacto al fallar y sustituido sólo con GET válido. |
| s35 | Cancelación a Proyectos sin escritura y sin conservar borrador local. |
| s36–s37 | Retirada privada y respuestas tardías con SessionGate, BroadcastChannel, navegación y montaje real StrictMode. |
| s38 | Validación local del vacío y número incorrecto; entrada incompleta mediante teclado nativo: integración. |
| s39 | Semántica, controles nativos y foco condicional: UI; medidas 44×44, matriz y zoom: integración. |
| s40–s42 | Total del borrador sin tiempo ganado ni suma parcial; GET incoherente recuperable. Defectos exhaustivos del DTO/ETag: API. |
| s43–s44 | Campos bloqueados y respuesta distinta de intención: UI y validación estricta del API. |
| s45–s46 | Zona histórica conservada como no disponible y elección exigida antes de guardar; pertenencia vigente al escribir: backend. |
| s47 | Ruta exacta, enlace activo, retorno tras login, variantes rechazadas, aviso permanente y cancelación fija. |

La UI mantiene snapshot, borrador textual y catálogo separados en un componente local. Reutiliza cliente CSRF, shell y controles SCSS. No crea router, almacenamiento local, calendario, evento ni editor universal. Matriz UX y dispositivos físicos no se declaran comprobados por este autor.

Init conjunto independiente 8318 EXIT 0: 984 backend, 841 frontend y lint global verdes. Raíz aprobó revisión y ejecución focal de Stryker. Antes de arrancar se corrigió únicamente la etiqueta CSRF de @s27 a @s24, sin cambio de comportamiento. Campaña original en sesión 68808: 637 mutantes instrumentados, concurrency 2; no se modifican fuentes ni pruebas durante esa ejecución.

Excepción explícita del coordinador durante la campaña: sólo SCSS, que no está en el alcance instrumentado. Integración midió nav clientWidth 169/scrollWidth 232 y texto fuera de sus enlaces a 701/720/759/760 px. Se alineó únicamente el breakpoint de navegación de 760 a 700, coincidente con sidebar. Prettier check de styles.scss verde. Integración conserva la imagen anterior para la aserción RED formal y comprobará el bundle reconstruido. TS/TSX, pruebas y perfiles permanecen congelados en la campaña original.

Integración entregó el GREEN posterior del CSS: 2/2 casos (matriz de 28 anchos y bordes), 26,9 segundos; nav 169/169 entre 701 y 761, texto sin solape. Firefox/WebKit 2/2 y capturas de escritorio, móvil y zoom real revisadas por integración. Esta evidencia corresponde al bundle con CSS corregido; no se atribuye al snapshot anterior de mutación.

Hallazgo abierto del coordinador que espera al informe original: HTTP 400 con message vacío o sólo espacios se reconoce como error de campo pero no produce mensaje útil. Se añadirá RED/GREEN mínimo y revisión después de conservar la campaña, sin tocar ahora lógica instrumentada.


## Cierre de hallazgos posteriores a mutación

Original conservado: 517/635 evaluables, 81,42 %, con 115 supervivientes, 3 NoCoverage y 2 RuntimeError fuera de detecciones. Véase mutation_availability_frontend.md.

- Mensaje 400 vacío o blanco: dos pruebas RED por ausencia de alerta; guarda trim mínima, GREEN conservando texto válido y borrador. Revisión raíz y navegador real confirman recuperación explícita.
- Enter desde input: Chromium mostró BODY tras éxito/503. Dos casos RED unitarios modelan esa pérdida nativa de foco; el destino externo ya cumplía. Se reutiliza submitFocus para input/select/button. GREEN en cuatro variantes y controles previos. Chromium confirmó éxito, 503, 400 y conservación del enlace elegido durante espera.
- Refuerzos sin más producción: contratos HTTP malformados, ETag de segunda escritura, avisos iniciales y CSRF, ARIA, opciones nativas, tres lecturas de reintento y resultados antiguos StrictMode. Las pruebas de foco distinguen modelo jsdom de medición real.

Corte final UI: 80/80 pruebas, formato y ESLint verdes. Build CpU8JHCd ya validado y comprobado por integración; posteriores cambios sólo de pruebas. Se entrega congelado al coordinador para regresión global y replay focal, sin repetir campaña original ni modificar umbrales.

Regresión final independiente 11298: lint y 875/875 frontend verdes; backend 984 conservado sin cambios (UP-TO-DATE). Replay focal posterior: 115/126, 91,27 %, separado del original 517/635. Raíz aceptó siete variantes de momento/destino de foco; la fuente conserva devolución al origen. Diagnóstico independiente de los dos RuntimeError registrado sin atribuirles Killed. Fuentes y pruebas liberadas; no se marca done desde esta bitácora.
