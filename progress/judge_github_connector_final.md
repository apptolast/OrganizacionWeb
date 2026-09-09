# Review — feature 27 `github_connector` (segunda lectura)

**Veredicto:** REJECTED

Superficie juzgada: `4023ba3` y `e597283`, integrados en `main` por el merge `fa49fd7`, releídos
**sobre el `main` de hoy** (`b92d518`), que además incorpora las features 25, 28, 30 y la 26
aprobada.

**Los cinco bloqueantes de mi dictamen anterior están cerrados, y bien cerrados.** No es maquillaje:
el `Escape` existe en el producto, la placebo ha desaparecido, el servicio falso es real, el zoom
nativo se ejecuta de verdad y la matriz UX está escrita con sus límites por delante. Si esto se
juzgara contra el árbol en que se escribió, sería APPROVED.

**Rechazo por un solo defecto, ajeno al artesano y nacido de la integración posterior**: la
unificación de `SecretCipher` (`b376c8c`, entrada en `main` por `5b9937e`) quitó el byte de versión
del formato en reposo y **nadie actualizó la aserción de bytes del E2E de la 27**, que es
justamente la prueba que yo exigí para que `conectada` dejara de fingirse. Hoy, en `main`, esa
aserción es aritméticamente falsa.

---

## El bloqueante

`e2e/github-connector.spec.mjs:164-168`

    expect(
      sql(
        "SELECT octet_length(token_ciphertext) FROM connector_connections WHERE owner_id=..."
      ),
    ).toBe(String(1 + 12 + TOKEN.length + 16));

Con `TOKEN = "ghp_token_de_pruebas"` (20 caracteres) espera **49**. Pero
`adapter/connectors/AesGcmSecretCipher.java:51-54` ya no antepone byte de versión:

    var output = new byte[NONCE_BYTES + sealed.length];   // 12 + (20 + 16) = 48

El valor real es **48**. La prueba «@s42 estado conectada, alcanzado conectando de verdad» falla de
forma determinista sobre el `main` actual. No es una sospecha: es aritmética sobre dos ficheros que
he leído.

Cronología, para que quede claro de quién es el defecto y de quién no:

| Commit | Fecha | Qué hizo |
| --- | --- | --- |
| `4023ba3` → merge `fa49fd7` | 09-sep 15:05 | Escribe la aserción `1 + 12 + len + 16`. **Correcta entonces** |
| `b376c8c` → merge `5b9937e` | 09-sep 16:45 | Quita el byte de versión. Actualiza el backend, **no el E2E de la 27** |

El backend **sí** se actualizó: `GithubConnectorWiringTest:46` afirma hoy
`hasSize(12 + "ghp_secreto123".length() + 16)`, que es literalmente el `12 + 14 + 16` que pide @s1.
Ejecutado por mí: verde. **@s1 se cumple ahora mejor que antes**, tal como esperaba el coordinador:
antes la prueba fijaba 1+12+14+16 y el contrato pedía 12+14+16; ahora coinciden exactamente.

Consecuencia que sí toca juzgar: **la evidencia de @s42 es anterior a la integración**.
`progress/ux_github_connector.md` declara «15 de 15 en verde» y la bitácora lo repite; esa ejecución
es legítima, pero se hizo contra el formato con byte de versión. No puedo certificar @s42 —que fue
*el* bloqueante— con una campaña verde obtenida sobre un formato en reposo que ya no es el que hay.
Hay que corregir la constante y **volver a pasar los dos specs**, regenerando `evidence.json`.

---

## Lo que exigiste que verificara, punto por punto

### 1. Escape — VERIFICADO. Existe, se pulsa, y el foco vuelve.

`frontend/src/github-connector.tsx:149-159`: `useEffect` que registra `keydown` en `document`
**sólo mientras `confirming`** y se desregistra al cerrar. El argumento del artesano es correcto y
lo he comprobado en el código: al abrir la confirmación el botón «Desconectar» se sustituye por el
par Confirmar/Cancelar, el foco cae a `body`, y un `onKeyDown` en el grupo nunca recibiría la tecla.
Escuchar en `document` es aquí la decisión correcta, y acotarla a `confirming` evita robarle Escape
al resto de la pantalla.

