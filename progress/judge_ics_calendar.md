# Review — feature 26 `ics_calendar`

**Veredicto:** REJECTED

Superficie juzgada: `git log --oneline 7ea682d..186ff8d` (11 commits, 51 ficheros),
integrada en `main` (hoy `a6164e4`). Contrato: `features/ics_calendar.feature` @s1–@s38.

El carril es, en su inmensa mayoría, trabajo de primera: el backend está cerrado con
oráculos byte a byte, la seguridad del feed público es sólida y los tres defectos que
destapó el E2E están arreglados de verdad. **El único bloqueante es @s38**: más de la
mitad de sus cláusulas no tiene ningún oráculo y el artefacto que el propio escenario
exige (la revisión de los treinta principios) no existe. Es exactamente el error contra
el que avisa `AGENTS.md:51` — «no declarar cumplimiento global por pasar únicamente axe
o unas pocas resoluciones».

---

## Lo que he ejecutado yo (distinto de lo que cita el artesano)

Suites filtradas, una tanda cada vez, sin suite completa, sin PIT y sin E2E (tres
carriles más en la máquina):

| Clase / fichero | Pruebas | Fallos |
| --- | --- | --- |
| `adapter.CalendarApiTest` | 29 | 0 |
| `domain.IcsCalendarTest` | 18 | 0 |
| `domain.CalendarWindowTest` | 7 | 0 |
| `domain.CalendarFeedSecretTest` | 9 | 0 |
| `application.CalendarFeedUseCasesTest` | 15 | 0 |
| `adapter.persistence.CalendarPersistenceTest` | 15 | 0 |
| `adapter.persistence.SnapshotRenderCalendarTest` | 2 | 0 |
| `adapter.config.CalendarWiringTest` | 1 | 0 |
| **Backend del carril** | **96 en 8 clases** | **0** |
| `frontend src/calendar.test.tsx` + `src/calendar-feed-api.test.ts` | 40 | 0 |
| `adapter.SecurityHeadersTest` | 3 | 0 |
| `adapter.ApiCredentialBearerTest` | 53 | 0 |
| `ArchitectureTest` | 1 | 0 |
| `adapter.config.ApplicationWiringTest` | 24 | 0 |

Las 96 del backend y las 40 del frontend quedan **confirmadas por ejecución propia** (la
bitácora dice 95 y 39). Los vecinos de la feature 24 y el cableado global siguen verdes:
**no hay regresión**.

**`bin/harness init` NO se ha ejecutado.** `.harness/harness.mjs` lo define como lint +
`node scripts/project.mjs test`, es decir la suite completa (~20 contenedores
PostgreSQL), prohibida explícitamente por la instrucción de recursos de esta sesión. Los
checkpoints que dependen de él quedan sin marcar y son puerta pendiente para el cierre.

---

## Los cinco puntos exigidos con lupa

### 1. `SecurityConfiguration` — VERIFICADO, correcto

Leído línea a línea:

- Las **tres** cadenas llaman a `.headers(SecurityConfiguration::securityHeaders)`:
  `publicCalendarSecurity` (línea 57), `bearerSecurity` (línea 86) y `security` (línea 117).
  El literal de CSP de la 24 (líneas 16-18) está intacto y `Referrer-Policy: same-origin`
  se emite del mismo sitio. Ninguna ruta, incluidas `/calendar/**`, se queda fuera.
- El hueco que el artesano dice haber destapado era real y está cerrado con prueba:
  `CalendarApiTest.s11_s15_thePublicChainAlsoEmitsTheSecurityHeadersOfTheWholeApi`
  (CalendarApiTest.java:211-219) exige CSP, `Referrer-Policy` y `nosniff` **sobre el feed
  servido y sobre su 404**. Ejecutada por mí: verde. Si alguien retira la línea 57, se
  pone roja.
- El canal Bearer de la 24 sigue intacto: el `securityMatcher` (líneas 82-85) conserva
  `getHeader("Authorization") != null` y sólo le resta `!CalendarPaths.isCalendar(request)`.
  Esa resta **la exige el contrato**: @s7 pide `401 UNAUTHENTICATED`, no
  `API_UNAUTHENTICATED`, para las cuatro rutas con `Authorization` presente, y
  `CalendarApiTest.s7_...` (líneas 148-162) lo recorre con y sin cabecera Bearer.
  `ApiCredentialBearerTest` (53) y `SecurityHeadersTest` (3) siguen verdes.
