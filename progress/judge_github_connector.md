# Review — feature 27 `github_connector`

**Veredicto:** REJECTED

Superficie juzgada: `git log --oneline 081ec08..claude/github-connector` (18 commits, 80 ficheros),
integrada en `main` por el merge `a08d3be`. Contrato: `features/github_connector.feature` @s1–@s42,
con la enmienda de @s31 del 9 de septiembre de 2026.

El backend de este carril es lo mejor que he revisado en este repositorio: la criptografía está bien
construida y bien probada, la defensa contra SSRF es una lista blanca cerrada validada al arrancar, y
el token es **estructuralmente** irregistrable porque ningún método del puerto de bitácora lo admite.
Confirmo 259 pruebas de backend en verde por ejecución propia.

**El bloqueante es @s42, y es el mismo por el que rechacé la feature 26.** Audita 3 de los 7 estados,
nunca ejecuta el zoom nativo al 200 % que el contrato nombra dos veces, su prueba de Escape no pulsa
Escape (y el componente no implementa Escape), y `progress/ux_github_connector.md` no existe pese a
diecisiete precedentes, a `AGENTS.md:51` y a que `project-spec.md:2128` lo pide para esta feature por
su nombre. Aplico el mismo rasero: ni más duro ni más blando.

---

## Lo que he ejecutado yo (distinto de lo que cito del artesano)

Suites filtradas, sin suite completa, sin PIT y sin E2E (tres carriles más en la máquina):

| Clase / fichero | Pruebas | Fallos |
| --- | --- | --- |
| `adapter.GithubConnectorApiTest` | 43 | 0 |
| `adapter.config.GithubConnectorWiringTest` | 15 | 0 |
| `adapter.connectors.AesGcmSecretCipherTest` | 18 | 0 |
| `adapter.connectors.GithubApiBaseTest` | 22 | 0 |
| `adapter.connectors.HttpGithubIssueSourceTest` | 32 | 0 |
| `application.ConnectGithubTest` | 16 | 0 |
| `application.ImportGithubIssuesTest` | 36 | 0 |
| `application.GithubConnectorQueriesTest` | 8 | 0 |
| `adapter.persistence.GithubConnectorPersistenceTest` | 26 | 0 |
| `adapter.persistence.ConnectorExportExposureTest` | 2 | 0 |
| `adapter.logging.ConnectorAuditTest` | 5 | 0 |
| `domain.ExternalIssueTest` / `ExternalIssueCriterionTest` | 8 / 8 | 0 |
| `domain.GithubRepositoryTest` / `PersonalAccessTokenTest` | 12 / 8 | 0 |
| **Backend del carril** | **259 en 14 clases** | **0** |
| `pnpm vitest run` sobre los tres ficheros del carril | **53 en 3 ficheros** | 0 |

**Las 259 del backend quedan confirmadas por ejecución propia: la cifra del artesano es exacta.**
La del frontend **no**: dice «68 de frontend» y son **53**.

**`bin/harness init` NO se ha ejecutado**: `.harness/harness.mjs` lo define como lint +
`node scripts/project.mjs test`, es decir la suite completa (~20 contenedores PostgreSQL), prohibida
por la instrucción de recursos de esta sesión. Los checkpoints que dependen de él quedan sin marcar.

El E2E (12/12 en el puerto 18094) **lo cito del artesano, no lo he ejecutado**. Sí he leído
`e2e/github-connector.spec.mjs` línea a línea, y la lectura es la base del bloqueante.

---

## Los ocho puntos exigidos con lupa

### 1. Seguridad — VERIFICADA, sólida. Sin hallazgos de seguridad.

**Cifrado en reposo con el propietario como AAD.** `AesGcmSecretCipher` (AES-256-GCM, etiqueta de
128 bits, nonce nuevo por escritura, `updateAAD(ownerId)`, líneas 37-81). Formato:
`1 versión + 12 nonce + texto + 16 etiqueta`. Verificado por ejecución: `AesGcmSecretCipherTest` (18)
cubre las tres filas de @s2 —nonce distinto por escritura, otro `ownerId` falla por etiqueta, otra
clave falla por etiqueta— y `GithubConnectorWiringTest.s1_...` fija el tamaño exacto. El invariante
vive además en el esquema: `V25__github_connector.sql` impone
`octet_length(token_ciphertext) BETWEEN 30 AND 284`, así que nadie puede guardar texto en claro.
`ConnectorKeyRing.candidatesFor` ordena candidatas por el byte de versión pero **quien decide es la
etiqueta**, con el comentario que lo explica (línea 40): el byte de versión no es una autoridad.

**El token no sale por ningún canal.** Es lo mejor del carril:

- `application/ConnectorAudit.java` **no tiene ni un método que acepte el token**. No es disciplina
  de quien escribe el log: es imposible por el tipo. `ConnectorAuditTest:93` recorre los métodos por
  reflexión para que nadie añada uno después. Ejecutada: verde.
- `ConnectionView` no tiene hueco para el token; la ausencia es estructural, no una omisión al
  serializar. `GithubConnectorApiTest` compara el **conjunto exacto de claves serializadas**.
