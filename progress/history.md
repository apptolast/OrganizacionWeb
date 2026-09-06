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
