# TDD del soporte PIT de apariencia

Scope aprobado por root: once patrones de appearance_mutation_scope_proposal.md.
Cambios limitados a scripts/project.mjs, scripts/project.test.mjs y
backend/build.gradle.kts. No se modifican Java durante su revisión independiente.

- Dispatcher: RED 6329b6 por target desconocido; GREEN dc5274 con invocación
  exacta de pitest y -PmutationScope=appearance, sin ejecutar la campaña.
- Alcance DSL/defaults: RED 511cd2 por lista ausente; GREEN 5a91fd tras añadir
  once patrones, todos los JUnit como candidatos y reporte pitest-appearance.
  Los tests HTTP/PG/wiring también quedan disponibles en el alcance default.
- Suite completa de dispatcher: 53/53 GREEN eb967f después de Prettier.
  Prettier no está en el paquete raíz (930cb5 EXIT1); ejecución desde el
  paquete frontend instalado 5c7b90 EXIT0, sin instalar dependencias nuevas.
- Gradle help -PmutationScope=appearance valida evaluación real del DSL:
  EXIT0 a0c1bf. No se ejecutó pitest, tests Java ni una campaña global.

Se conservan mutadores, exclusiones, timeout, heap y cuatro workers actuales,
umbral 80 y reportes históricos. Freeze en appearance_scope_freeze.json.
Los hashes de ambos Java HTTP siguen siendo los del freeze previo.
