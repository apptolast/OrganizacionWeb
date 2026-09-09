# Revalidación de la feature 27 `github_connector` — carril D

**Motivo**: el dictamen `progress/judge_github_connector_final.md` dictó REJECTED por **un solo
bloqueante ajeno al artesano**: `e2e/github-connector.spec.mjs:168` afirmaba
`String(1 + 12 + TOKEN.length + 16)` (49 octetos) contra un formato en reposo que el merge de la
unificación de `SecretCipher` (features 27 y 28) dejó en 48 al retirar el byte de versión.

El juez cerró su veredicto con una promesa explícita (`:340`): *«Cuando salga verde, esta feature
está aprobada por mi parte: no tengo ninguna otra objeción pendiente.»* Lo que falta, por tanto, no
es cobertura ni diseño: es **una campaña verde medida sobre el árbol de hoy**, porque la anterior se
midió sobre un formato en reposo que ya no existe.

- Worktree: `C:/Users/vhurt/ow-worktrees/github-connector`, rama `claude/github-connector`.
- Base: `c5f9f93` (main del 9-sep, con las features 25, 26, 28 y 30 integradas).
- `E2E_WEB_PORT` asignado: **18096**.

---

## Paso 1 — Barrido: ¿queda algo que asuma 49 octetos o el byte de versión?

`d418a5d` («fix(e2e): el texto cifrado del conector mide 48 octetos, no 49») **es ancestro de
`HEAD`** en este worktree, comprobado con `git merge-base --is-ancestor d418a5d HEAD`. La corrección
está, pero el encargo era barrer el árbol entero, no fiarse de un commit.

### Lo que se barrió

Tres barridos sobre `e2e/`, `backend/src/` y `frontend/src/` (excluyendo `backend/build/`, que es
salida de compilación):

1. `octet_length` en `.mjs`, `.java`, `.sql`, `.ts`, `.tsx`
2. `token_ciphertext` / `tokenCiphertext` (la columna y su acceso, todos los puntos)
3. `byte de versión` / `version byte` / `VERSION_BYTES` / `1 + 12` / `1+12`

### Resultado, punto por punto

| Punto | Qué afirma hoy | Formato | Veredicto |
| --- | --- | --- | --- |
| `e2e/github-connector.spec.mjs:170` | `String(12 + TOKEN.length + 16)` = **48** | sin byte | **correcto** |
| `backend/.../AesGcmSecretCipher.java:51` | `new byte[NONCE_BYTES + sealed.length]` = 12 + n + 16 | sin byte | correcto (es el productor) |
| `backend/.../AesGcmSecretCipherTest.java:38` | `assertEquals(12 + TOKEN.length() + 16, sealed.length)` | sin byte | correcto |
| `backend/.../GithubConnectorWiringTest.java:46` | `hasSize(12 + "ghp_secreto123".length() + 16)` | sin byte | correcto, y es **literalmente** lo que pide @s1 |
| `backend/.../GithubConnectorPersistenceTest.java:150,154` | `hasSize(43)` y `octet_length = 43` | **indiferente al formato** | correcto, ver nota |
| `backend/.../ConnectGithubTest.java:60-62,130,161-175` | identidad, no longitud (`decrypt` de ida y vuelta, AAD del propietario, nonce nuevo por escritura) | indiferente | correcto |

**Nota sobre la persistencia**: `GithubConnectorPersistenceTest` no usa el cifrador real. Su
`ciphertext(marker)` (`:99-103`) fabrica un `byte[43]` con un marcador en la primera posición. El 43
no es «12 + n + 16» de nada: es un relleno arbitrario que cae dentro del `CHECK` de la tabla, y su
oficio es distinguir la fila A de la fila B en las pruebas de reemplazo (`:174`, `:187`). El cambio
de formato **no le afecta**, y es correcto que no le afecte: esa clase mide persistencia, no cifrado.

### Un hallazgo del barrido, ajeno al bloqueante

`backend/src/main/resources/db/migration/V25__github_connector.sql:4-6` — el **comentario** de la
migración sigue describiendo el formato viejo:

    -- es 1 byte de versión de clave + 12 de nonce + texto cifrado + 16 de etiqueta.

Es el mismo territorio del hallazgo menor 5 del juez (la cota inferior del `CHECK`), y la aritmética
confirma su sospecha:

