# Review — feature 29 `additional_connectors`

**Veredicto:** APPROVED **CONDICIONADO** a las siete enmiendas de la sección
«Condiciones» y a la campaña de mutación con los ámbitos corregidos.

Revisión de solo lectura, sin ejecutar suite ni mutación (había 22 contenedores
y otro juez trabajando). Todo lo que sigue sale de leer el código, no de correrlo:
donde afirmo «no hay oráculo» es porque he buscado y no existe el fichero, no
porque haya fallado. Donde digo «pasa» me apoyo en la bitácora, no en evidencia
propia.

---

## 1. Cobertura del contrato (38 escenarios)

Recuento del juez: **35 cerrados, 3 parciales**. Coincide en el total con el
inventario del artesano, **no en la membresía**: él da `@s25` y `@s33` por
cerrados y anota `@s2`/`@s31` como dudosos. Es al revés.

### Cerrados con oráculo que puede fallar (35)

`@s1` `@s2` `@s3` `@s4` `@s5` `@s6` `@s7` `@s8`–`@s24` `@s26`–`@s32`
`@s34` `@s35` `@s36` `@s37`.

Comprobaciones puntuales hechas a mano, porque el mandato era refutar:

- **`@s16`** — `ImportGitlabIssuesTest:387-404`. Paramétrica de cuatro filas que
  afirma peticiones, `created`, `truncated` **y** enlaces. No es decorativa:
  ninguna mutación única mata las cuatro filas, como el propio artesano predijo.
- **`@s4`** — `ConnectorStatusSourcesTest:364-375` afirma sobre **a quién se
  preguntó** (`asked`), no sólo sobre lo que se contestó. Esa es la forma
  correcta; un `assertEquals("not_connected", ...)` no habría discriminado.
- **`@s3`** — `ConnectorCatalogWiringTest:66-77` construye el catálogo con la
  lectura **real** de GitLab y afirma `verifyNoInteractions(gitlabConnections)`.
  Compara la lista **entera** (`containsExactlyElementsOf(ORDER)`), no posiciones.
- **`@s32`** — `gitlab-connector.test.tsx:241` afirma que el campo **sí** contiene
  el token antes de afirmar que ningún `outerHTML` lo contiene. Sin esa primera
  línea la prueba sería verde con el campo vacío. Es exactamente la construcción
  que hace no-vacua una prueba de ausencia.
- **`@s5`** — el oráculo que muerde es `ConnectorStatusSourcesTest:378-395` (la
  vista lleva `grupo/proyecto`, `gitlab.example.com` y `WXYZ`; la fila resultante
  no) y `HttpGitlabIssueSourceTest:93-99` (cuerpo real
  `"invalid_token: glpat-abcdef1234"`, se afirma que no sale). `@s5` queda
  cerrado — pero ver el hallazgo H1.

### Parciales (3)

- **`@s25` — parcial, no cerrado.** La fila «200 con cuerpo JSON de más de
  5 MiB» **no tiene prueba ni mecanismo**. `HttpGitlabIssueSourceTest` cubre tres
  de las cinco filas (`:211` redirección, `:223` sin respuesta, `:232` cuerpo que
  no es lista) y `ImportGitlabIssuesTest:228` cubre el caso genérico. No hay
  ninguna prueba con un cuerpo grande, y no puede haberla: en
  `HttpGitlabIssueSource.java:128` el cuerpo se lee con
  `HttpResponse.BodyHandlers.ofString()`, **sin techo**. Buscar "MiB" o "maxBody"
  en `adapter/connectors/` no devuelve nada. El escenario no está cubierto: está
  sin implementar. Ver también S1.
- **`@s33` — parcial.** Dos de sus tres mitades sí están:
  - la posición en el menú, por `e2e/export-data.spec.mjs:61-80` y
    `e2e/import-data.spec.mjs`, que afirman **la lista canónica entera de catorce
    entradas por nombre y por índice, más el recuento**. Es un oráculo excelente
    y sí incluye «Conectores» detrás de «Importación» y «Hoy» en primera.
  - la pantalla, por `connectors-catalog.test.tsx` (13 pruebas: un solo GET
    `:53`, orden `:64`, foco al h1 `:75`, texto y marcador con el mismo nombre
    accesible `:86` y `:120`, rutas `:188` y `:199`, «No disponible» `:205`).
  - **lo que falta**: «y llega a /conectores». Ver condición C1.
