# Review — feature 27 `github_connector` (cierre)

**Veredicto:** APPROVED CONDICIONADO A la puerta de mutación (C7) con los mínimos fijados abajo,
y a `bin/harness init` verde en manos del orquestador (C1/C4).

Superficie juzgada: worktree `C:/Users/vhurt/ow-worktrees/github-connector`, rama
`claude/github-connector`, `HEAD = cc76ec5` sobre la base `c5f9f93`. Todo lo que sigue lo he leído
yo sobre ese árbol; nada se da por bueno por venir escrito en la revalidación.

Restricción de sesión respetada: no he ejecutado la suite completa ni `bin/harness init` ni ninguna
campaña de mutación. No he editado un solo byte del árbol.

---

## 1. El bloqueante: **levantado**

`e2e/github-connector.spec.mjs:173-177`, leído por mí:

    expect(
      sql(
        "SELECT octet_length(token_ciphertext) FROM connector_connections WHERE owner_id=..."
      ),
    ).toBe(String(12 + TOKEN.length + 16));

Con `TOKEN = "ghp_token_de_pruebas"` (`:18`, 20 caracteres) afirma **48**, que es exactamente lo que
produce `adapter/connectors/AesGcmSecretCipher.java:51`
(`new byte[NONCE_BYTES + sealed.length]`, con `NONCE_BYTES = 12` en `:23` y etiqueta de 128 bits en
`:24`). La aritmética que sostuvo mi rechazo ya no se cumple: **la aserción es cierta sobre este
árbol.** `git merge-base --is-ancestor d418a5d HEAD` devuelve sí.

**Barrido propio, no heredado.** Tres pasadas sobre `backend/src`, `e2e/` y `frontend/src`
(excluido `backend/build/`, que es salida de compilación):

| Sonda | Resultado |
| --- | --- |
| `octet_length` en `.java`/`.mjs`/`.sql`/`.ts`/`.tsx` | 26 apariciones; las del conector son `V25:13`, `GithubConnectorPersistenceTest:154` y `spec.mjs:175`. Ninguna otra |
| `1 + 12`, `VERSION_BYTES`, «byte de versión», «version byte» | sólo `AesGcmWebhookSecrets` (feature 23, formato propio con byte de versión **por diseño**, otra tabla y otra columna), el comentario de `V25:6`, y dos comentarios que **niegan** el byte (`AesGcmSecretCipher:19`, `AesGcmSecretCipherTest:33`) |
| literales `49`, `284`, `BETWEEN 30` | ninguna aserción del conector; los tres aciertos son ajenos (substring de un token de sesión, un índice 49 de paginación, un secreto de webhooks de 49 caracteres) |

Los dos oráculos numéricos del formato dicen hoy lo mismo, y los he leído:
`AesGcmSecretCipherTest:38` afirma `assertEquals(12 + TOKEN.length() + 16, sealed.length)`;
`GithubConnectorWiringTest:46` afirma `hasSize(12 + "ghp_secreto123".length() + 16)`, que es
**literalmente** el «octet_length 12 + 14 + 16» que exige `features/github_connector.feature:20`.

La excepción que la revalidación declara —`GithubConnectorPersistenceTest`— la he comprobado y es
correcta: su `ciphertext(marker)` (`:99-103`) fabrica un `byte[43]` con un marcador en la posición 0,
que no es «12 + n + 16» de nada, sino un relleno que cae dentro del `CHECK` y sirve para distinguir
la fila A de la B. Esa clase mide persistencia, no cifrado, y es correcto que el formato le sea
indiferente.

**No queda en el árbol ningún punto que asuma 49 octetos ni el byte de versión.**

## 2. La evidencia de la revalidación: **creíble**, y no por confianza

No me fío de cifras que otro dice haber medido, así que las he contrastado contra el árbol. Lo que
las sostiene:

- **Los números de línea del listado delatan el orden real.** La segunda campaña
  (`revalidacion_github_connector.md:172-189`) lista «estado sin conexión» en `:148` y «estado
  conectada» en `:157`; la primera (`:114-115`) los lista en `:142` y `:151`. El parche añade
  **exactamente 6 líneas** en `:128` (`git diff c5f9f93..HEAD`), y 142+6 = 148, 151+6 = 157. Un
  listado inventado no acierta ese desplazamiento en las dos direcciones. Y `:124` (deshabilitado,
  anterior al punto de inserción) **no** se mueve, que es justo lo que debe pasar. Los cuatro
  números que he verificado contra el fichero (`124`, `148`, `157`, `182`) coinciden con los
  `test(` reales.