- `HttpGithubIssueSource:104-107`: la excepción de red se descarta entera y se sustituye por
  `IssueSourceException.unavailable()`, con el porqué escrito al lado.
  `s34_noFailureMessageEverCarriesTheToken` lo fija.
- `ConnectorExportExposureTest` (2, ejecutadas) fija que la exportación conserva catorce colecciones
  y ninguna del conector: si alguien añadiera las tablas nuevas al volcado, el texto cifrado acabaría
  en un fichero descargable.
- No existe `tokenHint` en esta feature (es de la 28): no hay pista parcial que filtrar.
- URLs: el token viaja sólo en la cabecera `Authorization`, nunca en la consulta
  (`HttpGithubIssueSource:88-97`).

**SSRF — la defensa es correcta y es de lista blanca, no de lista negra.** `GithubApiBase.of`
(27-34) exige: o exactamente `https://api.github.com` sin puerto, o un host de loopback
(`127.0.0.1`, `localhost`, `[::1]`), y además **rechaza ruta, consulta, fragmento y credenciales**
(`hasExtraParts`, 63-68). Eso cierra `https://user@api.github.com`,
`https://api.github.com.atacante.test` y `https://api.github.com/../..`. Se valida **al construir el
bean** (`ConnectorConfiguration:40-43`), nunca desde nada con alcance de petición, y la única parte
que aporta el usuario —el nombre del repositorio— llega ya validada por `GithubRepository`, cuyo
patrón no admite barra extra, dos puntos, arroba, dos puntos suspensivos ni espacios. Añadido:
`followRedirects(NEVER)` (línea 45), con la razón escrita —seguir un 302 llevaría el PAT a un destino
que elige el otro extremo—. 22 pruebas de `GithubApiBaseTest` y 32 de `HttpGithubIssueSourceTest`,
ejecutadas, verdes.

**Ninguna prueba habla con api.github.com — verificado, no citado.** Tres capas: `FakeGithub` es un
`com.sun.net.httpserver` en loopback con puerto efímero; los tests de wiring sólo tratan la base
oficial como *cadena*, sin abrir conexión; y en E2E, `scripts/e2e.mjs:14-19` fija
`APP_GITHUB_API_BASE=http://127.0.0.1:9`, el puerto de descarte, dentro del contenedor. Confirmado
leyendo el diff. La afirmación del artesano es cierta.

### 2. @s31 con el contrato enmendado — VERIFICADO. Las siete filas, ninguna por casualidad.

- **Cinco sin credencial**: `GithubConnectorApiTest:608-630` recorre las cinco operaciones y afirma
  `status().isUnauthorized()` **y** `jsonPath("$.code").value("UNAUTHENTICATED")`. El refuerzo que el
  artesano dice haber hecho está: afirma el **código**, no sólo el estado.
- **Dos del canal Bearer**: `:639-663` **stubea `authenticateCredential.authenticate` para devolver
  un `ApiCredentialAccess` válido con cuatro alcances**. La credencial es de verdad válida y de
  verdad se identifica; el 403 sale de la comprobación de rutas permitidas, no de un fallo de
  autenticación. Afirma `isForbidden()` **y** `API_SCOPE_DENIED`. No pasa por casualidad: si el
  filtro dejara pasar, `verifyNoInteractions(connect, importIssues, quota)` reventaría, y ese `quota`
  es el detalle fino —la credencial tampoco consume cuota del canal de integraciones—.

**Sin hallazgos. La enmienda está bien implementada y bien probada.**

### 3. @s42 y accesibilidad — **BLOQUEANTE**. Sección propia más abajo.

### 4. «El conector no añade entrada de menú» — VERIFICADO, la prueba afirma de verdad.

`github-connector-routing.test.tsx:66-76` y `:41-64`: renderiza la ruta raíz (ajena al conector),
captura `navHrefs()` —los destinos de los enlaces del menú «Principal», **en orden**—, desmonta,
navega a la ruta del conector y exige `toEqual(menuElsewhere)`. **No pasa siempre**: si el conector
añadiera una entrada, el array tendría un elemento más y la igualdad fallaría; si la reordenara,
también. Es estrictamente mejor que el `navLinkCount = 9` que sustituye, porque además de contar
compara identidad y orden. Refuerzo redundante y bienvenido: la aserción negativa sobre el destino y
la consulta negativa por rol dentro del menú. Ejecutada por mí: verde.

### 5. Los dos defectos del E2E — ARREGLADOS en la causa; el arreglo de compose no debilita nada.

1. **La clave del conector ausente en la pila de E2E.** Arreglado en la causa:
   `docker-compose.yml:26-30` acepta las tres variables y `scripts/e2e.mjs:41-45` fija clave de
   pruebas y base de descarte. **No debilita producción**: la clave se declara con expansión por
   defecto vacía, y `ConnectorConfiguration.configured` (98-100) traduce vacío a ausencia → conector
   deshabilitado → 503, que es lo correcto para un despliegue que no lo usa. Y sobre todo, la base de
   la API se declara con expansión por defecto `https://api.github.com`: **conserva la base oficial**,
   así que un despliegue real no hereda nada del entorno de pruebas. Correcto.
