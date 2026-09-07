# Refuerzos posteriores a PIT original de apariencia

Autorización root: tres oráculos públicos, sin cambios de producción ni del
scope original. El raw de159 mutantes permanece idéntico; no se reemplaza.

1. AppearanceValuesTest.s11_acceptsAValidAccentWithANonzeroLowLinearChannel:
   #0002D0, mínimo7.856465610995792; inicialmente GREEN ccdf75.
2. AppearanceValuesTest.s11_acceptsAValidAccentWhoseContrastNeedsTheLuminanceOffset:
   #0033FF, mínimo5.168912080757051; inicialmente GREEN 2ab38b.
3. AppearancePersistenceTest.s35_lastPublicMicrosecondCanBeCommittedAndReadByANewStore:
   Clock9999-12-31T23:59:59.999999Z, Save real, commit, nuevo Store y SELECT
   JDBC confirman exactamente el instante; inicialmente GREEN6786b3.

No se llama a métodos privados ni se duplica la fórmula en los tests.
PostgreSQL/Testcontainers y Flyway son los existentes. Dos archivos reciben
42 líneas; sin fixtures imposibles ni mocks que fabriquen el resultado.

Formato por archivo:93cdfd y aa9a89 EXIT0. Primer comando de regresión53055a
falló antes de tests porque --tests quedó asociado a spotlessJavaCheck.
Orden corregido, sin editar los oráculos: d25688 EXIT0,29/29 (9dominio,
20PG), SpotlessCheck. XML preservados appearance_refinement_xml.
Comprobación31bbbf:389 de391 entradas originales intactas; sólo cambian los
2tests autorizados. XML original conserva SHA293593AC…2EDA5E.

## Replay propuesto, todavía no ejecutado

Configuración local appearance_pit_replay.init.gradle reutiliza el mecanismo
ya probado en19: tres clases, métodos donde residen las firmas y mutadores
CONDITIONALS_BOUNDARY/MATH. Conserva todos los JUnit candidatos, cuatro workers,
heap, timeout, umbral80 y configuración principal. Sólo este replay delimita
métodos para evitar repetir cambios ajenos a los cinco objetivos.

Cinco objetivos exactos en appearance_refinement_targets.json:

- AppearanceValues.channel:62, MATH,index23.
- AppearanceValues.contrast:53, MATH,index16.
- AppearanceValues.color:40, CONDITIONALS_BOUNDARY,index44 (sin prometer kill).
- PostgresAppearanceStore.lambda$select$0:45, CONDITIONALS_BOUNDARY,index88.
- SaveAppearance.timestamp:66, CONDITIONALS_BOUNDARY,index23.

El mecanismo instalado selecciona clase/método/mutador, no un único opcode.
La proyección del XML original sobre esos filtros tiene15 firmas: los cinco
objetivos y10 acompañantes (9K originales y el boundary channel ya explicado).
No se añade plugin para retirar este último opcode: se inventariarán todos.
No se afirma equivalencia de color4.5; si sobrevive conserva su limitación.

Comando desde backend, únicamente tras revisión root:

```powershell
.\gradlew.bat pitest -PmutationScope=appearance -I ../progress/appearance_pit_replay.init.gradle --no-daemon
```

Reporte exclusivo backend/build/reports/pitest-appearance-replay. Preservar
hashes antes/después, XML y todos los estados, comprobar cinco firmas completas
(clase/método/descriptor/línea/mutador/index/block) y separar resultados de159.
No otra campaña o cambio de pruebas sin diagnóstico y coordinación.

## Ejecución posterior autorizada

Root revisó el init script y autorizó un replay. EXIT0 0a8f27: 13 K y 2 S
sobre 15, cuatro objetivos ahora K y color4.5 aún S. Inventario y límites en
mutation_appearance_backend_replay.md. No se ejecutan más pruebas Java.