- **El recuento cuadra.** `grep -c` de `test(` a principio de línea: 15 en
  `github-connector.spec.mjs` más 1 en `github-connector-native-zoom.spec.mjs` igual a **16**.
  `playwright.config.mjs:5-7` confirma `workers: 1`, `retries: 0`, `timeout: 30_000`.
- **Artefactos en disco, con hora.** `test-results/.last-run.json` dice `status: passed` con
  `failedTests` vacío (9-sep 20:11);
  `.e2e-work/github-connector-zoom/organizationweb-e2e-66604/evidence.json` (20:09) trae las nueve
  cifras declaradas —`dpr 3`, `scroll == client` en 320/768/1440, 5 controles, 0 violaciones— y sus
  tres capturas. Los dos directorios de pila (`67024` y `66604`) existen, en ese orden de hora, que
  es el orden de las dos campañas.
- **Backend, tanda B: comprobada en el XML.**
  `TEST-...GithubConnectorPersistenceTest.xml` da `tests=26 failures=0 errors=0 skipped=0`;
  `TEST-...ConnectGithubTest.xml` da `tests=16 failures=0 errors=0`. Timestamp 18:14Z.
- **Backend, tanda A: los XML ya no están** (la segunda invocación de Gradle limpia
  `build/test-results/test/`). Así que **los he recontado en el código fuente**:
  `AesGcmSecretCipherTest` son 8 `@Test` más `@ValueSource(ints = {0,1,12,13,27,28})` más un
  `@ValueSource` de 4 cadenas, es decir **18**; `GithubConnectorWiringTest` son 7 `@Test` más un
  `@ValueSource` de 4 cadenas más un `@NullSource` con `@ValueSource` de 3, es decir **15**.
  Coinciden con lo declarado. Las 33 son creíbles.

**Cautela que dejo escrita:** `.last-run.json` acredita que la última ejecución de Playwright en este
worktree terminó verde, no *qué* ejecutó. Es el listado con líneas desplazadas —y no el JSON— lo que
ata la campaña a este árbol. Con ambas cosas, lo doy por bueno.

## 3. El `test.setTimeout(240_000)`: **corrección legítima, no relajación de la puerta**

Lo he juzgado por el diff, no por el relato. `git diff c5f9f93..HEAD` sobre código es de **seis
líneas**: cinco de comentario y `test.setTimeout(240_000)` en `e2e/github-connector.spec.mjs:133`.
Nada más. Ni una aserción tocada, ni un `waitFor` alargado, ni un selector aflojado.

La contradicción que denuncia es real y la he verificado en el fuente:

- `e2e/support/connector.mjs:65` — `waitForBackend` se concede `{ timeout: 90_000 }`.
- `:96-105` y `:108-117` — `withConnectorDisabled` llama a `compose up --force-recreate` **dos
  veces** (cada una con `timeout: 180_000`, `:47`) y espera con `waitForBackend` **las dos veces**.
- `playwright.config.mjs:7` — presupuesto de prueba: **30_000**.

Una espera interior de 90 s bajo un presupuesto exterior de 30 s no se puede honrar nunca. La prueba
no era verde por diseño: era verde mientras la máquina recreara Compose en menos de 30 s. Los 30,7 s
de la segunda campaña lo prueban por siete décimas.

**Un timeout es presupuesto, no oráculo.** Lo que decide si la prueba muerde sigue intacto:
`:134-145` recrea el backend **sin `APP_CONNECTOR_KEY` de verdad** y exige `role="alert"` con
«configuración del servidor», `toHaveCount(0)` del campo token, `toHaveCount(0)` del botón de
importar, `auditAxe` y `assertLayout`. Si el estado no se alcanza, la prueba revienta igual que
antes. Y hay precedente en el propio repositorio: `github-connector-native-zoom.spec.mjs:41` ya
pagaba lo mismo con `test.setTimeout(180_000)`. **Aprobado.**

Pega de artesano, no bloqueante: **240_000 es un número mágico que el comentario no deriva**. El
propio andamiaje declara 2 × (180_000 + 90_000) = 540_000 en el peor caso; 240_000 no es ni el peor
caso ni una fracción razonada de él. Si esta prueba vuelve a morir bajo carga, que nadie la lea como
un fallo del producto: es el presupuesto otra vez.

## 4. Los dos defectos abiertos: **ninguno bloquea**, y digo por qué

### 4.1 El `CHECK` de `V25__github_connector.sql:13` — **no bloqueante, deuda con dueño**

    token_ciphertext BYTEA NOT NULL CHECK (octet_length(token_ciphertext) BETWEEN 30 AND 284)