- `CalendarPaths.isCalendar` (líneas 23-26) usa igualdad exacta para
  `/api/v1/me/calendar-feed` y `/api/v1/me/calendar.ics`, y prefijo sólo para
  `/calendar/`: no abre agujeros por coincidencia parcial. La ruta SPA `/calendario` no
  cae dentro del prefijo `/calendar/`, ni en Spring ni en nginx.

**Sin hallazgos.**

### 2. `@s11`: `text/calendar;charset=utf-8` sin espacio — DESVIACIÓN ACEPTABLE, sin código

Dictamino: **es una desviación aceptable del contrato; no exige código, y exige una
enmienda mínima de redacción del `.feature`**, que por tocar contrato aprobado lleva el
coordinador a la puerta humana. No bloqueo por ella.

Razones:

- **Es inevitable desde la aplicación.** `CalendarDocuments.write` (línea 29) llama a
  `setContentType("text/calendar; charset=utf-8")`; Tomcat parsea tipo y charset y los
  reserializa como `tipo + ";charset=" + encoding`. `setHeader("Content-Type", …)`
  desemboca en el mismo `setContentType`. El artesano no se escuda: describe bien la causa.
- **Es semánticamente idéntica.** El espacio tras `;` es OWS opcional en la gramática de
  parámetros de RFC 9110. Ningún cliente de calendario distingue las dos formas.
- **Arreglarlo por código sería peor que la enfermedad**: exigiría un filtro o un valve
  peleándose con la normalización del contenedor para recuperar un octeto sin significado.
  Eso sí sería maquillaje.
- **No está tapada, y el cliente no la ignora**: `calendar-feed-api.ts:82` valida con
  `/^text\/calendar\s*;\s*charset=utf-8$/i`, tolerante al OWS y estricta en lo demás; el
  E2E afirma lo que entrega el contenedor con el porqué al lado
  (`e2e/ics-calendar.spec.mjs:66-71`); la prueba MockMvc sigue fijando lo que pide el
  controlador. La trazabilidad es honesta en las tres capas.

Enmienda recomendada: una línea en el Background del `.feature` diciendo que, al comparar
`Content-Type` exacto, el OWS opcional tras `;` no cuenta.

### 3. `location /api/` duplica tres cabeceras — CONFIRMADO, defecto real ajeno al carril

Verificado en `deploy/nginx.conf`:

- Las tres `add_header … always` están a nivel de `server` (líneas 13-15) y se heredan en
  toda `location` que no declare las suyas.
- `location /calendar/` (líneas 40-53) las neutraliza con tres `proxy_hide_header`
  (44-46): llega **una** copia de cada una. Correcto.
- **`location /api/` (líneas 54-61) no tiene ni `add_header` propio ni `proxy_hide_header`.**
  Desde el hallazgo A8 de la feature 24 el backend emite CSP y `Referrer-Policy` en sus
  cadenas, y Spring Security emite `nosniff` por defecto. Con `add_header` sumando,
  **todas las rutas `/api/` están devolviendo `X-Content-Type-Options`,
  `Content-Security-Policy` y `Referrer-Policy` por duplicado detrás del proxy.**

El aviso del artesano es correcto y su decisión de no tocarlo, defendible. Severidad real:
**baja pero no nula**. Hoy es benigno (Fetch toma el primer valor de nosniff; dos CSP
idénticas se intersecan en sí mismas; `Referrer-Policy` toma el último válido), pero deja
de serlo en cuanto las dos posturas diverjan — y hoy son dos literales copiados a mano en
dos ficheros distintos, que es justo el escenario en el que divergen. Corresponde a un
carril propio sobre la feature 24, no a éste.

### 4. Seguridad del feed público — VERIFICADA, sin hallazgos

- **Entropía y generación**: `CalendarFeedSecret.issue` toma 32 octetos de `SecureRandom`
  → 256 bits → 43 caracteres base64url sin relleno. No se fabrica desde el identificador
  ni desde el reloj. `CalendarFeedSecretTest` fija longitud, alfabeto y no repetición.
- **Sólo el hash en reposo**: `V24__calendar_feed_tokens.sql` impone
  `token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32)`; no hay columna
  para el claro. `CalendarPersistenceTest.s1_s29_...` lo comprueba contra PostgreSQL real.