| | formato viejo (1+12+n+16) | formato de hoy (12+n+16) |
| --- | --- | --- |
| token de 1 carácter | 30 | **29** |
| token de 255 (máximo de `PersonalAccessToken`) | 284 | **283** |
| `CHECK` escrito | `BETWEEN 30 AND 284` | sin tocar |

La cota alta (284) es hoy **inalcanzable**, y la baja (30) **excluye un token de un solo carácter**,
que el dominio sí admite: `PersonalAccessToken` sólo acota el máximo (255) y exige ASCII sin
espacios. Un token de un carácter produciría 29 octetos y la fila sería rechazada por el `CHECK` en
vez de por el dominio.

**No lo he corregido, y aquí está el porqué.** En este repositorio las migraciones de Flyway **no se
reescriben nunca** una vez creadas: `git log` sobre `V25__github_connector.sql` y sobre
`V23__webhooks.sql` devuelve **un solo commit cada una**, el de su creación. Reescribir el fichero
—aunque fuera sólo el comentario— cambia la suma de comprobación de Flyway y rompe cualquier base ya
migrada. La corrección correcta es una migración nueva (`ALTER TABLE ... DROP CONSTRAINT` /
`ADD CONSTRAINT` con `BETWEEN 29 AND 283`), y el siguiente número de versión lo están consumiendo
otros carriles ahora mismo: crearlo aquí garantiza un conflicto de integración por un defecto que el
propio juez calificó de **menor y no bloqueante**. Queda escrito para el orquestador, con el número
recalculado, para que lo tome el carril que abra la siguiente migración.

**Conclusión del paso 1**: no queda en el árbol **ningún** punto que asuma 49 octetos ni el byte de
versión, salvo el comentario de la migración descrito arriba —que no es una aserción y no puede
hacer fallar ninguna prueba— y `AesGcmWebhookSecrets`, que es el cifrador **de la feature 23**
(webhooks) y tiene su propio formato con byte de versión por diseño: no comparte columna, ni tabla,
ni contrato con el conector.

---

## Paso 2 — Primera campaña E2E sobre el árbol de hoy: 14 de 16, y el bloqueante del juez en verde

    E2E_WEB_PORT=18096 pnpm test:e2e -- e2e/github-connector.spec.mjs \
                                        e2e/github-connector-native-zoom.spec.mjs

Pila propia (`organizationweb-e2e-67024`), construida desde cero, cuatro contenedores sanos
(postgres, backend, web, github-fake), un solo worker, `retries: 0`.

**Resultado: 16 pruebas, 14 verdes, 2 rojas, 2,2 minutos.**

Lo primero que hay que decir, porque es lo que el juez pedía:

> `✓ 4 e2e\github-connector.spec.mjs:151:1 › @s42 estado conectada, alcanzado conectando de verdad (7.1s)`

**Esa es la prueba del bloqueante.** Es la que contiene
`toBe(String(12 + TOKEN.length + 16))`, la que el juez declaró «aritméticamente falsa» sobre el
formato viejo. Pasa contra el `octet_length` real de la fila que el backend acaba de escribir con el
cifrador de hoy. El formato en reposo son **48 octetos** y la aserción los afirma. El bloqueante
está cerrado con ejecución, no con lectura.

Y `github-connector-native-zoom.spec.mjs` pasó también, en primera posición y en 12,2 s.

### Las dos rojas: qué eran de verdad

    ✘ 2 ...:124:1 › @s42 estado deshabilitado: el servidor sin clave de conectores (32.3s)
    ✘ 3 ...:142:1 › @s42 estado sin conexión (3.3s)

    Error: expect(received).toBe(expected)
    Expected: 200
    Received: 502
    Call Log: - Test timeout of 30000ms exceeded
        at waitForBackend (e2e\support\connector.mjs:67:6)
        at withConnectorDisabled (e2e\support\connector.mjs:117:11)

**No es el producto: es una contradicción de presupuestos en el andamiaje de la prueba.** La
aritmética lo dice sola:

- `withConnectorDisabled` recrea el backend **dos veces** —una sin `APP_CONNECTOR_KEY` y otra de
  vuelta con ella, en el `finally`— y espera a que responda cada vez.