2. **El selector de proyecto medía menos de 44 × 44.** Arreglado en la causa: bloque
   `.github-connector` en `styles.scss:2160-2207`, con `min-width`/`min-height` de 44 px sobre
   `button, select`, `box-sizing: border-box` y **sólo tokens de tema** (`--ink`, `--editable`,
   `--control-border`, `--muted`): ningún color fijo, así que las guardas de modo oscuro de `main` no
   se ven afectadas.

   **Matiz**: como la regla CSS garantiza los 44 px por construcción, la prueba de E2E que los mide
   es hoy casi tautológica frente a esa regla —cazaría una regresión de especificidad, poco más— y no
   mide los campos de repositorio y token, que también son destinos de puntero.

### 6. Las dos aserciones relajadas — una razón es cierta, la otra está mal aplicada.

- **«El selector necesita un proyecto para tener geometría real»: CIERTO y correcto.** Un `select`
  sin opciones colapsa; medirlo vacío mide el vacío. La prueba crea un proyecto antes de medir
  (spec:169-176). No tapa nada.
- **«WCAG 2.2 §2.5.8 exime a los enlaces de texto en línea»: la norma dice eso, pero aquí está mal
  aplicada.** La excepción *Inline* cubre el destino que está **dentro de una frase o cuyo tamaño lo
  limita el interlineado de texto que no es destino**. Los dos enlaces de esta pantalla no cumplen
  eso: `github-connector.tsx:268-270` es un párrafo cuyo **único** contenido es el enlace
  «Integraciones», y `:429-433` es un enlace suelto dentro de la sección de resultado, sin texto
  alrededor. Ninguno está «en una frase». Y el selector del E2E (`main button, main select`,
  spec:181) no aplica una excepción a un enlace concreto: **excluye todos los enlaces en bloque**,
  cuando @s42 dice «todos los controles». La razón invocada es real; su aplicación aquí no está
  establecida.

### 7. Cobertura honesta, escenario a escenario — sección propia, con los huérfanos nombrados.

### 8. Unificación de `SecretCipher` con la feature 28 — respuesta concreta al coordinador

No lo juzgo como defecto de la 27. Lo que la unificación se llevaría por delante, medido:

- **`String decrypt(String, byte[])` tiene EXACTAMENTE UN punto de llamada en producción**:
  `application/ImportGithubIssues.java:126` —
  `var token = cipher.decrypt(ownerId, connection.tokenCiphertext());`. Pasar a
  `Optional<String> decrypt` es barato: esa única línea debe decidir qué hacer con el vacío.
  **Cuidado con la semántica**: hoy un texto indescifrable lanza `SecretUndecipherableException`, que
  **nadie mapea en la frontera HTTP** (comprobado: no aparece en `adapter/http/`), así que sale como
  500. Con un `Optional` vacío ese fallo pasaría a ser silencioso salvo que la línea 126 lo convierta
  explícitamente. **Es el punto donde la unificación puede empeorar las cosas.**
- **`boolean enabled()` tiene CINCO puntos de llamada en producción**, y ahí sí duele si desaparece:
  `ConnectGithub:39`, `DisconnectGithub:19`, `ImportGithubIssues:64`, `ReadGithubConnection:21`,
  `ReadIssueImport:21` — los cinco `if (!cipher.enabled()) throw new ConnectorsDisabledException();`,
  que es como se cumple @s3 (503 `CONNECTORS_DISABLED` en las cinco rutas). Si la firma de la 28 no
  ofrece un equivalente, **@s3 se cae entero**. Lo afirman además `GithubConnectorWiringTest.s3_...`
  (tres pruebas) y `AesGcmSecretCipher:32-34,38,57`.
- En pruebas, `SecretUndecipherableException` se afirma en `AesGcmSecretCipherTest` (4 sitios),
  `ConnectGithubTest:63` (sobre el doble, no sobre producción) y `ConnectorFakes:120`.

**Resumen para la 28: migrar `decrypt` es trivial (1 llamada) pero cambia la semántica del fallo;
retirar o renombrar `enabled()` rompe 5 llamadas y el escenario @s3 completo.**

---

## El bloqueante: @s42

`e2e/github-connector.spec.mjs` (12 pruebas) es su único oráculo de comportamiento, más
`github-connector-routing.test.tsx` para la parte estructural. Cláusula por cláusula:

