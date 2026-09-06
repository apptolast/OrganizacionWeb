# V13 — integridad aditiva, TDD individual

Autoría aislada tras checkpoint E2E local `8e91436`, no publicado. El único E2E13 sigue RED por integración backend pendiente y no se modifica. Baseline init `f4721d`:1444backend/1495frontend/18scripts GREEN; no se repite init. Diseño aprobado por root en `review_reschedule_migration_design.md` del árbol backend; sólo nueva V13, RescheduleMigrationTest y esta bitácora.

Las pruebas usan PostgreSQL17.9 Testcontainers y Flyway reales, esquema único por caso, migración inicial hasta12. No Spring context, pooling adicional, procesos globales ni cambio del build del autor core. V11/V12 se conservan. Ponytail full/Caveman lite; un caso nuevo y ejecución antes del siguiente.

1. Revisión de proyección cero: RED `a7fb1a/326452` (1fallo porque no se rechazaba). V13 añade sólo CHECK version>0. GREEN `39458d`,1test.
2. Cancelación legada metadata-only: test de upgrade conserva fila original, proyección completa idéntica con ochoNULL y ausencia de recibos/outbox nuevos. Ejecución en curso; no se anticipa resultado.

Trabajo parcial: no entregar esta migración hasta completar constraints y pruebas de compatibilidad/rollback aprobadas. Sin commits ni mutación.

2. Cancelación metadata-only inicialmenteGREEN d9a75a (1caso), sin producción nueva.
3. Sólo después se añade fila planned/revisiónmáxima al mismo test de compatibilidad: inicialmenteGREEN5dac9b (2casos), sin producción nueva.
4. Estado de proyección desconocido: RED4e9f4a/7ee3bd (1fallo); CHECK status enum añadido. GREEN pendiente.

4. Estado desconocido GREENe4cdce (1test).
5. Intervalo parcial: RED8e6a5a; CHECK num_nonnulls nativo en(0,8), sinNOTNULLindividual. GREEN8b7690 (nuevo+2legados,3tests).
6. Único caso duración distinta de instantes: en ejecución. El helper inserta intervalo completo válido desde creación para aislar la violación; no se modifican hechos originales.

6. Duración incoherente RED372c6d→GREENe4794c; CHECK diferencia=make_interval heredado11.
7. Intervalo0min RED960bbb→GREENca3a12; CHECK BETWEEN1AND1440 heredado11. Positividad y diferencia exacta implican inicio<fin sin constraint duplicada.
8. Nuevo caso individual fracción de segundo conserva60min reales pero desplaza ambos instantes; ejecución en curso.

8. Segundos fraccionarios RED89a393→GREEN78e350; date_trunc heredado11.
9. Año resuelto10000 REDc64b34→GREENd90ad0; límites de instante heredados11.
10. Único caso segundos locales actualmente en ejecución. No se añaden casos futuros antesdeGREEN.

10. Segundoslocales RED3a168c→GREEN8d130f; precisión minuto heredada11.
11. Añadido después año local10000: RED39c493→GREEN9c7837; rangos locales heredados11.
12. Nuevo caso zona vacía pendiente; no catálogo ni aliasados.

12. Zona vacía REDe0c68a→GREEN3efe3a; btrim no modifica el valor, sólo exige contenido sin consultarTZDB.
13. Revisión de cambio0 REDb63e22; se reutiliza recibo interno serializado con version y PlannedBlock request/time, noDTOHTTP. Guarda positiva añadida, GREENpendiente.

13. Revisión de cambio GREEN0f3679 (1 test).
14. Tipo desconocido RED67e6bd (UPDATED aceptado); añadido CHECK de los dos tipos del contrato, GREEN en curso.

14. Tipo desconocido GREEN4263ea (1 test).
15. Dos cambios para la misma revisión: RED25037f; el segundo INSERT usaba otra clave de petición y se aceptaba. Añadida UNIQUE(block_id,version); verificación en curso.