- **`@s38` — parcial**, igual que lo declaró el artesano, y **sigue igual después
  del cableado**. Sólo su tercera línea tiene oráculo
  (`gitlab-connector.test.tsx:715,731,743`). En `e2e/` sólo existen los dos
  ficheros de GitHub: **no hay ningún spec de navegador para `/conectores` ni
  `/conectores/gitlab`**. Anchos 320/768/1280, texto al 200 %, zoom nativo,
  44 x 44, recorrido de teclado y axe: cero evidencia. Y la última línea del
  escenario —«la evidencia registra los límites humanos y de dispositivos sin
  inferir cumplimiento universal desde axe»— no tiene ni fichero donde
  registrarse.

  El artesano tenía razón al no escribir una spec que no podía correr. Pero el
  cableado del 10 de septiembre **quitó ese motivo**: hoy es medible y sigue sin
  medirse. Eso ya no es prudencia, es deuda.

### Hallazgos sobre la calidad de los oráculos

- **H1 — un oráculo que no puede fallar.** `ConnectorCatalogApiTest:130-150`
  (`s5_thewholeBodyCarriesNoSecretNoUrlAndNoProjectPath`). El doble devuelve un
  `ConnectorCatalog` de seis `ConnectorRow` cuyos únicos datos son `"gitlab"`,
  `"CONNECTION_INVALID"` y un `Instant`. Las cinco aserciones dicen que el cuerpo
  no contiene `glpat`, `invalid_token`, `WXYZ`, `grupo/proyecto` ni `http`.
  **Ninguna de esas cadenas entra jamás en el caso de prueba**: `ConnectorRow`
  sólo tiene `(id, status, lastActivityAt, lastError)` y no hay hueco donde
  meterlas. El controlador podría serializar todo lo que recibe y la prueba
  seguiría verde. Es decorativa. La sustancia de `@s5` la sostienen las otras dos
  pruebas citadas arriba, así que el escenario no se cae — pero el inventario
  acredita tres oráculos donde hay dos.

  Para que muerda hay que meter en la fila algo que el contrato prohíba publicar
  (por ejemplo un `ConnectorError` cuyo `code` fuera el texto del proveedor) y
  afirmar que no sale.

- **H2 — aserción incompleta.** `ConnectorCatalogApiTest:216-220`
  (`s31_withoutASessionTheCatalogAnswersNothing`) afirma `isUnauthorized()` y
  `verifyNoInteractions`, pero **no afirma el código `UNAUTHENTICATED`**, que es
  lo que la fila del `@s31` fija. La hermana de Bearer (`:223-232`) sí afirma
  `$.code`. Asimetría gratuita.

- **H3 — bien resuelto, y conste.** El artesano documentó tres casos en que una
  prueba pasó a la primera y **no la dio por buena**: `@s26` (bitácora 182-208,
  la primera rotura no discriminaba y quedó anotada para que nadie la repita),
  `@s37` (ciclo 7: cuatro pruebas verdes **por construcción**, porque afirmaban
  sobre un árbol desmontado; rehechas sobre el `AbortSignal`) y la regex del
  heredoc que «pasaba por suerte» (ciclo 8). Esa honestidad es la mitad del valor
  de esta bitácora y hay que decirlo tan alto como los defectos.

---

## 2. La prueba de contrato `gitlab-connector-client.test.ts:265-305`

**Es sólida en lo esencial y está mal apuntada. Se queda; hay que extenderla.**

Lo que hace bien, y no es poco:

- **Muerde de verdad.** `RECEIPT_KEYS` (`gitlab-connector-client.ts:9`) no es una
  constante de adorno para la prueba: es la misma que `decodeReceipt` pasa a
  `exact()` en `:153`, y `exact` (`schedule-block-api.ts:341-350`) compara
  cardinalidad **y** presencia de cada clave. Renombrar un componente del `record`
  rompe la prueba y rompería la producción. La atadura es real.
- **Falla ruidosamente cuando no encuentra el fichero** (`:292`, `throw`), en vez
  de saltarse en silencio. Es la trampa en la que caen nueve de cada diez pruebas
  que leen código fuente, y aquí está evitada.
- **Rojo acreditado con la regresión real** (bitácora, ciclo 5): renombrar
  `source` a `repository` en el `record` la tumba.

Por qué es frágil **de otra manera**:

