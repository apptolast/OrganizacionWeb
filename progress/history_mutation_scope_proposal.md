# Alcance de mutación de historial18

Propuesta para revisión, sin configuración ni campañas ejecutadas. Corte aislado con PG `6d5906d`, wiring `71585ee`, cliente `adbf189` y HTTP aprobado `5578514`. El diff final de A/B sigue pendiente; revisar la lista contra ese freeze antes de ejecutarla.

## PIT

Nuevo selector `history-backend`, despachado por `node .harness/harness.mjs mutate history-backend` al comando existente de Gradle `pitest -PmutationScope=history`. Hoy el dispatcher no reconoce ese target: primero se añadirá mediante su test de selección y validación de DSL, sin ejecutar una campaña al probar el despacho. No usar el scope17 como gate18.

Lista explícita propuesta, con módulos nuevos completos:

- `com.apptolast.organization.application.ReadHistory`
- `com.apptolast.organization.application.ReadHistoryUseCase`
- `com.apptolast.organization.application.HistoryQueries`
- `com.apptolast.organization.application.HistoryEntry`
- `com.apptolast.organization.application.HistoryFilters`
- `com.apptolast.organization.application.HistoryCursor`
- `com.apptolast.organization.application.HistoryPosition`
- `com.apptolast.organization.application.HistoryPage`
- `com.apptolast.organization.adapter.persistence.PostgresHistoryQueries*`
- `com.apptolast.organization.adapter.http.HistoryController*`
- `com.apptolast.organization.adapter.http.HistoryCursorCodec*`
- `com.apptolast.organization.adapter.config.ApplicationConfiguration`

Los asteriscos incluyen clases internas propias, no familias históricas. Interfaces y records sin lógica pueden no generar mutantes; su inclusión no se presenta como cobertura medida. ApplicationConfiguration conserva el archivo completo para incluir sus dos factories nuevas, sin excluir métodos heredados. No se añaden a la lista validadores/DTO de14–17 sólo por reutilizarlos: no han cambiado para18. Si el delta final modifica lógica compartida, se incorpora antes del gate.

Conservar todos los JUnit disponibles, 4 workers, umbral80, mutadores/timeout vigentes, `-FRECORD` y exclusión existente de equals/hashCode/toString. Salida separada `backend/build/reports/pitest-history`, XML/HTML y log propios; before/after de producción, pruebas y configuración. No recortar validaciones, RR, cursor, owner o mapeos por coste o supervivientes. PIT no acredita por sí solo predicados SQL: la regresión PG real documentada por A es complementaria.

## Stryker, después del freeze UI

No se fijan rangos ahora. Los módulos nuevos `history-api.ts` y `history.tsx` entrarán completos; cualquier nuevo helper propio también. App/Workspace y detalles de proyecto/tarea recibirán rangos de nodos añadidos y sus guardas sólo si su delta final se limita a rutas/enlaces. Si cambian lógica de navegación o privacidad compartida, incluir toda la lógica afectada. Validación del decoder nunca parcial.

Mantener base Vitest completa, perTest, 8 workers, umbral80 e ignorePatterns protegido. Report/temp18 separados. Prevalidar los rangos finales con el parser instalado (columna1 de informes frente a columna0 del selector), guardar archivo/hash/rango y motivo; no repetir la matriz heredada completa de17 ni declarar equivalentes por intención. No hay configuración final Stryker antes de revisar el diff UI.
