# Propuesta de alcance PIT de apariencia

Corte inspeccionado: 4567520 más adaptador HTTP propio, pendiente de revisión.
No se modifica configuración ni se ejecuta campaña con este documento.

Añadir selector `appearance-backend` al dispatcher, que invoque el mecanismo
existente con `-PmutationScope=appearance`. En el DSL, lista explícita completa:

```text
com.apptolast.organization.application.ReadAppearance
com.apptolast.organization.application.ReadAppearanceUseCase
com.apptolast.organization.application.SaveAppearance
com.apptolast.organization.application.SaveAppearanceUseCase
com.apptolast.organization.application.AppearanceQueries
com.apptolast.organization.application.AppearanceEditing
com.apptolast.organization.application.AppearanceConflictException
com.apptolast.organization.domain.Appearance*
com.apptolast.organization.adapter.persistence.PostgresAppearanceStore*
com.apptolast.organization.adapter.http.AppearanceController*
com.apptolast.organization.adapter.config.ApplicationConfiguration
```

Son once patrones; el de dominio incluye Appearance, AppearanceRevision y
AppearanceValues. Los puertos sin lógica pueden no generar mutantes, pero no
se excluyen módulos nuevos. Se cubren helpers, contraste, guardas y wiring.
La migración V19 requiere sus pruebas PostgreSQL reales; PIT no muta SQL.
ApiErrors y seguridad se reutilizan sin cambios y tienen oráculos HTTP de
regresión; no se añade una campaña heredada completa sobre esos módulos.

Conservar todos los JUnit como candidatos (`com.apptolast.organization.*`),
cuatro workers, umbral 80, mutadores y límites temporales actuales. Salida
separada `backend/build/reports/pitest-appearance`; no sobrescribir informes
anteriores. El alcance por defecto deberá incluir los módulos nuevos y sus
tests HTTP/PG/wiring, siguiendo el arreglo ya aplicado para Historial.

Validar dispatcher mediante su prueba existente, revisar diff final y
congelar hashes antes de lanzar
`node .harness/harness.mjs mutate appearance-backend`. Conservar XML original,
todos los estados y denominador, inventario de residuos y hashes posteriores.
No excluir supervivientes ni bajar umbral. La revisión de root precede a
cualquier cambio del DSL o campaña.