- **F1 — vigila el conector que no se rompió.** La regresión de `a347936` fue
  `github-connector-client.ts` contra `GithubConnectorController`. No existe la
  prueba equivalente para GitHub: ningún test de `frontend/src` menciona
  `GithubConnectorController.java`. El fichero que reventó sigue sin guarda.
  **Esta es la crítica principal**: la lección se aprendió en el carril
  equivocado.
- **F2 — cubre dos de los tres `record` que el cliente decodifica.**
  `GitlabConnectorController.java:298` publica
  `record ErrorResponse(String code, Instant at)` y el cliente lo valida contra
  `ERROR_FIELDS = "code at"` (`gitlab-connector-client.ts:11`, usado en `:112`).
  Ese par **no se compara**. Renombrar `code` por `errorCode` en `ErrorResponse`
  pasa la prueba de contrato entera y rompe `decodeFailure` en ejecución: la misma
  clase de fallo, un nivel más abajo.
- **F3 — sobre-especifica el orden.** Compara con `toEqual`, es decir en orden.
  Pero `exact()` es indiferente al orden y JSON también. Reordenar componentes de
  un `record` es un no-op en producción y pone la prueba en rojo. Las alarmas
  falsas erosionan la guarda: la tercera vez que alguien la vea roja por un
  reordenamiento cosmético, la relajará. Que compare conjuntos, no listas.
- **F4 — la regex es optimista.** `record NOMBRE\(([^)]*)\)` (`:274`) se rompe con
  cualquier componente cuyo tipo o anotación lleve un paréntesis o una coma
  (`Map<String, Object>`, `@JsonFormat(...)`). Falla ruidosamente, sí, pero con un
  mensaje que no explica nada. Además coge **la primera** aparición del nombre: un
  `record` anidado homónimo en otro punto del fichero ganaría en silencio.
- **F5 — ata nombres, no serialización.** Prueba que el `record` tiene esos
  componentes; no prueba que el controlador devuelva ese `record` ni que Jackson
  lo serialice con esos nombres. Un `@JsonProperty("repo")` sobre `projectPath` la
  dejaría verde y rompería el cliente. Hoy no hay anotaciones, así que el riesgo
  es teórico — pero es el techo de lo que esta técnica puede prometer, y conviene
  que esté escrito antes de que alguien la crea infalible.

**Dictamen:** buena idea, ejecutada con cuidado (F5 aparte, que es intrínseco) y
aplicada al fichero equivocado. Vale su sitio.

---

## 3. La regresión de `a347936`, para que conste

**Qué pasó.** El commit unificó `ImportGithubIssues` en `ImportIssues` y el recibo
compartido pasó de once claves con `repository` a doce con `source` y
`projectPath`, como exige `@s20` («los dos recibos tienen las mismas claves»). El
cliente de GitHub validaba el juego exacto de once claves con `exact()`, que
compara cardinalidad: 12 distinto de 11, `decodeReceipt` lanzaba «Confirmación
incompatible» y la sección entera dejaba de pintarse. Y por partida doble, porque
`CONNECTION_FIELDS` incluye `lastImport`, que también pasa por `decodeReceipt`:
por eso cayeron cuatro E2E y no una.

**La decisión de fondo fue correcta.** `@s15` pide `source` y `projectPath`, y
`@s20` obliga a que los dos recibos tengan las mismas claves; con la línea 5 del
`.feature` delante, no había otra lectura. Cambiar el recibo de GitHub era lo
correcto. Lo que faltó fue la guarda, no el criterio.

**Estado hoy.** El fichero está arreglado: `github-connector-client.ts:11-12`
lleva las doce claves y `source` "github", con un comentario que cita
`additional_connectors.feature:242`. Coincide con
`GithubConnectorController.java:318-330`. Correcto **hoy**.

**¿Dejó la feature protección suficiente para que no se repita? No.** El arreglo
es manual y el único guardián automático mira al otro conector (F1). Si mañana
alguien toca `GithubConnectorController.ImportResponse` —y `@s20` garantiza que
los dos recibos se muevan juntos— vuelve a pasar lo mismo, en el mismo fichero,
por el mismo motivo. La protección que **sí** dejó, y hay que reconocerla: la
técnica está inventada, probada y con su rojo acreditado; replicarla cuesta veinte
minutos. Es una condición de cierre, no un rediseño.