| Cláusula de @s42 | Estado |
| --- | --- |
| «en los estados deshabilitado, sin conexión, conectada, importando, resultado, error recuperable e inválida» (siete) | **TRES auditados con axe**: `sin conexión` (spec:56), `conectada` (:72), `inválida` (:88). **Faltan `deshabilitado`, `importando`, `resultado` y `error recuperable`** |
| «recorro cada estado … con axe» | **[ ] no**: axe corre en 3 de 7, y ningún estado se audita a los tres anchos |
| «en los anchos 320, 768 y 1440 px CSS» | [x] spec:148-167, pero **sólo en el estado `conectada`** |
| «**con zoom nativo 200 por ciento**» | **[ ] nunca se ejecuta.** No hay `deviceScaleFactor`, ni emulación de zoom, ni una segunda pasada |
| «todos los controles se alcanzan y activan con **Tab**» | [~] spec:103-128 recorre 40 tabuladores y exige que aparezcan «Importar», «Desconectar» y un `SELECT`. Mejor que un `Tab` suelto, pero **no comprueba orden** ni que sean *todos* los controles |
| «… y activan con **Enter**» | [~] sólo un `Enter` sobre «Desconectar» (spec:137) |
| «… y activan con **Escape**» | **[ ] no existe, ni en la prueba ni en el producto.** La prueba `:130` se llama «can be cancelled with **Escape**» y **nunca pulsa Escape**: abre con Enter y luego hace `.click()` sobre «Cancelar» (`:140`). Y `Escape` no aparece ni una vez en `frontend/src/github-connector.tsx`: **el componente no implementa Escape**. El título de la prueba afirma algo que el producto no hace |
| «con **foco visible**» | **[ ] sin oráculo**: nada mide el indicador de foco |
| «y área mínima de 44 por 44 px» | [~] spec:169-191 mide de verdad, pero **sólo en el estado `conectada`**, sólo `button, select` —excluye enlaces (punto 6) y los campos de repositorio y token—, y contra una regla CSS que ya garantiza el mínimo |
| «axe no reporta ninguna violación» | [x] para los tres estados auditados |
| «y cada error se anuncia por aria-live o por el foco» | [~] cubierto en Vitest (`role="status"` con `aria-live="polite"`, `github-connector.test.tsx:297`, y el foco al campo token en `:237`), **no en el recorrido de @s42** |
| «a 320 px con zoom 200 por ciento no hay desplazamiento horizontal» | [~] el desbordamiento sí (`scrollWidth > clientWidth`, spec:159-165); **con zoom 200 %, no** |
| «ni contenido recortado» | **[ ] sin oráculo** |
| «"/integraciones" enlaza "Conector de GitHub"» | [x] spec:193 y `routing.test.tsx:41` |
| «y el menú no gana ninguna entrada nueva» | [x] verificado, punto 4 |
| «la revisión registra evidencia de las **30 filas** de `docs/ux-requirements.md` sin atribuir estudio humano a axe» | **[ ] el artefacto no existe**: no hay `progress/ux_github_connector.md` |

Y una pieza que no debería estar ahí: **spec:231-233 es una prueba placebo**.

    test("@s42 the seven states of the contract are the ones this suite walks", () => {
      expect(states).toHaveLength(7);
    });

`states` es un array literal de siete cadenas declarado en la línea 18 del mismo fichero. La prueba
afirma que un literal de siete elementos tiene siete elementos. **No puede fallar nunca, no toca el
producto y su nombre afirma que la suite recorre los siete estados, que es justo lo que la suite no
hace.** Es una de las doce del «12 de 12 en verde». Eso es maquillaje, y en un carril por lo demás
honesto desentona.

**Por qué esto no es formalismo, y por qué el rasero es el mismo:**

1. **Precedente unánime**: diecisiete features tienen su `progress/ux_<name>.md`
   (`ux_export_data.md`, `ux_integration_api.md`, …). Ninguna excepción hasta ahora.
2. **`AGENTS.md:51` lo prohíbe literalmente**: «No declarar cumplimiento global por pasar únicamente
   axe o unas pocas resoluciones». Es exactamente lo que hay: tres estados con axe.
3. **`docs/ux-requirements.md` («Regla de revisión») es explícito**: «Toda feature con interfaz
   revisará las 30 filas … Ninguna fila se omite … una suite automática no demuestra por sí sola
   facilidad de uso».
4. **`project-spec.md:2128` lo pide para ESTA feature, por su nombre**: «controles de 44 por 44,
   320/768/1440 px, **zoom 200 %** y **matriz UX de 30 filas con evidencia**».
5. **La comprobación que falta es justo la que encuentra defectos aquí.** `progress/current.md`
   registra que el único fallo de CI de la feature 24 fue «@s41 texto 200 % a 320 px, marca de
   345 px», y el repositorio tiene specs dedicadas (`e2e/reschedule-native-zoom.spec.mjs`). Esta
   pantalla tiene el control de riesgo típico: un `select` de nombres de proyecto de ancho libre y un
   `dl` en rejilla de dos columnas, a 320 px, al 200 %.
6. **Rechacé la 26 por menos matices que éstos.** Aprobar aquí sería cambiar la vara entre features.

### Causa raíz: el E2E no tiene servidor falso de GitHub

