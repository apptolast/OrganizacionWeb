# Seguimiento TDD API schedule_block tras mutación

2026-09-06. Autor resume_review; Ponytail full/Caveman lite. Root autoriza sólo API test y mínimos arreglos API ante RED real; frontend conserva UI/shared. Ejecuciones focales coordinadas: frontend está documental readonly y cede vitest focal y formato específico. Sin Gradle ni Stryker. Base190 API casos; no se autoaprueba este seguimiento. Las18 candidatas de equivalencia aceptadas condicionalmente por root no generan exclusiones ni cambios de score.

Cada ciclo añade un comportamiento y ejecuta `pnpm exec vitest run src/schedule-block-api.test.ts -t <nombre>` desde frontend. Los skipped de estos filtros son casos deliberadamente no seleccionados; no una regresión completa.

| Ciclo | Comportamiento | Primera ejecución |
| --- | --- | --- |
|1|Página terminal20 DTO distintos|1GREEN13207b|
|2|Objetivo500 Unicode en listado/preview|2GREENaf9140|
|3|ETag versiones1,10,100,MAX_BIGINT|4GREEN4945b2|
|4|Opciones ±18:00 añadidas a matriz positiva existente|6GREEN8fc8c2 (2nuevos)|
|5|Instantes con .000Z como segundos enteros|1GREEN (salida del ciclo5)|

Todos inicialmente verdes; ninguna producción cambiada ni RED fabricado. Transporte se simula mediante fetch/Response.json como en la suite existente; las aserciones verifican protocolo aceptado/rechazado por puertos públicos del API.

Ciclo5 salida exacta8910a2. Ciclo6 problemas especializados code/status/bodyStatus/extra,16GREENcac362. Ciclo7 matriz offsets añade4formas incompatibles (2errores,codeajeno,fieldajeno,array-like),20GREEN1959ed. Ciclo8 contenedor presupuesto zona vacía/ausente y days objeto/null,4GREEN1bba77. Ciclo9 mezcla errores válido+incompatible,21GREEN199882 (1nuevo). Ciclo10 colección presupuesto mezcla de excesos aceptada/inválido segundo/orden inverso/duplicado rechazados,4GREEN (salida del ciclo10). Todos inicialmente verdes sin producción. Matrices validan null exacto en problemas incompatibles; no excepción genérica que enmascare cambios.

Ciclo10 salida exacta71c726. Ciclo11 amplía rechazo DTOlistado con UUIDarray/prefijo/sufijo,objetivo501/whitespace,duraciónincoherente y fracción.100Zenambosextremos:20GREEN5d78a2 (7nuevos). Ciclo12 offsets array/prefijo/sufijo:24GREEN6ed0b8 (3nuevos). Ciclo13 fecha presupuesto array:5GREEN1df265 (1nuevo). Ciclo14 ETagarray/prefijo/sufijo/negativo/ceroinicial:41GREEN534182 (5nuevos). Ciclo15 endOffsetexplícito incompatible antes de POST:4GREEN7a9c5d (1nuevo),sinfetch. Ciclo16 duraciónpreview59 y suma3540 frente instantes60:42GREEN86a2e0 (1nuevo). Ciclo17 locales separadorespace/fecha normalizablefeb30 conrespuestaUTCcoherente:2GREEN008c6a. Ciclo18 varioswhitespacesUnicode recortados en input:1GREEN (salida del ciclo18).

Todos los ciclos inicialmente verdes. No hubo RED de producción, no se alteró schedule-block-api.ts ni se ejecutó Stryker. Los casos .100Z y duración59 mantienen las otras invariantes para no enmascarar la guarda probada; id array se prueba en listado, sin el sameId adicional de detalle. Pendiente formato focal, tipos/lint focal y regresión completa de la suite API, después freeze para root.

Cierre de autoría para revisión (no done de feature): ciclo18 salida2d7b08. Formato focal17b6df, ESLint focal7049b0, TypeScript global3184c6 y regresión de la suite API completa **250/250 PASS,0omitidos,EXIT0bd27c5**. Incremento60 casos respecto a190; ninguna producción modificada. FuenteAPI conserva SHA256 BFC93601AC6EFB2C9367604CA4D3AED76D1E6278AB7BA417C979DFA6C8BEBAAB. Freeze del test y documento entregado a root; no Stryker ni exclusiones por equivalencias. La eliminación de mutantes se comprobará en medición posterior, no se deduce de GREEN.

Mapa de las seis matrices propuestas: fronteras ciclos1–5; problemas cerrados6–8; colecciones mixtas9–10; coerción/canonicalidad11–14; tiempo/intención11+15–17; objetivo normalizado2+11+18. Los mismos helpers/input/preview/block y matrices negativas existentes se reutilizan. Las equivalencias aceptadas condicionalmente por root permanecen sin tests artificiales de excepciones internas.