El retorno del foco no es un efecto colateral: `cancelDisconnect` (`:246-249`) marca
`focusDisconnect.current = true` y el `useLayoutEffect` lo devuelve tras pintar. Y el refactor de
paso está bien hecho: el `onClick` inline de «Cancelar» pasa a llamar a la misma función, de modo
que ratón y teclado comparten un único camino.

Tres pruebas de unidad, y **pulsan la tecla**:

- `github-connector.test.tsx:476` — `userEvent.keyboard("{Escape}")`, y afirma las tres cosas: la
  confirmación desaparece, **cero DELETE**, y el botón «Desconectar» **`toHaveFocus()`**.
- `:491` — la misma desde el foco en «Confirmar desconexión».
- `:504` — **la que le da mordida al conjunto**: Escape *fuera* de la confirmación no altera la
  pantalla. Sin ella, un escuchador global permanente pasaría; con ella, no.

E2E `github-connector.spec.mjs:339-361`: abre con `Enter` sobre «Desconectar», pulsa
`keyboard.press("Escape")`, exige `toBeFocused()` y comprueba en la base que la conexión **sigue
ahí**. Ni un `click()` bajo un título que diga «Escape». Corregido de verdad.

Ejecutado por mí: `github-connector.test.tsx` + `github-connector-routing.test.tsx` → **33 en verde**.

### 2. La placebo — BORRADA, y lo que la sustituye discrimina.

`expect(states).toHaveLength(7)` ya no existe; tampoco el array literal `states`. En su lugar hay
**siete pruebas, una por estado** (`:124-254`), y ninguna pasaría si el estado no se alcanzase,
porque cada una afirma contenido propio de ese estado antes de auditar:

| Estado | Cómo se alcanza | Qué lo discrimina |
| --- | --- | --- |
| deshabilitado | `withConnectorDisabled` **recrea el backend sin clave** | `role="alert"` con «configuración del servidor», y **ausencia** del campo token y del botón importar |
| sin conexión | navegación limpia | los dos campos visibles |
| conectada | `connectThrough` por la interfaz | «Conectada», el repositorio, **y el `login` que devuelve el falso** — prueba que hubo ida y vuelta |
| importando | clic real en importar | `role="status"` con «Importando issues…» y **sin `%`** |
| resultado | el mismo recorrido | «Creadas 1 / Omitidas 0 / **Fallidas 1**», enlace al proyecto y foco en el `h1` |
| error recuperable | repositorio `limitado` → 429 | «Reintenta en **90** segundos» y el botón **habilitado** |
| inválida | `UPDATE status=invalid` + recarga | «Conexión inválida», «Reconectar», sin botón de importar |

Los contadores son la clave de la mordida: «Creadas 1, Fallidas 1» sólo sale si el falso sirvió las
dos issues con una de título en blanco (`server.mjs:24`, `blankTitleAt`), y «Omitidas 3» en el
reimportar (`:200-214`) sólo si la primera importación escribió los tres enlaces. Un estado no
alcanzado revienta la prueba. **Esto es lo contrario de una placebo.**

Único estado que aún se fabrica con SQL es `inválida`, y me parece correcto: exige que GitHub deje
de aceptar un token ya aceptado, que el falso no puede representar sin un canal de control —y no
tenerlo es una virtud de su diseño—.

### 3. El servicio falso — CORRECTO, y **la lista blanca no se ha tocado**.

Lo he comprobado del modo más duro disponible, y el resultado es limpio:

    git log --oneline a08d3be..HEAD -- "**/GithubApiBase.java"   → (vacío)
    git diff  a08d3be..HEAD -- "**/GithubApiBase.java"           → (vacío)

**Cero commits, cero bytes de diferencia.** `GithubApiBase.of` sigue exigiendo o exactamente
`https://api.github.com` sin puerto, o un host de loopback, y sigue rechazando ruta, consulta,
fragmento y credenciales. La tentación de «sólo para pruebas» no se ha materializado: el falso entra
por la puerta que **ya** estaba abierta —loopback, prevista desde el principio para pruebas—, no por
una rendija nueva.

El mecanismo es el acertado: `network_mode: "service:backend"` (docker-compose.yml) mete al falso en
el espacio de red del backend, así que éste lo alcanza en `http://127.0.0.1:9000`, que es loopback
**de verdad**, no un nombre de servicio que hubiera obligado a admitir hosts arbitrarios. Es la
solución que no toca la defensa.