No es descuido, es estructural, y por eso lo señalo aparte. `scripts/e2e.mjs` apunta la base de la
API a `http://127.0.0.1:9`, un puerto de descarte. Eso resuelve muy bien «ninguna prueba habla con
api.github.com», pero significa que **conectar siempre falla y no se puede importar nunca**: los
estados `importando` y `resultado` son inalcanzables desde el E2E, y el estado `conectada` se fabrica
con un `INSERT` directo cuyo `token_ciphertext` son 43 bytes de ceros (spec:29-34) que jamás
descifrarán. `project-spec.md:2128` pedía otra cosa: «E2E Playwright con un **servicio falso de
GitHub en la pila de compose de e2e** (`APP_GITHUB_API_BASE` apuntando a él): conectar, importar,
reimportar con `skipped`, desconectar y estado deshabilitado, con axe y barrido responsive».
**Ninguno de esos cinco recorridos existe hoy.** Sin ese servicio en la pila, los cuatro estados que
faltan no se pueden auditar; con él, salen casi solos.

---

## Cobertura de escenarios (@s ↔ test)

Verificada leyendo las pruebas, no fiándome de la tabla del artesano. Ejecutadas todas las clases
citadas salvo donde se indique.

- @s1 [x] `ConnectGithubTest.s1_...` (×4, incluida la ida y vuelta con AAD), `GithubConnectorApiTest`, `HttpGithubIssueSourceTest` (las cuatro cabeceras exactas), `GithubConnectorPersistenceTest`, `GithubConnectorWiringTest.s1_...` (tamaño 1+12+n+16)
- @s2 [x] las tres filas en `AesGcmSecretCipherTest`
- @s3 [x] `GithubConnectorWiringTest.s3_...` (×3), más el `enabled()` de los cinco casos de uso y las cinco rutas en `GithubConnectorApiTest`
- @s4 [~] **tres de las cuatro filas.** `s4_aKeyThatIsPresentButMalformed...` cubre 16 bytes, 33 bytes y no-base64, con `doesNotContain(value)`. **La cuarta fila, «cadena vacía», está deliberadamente contradicha**: el contrato pide que impida arrancar y `s3_anEmptyValueCountsAsAbsent...` (línea 34) afirma lo contrario. Ver «Cambios requeridos» 4
- @s5 [x] `GithubRepositoryTest` (12: recorte U+00A0, guion inicial/final, 40 letras, 101 letras, espacio interior, URL completa, `REQUIRED`, `INVALID_TYPE`), `GithubConnectorApiTest`
- @s6 [x] `PersonalAccessTokenTest` (8) y la frontera HTTP para `UNKNOWN_FIELD`, query string, JSON truncado y 415
- @s7 [x] `ConnectGithubTest`, `HttpGithubIssueSourceTest`, `GithubConnectorApiTest`
- @s8 [x] las cuatro filas repartidas entre `ConnectGithubTest`, `ImportGithubIssuesTest` y `HttpGithubIssueSourceTest` (403 sin cabeceras de cuota ⇒ repositorio no disponible)
- @s9 [x] `ConnectGithubTest`, `GithubConnectorPersistenceTest`
- @s10 [x] las cuatro filas en `GithubConnectorQueriesTest` + `GithubConnectorApiTest` (conjunto exacto de claves, incluidas las nulas)
- @s11 [x] `GithubConnectorQueriesTest` (idempotencia), `GithubConnectorPersistenceTest`, `GithubConnectorApiTest` (204 sin cuerpo)
- @s12 [x] `ImportGithubIssuesTest`, `GithubConnectorPersistenceTest` (tarea + evento + enlace), `HttpGithubIssueSourceTest` (descarte de `pull_request`)
- @s13 [x] `HttpGithubIssueSourceTest`: consulta exacta y `followRedirects(NEVER)` con la ruta del `Location` sin visitar
- @s14 [x] las siete filas en `ExternalIssueTest` (8) e `ImportGithubIssuesTest`, incluido el par sustituto del emoji
- @s15 [x] las cinco filas en `ExternalIssueCriterionTest` (8)
- @s16 [x] **las seis filas** por `@CsvSource` (`ImportGithubIssuesTest:129-137`) más `s16_theSecondPageIsAskedOnlyBecauseTheFirstCameFull...`, que es la cláusula fina del escenario
- @s17 [x] las cuatro filas en `ImportGithubIssuesTest`; la unicidad por clave primaria del enlace en `GithubConnectorPersistenceTest`
- @s18 [x] `ImportGithubIssuesTest`, con la aritmética created + skipped + failed == 5
- @s19 [x] `ImportGithubIssuesTest`, `GithubConnectorPersistenceTest` (fallo inyectado en el enlace), `GithubConnectorApiTest`
- @s20 [x] **las seis filas**, una prueba por fila en `HttpGithubIssueSourceTest:183-251`, más el 503 con `Retry-After` en la frontera HTTP
- @s21 [x] `ConnectGithubTest`, `GithubConnectorApiTest`
- @s22 [x] `ImportGithubIssuesTest` (401 en page=2 tras 100 issues, conexión a `invalid`, reconexión que vuelve a permitir)
- @s23 [x] las seis filas en `GithubConnectorApiTest` e `ImportGithubIssuesTest`, con el cuerpo idéntico para inexistente y ajeno
- @s24 [x] las dos filas, incluida la que pasa a `completed` a mitad
- @s25 [x] **concurrencia real**: `GithubConnectorPersistenceTest` con ocho hilos y `CyclicBarrier` sobre PostgreSQL; exactamente uno empieza. Buen trabajo
- @s26 [x] las dos filas (14 min 59 s y 15 min 1 s) con reloj inyectado
- @s27 [x] `ImportGithubIssuesTest` (progreso incremental) y `GithubConnectorPersistenceTest`
- @s28 [x] las seis filas en `HttpGithubIssueSourceTest`, con los plazos medidos con reloj de pared (2 s conexión, 4 s petición)
- @s29 [x] las seis filas del orden fijo de precondiciones
- @s30 [x] las cinco filas en `GithubConnectorQueriesTest` y `GithubConnectorApiTest`
- @s31 [x] **las siete filas**, con el código del problema y no sólo el estado. Ver punto 2
- @s32 [~] **tres de las cinco filas.** `s32_writingWithoutACsrfTokenIsForbidden` cubre «CSRF ausente» en PUT, DELETE y POST. **Sin oráculo en esta suite: las dos filas «Origin de otro sitio»** (existe `OriginGuard`, probado para otras rutas, no para las del conector) **y la fila «token CSRF inválido»**, que es un camino distinto de «ausente»
- @s33 [x] `GithubConnectorQueriesTest`, `GithubConnectorPersistenceTest` (dos propietarios con el mismo `external_id` 101)
- @s34 [~] cinco de las seis filas con oráculo real (respuestas, problemas, logs, exportación, y el almacenamiento del navegador en Vitest + E2E). **Sin oráculo: la fila «la respuesta de auditoría de sesión»** — nadie consulta esa ruta para comprobar que el token no está. Se cumple por composición (el token no entra nunca en ese camino), pero es composición, no oráculo
- @s35 [x] las cuatro filas: `GithubApiBaseTest` (22), `GithubConnectorWiringTest.s35_...` (incluido el fallo con la base en http), `GithubConnectorApiTest` (`apiBase` ⇒ `UNKNOWN_FIELD`)
- @s36 [x] los seis estados del Examples en `github-connector.test.tsx:109-211`
- @s37 [x] `:213-271`: `type="password"` y `autocomplete="off"` antes y después, vaciado en ambos casos, repositorio conservado sólo en el 409, `aria-describedby` y foco al campo
- @s38 [x] `:272-378`: selector filtrado, botón deshabilitado, `role="status"` con `aria-live="polite"` sin porcentaje, resumen medible y foco al h1
- @s39 [x] **las cinco filas**, `:380-455`, contando las llamadas para probar que nada se reintenta solo
- @s40 [x] `:456-488`, cancelar sin DELETE y confirmar con uno solo
- @s41 [x] las cuatro filas, `:489-540`, con `key={owner}` y `AbortController`
- @s42 [ ] **SIN COBERTURA SUFICIENTE. BLOQUEANTE.** Ver la sección propia

