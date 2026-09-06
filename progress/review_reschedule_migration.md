# Revisión V13 — APPROVED parcial

Coordinador independiente del autor. Alcance: V13__block_change_integrity.sql, RescheduleMigrationTest.java y bitácora tdd_reschedule_migration.md. No acredita el cierre de Replanificar, E2E13 ni despliegue.

Lectura de SQL y pruebas ea68cf/c33630/ad831a: coincide con el diseño mínimo aprobado. Proyección con versión positiva y estado conocido; las ocho columnas temporales se conservan todas nulas o completas, incluidas las cancelaciones legadas. Para intervalos completos se exige duración real y precisión/rangos heredados de V11, sin imponer orden local ni consultar catálogo de zonas histórico. El caso DST conserva horas locales invertidas con instantes válidos. No hay UPDATE, backfill, triggers ni parser SQL duplicado de offsets.

Los cambios tienen revisión positiva, tipo conocido, unicidad por bloque/revisión y FK que vincula proyecto/tarea/bloque en conjunto. La unicidad original task/request_key permanece. El JSON exige solamente objeto: almacena el record interno con PlannedBlock, no el DTO HTTP. El índice adicional deriva de la UNIQUE de soporte necesaria para la FK compuesta.

Upgrade PostgreSQL/Flyway real: bloques sin proyección, ambos metadata-only, cancelación completa y movimiento/recibo sobreviven sin cambios de filas ni nuevos eventos. Ante un recibo inválido que falla en la última constraint, se revierten todas las constraints nuevas y el historial Flyway queda idéntico, junto con originales/proyecciones/recibos. No se usa repair ni corrección automática de datos inválidos.

Verificación independiente ad831a: hashes coinciden con freeze del autor; XML de 21 pruebas de migración y 38 de persistencia11, todas verdes, sin errores ni omitidas. Regresión del autor4a9c9a EXIT0; formato focal401312 EXIT0. V11/V12 y el snapshot Java d3ffecf no tienen diff. Los casos inicialmente verdes se documentan como tales; no se fabrica RED para compatibilidad ya satisfecha.

- SQL SHA256:652E9253B79D7179E88AF8F1D0DE4F36548174D69905364A3C3B2656CA23E982.
- Test SHA256:83BEEB8FE654B3289746442D7C852F15028FB7DA4B227916646284C22532E4A0.
- Bitácora SHA256:5FEE8797E452521B53ED5DB46D331C064DE791540A4D10F09DAC5A9D7F9B4B07.

Integrar exclusivamente este paquete aditivo, después en la regresión conjunta con los comandos y lecturas nuevos. La SQL no recibe score PIT de bytecode. Compatibilidad demostrada con fixtures del contrato, sin afirmar auditoría de datos de un servidor al que aún no se ha desplegado.
