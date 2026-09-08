# Refuerzos frontend de integración API

Autorización root: cinco grupos públicos tras original 997, sin cambiar fuentes ni pruebas originales. Se añadieron sólo `integration-api.mutation.test.tsx` y `integration-api-client.mutation.test.ts`.

| Ciclo | Conducta | Resultado inicial |
| --- | --- | --- |
| 001 | Seleccionar y retirar permiso; elegir 7 y después 90 días; PUT exacto | 1 GREEN |
| 002 | 81 puntos con espacio interior rechazados; 80 con Unicode lateral múltiple aceptados | 1 GREEN |
| 003 | Metadata de seis scopes canónicos y nombre de 80 emojis | 1 GREEN |
| 004 | Scopes vacíos, ajeno, duplicados y fuera de orden | 4 GREEN |
| 005 | Nombre de metadata de 81 puntos rechazado | 1 GREEN |
| 006 | Clipboard exacto sólo tras gesto, anuncio y sin secreto persistido | 1 GREEN |
| 007 | 400/409 INVALID/LIMIT/CONFLICT con status o type incoherentes conservan incertidumbre e id | 6 GREEN |

No hubo RED productivo: la implementación ya satisface estos nuevos oráculos. Cada ciclo se ejecutó antes del siguiente; los logs y EXIT originales `integration24_refinement_001_initial` a `007_initial` conservan resultados focales con omisiones por filtro donde corresponde. Formato, TypeScript y ESLint pasaron. Foco final: 83/83 en cinco suites, incluidos los originales, EXIT 0. No se ejecutó todavía la campaña correctiva ni se atribuyen muertes a estos tests antes de medirlas.

La configuración funcional y el universo de 997 mutantes siguen iguales. El cambio separado de `.dockerignore` sólo excluye el scratch del contexto Docker, autorizado después de preservar el original. Revisión independiente y validación global pendientes antes de campaña.