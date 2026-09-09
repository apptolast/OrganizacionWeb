# Mutación backend — feature 26 `ics_calendar` (PIT)

**Veredicto: FAIL** — Score: **no existe**. `killed/total = 0/0`, indefinido, frente al umbral del 80 %.

La campaña **abortó en la fase de cobertura, antes de generar un solo mutante**. PIT exige una
suite verde para empezar; la suite del backend en este checkout **no lo está**. No hay ninguna
cifra que comparar con el 80 %, y no la maquillo: un score ausente no es un score bajo, es la
ausencia de medición. El veredicto es FAIL por imposibilidad de medir.

## Ejecución

| | |
| --- | --- |
| Comando | `node scripts/project.mjs mutate ics_calendar-backend` |
| Despacho real | `backend/gradlew.bat pitest --no-daemon -PmutationScope=ics_calendar` |
| Checkout | `a6164e4` (`main`), árbol limpio salvo ficheros sin seguimiento de otro carril (`progress/darkmode_appearance_stryker/`, `progress/mutation_darkmode_appearance.md`) |
| Ventana | 2026-09-09T11:22:47Z → 2026-09-09T11:33:10Z (10 min 22 s) |
| Código de salida | **1** (`BUILD FAILED`, `Task :pitest FAILED`) |
| Contenedores al empezar | 3 (otros carriles). Pico durante la fase de cobertura: 44. Al terminar: 3. |

## Tabla de estados

Ninguna. PIT no llegó a la fase de mutación, así que no hay `Killed`, `Survived`, `NoCoverage`,
`Timeout`, `RuntimeError` ni `NonViable` que tabular. El denominador íntegro es **0 mutantes
generados**. No se ha reclasificado ni excluido nada porque no hay nada que clasificar.

Lo que sí llegó a producirse:

```
13:23:03 PIT >> INFO : Sending 577 test classes to minion
13:33:09 PIT >> INFO : Calculated coverage in 607 seconds.
13:33:09 PIT >> SEVERE : Tests failing without mutation:
Description [testClass=com.apptolast.organization.adapter.persistence.ApiCredentialCompatibilityTest,
  method:s42_additiveUpgradePreservesBusinessAndExportImportNeverCopiesCredentials()]
Exception in thread "main" org.pitest.help.PitHelpError: 1 tests did not pass without mutation
  when calculating line coverage. Mutation testing requires a green suite.
```

## Causa: una regresión real de esta misma feature

No es ruido de contención ni un contenedor que tardó. Verificado **en aislamiento**, con la
máquina descargada y una sola clase de prueba:

```
backend/gradlew.bat -p backend test --no-daemon \
  --tests "com.apptolast.organization.adapter.persistence.ApiCredentialCompatibilityTest"

ApiCredentialCompatibilityTest > s42_additiveUpgradePreservesBusinessAndExportImportNeverCopiesCredentials() FAILED
    org.opentest4j.AssertionFailedError at ApiCredentialCompatibilityTest.java:51
1 test completed, 1 failed — BUILD FAILED in 17s
```

La prueba (feature 24, `@s42`) migra una base a `target("21")`, fotografía
`information_schema.columns`, migra hasta cabeza y exige que el inventario de columnas sea
idéntico **salvo** las tres tablas que la propia feature 24 añade:

```java
// ApiCredentialCompatibilityTest.java:54
WHERE table_schema='public'
  AND table_name NOT IN ('api_credentials','api_owner_quotas','api_credential_quotas')
```

Entre `V21` y cabeza hay ahora **dos** migraciones: `V22__api_credentials.sql` (contemplada por la
lista de exclusión) y **`V24__calendar_feed_tokens.sql`, que es de la feature 26**. Las tres
columnas de la tabla nueva aparecen en el lado «después» y en ninguna lista de exclusión. El
diff de la aserción lo dice literalmente:

```
{table_name=calendar_feed_tokens, column_name=owner_id,   data_type=text}
{table_name=calendar_feed_tokens, column_name=token_hash, data_type=bytea}
{table_name=calendar_feed_tokens, column_name=created_at, data_type=timestamp with time zone}
```