El perfil aísla de verdad: `profiles: [e2e]` hace que Compose **no** levante el servicio salvo
`--profile e2e`, que sólo aparece en `scripts/e2e.mjs:34-36` y en `e2e/support/connector.mjs:41-42`.
Un `docker compose up` normal no lo arranca. Además `scripts/e2e.mjs:50` fija
`APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES: "false"`: la guarda SSRF de la 28 queda **activada** en el
E2E, no relajada.

Y sigue siendo cierto que nada habla con GitHub: el nombre oficial ya ni aparece en la configuración
de la pila (antes era el puerto de descarte `:9`, ahora el falso).

El falso está bien construido: sin canal de control, la conducta depende del nombre del repositorio
(`server.mjs:19-31`), así que cada recorrido es determinista y no hay estado que se filtre de una
prueba a otra. Buena decisión de diseño.

`withConnectorDisabled` (`e2e/support/connector.mjs`) merece una mención: alcanza el estado
`deshabilitado` **recreando el backend sin clave** y restaurándolo en el `finally`, en vez de fingir
un 503 desde el cliente. Y el comentario `:25-29` documenta una trampa real —el entorno del proceso
gana sobre `--env-file`, así que la clave se pasa explícitamente—. Eso es un fallo que costó
encontrar y que queda escrito para el siguiente.

### 4. El zoom nativo — SE EJECUTA. No hay skip silencioso.

`e2e/github-connector-native-zoom.spec.mjs` no contiene `test.skip`, ni condición sobre
`project.name`, ni `fixme`. **La trampa que señalas no está aquí.** No podría estarlo: la prueba no
depende de los proyectos de la configuración, se abre su propio navegador
(`chromium.launchPersistentContext`, `:68`) con una extensión efímera.

Y sobre todo, **falla ruidosamente si el zoom no se aplica**, que es la propiedad que pediste:

- `:123` — `expect(zoom, "el zoom nativo no llegó al 200 %").toBe(2)` sobre lo que devuelve
  `chrome.tabs.getZoom`.
- `:124-126` — `expect.poll(devicePixelRatio).toBe(baseline.dpr * 2)`, medido **contra la base
  tomada antes de ampliar** (`:112`), no contra una constante.
- `:170` — dentro del bucle, `expect(measured.dpr, "zoom perdido a N px")`: comprueba en **cada**
  ancho que el zoom sigue puesto.
- `:139-143` — `expect.poll(innerWidth).toBe(width)`: si la ventana no llega al ancho CSS pedido, se
  para. El cálculo `width * 2 + chrome_` con el cromo medido antes es correcto.

El `dpr` 3 de la evidencia (1,5 × 2) cuadra con una pantalla a 150 % y confirma que el 200 % se
aplicó encima. Y el barrido mide las dos cláusulas que el contrato nombra y que antes no tenían
oráculo: sin desplazamiento horizontal (`:172-175`) y **sin contenido recortado** (`:177-186`,
ningún control se sale por ninguno de los dos lados), más 44 × 44 y axe en los tres anchos.

Nota menor, no bloqueante: `headless: false` y `channel: "chromium"` hacen esta prueba dependiente
de entorno gráfico. Es el mismo precio que ya paga `e2e/reschedule-native-zoom.spec.mjs`, y está
declarado en el javadoc.

### 5. @s42 con el mismo rasero — AHORA SÍ, cláusula por cláusula.