- `waitForBackend` (`connector.mjs:65`) se concede `{ timeout: 90_000 }` por espera.
- El presupuesto de la prueba era el de `playwright.config.mjs`: **30 s**.

Una espera interior de 90 s bajo un presupuesto exterior de 30 s **no se puede honrar nunca**: la
prueba muere a los 30 s aunque el backend fuese a levantar en el segundo 31. Con la máquina cargada
—cinco carriles a la vez— una recreación de Compose pasa de 30 s con holgura, y eso es exactamente
lo que ocurrió: el 502 es el proxy web contestando mientras el backend aún arranca.

La segunda roja es **consecuencia de la primera, no un defecto propio**: al abortar la prueba a los
30 s, el `finally` que restaura la clave quedó a medias, y «estado sin conexión» encontró el backend
todavía levantándose (`502 !== 200` en el `loginSession` de su propio fixture). Que las doce pruebas
siguientes pasaran confirma que la pila se restauró sola unos segundos después.

Esto explica también por qué la campaña anterior del artesano fue verde y ésta no: **la prueba
dependía de que la máquina recreara el backend en menos de 30 s.** Era verde por suerte de carga, no
por presupuesto.

### El arreglo, mínimo y en el sitio correcto

`e2e/github-connector.spec.mjs`, en la única prueba que recrea la pila:

    test.setTimeout(240_000);

con el porqué escrito encima: dos esperas de hasta 90 s más las dos recreaciones de Compose no caben
en 30 s. **No se relaja ninguna aserción ni se pierde cobertura**: el estado `deshabilitado` se
sigue alcanzando recreando el backend sin clave de verdad, y se le siguen exigiendo el `role="alert"`
con «configuración del servidor», la ausencia del campo token, la ausencia del botón de importar,
axe y la geometría. Sólo se le da a la prueba el tiempo que su propio andamiaje ya declaraba
necesitar. Es el mismo remedio que `github-connector-native-zoom.spec.mjs` ya aplicaba
(`test.setTimeout(180_000)`) por la misma razón.

**El rojo está acreditado por ejecución real**, no por construcción: la traza de arriba es de la
campaña ejecutada, con su mensaje y su línea.

---

## Paso 3 — Segunda campaña: **16 de 16 en verde**

Mismo comando, mismo puerto 18096, pila nueva (`organizationweb-e2e-66604`), `retries: 0`, un
worker. Ésta es la evidencia que el juez pide.

    Running 16 tests using 1 worker

      ✓  1 github-connector-native-zoom.spec.mjs:37 › @s42 los tres anchos al 200 % de zoom nativo, sin recorte ni desplazamiento (7.1s)
      ✓  2 github-connector.spec.mjs:124 › @s42 estado deshabilitado: el servidor sin clave de conectores (30.7s)
      ✓  3 github-connector.spec.mjs:148 › @s42 estado sin conexión (6.2s)
      ✓  4 github-connector.spec.mjs:157 › @s42 estado conectada, alcanzado conectando de verdad (7.0s)
      ✓  5 github-connector.spec.mjs:182 › @s42 estado importando y estado resultado, importando de verdad (9.2s)
      ✓  6 github-connector.spec.mjs:209 › @s42 reimportar omite lo ya importado (9.1s)
      ✓  7 github-connector.spec.mjs:225 › @s42 estado de error recuperable: cuota agotada con su plazo (6.6s)
      ✓  8 github-connector.spec.mjs:243 › @s42 estado inválida, tras un token que GitHub deja de aceptar (8.7s)
      ✓  9 github-connector.spec.mjs:267 › @s42 el recorrido con Tab alcanza los controles en el orden en que se leen (5.6s)
      ✓ 10 github-connector.spec.mjs:300 › @s42 el foco es visible en cada control del contenido (4.2s)
      ✓ 11 github-connector.spec.mjs:348 › @s42 Escape cancela la confirmación de desconexión y devuelve el foco (6.5s)
      ✓ 12 github-connector.spec.mjs:372 › @s42 desconectar desde el teclado borra la conexión y conserva lo importado (5.0s)
      ✓ 13 github-connector.spec.mjs:399 › @s42 «/integraciones» enlaza el conector y el menú no gana ninguna entrada (5.4s)
      ✓ 14 github-connector.spec.mjs:414 › @s34 @s42 el token no queda en ningún almacén del navegador (4.3s)
      ✓ 15 github-connector.spec.mjs:434 › @s34 la respuesta de auditoría de sesión no lleva el token ni rastro del conector (6.5s)
      ✓ 16 github-connector.spec.mjs:452 › @s37 un token rechazado se explica junto al campo y lo vacía (7.3s)

      16 passed (2.2m)

