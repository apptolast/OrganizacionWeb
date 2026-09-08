# Historial de sesiones

> Bitácora **append-only**. Al cerrar cada sesión, añade aquí el resumen que
> estaba en `current.md` (feature, fases recorridas, veredictos, resultado).

<!-- Ejemplo de entrada:
## 2026-01-01 — feature `ejemplo_feature`
- spec_partner: decisiones cerradas (ver project-spec.md).
- gherkin_author: features/ejemplo_feature.feature (@s1..@s5), aprobado por el humano.
- tdd_craftsman: 5 ciclos Rojo-Verde-Refactor. Tests verdes.
- judge: APPROVED (ver progress/judge_ejemplo_feature.md).
- mutation_tester: score 0.92 > 0.80 (ver progress/mutation_ejemplo_feature.md).
- Resultado: done.
-->

## 2026-09-05 — feature `create_project`

- Inicio: repositorio clonado e init correcto; contrato preparado por spec_partner/gherkin_author (28 escenarios, 58 casos). Se consultó el artefacto de arquitectura en Chrome y los repositorios de infraestructura en lectura. Se incorporaron las skills React/Web Design Guidelines de Vercel. No se implementó producción antes de la aprobación.
- Puerta humana: el usuario respondió «Por supuesto» tras recibir los escenarios y autorizó commits/push a apptolast/OrganizacionWeb. Se mantuvo una sola feature de implementación en progreso.
- Implementación: React/TypeScript/pnpm/SCSS sin Tailwind, Spring Boot/Java/Gradle Kotlin DSL, puertos de entrada/salida y dominio puro. POST autenticado crea proyecto y ProjectCreated.v1 pendiente en una única transacción PostgreSQL. HTTP Basic bootstrap sin credenciales predeterminadas y validación estricta de JSON/origen.
- TDD: ciclos y mapa @s1–@s28 en progress/tdd_create_project.md y bitácoras por frontera. Pruebas reales detectaron y corrigieron rollback, precisión temporal, foco de teclado y documentos JSON concatenados. Las regresiones inicialmente verdes se documentaron sin inventar rojos.
- Verificación final local: node .harness/harness.mjs verify, exit0. Backend65 tests, frontend38 tests y8 E2E verdes; lint, formato y builds verdes. PostgreSQL real mediante Testcontainers y stack Compose aislado; recarga y reinicio conservan registros exactos.
- Judge: APPROVED en progress/judge_create_project.md, incluida revisión independiente del núcleo y revisión raíz del tooling de integración.
- Mutación: PIT36/36 (100%) y Stryker143/148 (96,62%), umbral80% superado en ambas suites. FRECORD desactivado para incluir validación manual de records. Los cinco supervivientes frontend conservados en el denominador y justificados como equivalentes en sus consumidores actuales; no hay falta de cobertura ni timeouts. Ver progress/mutation_create_project.md.
- Resultado: create_project completada y autorizada para cierre por el coordinador tras verificación/revisión. El software completo del roadmap no se declara terminado.
- Operación: no desplegado en servidor. GitHubCI pendiente de push/ejecución remota por el coordinador; los resultados anteriores son locales. Infraestructura productiva, dominio, secretos y backups requieren integración posterior.
- Continuidad: publish_outbox es el siguiente contrato propuesto, pendiente de su propia aprobación humana. No se implementó el publicador RabbitMQ ni las demás features del roadmap.
- Verificación remota posterior al push: Application CI completado correctamente en Linux para 38f4fed328caf469085f3e4667edece5736ac9cb, run33989815530 (6m43s): instalación, lint/tests/mutación, build y E2E. https://github.com/apptolast/OrganizacionWeb/actions/runs/33989815530. El commit posterior solo registra este resultado documental y no cambia código/configuración/tests.

## 2026-09-05 — referencia UI/UX incorporada

- Usuario exige Laws of UX y responsive para móvil/tablet/ordenador. Catálogo español revisado (30 principios) y matices Miller/Postel/Doherty/Parkinson consultados.
- Añadido docs/ux-requirements.md con matriz completa, criterios observables, cobertura previa y pendientes explícitos; enlazado desde especificación y mapa de agentes.
- Cambio documental; no altera producción, escenarios aprobados ni estado de publish_outbox. No se afirma que la interfaz actual ya supere la matriz ampliada.

## 2026-09-05 — monorepo confirmado

- Usuario confirma API y web dentro del mismo repositorio, separadas por carpetas. Verificada estructura existente backend/ y frontend/, comandos raíz y builds independientes; documentado explícitamente en arquitectura y especificación.
- Sin cambios funcionales ni movimientos de código. La aclaración no cambia el estado del contrato publish_outbox.

## 2026-09-05 — feature `publish_outbox`