- **Resolución**: `PostgresCalendarStore.ownerOf` es `WHERE token_hash=?` con el digest
  completo. Ni prefijo, ni `LIKE`, ni comparación parcial (@s15).
- **404 y 405 indistinguibles**: `PublicCalendarController.unsupportedMethod` responde
  **antes de leer el candidato** (líneas 39-52), así que un token válido y uno inexistente
  dan respuesta idéntica en estado, cabeceras y cuerpo: `CalendarApiTest.s16_...` compara
  las dos cadenas completas. Los ocho 404 de @s15 son byte a byte idénticos, incluidas
  todas las cabeceras (`s15_everyUnresolvableAddressAnswersTheSameNotFound`). El detalle
  fino que lo hace posible — `CalendarProblems` escribe el problema directamente en la
  respuesta para esquivar el `Content-Disposition: inline;filename=f.txt` que
  `AbstractMessageConverterMethodProcessor` cuelga de toda ruta acabada en `.ics` — está
  bien razonado y documentado en el propio fichero. Por tiempos no hay fuga útil: el 405
  no hace E/S alguna y todos los 404 de token bien formado hacen la misma búsqueda
  indexada.
- **Revocación**: `DELETE` real de la fila, no bandera; verificado contra base real
  (`s5_revokeDeletesTheRowAndRepeatingItChangesNothing`) y de punta a punta en
  `CalendarWiringTest`. Regenerar sustituye con `ON CONFLICT (owner_id) DO UPDATE` y deja
  una sola fila incluso con dos escrituras concurrentes (`s9_...`, ejecutada, verde).
- **Caché**: `Cache-Control: private, no-store` se aplica en `CalendarDocuments.harden`
  **antes** de resolver nada, así que lo llevan también el 404, el 405, el 413 y el 503.
- **Logs**: `CalendarApiTest.s11_s15_theRequestLeavesNoTokenAndNoCalendarPathInTheLogs`
  captura el logger raíz y afirma que no aparecen ni el token ni `/calendar/`;
  `deploy/nginx.conf:41` añade `access_log off` para que el token no caiga en un log de
  acceso. Los 404 y 413 se resuelven con manejadores propios que no tocan el logger de
  `ApiErrors`.
- **Referrers**: el token nunca es destino de navegación. La vista lo muestra en un
  `input readOnly` y lo copia al portapapeles; no hay ningún `<a href>` con la url del
  feed. Y la cadena emite `Referrer-Policy: same-origin`.

### 5. Los tres defectos del E2E — ARREGLADOS, no silenciados

1. **Cliente atado a `https://`** (habría roto cualquier despliegue sin TLS):
   `calendar-feed-api.ts:11` es hoy `^https?:\/\/…` con el resto de la forma intacta, y hay
   una prueba con nombre que lo sujeta: «@s32 accepts the address of a deployment served
   over plain http» (`calendar-feed-api.test.ts:115`). No es una relajación general: sigue
   rechazando una url que no sea la dirección pública del feed («@s32 refuses a creation
   answer whose url is not the public feed address»).
2. **`nosniff, nosniff`**: cerrado con los tres `proxy_hide_header` de `nginx.conf:44-46`,
   y el E2E lo comprueba con una aserción que caza la duplicación, no sólo la presencia
   (`expect(csp).not.toContain("frame-ancestors 'none', ")`, spec:79-81).
3. **Aislamiento entre sus propias pruebas**: `withoutFeedToken()` al principio de cada
   preparación (spec:20-25) en vez de heredar el estado de la anterior; y la lectura sin
   espera se sustituyó por la aserción web-first `await expect(field).not.toHaveValue(first)`
   (spec:142). Arreglada la causa, no el síntoma.

---

## Cobertura de escenarios (@s ↔ test)

Verificada leyendo cada prueba, no fiándome de la tabla del artesano.

