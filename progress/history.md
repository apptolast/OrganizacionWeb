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
