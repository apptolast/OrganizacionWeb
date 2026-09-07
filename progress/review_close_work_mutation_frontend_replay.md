# Replay dirigido de mutación frontend 16

Attempt2 terminó EXIT0 real d8a58f, en1min17s. Generó15mutantes:12Killed y3Survived, cero NoCoverage/RuntimeError/CompileError/Timeout. Score12/15=80%, alcanza exactamente el umbral configurado. No significa que las doce firmas objetivo hayan muerto.

Las doce firmas objetivo están presentes por coincidencia exacta de archivo/posición/mutador/reemplazo:9Killed y3Survived. Extra originales555/557/627: los tresKilled. Correspondencia completa en close_work_stryker_replay_mapping.json. Privacidad611 y retry628/630 quedaronKilled. Sobreviven originales546 (aria-invalid progress siempretrue),548 (noteError OR inválida) y558 (descripción accesible next vacía).

El nuevo caso de accesibilidad sólo observa progress inválido después de submit y la descripción de progress. La implementación vigente permite corregir ese campo, y mantiene la descripción de ambos campos vinculada al mensaje común. Ajuste mínimo propuesto al MISMO caso: corregir progress a texto válido y poner next inválido, sin nueva escritura; exigir progress aria-invalid=false, next=true y descripción accesible de next enlazada al alert. Distingue los tres supervivientes sin matriz nueva. Es defecto del oráculo, no defecto de producto demostrado. Ningún cambio ni nueva campaña iniciado a partir de esta propuesta.

El JSON íntegro está en close_work_stryker_replay_attempt2.json, SHA2564BA214FEB030FD4E20085002B6EB80836C58933A0037F52A4816F0DE883B868B; log attempt2 separado. Los91archivos de attempt2_before_hashes y attempt2_after_hashes permanecen idénticos (0da241). Test FE9CFF2684A1E51EC8BA3EF9AFFE197CE29E6942CFEF704D51825EC39369ACDF; config9C4B59B33C7315257DD68FE2B1EE85158C8B624020A056272419B1FFCAFC5F84.

El primer intento tuvo un error de selección de columnas, preservado y explicado en plan_close_work_frontend_replay.md y attempt1*. Sólo midió2mutantes, no docefirmas. Se corrigieron las columnas del selector (base0) desde las del JSON (base1), con autorización root; las líneas no cambiaron. La implementación local concatena rangos del mismoarchivo. No se alteró producción ni tests durante ninguna medición.

Los1277resultados originales,169Survived ydosRuntimeError conservan su estado original. No se suman scores de campañas ni se usa el replay para certificar código fuera de sus ocho rangos. No se declara equivalencia de los tres supervivientes.

Decisión posterior root: aprovechar el caso existente de next inválido (progress vacío válido) y añadir allí las aserciones de progress aria-invalid=false y descripción accesible de next. Conserva sus aserciones anteriores y evita extender la secuencia del caso nuevo. B tiene esa edición exclusiva; attempt2 y91hashes ya estaban preservados antes del cambio. Se espera su freeze y revisión para la última medición con la misma configuración.

## Cierre de la última medición autorizada

Attempt3 APPROVED en su alcance dirigido: EXIT0 real002701, duración1min27s; dry run641GREEN. Resultado15/15Killed,100%, ceroSurvived/NoCoverage/RuntimeError/CompileError/Timeout. Las doce firmas objetivo están presentes yKilled por coincidencia exacta: originales543,546,547,548,549,550,554,556,558,611,628,630. Extras555,557,627 tambiénKilled. Ninguna firma faltante. La correspondencia completa está en close_work_stryker_replay_attempt3_mapping.json (af1559).

Se usó la misma configuración9C4B59B33C7315257DD68FE2B1EE85158C8B624020A056272419B1FFCAFC5F84 y el test de B revisado83F1931C3C95088550EA67BFF00368F4A480458F03DEE7438E23E266712ECD13. Sus91hashes before/after son idénticos af1559; no hubo cambios de producción, pruebas ni configuración durante la medición.

Raw preservado close_work_stryker_replay_attempt3.json, SHA256755BC92683B657EB194E5F77B3119053C28976CDF7571557C206BE0CF851BB50; log close_work_stryker_replay_attempt3.log. Attempt1 y attempt2 permanecen preservados con resultados y limitaciones anteriores. Este cierre acredita los oráculos dirigidos de privacidad, accesibilidad y retry; no cambia el score original86,59%, Reader original78,29%,169supervivientes originales ni losdosRuntimeError. No se ha vuelto a ejecutar la campaña completa y no se anuncia100% global. Sin nuevos refuerzos ni ejecuciones pendientes en este paquete.