La aritmética del revalidador es correcta y la he rehecho: con el formato de hoy la cota real es
**29-283**. Y **el caso es alcanzable**, no teórico:
`domain/PersonalAccessToken.java:11-16` sólo rechaza nulo, vacío, más de 255 y no-ASCII —**no impone
longitud mínima distinta de 1**—, así que el token de un carácter es válido para el dominio, produce
29 octetos y lo rechazaría PostgreSQL. Peor: lo rechazaría como violación de restricción, no como
error de dominio, o sea con un 500 en vez del `VALIDATION_ERROR` que la pantalla sabe explicar. Es un
defecto de verdad, y no lo maquillo.

**Aun así no bloquea, por dos razones que he verificado:**

1. **No incumple ningún `@s`.** La tabla de estrictez del contrato
   (`features/github_connector.feature:95-101`) enumera token ausente, cadena vacía, 256 caracteres,
   espacio interior, `U+00E9`, `U+0009` y número. **Ningún escenario ejerce un token de un
   carácter.** No hay cobertura que falte: hay contrato que no lo nombra.
2. **La corrección correcta no cabe aquí.** `git log` sobre las migraciones muestra un solo commit
   por fichero: en este repositorio no se reescriben, porque cambiaría la suma de Flyway. Hace falta
   una migración nueva (`DROP CONSTRAINT` y `ADD CONSTRAINT ... BETWEEN 29 AND 283`) y el número de
   versión siguiente lo están consumiendo otros carriles. Meterla aquí es un conflicto garantizado
   por un defecto inalcanzable con tokens reales de GitHub.

**Exijo que quede como deuda con dueño y número**, no como nota suelta, y que la misma migración
arregle el comentario de `V25:6` —«es 1 byte de versión de clave + 12 de nonce...»—, que **hoy
miente** y que tampoco puede editarse en el sitio. Un comentario que describe mal el presente es peor
que ninguno; ya lo dije del de `ImportGithubIssues`, y **ese sí está corregido**: `:126-128` dice
ahora que el adaptador HTTP la traduce a 503 `CONNECTOR_KEY_MISMATCH`. Mi hallazgo menor 2 queda
cerrado.

### 4.2 `headless: false` y `channel: "chromium"` — **no bloqueante, y no se toca**

`github-connector-native-zoom.spec.mjs:71-72`. Acepto el razonamiento y lo he comprobado: el zoom se
aplica con `chrome.tabs.setZoom` desde una extensión MV3, que necesita navegador con ventana;
`deviceScaleFactor` cambiaría la densidad **sin producir el reflujo** que @s42 exige medir. Volverla
headless no la haría más portátil: la dejaría sin oráculo. Y la prueba **falla ruidosamente** si el
zoom no llega (`expect(zoom).toBe(2)`), sin `skip` ni `fixme`. El precedente
`reschedule-native-zoom.spec.mjs` paga lo mismo.

Para el orquestador: si el CI acaba sin entorno gráfico, la salida es etiqueta propia y runner con
pantalla, **no** headless. Es política de pipeline, no defecto de la 27.

---

## Cobertura de escenarios (@s ↔ test) — sólo lo que cambia

Los 41 escenarios que ya di por cubiertos en `judge_github_connector_final.md` siguen cubiertos; no
he encontrado nada que los toque. El único que estaba en el aire:

- **@s42 [x] revalidado.** Antes lo marqué `[~]` por evidencia no revalidable: su E2E contenía una
  aserción falsa. Hoy la aserción es cierta (`spec.mjs:177`), los 16 recorridos pasaron sobre este
  árbol y `evidence.json` está regenerado. **C6 queda marcado.**
- @s1 [x] sigue mejor que antes: `GithubConnectorWiringTest:46` fija `12 + 14 + 16`, literalmente lo
  que dice `features/github_connector.feature:20`.

## Disciplina TDD

- **¿Producción sin test que la pida? NO.** Y esta vez es trivial de comprobar: el diff de la
  revalidación **no toca `src/` en absoluto** (`git diff c5f9f93..HEAD` da
  `e2e/github-connector.spec.mjs` con +6 y dos ficheros de `progress/`). Cero producción nueva.
- **¿Evidencia de Rojo-Verde-Refactor? SÍ**, y aquí de la mejor clase: el rojo está **acreditado por
  ejecución** (`revalidacion_github_connector.md:114-122`, con mensaje, fichero y línea del fallo),
  no construido de memoria, y el arreglo es el mínimo que lo vuelve verde sin tocar el oráculo.
- **Honestidad de las cifras**: la revalidación declara lo que no midió (mutación, `harness init`) en
  vez de rellenarlo. Es lo que quiero ver.