**16 pruebas, 16 verdes, 0 rojas, 2,2 minutos.** Exit code 0.

Detalle que confirma el diagnóstico del paso 2 en lugar de taparlo: la prueba del estado
`deshabilitado` tardó **30,7 s**. Con el presupuesto viejo de 30 s habría muerto **por siete
décimas**. No era una prueba lenta ni un producto roto: era un presupuesto mal puesto, y la cifra lo
dice sola.

### Evidencia del zoom, regenerada sobre este árbol

`.e2e-work/github-connector-zoom/organizationweb-e2e-66604/evidence.json`:

| Ancho CSS | devicePixelRatio | scrollWidth | clientWidth | Controles | Violaciones axe |
| --- | --- | --- | --- | --- | --- |
| 320 | 3 | 312 | 312 | 5 | 0 |
| 768 | 3 | 760 | 760 | 5 | 0 |
| 1440 | 3 | 1432 | 1432 | 5 | 0 |

`dpr` 3 = 1,5 (pantalla al 150 %) × 2 (el zoom nativo), medido contra la base tomada antes de
ampliar. `scrollWidth == clientWidth` en los tres anchos: sin desplazamiento horizontal. Coincide
fila a fila con la campaña anterior, que es lo esperable —quitar un byte del formato en reposo no
cambia el pintado—, pero **ahora está medida sobre el árbol vigente**, que era justamente la
objeción.

`progress/ux_github_connector.md` queda actualizado con esta campaña y con la razón por la que la
anterior no valía.

### Lo que esto cierra del veredicto

| Cambio requerido por el juez | Estado |
| --- | --- |
| 1 (BLOQUEANTE) `spec.mjs:168` → `String(12 + TOKEN.length + 16)` | **cerrado**: ya estaba en el árbol (`d418a5d`) y **verificado por ejecución**, no por lectura |
| 2 (BLOQUEANTE) volver a pasar los dos specs sobre el árbol actual y regenerar la evidencia | **cerrado**: 16 de 16, `evidence.json` regenerado, matriz UX actualizada |
| C6 «@s42 no revalidable» | **revalidado**: los dieciséis recorridos de @s42 pasan sobre el formato de hoy |

El juez escribió (`:340`): «Cuando salga verde, esta feature está aprobada por mi parte: no tengo
ninguna otra objeción pendiente.» Salió verde.

---

## Paso 4 — Las clases de backend que el cambio de formato toca

Por clase concreta y **nunca más de dos por invocación**, según `progress/carriles/REGLAS.md:24`.
Ruta absoluta a `gradlew.bat`, `--no-daemon`.

### Tanda A — cifrado y cableado

    gradlew.bat test --tests "...adapter.connectors.AesGcmSecretCipherTest" \
                     --tests "...adapter.config.GithubConnectorWiringTest" --no-daemon
    BUILD SUCCESSFUL in 29s

| Clase | Pruebas | Fallos | Errores | Omitidas | Tiempo |
| --- | --- | --- | --- | --- | --- |
| `AesGcmSecretCipherTest` | **18** | 0 | 0 | 0 | 0,054 s |
| `GithubConnectorWiringTest` | **15** | 0 | 0 | 0 | 0,152 s |

**33 en verde.** Cifras leídas de los XML de `build/test-results/test/`, no del resumen.

Son las dos clases que fijan el formato en reposo por número:
`AesGcmSecretCipherTest:38` afirma `assertEquals(12 + TOKEN.length() + 16, sealed.length)` y
`GithubConnectorWiringTest:46` afirma `hasSize(12 + "ghp_secreto123".length() + 16)` —que es
literalmente el `12 + 14 + 16` del contrato de @s1—. Ambas verdes: el productor y los dos oráculos
del formato coinciden.

