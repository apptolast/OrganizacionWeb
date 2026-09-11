# Troceado de la campaña completa de mutación

11 de septiembre de 2026. Rama local `codex/mutation-shards`, sobre `origin/main`
`2ac34cd`. **Aplicado, pendiente de calibración**: todavía no ha corrido ni una
vez en GitHub.

## 0. El problema, en una línea

`harness verify` sin objetivo muta el universo de la rama `else` de
`targetClasses` (456 clases de backend) y los 53 ficheros de
`frontend/stryker.config.json`. En un job hospedado **no cabe**: el run
34322164783 llevaba 3 h 41 min analizando PIT cuando lo mató el
`timeout-minutes: 240`, el frontend no arrancó nunca y el techo duro son 360 min.
Trocear por ámbitos con nombre mide menos (308 de 456,
`progress/medicion_ambito_mutacion.md`), así que queda prohibido.

## 1. De dónde sale este diseño

`progress/auditoria_workflows.md` §4 dice que el troceado estaba «diseñado y
verificado» y que traía «tres correcciones bloqueantes pendientes». **Ese diseño
no se ha podido recuperar**: `git log --all -S mutationShard` sale vacío y no
aparece en ninguna rama, ni en `docs/`, ni en los scripts. Lo que sigue es una
reconstrucción hecha desde el repositorio y comprobada contra las fuentes de PIT
1.22.0 y de Stryker 10.0.0. Las tres correcciones probables son las de abajo,
pero son **inferidas, no confirmadas**:

1. **Exclusión, no inclusión.** 64 de los 225 patrones de la rama `else` son
   nombres exactos sin `*`. Tres de ellos (`TaskController`,
   `TaskHistoryController`, `TaskStatusController`) tienen tipos anidados que hoy
   PIT **no** muta. Una lista de inclusión `FQN` + `FQN$*` los añadiría y cambiaría
   el denominador. Con `FQN*` sería peor: `Task*` mete `TaskController` y
   `TaskQueries` en el trozo de `Task`.
2. **Umbral agregado, no por trozo.** Hoy la puerta es una sola puntuación sobre
   todo el universo. Un 80 en cada trozo sería otra puerta, no equivalente, y
   un fallo de umbral no se distingue de un fallo de PIT.
3. **Completitud probada por manifiestos.** Una clase o un fichero sin mutantes no
   aparece en el `mutations.xml` de PIT ni en el `mutation.json` de Stryker. Así
   que «falta una clase» sólo se puede detectar comparando con el universo, no
   leyendo los informes.

## 2. Lo que se ha aplicado

### Backend: `-PmutationShard=k/N` (`backend/build.gradle.kts`, final del bloque `pitest`)

- Sin la propiedad, nada cambia: `harness verify` y `harness mutate` locales ven
  la misma configuración. No se ha tocado ninguna línea que fijen las guardas de
  `scripts/project.test.mjs` (las ramas `else`, `mutationThreshold.set(80)` y el
  `threads.set(...)`).
- Se parsea con `^([1-9][0-9]*)/([1-9][0-9]*)$` y `1 <= k <= N`. Si no casa, lanza
  `GradleException`. También lanza si viene junto a `mutationScope`: un valor
  desconocido nunca se degrada a «todo», que es la lección de `noche_cinco`.
- El universo son los patrones de `targetClasses.get()`, leídos después del
  `when` para reutilizar la rama `else` en vez de copiarla. Se convierten como
  `org.pitest.util.Glob`. Las clases propietarias son los `.java` de
  `src/main/java` que casan con esos patrones, ordenados.
- `mine = owners[i]` con `i % N == k-1`, y **`excludedClasses`** se lleva a todos
  los demás como `FQN` y `FQN$*`. `targetClasses` y `targetTests` no se tocan, y
  `threads` sigue en 4.
- En modo troceado: `reportDir = build/reports/pitest-shard-k-of-N` y
  `mutationThreshold.set(0)`. La puerta la pone el veredicto.
- Manifiesto hermano del informe (`pitest-shard-k-of-N.manifest.json`) con las
  clases del trozo, el total del universo, el `sha256` de los patrones ordenados,
  el número de exclusiones y la versión de PIT.

### Frontend (`scripts/mutation-shards.mjs run frontend k/N`)

