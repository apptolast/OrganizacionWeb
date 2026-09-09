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

(se completa abajo)

## Acoplamientos ocultos encontrados

(se completa abajo)

## Evidencias

(se completa abajo)

## Trabajo aparcado

El contenedor único compartido para toda la ejecución sigue siendo buena idea,
pero no es lo que rompe la CI. El avance queda guardado y se retomará en un
commit aparte.