- Puerta humana: contrato de 23 escenarios y 36 casos aprobado explícitamente con «Sí la apruebo… continúa». Init correcto antes de producción; una sola feature implementada a la vez.
- Implementación: dominio/aplicación puros, puertos de entrada/salida, publicador RabbitMQ con confirms y mandatory, transacción PostgreSQL por reclamación con SKIP LOCKED, migración aditiva, reintentos acotados, aislamiento de eventos inválidos y auditoría sin datos privados. Deshabilitado por defecto; creación de proyectos independiente del broker.
- TDD: ciclos reales RED/GREEN y regresiones identificadas como tales en las bitácoras por frontera. PostgreSQL y Rabbit reales; pruebas de caída matan un proceso Java propio antes/después de aceptación, verifican liberación de reclamación e identidad de una/dos copias. Trigger PostgreSQL comprueba rollback posterior a aceptación real.
- Verificación local final del coordinador 6887: exit 0. Lint, builds, 147 tests backend y 38 frontend correctos. E2E 49506: ocho pruebas base y tres etapas de smoke del publicador verdes, incluidas caída/recuperación y persistencia tras reiniciar Rabbit con su volumen.
- Mutación: PIT 90/90 (100 %: 54 mutantes nuevos y 36 previos), sin supervivientes ni falta de cobertura. Stryker 143/148 (96,62 %), cinco supervivientes del baseline anterior sin cambios frontend. Cuatro mutantes semánticos adicionales del adaptador Rabbit detectados en copia aislada. Alcances y exclusiones explícitos en progress/mutation_publish_outbox.md.
- Juez: APPROVED en progress/judge_publish_outbox.md; tooling revisado por el coordinador en progress/judge_publish_outbox_tooling.md.
- Verificación remota: Application CI SUCCESS para código `1a3737758c655462fc3814f6af8d0f87138eb1a8`, run `33993262637`, incluidos verify, build, E2E y publisher smoke. [Ejecución GitHub Actions](https://github.com/apptolast/OrganizacionWeb/actions/runs/33993262637).
- Resultado: feature 2 done tras señal expresa de cierre del coordinador posterior al CI verde. No desplegada en servidor. El roadmap completo no se declara terminado y la entrega es al menos una vez, con duplicados de identidad estable posibles.
- Continuidad: feature 3 read_projects en spec_ready, contrato de 32 escenarios / 50 casos validado y matriz completa de 30 principios UX con verificaciones pendientes. Requiere su propia aprobación humana; no se ha implementado. Resumen de revisión en outputs/Consultar-proyectos.md del workspace.

## 2026-09-06 — feature `read_projects`

- Autorización persistente del usuario: «Si las apruebo todas». Se mantuvo el contrato propio de 32 escenarios / 50 casos y una sola implementación activa.
- Backend: lista privada de 20 proyectos por cursor estable, detalle propio, respuestas sin caché, errores uniformes y ninguna escritura al consultar. Índice aditivo por propietario/fecha/id. Una revisión detectó fechas de cursor fuera del rango PostgreSQL; corrección comprobada con fronteras HTTP reales.
- Frontend: lista/detalle persistentes, navegación por URL, espera/errores recuperables, texto literal y protección frente a respuestas obsoletas. React/pnpm/SCSS y arquitectura hexagonal conservados.
- Verificación raíz 91741: exit 0, 190 tests backend y 73 frontend, lint correcto. Se añadieron después cinco casos frontend sin cambios de producción: 40 pruebas focalizadas de lectura y lint final verdes; 78 casos declarados en total, sin atribuir una segunda ejecución global.
- Integración: 14 E2E Chromium, dos recorridos Firefox/WebKit, 12 anchos, teclado/axe y zoom nativo al 200 % con reflow de 320 px. Capturas corregidas inspeccionadas por el coordinador. Pendientes explícitos de dispositivos físicos y evaluación humana, sin certificación global de UX/accesibilidad.
- Mutación: PIT 103/103 (13 nuevos y 90 previos), sin supervivientes. Stryker global 276/297 (92,93 %); 21 supervivientes iniciales revisados. Replay selectivo 17/17 detecta seis huecos observables tras reforzar pruebas; 15 equivalentes justificados. No se suman denominadores ni se elimina código del alcance.
- Jueces de backend y frontend APPROVED; integración revisada independientemente por el coordinador. Ver progress/judge_read_projects_backend.md, progress/judge_read_projects_frontend.md y progress/judge_read_projects_integration.md.
- Resultado: feature 3 done localmente. Código publicado como `24b1e50ad000fe6fbc96fef5809c12f82d552854`; [Application CI 33995196185](https://github.com/apptolast/OrganizacionWeb/actions/runs/33995196185) todavía en curso en la última consulta. No se declara éxito remoto ni despliegue en servidor.
- Continuidad: edit_project tiene contrato aprobado bajo la autorización global y queda spec_ready. No requiere otra aprobación humana. Producción de feature 4 todavía sin iniciar al cerrar esta entrada.
- Eficiencia: Ponytail full y Caveman lite leídos y activos; no se omiten requisitos de arquitectura, TDD, seguridad ni accesibilidad.

## 2026-09-06 — feature `edit_project`

- Autorización global persistente; contrato aprobado antes de producción, TDD por ciclos y una sola feature activa. Ponytail full y Caveman lite activos, subordinados a arquitectura, seguridad, accesibilidad y pruebas exigidas.
- Backend: GET detalle y ETag comparten un snapshot SQL. PUT propio valida una precondición fuerte exacta; comprueba versión y no-op dentro de la transacción. Actualización de proyecto e inserción de ProjectUpdated.v1 atómicas, ambas con comprobación de una fila. Migración V4 aditiva y ruta RabbitMQ Updated cerrada, sin cambiar Created.
- Frontend: formulario de edición con borrador, validación, cancelación, conflicto y recarga deliberada. Conserva literalidad, accesibilidad, foco y protección frente a respuestas obsoletas; no guarda borradores ni credenciales en almacenamiento persistente.
- Verificación raíz 8183: salida 0, lint, 240 pruebas backend y 122 frontend verdes, sin fallos, errores ni omitidos backend. PIT 125/125, líneas 150/150. Stryker completo 209/255 (81,96 %); replays 36/42 y 3/3 documentados por separado, sin sumar sus denominadores al resultado global. Huecos observables reforzados y equivalencias justificadas.
- Integración final: 18/18 E2E, 22 anchos, teclado/táctil, Firefox/WebKit y zoom nativo al 200 %. Smoke con worker activo y RabbitMQ detenido confirma PUT 200, evento pendiente y publicación original tras recuperar el broker. El coordinador revisó su fuente y resultado. La evidencia no certifica dispositivos físicos ni evaluación humana.
- Juez conjunto APPROVED en progress/judge_edit_project.md y revisión backend independiente APPROVED. Informes TDD, mutación y UX enlazados desde los informes del corte.
- Resultado: feature 4 done localmente por señal del coordinador. CI de edición pendiente de commit y ejecución al registrar este cierre; no se afirma éxito remoto ni despliegue en servidor. El MVP completo sigue en desarrollo.
- Continuidad: feature 5 project_states queda spec_ready, con contrato preparado y revisado dentro de la autorización global. No requiere nueva aprobación humana y no se ha iniciado producción en este cierre.

## 2026-09-06 — feature `project_states`

- Contrato aprobado antes de producción bajo la autorización global. Ponytail full y Caveman lite activos; se conservaron arquitectura hexagonal, TDD, seguridad y accesibilidad.
- Backend: cuatro estados, tabla cerrada de transiciones y no-op vigente; comparte versión/ETag con edición. Capacidad propia configurable entre 1 y 10, predeterminada 3. Bloqueo asesor transaccional global antes de fila/conteo bajo READ_COMMITTED, con su límite de concurrencia documentado. Estado y ProjectStatusChanged.v1 de ocho campos se confirman atómicamente; rutas anteriores intactas.
- Evidencia PostgreSQL/RabbitMQ real: última plaza concurrente con un único éxito, aislamiento por propietario, liberación de plaza, reducción sin pausas automáticas, rollback de errores y escrituras suprimidas, conflictos entre texto y estado, publicación del evento original y recuperación con broker detenido.
- Regresión raíz 51375: salida 0, lint, 328 pruebas backend sin fallos, errores ni omitidos y 171 frontend verdes. Tras los refuerzos de mutación, el autor confirmó la suite frontend final de 176 y lint verdes. No se atribuye una repetición backend innecesaria.
- Mutación: PIT 163/163 eliminados; 205/206 líneas, únicamente el constructor privado vacío de ProjectStates sin recorrer. Stryker global 284/312 (91,03 %), replay selectivo 14/14 y 22 equivalencias justificadas. Denominadores separados, sin puntuación global inventada.
- Integración final: 22 E2E, dos recorridos adicionales Firefox/WebKit y smoke con salida 0; fixture aislado limpiado. Evidencia responsive, zoom y matriz UX documentada con límites explícitos. No se certifican dispositivos físicos ni evaluación humana universal.
- Juez conjunto APPROVED en progress/judge_project_states.md. Resultado: feature 5 done localmente por señal del coordinador. Commit/push y CI de esta entrega pendientes al registrar el cierre; no se declara despliegue en el servidor ni finalización del MVP.
- Continuidad: feature 6 authentication queda spec_ready con contrato aprobado en febc9d1. La propuesta contrasta APIs y esquema de Spring Security 6.5.8 / Spring Session JDBC 3.5.5, sin producción iniciada. Espera activación del coordinador posterior al commit/push de estados.

## 2026-09-06 — feature `authentication`

- Contrato aprobado bajo la autorización global, TDD y arquitectura conservados. Ponytail full y Caveman lite activos.
- Spring Security y Spring Session JDBC sustituyen Basic por formulario, cookie de sesión y CSRF. Login rota sesión/token; logout elimina la sesión. Cookies HttpOnly/Lax con Path /api y Secure en HTTPS. HTTP sólo se admite en loopback según origen configurado.
- HTTP real con PostgreSQL demuestra fallos de guardado de login y eliminación de logout: respuesta 503 SESSION_UNAVAILABLE, sin éxito ficticio ni cookie provisional. El logout fallido conserva la cookie para reintentar. La lectura inaccesible no se representa como anonimato confirmado.
- Verificación raíz: 384 pruebas backend y 241 frontend, lint verdes. Refuerzos posteriores del frontend: suite final de 260 pruebas y lint verdes. Las API históricas conservan sus contratos usando sesiones y CSRF; no se atribuye una repetición backend tras cambios exclusivos de pruebas frontend.
- Mutación pertinente: PIT de cuatro adaptadores propios 41/44 (93,18 %), tres equivalencias aceptadas independientemente por coincidir con defaults oficiales del serializador. El scope predeterminado incluye esos adaptadores. Stryker global 302/355, replays 79/79 y 1/1 verificados como informes independientes; no se suman denominadores.
- Integración final: 27/27 E2E y publisher con salida 0. Pruebas de navegador, persistencia, expiración, CSRF, origen y evidencia UX constan en los informes; no se afirma cobertura universal de dispositivos físicos.
- Juez conjunto APPROVED en progress/judge_authentication.md. Authentication queda done localmente por señal del coordinador. Commit y CI de esta entrega pendientes al cerrar; no se declara despliegue en servidor ni MVP completo.
- Continuidad: create_task conserva sólo propuesta y borrador de contrato revisado, sin activación ni producción. El coordinador determina el siguiente inicio dentro de la autorización persistente.

### Confirmación remota de authentication

El coordinador confirmó CI 34001003734 SUCCESS sobre 0913d758e0225efbeb0c32e6ee63f9915950bcb8, incluidos verify/mutación, build, E2E y publisher. Esta confirmación completa el estado remoto pendiente del cierre local de feature 6; no implica despliegue en servidor.

## 2026-09-06 — feature `create_task`

- Contrato aprobado de 35 escenarios bajo autorización persistente. Ponytail full y Caveman lite activos; arquitectura hexagonal, TDD, seguridad y accesibilidad conservados.
- Backend: tareas hijas del proyecto con ocho campos, validación Unicode/estimación y lecturas propias paginadas por cursor vinculado al proyecto. V7 aditiva y FK de outbox conservada. La creación bloquea la fila del proyecto y confirma tarea y TaskCreated.v1 juntos, sin cambiar ETag ni capacidad.
- PostgreSQL real verifica privacidad, UUID/cursor estrictos, rollback de errores y escrituras suprimidas, y carrera con completar en ambos órdenes. RabbitMQ real verifica el evento original y ruta nueva; entrega al menos una vez conservada.
- Frontend: formulario y lista independientes en el detalle, recarga persistente, errores con borrador conservado, sesión y respuestas obsoletas protegidas. Una carrera entre POST retenido y reintento GET se reprodujo y corrigió con actualización funcional; no se ocultó mediante una relajación de pruebas.
- Init final 73511: 486 pruebas backend, 366 frontend y lint verdes. Tras refuerzos exclusivos de pruebas frontend, suite final 371 y lint verdes. No se atribuye una nueva ejecución global conjunta posterior.
- Mutación backend: perfil completo 182/186, cero timeouts y NO_COVERAGE; replay separado 15/15 elimina tres huecos reales y deja una equivalencia de normalización justificada. Se corrigió lifecycle del fixture PostgreSQL y se midió margen de arranque Rabbit, sin tratar fallos del entorno como mutantes eliminados.
- Mutación frontend: campaña inicial 402/504; después de la corrección funcional se ejecutaron los 505 mutantes actuales, con 480 eliminados (95,05 %). Replay separado 16/16 confirma las cuatro identidades reforzadas; 21 variantes justificadas y ningún hueco real abierto según revisión independiente. No se suman denominadores ni se afirma reutilización incremental inexistente.
- Integración: 32/32 E2E originales, 2/2 Firefox/WebKit, 22 anchos y zoom nativo, feedback medido en 2 ms y smoke de caída/recuperación/retención TaskCreated tras reiniciar broker con salida 0. La corrección funcional posterior tiene regresión real focal 1/1, conservada por separado. No se certifican dispositivos físicos ni usabilidad universal.
- Dictamen final APPROVED en progress/judge_create_task.md. Feature 7 queda done localmente; commit/push y CI de esta entrega pendientes al registrar el cierre. No se declara despliegue en servidor ni finalización del MVP.
- Limpieza: la revisión automática rechazó eliminar .e2e-work/read-review-state.json y .e2e-work/read-review-stop con el motivo literal «blocked by policy». Permanecen ignorados por Git; no se expuso su contenido ni se eludió el bloqueo. Es una limitación de limpieza, no de funcionamiento de la aplicación.
- Continuidad: split_task conserva propuesta y borrador @draft revisado de 38 escenarios. No se activa feature 8 ni se inicia producción durante este cierre.

## 2026-09-06 — feature `split_task`

- Contrato aprobado de 38 escenarios y 82 casos locales, además de todas las filas heredadas en los endpoints nuevos. Ponytail full y Caveman lite activos; se conservaron arquitectura hexagonal, TDD, seguridad y accesibilidad.
- Backend: relación tasks.parent_id restringida al mismo proyecto por FK compuesta, sin movimientos ni cascadas. Creación confirma tarea, relación y un único SubtaskCreated.v1; padre y proyecto conservan estado, fechas y versión. Las consultas devuelven padre confirmado o hijos directos paginados y mantienen DTO8. Colección plana y cuatro tipos históricos conservan sus contratos.
- PostgreSQL real verifica integridad, privacidad, sesión JDBC vencida, no-store, rollback por error o cero filas en ambos registros y carrera con completar el proyecto. RabbitMQ real verifica JSON original, ruta cerrada y persistencia.
- Suite del alcance 29329: 370 pruebas verdes. Init independiente del coordinador 9396: 622 backend y 462 frontend, cero fallos/errores/omisiones backend y lint global verde. Tras refuerzos exclusivos de pruebas frontend, el coordinador repitió la suite final 475/475; no se atribuye una nueva ejecución conjunta 622/475.
- PIT 76051: 235/236 (99,58 %), un superviviente equivalente de normalización, cero timeouts y NO_COVERAGE. XML y equivalencia revisados independientemente; no hizo falta replay backend.
- Stryker original: 558/601 (92,85 %), 41 supervivientes y dos NoCoverage. Replay separado: 56/58 (96,55 %); el coordinador emparejó y comprobó Killed para las 24 identidades originales objetivo, incluidas ambas NoCoverage. Restan 12 equivalencias limitadas a usos actuales y siete variantes permitidas, revisadas sin presentarlas como equivalencias estrictas. No se suman denominadores y no queda un hueco contractual abierto.
- E2E de la imagen final: 37/38; el único fallo histórico se corrigió en la prueba de Tab/foco sin cambiar producción. Replay separado 1/1 en 17,5 segundos; no se declara una campaña agregada 38/38. Firefox/WebKit: 2/2 del recorrido jerárquico.
- Smoke 32635 EXIT 0: cinco rutas, confirmación con broker detenido, recuperación del evento original y retención tras reiniciar RabbitMQ con backend detenido. Fixture nuevo limpiado. UX: 22 anchos, controles 44 por 44, teclado/axe, zoom nativo 200 % con interior 320 y feedback de 1 ms. Las capturas y medidas fueron revisadas por el coordinador; no se certifican dispositivos físicos ni lector de pantalla real.
- Dictamen final APPROVED en progress/judge_split_task.md. Feature 8 queda done localmente por señal del coordinador; commit/push y CI de esta entrega pendientes al registrar este cierre. No se declara despliegue en servidor ni finalización del MVP.
- Create_task ya tiene CI 34004667683 SUCCESS sobre db4d20bf88d8f8285c92dc1f1708f94a854382e3. Ese resultado no se atribuye a split_task.
- Limpieza heredada: la revisión automática rechazó eliminar .e2e-work/read-review-state.json y .e2e-work/read-review-stop con «blocked by policy». Permanecen ignorados, sin exponer contenido ni eludir el bloqueo; no se afirma limpieza completa.
- Continuidad: feature 9 complete_reopen_task permanece pending y sólo tiene preparación documental. Se revisaron ETag propio, historia duradera, cursor y elección de bloqueo; no se inicia producción ni se modifica el contrato aprobado durante este cierre.

## 2026-09-06 — feature `complete_reopen_task`

- Contrato aprobado d65bba5: 36 escenarios, 137 casos locales y variantes referidas. Ponytail full y Caveman lite activos; arquitectura hexagonal, SDD/TDD y seguridad conservados.
- Backend: pending/completed con revisión propia y DTO8 compatible; GET/PUT de estado devuelve snapshot de tres campos y ETag de la misma lectura SQL. La comparación de versión precede al no-op. V9 confirma tarea, historia duradera y TaskStatusChanged.v1 atómicamente; la historia no depende de retención del outbox.
- PostgreSQL real verifica seis fallos/supresiones de escritura, privacidad, sesión JDBC expirada, no-store, reloj igual/anterior y las dos carreras crear hijo/completar padre con FOR NO KEY UPDATE OF t. Dos PUT con el mismo ETag producen 200/412 y una transición. Historial paginado por versión con cursor estricto de proyecto/tarea; las cuatro vistas conservan DTO8.
- Init independiente 58990: 798 backend, 625 frontend y lint verdes. Después, cuatro fixtures históricas ajustadas para ciclo de vida PIT se verificaron con 163 pruebas y formato, sin cambiar producción. Suite frontend final del coordinador: 646/646; compatibilidad focal posterior: 7/7, con un caso añadido después. No se atribuye una nueva suite global conjunta 798/647.
- PIT original 11298: 270/270 KILLED, cero supervivientes, timeouts o NO_COVERAGE. XML revisado independientemente; sin replay backend. Incluye lógica compartida y nueva, ApiErrors completo y los adaptadores propios; no se atribuye todo el denominador a líneas nuevas.
- Stryker original: 415 Killed, un Timeout y 49 Survived; 416/465 (89,46 %). Replays separados: 83/89, 9/10 y 8/8. El timeout original resultó superviviente al repetir y motivó una aserción DTO8 adicional; replay final lo elimina. El coordinador emparejó las 26 brechas resueltas y revisó 16 equivalentes y ocho variantes restantes. No se suman denominadores ni se afirma una nueva campaña global 100 %.
- Integración E2E 83167: 43/43 Chromium. Firefox/WebKit 94713: 2/2 en recorrido acotado. Smoke 88526 EXIT 0: seis rutas, guardado con broker caído, recuperación del evento original, retención tras reiniciar broker con backend detenido y misma sesión recuperando snapshot/ETag/historia.
- UX: treinta principios, 22 anchos, controles de 44 px, axe, feedback de 4 ms y zoom nativo 200 % con interior 320 CSS. Capturas finales reconstruidas tras corregir espacio antes de UTC; sin certificación universal, dispositivos físicos ni lector de pantalla real.
- Dictamen final APPROVED en progress/judge_complete_reopen_task.md. Feature 9 queda done localmente por señal del coordinador. Commit/push y CI de esta entrega pendientes al registrar cierre; no hay despliegue en servidor ni MVP completo.
- CI de feature 8, split_task/3675c36, completada SUCCESS en run 34007601179. Ese resultado no se atribuye al corte 9.
- Limpieza heredada: .e2e-work/read-review-state.json y .e2e-work/read-review-stop siguen ignorados porque revisión automática rechazó su eliminación con «blocked by policy». No se expuso contenido ni se eludió el bloqueo. Los fixtures nuevos limpiaron sus recursos propios.
- Continuidad: disponibilidad (10) permanece pending, con propuesta y borrador sólo documentales. No se inicia producción 10 en este cierre. Backend, Gradle y metadatos liberados para el coordinador.

## 2026-09-06 — feature `availability`

- Contrato aprobado 3f9a293: 47 escenarios y 237 casos. Dictamen final APPROVED para cierre local en progress/judge_availability.md; feature 10 queda done por señal del coordinador. Ponytail full y Caveman lite activos, con arquitectura hexagonal, TDD y React/SCSS conservados.
- Preferencia personal con zona, siete presupuestos diarios y revisión propia persistida en PostgreSQL. Ausencia explícita, catálogo nativo, transacción, no-op vigente, concurrencia de primera escritura y cambios, y recuperación deliberada del borrador. Sin reservas, trabajo acreditado ni modificaciones de proyectos/tareas/outbox.
- Init 8318: 984 pruebas backend sin fallos, errores u omisiones, 841 frontend y lint verdes. Init final 11298: 875/875 frontend en 19 archivos y lint verdes; backend UP-TO-DATE sobre el corte anterior, sin atribuir una segunda ejecución física.
- PIT original 57648: 130/130 KILLED, cero supervivientes, errores o timeouts. XML revisado independientemente; sin replay backend. El desglose identifica handlers históricos de ApiErrors y no atribuye todo el denominador a lógica nueva.
- Stryker original: 517 Killed, 115 Survived, tres NoCoverage y dos RuntimeError; 81,42 % sobre 635 puntuables, EXIT 0. Replay UI separado: 115/126 Killed (91,27 %), sin errores/timeouts; 64 objetivos originales emparejados, 57 Killed y siete variantes de foco permitidas. Replay API separado: 3/3 Killed, detectando la identidad original 31.
- Las 120 identidades originales no eliminadas quedan explicadas: 58 detectadas posteriormente, 45 equivalencias, 15 variantes permitidas y dos errores diagnosticados con Vitest normal en copias aisladas. Los dos últimos terminan con exit 1 por excepciones no controladas aunque sus aserciones pasan; no se contabilizan como Killed. No se mezclan campañas para fabricar un nuevo porcentaje global.
- Integración original 48/48; corrección de solape entre anchos 701 y 760 contrastada por matriz/navegación 2/2. Matriz de 28 anchos, controles 44 por 44, axe sin violaciones en reglas ejecutadas, zoom nativo 200 % con interior 320 CSS y revisión visual raíz. Los treinta principios UX conservan límites humanos y no equivalen a certificación universal.
- Correcciones posteriores de mensaje HTTP 400 y foco al guardar con Enter verificadas por TDD/revisión. Bundle final CpU8JHCd y CSS Codz1mIb: dos recorridos focales Chromium. Enter también pasa Firefox 1/1 y WebKit 1/1 con éxito, 503, 400 y foco externo con Shift+Tab. No se extrapola la matriz completa a esos motores ni se suman ejecuciones distintas.
- Persistencia tras reinicio, concurrencia y separación de outbox confirmadas. No quedan hallazgos de producto abiertos; siguen fuera de la evidencia dispositivos físicos, teclado virtual, lector de pantalla real y evaluación de usabilidad por personas.
- CI de feature 9 e1afc11, run 34010190766, terminó por timeout durante Stryker y no está verde. Commit local 704ff0f amplía el techo a 120 minutos sin bajar controles; siguiente CI pendiente. Commit/push del cierre 10 corresponde al coordinador. Sin despliegue productivo y sin declarar el MVP completo.
- Se conservan temporales ignorados cuya limpieza fue rechazada automáticamente, incluidos .e2e-work/read-review-state.json y .e2e-work/read-review-stop. También consta el rechazo de la propuesta auxiliar futura; no es requisito de disponibilidad. No se reintentó la escritura de esa propuesta ni se eliminan directorios ascendentes para eludir restricciones. El registro documental posterior del bloqueo de limpieza se detalla por separado en mutation_availability_frontend.md. No se afirma limpieza completa.
- Continuidad: feature 11 schedule_block permanece pending. No se activa contrato ni implementación durante este cierre. Metadatos y backend liberados para el coordinador.

## 2026-09-06 — feature `schedule_block`

- Feature11 queda **done localmente** por dictamen final APPROVED6b937b en progress/judge_schedule_block.md. Contrato a84e42f:62 escenarios y325 casos trazados. Se conservan autorización global del usuario, Ponytail full/Caveman lite, TDD, arquitectura y puertas independientes. No se declara finalización del MVP/proyecto ni despliegue.
- Planificación con revisión previa de objetivo/horario/zona, resolución explícita de ocurrencias DST y presupuesto por día. Consentimiento para exceso separado de la prohibición de solape; tiempo planificado no se acredita como trabajo realizado. Reserva atómica, identidad retenida, consultas propias y recuperación después de incertidumbre o reinicio real del backend con PostgreSQL conservado.
- Persistencia, carreras, rollback, privacidad, ETag/idempotencia y publicación verificadas en sus suites y fixtures reales. Frontend comparte estado confirmado, retira contexto incierto y conserva la recuperación de identidades enviadas. La única corrección productiva del seguimiento frontend elimina estado anterior en retry de TaskReader: RED9d5579/GREEN56ed8e.
- Init final94736 EXIT0/8d8c38: lint,1209/1209 frontend y10 pruebas del arnés verdes. Backend Gradle UP-TO-DATE sobre1365 pruebas previamente verificadas, sin cambios productivos backend; no se atribuye otra ejecución física. Un init anterior falló1208/1209 por una aserción antigua que observaba DOM retirado antes del cleanup pasivo; diagnóstico confirmó la secuencia y se sincronizó sólo el test con404/cancelación, conservando no restauración.
- Backend PIT: campaña inicial414/454; seguimiento separado453/454 Killed (99,78 %), una equivalencia contextual BlockBudget34 y cero NoCoverage/errores/timeouts.454 identidades contrastadas; denominadores sin exclusiones.
- Frontend original:1332 Killed de1561 generados,225 Survived,3 NoCoverage,1 RuntimeError y0 Timeout; Stryker85,38 % sobre1560 evaluables. Informe original preservado, sin presentar el error como detección.
- Primer replay frontend:362/404 Killed,41 Survived y1 RuntimeError; bruto89,60 % y Stryker89,83 %.167 objetivos emparejados:133 Killed,33 Survived y1 RuntimeError. Incluye las3 NoCoverage originales ahora detectadas; línea nueva TaskReader también Killed. Las equivalencias conservan justificación contextual y no se descuentan.
- Medición final79a726:55/55 Killed, sin supervivientes, NoCoverage, Timeout ni RuntimeError en esa campaña. Root verificó29/29 identidades objetivo exactas47a669; los26 adicionales generados por rangos también Killed. Son tres campañas frontend separadas, no un supuesto100 % global. Evidencia en progress/mutation_schedule_block_frontend.md, mutation_schedule_block_frontend_replay.md y mutation_schedule_block_frontend_final.md.
- El juez acepta explícitamente **RuntimeError945 histórico como límite del adaptador Stryker**. No es equivalente, Killed ni error reparado. El replay bruto lo cuenta como no eliminado y supera80; la campaña final no lo incluyó. Se preservan informe, error literal y sus dos reinicios internos, sin workaround ni ocultación.
- Build391add y E2E72ed46 (7/7 Docker/API/PostgreSQL, incluido reinicio real) corresponden a la misma producción vigente56ced31. Siete recorridos Chromium y14 Firefox/WebKit; matrices responsive/zoom/axe y30 principios UX documentados. No se certifican dispositivos físicos, lector de pantalla real ni usabilidad universal.
- E2E global previo57/58 tuvo un timeout histórico en la matriz de28 anchos; traza confirmó coste agregado y se dividieron pruebas manteniendo controles/axe/navegación. Grupo afectado31/31 pasó por separado. No se suman estas ejecuciones como una única campaña global.
- CI de3671b94 terminó cancelled por timeout120min durante Stryker. Ampliación puntual a240min revisada, sin reducir puertas. CI2133120/run34030806009 sigue **en curso al registrar el cierre**; no se certifica success remoto ni eficacia final del timeout240. Commits/push del cierre quedan a cargo del coordinador.
- Se conservan las restricciones de rutas y acciones previamente rechazadas. No se leen, limpian ni eliminan ascendientes de proposal_schedule_block_time.md, .e2e-work/read-review* ni frontend/.stryker-tmp-availability-replay; no se afirma limpieza completa. Patrones protectores de contexto/copia permanecen activos, comprobados sin acceder al contenido protegido.
- Continuidad: feature12 `today` permanece **pending** bajo autorización persistente. No se activa ni se redacta nueva especificación en este cierre. Features1–11 terminadas;12–30 pendientes.

## 2026-09-06 — feature today: registro de sesión previo al cierre

# Sesión actual

## Feature activa

Feature12 `today` in_progress. Contrato de38 escenarios y105 casos revisado (e6781d y ajustes1a8e28) bajo autorización global, antes de TDD. Especificación86dc6c. Se inicia implementación acotada de API y pantalla, con fuentes separadas por autor y revisión posterior. Feature11 conserva cierre done APPROVED6b937b y evidencia histórica.

Backend implementado, revisado independientemente y subido en5cc80a9 (push4ea98f). Init35422 EXIT0/292711:1415 backend,1317 frontend,13 pruebas del arnés y lint verdes. Este corte precede a la última corrección UI de retorno visible con GET pendiente. Frontend permanece en revisión; E2E reales vacío y agenda pasan por separado. Capturas320/1440 revisadas por root; matriz UX completa y mutación pendientes. Ver progress/judge_today.md para alcance y evidencias, sin atribuir cierre a estas mediciones parciales.

Checkpoint posterior f568f6e publicado (43e507): frontend final de fronteras temporales, regresión1319/1319 y lint9698e6. PIT backend80/80 Killed confirmado por root1b93c3. Stryker frontend sesión83950 mide521mutantes sobre este corte; baseline657GREEN, todavía sin resultado. No modificar fuentes/tests/config durante esa campaña.

E2E posteriores: históricos19/19c437d1; tres smokes de Hoy pasan en Chromium/Firefox/WebKit; zoom nativo200%1/1c492dc. UXFirefox/WebKit completó155medidas y cinco axe por motor, pero mantiene EXIT1 por defecto real de foco: disabled de Actualizar envía el foco a BODY al pulsar Enter (cc49ad). Autor resume_review tiene diseñado cambiar a aria-disabled preservando guard de petición; espera terminar Stryker antes de editar. Resume_frontend espera ese fix para ejecutar los cuatro tests Today en tres motores, sin repetir los históricos/zoom si sólo cambia esa semántica. Root revisó PNG de zoom44224 y capturas9324. Matriz30 y límites físicos/humanos en tdd_today_e2e.md. Feature12 no está cerrada.

El corte anterior queda superado: foco corregido en5fe9afc,12ejecuciones Today verdes (4por motor), UX técnica independiente APPROVED2a09f8. Mutación original frontend418/521=80,23%,102S+1NC,0errores/timeouts. Se justificaron40 individualmente y reforzaron63 observables mediante17casosAPI+16UI y una aserción de carga. Revisiones independientes aprobadas. Init14639 EXIT0/a6e120:1353frontend,15 arnés y lint verdes; backend1415 sin cambios. Soporte replay63identidades+regiónfoco revisado8877d8, informes separados; se autoriza medición y se congelan fuentes/tests/config hasta terminar. No se declara done ni un score ajustado. Ver judge_today.md y documentos de seguimiento.

Replay sobre1f7090e finalizó721bcf EXIT0 a15:56:46:107/113 Killed=94,69%,6Survived,0NoCoverage/Timeout/errors.67hashes intactos y original conservado. Root a63608 y mapping76bb72 coinciden:61/63 objetivos K;337 y412 siguenS. Cuatro extrasS corresponden a215/216/219/336 ya justificados. Región nueva de foco generó sólohandler89K, ningún mutante directo de aria-disabled. Freeze liberado: resume_review refuerza412 con GET manual pendiente antes de ocultar/volver; resume_backend revisa337 (aborto explícito frente a cleanup); resume_frontend conserva informe/mapping. No nueva campaña hasta revisión y selección. Feature12 continúa in_progress.

## Último cierre verificado

- Init94736 EXIT0/8d8c38:1209 frontend,10 pruebas del arnés y lint verdes. Backend UP-TO-DATE sobre1365 pruebas previamente verificadas.
- PIT453/454; frontend original85,38 %, primer replay89,83 % y campaña final55/55 Killed con29/29 objetivos exactos47a669. Son mediciones separadas. RuntimeError945 histórico aceptado explícitamente como límite del adaptador, nunca contado como Killed ni presentado como reparado.
- Build391add y E2E72ed46 (7/7, incluido reinicio real) conservan la misma producción vigente56ced31. Evidencia multi-navegador y UX con límites en las bitácoras.

## Continuidad

Feature12 `today`: in_progress, TDD autorizado tras contrato aprobado. Features1–11 done;13–30 pending. Autorización global del usuario vigente: no se repite permiso por feature, se conservan contrato previo, TDD y revisión. El avance de esta feature no declara MVP completo ni despliegue.

## Pendiente remoto y límites

CI2133120/run34030806009 sigue en curso al registrar el cierre. El job anterior terminó por timeout120min; la ampliación240min no se certifica como success remoto. Commits/push de estos metadatos a cargo del coordinador.

Consulta remota e35040: CI56ced31/run34028599117 terminó success;2133120,540381e y a127747 seguían in_progress. Este éxito pertenece al commit indicado, no al backend nuevo ni a la feature12 completa.

Ponytail full y Caveman lite vigentes. Se conservan rutas/acciones protegidas y temporales cuya limpieza fue rechazada: no leer ni limpiar proposal_schedule_block_time.md, .e2e-work/read-review* o frontend/.stryker-tmp-availability-replay, ni eliminar sus ascendientes. No se afirma limpieza completa ni se repiten acciones rechazadas. Sin resets, automaciones o despliegue productivo.

Aclaración técnica de integración30942d/d60c5a: Clock.systemUTC puede dar nanosegundos y el formato heredado admite hasta microsegundos. serverNow se normaliza una vez a microsegundos; el contrato no amplía DTO11. Backend incorpora test de reloj de9 decimales. Continúa TDD; no es prueba E2E ni cierre.


## 2026-09-06 — feature `today`: cierre aprobado

- Feature12 queda **done localmente** por dictamen global final APPROVED en [judge_today.md](judge_today.md), con revisión independiente67b67b. Contrato aprobado38 escenarios/105 casos; autorización global, Ponytail full/Caveman lite, TDD y jueces independientes conservados. Features1–12 done;13–30 pending. No se declara MVP completo, despliegue ni éxito de CI remoto.
- Hoy es la entrada de la web; captura en /proyectos/nuevo y deep links conservados. Agenda propia con nombres/objetivos/instantes, día por zona de disponibilidad o fallback UTC explicado, presupuesto y exceso planificados, actual/próximo y cierre real. Lectura consistente read-only sin eventos ni acreditación de trabajo; preserva bloques históricos de tareas/proyectos completados.
- Actualización manual, visibilidad/foco coalescidos y frontera única; retirada del día vencido, aborto/generaciones y logout protegen privacidad. Los defectos reales de pérdida de deadline y foco se corrigieron con evidencia determinista y navegador. Los refuerzos posteriores sólo cambiaron tests, sin retocar producción para mutantes.
- Init78050 EXIT0/0ba43b:1354 frontend en23 archivos,17 pruebas del arnés y lint verdes. Backend1415 conserva evidencia vigente sin cambios; no se presenta UP-TO-DATE como nueva ejecución física. El cierre independiente confirmó71 hashes intactos; metadatos de cierre no requieren rerun.
- PIT backend80/80 Killed. Frontend original418/521=80,23%,102Survived+1NoCoverage y cero errores/timeouts; seguimiento107/113=94,69%,6Survived y cero NoCoverage/errores/timeouts. Ambas campañas amplias superan80 por separado. Informes originales y40 equivalencias individuales aprobadas permanecen intactos.
- Diagnóstico final ea3333 conserva **FAIL bruto2/3=66,67%, EXIT1**: objetivos337/412 Killed confirmados b40220, extra336 Survived y justificado previamente. No se descuenta ni se presenta el comando como PASS; no hay score combinado. Este diagnóstico resuelve las últimas identidades dentro del criterio independiente fijado antes del resultado, sin sustituir las dos puertas amplias aprobadas. Los63 objetivos originales quedan detectados en las campañas trazadas.
- E2E Hoy: Chromium4/4 6b8014, Firefox4/4 862700, WebKit4/4 d7349f. Regresión histórica19/19 c437d1 y zoom nativo200%1/1 c492dc se conservan como ejecuciones separadas. Cada motor acredita155 medidas (31 anchos/cinco estados), cinco axe y feedback2,9/6/4ms; matriz30 UX aprobada. No certifica dispositivos físicos, lector real ni comprensión/usabilidad humana universal.
- README y roadmap reflejan validación local cerrada. Commits/push quedan a cargo del coordinador; no se certifica CI remoto de565c5be. Restricciones de rutas y acciones previamente rechazadas permanecen vigentes; no se leen ni limpian rutas protegidas o ascendientes, ni se afirma limpieza completa.
- Continuidad: feature13 `reschedule` queda pending; este cierre no redacta su especificación ni activa implementación.


## 2026-09-06 — archivo íntegro de current anterior al cierre13

Los párrafos siguientes conservan la secuencia histórica y sus estados provisionales. El dictamen de cierre posterior los actualiza, sin atribuir resultados antes de su ejecución.

# Sesión actual — continuación integral de Codex

Feature13 Replanificar sigue in_progress; 1–12 done y 14–30 pending. El usuario detuvo Claude Code y encargó a Codex todo el desarrollo y la resolución de PR. Ya no se espera ACK externo. Ponytail full y Caveman lite vigentes.

## Estado remoto comprobado el 6 de septiembre

PR3 cerrada por root como alternativa incompleta, rama d0e83bb conservada. PR4 (9878c94), PR2 (9d0df00) y PR1 (77c7c1d) fueron fusionadas desde otra sesión mientras root comprobaba CI; no son merges ejecutados por este coordinador ni cierres funcionales. La comprobación f2e4b9 confirma los tres merges. No se atribuye aprobación final ni CI verde por estar en main.

El usuario aclaró después que él está haciendo squash and merge desde GitHub. No es otra pista de desarrollo activa. También fusionó PR5 en53ed311. Los nuevos pushes sustituyen el CI anterior por la política concurrency existente; coordinar el siguiente merge con la finalización del CI, sin pedir autorización funcional de nuevo.

El intento local de separar PR4 en f59284d quedó superado por esas fusiones y no debe integrarse: retiraría fuentes13 que ahora están en main. Su CI fue cancelada. Conservar el trabajo vigente de frontend y backend al reconciliar las ramas; no usar force-push.

## Trabajo activo

- Frontend: panel e integración TaskBlocks avanzados. Init99190 pasó 1490 pruebas frontend, 18 scripts y backend; EXIT1 por formato de block-confirmation.test.tsx, ya corregido localmente por su autor conservando AST. Revisión independiente solicita seis correcciones concretas (privacidad de consulta404, recuperación de preview412, foco, aviso al cerrar, errores obsoletos y feedback). Ver review_reschedule_frontend.md y tdd_reschedule_frontend.md. Todavía sin gate de mutación ni E2E13.
- Backend: continuación del checkpoint en worktree OrganizacionWeb-backend. Baseline aislado d72c00 verde tras formato: 1444 tests backend, 1373 frontend y17scripts. El autor core trabaja en contrato HTTP, movimiento, concurrencia y persistencia; otro autor posee exclusivamente el publicador. Ventanas Gradle coordinadas. V12 publicada se conserva; constraints nuevas deben evaluarse en migración aditiva.
- CI: correcciones de navegación, siete locators de estado y Xvfb publicadas. 36 recorridos responsive y cuatro pruebas de estado verdes localmente. CI del main fusionado pendiente; no se declara despliegue.

Actualización verificada: los seis hallazgos frontend se corrigieron y root aprobó la revisión posterior; init integrado6673 EXIT0bc2678 con1495frontend,18scripts y backend verde. Código y alcance congelados en bf99fa5, luego fusionados por el usuario en53ed311. Campaña Stryker18743 en curso:1416mutantes, dry run745tests correcto, sin score final todavía. No modificar fuentes frontend durante la campaña.

CI remoto restaurado completamente: run34047746896 sobre mainae364e5 **SUCCESS**9b3b4e. Log5c33e1 confirma91E2E PASS en3,7min; init, build y publisher correctos. Root publicó formato538d55e y fixtures09fb970/mergeae364e5. La corrección incluye ambas tablas13 explícitas en once TRUNCATE efímeros, sin cambiar oráculos ni CASCADE; local91/91GREEN2ed762. No acredita endpoints13 aún incompletos ni despliegue productivo.

Siguiente corte documental publicado en main407a534, sin cambios de producto. Consulta5e3da7 confirma cero PR abiertas. Move directo y publicador nuevos revisados APPROVED parcial en el árbol backend: review_reschedule_move.md y review_reschedule_publisher.md. Root comprobó205 XML verdes de publicador en0ef577. El comando ampliado de Move tuvo61 casos propios/compartidos verdes pero EXIT1 por18 fixtures de wiring; éstos se corrigieron enb9736d. No se confunden esos cortes con backend13 completo.

Agentes actuales: resume_backend completa replay/intención, lecturas vigentes, constraints y carreras; resume_frontend ahora desarrolla puerto/adaptador/controlador separados de recibos e historial en el mismo árbol backend, con ventanas Gradle coordinadas. resume_review prepara primer E2E13 en nuevo worktree OrganizacionWeb-reschedule-e2e, rama codex/reschedule-e2e desde407a534; posee e2e/reschedule.spec.mjs y documentación, no producción. No integrar su RED previo a los endpoints como entrega verde.

Actualización posterior: Stryker18743 terminó179d24 EXIT0 en79min3s.1416 mutantes:1226 Killed,178 Survived,10 NoCoverage,2 RuntimeError,0 Timeout;86,70% Stryker y86,58% contando errores como no detectados. Informe original conservado. Revisar huecos antes del cierre; errores171/180 del adaptador no se cuentan como equivalentes ni Killed. Ver mutation_reschedule_frontend.md y review_reschedule_frontend_mutation_gaps.md.

Medición autorizada terminada: ejecución 83148 EXIT0 c20ceb, 44 minutos y 13 segundos frente a 79 minutos y 3 segundos. Comparación 120492: las 1.416 firmas mantienen exactamente sus estados, cero Timeout, mismos dos RuntimeError y 14 hashes de fuentes/tests intactos. Concurrency 8 adoptada; rutas normales restauradas y ambos informes archivados por separado. Ver reschedule_frontend_concurrency_comparison.json. Se libera la congelación operativa; el autor de recibos termina primero su paquete backend antes de abordar pruebas frontend.

Main documental9e59516 tiene CI34050389138 SUCCESSa1abc6. Ninguna PR abierta en comprobación5e3da7. No hay despliegue. Plan y revisión documental de infraestructura publicados; capacidad real pendiente de comprobar, no confundir contrato de recursos con RAM libre.

El primer E2E13 queda aislado en checkpoint8e91436, rama local codex/reschedule-e2e: REDb2b65e en preview500 frente a200; helper11 extraído y regresión GREEN551ece. No publicar como funcional hasta integrar backend. resume_review desarrolla ahora V13 aditiva y pruebas PostgreSQL/Flyway en ese mismo worktree, con E2E congelado. resume_backend conserva core/Today/atomicidad; resume_frontend conserva recibos/historia. El publicador y Move directo permanecen revisados parcialmente, sin cierre global.

Snapshot posterior para errores HTTP: core GREEN469422 (46 casos HTTP13) y lecturas GREEN066d26 (8PG) congelaron Java unos segundos. Root copió y comprobó SHA256 de176 archivos de producción9bb10f/38658b y creó checkpoint local d3ffecf en el árbol E2E. No se publica ni se integra ese snapshot completo: faltan los ajustes de tests/wiring de los autores y el cierre funcional. Al aprobar V13, resume_review tendrá exclusivamente BlockController.java, ApiErrors.java y nueva suite de errores13; core y lecturas liberaron esos archivos. Sólo su diff posterior se integrará, conservando avances de Store y recibos. Copia de respaldo y manifest en work/reschedule-error-snapshot-066d26; no incluye secretos, SQL, tests, configuración git ni build.

## Corte posterior: integración y medición terminadas

- Main publicado `9e9d916`, CI `34054091097` SUCCESS en `a9b21f`. Contiene documentación y concurrency 8 medida; frontend fuente/test iguales al corte validado antes del refuerzo nuevo.
- La comparación de mutación también verifica el multiconjunto completo: `fcf722`, 1.416 firmas únicas por informe, SHA256 común `ec08f248847b1dc5016636edd0e3899a3df4574e935114de5c1c80fd943b8041` para firmas/estados ordenados. No hay omisiones ni duplicados.
- V13 aprobada por root con 59 XML verdes (`ad831a`), commit aislado `f0fde3e`, cherry-pick backend `64d5174`. Regresión s20 previa al siguiente caso pasó sobre V13.
- Handlers compartidos aprobados con 224 XML verdes (`fdcd0b`), commit aislado `92e83e6`. Sólo su diff se aplicó al WIP backend con comprobación previa y tres hashes idénticos (`2b9537`); no se copió Store ni el snapshot antiguo. BlockController pasa a ser propiedad del autor de recibos para el cursor compartido.
- Atomicidad backend: las tres escrituras exigen una fila y se ha probado rollback por supresión y por fallo real del commit. Cancelación y movimiento concurrentes con la misma key devuelven 201/200; colisión de key entre bloques sin preferencia devuelve 201/409, ganador único y perdedor intacto. Presupuesto entre proyectos verde `f6785b`. Faltan filas restantes s21, solape creación/movimiento y s23; no se atribuyen a esos replays.
- Recibos: 11 PG y cinco casos de aplicación verdes; HTTP ID/key, página20+1 y query desconocida verdes en `326d1f`. Extracción del decoder compartido con regresión 173 API11 + 3 HTTP13 `961758`. Autor continúa privacidad/errores/cursor repetido/terminal20; ningún cierre global.
- Los seis refuerzos frontend están revisados en `review_reschedule_frontend_gaps.md`: 203 pruebas verdes del autor, cinco hashes comprobados por root `b628ea`, producción intacta. resume_review ejecuta replay focal de13 candidatos con reportes separados, umbral intacto y restauración exacta del config. Después se prevé delegarle las seis órdenes de concurrencia s23, reservadas por core. Core conserva s21/s22/s24; recibos conserva HTTP y cursor.

Los commits de snapshot/E2E (`8e91436`, `d3ffecf`) siguen locales y no deben fusionarse como entrega completa. Para integrar E2E se usará sólo su paquete de archivos; backend e interfaz deben pasar el flujo real antes de cerrar13.

## Entrega

Plan publicado en docs/mvp-delivery-plan.md, commit345543c: MVP funcionalidades1–18, previsión provisional36–72horas efectivas. No equivale a una garantía de fecha, consumo o ausencia absoluta de errores. Features19–30 siguen autorizadas para la entrega posterior.

Los documentos previos de esta pista se conservan en frontend-before-sole-takeover.md. Continuar contratos, TDD individual, review y mutación sin repetir autorización global. No cerrar13 por tests parciales.

## Límites

## Corte de integración posterior

Actualización vigente: rama codex/reschedule-backend-completion publicada en
1c467e5, PR6 en borrador, sin conflictos, CI34058642729 en curso. Init común
91757f pasó1617 pruebas backend (67XML verificados por root09ceee),1498frontend
y22scripts. Soporte PIT aprobado y committed0f355e0. Inventario54fuentes con
cero diferencias de hash comprobado en e181b3.

Campaña PIT sesión98852 y E2E completo sesión67422 en ejecución, sin resultados
finales todavía. No cambiar fuentes/tests/config ni empujar otro corte mientras
se evalúan. E2E incluye el paquete UX aprobado e7f70b0. Firefox/WebKit nominal
y condicional pasaron previamente en el árbol aislado; feedback Chromium2,5ms
con respuesta retenida y reintento real acreditado. El publicador smoke se
ejecutará después del E2E completo. Ningún cierre13/MVP/despliegue por estos
resultados parciales. Los párrafos siguientes conservan la secuencia anterior.

Main fc31969 tiene CI 34055744993 SUCCESS. La consulta remota 27bee7 confirma
cero PR abiertas; el usuario confirmó que realiza las fusiones desde GitHub.
Checkpoint backend local 81a4073: implementación y 447 pruebas focales verdes,
con revisión independiente documentada en review_reschedule_backend_integration.md.
El merge de main está en resolución; se conservan frontend de main y backend
validado. Ningún snapshot Java aislado se integrará como paquete completo.

El replay frontend corrigió la selección de columnas y comprobó los 13 mutantes
solicitados: 11 detectados, uno equivalente por inspección y un error de
herramienta; informe original preservado. Evidencia local 80f42be pendiente de
incorporar. Nominal E2E y recuperación tras reinicio real pasan en el árbol
aislado. UX tiene 31 anchuras y ampliaciones de texto y zoom nativo al 200 %;
faltan controles condicionales, otros motores y cierre del dictamen.

Siguiente puerta: incorporar pruebas E2E y soporte PIT, init común y mutación
backend. Feature13 permanece in_progress. La propuesta14 sólo prepara decisiones;
no inicia otra implementación. No hay despliegue productivo acreditado.

No leer ni limpiar .e2e-work/read-review*, frontend/.stryker-tmp-availability-replay ni progress/proposal_schedule_block_time.md; tampoco borrar ascendientes. No force-push, limpieza global o despliegue supuesto. Mantener fallos históricos explícitos y cada evidencia vinculada a su corte.


## 2026-09-06 — feature13 reschedule: cierre aprobado

- Feature13 pasa a done por judge_reschedule_final.md APPROVED del coordinador. Fuente validada1c467e5, PR6 autorizada para integración; este registro no afirma que ya esté fusionada.
- Init91757f:1617backend,1498frontend,22scripts verdes;67XML verificados09ceee. E2E98/98 verificado4c48a7,296hashes intactosb43b59. Smoke9PASS377658. CI34058642729 sobre1c467e5 SUCCESSff1b45.
- PIT750/758=98,944591%:3SURVIVED,5NO_COVERAGE,0errores/timeouts;265hashes intactos03c8ed. Residuales revisados sin descontar:3huecos HTTP,1equivalencia contextual de presupuesto y4accessors sin uso operativo. Stryker86,70% global conserva2errores de herramienta; replay posterior separado, sin score combinado ni kills inventados.
- Revisiones de dominio, persistencia, V13, publisher, concurrencia, UI y30principios UX aprobadas; dos mejorasP3 y límites de dispositivos físicos/lectores/personas siguen explícitos. Ver review_reschedule_backend_mutation.md y review_reschedule_ux_final.md.
- Se preservan creación11 inmutable, proyección vigente, recibos/eventos atómicos, recuperación idempotente y snapshotsToday. No se declara MVP completo ni despliegue productivo.
- Siguiente14 start_work_session: propuesta revisada/aprobada en proposal_start_work.md y review_proposal_start_work.md, pendiente de Gherkin propio. No implementación14 ni cambios de estado de14–30. Autorización global vigente, Ponytail full/Caveman lite, TDD y rutas protegidas conservados.


## Archivo de la sesión de inicio de trabajo — 7 de septiembre de 2026

# Sesión actual — feature14 en implementación

## Estado vigente: integración del 7 de septiembre, 02:15 Madrid

Esta sección prevalece sobre los checkpoints históricos inferiores. PR12 fusionada por root en main `ae861102ff0507e15617ccb1ff830d6e5babcc50`, tras CI34068314181 SUCCESS sobre3930123: backend,1585frontend,29scripts,104E2E y smoke real. MainCI34068993847 comprueba la fusión. Git fetch confirma que HEAD0c923e8 sólo añade cinco documentos respecto al árbol de main; el código integrado coincide.

Feature14 sigue in_progress hasta el dictamen final. Gates cuantitativos terminados: PIT340/347 KILLED estrictos (97,9827%;5SURVIVED,1NO_COVERAGE,1TIMED_OUT) y Stryker483/539 (89,6104%;56SURVIVED, cero errores/timeouts). Root33e759 verificó288/288 hashes backend y88/88 frontend sin diferencias entre snapshots. UX412 medidas y28axe, tres motores, con límites físicos explícitos. Informes finales conservan resultados brutos.

Tres agentes reactivados a petición del usuario: backend refuerza el esperado JSON independiente y prepara replay sólo de WorkSessionStarted; review añade un caso de calendario imposible a validación de publicación; frontend sigue CI de main y revisa camino crítico. Ningún cambio de producción pendiente en estos refuerzos. No repetir campañas globales para perseguir100%; se conservan residuales y límites. Root revisa e integra; no modificar fuentes durante replay.

Pendientes MVP después del cierre14:15 pausa/reanudación,16 cierre y duración real,17 aviso de fin,18 historial; contrato y TDD de cada una antes de implementar. El objetivo solicitado es hoy, sin convertirlo en garantía. Despliegue sigue pendiente de acceso SSH y dominio ya solicitados; no repetir preguntas. No se habilita uso habitual sin poder cerrar sesiones.

Feature13 Replanificar está cerrada e integrada. El usuario fusionó PR6 mediante squash en `9623990`; la evidencia de cierre se publicó en main `d997421`, con CI `34060054467` SUCCESS. No quedan PR abiertas en la última consulta registrada. El despliegue productivo sigue pendiente.

Feature14 start_work_session está **in_progress**. Especificación normativa `f4a87c5`, contrato `c54aee6`: 42 escenarios y 123 casos declarados. El juez independiente aprobó el contrato en [review_start_work_contract.md](review_start_work_contract.md), y root lo aceptó bajo la autorización global vigente. Estos recuentos no representan pruebas ejecutadas.

Primer avance revisado y versionado: backend `bf8f834` contiene núcleo y persistencia nominal con upgrade V13 a V14; dos pruebas verdes verificadas por root en XML8d682a. Cliente `642a5af` contiene POST y GET active con validación cerrada y precisión exacta; 17 pruebas verdes, tipos, lint y formato según su bitácora. Los dictámenes independientes de núcleo, persistencia y cliente aprueban únicamente esos cortes. Root revisó código y límites en d1418f, 1a2a75 y d21f16. Autores detenidos en el checkpoint, sin procesos de prueba activos. Root conserva Git, documentación e integración. No se implementan todavía pausa, cierre, aviso ni historial global (15–18), y el uso habitual requiere el ciclo de inicio/cierre completo.

Baseline validado de código: init91757f (1617 backend, 1498 frontend y 22 scripts), 98 E2E, 9 smoke; PIT 750/758 y frontend Stryker 86,70 % con sus errores de herramienta documentados. El cambio de rama desde main conservó idénticos backend, frontend, E2E y scripts. No repetir suites completas por cambios exclusivamente documentales; los cambios de producción nuevos requieren sus pruebas y gates finales.

Plan pendiente: [mvp-delivery-plan.md](../docs/mvp-delivery-plan.md), estimación provisional de 30–60 horas efectivas. Dominio solicitado al usuario y todavía pendiente; no bloquea la implementación local. Capacidad, acceso, HTTPS y restauración del servidor aún deben verificarse.

Ponytail full y Caveman lite. No leer ni limpiar `.e2e-work/read-review*`, `frontend/.stryker-tmp-availability-replay` ni `progress/proposal_schedule_block_time.md`, ni borrar/mover sus ascendientes. No force-push ni limpieza global. Conservar evidencia histórica y atribución por corte.

## Continuación concreta

Backend: siguientes ciclos individuales de rango temporal y zona fallback; después idempotencia por propietario, unicidad activa, integridad contextual y errores transaccionales antes de HTTP y consultas. Duración y elegibilidad del núcleo ya están implementadas en 53b37de; tipos/ausencia JSON siguen pendientes del adaptador HTTP. V14 sigue siendo una migración de desarrollo: por ahora sólo crea la tabla y retira la FK legacy del agregado outbox hacia proyectos; sus constraints de negocio todavía no están completos. El evento14 aún no tiene ruta de publicador implementada.

Frontend: consultas por ID/key implementadas en 9e67299. Siguientes ciclos: variantes contractuales restantes y componente de tarea con recuperación deliberada, privacidad y foco. Ninguna UI usa todavía este cliente y ningún endpoint/bean expone el nuevo backend. No se ha ejecutado una suite global, E2E ni mutación sobre feature14; los gates de feature13 no se atribuyen al código nuevo. Esta rama no está lista para merge ni despliegue.

## Avance posterior y estimación revisada

Plan actualizado en 170ca76: 30–60 horas sigue siendo hipótesis de baja confianza, con secuencia y definición de horas explícitas. No equivale a calendario ni a cuota garantizados; 19–30 siguen fuera del MVP sin cancelarse.

Cliente 9e67299: GET por ID/key añade diez casos, 27/27 verdes en 23584f, con tipos, lint y formato. Dictamen review_start_work_recovery_client.md APPROVED parcial; root leyó el diff completo en 2b53db. Backend 53b37de: rango 1–1440 y completed proyecto/tarea, ocho pruebas de núcleo verdes e55cd8; dictamen review_start_work_guards.md APPROVED parcial, root386a6a. Regresión PG existente d4b176 1/1 sobre el núcleo actualizado, XML confirmado por root888dd5. Los nueve casos backend se ejecutaron en dos corridas focales, no en una suite global nueva.

Ambos autores terminaron y dejaron el corte congelado, sin procesos activos. Conservados incidentes e inicialmente GREEN en bitácoras. Feature14 continúa in_progress; no nuevos endpoints, UI ni publicación, no merge de esta rama ni despliegue. No trasladar la aprobación de checkpoints a una aprobación final de feature14.

## Ejecución paralela solicitada por el usuario — 7 de septiembre

Tres autores activos para completar feature14 sin detenerse por cada microcheckpoint: resume_backend termina núcleo/persistencia/HTTP en este árbol; resume_frontend termina cliente/UI en este árbol con archivos separados; resume_review actúa ahora como autor del publicador en el worktree aislado OrganizacionWeb-session-publisher, branch codex/work-session-publisher desde020f7ff. Root coordina y revisa; habrá revisión cruzada independiente después del freeze. No hay autoaprobación del autor del publicador.

Publicación posee únicamente OutboxMessage, PublishOutbox, RabbitBrokerPublisher y tests asociados; backend no toca esos archivos. Frontend no toca backend. Gradle corre con salidas separadas entre los dos worktrees. El equipo dispone de24procesadores lógicos y63,4GiB RAM; no se observó presión de memoria en la comprobación3677e8. Se evita repetir suites globales por cada test y se conservan ciclos individuales y regresiones pertinentes.

Acceso al servidor intentado sin escritura: autenticación SSH rechazada por clave (468624). Solicitud de alias/clave configurada pendiente del usuario; dominio también pendiente. No detiene implementación local ni constituye medición de recursos del servidor. Detalles en docs/deployment-readiness.md.

## Publicador integrado y relevo HTTP

Publicador terminado por autor aislado, revisado independientemente por root en review_start_work_publisher.md: commit b4f425a integrado como6a8534e. XML62c0ab confirma185/185 (152 publicación,13 Rabbit,9 fallos Rabbit,11 configuración), sin fallos/errores/omitidos; hashes coinciden con freeze. Nuevo evento/ruta/cola implementados, sin modificar worker/config. Mutación e integración global14 todavía pendientes.

resume_review pasa a autor HTTP14 en el mismo worktree aislado OrganizacionWeb-session-publisher, ahora branch codex/work-session-http desdeb4f425a. Rootcopió desde el árbol común cinco dependencias de compilación reales y no versionadas (79c786): ReadWorkSessionsUseCase y WorkSessionAlreadyActiveException, WorkSessionIdempotencyConflictException, WorkSessionTimeOutOfRangeException, WorkSessionNotFoundException. **No fusionar esta rama entera ni incluir esas copias en el paquete HTTP.** El autor no las edita; root integrará sólo controller/tests/bitácora revisados. Esto evita que los ciclos RED de compilación del backend y HTTP interfieran.

Puertos: StartWorkSessionUseCase.start(owner,project,task,key,minutes) devuelve WorkSessionConfirmation; ReadWorkSessionsUseCase.active devuelve Optional<SessionStart>, detail y byRequest devuelven SessionStart. El backend real implementa sus puertos y PostgreSQL en el árbol común. ApplicationConfiguration/wiring queda reservado al autor HTTP al integrar, cuando constructor y almacén reales estén presentes. La copia aislada mantiene el constructor core anterior y no debe alterar esa configuración por anticipado.

## Estado vigente tras PR7 y corrección de CI

Esta sección sustituye las referencias anteriores a autores detenidos, V14 editable y main verde. El usuario fusionó PR7 desde 54ea132 mediante squash en 25f7447. V14 ya está publicada y se conserva; la unicidad nueva se implementa en V15. La CI de PR7 y de main falló porque veinte fixtures heredados truncaban tasks sin incluir la nueva tabla work_sessions. El fallo no acredita un defecto de los endpoints ni permite eliminar la integridad referencial.

El backend corrigió las veinte listas explícitamente, sin CASCADE. Reproducción SQLSTATE 0A000, veinte casos existentes verdes después de formato (7c5461 y XML0a729b), uno por fixture. Root verificó en44ce25 que los veinte archivos sólo cambian la inclusión de work_sessions y formato; los XML locales ya habían sido sustituidos por el siguiente ciclo TDD y no se presentan como segunda validación independiente. Commit8f2cd75, paquete sobre main a0ac0a3, PR9 abierta con CI completa en ejecución. El mismo paquete está aplicado al árbol HTTP como24b3bf3. No se han incluido los cambios incompletos de14 en esta corrección.

Los tres autores continúan: backend termina reglas y persistencia; frontend termina interfaz, cliente e integración TaskReader; HTTP termina adaptadores y pruebas. Hito frontend56/56 focales (9f8645):28 API,27 UI y1 integración. Hito HTTP11 casos con seguridad real y puertos simulados (6f0adf). Son avances parciales, no cierre ni cobertura global. Root revisa en paralelo; ha pedido comprobar la respuesta active=null tardía después de confirmar un POST.

Pendiente del cierre14: completar los tres paquetes, integrar wiring, revisión independiente, recorrido real, matriz UX y gates globales/mutación con objetivos específicos14. El arnés todavía no dispone del objetivo de mutación14; debe añadirse mediante autor y revisión antes de atribuir un resultado. No habilitar uso habitual sin cierre de sesiones16. La estimación30–60 horas sigue siendo una hipótesis pendiente de recalcular con el cierre14 y acceso al servidor; no se divide por el número de agentes.

## Hotfix completado, frontend y HTTP revisados

PR10 fusionada por root en861cdc8 tras CI34065173425 SUCCESS: init, build,98 E2E y publisher smoke completos. Incluye las18 listas de limpieza E2E (ef7f423/788c2f6) y seis líneas de formato Java en tres fixtures (306c68f/2e9976e); comprobación Spotless real c3942b. Navegador local4/4 en2fe098. PR11 posterior del usuario repite el mismo head306c68f y produce main83b69ff; diff de árboles vacío906436. Root canceló únicamente la CI redundante de PR11,34065968453, conservando CI de main34065973026. La CI de main anterior34065760784 se canceló al llegar esa fusión. No hay duda pendiente sobre quién fusiona.

Producto frontend congelado10archivos y revisado: commit local46913a7, dictamen review_start_work_frontend.md;73/73 casos propios y250/250 regresión pertinente, lint/tipos/formato verdes. Aún no se hace push de la UI antes de unir sus endpoints. Stryker14 está ejecutándose (sesión42307):533mutantes,8workers, baseline679tests seleccionado por herramienta verde; no resultado de mutación todavía. Registro en mutation_start_work_frontend.md/log y88hashes en mutation_start_work_frontend_before.json. No tocar frontend/config/runner durante campaña. El arnés frontend ya está revisado, pero su despacho backend no debe ejecutarse hasta añadir selector real en Gradle; pendiente de freeze PG.

Reconciliación local de main861cdc8 como03778ed: hubo conflictos add/add en API/test/bitácora y current.md por el squash (c4c5ae). Se conservaron inmediatamente nuestras versiones revisadas;14a871 verificó los10hashes idénticos y merge sin diff de contenido. La campaña ya estaba instrumentada; el autor confirmó88/88hashes intermedios iguales42316b. Incidente documentado, no atribuirlo a un defecto de producto ni afirmar que nunca hubo modificación transitoria. Esperar al final de la campaña para más reconciliaciones Git que afecten frontend.

HTTP aislado terminado:175878c contiene sólo controlador, suite y dos informes; review_start_work_http.md APPROVED para integración. Root verificó hashes y XML49/49 sin fallos/errores/omitidos en c739ab; Spotless focal limpio. Los cinco snapshots de aplicaciones permanecen sin versionar y no se integran. Aún no se ha cherry-picked este paquete al común. Autor resume_review preparó start_work_wiring_plan.md en común y espera freeze PG: registrar Store una vez mediante factory en ApplicationConfiguration y dos factories de casos de uso, sin reabrir Store. Después TDD HTTP+PG real y regresión Spring.

Backend está cerrando PG/core: órdenes de locks reales, concurrencia e identidades propias comprobadas; V15 añade FK compuesta reutilizando tasks_project_identity. Revisión encontró precedencia completed antes de consulta de disponibilidad, corregida con regla compartida WorkSessionContext.requireEligible; últimos ciclos y regresión aún pendientes. Otra arista23505 de outbox reproducida y corregida restringiendo captura de colisión a INSERT sesión. Tras freeze y revisión, el mismo autor ampliará publisher-smoke para evento14 y recuperación real tras reinicio/outbox retirado. Ninguna de estas evidencias sustituye gates finales14.

Optimización local comprobada: Gradle eliminaba daemons compatibles idle al trabajar dos árboles. Perfil JVM propio HTTP768MiB frente al backend512MiB conserva el PID entre ciclos; no modifica configuración del repositorio ni detiene procesos ajenos. Stryker dispone de8workers; la máquina tiene margen, sin atribuir un porcentaje de ahorro no medido.

## Cierre de paquetes14 e integración real en curso

Main83b69ff confirma CI34065973026 SUCCESS, verificación independiente6ea225: init, build, E2E y smoke de publicación. No es evidencia de la feature14 aún local.

HTTP integrado en7186fc3. Core/PG aprobado en review_start_work_backend.md y commit37c86d1, diecisiete archivos seleccionados; V14 sin cambio de contenido. XML/hash independiente492b1f confirma63/63. Autor HTTP ya trabaja en el árbol común para registrar un Store y dos casos de uso, probar HTTP con PG real y los contextos Spring afectados. No se hace push del paquete14 hasta dejar el wiring coherente y validado.

Stryker frontend terminó exit0 (c3875b):452Killed,81Survived,0NoCoverage,0RuntimeError,0Timeout sobre533, score84,80%, umbral80 superado;24m08. Root comprobó el JSON original en a2dd31. Los88hashes finales coinciden según446566; campaña liberada para siguientes acciones. Informe y análisis de supervivientes a cargo del autor frontend, que también completa selector PIT real14 antes de ejecutarlo. Después continúa E2E/UX14 mientras otro agente ejecuta smoke API/PG/Rabbit cuando wiring esté verde. Tres agentes más coordinador activos; no se abren features15–18 antes del cierre14.

## Gates globales y recuperación real14

Wiring revisado e integrado97e7a0a:75/75, cuatro hashes verificados47e2fe. Smoke real recupera ACK perdido, publicación tras caída de Rabbit y recibo tras reinicio/outbox publicado retirado: PASS3e2f05, revisión/hash8afe5c, commitbbd676d. Arnés y evidencia mutación frontend versionadosffd996f. Main sigue en83b69ff; estos commits14 aún locales.

Init global58795e/sesión46298 terminó EXIT0438035:1751Java/1571frontend/29scripts, lint/Spotless verdes. XML73suites Java verificado d0b518, sin fallos/errores/skips. PITbackend iniciadof0b0b1/sesión2804, quince unidades pre-scan, todavía sin resultado. Fuente/test/config backend congelados, snapshot y log propios.

Los tres agentes siguen con trabajo disjunto: E2E/UX14, seis grupos de refuerzos API/UItests tras dictamen81residuales, y mapa contractual42. Los refuerzos frontend empezarán después del initverde y no alteran backend. Campaña Strykeroriginal84,8030% se conserva; no se descuenta ningún residual del score. Hubo errores de cuota en los agentes; consulta posterior devolvió0%consumido y los tres reanudaron sin consumir un reset por herramienta.

## 7 de septiembre de 2026 — cierre local de pausa/reanudación15

Dictamen judge_pause_resume_final.md APPROVED. Pausar/reanudar conserva fin histórico, cuenta microsegundos exactos, mantiene una sola sesión abierta y recupera cambios con revisión/key. Revisiones independientes de backend/frontend y gates; root HTTP/publicación/smoke/E2E. Init local1890Java/1666frontend/32Node, corrección de formato y CI34075064014 SUCCESS6b8f360 con108E2E/smoke; CI34076019493 de los dos oráculos posteriores sigue en curso. PR14 draft, sin despliegue.

PIT523/525K (99,6190476%),2NC y0S/errores/timeouts,315hashesidénticos. Stryker741/861K (86,06271777%),119S/1NC y0errores/timeouts,89hashesidénticos. Refuerzo2tests82/82green; replay seisfirmasK,17/18K con1RuntimeError separado,90hashesidénticos. UX515medidas35axe0, tresmotores/texto200/zoomnativo, límitesfísicosexplícitos. No hay100%inventado ni campaña renombrada. Próximo contrato16 cierre de sesión;17aviso/18historial yservidor siguenpendientes.

## 7 de septiembre de 2026 — integración final15 y comienzo16

Corregido el fixture de active/state que falló en CI e091e7c;2e492a0 conserva producto y supera CI34077907038 con init/build/108E2E/smoke. Descontando tres detecciones atribuidas al fixture antiguo, mutación mantiene738/861=85,71%; raw sin reclasificar. PR14 fusionada en main b2ea1f2 y CI posterior34078825723 SUCCESS (rootc6fae0). No despliegue. Contrato16 aprobado41escenarios/113casos, TDD iniciado80c94b6. Checkpoint coredb8bb0e, publicador207pruebas2a3c0ea y clienteAPI109pruebas17d4a6e revisados; son paquetes parciales, no cierre de16. Tres agentes continúan en core/PG, HTTP y UI.


## Corte intermedio16 antes de refuerzos finales — 7 septiembre 2026

# Sesión actual — contrato de cierre de sesiones

Feature15 está aprobada y fusionada mediante PR14 en main b2ea1f211068e7d93c74d0a8d7e8717ec04323c3. CI34077907038 SUCCESS sobre2e492a0 resolvió el fixture sensible al orden con init/build/108E2E/smoke verdes. Watch99763 terminó EXIT0; log y detalle en ci_pause_resume.md. CI posterior de main34078825723 terminó SUCCESS sobre b2ea1f2 (rootc6fae0): init/build/108E2E/smoke; watch99747 EXIT0. No quedan procesos root de CI activos ni escrituras15 pendientes. La rama16 incorporó la historia del squash mediante0cf35fd tras verificar árboles idénticos; los cambios16 en curso se conservaron.

PIT523/525K,2NC,0errores/timeouts;315hashesidénticos. Stryker741/861K,119S/1NC,0errores/timeouts;89hashesidénticos. Replay complementario seisfirmasK,17/18K y1RuntimeError explícito;90hashesidénticos. Todos los residuos fueron revisados sin reclasificar. UX515/35axe0 en tres motores y zoom200. Evidencia detallada, límites y hashes en los dictámenes; historial actualizado.

## Trabajo siguiente

Contrato16 aprobado y versionado5cbe0b6:41 escenarios/113 casos declarados, SHA3E45F26004E2656E97443451E2D0368CBC9DE734D859725BD07896E206A44285; revisión independiente en review_close_work_contract.md. Root habilita TDD16: el arreglo15 ya está revisado con1668 pruebas locales verdes y su CI remota pasó init/build. La implementación16 comenzó sobre el corte15 congelado, sin modificar su CI; PR14 se fusionó después del resultado completo. Esto mantiene una sola feature en implementación y permite solapar validación remota con desarrollo independiente.

Jason toma core/PG/migración/wiring en COMMON; Confucius frontend en COMMON; Fermat HTTP/publicador en el nuevo árbol aislado OrganizacionWeb-close-http para no compilar cambios de Jason en vuelo. Firmas propuestas en gherkin_close_work_session.md; usar sólo tipos reales al congelarse, sin stubs. Root conserva revisión/Git y no escribe producción/tests. La cota conservadora de mutación15 descuenta tres detecciones del fixture:738/861=85,71%, raw intacto. La propuesta de reducir arranques de Testcontainers sigue sin implementar.

Después17 aviso y18 historial, validación y despliegue del MVP. Hoy sigue siendo objetivo, sin garantía de terminar el proyecto completo o de cuota. Acceso SSH publickey rechazado y dominio pendientes de respuestas ya solicitadas; no repetir preguntas mientras avance trabajo independiente. No se ha desplegado el producto ni se habilita uso habitual antes del cierre16.

## Coordinación

Estado operativo más reciente: PR15 sigue draft sobre910f405. CI34082838516 terminó SUCCESS con init/build/115E2E/smoke; verificación remota a09918 y watch15461 EXIT0 (0e8546). Full E2E local115 passed, EXIT0 a958e5; log SHA C3A526E513126043FB67042B1211232730A2DD92F79AE0C985A7C2E5BA84FD88. PIT30914 terminó EXIT0 ce3127:516/520,2S/2NC,321 hashes idénticos. Stryker23099 terminó EXIT0 aea3fb:1104K/169S/2NC/2RuntimeError, global86,59 %, Reader78,29 %,90 hashes idénticos. Los informes originales permanecen intactos. No hay campañas globales, CI ni runner18080 activos.

Refuerzos finales de pruebas: B añade tres casos Reader sobre privacidad de respuesta antigua, error accesible de progressNote y consulta activa duplicada pendiente. C añade un caso publisher positivo para workDate0001. Producción congelada. A prepara un único replay frontend dirigido; C prepara el replay backend de la frontera detectada. Sólo se ejecutan tras revisión y focal GREEN, con informes separados. Después root ejecutará init/build finales y cerrará el dictamen16. Gradle host se reserva primero al focal de C y luego a su replay; no iniciar otro simultáneamente. Feature17/18 continúan pendientes. Ponytail full y Caveman lite aplicados por todos; root no escribe producción ni tests.
Última frontera: backend completo1984/83 suites GREEN5b77fa, sesión3845 finalizada. Fix histórico Today revisado contra siete blobsGit exactos18beed, commitbe0b858;36Node verdes, sin reescribir informes. PR15 en borrador sobre240c66b, CI34081720550 activo con watch14146. PIT16 activo30914 con321 hashes before; Stryker16 activo23099 con90 hashes before, cinco fuentes/1277 mutantes y dry-run820 pruebas verde. No ejecutar otro Gradle host mientras PIT esté activo. Smoke16 GREEN12 pasos,323 hashes before/after idénticos, revisión root7cccfc e integrado d3dcff7. Ese snapshot precede al posterior arreglo SCSS. B corrigió enlace móvil44px con RED real y GREEN155 medidas/5axe0 en Chromium; TS/TSX permanecen congelados. C completó running, paused y ACK perdido y prepara intercalado@s39. B/C se turnan runner18080; no iniciar otro ni publicar nuevos pushes durante CI en curso sin motivo.

Actualización posterior: implementación16 revisada e integrada en2580d2f y1f5d5b7; dictamen parcial review_close_work_implementation.md. Init32404 terminó FAIL por dos comprobaciones históricas de Today que leían rangos/hashes de App/use-session actuales; C preserva el snapshot original desde1f7090e para resolverlo sin reescribir mediciones. Lint integrado pasó. Regresión frontend completa1718/35 archivos GREEN (8df5cb; sesión60906 terminó EXIT0). Suite backend completa en sesión3845. Primer E2E running real GREEN1/1, d73a29/26b9da; runner18452 retirado. A ejecuta smoke en sesión46148, puerto dinámico; B toma UX18080. Fuentes Java/TS congeladas, campañas todavía no iniciadas. Root no considera el init fallido equivalente a gate aprobado.

Actualización 7 septiembre, 05:49 CEST: paquetes revisados e integrados: core nominal db8bb0e, notas y consulta durable 2b1d7d8, publicador 2a3c0ea (207 pruebas), cliente API 17d4a6e (109 pruebas), HTTP 6eb191b (122 pruebas) y despacho de mutación 67f58a5 (36 pruebas Node; DSL validado sin campaña). Son evidencias parciales, no cierre de feature16. Jason termina concurrencia/inicio y lecturas reales; Confucius termina guardas y presentación del lector; Fermat prepara nominal HTTP+PostgreSQL real en árbol aislado sobre dependencias congeladas. Root revisó scopes completos y umbral80 conservado; contrastar fuentes frontend finales antes de mutar. No hay campañas, runner E2E ni PR16 activos todavía. V17 añade índice único de cierre sin reescribir migraciones publicadas.

COMMON OrganizacionWeb-backend es integración. Root controla Git/revisiones/documentación; no escribe producción ni tests. A/C/frontend quedan disponibles para16. pause-http conserva cinco snapshots locales, nunca fusionar su rama entera. start-work-final conserva evidencia14. El merge6b8f360 sólo reconcilió squash14 tras verificar baseline idéntico; no cambió contenido.

Ponytail full/Caveman lite. No tocar .e2e-work/read-review-state.json, .e2e-work/read-review-stop, frontend/.stryker-tmp-availability-replay ni progress/proposal_schedule_block_time.md, ni borrar/mover ascendientes. No force-push, limpieza global ni intervención en stack8080. V14 muestra M sin diff de contenido: no reescribirla. Evidencia raw local preservada; sólo informes/manifiestos versionados.


## 2026-09-08 — Vistas y campos21 desplegada

PR24 fusionada en e9c4cc810544c84e238d192a5395ab653943030e; infraestructura PR33 en5add5b851d9edb12c3f7ac6a63b58faf8da4a150. Producto publicado dfac90edcabdf04e442b906f0ab6db8894cbc4b2, con pruebas adicionales hasta e2d380c y CI34173869406 SUCCESS:2241 frontend,151 E2E y publicador. PIT378/386; Stryker incremental1494/1824,81.9079% conservador,1484 resultados reutilizados/340 programados, universo original idéntico y errores preservados.

Apply oficial sobre infraestructura0bb939b:38 correctas,5 cambios,0 fallos, operación c92fef1adddbebfecda4b396c9285dc699695398b023c46bc81f581efc36c60d liberada. API/web digests83e75c…4472fd y9fd69f…79cb9, PostgreSQL/Rabbit conservan contenedores;20 servicios1/1. Migración20 aplicada, tres tablas de personalización vacías tras lecturas, hechos existentes y ocho rutas anteriores conservados. Sesión de trabajo comparada excluyendo únicamente serverNow dinámico.

Aceptación Chromium: editor de vista a320/768/1280 y gestión/detalle TASK a320, sin overflow y cinco axe completos sin incidencias; recarga y logout verificados. No altasQA ni escrituras de personalización en producción. CRUD/tipos/rollback cuentan con ensayos aislados. Se mantienen límites de dispositivos físicos y WebKit previos. Evidencia externa organizationweb-customization-acceptance.json SHA57449bc710183e77a7bc9de04a586bdae9968f38e15a850d0307a885295bd55a; apply log SHA54e7a0bec2525cb59489e20d26aea48596de722b9edd4d9d44032b898dbfbb71.

Root y revisión independiente aprueban cierre21. Feature22 exportación sigue pendiente de contrato propio, bajo autorización global existente. Se conservan V14 ajeno y artefactos locales originales; no se afirma limpieza total del checkout compartido.


## 2026-09-08 — Exportación22 desplegada y aceptada

Feature 22 export_data queda done tras aprobación de root como judge y revisión de las mutaciones conservadoras: HTTP/recibos 216/259, persistencia 356/359 y frontend 261/326, conservando originales, residuos y límites sin agregar universos. CI final 34190090017 SUCCESS; aplicación PR25 main 83b027, infraestructura PR35 fba78f4 y fuente 5a2b860 aplicada con 38 correctas/4 cambios/0 fallos/0 omitidas. Release 0030513fa402502b5db87efa87d97fb979171b4e.

Aceptación HTTPS real: cero GET al abrir; una preparación y dos descargas idénticas de 8568 bytes; revocación Blob, axe 0, tres anchuras sin overflow, logout 204 y anónimo 401. Dieciséis tablas idénticas, veinte servicios 1/1, dieciocho contenedores sin reemplazar y ocho rutas heredadas conservadas. Evidencia pública exacta en export_live_acceptance.json (SHA256 750ba94d94fc5aebe36889b41a30c03cc1fe7a74f90df6232559660572b0f137); dictamen en judge_export_data.md. La muestra live es pequeña y usa Chromium; límites masivos acreditados localmente y restore previo sólo de esquema 19. Guía y plan actualizados; sin cambios de producto/tests ni nuevas suites. Funciones 23–30 permanecen pendientes.


## Importación 23 — contexto previo al cierre del 8 de septiembre de 2026

# Estado actual

## Corte vigente de importación 23 — 8 de septiembre, validación final

Actualización posterior: las CI 34217454004 (4c74e18) y 34219923300 (2ddb576) terminaron SUCCESS, ambas con 161 E2E correctos y dos omisiones documentadas. El catálogo corregido 7712fc8 pasa CI 34220016597, lint de directorio/historial y check oficial: 27 correctas, dos cambios previstos, cero fallos, 11 omitidas; bloqueo liberado. Los dos falsos positivos de digests públicos se resolvieron con una excepción exacta por regla/ruta/línea, conservando cinco controles negativos. El marcador del lint fallido se archivó mediante recuperación oficial y prueba de controlador detenido. Evidencias portátiles en import_operational_original_evidence.zip (SHA256 0ea13b18d8e4c7e9df3c749d61c9582b95edcb00bb70dfbe7868f80e55165d85) e import_ci_evidence.json. Falta el dictamen final de PIT de persistencia, apply y aceptación real. El texto siguiente conserva los cortes anteriores de esta validación.

La función 23 sigue en progreso y todavía no está desplegada. El producto candidato es 4c74e183e49afa6d280115b399dbaffedc7bfe7f; bda0195 añade únicamente la corrección revisada de instrumentación de dos pruebas E2E. PR27 permanece en borrador. Las funciones 1–22 continúan disponibles en el servidor; 24–30 siguen pendientes.

Init y build oficiales del candidato terminaron con EXIT 0: 3215 pruebas Java en 135 suites, 2424 frontend y 70 del arnés. Root contrastó los seis artefactos originales, los XML y las 843 entradas antes/después sin cambios. La primera ejecución fallida por coordenadas antiguas de Stryker se conserva; 4c74e18 corrige sólo esas coordenadas y su expectativa del arnés, sin cambios productivos.

Integridad y escala finales: 137 casos PostgreSQL, uno de wiring y dos de escala, más 22 pruebas independientes de concurrencia. La mutación frontend incremental conserva el universo original y alcanza 651/801 Killed (81,2734 % conservador); lector y HTTP mantienen 135/161 y 56/56. La campaña de persistencia continúa y todavía no tiene dictamen final. No se atribuyen sus timeouts a mutantes detectados ni se declara superado el umbral.

El E2E global original terminó con 159 correctos, dos omisiones documentadas de zoom y dos timeouts de instrumentación. El foco posterior pasa 2/2 en 17,2 segundos, con los mismos 31 anchos, cuatro estados, oráculos y límites de 120 segundos. Esta reparación no convierte el global original en verde: la CI del candidato continúa pendiente. Evidencia y límites en review_import_e2e_geometry_repair.md.

Las imágenes del producto 4c74e18 están publicadas y verificadas por digest y procedencia. El rollback aislado API 23 → 22 → 23 pasa, conservando 19 huellas de datos/metadatos y retirando todos sus recursos propios. El catálogo de infraestructura está en PR38: bootstrap y validate-iac pasan; se corrigen dos falsos positivos del escáner sobre digests públicos antes de completar lint y check. No se ha ejecutado apply. La aceptación prevista en producción es sólo preview de una exportación propia fresca, sin confirmar importaciones ni crear datos de prueba.

Los archivos portátiles import_final_validation_original_evidence.zip e import_e2e_geometry_original_evidence.zip conservan los originales; el segundo distingue el global fallido del foco verde. Quedan mutación de persistencia, CI, gates/check de infraestructura, despliegue y aceptación real antes de marcar 23 done.

## Contexto anterior de esta entrega

Funciones 1–22 desplegadas y aceptadas en https://organizacion.apptolast.com. Exportación22 queda done tras el judge aprobado, mutación conservadora y CI final 34190090017 SUCCESS. Aplicación main 83b027; infraestructura PR35 fba78f4, fuente aplicada 5a2b860. Aceptación live: dos descargas idénticas de 8568 bytes con un GET, datos y servicios preservados, cierre de acceso comprobado. Evidencia y límites en [judge_export_data.md](judge_export_data.md) y [export_live_acceptance.json](export_live_acceptance.json).

Importación 23 entra en implementación: root aprobó los 42 escenarios bajo la autorización global del usuario. Contrato SHA256 5678995ED52A89E969D5ED2E36D74DFD4030E6BD8A9D92C43E14A6E64608211E y especificación 959BE37196F6E07FFAF969C1824BB33C86DCAE3E6DBD4872DF15437BEDF49369. Baseline oficial verificado: 2846 pruebas Java, 2295 frontend y 66 del arnés, sin fallos; dos huellas de logs y 123 XML comprobados en 7b7847. Revisión en review_import_data_gherkin.md. Funciones 24–30 siguen pendientes. COMMON/V14 y artefactos ajenos siguen protegidos.

Infraestructura de cierre 22: PR36 fusionada en d1fa2114523800faada08915ef126d78a95db8d9, CI34192767162 SUCCESS. PR26 de aplicación está fusionada en 7e339e9149c7e4bcd75a21d486fd299a803c64f0 con CI34194343037 SUCCESS. Incluye la reparación mínima de espera asíncrona 31c1ab0, trasladada aquí como 1daef57, sin cambiar producto. La rama de importación incorpora main mediante 9640f3e, con árbol idéntico a su primer padre. PR37 de infraestructura está fusionada en 42eb63f27d46532825ae8de3f1c1879ebd3b89fe con CI34196987114 SUCCESS: documenta la copia y restauración aislada de esquema 20. No se repite despliegue por esos cambios documentales y de prueba.

Importación continúa en tres frentes. El corte f3bed6f restaura las catorce colecciones y conserva físicamente todas las filas ante NO_CHANGE, con31 pruebas PostgreSQL; integridad completa, unión, concurrencia y escala siguen en desarrollo. Frontend d2fa8b2 pasa2408 pruebas y la matriz responsive revisada. Su primera mutación termina con617/801 Killed (77,028714 %, insuficiente); se refuerzan oráculos sin reducir scope ni umbral. El E2E nominal aislado sobre f3bed6f pasa después de sustituir introspección DevTools no disponible por hash/tamaño calculados en servidor; falta el paquete final revisado y la regresión sobre el corte definitivo. Lector/decoder y HTTP superan PIT con135/161 y56/56 Killed, respectivamente, sobre entradas inmutables verificadas. Los scopes se integran en49ba7be en ventana sin Java. HTTP/proxy conserva62 pruebas HTTP y15 por socket con permisos equivalentes a producción. Quedan integración y pruebas finales, mutación de persistencia/frontend, rollback y aceptación en servidor sólo mediante preview. Detalle y límites en review_import_data_checkpoints.md. La función23 todavía no está desplegada ni terminada;24–30 no han comenzado.

## Registro histórico de preparación de esta entrega

Los estados y pendientes siguientes pertenecen a sus cortes originales; el estado vigente es el cierre anterior.

## Entrega publicada

Funciones1–21 desplegadas y aceptadas en https://organizacion.apptolast.com. Cierre de aplicación en main5a5464c; infraestructura PR34 fusionada en8b488544caf6220835d2bd4b33a35db9485e17a2 con CI34176136643 SUCCESS. Evidencia21 en judge_custom_views_fields.md y customization_live_acceptance.json. CI de main34176109030 también terminó SUCCESS.

## Exportación22

Checkout limpio OrganizacionWeb-export-data, rama codex/export-data desde5a5464c. Install/init oficiales verdes:2706 pruebas Java/113 suites,2241 frontend/54 suites y61 Node. Log externo export22-init.log SHA134A27C21FE35E082E52DF92D3550F7949BBF56E6E588CDC550D6C88BBF4B4A3.

Sección22 revisada: JSONv1 de catorce colecciones propias, snapshot consistente, datos durables e inactivos, sin infraestructura/secretos;100000 registros y32MiB inclusivos, memoria acotada y respuesta completa. La auditoría independiente corrigió el esquema histórico de los recibos de bloques. Gherkin de 33 escenarios y 79 ejemplos revisado y aprobado por root bajo autorización global; transición spec_ready a in_progress. SHA del contrato: 4F1EECB1810B5B8B4B5DA261A76176C3857943CBCCAB8005420B04DC06CB70B2. Empieza TDD en backend e interfaz con propietarios de archivos separados. Autorización global vigente, sin repetir puerta humana.

Backend de datos e interfaz trabajan en este checkout. HTTP trabaja aislado en OrganizacionWeb-export-http, rama codex/export-data-http, desde 9c0d038 para evitar interferencias entre ciclos RED de Java. Su install/init terminó verde (fab6e8); trasladó cinco archivos propios con hashes idénticos (d4e5c5) y retiró sólo los dos Java originales. La excepción 6b05a4e está integrada allí como c79b5e9. COMMON conserva V14 protegido y sus artefactos; no leer, copiar, restaurar ni modificar ese archivo. La lectura errónea anterior y el contraste válido con Git están documentados en spec_export_data.md. Ponytail full/Caveman lite vigentes; root coordina sin escribir producto/tests.

## Primeros cortes revisados

Root revisó el núcleo nominal: 7 huellas coincidentes y 2 pruebas verdes verificadas en XML. No acredita PostgreSQL ni exportación completa. El puerto PreparedExport mantiene filename/contentLength/writeTo; commit 5a1c534 y formato real verificado en 9c0d038. La primera invocación de formato no seleccionó archivos y su atribución se corrigió en la bitácora. La excepción ExportTooLargeException está fijada en 6b05a4e para integrar HTTP.

El adaptador HTTP completo de C está revisado e integrado en b8906a7: 25 pruebas MVC y dos del filtro verdes, cinco huellas verificadas. Incluye negociación, rechazo temprano, errores y conservación de cabeceras privadas. No acredita todavía socket ni PostgreSQL integrado.

Cliente, vista y UX revisados en 4666401: 2295 pruebas frontend, 63 del arnés, lint y build verdes. Las 170 entradas y ocho evidencias coinciden con sus hashes. El fallo real de foco al deshabilitar el botón se corrigió y comprobó en navegador. La matriz de treinta principios documenta tres motores, 768 medidas, 24 axe y zoom nativo Chromium con captura visible; API simulada y límites humanos indicados. Stryker terminó después con resultado aprobado, detallado abajo.

Persistencia nominal de catorce colecciones y wiring real revisados en 2b927e4; recibos integrados y refuerzo de fecha de creación en 32b9581. C validó el contexto integrado con 2818 pruebas Java y PIT de sus tres clases completas: 216/259, 83,3977%, sin errores ni NO_COVERAGE. Root comprobó el XML original y los hashes. Los refuerzos posteriores cambian sólo pruebas y conservan los 43 supervivientes originales, sin sumar detecciones no medidas.

A continúa integridad y rendimiento: snapshot concurrente y fallo tardío pasan; 100000 registros/32 MiB exactos y un byte adicional se comprueban realmente. La primera lectura por fila tardaba 76,94 s y provocó 504 en Nginx local; lotes acotados de proyectos redujeron la descarga a unos 1,6 s. El caso hermano de 80000 tareas también pasó de 504 a 200 en unos 2 s. Estas mediciones locales no acreditan rendimiento en producción. C trabaja HTTP real pequeño y A conserva los casos masivos/proxy.

La incidencia de solapamiento del ciclo 17 de A permanece documentada con logs originales y comprobación serial posterior. El primer PIT sin bean real se conserva como intento inválido, separado del resultado posterior válido. Exportación sigue in_progress: faltan integridad restante, mutación del lector, aceptación integral y despliegue. COMMON/V14 siguen protegidos.

## Validación posterior

Stryker frontend terminó con EXIT 0: 261 Killed, 60 Survived, 4 NoCoverage y 1 Timeout, 326 en total. El cociente conservador es 80,06134969%, sin contar Timeout como Killed. Root verificó las once huellas del manifiesto original y las 170 entradas idénticas antes/después (c86461 y 4b7d2a). El informe distingue huecos de cobertura de defectos productivos; no se repite la campaña para perseguir 100%. B refuerza únicamente el solapamiento de cancelación y siguiente preparación, sin cambiar producto ni reclasificar supervivientes.

C añadió cinco pruebas por socket real con PostgreSQL, autenticación y error tardío; integradas en 4b90519. El soporte del scope PIT de persistencia está revisado e integrado en 3e42941: once huellas originales coinciden, 64 pruebas del arnés aislado, 66 del combinado y evaluación real del DSL verdes. Conserva nueve patrones completos, todos los candidatos JUnit y los controles previos. Pendiente de ejecutar sobre el corte final de A.

E2E nominal real revisado en db13626: producto fijo ee4ca8d, 1/1 GREEN tras corregir sólo el oráculo del nombre accesible del menú. Dos descargas iguales a los 1332 bytes de una sola petición, propietario correcto, datos ajenos excluidos, catorce cantidades coherentes, filas/eventos sin cambios y Blob URL revocada al salir. Diez huellas verificadas; entre 1913 entradas sólo cambia el test. El stack efímero se retiró. No acredita las guardas posteriores de A ni el despliegue de exportación.

La pasada completa de integridad está registrada en review_export_data_integrity.md. A cierra los últimos campos TEXT sin CHECK, estado de sesión y formato de offsets; no añade reinterpretación TZDB ni normalización histórica. Las reservas masivas pasaron de 504 con 55000 filas a 200 en 1175 ms mediante lotes calculados por máximo SQL, con memoria acotada y Nginx intacto. Quedan freeze final, regresión global, PIT de persistencia y publicación/aceptación.

## Corte final y puertas restantes

Producto congelado en e9350e9. Init oficial final y build terminaron con EXIT 0: 2844 pruebas Java en 123 suites, 2295 frontend en 56 archivos y 66 del arnés. Evidencia revisada en judge_export_data.md y export_final_init_results.json. No repetir estas campañas sin cambios o un motivo nuevo.

E2E completo original terminó con EXIT 1: 154 correctos, dos timeouts de 180 segundos en matrices responsive heredadas y un caso de zoom omitido por nombre de proyecto. Exportación real y simulada pasan. Root verificó las siete huellas del freeze y las 1924 entradas idénticas antes/después (7a3a91, 4e1a89). Los dos fallidos esperan una repetición dirigida sin cambios cuando PIT libere recursos; el resultado original se conserva. PIT de persistencia sigue activo y ha comunicado un MEMORY_ERROR, pendiente de resultado final y clasificación conservadora.

Preflight remoto de infraestructura: bootstrap, validate-iac y lint terminaron correctamente sobre checkout limpio 0bb939bc29ab6e78edb4b0e28d3d6fd01859f415. La validación tuvo dos intentos interrumpidos: primero por reinicio del runtime local y después por un paginador interactivo. Ambos marcadores se recuperaron con la herramienta oficial, que verificó controlador detenido y archivó evidencia; no se retiraron manualmente. La ejecución final usa GIT_PAGER=cat, PAGER=cat y TERM=dumb. Logs externos en deployment-preparation/export22-infra-*.log. No se ha publicado ni desplegado exportación. La web actual responde HTTP 200.

Actualización posterior: el replay sin cambios dejó una matriz verde y otra en timeout. La reparación revisada del arnés sustituye únicamente comparaciones geométricas síncronas de Playwright por node:assert/strict, conservando todos los criterios. Ambas matrices pasan ahora en 8,3 segundos cada una; nueve huellas y 1924 entradas sin cambios comprobadas en 229739. Los intentos anteriores conservan sus resultados originales.

CI manual 34187626169 valida 0030513fa402502b5db87efa87d97fb979171b4e y está en E2E, después de init/build verdes. Imágenes candidatas publicadas desde ese checkout limpio, 1932 entradas sin cambios: API índice sha256:1276d6e618f5ab6aadfa816244f5022a2393cd46d2c8c09a6d5bde1e8ec9f49b y web índice sha256:2568a6bf4c2347171df4433f50d5127ac1e4537385ad4b067c595c889eaf8d92. Tag, plataforma linux/amd64 y label exacto verificados; evidencia externa release22-0030513-*. La reparación E2E posterior no cambia el producto de esas imágenes. A prepara catálogo candidato en nuevo DockerSwarmInfrastrcture-export-data, desde main8b488544. No hay apply ni aceptación productiva22; PIT sigue pendiente.

## Cierre de validación y publicación preparada

PIT final EXIT 0: 356/359 KILLED, 99,164345 % conservador; un superviviente, uno sin cobertura y un error de memoria preservados. Root verificó veinte huellas y 453 entradas idénticas. Originales portables en export_persistence_original_evidence.zip. Dos refuerzos test-only pasan con la clase completa 39/39; no replay ni nuevas eliminaciones atribuidas.

E2E final completo EXIT 0: 156 correctos, cero fallos y un skip con evidencia dedicada previa; 1924 entradas idénticas. CI productiva0030513 run34187626169 SUCCESS, incluidos 156 E2E y publisher. PR25 abierto en borrador. Su primera CI34189142299 falló en una espera del test de apariencia heredado; ajuste mínimo revisado, suite50/50 y frontend2295/2295 verdes. Espera el próximo gate del PR; el producto de las imágenes permanece idéntico.

Infra PR35 fusionada como fba78f495796cea69217812e6634a35d8bfc63b8; CI34189520466 SUCCESS. Fuente revisada/apta para operación5a2b8607a80624fbd0c410a9a3a81955afa564a1, checkout remoto limpio. Bootstrap/validate/lint y seguridad del candidato aprobados. Check UTF-8:27ok/2changed/11skipped/0failed, operación ca70b68b33ab1e1868ab7ae222a63cf5e7f0641967bb07a30f7f4da6e6c08c9b liberada. Primer check falló antes de tareas por LC_ALL=C; recuperación oficial archivada y nueva ejecución C.utf8 correcta. No se ha aplicado aún.

Baseline remoto nuevo:20 servicios1/1, mismos contenedores21,16 tablas de negocio/Flyway/outbox con cantidades y huellas sin contenido personal. Evidencia externa export22-live-*-before. Restore PostgreSQL previo acreditado sólo hasta snapshot7sep23:32/esquema19; no se presenta como restore20 ni se borran escrituras posteriores. Revisión operativa aprobada para esta entrega sin migración, pendiente apply/HTTPS. No quedan procesos de pruebas locales salvo nuevos gates expresamente indicados.

## Importación 23: baseline y contrato aprobado

Checkout limpio OrganizacionWeb-import-data, rama codex/import-data. Baseline oficial 7b7847, install/init EXIT 0 sobre 42639f7: 2846 pruebas Java en 123 suites, 2295 frontend en 56 archivos y 66 pruebas del arnés. No se repitieron gates para estos cambios documentales. Root integró después la corrección histórica de test 31c1ab0; HEAD de partida documental 1daef57. Feature 22 ya fue aceptada live por root; los pendientes anteriores se conservan como historia, no describen la puerta actual de 22.

Root aprobó el contrato 23 SHA 2DAA5BE12C1221B6FEE70A4E0FE4865AAFCA92B972A2FC86EFD36235FDBBA36C junto con revisión C B6FF4036ABA6FEED1B0C4E90DACDA33B3EE10250E8AC8C5C8A3913FF240645F2 para destilación bajo autorización global. La promoción de redacción a normativa conserva semántica y explicita que las dos locations exactas de Nginx son hermanas de /api/ y conservan sus directivas proxy explícitamente. Sin cambios de status, Gherkin ni producto; TDD espera revisión del contrato destilado.

Se preservan copias exactas del razonamiento y revisión en import23_spec_proposal_historical.md, review_import23_contract.md e import23_invariant_map.md. La propuesta histórica mantiene su vocabulario de borrador y sus decisiones intermedias; la sección 23 de project-spec.md es la normativa aprobada. Incluye fusión integral propia sin sobrescritura, recibo atómico sin republicar eventos, advisory locks compartidos PROJECT→TASK antes de 15 locks EXCLUSIVE y recepción acotada por API a 32 MiB tras autenticación. No se ejecutó ninguna escritura de aceptación ni operación live de 23.

## Estado vigente: exportación cerrada e importación en desarrollo

PR26 fusionada en main 7e339e9149c7e4bcd75a21d486fd299a803c64f0 tras CI34194343037 SUCCESS, incluidos init, build, E2E y publisher. Conserva el fallo anterior y repara únicamente dos esperas de efectos asíncronos de pruebas heredadas; no cambia las imágenes productivas de exportación. Infra PR36 fusionada en d1fa2114523800faada08915ef126d78a95db8d9 con CI34192767162 SUCCESS. Las funciones 1–22 están desplegadas y aceptadas.

Importación 23 está in_progress bajo los 42 escenarios aprobados en review_import_data_gherkin.md. Backend y frontend trabajan en archivos separados del checkout primario; HTTP trabaja en OrganizacionWeb-import-http después de init oficial verde. Los primeros puertos y el lector parcial no constituyen la implementación completa. No ejecutar regresión global durante ciclos RED ajenos ni modificar COMMON/V14.

El 8 de septiembre a las 06:40:09 UTC se creó una copia PostgreSQL nueva mediante el bloqueo operativo oficial y un helper revisado. Se restauró completamente en PostgreSQL aislado, sin red ni puertos: 16 tablas coinciden por cantidad y huella, incluido Flyway 20. Evidencia original copiada sin cambios en import_backup_restore.json, SHA d485e919e6e18ead1691a8f6a4a52af7cb01e23a1f02334b8eb216204ffe2e75. Archivo remoto de 58237 bytes, root:root 0600; la copia privada no se versiona. Contenedor y volumen temporales retirados y búfer privado borrado.

Esta prueba amplía la evidencia de recuperación al snapshot indicado del esquema 20. No modifica la limitación histórica del restore 19 ni acredita escrituras posteriores, futuras migraciones, recuperación integral de API/RabbitMQ/Swarm o custodia externa. No se ha ejecutado ninguna importación productiva.

Infra PR37 registra esta prueba posterior, fusionada en 42eb63f27d46532825ae8de3f1c1879ebd3b89fe tras CI34196987114 SUCCESS. Es documentación: no requiere apply. Cortes parciales de importación revisados en review_import_data_checkpoints.md: cliente/intención 56 pruebas y HTTP preview corregido 20 pruebas MVC. Persistencia continúa en su primer corte PostgreSQL; interfaz, integración, mutación y despliegue 23 siguen pendientes.

## Estado actual de importación tras revisión consolidada

22/30 funcionalidades completadas y publicadas. Importación 23 sigue in_progress. Frontend aprobado: 2424 pruebas y mutación conservadora 81,27 % con 801 firmas intactas. Lector/HTTP tienen PIT aprobado; concurrencia aislada 16/16 revisada e integrada. El registro detallado y límites de cada corte están en review_import_data_checkpoints.md.

A cierra integridad histórica y escala sobre persistencia. Sus nuevos ciclos de recibo corrupto y rollback tras catorce colecciones están en la bitácora; todavía no se atribuye aprobación final de root a ese trabajo mutable. Quedan corte backend fijo, revisión final, regresión integrada, PIT de persistencia, E2E completo, rollback efímero por digest y publicación/aceptación. No se ha desplegado importación ni se ha cambiado el estado de las funcionalidades 24–30.


Cierre: APPROVED y desplegada. Véase progress/judge_import_data.md e import_live_acceptance.json. API24 es la siguiente implementación; su propuesta y escenarios se prepararon de forma aislada durante la validación de23.

## API24: transición y primeros cortes (histórico)

# Estado actual

## Importación 23 cerrada y publicada — 8 de septiembre de 2026

Funciones 1–23 desplegadas y aceptadas en https://organizacion.apptolast.com. Producto 4c74e183; aplicación PR27 fusionada en 3f4c3ef e infraestructura PR38 en 183293b. Fuente aplicada 7712fc8, EXIT 0: 38 correctas, cuatro cambios, cero fallos y cero omisiones. Operación liberada.

Root aprueba los 42 escenarios con init/build y CI verdes y campañas completas sobre el umbral: lector 135/161, HTTP 56/56, frontend 651/801 y persistencia 316/330 KILLED (95,7576 %). Los timeouts y errores siguen en sus denominadores. La campaña alternativa se retiró incompleta tras verificar la original; sus resultados no se combinan.

Aceptación live sólo mediante preview de una copia propia en memoria, sin confirmar importación ni crear recibos: 17 tablas conservadas, 20 servicios 1/1, 18 contenedores intactos, ocho rutas anteriores iguales. Chromium a 320/768/1280 sin overflow ni incidencias axe, teclado, cancelación, logout 204 y rechazo anónimo 401. Evidencia y límites en judge_import_data.md, import_live_acceptance.json y paquetes originales import.

## Siguiente trabajo

API para integraciones24: propuesta revisada y 42 escenarios preparados en OrganizacionWeb-integration-api. Todavía no se implementa ni se marca done. El baseline de ese checkout detectó una espera heredada de appearance.test.tsx; su reparación de sincronización está revisada, con validación global y CI posteriores todavía pendientes, sin cambios de producto. 25–30 siguen pendientes de contrato/implementación. Autorización global vigente, sin nuevas puertas humanas rutinarias.

COMMON/V14 y artefactos ajenos permanecen protegidos. El histórico de esta entrega está preservado en history.md. Este cierre cambia documentación/estado/evidencia e incorpora la sincronización revisada del test heredado de apariencia (16d60f8, foco 50/50); no cambia producto ni repite despliegue por esos archivos. La validación global posterior del arreglo y la CI de cierre se registran por separado.

## API 24: contrato aprobado e implementación iniciada

Root aprueba A155A48E4AF40CA7A0BBFD2979FD14A89BBE0EDDCD3A587C8E3861EF087C4B95: 42 escenarios, revisión independiente y autorización global vigente. Transición pending → spec_ready → in_progress, después del cierre real de 23. Backend, frontend y frontera HTTP trabajan con propietarios separados; root coordina y revisa.

Baseline corregido sobre 2bb20674, init oficial EXIT 0: 70 pruebas Node y 2424 frontend ejecutadas. Java UP-TO-DATE reutiliza 3215 pruebas de 135 suites ejecutadas en el baseline anterior. Root verificó cinco artefactos y 843 entradas antes/después idénticas. Metadatos externos integration24-corrective-results.json SHA a7ab08f98b16ae17b574a2f28bcf217da815e2a9dbe61fb834284bac52b110f2. El intento fallido original se conserva; no se atribuye una segunda ejecución Java. Este apartado sustituye el pendiente de baseline anterior. PR28 espera CI; infra PR39 fusionada, sin nuevo despliegue por documentación.



## Checkpoint conservado antes de validación integrada de API24

# Estado actual — API para integraciones24

Las funcionalidades1–23 están desplegadas y aceptadas en https://organizacion.apptolast.com. Importación23 conserva producto4c74e183 y aplicación/infraestructura ya fusionadas. El cierre documental PR28 se fusionó en4c7d558 tras CI34231982918 SUCCESS (26m38,161E2E y2omisiones documentadas). InfraPR39 se fusionó en9dbc08e. No hubo otro despliegue por documentación o corrección del test heredado.

Feature24 está in_progress con42escenarios A155A48E aprobados por root bajo autorización global; propuesta y revisión independientes versionadas. Se precisó en de9b062 que microsegundos admite0–6decimales UTC y que el primer revokedAt válido puede preceder createdAt si retrocede el reloj. No hay nuevas puertas humanas rutinarias. 25–30 no están implementadas; la consulta opcional al usuario sobre proveedores de calendario/tareas sigue pendiente y no bloquea24.

## Trabajo paralelo y revisión

A desarrolla aplicación/dominio/PG/V22 en este checkout. B desarrolla cliente/intención/interfaz en archivos separados aquí. C desarrolla HTTP/seguridad/OpenAPI en work/OrganizacionWeb-integration-http; integra commits de A aprobados. Root coordina/revisa/integra/despliega, sin escribir producto ni tests. Ponytail full/Caveman lite vigentes.

Cortes revisados en review_integration_api_checkpoints.md: emisión nominal f485486 y creación durable c58bad5 (18fuentes+51evidencias,29tests). Incluyen replay/cupo/locks/rollback/wiring; A precisa dos oráculos de carrera/caducidad y continúa lectura/revocación/autenticación/cuotas. Cliente segundo corte32tests (26cliente+4intención+2UI nominal) aprobado tras corregir fechas con RED019/020; vista y su integración siguen en desarrollo. HTTP estructural16tests y corte creación/resolver24tests revisados: fronteraJSON4096, errores reales/no-store, resolver de sesión estándar todavía sin cadenaBearer integrada. No atribuir cierre funcional a esos cortes.

Baseline oficial corregido2bb20674 EXIT0: Node70 y frontend2424 ejecutados; Java UP-TO-DATE reutiliza3215pruebas/135suites ejecutadas originalmente. Root verificó5artefactos y843entradas idénticas. C inició checkout separado con install/init0 nuevos, logs originales preservados; no se atribuye su conteoJava desde XML reemplazado por focales. Git45e1edd une main4c7d558 con árbolidéntico al padre24: sólo ancestry, sin sobrescribir fuentes de agentes.

## Próximas puertas

Completar las tres fronteras, revisar/integrar, fijar corte, ejecutar regresión integrada, E2E/UX y mutación sobre todo lo tocado. Verificar rollback aditivo y export/import sin credenciales antes de construir/publicar imágenes24 y hacer aceptación HTTPS. No hay despliegue24 todavía. Plan operativo externo integration24-release-checklist.md. Evitar globales durante ciclosRED ajenos y repeticiones de gates válidos por docs.

COMMON/V14 protegido: no leer/hash/copiar/restaurar/modificar;8080protegido/18080reservado. No borrar artefactos ajenos, forzarGit ni parar procesos no identificados. Los originales23/PIT y campaña alternativa abortada siguen separados. Credenciales privadas permanecen fueraGit/logs/chat; escrituraQA de24 usa infraestructura efímera. El PC no se mantiene activo mediante automatizaciones ni otra sesión.


## Avance posterior — 16:30 Madrid

Gestión backend 17e9b3b revisada: 41 pruebas y 54 artefactos verificados. HTTP eb3cb38 integrado como ee4f275; rama publicada hasta 36912bf. Autenticación y cuota nominal cefd1c9 revisadas: 67 pruebas, 75 artefactos verificados y puertos liberados para C. El fallo de listado 503 quedó corregido. A completa concurrencia, atomicidad, invariantes y dos fixtures de wiring heredados.

B incorporó la ruta privada y navegación, recuperación por id, revocación incierta por credencial y tolerancia a almacenamiento inaccesible. Sigue cerrando UI/foco/SCSS y navegador; aún no hay freeze final. C integra los puertos reales y termina seguridad/OpenAPI; el mapa independiente de respuestas está en el artefacto externo integration24-openapi-source-map.md. A posee build.gradle.kts, B configuración Stryker y C scripts del arnés para las campañas de integración. No se ha lanzado una campaña ni suite global sobre ciclos activos.


## Avance posterior — 17:03 Madrid

Backend final propio 8d7d8b2 revisado: 122 pruebas/9 XML y 87 hashes verificados. Frontend 82a45c2 fijado, evidencia original en ZIP; revisados recuperación, privacidad, foco y guardas. Dispatcher integrado d153fbe. A revisa HTTP/Bearer parcial de C; C completa wiring real y OpenAPI; B cierra scope Stryker, rangos históricos y evidencia responsive. Campañas, regresión integrada, E2E real, rollback de imágenes y despliegue24 todavía pendientes.



## Estado archivado 2026-09-08T15:51:01.289Z

# Estado actual — API para integraciones (24)

Las features 1–23 están desplegadas y aceptadas en https://organizacion.apptolast.com. Producto 23: 4c74e183; cierres de aplicación e infraestructura fusionados. No hay despliegue 24 todavía. Las features 25–30 siguen pendientes, sin porcentaje de avance ficticio ni compromiso de terminar todas hoy.

Feature 24 está in_progress bajo los 42 escenarios aprobados (A155A48E), con autorización global para implementar, revisar, publicar y desplegar sin nuevas puertas humanas rutinarias. React/SCSS, arquitectura hexagonal, PostgreSQL y controles de seguridad se conservan. Ponytail full y Caveman lite vigentes.

## Cortes revisados

- Backend final propio 8d7d8b2: 122 tests en nueve XML; root verificó 87 entradas. Incluye creación y replay, cuotas atómicas, concurrencia observada, revocación, owner habilitado, COMMIT incierto, Unicode y compatibilidad V21/V22/export/import.
- HTTP 5b10287 y wiring 1466f07: 138 tests en seis XML y nueve casos reales con Clock fijo. Corrección de LogoutFilter aplicada exclusivamente a Bearer. Verificados originales y separación de sesión. OpenAPI completo aún pendiente.
- Frontend 82a45c2 y gate 9a15cb5: 2.492 tests en 65 suites, Node 72, lint y build verdes. Tres expectativas heredadas de posición del menú actualizadas; originales fallidos conservados. Scope Stryker completo y siete nodos AST verificados.
- Compatibilidad de negocio 20c3ac3: dos tests integrados inicialmente verdes para PUT/ETag/outbox y errores 412/428/404 que consumen cuota sin cambiar hechos. Once entradas verificadas. PIT usa ocho workers sólo para los dos scopes de integración, con los demás controles intactos.

## Trabajo activo y próximas puertas

B ejecuta Stryker oficial del frontend sobre fuentes fijas: 997 mutantes, ocho runners. Prepara E2E real aislado en puerto 18084; el navegador simulado acredita teclado, recuperación, temas, geometría 320–2560, texto 200 %, movimiento reducido, colores forzados y zoom nativo sólo Chromium. No confundir esas pruebas con backend real.

A prepara checkout DETACHED 20c3ac3 en OrganizacionWeb-integration-pit, install/init oficial y después una campaña integration_api-backend. C cierra OpenAPI y su revisión; root validará el esquema oficial offline. Root coordina revisión/Git/ops; no escribe producto ni tests.

Faltan regresión integrada final, E2E real, mutaciones con killed/total >=80 %, imágenes, ensayo API24→23→24, infraestructura y aceptación HTTPS. El helper operacional V22 está preparado, no ejecutado: compara 20 tablas más esquema y audita logs privadamente. No almacena tokens ni logs brutos en evidencia. Plan externo integration24-release-checklist.md.

## Límites operativos

COMMON/V14 protegido: no lectura ni hash manual, copia, modificación o restauración; excluir V14 de nuevas enumeraciones de contenido. Puertos 8080, 18080 y 18081 protegidos/reservados. No parar procesos ni alterar stacks ajenos; no forzar Git ni borrar artefactos de otros trabajos. Credenciales SSH/sudo permanecen fuera de Git/logs/chat. QA de escritura usa infraestructura efímera; aceptación live 24 prevista sin escrituras de negocio. La pregunta opcional sobre proveedores de calendario/tareas sigue pendiente y no bloquea 24.
