# Reparación de integración — guarda de compatibilidad de esquema

Trabajo puntual sobre `main` (no es un carril de feature). CI `34345112968`
sobre `a6164e4` en rojo en `node .harness/harness.mjs init`:

```
ApiCredentialCompatibilityTest > s42_additiveUpgradePreservesBusinessAndExportImportNeverCopiesCredentials() FAILED
    org.opentest4j.AssertionFailedError at ApiCredentialCompatibilityTest.java:51
3563 tests completed, 1 failed
```

Fichero: `backend/src/test/java/com/apptolast/organization/adapter/persistence/ApiCredentialCompatibilityTest.java`.

## ROJO (reproducido en local, no inventado)

```
backend/gradlew.bat test --no-daemon --tests "com.apptolast.organization.adapter.persistence.ApiCredentialCompatibilityTest"
→ AssertionFailedError at ApiCredentialCompatibilityTest.java:51 — 1 test completed, 1 failed
```

Causa: la guarda comparaba la foto de `information_schema.columns` en V21
contra la foto final **excluyendo a mano** las tablas nuevas conocidas:

```java
"... AND table_name NOT IN ('api_credentials','api_owner_quotas','api_credential_quotas') ..."
```

`V24__calendar_feed_tokens.sql` (feature 26) crea `calendar_feed_tokens`, que
no está en esa lista: aparece en el «después», no en el «antes», y la
aserción revienta. El mismo golpe se repetiría con V23 (webhooks, dos
tablas), V25 (conector GitHub), V26 (calendario externo) y V28
(automatizaciones).

## ANTES

- Una sola consulta con lista negra codificada en el SQL.
- Cada migración nueva obligaba a editar un test de la feature 24 ya
  dictaminado por su juez.
- Columnas comparadas: `table_name, column_name, data_type` (el orden ordinal
  solo iba implícito en el `ORDER BY`).

## DESPUÉS (VERDE)

La intención se expresa directamente: *«una actualización aditiva no altera
nada de lo que ya existía»*. Se fotografían todas las columnas públicas en
V21, se deriva el conjunto de tablas preexistentes y, tras migrar del todo,
se compara la foto de V21 contra **la parte del después restringida a esas
mismas tablas**:

```java
var columnsBeforeUpgrade = publicColumns(jdbc);
var tablesBeforeUpgrade = tableNamesOf(columnsBeforeUpgrade);
...
assertEquals(columnsBeforeUpgrade, columnsOfTables(publicColumns(jdbc), tablesBeforeUpgrade));
```

Con tres ayudantes privados: `publicColumns`, `tableNamesOf`,
`columnsOfTables`, y la consulta en la constante `PUBLIC_COLUMNS`.

Refuerzos, no debilitamientos:

- `ordinal_position` pasa a ser **columna seleccionada y comparada**, no solo
  criterio de orden: un cambio de posición ordinal ahora es un valor
  distinto, no solo una permutación de la lista.
- Toda tabla que no existiera en V21 es por definición una adición y la
  guarda no opina sobre ella: **cero mantenimiento** al añadir migraciones.
- El filtrado se hace en Java sobre el resultado, sin construir SQL por
  concatenación.

Resto del test intacto: el `row_to_json` de `projects` (xmin/ctid), la
igualdad byte a byte de la exportación, la credencial, las dos cuotas, el
`NO_CHANGE` del import y el conteo de `outbox_events` no se han tocado.

## Prueba de que la guarda SIGUE MORDIENDO

Sin esto solo se habría demostrado que el test pasa, no que sirve. Se añadió
una migración desechable `backend/src/main/resources/db/migration/V99__guard_probe.sql`
(que corre después de todas las demás, o sea sobre el esquema final), se
ejecutó la clase filtrada y se retiró.

### Sonda 1 — cambio de tipo de una columna preexistente

```sql
ALTER TABLE import_receipts ALTER COLUMN outcome TYPE VARCHAR(32);
```

Resultado: **BUILD FAILED**, `AssertionFailedError`. En el diff del informe
JUnit, la misma columna aparece con los dos tipos:

```
column_name=outcome, data_type=text               (esperado, foto de V21)
column_name=outcome, data_type=character varying  (obtenido, tras migrar)
```

### Sonda 2 — desaparición de una tabla de V21

```sql
DROP TABLE import_receipts;
```

Resultado: **BUILD FAILED**, `AssertionFailedError` en
`ApiCredentialCompatibilityTest.java:70` (la aserción de esquema). Las
columnas de `import_receipts` (p. ej. `identical_counts, data_type=jsonb`)
solo aparecen en el lado **esperado** del diff y están ausentes del obtenido:
la guarda detecta la eliminación de una tabla preexistente, que es el caso
que la lista negra jamás habría cazado.

Por construcción quedan cubiertos también el renombrado y el borrado de una
columna (desaparece/aparece una entrada dentro de una tabla de V21) y el
cambio de orden ordinal (`ordinal_position` se compara explícitamente).

### Retirada de la sonda

`V99__guard_probe.sql` eliminada; `git status` sobre `backend/` solo muestra
el fichero de test modificado.

## Verificación final

```
backend/gradlew.bat spotlessCheck test --no-daemon --tests "com.apptolast.organization.adapter.persistence.ApiCredentialCompatibilityTest"
→ BUILD SUCCESSFUL
```

Disciplina de recursos respetada: solo la clase filtrada, nunca la suite
completa, ni `pitest`, ni E2E (hay cuatro carriles más vivos en la máquina).

## Pendiente

Sin `git push` (integra el lead). Nada marcado `done`.