- `stryker.config.json` **no se toca**. El universo son sus 54 entradas agrupadas
  por fichero (53 ficheros): los dos rangos de `src/session-gate.tsx` viajan
  juntos. Es el universo de hoy, no la unión de 72 de la medición §4.3;
  ampliarlo es otra decisión, del propietario.
- Se genera `frontend/stryker.shard.config.json` (ignorado por git y borrado al
  terminar): una copia profunda con sólo dos cambios, `mutate` y
  `thresholds.break = null`. Un test afirma que todo lo demás es idéntico.

### Veredicto (`scripts/mutation-shards.mjs verdict`)

- Recalcula el universo **desde el árbol**, nunca desde los artefactos, y el
  reparto para cada N.
- Exige exactamente N_B artefactos `pit-shard-k-of-N` y N_F `stryker-shard-k-of-N`,
  ni uno más. Cada `done.json` debe tener `exitCode` 0, el SHA del run y hashes
  que casen con los ficheros descargados.
- Cada manifiesto debe ser el reparto recalculado. La unión debe ser el universo
  y los trozos, disjuntos dos a dos.
- Para PIT: la clase propietaria de cada `<mutation>` (lo que va antes del primer
  `$`) tiene que ser del trozo y del universo. La clave (clase, método,
  descriptor, línea, mutador, índices, bloques) es única entre todos los trozos,
  y `STARTED`/`NOT_STARTED` es fatal.
- Para Stryker: los ficheros del informe tienen que estar entre los del
  manifiesto, la clave (fichero, mutador, ubicación, reemplazo) es única y
  `Pending` es fatal.
- **Mismo umbral, misma fórmula, dos puertas**, como hoy:
  - PIT: `PercentageCalculator` de 1.22.0 con aritmética `float`
    (`Math.fround`), y falla si `score < 80`.
  - Stryker: `calculateMutationTestMetrics` sobre el informe fusionado. Se carga
    desde la ubicación de `@stryker-mutator/core`, exigiendo la versión exacta que
    declara (3.8.4), y falla si la puntuación es NaN o está por debajo de `break`.
    Nunca cae a una fórmula escrita a mano.
- También falla si PIT no es 1.22.0 o si el 80 de Gradle, el `break` de Stryker y
  el `threshold × 100` del arnés no coinciden.

### Workflow (`.github/workflows/harness-mutation.yml`)

Consta de `init`, que es la mitad init de `verify`, `plan`, las matrices
`backend-shard` y `frontend-shard` (`fail-fast: false`, 200 min) y `verdict`, con
`needs` de todos, `if: always()` y un paso final que falla si algún job previo no
es `success`. N vive en `env` (6 y 12) y se puede forzar en el dispatch (1..20).
**No hay cron.**

### Lo que NO se ha tocado

`.harness/harness.mjs`, `harness.config.json`, `harness.schema.json`, la ruta sin
objetivo de `scripts/project.mjs` y `frontend/stryker.config.json`. El trozo **no**
es un objetivo del arnés: `harness mutate backend-shard-1-of-4` da
`Invalid target`, así que nunca puede imprimir «Prueba de mutación superada» sin
umbral. La puerta de cierre por feature, CHECKPOINTS C7 con `mutation_tester`,
sigue igual.

## 3. Evidencia comprobada hoy

- El resolvedor portado (`backendUniverse`) da **225 patrones y 456 clases**, el
  mismo número que `progress/medicion_ambito_mutacion.md`. Frontend: **53
  ficheros y 54 entradas**.
- PIT 1.22.0 (fuente en la etiqueta `1.22.0`):
  - `Glob.convertGlobToRegex`: `*`→`.*`, `?`→`.`, `~` = regex cruda.
  - `PercentageCalculator.getPercentage` y `DetectionStatus`, con
    `detected=true` para KILLED, TIMED_OUT, NON_VIABLE, MEMORY_ERROR y RUN_ERROR.
  - El `commandline.MutationCoverageReport`:
    `if ((threshold != 0) && (stats.getPercentageDetected() < threshold))`.
  - `XMLReportListener`: atributos `detected`, `status` y `numberOfTestsRun`,
    más `<mutations partial=...>`.
- Stryker 10.0.0:
  - `determineExitCode` compara `metrics.systemUnderTestMetrics.metrics.mutationScore`
    sin redondear con `thresholds.break` sólo si es número, así que `null` quiere
    decir «sin puerta».
  - Las claves de `files` son rutas relativas al cwd.
  - Depende de `mutation-testing-metrics` 3.8.4 exacto, que es ESM con
    `exports: {".": ...}`.
  - `calculateMutationTestMetrics` probado en local sobre un informe fusionado.