| Cláusula de @s42 | Antes | Ahora |
| --- | --- | --- |
| los siete estados con axe | 3 de 7 | **[x] 7 de 7**, `:124-254`, tags `wcag2a/2aa/21aa/22aa/best-practice` |
| anchos 320/768/1440 | sólo en `conectada` | **[x]** en el barrido de zoom, y `assertLayout` en cada estado |
| **zoom nativo 200 %** | nunca se ejecutaba | **[x]** spec propia, con la evidencia medida |
| Tab alcanza los controles **en orden** | sin orden | **[x]** `:258-289`: compara la secuencia de tabulación con el orden del DOM, con `toEqual` |
| Enter | uno suelto | **[x]** `:347`, `:374-376` |
| **Escape** | no existía | **[x]** producto + 3 unitarias + 1 E2E |
| **foco visible** | sin oráculo | **[x]** `:291-337`, y bien hecho: recorre **con Tab** porque `:focus-visible` no responde a `focus()` programático —el propio artesano documenta ese falso negativo—, y exige `outlineStyle`, `outlineWidth >= 1` y color no transparente |
| 44 × 44 en todos los controles | sólo `conectada`, sólo `button, select` | **[x]** `geometry()` `:69-93` incluye `button, a, select, input, textarea`, **sin excluir enlaces**, en los siete estados |
| **sin contenido recortado** | sin oráculo | **[x]** `assertLayout:104-111` |
| errores por aria-live o foco | sólo en Vitest | **[x]** `role="status"` en `importando`, foco al `h1` en `resultado` |
| «/integraciones» enlaza, y el menú no gana entrada | [x] | [x] sigue |
| matriz de 30 filas | no existía | **[x]** `progress/ux_github_connector.md`, 30 filas contadas |

Y la excepción *Inline* de WCAG 2.2 §2.5.8, que en mi dictamen anterior estaba mal aplicada: el
artesano **la retira** y lo razona donde toca (`:64-68`), con el argumento correcto —los enlaces de
esta pantalla son el único contenido de su bloque—. Ahora los mide. Aceptado.

**Con el mismo rasero que apliqué a la 26 y a la 27 la primera vez, @s42 está cubierto.** Lo que me
falta no es cobertura: es una campaña verde sobre el árbol actual.

### 6. El 503 `CONNECTOR_KEY_MISMATCH` — correcto, y no tapa otro fallo.

`GithubConnectorController:174-184` mapea `SecretUndecipherableException` a 503 con ese código. La
elección de 503 es la correcta: es configuración del servidor, no error de quien llama.

`GithubConnectorApiTest:689-707` discrimina: hace que el caso de uso lance la excepción y exige
**503 + `application/problem+json` + `$.code == CONNECTOR_KEY_MISMATCH`**, y remata con
`assertThat(body).doesNotContain(TOKEN)`. Con el mock lanzando esa excepción concreta, lo único que
puede hacer pasar la prueba es el `@ExceptionHandler` nuevo: no tapa un fallo distinto.

Sobre el rojo antes del verde: la bitácora lo afirma explícitamente («ROJO antes que verde»,
`tdd_github_connector.md:490`) y es **estructuralmente creíble** —antes del handler el resultado era
un 500 genérico, que es exactamente lo que la prueba niega—. Lo doy por bueno.

La semántica **no** se ha perdido en la unificación, que era el riesgo que yo mismo señalé:
`ImportGithubIssues:129-132` traduce el `Optional` vacío con
`.orElseThrow(SecretUndecipherableException::new)`. El fallo no se ha vuelto silencioso.
**Pero el comentario que hay justo encima ha quedado mintiendo** (ver Calidad).

---

## Integración: qué le ha hecho a la 27 el `main` de hoy

- **`SecretCipher` unificado**: `enabled()` **se conserva** —era mi preocupación mayor, porque de él
  cuelgan los cinco `if (!cipher.enabled())` que sostienen @s3—. `AesGcmSecretCipher:34-37` lo
  implementa. @s3 sigue en pie.
- **`decrypt` → `Optional<String>`**: un único punto de llamada, traducido correctamente. Bien.
- **Byte de versión fuera**: el backend, correcto y **más fiel a @s1 que antes**. El E2E, sin
  actualizar. Es el bloqueante.
- **`AddressPolicy` unificada**: no toca `GithubApiBase`, que sigue byte a byte igual. La 27 no se ha
  visto afectada.
- **Ejecutado por mí sobre el `main` actual**: `GithubConnectorWiringTest`, `AesGcmSecretCipherTest`
  y `GithubApiBaseTest` → **verde**. `github-connector.test.tsx` +
  `github-connector-routing.test.tsx` → **33 en verde**.

---

## Cobertura de escenarios (@s ↔ test) — sólo lo que cambia

Los 41 escenarios que ya di por cubiertos siguen cubiertos. Cambios:

- @s1 [x] **mejor que antes**: `GithubConnectorWiringTest:46` fija ahora `12 + 14 + 16`, que es
  literalmente lo que dice el contrato. Antes fijaba un byte de más.
