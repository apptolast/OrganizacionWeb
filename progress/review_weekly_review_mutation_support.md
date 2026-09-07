# Soporte de mutación de revisión semanal

Corte preparado y congelado para revisión de root. No se ha ejecutado campaña. Los dos destinos son `weekly_review-backend` y `weekly_review-frontend`; no se añade un destino combinado inexistente en el patrón History.

## Alcance PIT propuesto y aplicado

`mutationScope=weekly_review` selecciona siete patrones bajo `com.apptolast.organization`: `application.ReadWeeklyReview`, `application.ReadWeeklyReviewUseCase`, `application.WeeklyReviewQueries`, `domain.WeeklyReview*`, `adapter.persistence.PostgresWeeklyReviewQueries*`, `adapter.http.WeeklyReviewController*` y `adapter.config.ApplicationConfiguration`. El patrón de dominio incluye el DTO y sus records internos, Window/Interval y la excepción temporal; los adaptadores incluyen internos. No se añade publicador, porque 19 no lo cambia.

Todos los JUnit son candidatos (`com.apptolast.organization.*`). Se conservan cuatro workers, umbral 80, timeout adicional 15000 ms, filtros heredados de equals/hashCode/toString y FRECORD, XML/HTML y directorio propio `backend/build/reports/pitest-weekly-review`. El default agrega las clases nuevas y los tests WeeklyReviewApiTest, WeeklyReview*Test de persistencia y ApplicationWiringTest; aplicación/dominio ya entran por core.

Frontend se despacha a `frontend/stryker.weekly-review.config.json`, propiedad de B. Este paquete no modifica su configuración ni certifica su campaña.

## Ciclos individuales

1. Dispatcher backend: RED 81826c por target inválido; mínimo branch cerrado; GREEN e39ba3, 1/1.
2. Dispatcher frontend: RED 6d931e por target inválido; mínimo branch cerrado; GREEN ad97e1, 1/1.
3. Contrato del scope completo: RED 65162d por ausencia de weeklyReviewClasses; selector/patrones/candidatos/reporte/default añadidos; GREEN a80887, 1/1.
4. Candidatos del default: RED e8510f, TAP 0/1 por ausencia del conjunto (el comando de lectura posterior devolvió shell 0, no se atribuye ese EXIT a Node); agregado de adaptadores y wiring; GREEN fdf6ee, 1/1.

Comandos focales: `node --test --test-name-pattern='<nombre de cada caso>' scripts/project.test.mjs`. Tras Prettier de los dos scripts, `node --test scripts/project.test.mjs`: 5453ae EXIT 0, 51/51, cero fallos/omitidos. No se repitieron suites funcionales Java.

Coordinado con B después de retirar su primer Docker E2E, `gradlew.bat pitest --dry-run -PmutationScope=weekly_review --daemon --max-workers=4`: 24d0a9 EXIT 0, BUILD SUCCESSFUL en 2 s y todas las tareas SKIPPED. Sólo valida configuración/grafo; no acredita mutantes, cobertura ni ejecución de tests. Diff check limpio a51412.

## Freeze

- backend/build.gradle.kts: AED484175B76734B54955B0665CC5F7A109FF3DB167C6358B2E552569B544E4F
- scripts/project.mjs: 7AE95F27645A40DB559E1681497448D4EC2E6C8B7FDA992861C7BB2A25C0C792
- scripts/project.test.mjs: 1C442805EEDC776E38271ED9F0B13C3884E887B114F0A4180EC01FE7717EF23C

Diff: tres archivos, 120 inserciones y dos sustituciones de unions default. Root aprobó el alcance antes del dry-run. Pendientes revisión final de este paquete, init/build integrado y autorización de campaña con manifiesto de inputs antes/después. Producción funcional permanece congelada en 1301fa8; no se ha realizado Git mutation.
