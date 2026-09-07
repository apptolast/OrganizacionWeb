# TDD backend apariencia20

Ponytail full/Caveman lite. Init root25384 EXIT0/14625f heredado para este corte. No HTTP, frontend ni cambios de migraciones previas.

## Primer bundle nominal, no cierre funcional

1. @s1 ReadAppearanceTest.s1_readsConfirmedAbsenceForTheAuthenticatedOwner. Incidente81bf7d: ruta backend/backend errónea, no se creó test y runner no encontró test; NO RED contractual. Corregida ruta. RED real a6e81a (clase ausente), progress/appearance_01_red_actual.log → GREENc70bb7, appearance_01_green.log.
2. @s2 ReadAppearanceTest.s2_readsTheStoredValuesAndRevisionWithoutChangingThem. RED6607ae (record sin campos)→GREEN87925d (2/2); logs appearance_02_red/green.log. Resultado Optional real, sin Clock ni defaults fingidos en dominio; HTTP mapeará ausencia.
3. @s2 SaveAppearanceTest.s2_createsTheFirstPreferenceWithCanonicalColorsAndMicroseconds. RED1b6abe (tipos ausentes)→GREENbd03e9. Campos exigidos por test, UUID propio, versión0, mayúsculas, instante truncadoµs. Puerto callback sin DB simulada como evidencia PG.
4. @s6 SaveAppearanceTest.s6_rejectsARevisionWhoseIdentityDoesNotMatchTheOwnersRow. RED92ab2a (excepción ausente)→GREENf6eda8 conjunto4/4. Excepción pública real para HTTP412.

Formato focal y regresión fc1b25 (logs appearance_bundle_format/green.log); regresión UP-TO-DATE del mismo corte, no cuatro ejecuciones adicionales. Manifiesto progress/appearance_first_bundle.json contiene9fuentes+2tests.

Firmas: ReadAppearanceUseCase.get(String owner)→Optional<Appearance>; SaveAppearanceUseCase.execute(String owner, AppearanceRevision expected, String theme, String accentLight, String accentDark)→Appearance. Appearance(id UUID,owner String,theme String,accentLight String,accentDark String,version long,updatedAt Instant); AppearanceRevision(UUID id,long version). AppearanceConflictException local412; ValidationException/StorageUnavailableException existentes se reutilizarán.

Pendiente explícito: theme/formato/contraste y errores ordenados; comparación versión/ausencia, no-op y cambios existentes, overflow/Clock/rango; Store PG/concurrencia/rollback; V19 y wiring. SaveAppearance actual sólo es nominal de alta y guarda identidad de fila presente; NO usarlo aún como implementación completa20. No stubs HTTP ni fuente de persistencia parcial en este bundle. Root revisará/commiteará checkpoint para C antes de continuar estos archivos.

## Validación independiente mientras root conserva el primer bundle

Archivos nuevos AppearanceValues/AppearanceValuesTest; ninguno de los11archivos congelados fue modificado.
5. @s10 tema inválido antes de ambos acentos: REDcd130a→GREEN7f4d65.
6. @s10 formato claro antes de oscuro: REDbfc635→GREEN9e954e.
7. @s11 contraste claro pese a DARK: RED9b96b1→GREEN1a067f.
8. @s10 formato oscuro tras claro válido: RED87d007→GREENf29230.
9. @s11 contraste oscuro pese a LIGHT: RED1745d0→GREENd5a5c5.
10. @s2 defaults minúsculos canónicos y válidos contra los fondos reales: RED0d53a3→GREENdb62e2 (6/6dominio). Logs appearance_05..10_red/green.log. Fórmula y seis fondos de cada tema implementados sólo en dominio, aún sin conectarlos al caso de uso congelado. No se atribuye validación HTTP/PG todavía.
