# TDD del soporte de mutación17

Trabajo exclusivo en árbol aislado end-http tras HTTP5723f1c. Scope aprobado por raíz; no cambios de producción ni campañas. Se conserva la lista explícita de19 clases/patrones Java con guardas, shared y wiring, todos los candidatos JUnit, cuatro threads y umbral80. Frontend usa las siete fuentes confirmadas por B, ocho workers, umbral80 y la exclusión protegida heredada. Antes de ejecutar campañas deberá contrastarse de nuevo el diff final de A/B: la integración frontend sigue en desarrollo.

Cuatro ciclos individuales:

1. Despacho backend exacto: RED708df2 por destino desconocido → mínimo GREEN04f15f.
2. Despacho frontend exacto: REDb02e60 → GREENa843af. El runner inyectado registra llamadas; no lanza PIT ni Stryker.
3. Selección PIT explícita, default inclusivo, todos los JUnit y reporte propio: RED7b5550 → GREENb8fb3e.
4. Config Stryker17, superficies compartidas, protección y reporte propio: RED888f26 por archivo ausente → GREEN523190.

Formato: intento inicial desde root2a7ca5 falló porque Prettier no está instalado en ese paquete; no se cambió dependencia. Se utilizó el ejecutable existente del paquete frontend: GREENa1ac3c. Regresión final Node **40/40 EXIT0 a1beec**, sin saltos. `git diff --check` EXIT0 y conteo9827ba. Gradle `pitest --dry-run -PmutationScope=end_time_notification` **EXIT0 be4f62**, configuración compilable y todas las tareas SKIPPED; no es una campaña ni una compilación de fuentes. Logs propios end_scope_*.log.

Freeze de cuatro archivos: scripts/project.mjs, scripts/project.test.mjs, backend/build.gradle.kts y frontend/stryker.end-time-notification.config.json. Hashes en end_scope_freeze_hashes.json. Los scopes/configs/reportes14–16 y snapshot histórico de Hoy permanecen intactos; no se edita harness global ni se reduce el umbral. La integración futura debe validar existencia de las siete fuentes en COMMON y el diff final antes de ejecutar.
