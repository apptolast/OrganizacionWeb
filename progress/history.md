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
