# Limpieza de fixtures sin enumerar tablas

## Diagnóstico confirmado (no el mío previo)

El primer diagnóstico que se me entregó —52 contenedores, caché de contextos de
Spring desbordada, agotamiento de recursos— **era falso**. Lo confirmo en
negativo, con la ejecución en aislamiento que se me pidió:

```
backend/gradlew.bat test --no-daemon --tests "com.apptolast.organization.adapter.TaskApiTest"
-> 65 tests completed, 65 failed        (un solo contenedor, sin contención)
```

Y el error exacto, extraído del XML de resultados:

```
org.springframework.jdbc.UncategorizedSQLException:
  StatementCallback; uncategorized SQLException for SQL [TRUNCATE project_cust...]
Caused by: org.postgresql.util.PSQLException:
  ERROR: cannot truncate a table referenced in a foreign key constraint
  Detail: Table "task_external_links" references "tasks".
  Hint: Truncate table "task_external_links" at the same time, or use TRUNCATE ... CASCADE.
```

La causa es la que se me indicó en la corrección: `V25__github_connector.sql`
añadió dos tablas con clave ajena hacia tablas que las fixtures ya truncaban:

```sql
CREATE TABLE issue_import_receipts ( ... project_id UUID NOT NULL REFERENCES projects(id) ... );
CREATE TABLE task_external_links   ( ... task_id    UUID NOT NULL UNIQUE REFERENCES tasks(id) ... );
```

Las fixtures truncan `projects` y `tasks` **sin `CASCADE`** y sin nombrar las
tablas nuevas, así que PostgreSQL rechaza la orden entera. El fallo está en la
línea del `TRUNCATE` del `@BeforeEach`, por eso cae la clase completa. Los
`Connection ... refused` del log de CI son secuela posterior, no origen.

## Diseño

Un único punto en `backend/src/test/java/com/apptolast/organization/support/`
que **no enumera tablas**: las descubre en `information_schema` y las vacía
todas juntas con `RESTART IDENTITY CASCADE`, excluyendo `flyway_schema_history`
(si se borrara, la clase siguiente reejecutaría migraciones o fallaría).

Alcance de la sustitución: **sólo las limpiezas que vacían la base entera**
(las listas de `TRUNCATE` escritas a mano y las dos listas equivalentes de
`DELETE FROM` sin filtro). Se dejan intactos los borrados quirúrgicos por
propietario (`DELETE ... WHERE owner_id=...`), que expresan otra intención
—«limpio mis filas»— y cuyas clases acumulan estado a propósito entre pruebas.

## Ciclos rojo-verde-refactor

**Ciclo 1 — el punto único.**

- ROJO: `TestDatabaseTest.emptiesATableThatArrivedAfterTheFixturesWereWritten`
  crea una tabla con clave ajena hacia `tasks` *después* de escritas las
  fixtures, la llena y exige que la limpieza la vacíe. No compilaba (`cannot
  find symbol: TestDatabase`), que es fallar. Segunda prueba:
  `keepsTheFlywayHistorySoTheNextClassDoesNotMigrateAgain`.
- VERDE: `TestDatabase.empty(JdbcTemplate)`, que consulta
  `information_schema.tables` y emite un solo
  `TRUNCATE ... RESTART IDENTITY CASCADE` sobre todo `public` menos
  `flyway_schema_history`.
- REFACTOR: la consulta a constante con nombre, y el nombre de la tabla de
  historial a constante.

Commit: `1d03160`.

**Ciclo 2 — sustituir las listas.**

- ROJO de partida: `TaskApiTest`, 65/65 en rojo (ver evidencia 1).
- VERDE: las 29 clases llaman a `TestDatabase.empty(jdbc)`.

Commit: `e3c08a3`.

## Acoplamientos ocultos encontrados

Ninguno. Se buscaron expresamente clases que dependieran de que algo
**sobreviviera** entre pruebas, porque al unificar todas limpian más:

- Las 25 clases con lista de `TRUNCATE` ya vaciaban el esquema de negocio
  entero en cada prueba; las tablas que su lista no nombraba estaban vacías de
  todos modos, porque cada clase tiene hoy su propio contenedor. La limpieza
  nueva reproduce exactamente el mismo estado de partida.
- `CalendarPersistenceTest` y `GithubConnectorPersistenceTest` hacían lo mismo
  con `DELETE` sin filtro, y `CustomFieldValuesPersistenceTest` con una lista
  recorrida en bucle. Mismo razonamiento, mismo resultado.
- `AppearancePersistenceTest` sólo vaciaba `appearance_preferences`. Se revisó
  si escribía en `public`: su única inserción en `projects` ocurre dentro del
  esquema aislado `appearance_upgrade`, que la limpieza no toca. Unificarla es
  inocuo, y se verificó en verde.

**Lo que se dejó a propósito sin tocar**, porque expresa otra intención y no
arrastra el mismo vicio:

- Borrados quirúrgicos por propietario (`DELETE ... WHERE owner_id=...`) en
  `ApiCredentialHttpPersistenceTest`, `ApiCredentialBusinessCompatibilityTest`,
  `ExportSocketTest`, `ImportScaleTest`, `ImportSocketTest`,
  `CustomizationHttpPersistenceTest`, `ExportDataHttpPersistenceTest`. Esas
  clases acumulan estado entre pruebas a propósito.