- @s4 [x] con la enmienda ratificada por el propietario (fuera «cadena vacía»). Las tres filas
  restantes —16 bytes, 33 bytes, no-base64— tienen prueba. **No lo juzgo**, según instrucción.
- @s32 [x] **las cinco filas**: se añaden `s32_writingFromAnotherOriginIsForbidden` (las dos filas
  «Origin de otro sitio») y `s32_writingWithAnInvalidCsrfTokenIsForbidden`. Y un control con el
  origen propio, `s32_theOwnOriginIsAccepted`, **para que las tres no pasen por vacuidad**: sin él,
  una guarda que rechazase todo también habría pasado. Ese cuarto test es lo que convierte los otros
  tres en prueba.
- @s34 [x] **las seis filas**: se cierra «respuesta de auditoría de sesión» consultando
  `GET /api/session` tras conectar.
- @s42 [~] **cubierto en diseño y en código; evidencia no revalidable** por el bloqueante.

Fuera de contrato pero sano: el 503 `CONNECTOR_KEY_MISMATCH` cierra el «menor 7» de mi dictamen.

## Disciplina TDD

- **¿Producción sin test que la pida? NO.** Lo nuevo de producción son dos cosas y las dos tienen
  prueba que las exige: el escuchador de Escape (`github-connector.tsx:149-159`, con tres pruebas que
  pulsan la tecla, incluida la negativa de `:500`) y el `@ExceptionHandler`
  (`GithubConnectorController:179`, con `GithubConnectorApiTest:690`). El servicio falso es andamiaje
  de pruebas, no producción, y vive fuera de `src/`.
- **¿Evidencia de Rojo-Verde-Refactor? SÍ.** Y con la marca de honestidad que más valoro: la bitácora
  (`:466-475`) registra **cuatro rojos que corrigieron la prueba y no el código**, entre ellos que su
  propio oráculo de foco visible daba **falsos negativos** por `:focus-visible`, y que
  `getByText("Creadas 3")` era ambiguo porque el resumen y la región `aria-live` repiten los
  contadores. Reconocer que el oráculo estaba mal es lo contrario de maquillar.
- **Corrige su propia cifra**: dice 56 de frontend. **Medido por mí: 28 + 5 + 23 = 56.** Exacto.
  La corrección del 68 de la vez anterior queda saldada.
- **Registra el desvío de @s4** que antes daba por cubierto, y explícitamente **no toca el código**,
  dejándolo en la puerta del coordinador. Trazabilidad arreglada.

## Calidad (lente de artesano)

Lo bueno:

- **El escuchador de Escape acotado a `confirming`**, con el porqué escrito (`:149-151`): explica una
  *fuerza* —el botón que tenía el foco se sustituye y el foco cae a `body`—, no lo que hace el
  código. Es el tipo de comentario que sí vale.
- **`cancelDisconnect` extraída**: un solo camino para ratón y teclado. El refactor correcto.
- **`geometry()` y `assertLayout()`**: una función que mide, otra que juzga, y mensajes de fallo que
  **nombran el estado y el control** (`:103`). Cuando esto se rompa dentro de seis meses, el mensaje
  dirá dónde.
- **El falso sin canal de control**: la conducta se deriva del único dato que la prueba escribe en la
  interfaz. Cero estado compartido. Es una decisión de diseño, no una comodidad.
- **`progress/ux_github_connector.md` pone los límites ANTES de los resultados** (siete puntos: sin
  personas, un solo motor, sin lector de pantalla real, sin `forced-colors`, el falso no es GitHub,
  Doherty sin cronómetro). Y cada una de las **30** filas marca **medido** o **heurístico**. Cumple
  `AGENTS.md:51` en la letra y en el espíritu: no infiere accesibilidad desde axe. Es de las mejores
  matrices del repositorio.

Hallazgos:

1. **`e2e/github-connector.spec.mjs:168`** — el bloqueante. `1 + 12 + TOKEN.length + 16` da 49 contra
   48 reales.
2. **`backend/src/main/java/com/apptolast/organization/application/ImportGithubIssues.java:126-128`**
   — comentario **obsoleto que ahora miente**: dice «Hoy nadie la captura y acaba en 500; queda
   anotado como defecto latente de la 27». Es falso desde `4023ba3`:
   `GithubConnectorController:179` **sí** la captura y devuelve 503. Lo escribió el carril de la 28
   sin ver el arreglo de la 27. Un comentario que describe mal el presente es peor que ninguno.
