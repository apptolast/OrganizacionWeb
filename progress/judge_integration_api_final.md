# Review final — feature 24 `integration_api`

**Veredicto:** APPROVED

Juez de cierre, 9 de septiembre de 2026. Dictamen sobre las correcciones al
REJECTED de `progress/judge_integration_api.md` (sección 8, «Cambios
requeridos»). Checkout `main` en `926fb6a`.

Los **tres bloqueantes están cerrados** y verificados uno por uno contra el
código y los artefactos, no contra el relato del artesano. Los dos puntos no
bloqueantes (4 y 5) también están hechos. El punto 6 —mi propia afirmación
sobre la mutación— **lo retiro en parte**: ver sección 4. Apruebo mi puerta;
`done` sigue condicionado a la puerta del `mutation_tester` y a las condiciones
de la sección 8.

Superficie total del trabajo desde `f8570d7` (verificada con
`git diff --name-only f8570d7..926fb6a`), fuera de `progress/`:

```
.env.example
backend/src/main/java/.../adapter/config/SecurityConfiguration.java   <- único fichero de producción
backend/src/test/java/.../adapter/ApiCredentialBearerAdmissionTest.java
backend/src/test/java/.../adapter/ApiCredentialBearerTest.java
backend/src/test/java/.../adapter/ApiCredentialHttpPersistenceTest.java
backend/src/test/java/.../adapter/SecurityHeadersTest.java            <- nuevo
docs/ecc-harness.md
project-spec.md
```

`features/integration_api.feature` **no se ha tocado**: el contrato aprobado
sigue intacto, que es lo correcto dado que A1 y A2 no se implementaron.

---

## 1. Bloqueante 1 — oráculo de scope cruzado: **CERRADO**

El criterio de aceptación que fijé era exacto: sustituir a mano
`!access.scopes().contains(permission.get().scope())` por
`access.scopes().isEmpty()` en `ApiCredentialBearerFilter.java:149` **debe
poner la suite en rojo**. Se cumple, y de la forma más discriminante posible.

Evidencia recontada por mí desde el XML crudo
`progress/int24_closure/red_cross_scope_TEST-ApiCredentialBearerAdmissionTest.xml`
(`tests="29" skipped="0" failures="11" errors="0"`, sello 2026-09-08T23:51:48Z),
parseando testcase a testcase:

- Las **18** filas de `s22_eachIndependentScopeAdmitsItsDeclaredOperation`
  pasan bajo el mutante. Es decir: la suite anterior era **ciega** a la
  sustitución, tal y como afirmaba H1.
- Las **11** filas nuevas de `s22_s23_anotherValidScopeNeverGrantsTheOperation`
  fallan, todas con
  `org.opentest4j.AssertionFailedError: expected: <403> but was: <200>`.
- No falla nada más: 11 fallos, 11 filas nuevas. La correspondencia es exacta.

Comprobado además que el rojo es **real y no accidental**, cruzando cada fila
con la tabla `PERMISSIONS` (`ApiCredentialBearerFilter.java:27-102`): las once
rutas están en la allowlist y en las once el scope concedido es válido pero
distinto del requerido, así que bajo el mutante el permiso se concede y la
petición llega a la cadena (200); con el código real se deniega (403).

| Fila | Ruta (scope exigido por `PERMISSIONS`) | Scope concedido |
| --- | --- | --- |
| 1 | `GET /api/v1/projects/{id}` (`projects:read`, :34) | `projects:write` |
| 2 | `PUT /api/v1/projects/{id}` (`projects:write`, :41) | `projects:read` |
| 3 | `GET /api/v1/projects` (`projects:read`, :30) | `tasks:read` |
| 4 | `POST /api/v1/projects` (`projects:write`, :37) | `history:read` |
| 5 | `GET .../tasks` (`tasks:read`, :45) | `projects:read` |
| 6 | `POST .../tasks` (`tasks:write`, :65) | `tasks:read` |
| 7 | `GET .../tasks/{id}` (`tasks:read`, :49) | `agenda:read` |
| 8 | `GET /api/v1/today` (`agenda:read`, :68) | `history:read` |
| 9 | `GET .../blocks` (`agenda:read`, :72) | `tasks:read` |
| 10 | `GET /api/v1/history` (`history:read`, :93) | `agenda:read` |
| 11 | `GET /api/v1/weekly-review` (`history:read`, :97) | `projects:write` |