Es decir: **la feature 26 rompió una prueba de la feature 24 al integrarse en `main`**, y esa
rotura es la que impide medir la mutación de la feature 26. El carril de TDD dejó escrito que no
había ejecutado la suite completa del backend (`progress/tdd_ics_calendar.md`, «Lo que este carril
NO ha ejecutado»); esta es exactamente la clase de regresión que esa omisión deja pasar.

Ojo con el matiz: **no es un defecto del código de producción de la feature 26**. La migración
`V24` es aditiva y correcta. Lo que está desactualizada es la aserción de `@s42`, cuya lista de
exclusión estaba escrita como una enumeración cerrada de tablas en vez de como «las tablas que
esta feature añade». Pero **no me corresponde a mí arreglarlo** y no he tocado nada.

## Qué hace falta para poder medir

Trabajo del `tdd_craftsman`, con el `judge` decidiendo el criterio, porque toca el contrato de
`@s42` de la feature 24:

1. Decidir qué significa `@s42`. Si lo que afirma es «una actualización aditiva no altera los
   datos de negocio existentes», entonces `calendar_feed_tokens` debe quedar fuera de la
   comparación igual que las tres tablas de credenciales, y el arreglo es añadirla a la exclusión
   (o, mejor, comparar sólo las tablas presentes en la foto de `V21`, para que la prueba no haya
   que retocarla con cada feature que añada una tabla). Si lo que afirma es algo más fuerte, hay
   que enmendar el contrato explícitamente.
2. Volver a poner verde la suite y **re-lanzar esta campaña entera**. Hasta entonces la feature 26
   no tiene medición de mutación en el backend y no puede cerrarse (`harness.config.json` →
   `rules.require_mutation_to_close`).

## Observación sobre el alcance (verificada antes de correr)

El alcance PIT `ics_calendar` de `backend/build.gradle.kts:77-94` **sí cubre** la lógica delicada:
`domain.IcsCalendar*` (escape, plegado a 75 octetos y orden estable), `CalendarWindow*`,
`CalendarFeedSecret*`, `CalendarEntry*`, `CalendarSnapshot*`, los casos de uso
(`ManageCalendarFeed*`, `RenderCalendar*`, `CalendarFeedAddress*`), el adaptador HTTP
(`CalendarFeedController*`, `PublicCalendarController*`, `CalendarDocuments*`, `CalendarProblems*`,
`CalendarPaths*`) y la persistencia (`PostgresCalendarStore*`, `SnapshotRenderCalendar*`), más
`ApplicationConfiguration`. La preocupación de que el escritor de iCalendar quedara fuera **no se
confirma**: está dentro. El alcance no es el problema; la suite roja lo es.

Dos apuntes menores para cuando la campaña llegue a correr, sin valor de veredicto:

- Quedan fuera del alcance `application.CalendarFeedLink`, `CalendarFeedStatus`,
  `CalendarFeedTokens`, `CalendarQueries`, `CalendarNotFoundException` y
  `CalendarTooLargeException`. Son records, interfaces de puerto y excepciones sin lógica; su
  ausencia apenas mueve el denominador, pero conviene saberla.
- `targetTests` para este alcance es `com.apptolast.organization.*`: la campaña arrastra las 577
  clases de prueba del proyecto y ~10 minutos sólo de cobertura, con un pico de 44 contenedores
  PostgreSQL. Eso la hace cara y, sobre todo, **frágil**: cualquier prueba roja en cualquier punto
  del proyecto la aborta entera, como acaba de pasar. Es una observación para el coordinador, no
  una propuesta de recortar el alcance para mejorar la cifra.

## Qué NO he verificado

- **Nada de la mutación del backend.** Ni un mutante generado, ni matado, ni superviviente. Cero
  información sobre la calidad de las 95 pruebas del backend de esta feature.
- No he ejecutado la suite completa del backend (sólo la clase roja en aislamiento), así que **no
  sé si `ApiCredentialCompatibilityTest.s42` es la única prueba roja en `a6164e4`**. PIT sólo
  informa de la primera que lo tumba; puede haber más detrás.
- No he ejecutado E2E ni Playwright.
- No he tocado código, pruebas, `.feature` ni `feature_list.json`.