- @s1 [x] `CalendarFeedUseCasesTest.s1_...`, `CalendarPersistenceTest.s1_s29_...`, `CalendarApiTest.s1_...`, `CalendarFeedSecretTest`
- @s2 [x] `CalendarFeedUseCasesTest.s2_...`, `CalendarApiTest.s2_...`
- @s3 [x] `CalendarFeedUseCasesTest.s3_...`, `CalendarApiTest.s3_...` (afirma ausencia de token y de `url`)
- @s4 [x] `CalendarFeedUseCasesTest.s4_...`, `CalendarPersistenceTest.s4_...`, `CalendarWiringTest`
- @s5 [x] las tres filas: `CalendarFeedUseCasesTest.s5_...`, `CalendarPersistenceTest.s5_...`, `CalendarApiTest.s5_...`
- @s6 [x] `CalendarApiTest.s6_...`, las cinco filas exactas del Examples
- @s7 [x] `CalendarApiTest.s7_...`, las cuatro rutas × con y sin `Authorization`, con `Set-Cookie` vacío
- @s8 [x] `CalendarApiTest.s8_...` (CSRF y Origin) y `s8_s11_...` (feed sin credenciales)
- @s9 [x] `CalendarPersistenceTest.s9_twoConcurrentRegenerationsLeaveExactlyOneToken`, sobre PostgreSQL real
- @s10 [x] `CalendarFeedUseCasesTest.s10_...`, `CalendarPersistenceTest.s10_...`, `CalendarApiTest.s10_...`
- @s11 [x] con la desviación del punto 2 registrada: `s8_s11_...`, `s11_headAnswersTheSameHeadersWithAnEmptyBody`, `s11_s15_...logs`
- @s12 [x] **oráculo byte a byte**: `IcsCalendarTest.s12_...` compara el documento entero, 714 octetos, 21 líneas y las cinco longitudes físicas (74/75/20 y 75/55); `CalendarPersistenceTest.s12_...` para versión 1 y DTSTAMP de `created_at`
- @s13 [x] `IcsCalendarTest.s13_emptyCalendarIsSevenCrlfLinesOf158Octets`
- @s14 [x] `IcsCalendarTest.s14_...` (las tres filas), `CalendarPersistenceTest.s14_...`
- @s15 [x] `CalendarApiTest.s15_...` (las ocho direcciones, cabeceras y cuerpo idénticos), `CalendarFeedSecretTest.s15_...`, `CalendarPersistenceTest.s15_...`, `SnapshotRenderCalendarTest.s15_s30_...`
- @s16 [x] `CalendarApiTest.s16_...`, cuatro métodos × token conocido y desconocido
- @s17 [x] `CalendarApiTest.s17_...` (con y sin token), `CalendarFeedUseCasesTest.s17_...`
- @s18 [x] `CalendarWindowTest.s18_...` con las seis filas exactas del Examples, incluidos los segundos que el esquema V11 no permite persistir, más `CalendarPersistenceTest.s18_...` en las fronteras representables. La desviación está bien argumentada en la bitácora (ciclos 3 y 4).
- @s19 [x] `CalendarPersistenceTest.s19_...`
- @s20 [x] `CalendarPersistenceTest.s20_...`
- @s21 [x] `CalendarPersistenceTest.s21_...`
- @s22 [x] `IcsCalendarTest.s22_...`
- @s23 [x] `IcsCalendarTest.s23_...`: siete filas más 500 puntos de código con multibyte y fuera del BMP, con desescape de vuelta
- @s24 [x] `IcsCalendarTest.s24_...`, las cinco fronteras de plegado
- @s25 [x] `IcsCalendarTest.s25_...`
- @s26 [~] `CalendarFeedUseCasesTest.s26_...` (2000 y 2001) y `CalendarApiTest.s26_...` (413 en las dos rutas, sin `Content-Disposition`). **La cuarta fila del Examples — 2001 de los que 1 está cancelado → 200 con 2000 — no tiene prueba propia**; se cumple por composición (`CURRENT_BLOCKS` filtra `status='planned'` con `LIMIT 2001`, y @s19 verifica el filtro). Aceptable, pero es composición, no oráculo.
- @s27 [x] `CalendarFeedUseCasesTest.s27_...`, `CalendarPersistenceTest.s27_...`
- @s28 [x] `CalendarPersistenceTest.s28_...` (snapshot repetible real) y `SnapshotRenderCalendarTest.s28_...`
- @s29 [x] `CalendarPersistenceTest.s1_s29_...` (tienda nueva sobre la misma base) más el reinicio real del proceso en `e2e/ics-calendar.spec.mjs:154` con `restartBackend`. **El E2E lo cito del artesano; no lo he ejecutado.**
- @s30 [x] `CalendarFeedUseCasesTest.s30_...`, `CalendarPersistenceTest.s30_...`, `CalendarApiTest.s30_...` (las tres rutas, sin trazas ni token)
- @s31 [x] las cuatro filas en `calendar.test.tsx`, más la entrada de navegación justo después de «Exportación» con el índice comprobado
- @s32 [x] doble activación con un solo POST, anuncio, y olvido de la url al remontar
- @s33 [x] las tres filas del portapapeles
- @s34 [x] `it.each` con las dos acciones: foco en el `role="group"`, ambos botones y cero peticiones
- @s35 [x] las cinco filas (cancelar ×2, confirmar ×2, fallo sin reintento) más el foco al h1
- @s36 [x] siete filas repartidas entre `calendar-feed-api.test.ts` y `calendar.test.tsx`
- @s37 [~] la fila «navega a otra ruta» tiene oráculo (aborta, descarta el 401 tardío, nada en almacenamiento ni consola). **Las filas «cierra sesión» y «cambia la identidad de acceso» no tienen prueba propia**; se apoyan en `key={owner}` (`calendar.tsx:35`), que desmonta la vista por el mismo camino. Aceptable por composición; lo digo por su nombre.
- @s38 [ ] **SIN COBERTURA SUFICIENTE. BLOQUEANTE.** Ver la sección siguiente.