**Ninguno de los 42 se queda sin ninguna prueba. Los que no tienen oráculo completo son, por su
nombre: @s42 (bloqueante), @s4 fila «cadena vacía» (contradicha), @s32 filas «Origin» ×2 y «CSRF
inválido», y @s34 fila «respuesta de auditoría de sesión».**

---

## Disciplina TDD

- **¿Producción sin test que la pida? Prácticamente no.** He buscado alcance inflado y he encontrado
  poco: `SecretUndecipherableException` no está mapeada en la frontera HTTP, así que su camino (texto
  cifrado ilegible tras rotar dos veces) sale como 500 sin que ninguna prueba lo fije —es un hueco de
  contrato, no producción sobrante—. `ConnectorKeyRing.candidatesFor` ordena por versión cuando la
  etiqueta ya decide: es una optimización que ninguna prueba exige por rendimiento, pero
  `b5_aRotatedKeyStillDecipherWhatThePreviousOneSealed` sí exige el comportamiento. Nada más.
- **¿Evidencia de Rojo→Verde→Refactor? SÍ**, y de calidad alta: veinte ciclos, con el rojo nombrado
  antes del verde y **cuatro rojos que corrigieron la prueba y no el código**, admitidos como tales
  (el fixture de @s23 con un UUID «en mayúsculas» idéntico a sí mismo; el `ObjectMapper` pelado
  incapaz de serializar `Instant`; las cabeceras normalizadas a `Headers`; la ruta en minúsculas).
  Reconocer que la prueba estaba mal vale tanto como arreglar el código.
- **Dos comprobaciones de mordida hechas por iniciativa propia** (ciclo 8: subir `MAX_PAGES` a 3
  rompe 9 de 34; ciclo 14: cuatro mutaciones manuales rompen cuatro pruebas distintas). Es lo que se
  le pide a un artesano antes de llamar al `mutation_tester`.