### Tanda B — persistencia y caso de uso

    gradlew.bat test --tests "...adapter.persistence.GithubConnectorPersistenceTest" \
                     --tests "...application.ConnectGithubTest" --no-daemon

    BUILD SUCCESSFUL in 30s

| Clase | Pruebas | Fallos | Errores | Omitidas | Tiempo |
| --- | --- | --- | --- | --- | --- |
| `GithubConnectorPersistenceTest` | **26** | 0 | 0 | 0 | 12,922 s (PostgreSQL real) |
| `ConnectGithubTest` | **16** | 0 | 0 | 0 | 0,341 s |

**42 en verde.** `GithubConnectorPersistenceTest` es la que escribe contra la columna con el `CHECK`
de `octet_length` y contra PostgreSQL de verdad: si la cota del `CHECK` chocara con lo que la
aplicación guarda, saltaría aquí. No salta, y el porqué está en el paso 1 (usa un relleno de 43
octetos, dentro de la cota, porque mide persistencia y no cifrado).

`ConnectGithubTest` es la que cierra el círculo sin depender de longitudes: cifra, descifra de ida y
vuelta, exige que el propietario ajeno **no** pueda descifrar (el AAD) y que dos escrituras del mismo
token den textos cifrados distintos (el nonce nuevo por escritura). Verde.

**Total del paso 4: 75 pruebas de backend, 75 verdes, 0 fallos, 0 errores, 0 omitidas.**
Ninguna suite completa: cuatro clases en dos invocaciones, un solo contenedor PostgreSQL vivo por
tanda.

---

## Paso 5 — La nota menor del `headless: false`: no se toca, y aquí está el porqué

El juez la marcó **no bloqueante** (`:176-178`): `headless: false` y `channel: "chromium"` hacen la
prueba de zoom dependiente de un entorno gráfico.

**No la cambio, y el motivo no es la prisa: es que el cambio destruiría lo que la prueba mide.**

El zoom nativo se aplica con `chrome.tabs.setZoom` desde una extensión MV3 efímera, y esa es la
única forma de ampliar **de verdad**: `deviceScaleFactor` cambia la densidad de píxeles pero no
produce el reflujo que @s42 quiere medir —«sin desplazamiento horizontal ni contenido recortado» a
320 px con zoom 200 %—. Una extensión con `background.service_worker` y permiso `tabs` necesita un
proceso de navegador real; el `--window-size` de 1440×1000 y la medida del cromo de la ventana
(`width * 2 + chrome_`) también dependen de que haya ventana. Quitar `headless: false` convertiría
una prueba que hoy **falla ruidosamente si el zoom no se aplica** (`expect(zoom).toBe(2)` sobre lo
que devuelve `chrome.tabs.getZoom`) en una que no podría aplicarlo: el remedio sería peor que la
nota.

Tres hechos que acotan el riesgo real:

1. **Es el precedente del repositorio, no una excepción**: `e2e/reschedule-native-zoom.spec.mjs`
   paga el mismo precio por la misma razón, y el javadoc de la spec lo declara (`:13-16`).
2. **Se ejecuta de verdad y no se salta en silencio**: no hay `test.skip`, ni `fixme`, ni condición
   sobre `project.name`. Si no hay entorno gráfico, **falla**; no pasa fingiendo.
3. **Está medida hoy, dos veces**: 12,2 s en la primera campaña y 7,1 s en la segunda, con
   `evidence.json` regenerado.

Si el CI acaba sin entorno gráfico, la decisión correcta no es volverla headless —perdería el
oráculo— sino darle etiqueta propia y un runner con pantalla. Eso es política de pipeline, no de
esta feature, y queda anotado para el orquestador.

---

## Paso 6 — `bin/harness init`: **deliberadamente no ejecutado**

El juez dejó constancia (`:321`, `:357`) de que no llegó a ejecutarlo por la restricción de
recursos, y lo dejó como puerta pendiente del cierre de sesión.

**Yo tampoco lo ejecuto, y es una decisión, no un olvido.** `bin/harness init` arrastra la suite
completa: **49 clases de test con su propio `PostgreSQLContainer` estático**, es decir 49
contenedores PostgreSQL vivos a la vez. Hay **cuatro carriles más** trabajando en esta misma máquina
ahora mismo, y `progress/carriles/REGLAS.md:7-11` lo prohíbe explícitamente por escrito: el 8 de
septiembre siete carriles simultáneos dieron **rendimiento cero** por saturación con 130-170
contenedores.