## Calidad (lente de artesano)

Lo bueno, comprobado en el árbol:

- `e2e/support/connector.mjs:25-29` — el comentario explica una **fuerza** (el entorno del proceso
  gana sobre `--env-file`, así que la clave se pasa explícita), no lo que hace el código. Vale su
  sitio.
- `AesGcmSecretCipher:19-20` — deja escrito por qué la rotación sobrevive sin byte de versión: el
  llavero prueba claves y decide la etiqueta GCM. Es la justificación del formato, donde toca.
- `spec.mjs:170-172` — el comentario nuevo ata la constante al contrato («@s1 exige 12 + 14 + 16 y el
  byte lo contradecía»). Dentro de seis meses, quien lea esa línea sabrá por qué es 48.

Hallazgos (ninguno bloqueante):

1. `backend/src/main/resources/db/migration/V25__github_connector.sql:13` — cota `BETWEEN 30 AND 284`
   contra un rango real de 29-283. Alcanzable con un token de 1 carácter (`PersonalAccessToken:11-16`
   lo admite). Exige migración nueva.
2. `V25__github_connector.sql:6` — comentario que describe el formato viejo. Se corrige en la misma
   migración.
3. `e2e/github-connector.spec.mjs:133` — `240_000` sin derivar; el andamiaje declara 540_000 de peor
   caso.
4. `e2e/github-connector.spec.mjs:31-38` — `forget()` sigue interpolando el propietario en SQL por
   concatenación. Constante del propio fichero, hoy inocuo; sigo sin querer que se copie.
5. `frontend/src/github-connector.tsx:264` — `<main id="proyectos">`. Deuda del proyecto, no de este
   carril.

**Seguridad: nada nuevo.** El diff no toca `src/`, ni la lista blanca, ni la guarda SSRF, ni el
perfil `e2e`.

## Checkpoints

- **C1** [~] Ficheros y documentación presentes. `bin/harness init` **no ejecutado**: prohibido por
  instrucción de sesión (cuatro carriles y dos campañas de mutación en esta máquina). Sin marcar.
- **C2** [ ] `feature_list.json:311` y `:322` dejan la 27 **y** la 28 en `in_progress`. Es decisión
  de carriles del coordinador, no defecto de la 27.
- **C3** [x] Capas respetadas. El servicio falso vive en `e2e/`, fuera de `src/`.
- **C4** [~] Todo lo nuevo tiene prueba; `bin/harness test` completo no ejecutado por la misma razón
  que C1.
- **C5** [ ] `progress/history.md` sigue **sin entrada** de este carril (verificado por grep).
- **C6** [x] **Marcado.** @s42 revalidado sobre el formato de hoy: 16 de 16 y `evidence.json`
  regenerado. Era el único que quedaba.
- **C7** [ ] Mutación pendiente. **Es la condición de este veredicto.**

---

## La condición: qué exijo de las dos campañas de mutación

Las campañas anteriores se midieron sobre el formato con byte de versión y **no valen**, por la misma
razón por la que no valía la campaña de E2E. Estas son las cotas que exijo, y **no acepto un
porcentaje sin el recuento al lado**: un ámbito mal apuntado da buena puntuación sin generar un solo
mutante de la clase que importa, y eso no es una puerta, es un espejismo.

### Backend — PIT, ámbito `github_connector`

`backend/build.gradle.kts:665` fija `mutationThreshold = 80` y `harness.config.json:22` fija 0.80.

1. **Puntuación mínima: 80.**
2. **El informe debe declarar el total de mutantes generados**, no sólo el porcentaje.
3. **Desglose por clase.** He leído el ámbito (`build.gradle.kts:67-88`) y confirmo que **incluye
   `com.apptolast.organization.adapter.connectors.*` (`:82`)**, que cubre las cuatro clases del
   paquete: `AesGcmSecretCipher`, `ConnectorKeyRing`, `GithubApiBase` y `HttpGithubIssueSource`. El
   glob muerto `adapter.crypto.*` que documenta `progress/auditoria_colisiones_25_28_30.md`
   **no está** en este ámbito: lo he comprobado, la única mención de `adapter.crypto` en el fichero
   es el comentario de `:399-401`, que explica el arreglo ya hecho en el ámbito de la 28. Aun así,
   **quiero el desglose**, porque el glob correcto no garantiza mutantes.
