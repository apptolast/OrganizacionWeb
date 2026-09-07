# TDD custom_views_fields — backend21

Ponytail full/Caveman lite. Init21 root94704 EXIT0 ce0f1d heredado del corte
aprobado, sin nuevo init local ficticio. Contrato c91bb8b, 42 escenarios y
174 ejemplos léxicos, no equiparados al conteo de tests. A posee dominio,
aplicación, persistencia, V20 y wiring; C HTTP, B frontend. V14 no se toca.

## Primer bundle de lectura, no cierre funcional

1. @s1 ReadCustomizationTest.s1_readsAbsenceForOnlyTheAuthenticatedOwnerAndRequestedScope:
   RED real 3bb0ff por tipos ausentes (customization_01_red.log), GREEN c9a6ac
   (customization_01_green.log). Puerto devuelve Optional real, sin Clock.
2. @s2 ReadCustomizationTest.s2_readsTheStoredScopeAndDefinitionOrderWithoutChangingItsRevision:
   RED 5d3e0f por modelos ausentes/incompletos, GREEN bc1efe, logs
   customization_02_red/green.log. Campos exigidos por el test; preserva el
   orden de inserción incluso cuando UUID lexicográfico sería distinto.

Formato real spotlessJavaApply y regresión focal 2/2 GREEN62c3f4, log
customization_first_bundle_green.log. Manifest customization_first_bundle.json
contiene siete fuentes y un test. Sólo nominal lectura por puerto; no acredita
PostgreSQL, defaults HTTP, validación completa, comandos, concurrencia ni beans.
Se entrega temprano para que C use tipos reales sin duplicarlos.

Firmas estables: ReadCustomizationUseCase.get(String owner,CustomizationScope
scope) devuelve Optional<Customization>; CustomizationQueries.find igual.
Customization(id,owner,scope,visibleFields,customFields,version,updatedAt);
CustomFieldDefinition(id,label,type,active); Scope PROJECT/TASK y Type
TEXT/NUMBER/DATE/BOOLEAN. Enums y records puros sin HTTP/Jackson/Spring.

Siguiente ciclo: defaults y validación de dominio; lectura PG nominal y bean
real tras adaptador, sin registrar un bean con dependencia ficticia. Guardas,
escrituras y lectura compuesta de valores todavía pendientes explícitamente.