- **Honestidad, con una mancha.** La bitácora es en general escrupulosa: avisa de los diez
  `stryker.*.config.json` desincronizados que no ha tocado y de dos fallos preexistentes ajenos a su
  carril. Contra eso: la tabla de trazabilidad da @s42 por cerrado tras el ciclo 18 («12 de 12 en
  verde, incluidos axe en **tres** estados» — lo dice él mismo, y el contrato pide siete), y la
  prueba placebo de spec:231 existe para que la suite *parezca* recorrer los siete. Ahí la vara se
  aflojó.

## Calidad (lente de artesano)

Lo bueno, que es mucho:

- **`ConnectorAudit`**: hacer imposible por el tipo lo que normalmente se pide por disciplina, y
  ponerle una prueba por reflexión para que siga siendo imposible. La mejor decisión del carril.
- **`GithubApiBase`**: lista blanca, no lista negra; validada al arrancar, no al usar; con la razón
  («el PAT del usuario viaja hacia esta base») escrita en el javadoc. Un comentario que explica una
  fuerza, no lo que hace el código.
- **Invariantes en el esquema, no en el código** (`V25__github_connector.sql`): índice único parcial
  para `running`, clave primaria del enlace, `CHECK ((status='running') = (finished_at IS NULL))` y
  el `octet_length` del texto cifrado. Y las pruebas los comprueban **rompiéndolos**.
- **`ConnectorFailures`** concentra la traducción de `IssueSourceException`, que difiere entre
  conectar e importar: un solo motivo para cambiar.
- **`PostgresImportedTaskCommit` repropaga** `ResourceNotFoundException`, `ProjectCompletedException`
  y `ValidationException` en vez de disfrazarlas de fallo del almacén. El recibo no miente sobre la
  causa. Sutil y correcto.
- **Renombrado por colisión real** (`ImportReceipt` → `IssueImportReceipt` y familia) sin tocar
  ninguna clase existente.
- **Arquitectura respetada**: `ArchitectureTest` verde; dominio y aplicación sin dependencias nuevas;
  puertos en `application`, adaptadores en `adapter`.
- **`HttpGithubIssueSource`**: 176 líneas, funciones cortas, sin números mágicos (`CONNECT_TIMEOUT`,
  `DEFAULT_RETRY_SECONDS`, `MINIMUM_RETRY_SECONDS`), y la clasificación 401/403/404/429 con el porqué
  del 403 ambiguo explicado donde se decide.

Fragilidades menores, ninguna bloqueante por sí sola:

1. `e2e/github-connector.spec.mjs:231-233` — la prueba placebo. Debe desaparecer.
2. `e2e/github-connector.spec.mjs:130` — el título dice «Escape» y la prueba hace `click()`.
3. `frontend/src/github-connector.tsx:264` — `<main id="proyectos">` en la pantalla del conector.
   Sigue la convención del destino del salto de navegación, pero el nombre miente sobre el contenido.
   Mismo hallazgo que en la feature 26; es deuda del proyecto, no de este carril.
4. `SecretUndecipherableException` sin mapeo en `adapter/http/` ⇒ 500 genérico (no filtra el token).
5. `e2e/github-connector.spec.mjs:29-34` — el `INSERT` interpola propietario y estado en SQL por
   concatenación. Son constantes del propio fichero, así que hoy es inocuo, pero es el patrón que no
   queremos que nadie copie a un sitio donde el valor venga de fuera.

## Checkpoints

- **C1** [~] Ficheros base y documentación presentes. **`bin/harness init` no ejecutado** (es la
  suite completa, prohibida en esta sesión): la casilla del arnés queda sin marcar.
- **C2** [ ] `feature_list.json` tiene **cinco** features en `in_progress` (25, 26, 27, 28, 30). La
  27 figura correctamente como `in_progress`, así que por esta feature el estado es coherente; el
  incumplimiento de «una sola feature a la vez» es la decisión de carriles paralelos del coordinador,
  no un defecto de la 27.
- **C3** [x] Capas respetadas, sin logs de depuración ni TODOs sueltos. `ArchitectureTest` verde
  según la bitácora del ciclo 12.
- **C4** [~] Todo módulo nuevo tiene prueba y el aislamiento es real (PostgreSQL con Testcontainers,
  no dobles de base de datos). `bin/harness test` completo, no ejecutado.
- **C5** [ ] `progress/history.md` no tiene todavía entrada de la sesión de este carril.
- **C6** [ ] **@s42 sin cobertura suficiente**; @s4 con una fila contradicha y @s32/@s34 con filas sin
  oráculo. El resto del mapa @s → prueba es correcto y verificado uno a uno.
- **C7** [ ] Mutación pendiente: es la puerta del `mutation_tester`. Los alcances están declarados
  (`build.gradle.kts`, `stryker.github-connector.config.json`, objetivos en `scripts/project.mjs`) y
  guardados contra desplazamiento de líneas, que es buena preparación.

---

## Cambios requeridos

