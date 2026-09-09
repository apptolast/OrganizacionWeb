# TDD — Feature 30 `automations` (fase 1)

Worktree `C:/Users/vhurt/ow-worktrees/automations`, rama `claude/automations`. Contrato: `features/automations.feature` (@s1–@s43). Puerto E2E 18095. Skills Ponytail full y Caveman lite aplicadas. `.memoria-cache/patterns/` no contiene patrones de testing ni arquitectura en este worktree: se diseña desde las plantillas del repositorio (feature 24, custom_views_fields, export_data, CreateTask/TaskCommit).

Feature en curso: 30 — automations. Escenarios a recorrer en fase 1: @s1–@s14, @s18, @s19 (parcial), @s21 (parcial), @s27 (parcial), @s28–@s43. Diferidos a fase 2 (dependen del worker compartido y de la feature 25): @s15, @s16 (ciclo del worker), @s17, @s20, @s22, @s23, @s24, @s25, @s26 y las filas NOTIFY_WEBHOOK reales de @s5, @s12, @s21, @s32 y @s33.

## Bitácora de ciclos

### Ciclo 1 — @s1, @s3, @s4, @s5 (validación de referencias al crear)

- **Rojo**: `CreateAutomationTest` (3 tests) no compilaba: faltaban `AutomationRule`,
  `AutomationRuleStore`, `AutomationTargets`, `WebhookEndpointLookup` y las cuatro
  excepciones. 29 errores de compilación en `compileTestJava`.
- **Verde**: `AutomationRule` (id, draft, version, createdAt, updatedAt),
  el puerto `AutomationRuleStore`, los puertos de referencias y `CreateAutomation`,
  que valida referencias, captura el instante con `CustomizationTime` (micros) y
  guarda versión 1. `WebhookEndpointLookup` es el puerto de la fase 2: en fase 1 se
  usa un doble que responde «no encontrado».
- **Refactor**: la comprobación de referencias vive en `AutomationReferences`,
  colaborador de paquete, para que la reutilicen reemplazo y simulación.
- Focal verde: `--tests "…application.CreateAutomationTest" --tests "…domain.Automation*"`.

## Estado en curso (nota para el coordinador)

Se ejecutan **sólo pruebas focales con filtro** por la contención de Testcontainers
en la máquina; la suite completa queda para el cierre, con turno del coordinador.

## Trazabilidad

(mapa @s → test al cierre)

## Comandos y números

(al cierre)

## Decisiones y límites

(al cierre)
