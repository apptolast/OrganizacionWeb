# Mutación de backend de la feature 30 — remedida, 10 de septiembre de 2026

**98.09 %** (615/627), calculado del `mutations.xml`.

Sobre el SHA `33b94a6e`, árbol limpio, máquina drenada.

## Cierra la condición 1 del veredicto final: los tres umbrales se cumplen

| Objetivo | Umbral | Medido | |
|---|---|---|---|
| global backend | 0,80 | **98.09 %** | ✅ |
| `ExecuteAutomations` | **0,90** | **95.08 %** (58/61) | ✅ |
| global frontend | 0,80 | 90,29 % | ✅ |
| `automations.tsx` | 0,80 | 83,52 % | ✅ |
| `automations-api.ts` | 0,80 | 99,46 % | ✅ |

## La cifra sube de 95,72 % a 98.09 %, y el 96,00 % nunca existió

| | antes | ahora |
|---|---|---|
| Publicado | «96,00 %» — entero redondeado de un HTML | — |
| Real medido | 95,72 % (581/607) | **98.09 %** (615/627) |

Sube porque el carril de supervivientes mató 17 mutantes a mano, uno a uno, con el
mutante aplicado al fuente y su rojo acreditado. Y porque los cuatro defectos de
producción que arreglaron los otros dos carriles trajeron **20 mutantes nuevos**
(627 contra 607), que también se matan.

La previsión que dejó escrita el carril era 98,68 %; el medido es 98.09 %. Se
anota la diferencia porque **una previsión no es una medida**, y confundirlas es la
falta que produjo el «96,00 %».

## Cuarta confirmación del arreglo del `avoidCallsTo`

`Slf4jAutomationAudit` pasa de **cero** mutantes a **2**, los 2 muertos.
Es la cuarta feature independiente donde se confirma. Cierra además la condición 3
del veredicto, que exigía comprobarlo en la campaña y decirlo si seguía a cero.

## Los 12 sin matar, nominalmente

| Clase | Método | Línea | Mutador | Estado |
|---|---|---|---|---|
| `ApplicationConfiguration` | `lambda$enabledApiCredentialOwner$0` | 22 | BooleanTrueReturnValsMutator | NO_COVERAGE |
| `AutomationCursor` | `precedes` | 20 | ConditionalsBoundaryMutator | SURVIVED |
| `AutomationEvent` | `payload` | 9 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `AutomationSchedule` | `tick` | 28 | VoidMethodCallMutator | SURVIVED |
| `EventTask$OfWorkSession` | `sessionId` | 11 | NullReturnValsMutator | NO_COVERAGE |
| `ExecuteAutomations` | `attempt` | 87 | BooleanTrueReturnValsMutator | SURVIVED |
| `ExecuteAutomations` | `confirm` | 127 | BooleanFalseReturnValsMutator | SURVIVED |
| `ExecuteAutomations` | `confirm` | 127 | BooleanTrueReturnValsMutator | SURVIVED |
| `TemplateValues` | `eventType` | 6 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `TemplateValues` | `occurredAt` | 6 | NullReturnValsMutator | NO_COVERAGE |
| `TemplateValues` | `projectName` | 6 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `TemplateValues` | `taskTitle` | 6 | EmptyObjectReturnValsMutator | NO_COVERAGE |