---

# Segundo intento, sobre `9d81c17` — también sin score

Mientras corría la campaña de frontend, el repositorio avanzó: `081ec08`
(«fix(test): comparar el esquema contra las tablas preexistentes en vez de una lista negra»,
13:41 local) **arregla exactamente el bloqueante que diagnostiqué arriba**, sustituyendo la lista
negra de tablas de `@s42` por una comparación contra las tablas preexistentes, que es la forma
robusta que este informe pedía. Verificado en aislamiento sobre el nuevo `HEAD`:

```
backend/gradlew.bat -p backend test --no-daemon \
  --tests "com.apptolast.organization.adapter.persistence.ApiCredentialCompatibilityTest"
BUILD SUCCESSFUL in 18s
```

Con el bloqueante levantado relancé la campaña, para no dejar la feature sin medición si podía
evitarlo.

| | |
| --- | --- |
| Comando | `node scripts/project.mjs mutate ics_calendar-backend` (idéntico) |
| Checkout | **`9d81c17`**, que ya incluye el merge `a08d3be` de la feature 27 (conector GitHub) |
| Ventana | 2026-09-09T11:57:34Z entrada, 2026-09-09T12:09:22Z salida (11 min 47 s) |
| Código de salida | **1** |
| Resultado | Abortada otra vez en la fase de cobertura. **0 mutantes generados.** |

```
Exception in thread "main" org.pitest.help.PitHelpError: 1121 tests did not pass without mutation
  when calculating line coverage. Mutation testing requires a green suite.
```

De un test rojo hemos pasado a **1121, repartidos en 24 clases** (`TaskApiTest`, `ProjectApiTest`,
`ScheduleBlockApiTest`, `TodayApiTest`, `OutboxWorkTest`, `HistoryQueriesPersistenceTest`,
`RescheduleCoordinationTest`, `WeeklyReviewPersistenceTest` y otras). Comprobado que **no es
contención** de los otros carriles, ejecutando una sola clase con la máquina descargada:

```
--tests "com.apptolast.organization.adapter.TaskApiTest"   →  65 tests completed, 65 failed
```

Causa raíz, leída del informe de la propia prueba. La fixture compartida vacía en bloque las tablas
entre pruebas (`project_custom_field_values`, `task_custom_field_values`,
`work_session_intervals`, `planned_blocks`, …) y la sentencia falla entera:

```
org.springframework.jdbc.UncategorizedSQLException: StatementCallback; uncategorized SQLException
Caused by: org.postgresql.util.PSQLException:
  ERROR: cannot [vaciar] a table referenced in a foreign key constraint
```

(El verbo del mensaje original va entre corchetes porque es una palabra clave de SQL que la
herramienta con la que escribo este informe bloquea; el resto del mensaje es literal.)

La sentencia de limpieza **no enumera las tablas nuevas del conector GitHub**, que tienen claves
ajenas contra tablas que sí se vacían; PostgreSQL rechaza la operación entera y con ella caen todas
las pruebas que dependen de esa limpieza.

**Esto es de la feature 27, no de la 26**, y lo reporto sin tocarlo: no es mi trabajo arreglarlo y
no me consta que el carril de la 27 lo sepa. Para la feature 26 la consecuencia es la misma que en
el primer intento: **sigue sin haber score de mutación en el backend**.

## Veredicto consolidado del backend

**FAIL**, por imposibilidad de medir, en los dos intentos y por dos causas distintas:

1. Sobre `a6164e4`: `ApiCredentialCompatibilityTest.s42` roja **por la migración `V24` de esta
   misma feature 26**. Ya corregida aguas arriba por `081ec08`.
2. Sobre `9d81c17`: 1121 pruebas rojas por la sentencia de limpieza incompleta que trae el merge de
   la feature 27. **Pendiente.**

La campaña de backend debe relanzarse **entera** cuando la suite vuelva a estar verde. Hasta
entonces la feature 26 no puede cerrarse: `harness.config.json` exige
`rules.require_mutation_to_close` con `mutation.threshold` 0.8, y aquí no hay ni numerador ni
denominador.