- Borrados de una sola tabla **dentro** de una prueba, que son parte del
  escenario y no de la fixture: `DELETE FROM availability_preferences` para
  ejercitar la rama «sin configurar» en `ScheduleBlockApiTest` y `TodayApiTest`,
  y los `outbox_events` de `TaskHistoryApiTest`, `TaskStatusApiTest`,
  `WorkSessionStoreTest` e `HistoryQueriesPersistenceTest`.
- `CustomizationPersistenceTest` y `CustomizationWiringTest`, que borran sólo
  `customization_preferences` con `DELETE`. `DELETE` no tiene el problema del
  `TRUNCATE`: falla sólo si hay filas que de verdad la referencian, y entonces
  el fallo es una señal legítima.

## Evidencias

**1. El rojo de partida, reproducido en aislamiento (antes de tocar nada).**

```
backend/gradlew.bat test --no-daemon --tests "...TaskApiTest"
65 tests completed, 65 failed
org.postgresql.util.PSQLException: ERROR: cannot truncate a table referenced in a foreign key constraint
  Detail: Table "task_external_links" references "tasks".
  Hint: Truncate table "task_external_links" at the same time, or use TRUNCATE ... CASCADE.
```

**2. Esa misma clase en verde, después.**

```
backend/gradlew.bat test --no-daemon --tests "...TaskApiTest"
BUILD SUCCESSFUL   (65 pruebas, 0 fallos)
```

Nótese que son **las mismas 65** antes y después: la clase no pierde pruebas.

**3. Las clases cuya limpieza se ensanchó más, en verde.**

`AppearancePersistenceTest`, `CustomFieldValuesPersistenceTest`,
`CalendarPersistenceTest`, `GithubConnectorPersistenceTest`,
`WorkSessionStoreTest`, `WeeklyReviewPersistenceTest` → `BUILD SUCCESSFUL`.

**4. Ninguna aserción cambia.** El diff de las 30 clases no toca ni una
declaración de prueba:

```
git diff -U0 | grep -E "^[+-].*(@Test|@ParameterizedTest|@ValueSource|@CsvSource|@EnumSource|void s[0-9])"
(vacío)
```

De ahí se sigue el conteo: el número de pruebas descubiertas no puede cambiar
salvo por las 2 nuevas de `TestDatabaseTest`.

**5. Suite completa en verde.**

```
backend/gradlew.bat cleanTest test --no-daemon
BUILD SUCCESSFUL in 11m 24s
clases=174  tests=3824  failures=0  errors=0  skipped=0
```

Conteo: **3822 antes → 3824 después**. Las dos de diferencia son exactamente
las dos pruebas nuevas de `TestDatabaseTest` (comprobado en su XML:
`tests="2"`). Ninguna clase pierde pruebas; la evidencia 4 lo respalda: el diff
no toca ninguna declaración de prueba, así que el conjunto descubierto por
JUnit no puede haber encogido. `TaskApiTest` lo ilustra en concreto: 65 antes
(las 65 rojas) y 65 después (las 65 verdes).

**6. Orden de clases.** Advertencia honesta: Gradle ejecuta cada clase con su
propio procesador y **no** aplica `ClassOrderer`, así que no se pudo forzar el
orden inverso ni con `-Djunit.jupiter.testclass.order.default` ni con un script
de inicio que lo reenviara al JVM de pruebas (se probó: el orden no cambió).
Además, en este commit **cada clase sigue teniendo su propio contenedor**, así
que dos clases no pueden verse los datos aunque se ordenen al revés: la
demostración de orden sólo se vuelve concluyente cuando entre el contenedor
compartido. Lo que sí se comprobó: la pareja `TaskApiTest` +
`TaskStatusApiTest` en verde junta en un mismo JVM y cada una por separado, con
idéntico número de pruebas (65 y 80).

## Trabajo aparcado: el contenedor único

Sigue siendo buena idea, pero no era lo que rompía la CI. El avance que llevaba
hecho (una clase `SharedPostgres` con contenedor único sin `@Testcontainers`,
una base `SpringDatabaseTest` con un solo `@DynamicPropertySource`, y 8 clases
de persistencia ya convertidas) **no se ha tirado**: está guardado en el
directorio de trabajo de la sesión, en `container-wip/` (los dos ficheros de
`support/` y un parche de 395 líneas con las 8 clases). No se ha commiteado
porque estaba a medias y no compilaba suelto.

Dato medido de paso, que sirve para dimensionar ese trabajo cuando se retome:
la suite completa tarda **11m 24s** y llega a tener **22 contenedores** vivos a
la vez en esta máquina.

Un detalle técnico que conviene no perder, porque no es obvio y condiciona el
diseño: hoy **ninguna** clase de Spring puede compartir contexto con otra,
aunque tengan `@SpringBootTest(properties=...)` idénticas, porque cada una
declara su propio método `@DynamicPropertySource` y
`DynamicPropertiesContextCustomizer` compara por el *conjunto de métodos*
(`equals`/`hashCode` sobre `Set<Method>`, verificado en las fuentes de
spring-test 7.0.9). Declarándolo una sola vez en una clase base heredada, el
`Method` es el mismo para todas y los contextos sí se reutilizan. Ése es el
beneficio de verdad, más que el ahorro de contenedores.
