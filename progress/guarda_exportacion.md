# Guarda del conjunto de colecciones de la exportación (feature 22)

Carril `C:/Users/vhurt/ow-worktrees/export-guarda`, rama `claude/export-guarda`.
Reinjerto de la guarda que se perdió con `ConnectorExportExposureTest` al retirar
las features 27, 29 y 30 (`progress/retirada_features.md`, agujero declarado 4).

## 1. El número correcto: siguen siendo catorce

**Son catorce.** El encargo suponía que la retirada había bajado la cifra
("ya no son catorce: se llevó las colecciones de conectores, recibos de
importación, enlaces externos y automatizaciones"). **Esa suposición es falsa,
y lo es por una razón que conviene dejar escrita: esas colecciones nunca
estuvieron en la exportación.**

El propio `project-spec.md:2144` lo dice del conector de GitHub:

> `external_id` es el `id` numérico global de la issue en GitHub, en decimal;
> `url` es su `html_url`. La conexión y los enlaces **no forman parte del JSON
> v1 de exportación (22) ni de importación (23)**: añadir colecciones cambiaría
> esos contratos cerrados.

Y `ConnectorExportExposureTest` no afirmaba que el conector *aportara*
colecciones: afirmaba justo lo contrario, que **ninguna** de las catorce era
suya (`noneMatch(... contains("connect"/"token"/"link"/"github"/"import"))`).
Por eso su desaparición no cambió el número: retirar las features 27, 29 y 30
no tocó `ExportJsonWriter`.

`import_receipts` tampoco entra en la cuenta: es la tabla operativa del recibo
de importación (feature 23), y la propia retirada anotó que no tiene nada que
ver con las issues.

### De dónde sale el catorce — tres fuentes, contadas y coincidentes

| Fuente | Cuenta |
|---|---|
| `backend/src/main/java/com/apptolast/organization/adapter/persistence/ExportJsonWriter.java`, constante `COLLECTIONS` | 14 |
| `project-spec.md`, sección 22, tabla «Colección / Campos exactos» (líneas 1625-1638) | 14 filas |
| `features/export_data.feature`, @s1 y @s33: «las catorce colecciones de la sección 22» | 14 |

Coinciden además, nombre a nombre: `frontend/src/export-data-api.ts:4`
(la lista `collections` del validador de cliente) y el `switch` de
`PostgresExportDataQueries` (14 `case`, uno por colección).

## 2. El conjunto exacto

```
projects, tasks, taskStatusHistory, availability, plannedBlocks,
blockProjections, blockChanges, workSessions, workSessionIntervals,
workSessionChanges, appearance, customization, projectCustomFieldValues,
taskCustomFieldValues
```

## 3. La guarda, y dónde vive ahora

`backend/src/test/java/com/apptolast/organization/adapter/persistence/ExportCollectionSetTest.java`
— en el carril de exportación, junto a `ExportJsonWriterTest`, no en uno de
conectores. Trazada a `@s1` y `@s33` de `features/export_data.feature`.

Dos pruebas:

- `s1_dataCarriesExactlyTheCollectionsOfSection22` — compara el **conjunto**
  de claves de `data` contra las catorce de la sección 22
  (`containsExactlyInAnyOrderElementsOf`), más `hasSize(COLLECTIONS_IN_SECTION_22)`
  para clavar la cifra que el contrato escribe con letra.
- `s1_countsCarriesExactlyTheSameCollectionsAsData` — `counts` lleva exactamente
  las mismas claves que `data`, para que un count no pueda quedarse atrás.

Se compara conjunto y no cardinalidad **a propósito**: ver el rojo C.

## 4. Los rojos acreditados

La prueba pasó a la primera, cosa que aquí no demuestra nada: el
comportamiento ya era correcto. Así que el rojo se acredita mutando producción
en las dos direcciones que la guarda debe atrapar (y en una tercera que un
recuento no atraparía). Producción quedó restaurada tras cada una:
`git status` limpio salvo el fichero de prueba nuevo.

### Rojo A — falta una colección: datos incompletos y sin enterarse

Quitada `"blockProjections"` de `ExportJsonWriter.COLLECTIONS`:

```
ExportCollectionSetTest > s1_dataCarriesExactlyTheCollectionsOfSection22() FAILED
java.lang.AssertionError: [colecciones exportadas en data frente a la sección 22]
Expecting actual:
  ["projects", "tasks", "taskStatusHistory", "availability", "plannedBlocks",
   "blockChanges", "workSessions", "workSessionIntervals", "workSessionChanges",
   "appearance", "customization", "projectCustomFieldValues", "taskCustomFieldValues"]
to contain exactly in any order:
  ["projects", "tasks", "taskStatusHistory", "availability", "plannedBlocks",
   "blockProjections", "blockChanges", "workSessions", "workSessionIntervals",
   "workSessionChanges", "appearance", "customization", "projectCustomFieldValues",
   "taskCustomFieldValues"]
but could not find the following elements:
  ["blockProjections"]
	at ExportCollectionSetTest.s1_dataCarriesExactlyTheCollectionsOfSection22(ExportCollectionSetTest.java:43)
```

### Rojo B — sobra una colección: fuga de datos privados por la exportación

Añadida `"connectorConnections"` al final de `COLLECTIONS`:

```
java.lang.AssertionError: [colecciones exportadas en data frente a la sección 22]
Expecting actual:
  [... "projectCustomFieldValues", "taskCustomFieldValues", "connectorConnections"]
to contain exactly in any order:
  [... "projectCustomFieldValues", "taskCustomFieldValues"]
but the following elements were unexpected:
  ["connectorConnections"]
```

### Rojo C — falta una y sobra otra: siguen siendo catorce

`"blockProjections"` **sustituida** por `"connectorConnections"`. El recuento no
se mueve —catorce antes, catorce después—, así que una guarda de cardinalidad
habría pasado en verde con la exportación rota y filtrando a la vez:

```
java.lang.AssertionError: [colecciones exportadas en data frente a la sección 22]
...
elements not found:
  ["blockProjections"]
and elements not expected:
  ["connectorConnections"]
```

Este es el rojo que justifica comparar conjuntos.

### Verde final

`ExportCollectionSetTest`, `ExportJsonWriterTest`, `ExportBufferTest` y
`ExportReceiptWriterTest`: BUILD SUCCESSFUL. `spotlessCheck` limpio.
Tests por clase concreta, sin suite entera y sin campañas de mutación.

## 5. Discrepancias — nada que enmendar, dos cosas que decidir

### 5.1 Contrato y spec dicen bien el número

Ni `features/export_data.feature` ni la sección 22 de `project-spec.md` nombran
un número que ya no sea. **No hay línea que corregir.** Las citas de «catorce»
en las secciones 23 (importación) y su lista de locks también cuadran: catorce
tablas durables más `import_receipts`, que es el recibo operativo y va aparte.

### 5.2 La guarda no estaba tan perdida como parecía — pero ahora tiene nombre

Contra lo que decía el encargo («hoy ninguna de las dos cosas se detecta»),
`ExportJsonWriterTest.s1_emptyAccountProducesTheClosedDocumentAndExactDownloadMetadata`
ya afirmaba las claves de `data` y de `counts` con `containsExactlyElementsOf`
sobre una lista local de catorce. Los rojos A, B y C también la habrían tumbado.
Lo que se perdió con `ConnectorExportExposureTest` fue una guarda **dedicada y
con nombre propio**: en `ExportJsonWriterTest` el conjunto viaja de polizón
dentro de una prueba que se llama «documento cerrado y metadatos de descarga»,
y quien la edite para otra cosa puede aflojarla sin notar qué está aflojando.
Hay solape deliberado entre ambas; no lo he retirado porque tocar la prueba de
`@s1` de una feature cerrada no es cosa mía.

### 5.3 Para ti: hay una segunda puerta al «datos incompletos y sin enterarse»

`PostgresExportDataQueries` resuelve cada colección con un `switch` que termina
en `default -> 0`. Hoy están los catorce `case`, así que **no hay defecto**.
Pero si mañana alguien añade una colección a `COLLECTIONS` y olvida su `case`,
la exportación la escribiría como **array vacío con count 0**: un archivo que
parece completo, con la colección presente y en cero. Mi guarda no lo ve —
compara nombres, no contenido — y la del conjunto tampoco fallaría.

No lo toco: sería cambiar producción de la feature 22, que está en `done`, y
tocar su ámbito de mutación es decisión tuya. Si quieres cerrarlo, el arreglo
natural es que `default` lance en vez de devolver 0.

## 6. Mapa @s → test

| Escenario | Test |
|---|---|
| `@s1` (data con las catorce colecciones y counts con sus catorce ceros) | `ExportCollectionSetTest.s1_dataCarriesExactlyTheCollectionsOfSection22`, `ExportCollectionSetTest.s1_countsCarriesExactlyTheSameCollectionsAsData` |
| `@s33` (documento íntegro con sus catorce colecciones) | mismas dos, sobre el mismo conjunto canónico |

## 7. Ámbito de mutación

No he lanzado mutación y no he tocado `backend/build.gradle.kts`. El cambio es
**sólo de test**: cero líneas de producción. Si quieres que
`ExportCollectionSetTest` cuente dentro del `mutationScope` de la feature 22,
es una línea en ese fichero y la llevas tú.