15. Unicidad de revisión GREEN68dd1b (1 test).
16. Contexto ajeno: RED7ce277/22d7de. Dos FK independientes permitían vincular otra tarea válida al bloque. Añadida FK compuesta y UNIQUE de soporte en planned_blocks, sin UPDATE; GREEN en curso.

16. FK contextual GREEN1a4284 (1 test).
17. JSON no objeto: RED8c5131 al aceptar []; añadida guarda jsonb_typeof nativa. No valida el DTO HTTP dentro del recibo interno ni añade parser. GREEN en curso.

17. JSON objeto GREENf7db39 (1 test).
18. Upgrade sin proyección: inicialmente GREEN3e9b69; se conserva el bloque y no se hace backfill ni se escriben recibos/outbox.
19. Preservación de proyección cancelada completa y recibo interno: caso en ejecución, sin SQL nuevo.

19. Cancelación completa y recibo interno: inicialmente GREEN945963.
20. Movimiento con locales 02:45→02:15 y offsets +02:00→+01:00 (30 minutos reales), zona histórica fuera de catálogo: inicialmente GREENdd20fd. Original, proyección y recibo RESCHEDULED se conservan idénticos; sin outbox nuevo. No se impone orden local.
Pausa de build tras este GREEN para copia de producción backend coordinada por root; no modifica V13 ni esta suite.

21. Tras snapshot local d3ffecf, rollback integral inicialmente GREEN54a2a0. Se carga V12 con un recibo no objeto: falla la última constraint V13. Las constraints anteriores, el historial Flyway y todas las filas quedan idénticos; no se escribe outbox. No se simula Flyway ni PostgreSQL.
Refactor en GREEN: imports explícitos y formato focal mediante Google Java Format 1.31.0 ya instalado (6b211a), sólo RescheduleMigrationTest. Estado81982c confirma únicamente los tres archivos nuevos propios; V11/V12 y las fuentes copiadas no se modificaron. Regresión de migración y persistencia heredada en curso.

## Entrega para revisión independiente

Regresión final `4a9c9a`, EXIT 0: `RescheduleMigrationTest` y `ScheduleBlockPersistenceTest`. XML `a69d6f` confirma **59 tests: 21 de migración y 38 de persistencia heredada**, cero fallos, errores u omitidos. Se ejecutó sobre el snapshot Java aislado `d3ffecf`; ese checkpoint no debe publicarse ni integrarse completo porque contiene trabajo parcial de otros autores.

La migración añade únicamente constraints y un índice único de soporte. Mantiene V11/V12, las filas originales, las proyecciones metadata-only de ambos estados y los recibos internos. Las pruebas de upgrade incluyen cancelación completa, movimiento con locales invertidos y zona histórica, ausencia de backfill y rollback integral al fallar una guarda tardía. No se consulta TZDB ni se interpreta el recibo como DTO HTTP. Las operaciones y carreras de Store permanecen en la pista de su autor.

Formato focal aplicado con GJF 1.31.0. La comprobación `9fb8e5` no imprimió diagnósticos: su EXIT 1 correspondió a `git diff --no-index` de archivos nuevos. Comprobación explícita posterior `a69d6f` confirmó ausencia de whitespace inválido; no se rebajó ninguna regla.

Hashes SHA-256 de fuentes congeladas (`888634`):

- `backend/src/main/resources/db/migration/V13__block_change_integrity.sql`: `652E9253B79D7179E88AF8F1D0DE4F36548174D69905364A3C3B2656CA23E982`.
- `backend/src/test/java/com/apptolast/organization/adapter/persistence/RescheduleMigrationTest.java`: `83BEEB8FE654B3289746442D7C852F15028FB7DA4B227916646284C22532E4A0`.

Sólo estos dos archivos y esta bitácora forman la entrega SQL. Sin commits, mutación ni nuevas ejecuciones E2E; el E2E13 del checkpoint `8e91436` conserva su RED por integración pendiente. No se declara cierre de feature13 ni aprobación propia de esta migración.

GJF dry-run aislado final: 401312, EXIT 0, sin cambios pendientes.