---

## El bloqueante: @s38

`e2e/ics-calendar.spec.mjs:192-249` es su único oráculo. Lo que pide el escenario frente a
lo que hace la prueba:

| Cláusula de @s38 | Estado |
| --- | --- |
| «los estados cargando, sin enlace, con enlace activo, enlace recién creado, confirmación abierta, descarga preparada y fallo» (siete) | **cuatro**: `sin enlace`, `enlace recién creado`, `confirmación abierta`, `descarga preparada`. Faltan `cargando`, `con enlace activo` (el de después de recargar, distinto del recién creado: sin campo de url) y `fallo`. El artesano informó de «cinco estados»: **son cuatro**, contadas una a una las llamadas a `audit(...)` |
| anchos 320 / 768 / 1280 | [x] |
| «con texto al 200 %» | **[ ] nunca se ejecuta** |
| «y con zoom nativo al 200 % a 320 px» | **[ ] nunca se ejecuta** |
| «todos los controles se alcanzan con Tab en orden lógico, foco visible» | **[ ] el oráculo es un único `page.keyboard.press("Tab")` con `expect(focused).toBeTruthy()`** (líneas 246-248). No comprueba ni orden ni foco visible: pasaría con cualquier elemento enfocado |
| «objetivo de al menos 44 × 44 px CSS» | [~] sólo por la regla `target-size` que arrastra el tag `wcag22aa` de axe; no hay medida propia |
| «el campo de url … su contenido completo se selecciona con teclado» | **[ ] sin oráculo**: el `onFocus … select()` de `calendar.tsx:248` no lo comprueba nadie |
| «ningún ancho recorta la url ni provoca desbordamiento horizontal» | [~] el desbordamiento sí (`scrollWidth > clientWidth`); el recorte de la url, no |
| «axe no reporta violaciones en ninguno de los estados y anchos» | [x] para los cuatro estados auditados |
| «temas claro y oscuro, forced-colors y movimiento reducido conservan legibilidad y operación» | **[ ] nunca se ejecuta**: no hay `emulateMedia`, ni `colorScheme`, ni `forcedColors`, ni `reducedMotion` |
| «la revisión de los treinta principios registra evidencias y límites humanos sin inferir cumplimiento universal desde axe» | **[ ] el artefacto no existe**: no hay `progress/ux_ics_calendar.md`, y `docs/ics-calendar.md` (91 líneas) no contiene ninguna matriz de principios |

Por qué esto no es formalismo:

1. **Hay precedente firme y unánime.** Diecisiete features tienen su `progress/ux_<name>.md`
   con la matriz y sus límites (`ux_export_data.md`, `ux_integration_api.md`, …). El
   escenario gemelo de export_data (`features/export_data.feature:352`) pide literalmente
   lo mismo y su juez lo dio por cumplido contra ese artefacto.
2. **`AGENTS.md:51` lo prohíbe de forma explícita**: «No declarar cumplimiento global por
   pasar únicamente axe o unas pocas resoluciones». Es exactamente lo que hay hoy.
3. **La comprobación que falta es justo la que encuentra defectos aquí.**
   `progress/current.md` registra que el único fallo de CI de la feature 24 fue «@s41
   texto 200 % a 320 px, marca de 345 px». El proyecto tiene specs dedicadas
   (`e2e/reschedule-text.spec.mjs`, `e2e/reschedule-native-zoom.spec.mjs`). Y esta vista
   tiene precisamente el control de riesgo: un `input` con una url de 43 caracteres a
   ancho completo, a 320 px, con el texto al 200 %.