---

## 4. Las cuatro dimensiones

### Cobertura — buena, con tres huecos nombrados
Ver seccion 1. 35/38 con oráculo que muerde; `@s25`, `@s33` y `@s38` parciales.
El backend está mucho mejor cubierto que el navegador, y dentro del navegador el
componente mucho mejor que la integración.

### Calidad de los oráculos — alta, con una excepción y una asimetría
H1 (oráculo vacuo en `ConnectorCatalogApiTest:130`) y H2 (código no afirmado en
`:216`). Contra eso, tres casos ejemplares de rojo acreditado por mutación con
conjuntos disjuntos (ciclo 1: cuatro y cuatro, exactamente las previstas) y tres
de pruebas verdes rechazadas por no discriminar (H3). El listón es alto.

Diseño, de paso, sin objeciones que bloqueen:

- `GitlabConnectorController` tiene 333 líneas pero cada método es corto y de un
  solo motivo; los `@ExceptionHandler` son un mapa de código a estado, no lógica.
- `ConnectGitlab.java:43` pone la guarda de clave antes que nada y `:53` construye
  la fila entera antes de `save`: cualquier fallo deja la conexión anterior
  intacta, que es lo que `@s12` exige.
- `HttpGitlabIssueSource` respeta las capas: `adapter/connectors` no conoce
  `application` salvo por los puertos. `docs/architecture.md` respetado.
- Números mágicos: los del adaptador (`CONNECT_TIMEOUT`, `REQUEST_TIMEOUT`,
  `DEFAULT_RETRY_SECONDS`, `MINIMUM_RETRY_SECONDS`) tienen nombre y comentario.

### Accesibilidad — excelente en componente, **inexistente en navegador**
A favor: `type="password"` con `autocomplete` y etiqueta asociada
(`gitlab-connector.test.tsx:65`), ayuda visible y recomendación de `read_api`
(`:76`), `aria-live` «Guardando...» con botón deshabilitado (`:97`), error junto
al campo señalado (`:179`), progreso sin porcentaje (`:344`), confirmación
explícita que promete que las tareas sobreviven (`:408`), foco al h1 o al aviso en
los tres cambios de estado (`:715`, `:731`, `:743`), y en el catálogo el estado
como texto **y** como glifo distinto por estado con el mismo texto accesible
(`connectors-catalog.test.tsx:86` y `:120` — el color nunca es la única señal).

En contra: **cero** evidencia de navegador. Ni axe, ni 44 x 44, ni tres anchos, ni
texto al 200 %, ni zoom nativo, ni recorrido de teclado. Es el hueco más grande de
la feature y ya no está bloqueado por nada.

### Seguridad — sólida, con un agujero real

**El PAT.** Cifrado con `SecretCipher` ligado al `ownerId` como dato asociado
(`ConnectGitlab.java:53`), tras exigir `cipher.enabled()` **antes de tocar nada**
(`:43`). Sólo se publica `hint()`, cuatro caracteres. Viaja únicamente en la
cabecera `PRIVATE-TOKEN` (`HttpGitlabIssueSource.java:122`), nunca en la URL ni en
el cuerpo, con oráculo en `gitlab-connector-client.test.ts:142`. **No se siguen
redirecciones** (`:48`, `Redirect.NEVER`), con el motivo escrito (`:25-26`) y
oráculo que cuenta las peticiones recibidas (`HttpGitlabIssueSourceTest:211-220`):
seguir una llevaría el PAT del usuario a donde apunte GitLab. El texto del
proveedor nunca sube: `classify` (`:153-160`) traduce a códigos estables y
`HttpGitlabIssueSourceTest:93-99` lo afirma con el cuerpo real
`"invalid_token: glpat-abcdef1234"`. Los logs se comprueban con el appender real,
no con un doble (`GitlabTokenConfinementTest`, y lo dice en su javadoc). `apiBase`
**no se persiste** (decisión 4 de la bitácora): sale de la configuración del
servidor en cada respuesta, así que no hay superficie SSRF por conexión. La ruta
del proyecto se valida en `domain/GitlabProjectPath` (`grupo/../otro` rechazado,
`@s10`) y viaja codificada. Nada que objetar: está bien pensado.

**El catálogo**, que lee el estado de las seis integraciones del propietario. Tres
propiedades importantes, las tres guardadas:

1. Sin clave **no se pregunta** a las fuentes que cifran, se devuelven `disabled`
   (`ConnectorCatalogWiringTest:66-77`, con `verifyNoInteractions`). Un descifrado
   fallido no puede tumbar el catálogo entero ni filtrar un mensaje de cifrado.
2. Cada fuente se pregunta **por el propietario autenticado y por nadie más**
   (`ConnectorStatusSourcesTest:364-375`, sobre la lista de a quién se preguntó).
   El controlador pasa `principal.getName()` y `ConnectorCatalogApiTest:190` lo
   verifica.
3. La fila de GitLab **no vuelve a derivar nada**: reutiliza
   `ReadGitlabConnectionUseCase` y se queda con tres de sus ocho campos, y los
   cinco que descarta son exactamente los que `@s5` prohíbe. Detalle de diseño
   excelente: el catálogo y la pantalla de detalle no pueden discrepar.

**S1 — el agujero: cuerpo de respuesta sin techo.**
`HttpGitlabIssueSource.java:128` lee el cuerpo con `BodyHandlers.ofString()` sin
límite de tamaño. `@s25` fija expresamente la fila «200 con cuerpo JSON de más de
5 MiB, 503 GITLAB_UNAVAILABLE» y no hay ni prueba ni mecanismo. No es teórico:
`app.gitlab.api-base` es configurable precisamente para instancias autoalojadas, y
`MAX_PAGES = 2` acota el número de respuestas pero no el tamaño de cada una. Un
GitLab hostil o comprometido, o un intermediario, agota la memoria del servidor
con dos respuestas. Comparte el defecto `HttpGithubIssueSource.java:101`, así que
es anterior a esta feature — pero es **esta** feature la que lo pone en su
contrato y lo declara cerrado.

**S2 — el validador de rutas no recibe mutantes.** `domain.GitlabProjectPath` es
la clase que rechaza `grupo/../otro`, los seis segmentos y los espacios, y **no
está en ningún ámbito PIT**: no aparece ni una vez en `backend/build.gradle.kts`.
El conjunto `additionalConnectorsClasses` (`:84-108`) no incluye **ninguna** clase
de `domain`. Ver sección 7.

---

## 5. Enmienda de contrato del `@s31` — **correcta, no se pasó de la raya**

Las dos filas de credencial Bearer válida pasan de 401 `UNAUTHENTICATED` a 403
`API_SCOPE_DENIED` (`features/additional_connectors.feature:388-401`, razonada en
`progress/tdd_additional_connectors.md:364-379`).

La apruebo, por cuatro motivos y con una condición:

1. **La delegación es del propio contrato, no del artesano.** La línea 5 de
   `features/additional_connectors.feature` dice, escrita antes de que existiera
   este conflicto, que «donde 27 fija algo distinto, 27 prevalece», y enumera
   «códigos y estados HTTP compartidos» entre lo que 27 gobierna. No es una
   interpretación forzada: es el supuesto exacto para el que se escribió la
   cláusula.
2. **El precedente está ratificado y es literal.**
   `features/github_connector.feature:392-402` lleva la misma enmienda del 9 de
   septiembre, marcada «ratificada por el propietario», con el mismo razonamiento
   palabra por palabra. Mantener 401 aquí produciría dos códigos distintos para la
   misma credencial según qué subrecurso de `/api/v1/me/connectors` pidiera. Eso
   sí sería un defecto.
3. **El razonamiento es verdadero, no cómodo.** Una credencial Bearer válida de 24
   está autenticada: el filtro la identifica y **después** mira su lista de rutas.
   401 significa «no sé quién eres» y sería falso. 403 con `API_SCOPE_DENIED` dice
   lo que pasa.
4. **La propiedad de seguridad no se toca, y se afirma.**
   `ConnectorCatalogApiTest:223-232` exige 403, el código en el cuerpo **y**
   `verifyNoInteractions(catalog)`: la credencial no abre nada, no escribe nada y
   no llega al caso de uso. Una enmienda que relajara la seguridad se vería aquí;
   esta no la relaja.

**Dónde estaría la raya**, para que quede dicho: si hubiera enmendado a 403 sin
precedente ratificado, o si el cambio hubiera convertido un rechazo en un permiso,
o si lo hubiera hecho sólo en la bitácora sin tocar el `.feature`. Hizo las tres
cosas bien: enmendó el `.feature` con el comentario delante, lo razonó donde
`REGLAS.md` seccion 8 manda, y no cambió lo que la prueba impide.