Las **dos filas literales del contrato** (`@s23` `Examples`, líneas 266 y 267
de `features/integration_api.feature`: «GET de proyecto con sólo
projects:write» y «PUT de proyecto con sólo projects:read») son las filas 1 y 2.
Los seis scopes aparecen como scope concedido y como scope exigido, de modo que
ninguna sustitución trivial de la condición —ni un `contains` por un scope
fijo— sobrevive.

Oráculo del test (`ApiCredentialBearerAdmissionTest.java:106-127`), leído
entero: 403, `application/problem+json`, cuerpo con `API_SCOPE_DENIED`, sin
`Set-Cookie`, `SecurityContextHolder` sin autenticación propagada,
`verify(authenticate).authenticate(TOKEN)` y `verifyNoInteractions(quota, chain)`.
Eso cubre la cláusula `And no concede otro scope implícitamente` de @s22 y el
`sin CSRF_INVALID, negocio ni consumo de cuota` de @s23 en el nivel unitario.

Estado actual del código: la condición está **restaurada**
(`ApiCredentialBearerFilter.java:148-149` usa `contains`), no quedó ningún
resto del mutante. Producción sin cambios: el defecto era de oráculo, no de
implementación, y se resolvió sin tocar `src/main`. Correcto.

Reejecución mía en el árbol actual (`926fb6a`), única ejecución que he corrido:

```
backend\gradlew.bat test --no-daemon --tests "com.apptolast.organization.adapter.ApiCredentialBearer*"
BUILD SUCCESSFUL in 15s
TEST-...ApiCredentialBearerAdmissionTest.xml  tests="29" skipped="0" failures="0" errors="0"
TEST-...ApiCredentialBearerTest.xml           tests="53" skipped="0" failures="0" errors="0"
```

## 2. Bloqueante 2 — filas de la matriz de la allowlist: **CERRADO**

Las tres filas pedidas están en el `@CsvSource` de
`ApiCredentialBearerTest.java:170-173`, dentro de
`s23_allowlistDeniesOtherMethodsAndRoutesEvenWithAllScopes` (`:174`), que
concede los **seis** scopes y adjunta una cookie `SESSION` válida:

| Fila `Examples` de @s23 sin cubrir (H1) | Ruta añadida |
| --- | --- |
| `crear o modificar bloque` (:270) | `POST /api/v1/projects/p/tasks/t/blocks` |
| `consultar sesión de trabajo fuera de la allowlist` (:271) | `GET /api/v1/work-sessions/active` |
| `consultar preferencias, exportación o importación` (:272) | `GET /api/v1/me/appearance` |

Rojo verificado por mí en
`progress/int24_closure/red_allowlist_rows_TEST-ApiCredentialBearerTest.xml`
(`tests="53" skipped="0" failures="3" errors="0"`, sello 2026-09-09T00:23:38Z):
fallan **exactamente** las filas `[17]`, `[18]` y `[19]` —las tres nuevas— con
`java.lang.AssertionError: Status expected:<403> but was:<500>`, y ninguna de
las 16 previas.

El rojo se fabricó ampliando temporalmente `PERMISSIONS` en vez de mutando la
condición. Es una técnica más débil que la del bloqueante 1, pero aquí es la
adecuada: lo que estas filas fijan es **la frontera de la allowlist**, y la
única forma de moverla es añadir la ruta. Y tiene un efecto colateral valioso
que confirmo: el `500` (en vez de `404`) demuestra que las tres rutas
**resuelven contra manejadores reales** del producto. Es decir, la denegación
que fija el test es una denegación de verdad, no un 403 accidental sobre una
ruta inexistente. Ese matiz es el que faltaba en las filas 3, 4 y 5 de H1.

Verde en el árbol actual: 53/53, ver sección 1.