4. **`project-spec.md:2122` también lo pide** para esta feature: «axe y barrido
   320/768/1440 **con zoom 200 %**». No está.

---

## Disciplina TDD

- **¿Producción sin test que la pida? Prácticamente no.** Un único hallazgo menor:
  `PublicCalendarController.unsupportedMethod` incluye `RequestMethod.OPTIONS` en su mapeo
  (`PublicCalendarController.java:46`) y `CalendarApiTest.s16_...` sólo recorre POST, PUT,
  DELETE y PATCH, que son las cuatro filas del Examples. Es coherente con el título del
  escenario y cambia la respuesta a OPTIONS respecto de la de Spring, pero hoy no lo pide
  ninguna prueba roja. No bloquea; se cierra con una fila más en el `@ValueSource` o
  retirando la constante.
- **¿Evidencia de Rojo→Verde→Refactor? SÍ**, y de calidad inusual: catorce ciclos con el
  fallo literal citado antes del arreglo, y **dos rojos de comportamiento genuinamente
  instructivos** — el `Content-Disposition: inline;filename=f.txt` que rompía la
  indistinguibilidad de @s15 (ciclo 5) y la cadena `@Order(0)` sin CSP (ciclo 12) — con la
  prueba escrita antes en ambos casos.
- **Honestidad**: el ciclo 8 admite por su cuenta que las 25 pruebas de la vista se
  escribieron juntas y pasaron a la primera («es un ciclo más grueso que los del
  backend»); el ciclo 13 se **rectifica a sí mismo** sobre un fallo que había atribuido a
  otro carril. Eso vale mucho. Y es esa misma vara la que hace chirriar que @s38 se declare
  cerrado.

## Calidad (lente de artesano)

Lo bueno, que es casi todo:

- **`CalendarPaths`** concentra las tres direcciones en un sitio para que seguridad y
  enrutado no se separen. Es la decisión de diseño que sostiene el punto 1.
- **`CalendarDocuments.harden`** se aplica antes de resolver nada: éxito y fallo llevan la
  misma protección, y por eso los 404 salen idénticos sin esfuerzo.
- **`CalendarProblems`** documenta en el propio fichero *por qué* esquiva el conversor de
  mensajes: un comentario que explica una fuerza real, no lo que hace el código.
- **`IcsCalendar.line`** (17 líneas) resuelve plegado a 75 octetos, puntos de código y
  escapes con una sola noción, el «átomo». Sin números mágicos: `MAX_LINE_OCTETS`, `CRLF`.
- **`CalendarWindow`** es la respuesta correcta a un rojo de infraestructura: la regla se
  llevó al dominio, donde los segundos de @s18 sí son representables, y el SQL quedó como
  espejo exacto de `covers`.
- **Arquitectura respetada**: dominio sin frameworks (`ArchitectureTest` verde), puertos en
  `application`, adaptadores en `adapter`. El bean único que sirve a `CalendarFeedTokens` y
  `CalendarQueries` está justificado por el snapshot compartido, y el ciclo 10 corrigió el
  `@Component` que rompía `ApplicationWiringTest`.

Fragilidades menores, ninguna bloqueante:

1. `IcsCalendar.java:81` — `content.charAt(i) == '\' ? i + 2 : …` reventaría con
   `StringIndexOutOfBoundsException` si una línea terminase en barra invertida suelta. Hoy
   es imposible (`escapeText` siempre emite pares y las demás propiedades no llevan
   barras), pero es una precondición implícita que ninguna prueba fija.
2. `frontend/src/styles.scss` — `text-overflow: ellipsis` en el campo de url: correcto para
   el layout, pero es justo la cláusula «ningún ancho recorta la url» que @s38 deja sin
   verificar.
3. `frontend/src/calendar.tsx:196` — `<main id="proyectos">` en la vista de Calendario.
   Sigue la convención del destino del salto de navegación, pero el nombre miente sobre el
   contenido.

## Checkpoints

- **C1** [~] Ficheros base y docs presentes. `bin/harness init` **no ejecutado** (es la
  suite completa, prohibida en esta sesión): la tercera casilla queda sin marcar.