**Condición C7**: es una edición de contrato. La de 27 lleva «ratificada por el
propietario»; esta cita la de 27 pero no lleva firma propia. Que el propietario la
contrafirme en el `.feature` antes de marcar la feature `done`. Formalidad, no
desacuerdo.

---

## 6. El cableado del orquestador — **fiel al contrato, y sin oráculo en una mitad**

Revisado como cualquier otro cambio (commit `0ecc8d7`).

**Fiel, y lo verifico:**

- `frontend/src/workspace.tsx:107` («Importación») seguido de `:116`
  («Conectores», `/conectores`) y `:125` («API para integraciones»). Catorce
  `href` en total. «Hoy» sigue en `:44`, primera. Es exactamente lo que fija
  `@s33` (`additional_connectors.feature:421`): «situada después de «Importación»
  sin desplazar «Hoy»».
- La costumbre de la casa era añadir al final; el escenario ratificado de la
  feature dice otra cosa y **el escenario manda**. Decisión correcta y bien
  argumentada en el mensaje del commit.
- `project-spec.md:2529-2542`: la ampliación a catorce entradas es consistente con
  el menú, con el orden y con las dos specs E2E, y explica por qué prevalece el
  escenario. Fiel.
- **Y tiene oráculo**: `e2e/export-data.spec.mjs:61-80` afirma la lista canónica
  entera —recuento y nombre por posición— y la enmienda que hay encima (`:55-60`)
  prohíbe expresamente afirmar por índice relativo. «Conectores» está en la lista,
  en su sitio. Esto es lo que impide que la posición caduque en silencio; se hizo
  bien.
- `App.tsx:59-60`: la etiqueta `section` es «Conectores» para las dos rutas.
  Coherente.
- **Reparación de los ocho rangos de Stryker de las features 28 y 30**
  (`stryker.automations.config.json`, `stryker.external-calendar.config.json`),
  verificada imprimiendo el texto cubierto. Sin eso, dos campañas habrían mutado
  sentencias `import` y dado una puntuación sin significado. Era obligatorio y se
  hizo.

**Lo que falta: las dos rutas no tienen oráculo.** `App.tsx:50-51`
(`const connectorsCatalog = route === "/conectores"` y su gemela) y `App.tsx:90-93`
(el árbol que monta `<ConnectorsCatalog />` y `<GitlabConnector owner={username} />`)
son **producción que ninguna prueba exige**. Lo he buscado explícitamente: los
únicos ficheros de prueba que mencionan «conectores» son
`connectors-catalog.test.tsx` —que renderiza el componente, no `App`— y
`external-calendar-api.test.ts`, que no viene al caso. Ni una prueba navega a
`/conectores` ni a `/conectores/gitlab`. Borre esas cuatro líneas y toda la suite
sigue verde: el enlace del menú seguiría existiendo (lo afirman las E2E) y
llevaría a una pantalla en blanco. Las «27 pruebas de ruta» que cita el commit son
las que ya existían: acreditan que no se rompió nada, no que lo nuevo funcione.

**Y existe el precedente en la casa**:
`frontend/src/github-connector-routing.test.tsx` hace exactamente esto para la
feature 27 —`render(<App />)`, `go(path)`, comprobar que la pantalla aparece—. La
feature 29 no tiene su análogo. No pido inventar nada: pido copiar el patrón de al
lado.

**Nota sobre `progress/hallazgo_rangos_stryker.md`.** El hallazgo es correcto y
grave, y su alcance es anterior a esta feature: trece configuraciones ya cubrían
fragmentos a media palabra (`io`, `hooks`, `m "./externa`) y sentencias `import`
antes de este cableado. El commit `0ecc8d7` reparó las dos que estaban bien y se
habrían roto; no empeoró las demás, que ya estaban rotas. Suscribo la
**recomendación 2** del hallazgo: sacar de `App.tsx` y `workspace.tsx` la tabla de
rutas y la de entradas de navegación a un módulo por feature elimina la clase
entera de fallo. Fijar coordenadas linea:columna sobre ficheros compartidos que
crecen es frágil por construcción, y ninguna disciplina lo arregla. **No lo
condiciono a esta feature** —es deuda del proyecto, no de la 29— pero debería ser
la siguiente tarea de arnés.