## 3. Bloqueante 3 — mapa `@s1–@s42 → test`: **CERRADO**

`progress/tdd_integration_api.md` existe (260 líneas en `8288034`, +19 en
`19de112`), tiene sección `## Trazabilidad` con las 42 filas, referencia las
siete bitácoras de ciclo en vez de duplicarlas, y añade los ciclos C1–C4 de la
sesión de cierre, la tabla de renombrados de H3 y los límites.

**No me he fiado de la tabla: he verificado las referencias que cambiaron.**
`grep -n "void s"` sobre las tres clases tocadas devuelve, línea a línea,
exactamente lo que el mapa declara:

```
ApiCredentialBearerAdmissionTest.java:76   s22_eachIndependentScopeAdmitsItsDeclaredOperation
ApiCredentialBearerAdmissionTest.java:106  s22_s23_anotherValidScopeNeverGrantsTheOperation
ApiCredentialBearerTest.java:54            s19_malformedOrInvalidBearerNeverFallsBackToCookie
ApiCredentialBearerTest.java:90            s23_eachOperationRequiresItsOwnScopeBeforeQuota
ApiCredentialBearerTest.java:105           s24_authenticationPrecedesOriginAndOriginPrecedesScope
ApiCredentialBearerTest.java:174           s23_allowlistDeniesOtherMethodsAndRoutesEvenWithAllScopes
ApiCredentialBearerTest.java:198           s19_duplicateAuthorizationIsRejectedBeforeAuthentication
ApiCredentialBearerTest.java:211           s20_realSessionFilterDoesNotConsultCookieOrCreateSessionForBearer
ApiCredentialBearerTest.java:224           s32_openApiAuthenticatesWithoutScopesOrQuota
ApiCredentialBearerTest.java:237           s20_bearerAuthenticatesAdmitsThenReadsAsOwnerWithoutCookie
ApiCredentialHttpPersistenceTest.java:78   s19_s21_realInvalidRevokedOrDisabled...
ApiCredentialHttpPersistenceTest.java:127  s23_bearerLogoutIsDenied...
ApiCredentialHttpPersistenceTest.java:182  s23_defaultFrameworkLogoutCannotBypass...
ApiCredentialHttpPersistenceTest.java:196  s17_s20_realTokenStorageFailure...
```

Ninguna cita del mapa apunta a un método inexistente ni a una línea equivocada.
Los renombrados de H3 (punto 4, no bloqueante) están hechos y son puramente
mecánicos: el diff de `1889a98` no altera ni un solo oráculo, solo prefijos
`sNN_`. El punto 5 (ruta de los originales de Stryker) también está corregido.

Los 42 escenarios quedan con al menos un test concreto. **@s22 y @s23 pasan de
`[~]`/`[ ]` a `[x]`.**

## 4. Mutación — **retiro parcialmente mi punto 6**

Lo que dije: «tras el arreglo no hace falta repetir campaña porque los tests
nuevos solo pueden aumentar las muertes sobre el mismo universo». Esa
afirmación era válida **para el arreglo que yo pedí**, y sigue siéndolo:

- **Confirmo** el punto 6 para los ciclos C1, C2 y C3. Solo añaden y renombran
  tests; no tocan `src/main`. Sobre el mismo universo de mutantes, más tests
  solo pueden matar más. Las tres campañas (180/188, 114/118, 836/997) siguen
  siendo mediciones válidas y conservadoras para ese perímetro.
- **Retiro** el punto 6 para el ciclo C4, que yo no había previsto. C4 **añade
  producción**: `SecurityConfiguration.java` (+23 líneas). Y esa clase **está
  dentro del universo medido**: `progress/mutation_integration_api_http.md:5`
  declara los cinco patrones objetivo de la campaña HTTP —
  `ApiCredentialController`, `ApiCredentialSessionIdResolver`,
  `ApiCredentialBearerFilter`, `IntegrationOpenApiController` y
  **`SecurityConfiguration`**— sobre el checkout `c6121aa`.

