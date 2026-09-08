# Preparación incremental 21

Configuración seleccionada: frontend/stryker.custom-views-fields.config.json, mismo dispatcher node scripts/project.mjs mutate custom_views_fields-frontend. No se ha ejecutado esta campaña.

Stryker instalado 10.0.0. Lectura de dist/src/mutants/incremental-differ.js, función mutantCanBeReused: con cobertura perTest, un Killed se reutiliza sólo si conserva al menos un test culpable con estado same; para otros estados un test de cobertura added obliga a ejecutar. Los no Killed sin cobertura nueva pueden reutilizarse. No se afirma que los nueve RuntimeError se vuelvan a ejecutar si Stryker no detecta cobertura nueva. Se conservarán los estados reales y el denominador conservador completo.

mutation-test-report-helper.js escribe el incrementalFile; por ello se usa una copia byte a byte del original en reports/mutation-custom-views-fields-refined/incremental.json. El raw histórico permanece en progress/customization_stryker_original/mutation.json, SHA3DC6D9B548B532389660A53B9915B620683C552032FEF8E3510A10AFAE997942.

Cambio autorizado sólo: incremental=true; incrementalFile, JSON, HTML y temp propios; reporter progress-append-only añadido. Este nombre está registrado en dist/src/reporters/index.js línea14 de Stryker10. No se añade engine, dependencia ni dispatcher. Los cinco módulos, once nodos, perTest, ocho workers, umbral80, ignores y mutadores permanecen idénticos. La comparación JSON343710 confirma seis claves operativas cambiadas y seed idéntico.

Antes del arranque pendiente: revisión root de refuerzos A/B/C, global frontend final, manifiesto de entradas efectivo y copia final de configuración. Después: comprobar mismo universo por firmas, conteo de reutilización anunciado por Stryker, distribución completa y Killed/1824 sin sumar manualmente el original a un parcial. Una reutilización válida es evidencia declarada del modo incremental; no se presentará como1824 ejecuciones nuevas.
