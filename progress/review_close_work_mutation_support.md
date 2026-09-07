# Revisión del soporte de mutación16

APPROVED para integrar configuración y despacho, no como resultado de mutación. Root revisó el diff completo del paquete aislado y verificó los cuatro hashes de close_mutation_support_hashes.json antes del commit 5fe18d5, integrado selectivamente como 67f58a5.

Los dos destinos del arnés invocan únicamente su herramienta y scope específico. PIT mantiene candidatos JUnit completos, umbral80 y cuatro workers; los quince patrones incluyen clases compartidas completas, adaptadores y records nuevos. Stryker conserva el umbral80, ocho workers, análisis por test y protección del temporal histórico; incluye los cinco archivos frontend declarados por su autor. Ambos escriben informes separados de15. No se cambian los reportes anteriores ni se reducen ramas para mejorar la puntuación.

Evidencia del autor: cuatro ciclos individuales RED/GREEN, 36 pruebas Node verdes d293fc y DSL Gradle validado mediante dry-run812e67. Root no repitió una campaña ni presenta ese dry-run como ejecución de PIT. Antes de la campaña se contrastará el scope contra el diff final de producción, especialmente cualquier cambio adicional en WorkSession.tsx. El lector nuevo se incorporará desde la implementación real; su ausencia en el snapshot aislado no se solventa mediante un stub.
