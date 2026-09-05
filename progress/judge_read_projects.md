# Dictamen conjunto — read_projects

Resultado: **APPROVED localmente**, 6 de septiembre de 2026. Revisión coordinada independiente de los autores de producción, con dictámenes específicos en `judge_read_projects_backend.md`, `judge_read_projects_frontend.md` y `judge_read_projects_integration.md`.

La consulta aplica aislamiento por propietario, paginación estable, errores seguros y navegación accesible. La revisión detectó y corrigió el rango temporal del cursor fuera de PostgreSQL y una comparación E2E insuficiente del error privado 404. Las capturas de zoom inicialmente recortadas se sustituyeron por capturas correctas inspeccionadas visualmente.

Evidencia local: 190 tests backend sin fallos; 73 tests frontend, lint y build verdes antes de cinco pruebas adicionales, seguidas de 40 pruebas focalizadas y lint verdes. Hay 78 pruebas frontend declaradas; no se atribuye una nueva ejecución global de 78. Integración: 14 E2E Chromium y dos recorridos Firefox/WebKit, 12 anchos, teclado/axe y zoom real al 200 % con reflow a 320 píxeles CSS.

PIT: 103/103 mutantes eliminados en dominio/aplicación. Stryker global: 276/297 (92,93 %), 21 supervivientes, sin errores ni falta de cobertura. Cinco pruebas adicionales cierran seis huecos observables; replay separado de 17/17. Los 15 equivalentes restantes están justificados en `mutation_read_projects_frontend.md`. Los resultados separados no se combinan como una puntuación global nueva.

Límites: no se certifica WCAG global, dispositivos físicos, teclado virtual ni usabilidad humana. La CI 33995196185 del commit 24b1e50ad000fe6fbc96fef5809c12f82d552854 seguía en curso al emitir este dictamen; el cierre local no equivale a CI confirmada ni despliegue en el servidor.

Ponytail full y Caveman lite aplicados: reutilización, alcance acotado y evidencia explícita, conservando el contrato y las garantías de calidad.

Confirmación posterior: CI 33995196185 completada con `success` para 24b1e50ad000fe6fbc96fef5809c12f82d552854. La CI 33995645977 del cierre de pruebas y skills en 317e868cc22dead80c9ffcbe992062284240e46b sigue en curso; se distinguen ambos commits.

Confirmación final: CI 33995645977 también completada con `success`. El cierre de pruebas y la integración de las skills tienen CI confirmada en 317e868cc22dead80c9ffcbe992062284240e46b.