Consecuencia, dicha sin rodeos: **el 114/118 ya no describe las fuentes
actuales.** El literal `CONTENT_SECURITY_POLICY`, el método `securityHeaders` y
las dos llamadas `.headers(...)` son código que ningún mutante ha visitado
nunca. El `mutation_tester` **debe reejecutar la campaña
`integration_api-http-backend`** antes de que la 24 pueda marcarse `done`. Las
campañas de núcleo y de frontend no están afectadas: `SecurityConfiguration` no
entra en su universo y ninguna de sus fuentes cambió.

Esto no invalida nada de lo ya medido ni obliga a rehacer las tres campañas.
Obliga a una, y la afirmación de `progress/tdd_integration_api.md:257-259` («el
ciclo C4 sí añade producción, fuera del universo medido por las tres campañas»)
es **incorrecta en su segunda mitad**: está dentro del universo de la campaña
HTTP. Que quede corregido aquí.

## 5. A1 y A2 — dictamen independiente sobre la resolución del coordinador

**La resolución es correcta en el fondo. La confirmo, con una corrección de
forma que sí exijo.**

### El contrato dice literalmente lo que el artesano cita

He leído los escenarios en `features/integration_api.feature`, no la paráfrasis:

- **A2 contra @s32** (`:359-366`): `Given una sesión propia o un Bearer válido
  sin cuota restante` … `And no consume cuota ni ejecuta negocio`. Mover
  `quota.consume(access)` fuera del `if (!openApi)` rompe las dos cláusulas a la
  vez, y hace **inalcanzable el documento OpenAPI justo cuando la cuota está
  agotada**, que es precisamente cuando un integrador necesita leerlo para
  entender por qué le están rechazando. La cita es exacta.
- **A1 contra @s23** (`:263`): `Then recibe 403 API_SCOPE_DENIED sin
  CSRF_INVALID, negocio ni consumo de cuota`. Prohibición literal, no
  interpretativa.
- **A1 contra @s24** (`:285`): la fila `token válido sin scope y cuota agotada |
  ausente | 403 API_SCOPE_DENIED`. Comprobar la cuota antes del scope devuelve
  `429 API_RATE_LIMITED` en esa fila y la rompe. También exacto.

Tres escenarios aprobados, no uno. Y tres tests vigentes los fijan
(`ApiCredentialBearerTest.java:224`, `:105`, `ApiCredentialHttpPersistenceTest.java:235`).

### Juicio sobre la conducta y sobre la decisión

- **El artesano hizo lo correcto al parar.** Implementar A1/A2 habría sido
  cambiar el comportamiento contratado sin pasar por la puerta de aprobación
  humana del `.feature`, poniendo tres tests en rojo y «arreglándolos» después.
  Eso es exactamente la patología que estas puertas existen para impedir. Un
  hallazgo de una revisión de seguridad **de solo lectura** no es una
  autorización para reescribir el contrato: es una entrada para el
  `spec_partner`.
- **El contrato no debe reescribirse ahora**, y probablemente no deba
  reescribirse en la forma que proponía la revisión. La corrección propuesta en
  A2 es peor que el problema: acopla la lectura de un recurso **estático, sin
  datos del propietario y ya autenticado** al cupo de negocio, y degrada la
  diagnosticabilidad de la API en el peor momento. La de A1 rompe una
  precedencia —autenticación → origen → permiso → cuota— que está aserida con
  `inOrder` y que evita filtrar, vía código de estado, si un token tiene o no un
  scope cuando además está sin cuota.
- **El residuo es real pero acotado**, y lo digo sin suavizarlo: autenticar
  cuesta una `SELECT` sobre `api_credentials` que ningún contador mide. Un
  poseedor de credencial válida puede repetirla sin límite. No es acceso a
  datos ajenos ni escalada; es consumo de recursos por un cliente ya
  identificado y revocable.
- **La mitigación elegida es la proporcionada**: limitación de tasa por
  dirección en el proxy, que es la alternativa que la propia revisión ofrece en
  A1 y exige en A3. No contradice ningún escenario y cubre además el camino no
  autenticado, cosa que A1/A2 no hacían.