- Rutas con punto: `upload-artifact@v4` omite rutas ocultas por defecto, así que
  el directorio de preparación es `mutation-shards-out/`, sin punto.

## 4. Por qué N_B = 6 y N_F = 12 (estimaciones, no medidas)

**Backend.** El universo completo no terminó 254 unidades en 221 min de análisis
en CI, más 470 s de cobertura y 10 min 39 s de init. Su total es desconocido y
supera las 3 h 50 min. La suma de los 21 ámbitos con nombre en la máquina de
desarrollo es de 8 h 52 min, pero cada uno paga su cobertura y sólo cubren 308
de 456 clases. La estimación pesimista de trabajo es de **12 h de análisis** en
4 vCPU. El coste fijo por trozo (checkout, compilación y cobertura) ronda los
15 min, así que quedan unos 185 min para analizar. Con un factor de desequilibrio
de 1,5 (`import_data_persistence`: 4 clases, 2 h 21 min):
720 × 1,5 / 185 ≈ 5,8, de ahí **6**, con unos 120 min esperados por trozo.

**Frontend.** El perfil por defecto **no se ha medido nunca**. Con unos 120
mutantes por fichero × 53 ≈ 6.400, a concurrencia 2 en un runner de 4 vCPU
(unos 5 mutantes por minuto, un cuarto de los 18–20 medidos a concurrencia 8),
salen unas **21 h**. Entre 12, unos 107 min más el dry run, y con desequilibrio
de 1,5 unos 160 min por trozo. La concurrencia sigue en 2, como hoy: subirla
cambia resultados sensibles al timeout y es una decisión aparte, que habría que
medir.

En total son 21 jobs y el plan Free admite 20 concurrentes, así que algún trozo
puede esperar. Eso alarga el reloj, no cambia el resultado.

## 5. Calibración: qué hay que rellenar y cuándo vuelve el cron

Primera ejecución: `workflow_dispatch` sin entradas, sólo para calibrar. Copiar
de cada `done.json`, y del resumen del veredicto, el tiempo y los mutantes:

<!-- markdownlint-disable MD013 -->

| trozo | minutos | mutantes | detectados | exitCode |
| --- | --- | --- | --- | --- |
| pit-shard-1-of-6 … 6-of-6 | pendiente | pendiente | pendiente | pendiente |
| stryker-shard-1-of-12 … 12-of-12 | pendiente | pendiente | pendiente | pendiente |

<!-- markdownlint-enable MD013 -->

- Si un trozo pasa de 200 min, el veredicto sale rojo por falta de artefacto, que
  es el fallo cerrado correcto. En ese caso se sube N en `env` y se relanza.
- **El cron vuelve sólo si el trozo más lento medido queda en 150 min o menos**
  (25 % de margen bajo 200 y lejos de 360), anotado aquí con su run.

## 6. Riesgos abiertos

- El argumento `--excludedClasses` de un trozo son unas 380 clases × 2 globs,
  unos 50 KB. Cabe en Linux (`MAX_ARG_STRLEN` es de 128 KiB por argumento) pero no
  en la línea de órdenes de Windows, así que `run backend` se niega en win32. En
  el primer run hay que comprobar que gradle-pitest-plugin lo pasa en línea y no
  falla.
- Si alguien quita el job `verdict` o lo hace opcional, los trozos pasan sin
  umbral. `scripts/mutation-shards.test.mjs` exige que el bloque de Gradle y el
  veredicto existan juntos.
- El round-robin reordena los trozos cuando entra una clase. No afecta a la
  cobertura, sí a los tiempos de cada trozo.
- Las pruebas con Testcontainers y `timeoutConst` de 15 s en un runner cargado
  pueden mover un mutante entre KILLED, TIMED_OUT y RUN_ERROR respecto a una
  pasada entera. Mueve la nota al margen, no la cobertura.
- **No verificado aquí**: Java y Gradle no están instalados en esta máquina, así
  que el bloque Kotlin no se ha ejecutado. Sólo lo cubren las guardas textuales
  de `scripts/project.test.mjs` y el cruce del veredicto (`patternsSha256`,
  clases del manifiesto). El primer run de CI es su prueba real.