- **C2** [ ] `feature_list.json` da la feature 26 como **`spec_ready`** con su código ya
  integrado en `main`. La bitácora afirma que «sigue en `in_progress`»: no es así — el
  commit de estado se descartó en el reasentamiento del ciclo 11. Ninguna feature está
  `in_progress`. Incoherente; lo arregla el coordinador, no yo ni el artesano.
- **C3** [x] Capas respetadas, sin logs de depuración ni TODOs sueltos. `ArchitectureTest` verde.
- **C4** [~] Todo módulo nuevo tiene prueba y el aislamiento es real (Testcontainers con
  PostgreSQL, no dobles de base de datos). `bin/harness test` completo, no ejecutado.
- **C5** [ ] `progress/history.md` no tiene todavía entrada de la sesión de este carril y el
  estado de la feature no está reflejado (ver C2).
- **C6** [ ] **@s38 sin cobertura suficiente.** El resto del mapa @s → prueba es correcto y
  verificado uno a uno.
- **C7** [ ] Mutación pendiente: es la puerta del `mutation_tester`. Los alcances están
  declarados y guardados contra desplazamiento («ics calendar Stryker selects its own nodes
  of the shared files»), que es buena preparación.

## Cambios requeridos

1. **Cerrar @s38 de verdad** (bloqueante). En `e2e/ics-calendar.spec.mjs` o en una spec
   propia al estilo de `e2e/reschedule-text.spec.mjs` y `e2e/reschedule-native-zoom.spec.mjs`:
   1. Auditar los **siete** estados que nombra el escenario, no cuatro: añadir `cargando`
      (respuesta retenida), `con enlace activo` (tras recargar, sin campo de url) y `fallo`.
   2. Ejecutar el barrido **con texto al 200 %** y con **zoom nativo al 200 % a 320 px**,
      afirmando que no hay desbordamiento horizontal ni pérdida de controles. Es la
      comprobación que ya cazó un defecto real en la feature 24.
   3. Ejecutar **tema claro, tema oscuro, `forced-colors` y `prefers-reduced-motion`** con
      `page.emulateMedia`, afirmando operación y legibilidad; desactivar `color-contrast`
      en forced-colors como hace `e2e/appearance-ux-audit.spec.mjs:254`.
   4. Sustituir el `Tab` único por un recorrido real: todos los controles alcanzables en
      orden, con nombre accesible y foco visible; y afirmar que el campo de url se
      selecciona entero con teclado.
2. **Escribir `progress/ux_ics_calendar.md`** (bloqueante) con la matriz de los treinta
   principios de `docs/ux-requirements.md`, sus evidencias y sus **límites explícitos** (qué
   es automatizado, qué es heurístico, qué no se ha probado con personas), al modo de
   `progress/ux_export_data.md`. Sin inferir cumplimiento universal desde axe.
3. **Corregir la bitácora** `progress/tdd_ics_calendar.md`: dice «cinco estados» (son
   cuatro), «95 pruebas» (son 96), «39 del frontend» (son 40) y «la feature 26 sigue en
   `in_progress`» (está en `spec_ready`). Los números pequeños importan: el juez siguiente
   los usa como mapa.
4. **Menor, no bloqueante**: añadir la fila `OPTIONS` al `@ValueSource` de
   `CalendarApiTest.s16_...`, o retirar `RequestMethod.OPTIONS` de
   `PublicCalendarController.java:46`, para que ninguna producción sobreviva sin una prueba
   que la pida.

## Para el coordinador (fuera del alcance de este carril)

- **`deploy/nginx.conf:54-61`**: `location /api/` duplica `X-Content-Type-Options`,
  `Content-Security-Policy` y `Referrer-Policy` detrás del proxy, por la misma causa que
  este carril arregló en `/calendar/`. Defecto real de la feature 24 destapado aquí. Merece
  carril propio; y con él, considerar una única fuente para el literal de CSP en vez de dos
  copias a mano (`SecurityConfiguration.java:16-18` y `nginx.conf:15`).
- **Enmienda de @s11**: llevar a la puerta humana la nota de que el OWS opcional tras `;`
  en `Content-Type` no cuenta en la comparación exacta.
- **`feature_list.json`**: la feature 26 figura como `spec_ready` con el código en `main`.
- **`bin/harness init`** sigue sin ejecutarse en este veredicto por la restricción de
  recursos: es puerta pendiente para el cierre de sesión.