**Le corresponde al orquestador, al integrar, con la máquina para él.** Los checkpoints C1 y C4
siguen, por tanto, sin marcar por la misma razón por la que el juez no los marcó, y no por falta de
cobertura.

---

## Mutación: **no ejecutada**, por instrucción expresa del coordinador

Mi encargo original pedía reejecutar las dos campañas de mutación de la feature 27 (backend y
frontend, umbral 0,80) sobre el árbol actual. **El coordinador retiró ese punto en mitad del
trabajo**, con estas palabras: «**NO ejecutes campañas de mutación**: tardan demasiado y las corre el
orquestador después.»

Así que **no tengo dos puntuaciones de mutación que dar, y no las invento**. Lo que sí puedo dejar
acotado para quien las corra:

- **Siguen haciendo falta.** El razonamiento del juez sobre la campaña E2E vale igual aquí: las
  campañas de mutación anteriores se midieron sobre el formato con byte de versión. Un mutante que
  toque el desplazamiento del nonce o el corte del sellado en `AesGcmSecretCipher` se mata hoy con
  aserciones distintas de las de entonces.
- **Hay un aviso que el que las corra debe leer antes**: `progress/auditoria_colisiones_25_28_30.md`
  documenta que `backend/build.gradle.kts` nombra en `targetClasses` dos globs muertos
  (`...adapter.crypto.AesGcmSecretCipher*` y `...adapter.crypto.ConnectorCipher*`) que **ya no casan
  con ninguna clase**: la unificación movió el adaptador a `adapter.connectors`. Si el alcance del
  conector arrastra ese mismo error, la campaña saldría con una puntuación cómoda **sin generar un
  solo mutante del cifrador**, que es justamente la clase que el cambio de formato tocó. Una
  puntuación alta sobre cero mutantes no es una puerta: es un espejismo. Conviene comprobar el
  recuento de mutantes generados, no sólo el porcentaje.

**Puerta C7: pendiente**, correctamente después de ésta, tal como el juez la ordenó.

---

## Resumen para el juez

| Lo que exigías | Cómo queda |
| --- | --- |
| BLOQUEANTE 1 — la constante de 48 octetos | **cerrado y verificado ejecutando** |
| BLOQUEANTE 2 — los dos specs verdes sobre el árbol actual | **cerrado: 16 de 16, 2,2 min** |
| Menor — nota del `headless: false` | **razonado por escrito**: no se toca porque el cambio destruiría el oráculo del zoom |
| Menor — cota del `CHECK` de `V25` | **recalculada (29–283) y documentada**; no se aplica aquí porque exige migración nueva y colisionaría con otros carriles |
| `bin/harness init` | **no ejecutado a propósito**; es del orquestador |
| Mutación | **no ejecutada**: el coordinador la retiró del encargo y la corre él |

Números medidos hoy, ninguno declarado de memoria:

- **E2E**: 16 pruebas, **16 verdes**, 0 rojas, 2,2 min, `retries: 0`, un worker, puerto 18096.
- **Backend**: 4 clases, 75 pruebas, **75 verdes**, 0 fallos, 0 errores.
- **Zoom**: `dpr` 3 en 320/768/1440, `scrollWidth == clientWidth` en los tres, 0 violaciones axe.

Lo único que cambia en el producto respecto a lo que ya juzgaste es **nada**: el único fichero de
código que he tocado es la spec de E2E, y sólo para darle a una prueba el presupuesto de tiempo que
su propio andamiaje declaraba necesitar. Ninguna aserción se ha relajado, ninguna cobertura se ha
perdido.

## Ficheros tocados

| Fichero | Compartido con otros carriles |
| --- | --- |
| `e2e/github-connector.spec.mjs` | no, es de este carril |
| `progress/revalidacion_github_connector.md` | no, nuevo |
| `progress/ux_github_connector.md` | no, es de este carril |

**Ningún fichero de la lista de conflictos de `REGLAS.md:69-78`.** No se ha tocado `src/`, ni
`scripts/`, ni `docker-compose.yml`, ni `feature_list.json`, ni ninguna migración.