3. **`e2e/github-connector.spec.mjs:31-38`** — el `forget()` sigue interpolando el propietario en SQL
   por concatenación. Constante del propio fichero, hoy inocuo; sigo sin querer que se copie.
4. **`frontend/src/github-connector.tsx:264`** — `<main id="proyectos">` en la pantalla del conector.
   Deuda del proyecto, ya señalada, no de este carril.
5. **`V25__github_connector.sql:13`** — `CHECK (octet_length(token_ciphertext) BETWEEN 30 AND 284)`.
   Sin el byte de versión el mínimo real de un token de 1 carácter es 29: la cota inferior quedó
   desajustada por el mismo cambio de formato. Inalcanzable en la práctica —`PersonalAccessToken`
   sólo acota el máximo, 255—, pero **la cota se escribió para un formato que ya no existe** y
   conviene recalcularla al tocar el punto 1.

**Seguridad: nada nuevo que objetar.** Lo revisado hoy —falso en loopback, perfil `e2e`, guarda SSRF
activada, lista blanca intacta— no abre nada. El 503 nuevo no revela información: la prueba afirma
que el cuerpo no contiene el token.

## Checkpoints

- **C1** [~] Ficheros y documentación presentes. `bin/harness init` **no ejecutado**: es la suite
  completa y hay una verificación corriendo en esta máquina (instrucción de sesión). Sin marcar.
- **C2** [ ] `feature_list.json` mantiene la 27 en `in_progress`; varias features a la vez es
  decisión de carriles del coordinador, no defecto de la 27.
- **C3** [x] Capas respetadas; el falso vive en `e2e/`, fuera de `src/`.
- **C4** [~] Todo lo nuevo tiene prueba. `bin/harness test` completo no ejecutado.
- **C5** [ ] `progress/history.md` sigue sin entrada de este carril.
- **C6** [ ] **@s42 no revalidable**: su E2E contiene una aserción hoy falsa.
- **C7** [ ] Mutación pendiente (puerta del `mutation_tester`), correctamente **después** de ésta.

## Cambios requeridos

1. **(BLOQUEANTE)** `e2e/github-connector.spec.mjs:168`: cambiar `String(1 + 12 + TOKEN.length + 16)`
   por `String(12 + TOKEN.length + 16)`, que es el formato vigente tras `b376c8c`. Mantener la
   aserción: sigue siendo lo que impide que `conectada` se finja.
2. **(BLOQUEANTE)** Volver a ejecutar `e2e/github-connector.spec.mjs` y
   `e2e/github-connector-native-zoom.spec.mjs` **sobre el `main` actual**, y actualizar la evidencia
   de `progress/ux_github_connector.md` (y `evidence.json`) con esa campaña. La actual es anterior al
   cambio de formato. **Cuando salga verde, esta feature está aprobada por mi parte**: no tengo
   ninguna otra objeción pendiente.
3. **(Menor)** `ImportGithubIssues.java:126-128`: reescribir el comentario. La excepción **sí** se
   captura y **sí** sale como 503 `CONNECTOR_KEY_MISMATCH`.
4. **(Menor)** Revisar la cota inferior del `CHECK` de `V25__github_connector.sql:13` a la luz del
   formato sin byte de versión.

## Para el coordinador

- **Esto es un defecto de integración, no del artesano de la 27.** Su trabajo cerró los cinco
  bloqueantes con solvencia; lo rompió un merge posterior que actualizó el backend y se dejó el E2E.
  Vale la pena preguntarse **por qué no lo cazó nadie**: la respuesta es que el E2E de la 27 no se ha
  vuelto a ejecutar desde `5b9937e`.
- **Regla que este caso sugiere**: cuando un merge cambie un formato en reposo, hay que buscar sus
  aserciones **en el E2E**, no sólo en las pruebas de unidad. Un `grep octet_length e2e/` habría
  bastado.
- El desvío de @s4 queda registrado por el artesano y ratificado por el propietario; yo no lo juzgo.
- `bin/harness init` sigue sin ejecutarse en este veredicto por la restricción de recursos: es puerta
  pendiente para el cierre de sesión.