### Corrección de forma que sí exijo (no bloquea la aprobación, sí el despliegue)

`progress/security_review_connectors.md:112` afirma que la mitigación queda
«registrada en la sección de enmiendas de seguridad de `project-spec.md` y en
`progress/current.md`». **He comprobado los dos sitios y esa afirmación no se
sostiene:**

- `project-spec.md:2502`, párrafo «Pendiente del coordinador, no de los
  carriles», habla **solo** de la entrada de sesión por formulario (A3):
  «La entrada de sesión por formulario no tiene hoy ninguna protección contra
  adivinación de contraseña». No menciona A1, ni A2, ni el canal Bearer, ni la
  aceptación del riesgo.
- `progress/current.md` tiene 49 líneas y **no contiene ninguna mención** a A1,
  A2, `limit_req`, riesgo aceptado ni limitación de tasa. Lo he leído entero.

Esto importa por una razón concreta, no burocrática: la enmienda tal y como
está escrita conduce a un `limit_req` sobre `location = /api/session`, y **eso
no mitiga A1 ni A2**, que viven bajo `/api/v1/**`. Un riesgo aceptado cuya
mitigación está mal delimitada en el único documento normativo es un riesgo
aceptado que nadie va a mitigar. La decisión es correcta; su registro, no.

Requiero, **antes de cualquier despliegue productivo** y no antes de `done`:
que la sección de enmiendas de `project-spec.md` recoja explícitamente la
aceptación de A1 y A2 y que el alcance del `limit_req` por dirección incluya
`/api/v1/**` además de `/api/session`; y que `progress/current.md` lo refleje,
ya que el documento de seguridad lo da por hecho.

## 6. Cabeceras de seguridad (A8) y `.env.example` (A9)

**No alteran ninguna respuesta que el contrato fije.** Verificado así:

- El diff de producción es de **una sola clase** y **solo añade dos escritores
  de cabecera** a las dos cadenas (`SecurityConfiguration.java:60` y `:94`). No
  toca cuerpos, códigos de estado, ETags, `Cache-Control`, `Set-Cookie`,
  rutas, matchers, CSRF, ni el orden de filtros. `git diff --stat` sobre
  `backend/src/main` entre `f8570d7` y `926fb6a`: un fichero, +23/-1.
- `.headers(...)` en Spring Security **añade** a los escritores por defecto, no
  los sustituye; `SecurityHeadersTest.java:66` y `:83` lo comprueban aserando
  que `X-Content-Type-Options: nosniff` sigue presente.
- El `Cache-Control: no-store` que fija el contrato en los `problem+json` y en
  OpenAPI lo pone el filtro con `setHeader` (`ApiCredentialBearerFilter.java:176`),
  que reemplaza; no hay interferencia posible con CSP ni `Referrer-Policy`.
- El backend **no sirve estáticos** (`backend/src/main/resources/static` no
  existe), así que una CSP estricta no puede romper la SPA: esa la sirve nginx.
- La política es **byte a byte idéntica** a `deploy/nginx.conf:15`. Comparadas
  literalmente. Por tanto, aunque el cliente reciba la cabecera duplicada
  (nginx `add_header ... always` + upstream), la intersección de dos políticas
  idénticas es la misma política: comportamiento inalterado.
- Corroboración global: la suite completa quedó verde **después** del cambio
  (`19de112`: 3467 tests, 150 clases, 0 fallos), y mi ejecución filtrada sobre
  las 82 pruebas del filtro confirma el estado en `926fb6a`.

**`SecurityHeadersTest` verifica lo que dice verificar**, con una salvedad:

- `:54` cadena Bearer, petición **admitida**: 200 + CSP + `Referrer-Policy` +
  `nosniff`. Correcto, y usa la cadena real vía `@WebMvcTest` importando
  `SecurityConfiguration`, no un mock de la configuración.
- `:70` cadena Bearer, petición **rechazada** con 401: CSP + `Referrer-Policy`.
  Correcto y bien elegido: la postura no debe depender de que la petición
  triunfe.