1. **Cerrar @s42 de verdad** (BLOQUEANTE). En `e2e/github-connector.spec.mjs`, al estilo de
   `e2e/reschedule-native-zoom.spec.mjs`:
   1. Auditar con axe **los siete** estados que nombra el escenario, no tres: añadir `deshabilitado`
      (pila sin clave de conectores), `importando`, `resultado` y `error recuperable`.
   2. Ejecutar el barrido de 320/768/1440 **con zoom nativo al 200 %**, y afirmar a 320 px al 200 %
      que no hay desplazamiento horizontal **ni contenido recortado**. Es la comprobación que ya cazó
      un defecto real en la feature 24.
   3. **Implementar Escape** en el grupo de confirmación de desconexión (hoy no existe) y pulsarlo de
      verdad en la prueba, en lugar de hacer `click()` sobre «Cancelar» bajo un título que dice
      «Escape».
   4. Añadir oráculo de **foco visible** y de **orden** en el recorrido con Tab.
   5. Medir el área mínima **en todos los estados con controles**, no sólo en `conectada`, e incluir
      los campos de texto. Si se excluye algún enlace por la excepción *Inline* de WCAG 2.2 §2.5.8,
      que sea enlace a enlace y sólo cuando esté **dentro de una frase**: los dos de esta pantalla
      (`github-connector.tsx:268-270` y `:429-433`) son el único contenido de su bloque y **no**
      cumplen la excepción tal y como está redactada.
   6. **Borrar la prueba placebo** de spec:231-233 (`expect(states).toHaveLength(7)`).
2. **Levantar el servicio falso de GitHub en la pila de compose de E2E** (BLOQUEANTE, y es la causa
   raíz del punto 1). `project-spec.md:2128` lo pide por su nombre, con `APP_GITHUB_API_BASE`
   apuntando a él, y con los recorridos conectar, importar, reimportar con `skipped`, desconectar y
   estado deshabilitado. Sin él, cuatro de los siete estados son inalcanzables y el estado `conectada`
   seguirá fabricándose con un `INSERT` de 43 bytes de ceros. Mantener la garantía actual: nada sale
   hacia api.github.com.
3. **Escribir `progress/ux_github_connector.md`** (BLOQUEANTE) con la matriz de las 30 filas de
   `docs/ux-requirements.md`, su evidencia y sus **límites explícitos** (qué es automatizado, qué es
   heurístico, qué no se ha probado con personas), al modo de `progress/ux_export_data.md`. Sin
   inferir cumplimiento universal desde axe (`AGENTS.md:51`).
4. **Resolver la fila «cadena vacía» de @s4** (no bloqueante, pero hay que decidirlo, no dejarlo). El
   contrato pide que impida arrancar; `ConnectorConfiguration.configured` (98-100) y
   `GithubConnectorWiringTest:34` afirman lo contrario a propósito. **La conducta implementada me
   parece la correcta** —una variable de entorno sin definir llega como cadena vacía y un despliegue
   que no usa conectores no debe dejar de arrancar—, pero eso es una **enmienda del `.feature`**, y el
   `.feature` es contrato aprobado: la lleva el coordinador a la puerta humana, como se hizo con @s31.
   Hoy la bitácora no la registra como desvío y su tabla da @s4 por cubierto: eso sí es un error de
   trazabilidad.
5. **Cerrar las filas sin oráculo** (no bloqueante):
   - @s32: las dos filas «Origin de otro sitio» y la fila «token CSRF **inválido**» sobre las rutas
     del conector.
   - @s34: la fila «la respuesta de auditoría de sesión».
6. **Corregir la bitácora** `progress/tdd_github_connector.md`: dice «68 de frontend» y son **53**
   (medido por mí sobre los tres ficheros del carril); y da @s42 por cerrado cuando el propio ciclo 18
   admite «axe en tres estados» frente a los siete del contrato. Las 259 del backend sí son exactas y
   lo dejo dicho.
7. **Menor**: mapear `SecretUndecipherableException` en la frontera HTTP a un problema explícito en
   vez de dejarla salir como 500.

## Para el coordinador (fuera del alcance de este carril)

- **Unificación de `SecretCipher` con la feature 28**: `decrypt` tiene **un** punto de llamada
  (`ImportGithubIssues.java:126`) y migrarlo a `Optional<String>` es barato, **pero** hoy un texto
  ilegible lanza `SecretUndecipherableException` y con un `Optional` vacío ese fallo se volvería
  silencioso si la línea 126 no lo traduce. `enabled()` tiene **cinco** puntos de llamada
  (`ConnectGithub:39`, `DisconnectGithub:19`, `ImportGithubIssues:64`, `ReadGithubConnection:21`,
  `ReadIssueImport:21`): si la firma de la 28 no ofrece equivalente, **@s3 se cae entero**. Detalle
  completo en el punto 8.
- **`feature_list.json`** tiene cinco features en `in_progress` a la vez.
- **`bin/harness init`** sigue sin ejecutarse en este veredicto por la restricción de recursos: es
  puerta pendiente para el cierre de sesión.
- **`e2e/github-connector.spec.mjs:231`** es el segundo caso que veo de prueba que no puede fallar.
  Si aparece un tercero, merece una regla en `docs/tdd.md`.