4. **Suelo de mutantes para el cifrador: `AesGcmSecretCipher` con 12 mutantes generados como mínimo,
   y cero `NO_COVERAGE`.** El suelo no es arbitrario: he contado los puntos mutables del fichero con
   los mutadores por defecto de PIT y salen holgadamente más de quince — dos `NEGATE_CONDITIONALS`
   sobre `!ring.enabled()` (`:41`, `:59`), la condición compuesta de `:60` (dos negaciones más el
   `CONDITIONALS_BOUNDARY` sobre el `<=`), `MATH` sobre `NONCE_BYTES + sealed.length` (`:51`) y sobre
   `TAG_BITS / 8` (`:25`), `VOID_METHOD_CALLS` sobre `random.nextBytes` (`:44`), los dos
   `System.arraycopy` (`:52-53`), `cipher.init` y `updateAAD` (`:80-81`), y los retornos de `:54`,
   `:60`, `:65` y `:73`. **Si la campaña reporta menos de 12 en esta clase, el ámbito está mal
   apuntado y la campaña es nula**, dé la puntuación que dé.
5. **Mutantes que exijo `KILLED` uno por uno**, porque son exactamente las líneas que el cambio de
   formato tocó y las que mi rechazo anterior puso en duda:
   - `:51` `new byte[NONCE_BYTES + sealed.length]` (MATH). Si sobrevive, nada mide la longitud en
     reposo, que es el contrato de @s1.
   - `:52-53` los dos `System.arraycopy` (VOID_METHOD_CALLS). Si sobreviven, el nonce podría no
     escribirse y las pruebas no se enterarían.
   - `:60` `ciphertext.length <= SHORTEST` (NEGATE_CONDITIONALS y CONDITIONALS_BOUNDARY). Es la
     frontera de los 28 octetos; el `@ValueSource(ints = {0,1,12,13,27,28})` de
     `AesGcmSecretCipherTest:86` existe justamente para matarlos.
   - `:61-62` los desplazamientos de `copyOf` y `copyOfRange`.
   - `:25` `SHORTEST = NONCE_BYTES + TAG_BITS / 8` (MATH).

   Un superviviente en cualquiera de estos puntos **es rechazo**, aunque la puntuación global pase.
6. **`ConnectorKeyRing` con 1 mutante generado como mínimo.** De él cuelgan @s3 (conector
   deshabilitado sin clave), @s4 (clave malformada detiene el arranque) y la rotación B5.
7. **Ninguna clase nombrada en el ámbito con cero mutantes**, salvo los tipos que legítimamente no
   generan ninguno una vez excluidos `equals`, `hashCode` y `toString` (`build.gradle.kts:664`): los
   portadores de datos `IssuePage`, `StoredConnection`, `ConnectionView` e `IssueSourceException`.
   Cualquier **otra** clase a cero es un glob muerto y hay que arreglarlo antes de dar la campaña por
   buena.
8. **Todo superviviente, documentado** en `progress/mutation_github_connector.md`: matado con test
   nuevo o justificado como equivalente, uno por uno (C7).

### Frontend — Stryker, `frontend/stryker.github-connector.config.json`

1. **Puntuación mínima: 80** (`thresholds.break: 80` en el propio fichero, coherente con el umbral
   0,80 del arnés).
2. **Total de mutantes generados declarado**, y **ninguno de los tres módulos de `mutate` a cero**:
   `src/github-connector-client.ts`, `src/github-connector.tsx` y `src/integrations-index.tsx`. Un
   fichero a cero mutantes significa que `coverageAnalysis: "perTest"` no encontró pruebas que lo
   toquen, no que esté bien probado.
3. **Mutantes exigidos `KILLED`** en el escuchador de Escape (`github-connector.tsx:149-159`): la
   condición que lo acota a `confirming` y el desregistro al cerrar. Son la producción que impuse
   como requisito en mi dictamen anterior; si sus mutantes sobreviven, las tres unitarias de
   `github-connector.test.tsx:476-504` no muerden lo que dicen morder.

### Ambas

4. **Medidas sobre este árbol** (`cc76ec5` o el merge de integración que lo contenga), no sobre el
   formato viejo. El informe debe decir contra qué commit se midió.

## Y lo que no depende de la mutación

5. **`bin/harness init` verde**, con la máquina para el orquestador (C1 y C4). No lo he ejecutado por
   instrucción expresa, y no apruebo a ciegas: si sale rojo, este veredicto decae.
6. **La deuda del `CHECK` de `V25`, anotada con dueño y número de migración** antes de marcar la 27
   como `done`, junto con el comentario de `V25:6`.
7. **Entrada en `progress/history.md`** por este carril (C5).

Cumplido lo anterior, **la feature 27 está aprobada por mi parte y no tengo ninguna otra objeción**.
El único bloqueante que sostuvo mi rechazo está levantado y verificado sobre el árbol, no leído en un
informe.