- `:79` cadena de **sesión** (sin `Authorization`, luego el `securityMatcher`
  de la cadena Bearer no aplica): CSP + `Referrer-Policy` + `nosniff`.
  Correcto en cuanto a qué cadena ejercita. **Nit:** es el único de los tres
  que **no asería el código de estado**, así que pasaría igual si
  `GET /api/session` dejara de existir y respondiera 404 a través de la misma
  cadena. Añadir un `status()` lo convertiría en un oráculo cerrado.

`.env.example` (A9): sustituye un valor vacío que hacía fallar el arranque
(`SessionCookiePolicy.create("")` → `IllegalArgumentException`) por el loopback
real, y corrige el comentario que invitaba a dejarlo vacío. Es un fichero de
ejemplo; no entra en ninguna respuesta.

**Hallazgos, no bloqueantes, sobre este cambio:**

1. **A8 entra sin escenario.** Ninguno de los 42 `@s` menciona CSP ni
   `Referrer-Policy`. La producción **sí** la pide un test rojo
   (`SecurityHeadersTest`), así que no vulnera mi regla dura, pero llega por la
   vía de la revisión de seguridad y no por la del contrato, y la sección de
   enmiendas de `project-spec.md` recoge B1–B11 y el pendiente del coordinador,
   **no A8 ni A9**. Si la postura de cabeceras es contractual, merece su `@s`
   en una feature futura; si no lo es, merece al menos su renglón en las
   enmiendas.
2. **El literal de la política está duplicado** en
   `SecurityConfiguration.java:17-19` y `deploy/nginx.conf:15`, sin nada que los
   ate. Hoy coinciden; el día que diverjan, el navegador aplicará la
   **intersección** de ambas y la SPA puede romperse de una forma difícil de
   diagnosticar. Es la clase de literal repetido que este arnés no tolera en
   otros sitios.
3. **El rojo de C4 no tiene artefacto.** `progress/int24_closure/` conserva los
   dos XML de C1 y C2, pero el «3/3 en rojo» de C4 solo consta en la bitácora y
   en el mensaje de `2c6bc55`. Es creíble —la cabecera no existía— y no lo
   discuto, pero es evidencia de menor calidad que la de los dos bloqueantes.

## 7. Checkpoints

| # | Checkpoint | Estado | Evidencia |
| --- | --- | :---: | --- |
| C1 | El arnés está completo | **[x]** | Ficheros base y `docs/` presentes e íntegros; ninguno se ha tocado en esta corrección. `bin/harness init` **no ejecutado por mí** (máquina ocupada por tres carriles, restricción explícita del encargo): me apoyo en la suite completa verde de `19de112` (3467 tests / 150 clases / 0 fallos) y en mi ejecución filtrada de 82 tests sobre `926fb6a`, verde. Ver límite 1. |
| C2 | El estado es coherente | **[x]** | `feature_list.json`: única `in_progress` = 24; 25–30 en `spec_ready`. No lo he editado. `progress/current.md` describe la sesión activa y la reducción a tres carriles. |
| C3 | El código respeta la arquitectura | **[x]** | El único cambio de producción es un `headers(...)` en la capa de configuración del adaptador, donde corresponde. `application/` intacto. Sin dependencias nuevas: `ReferrerPolicyHeaderWriter` y `HeadersConfigurer` ya venían con Spring Security. Sin logs de depuración ni TODOs. |
| C4 | La verificación es real | **[x]** | El oráculo nuevo es unitario sobre el filtro real, y se apoya en la matriz HTTP con contexto Spring y en los tests contra PostgreSQL real ya acreditados. El rojo de los dos bloqueantes está conservado en XML crudo y **recontado por mí**, no leído del informe. Suite completa no reejecutada aquí a propósito. |
| C5 | La sesión se cerró bien | **[ ]** | Los commits están limpios y `progress/history.md` tiene entrada, pero el árbol de trabajo **no está limpio**: ` M .claude/settings.json` (cambio de permisos y de descripciones de hooks, sin commitear) y `?? backend/bin/` (salida `bin/main`, `bin/test` no cubierta por `.gitignore`, que solo ignora `build/`). Ninguna de las dos pertenece a la feature; ninguna la toco yo. Ver sección 8. |
| C6 | Contrato Gherkin (BDD) | **[x]** | `project-spec.md` §24 presente; `features/integration_api.feature` con 42 escenarios tagueados e intacto; **los 42 tienen test concreto** (mapa en `progress/tdd_integration_api.md`, referencias verificadas línea a línea); no hay producción que ningún test rojo haya pedido. Los tres huecos del rechazo anterior (H1, H2, H3) están cerrados. |
| C7 | Prueba de mutación | **[ ]** | **Pendiente por causa nueva**, no por incumplimiento: `SecurityConfiguration` cambió y está en el universo de la campaña HTTP (sección 4). El 180/188 y el 836/997 siguen vigentes; el 114/118 no describe las fuentes actuales. Puerta del `mutation_tester`, que debe reejecutar `integration_api-http-backend`. |

