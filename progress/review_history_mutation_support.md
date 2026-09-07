# Revisión del soporte PIT de historial

APROBADO para integración. Se revisaron dispatcher, pruebas y DSL finales en `ea590c`, contrastando los cuatro hashes del manifiesto antes de versionar. El selector público history-backend incluye los doce patrones completos propuestos, todos los candidatos JUnit, cuatro workers y umbral 80, con reporte separado. Las exclusiones y ámbitos anteriores se conservan.

La revisión detectó que el selector default incorporaba clases nuevas sin sus pruebas de adaptador. La corrección añade HistoryApiTest y History*Test de persistencia; ReadHistoryTest ya pertenece a core. RED `0624cd` y GREEN final `f9ffae`: 43 pruebas Node, formato focal y validación DSL satisfactorios.

El dry-run dejó PIT SKIPPED: no es una campaña de mutación ni acredita un porcentaje. Antes de ejecutarla se contrastará el alcance con el freeze final del backend. Stryker queda pendiente del diff final de interfaz. Los archivos E2E y sus evidencias en curso quedan fuera de este commit.
