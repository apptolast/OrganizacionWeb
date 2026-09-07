# Soporte de mutación16, sin campañas

Trabajo aislado en close-http tras freeze HTTP, sin modificar Java. Propiedad: scripts/project.mjs, scripts/project.test.mjs, backend/build.gradle.kts y nueva configuración frontend. A confirmó que no edita selector; B confirmó cinco fuentes actuales y avisará al freeze si cambian. Root integra selectivamente este soporte, no la rama completa.

| Ciclo individual | Evidencia |
| --- | --- |
| Destino backend fijo | RED7f6930 → GREEN81fd54 |
| Destino frontend fijo | REDc8fbe0 → GREENb6c946 |
| Scope PIT y candidatos JUnit | REDcc831f → GREENb27f81 |
| Scope Stryker/protección/salidas separadas | RED4c7cdf → GREEN3d1c59 |

Regresión final36/36 Node, cero fallos/skip, resultado d293fc; runner inyectado, no lanza herramientas de mutación. Prettier se ejecutó con el binario instalado en COMMON, escribiendo únicamente los tres archivos propios del aislado. `pitest --dry-run -PmutationScope=close_work_session` validó DSL/tareas con EXIT0 812e67; no ejecutó tests ni PIT. Logs `close_dispatch_*`, hashes de cuatro archivos en `close_mutation_support_hashes.json`, diffcheck verde.

## Alcance y conservación

- `mutate close_work_session-backend` invoca PIT con `-PmutationScope=close_work_session`. Quince patrones incluyen Notes, State, OutboxMessage, ChangeWorkSession, ReadWorkSessionState compartido, ReadWorkSessionChanges, Receipt, Transition, WorkSessionChanging (default delegado), Closed, Closure, Store completo, Controller con records, Rabbit y ApplicationConfiguration compartida. El selector default también conserva esta unión. Todos los JUnit `com.apptolast.organization.*` siguen siendo candidatos: no se omiten las suites nuevas de cierre/migración ni las heredadas de14/15/publicación/wiring. Umbral80, cuatro threads, mutadores y exclusiones previas intactos. Salida propia `reports/pitest-close-work-session`.
- `mutate close_work_session-frontend` usa exclusivamente `stryker.close-work-session.config.json`, derivada de15. Archivos completos: work-session-state-api.ts, work-session-state.tsx, work-session-reader.tsx, App.tsx y use-session.ts. Sin exclusiones de ramas. Ocho workers, perTest, vite.config.ts y umbral80 heredados. Reportes propios `reports/mutation-close-work-session/`; temporal `.stryker-tmp-close-work-session`; se conserva `ignorePatterns` del directorio protegido `.stryker-tmp-availability-replay`.
- B declara WorkSession.tsx todavía intacto y no se añade por nombre previsto. El Reader nuevo aún no existe en este snapshot aislado: la config se integrará con su fuente real, no se crea stub para ejecutarla aquí. **Antes de la campaña, contrastar lista contra el freeze final de B**; si WorkSession u otro archivo cambia, actualizar scope y test de selección. Las suites frontend nuevas/compartidas se descubren mediante el mismo Vite, sin lista reducida.
- Configuraciones y reportes15 no se modificaron. No se ejecutó Stryker, PIT, init global, E2E ni smoke; este paquete acredita despacho/configuración, no el gate de16. Java HTTP/publicación continúa congelado.
