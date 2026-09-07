# TDD backend18 — historial propio

## 1. Nominal compilable @s1

ReadHistoryTest.s1_returnsTheOriginalSessionStartWithCurrentContextLabels exige consulta por owner, filtros y cursor completos, con SessionStart original conservado y etiquetas actuales. RED de compilación real ff11af por tipos/puertos ausentes; GREEN1f83d9 con caso de uso y frontera real. No PG/HTTP aún; mock sólo del puerto de lectura. HistoryEntry<D> conserva el tipo del detalle existente al construir cada entrada y la página heterogénea usa HistoryEntry<?>; no DTO HTTP en dominio, ObjectMapper ni dependencia de infraestructura.

Formato: 2e010d ignoró glob relativo;2a400e con rutas absolutas sólo informó IS DIRTY por el hook IDE, no aplicó. SpotlessJavaApply real84183d aplicó formato y foco ReadHistoryTest **1/1 GREEN93bad2** después. No se presenta el hook anterior como formato aplicado. No test global/init repetido.

Primer bundle compilable congelado en history_first_bundle.json: ocho fuentes application y un test. ReadHistoryUseCase.list(owner,HistoryFilters,HistoryCursor) devuelve HistoryPage; Queries.list misma entrada devuelve List<HistoryEntry<?>>. Filters(category,projectId,taskId,from,to); Cursor(owner,filters,upper,after); Position(occurredAt,type,id). El HTTP de C valida sintaxis; propiedad y luego semántica del cursor se completarán en adaptador PG. Primer caso terminal únicamente: paginación21 y cincofuentes/validación/RR/errores pendientes, no feature completa ni stubs PG.

Fuentes del bundle quietas para revisión/commit selectivo root. No Git mutado por autor, migración ni consumer. C avisado de firmas reales; no debe copiar archivos en vuelo.
