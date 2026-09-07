# Propuesta de replay dirigido de historial18

Pendiente revisión/autorización root. No se ha ejecutado ni editado configuración global.

Seis firmas originales y sus índices XML en history_pit_reinforcement_targets.json, SHA C871807A1234FAF8C756C90F71B2834661D94E9B7E2237BE01A944C393E363E9. Rangos originales de una línea: ReadHistory 15; PostgresHistoryQueries 215,229,258,286,300. No se propone que un selector por método equivalga a selector exclusivo de líneas.

Mecanismo mínimo: init script separado siguiendo close_work_boundary_replay.init.gradle; targetClasses sólo ReadHistory y PostgresHistoryQueries; mutators CONDITIONALS_BOUNDARY, EMPTY_RETURNS, TRUE_RETURNS, MATH. Mantener todos los candidatos JUnit, cuatro workers, umbral80 y configuración temporal/heredada. ReportDir propio reports/pitest-history-replay. Comando posterior de arnés/Gradle con -PmutationScope=history --init-script separado, sin tocar build global.

Para limitar métodos de estas dos clases: excluir explícitamente <init>, blockContext, compare, instantInRange, lambda$compare$0, lambda$list$0, lambda$read$0, position, rank, read, sameFact, sourceMatches, validBefore, validClosure, además de equals/hashCode/toString heredados. Quedan list, lambda$compare$1, sessionContext y validTransition. La lambda$list$1 de ReadHistory pertenece al stream: si genera un mutante de los tipos seleccionados, inventariarlo como extra; no depender de una renumeración opaca para afirmar selección exacta.

Proyección desde XML original (d0137c): 17 mutantes, seis objetivos y once extras; no asegurar el número antes del XML final. PG list1, lambda compare1, sessionContext1, validTransition13 y ReadHistory list1. Al terminar exigir correspondencia clase/método/línea/mutador/índice de las seis firmas originales y listar todos los extras, errores y ausencias. No sustituir el score original por el del subconjunto ni afirmar muertos sin medir.

Freeze de tests: history_reinforcement_freeze.json; ReadHistoryTest SHA5740DE1E394741A8E157ED840EB66658F58F4F714D35353B999DD3D1F3FEA2F6, PG SHA B22F856B8F77E569D7F5E9A8C44032DF57883C879D677B8E87B8D0D68FB3CB94. Antes de ejecutar se capturará un manifiesto nuevo con los mismos inputs y los dos cambios declarados. Regresión 61/61 y formato verdes; nada de producción cambiado.

Los otros 15 residuos originales conservan estado y análisis en mutation_history_backend.md. No equivalencias nuevas ni objetivo100%.