## 8. Condiciones antes de marcar `done` (no reabren mi puerta)

1. **Reejecutar la campaña de mutación HTTP** (`integration_api-http-backend`)
   y alcanzar el 80 % estricto con `SecurityConfiguration` ya modificada.
   Es la única campaña afectada.
2. **CI verde sobre `926fb6a`.** En el momento de firmar, la ejecución
   `34302616362` sobre ese commit estaba `in_progress`; la última completada con
   éxito es `34295169263`, sobre `ed4b41f`, **anterior** a las cabeceras, a los
   tests nuevos y al mapa. Nadie debe leer «CI verde» en este dictamen.
3. **Dejar el árbol limpio**: commitear o descartar `.claude/settings.json` —es
   configuración del agente, decisión del usuario, no mía ni del artesano— e
   ignorar o borrar `backend/bin/`.

Recomendado, no exigido: el `status()` que falta en `SecurityHeadersTest.java:79`,
atar el literal de CSP a `deploy/nginx.conf`, y llevar A8/A9 a la sección de
enmiendas de `project-spec.md`.

Antes de **desplegar**, y esto sí lo exijo: la corrección de registro de A1/A2
de la sección 5, con el `limit_req` por dirección alcanzando `/api/v1/**`.

## 9. Límites explícitos de este dictamen

Lo que **no** cubre, y que nadie debe dar por acreditado:

1. **No ejecuté `bin/harness init` ni la suite completa** (Java, Vitest ni E2E).
   La única ejecución de esta revisión fue
   `backend\gradlew.bat test --no-daemon --tests "com.apptolast.organization.adapter.ApiCredentialBearer*"`
   → 82 tests, 0 fallos, 0 errores. El «3467 tests, 0 fallos» de `19de112` es
   del artesano: lo cito, no lo he reproducido.
2. **No ejecuté ninguna campaña de mutación**, y esta vez tampoco recalculé los
   originales: los recálculos válidos son los de
   `progress/judge_integration_api.md` §4, sobre fuentes que para la campaña
   HTTP **ya han cambiado**.
3. **CI no verificada en verde sobre el commit juzgado** (sección 8, punto 2).
4. **No hay despliegue productivo.** Ninguna imagen de la 24 está desplegada, y
   el acceso SSH al host sigue denegado según `progress/current.md`.
5. **No hay aceptación en el servidor.** La API no se ha ejercitado sobre el
   dominio real con TLS. Nada aquí acredita comportamiento en producción.
6. **A1, A2 y A3 siguen siendo riesgo abierto** hasta que exista el
   `limit_req`. Apruebo la *decisión* de aceptarlos; no certifico que estén
   mitigados, porque no lo están.
7. **La evidencia UX sigue siendo automatizada y heurística**, y la evidencia
   axe es anterior a `01f80ab`. Sin cambios respecto al dictamen anterior.
8. **No juzgo las features 25–30** ni los commits de resguardo `wip(...)` de los
   carriles aparcados.
9. **No edité código, tests, `feature_list.json`, `.claude/settings.json` ni
   ningún otro fichero de configuración.** Este archivo es el único producto de
   la revisión.
