# TDD — feature 24 `integration_api`: mapa `@s → test`

Documento de cierre exigido por `docs/verification.md` Nivel 4 y `CHECKPOINTS.md`
C6 (hallazgo **H2** de `progress/judge_integration_api.md`). Consolida en un solo
sitio la cobertura de los 42 escenarios de `features/integration_api.feature`.

**No duplica las bitácoras de ciclo.** El detalle Rojo→Verde→Refactor, con hash
de log por ciclo, vive en:

| Bitácora | Alcance |
| --- | --- |
| `progress/tdd_integration_api_backend.md` | 41 ciclos de núcleo (dominio, aplicación, persistencia) |
| `progress/tdd_integration_api_http.md` | 48 ciclos del canal HTTP y del filtro Bearer |
| `progress/tdd_integration_api_http_wiring.md` | cableado de la cadena de seguridad y del reloj |
| `progress/tdd_integration_api_openapi.md` | documento OpenAPI 3.1 como recurso estático |
| `progress/tdd_integration_api_frontend.md` | 69 ciclos de la pantalla de credenciales |
| `progress/tdd_integration_api_dispatcher.md` | despacho de rutas y `@CsvSource` de la allowlist |
| `progress/tdd_integration_api_text200.md` | corrección `.brand` a texto 200 % (`01f80ab`) |

Rutas de test sin el prefijo `backend/src/test/java/com/apptolast/organization/`.
Las líneas corresponden al árbol de la rama `claude/integration-api-closure`.

## Trazabilidad

| @s | Test(s) | Nota |
| --- | --- | --- |
| @s1 | `application/CreateApiCredentialTest.java:167`, `:128`; `adapter/persistence/ApiCredentialPersistenceTest.java:586`; `adapter/ApiCredentialApiTest.java:488`; `adapter/persistence/ApiCredentialWiringTest.java:112` | núcleo + persistencia + HTTP |
| @s2 | `application/CreateApiCredentialTest.java:19`, `:113`; `adapter/ApiCredentialApiTest.java:361`, `:378`, `:131`; `frontend/src/integration-api.test.tsx` ciclo 068 | núcleo + HTTP + frontend |
| @s3 | `application/CreateApiCredentialTest.java:146`; `adapter/persistence/ApiCredentialPersistenceTest.java:19` | |
| @s4 | `adapter/ApiCredentialApiTest.java:394`, `:426`, `:441`, `:281` | |
| @s5 | `application/CreateApiCredentialTest.java:79`, `:167` | |
| @s6 | `application/CreateApiCredentialTest.java:52` | |
| @s7 | `adapter/persistence/ApiCredentialPersistenceTest.java:137` | |
| @s8 | `adapter/persistence/ApiCredentialPersistenceTest.java:175` | |
| @s9 | `adapter/persistence/ApiCredentialPersistenceTest.java:49`; `adapter/ApiCredentialApiTest.java:459` | |
| @s10 | `adapter/persistence/ApiCredentialPersistenceTest.java:106` | |
| @s11 | `adapter/persistence/ApiCredentialPersistenceTest.java:175` | carrera con bloqueo real observado |
| @s12 | `adapter/persistence/ApiCredentialPersistenceTest.java:561`; `adapter/ApiCredentialApiTest.java:224`, `:237`; `adapter/persistence/ApiCredentialWiringTest.java:100` | |
| @s13 | `adapter/persistence/ApiCredentialPersistenceTest.java:528`, `:476`; `adapter/ApiCredentialApiTest.java:193`; `frontend/src/integration-api.test.tsx:485` | núcleo + HTTP + frontend |
| @s14 | `adapter/ApiCredentialApiTest.java:153`, `:337`, `:94`, `:113`; `adapter/persistence/ApiCredentialPersistenceTest.java:309` | |
| @s15 | `adapter/persistence/ApiCredentialPersistenceTest.java:488`; `adapter/ApiCredentialApiTest.java:167` | |
| @s16 | `adapter/persistence/ApiCredentialPersistenceTest.java:488`; `adapter/ApiCredentialApiTest.java:56`; `frontend/src/integration-api-client.test.ts` ciclos 026–028 | |
| @s17 | `adapter/persistence/ApiCredentialPersistenceTest.java:256`, `:328`, `:407`; `adapter/ApiCredentialApiTest.java:304`; `adapter/ApiCredentialHttpPersistenceTest.java:196` (`s17_s20_…`) | |
| @s18 | `adapter/ApiCredentialSessionIsolationTest.java:27`, `:56`; `adapter/ApiCredentialApiTest.java:266` | |
| @s19 | `adapter/ApiCredentialBearerTest.java:54` `s19_malformedOrInvalidBearerNeverFallsBackToCookie` (6 variantes), `:198` `s19_duplicateAuthorizationIsRejectedBeforeAuthentication`; `adapter/persistence/ApiCredentialAuthenticationTest.java:15`; `application/AuthenticateApiCredentialTest.java:14`; `adapter/ApiCredentialHttpPersistenceTest.java:78`, `:235`, `:288` | renombrado en esta sesión (H3) |
| @s20 | `adapter/ApiCredentialBearerTest.java:237` `s20_bearerAuthenticatesAdmitsThenReadsAsOwnerWithoutCookie`, `:211` `s20_realSessionFilterDoesNotConsultCookieOrCreateSessionForBearer`; `adapter/ApiCredentialSessionIsolationTest.java:83`; `adapter/ApiCredentialHttpPersistenceTest.java:288`, `:196`; `adapter/persistence/ApiCredentialAuthenticationTest.java:56` | renombrado en esta sesión (H3) |
| @s21 | `adapter/persistence/ApiCredentialAuthenticationTest.java:15`; `adapter/persistence/ApiCredentialWiringTest.java:42`; `adapter/ApiCredentialHttpPersistenceTest.java:78` (`s19_s21_…`) | PostgreSQL real |
| @s22 | `adapter/ApiCredentialBearerAdmissionTest.java:76` `s22_eachIndependentScopeAdmitsItsDeclaredOperation` (18 operaciones, cada una con exactamente su scope) **y** `:106` `s22_s23_anotherValidScopeNeverGrantsTheOperation` (11 filas con un scope válido ajeno a la ruta) | **cerrado en esta sesión.** El segundo test es el oráculo del `And no concede otro scope implícitamente` que faltaba (H1) |
| @s23 | `adapter/ApiCredentialBearerAdmissionTest.java:106` `s22_s23_anotherValidScopeNeverGrantsTheOperation`; `adapter/ApiCredentialBearerTest.java:90` `s23_eachOperationRequiresItsOwnScopeBeforeQuota` (18 operaciones, scopes vacíos), `:174` `s23_allowlistDeniesOtherMethodsAndRoutesEvenWithAllScopes` (19 rutas/métodos fuera de la allowlist con los seis scopes y cookie SESSION presente); `adapter/ApiCredentialHttpPersistenceTest.java:127`, `:182` (logout) | **completado en esta sesión.** Cubierto en dos niveles: unitario del filtro y HTTP con contexto Spring |
| @s24 | `adapter/ApiCredentialBearerTest.java:105` `s24_authenticationPrecedesOriginAndOriginPrecedesScope` (401 vs UNTRUSTED_ORIGIN), `:90` (403 sin Origin), `:124` fila `limited` (429) | renombrado en esta sesión (H3) |
| @s25 | `adapter/ApiCredentialBusinessCompatibilityTest.java:57`, `:106` | |
| @s26 | `adapter/persistence/ApiCredentialQuotaTest.java:422`, `:14`; `adapter/persistence/ApiCredentialPersistenceTest.java:380`; `adapter/ApiCredentialHttpPersistenceTest.java:235` | |
| @s27 | `adapter/persistence/ApiCredentialQuotaTest.java:173`; `adapter/ApiCredentialBearerTest.java:124` | carrera real entre dos conexiones |
| @s28 | `adapter/persistence/ApiCredentialQuotaTest.java:371` | |
| @s29 | `adapter/persistence/ApiCredentialQuotaTest.java:97`; `adapter/ApiCredentialBusinessCompatibilityTest.java:106`; `adapter/ApiCredentialBearerTest.java:124` | |
| @s30 | `adapter/persistence/ApiCredentialQuotaTest.java:276`, `:51` | |
| @s31 | `adapter/persistence/ApiCredentialAuthenticationTest.java:15`; `adapter/persistence/ApiCredentialQuotaTest.java:51`; `adapter/ApiCredentialHttpPersistenceTest.java:78` | |
| @s32 | `adapter/IntegrationOpenApiTest.java:27`, `:78`, `:117`, `:162`, `:215`, `:273`, `:323`; `adapter/ApiCredentialBearerTest.java:224` `s32_openApiAuthenticatesWithoutScopesOrQuota`; `adapter/ApiCredentialHttpPersistenceTest.java:235` | |
| @s33 | `frontend/src/integration-api.test.tsx:15`; `e2e/integration-api.spec.mjs:5`; `e2e/integration-api-browser.spec.mjs:8` | unidad + E2E |
| @s34 | `frontend/src/integration-api.test.tsx:69` | |
| @s35 | `frontend/src/integration-api.test.tsx:89`, `:122`, `:199`, `:434` | |
| @s36 | `frontend/src/integration-api.test.tsx:277`, `:344`, `:398`, `:1145` | |
| @s37 | `frontend/src/integration-api.test.tsx:154`, `:726`, `:748`, `:825`, `:867`, `:981`, `:1283`, `:1347`; `e2e/integration-api.spec.mjs:5`; `e2e/integration-api-browser.spec.mjs:8` | unidad + E2E |
| @s38 | `frontend/src/integration-api.test.tsx:227`, `:951` | |
| @s39 | `frontend/src/integration-api.test.tsx:680` | |
| @s40 | `frontend/src/integration-api.test.tsx:627`, `:902`, `:1444`; `e2e/integration-api.spec.mjs:5` | unidad + E2E |
| @s41 | `frontend/src/integration-api.test.tsx:485`, `:519`, `:771`, `:1021`, `:1057`, `:1388`; `e2e/integration-api-browser.spec.mjs:8`, `:272`, `:437`, `:570`, `:658` | unidad + E2E en tres motores |
| @s42 | `adapter/persistence/ApiCredentialCompatibilityTest.java:17`; `e2e/integration-api.spec.mjs:5` | |

Los 42 escenarios tienen al menos un test concreto. Base de partida: la tabla de
la sección 1 de `progress/judge_integration_api.md`, actualizada con los tests
añadidos en esta sesión y con los renombrados de H3.

## Ciclos de esta sesión de cierre

### Ciclo C1 — @s22/@s23: un scope válido distinto no concede la operación (H1)

- **ROJO demostrado con el criterio exacto del juez.** Sustituí a mano en
  `ApiCredentialBearerFilter.java:148-149`
  `!access.scopes().contains(permission.get().scope())` por
  `access.scopes().isEmpty()` y ejecuté el test nuevo:

  ```
  ApiCredentialBearerAdmissionTest > s22_s23_anotherValidScopeNeverGrantsTheOperation …
  29 tests completed, 11 failed
  message="org.opentest4j.AssertionFailedError: expected: <403> but was: <200>"
  ```

  Fallan las 11 filas nuevas y **ninguna** de las 18 antiguas: eso confirma que la
  suite anterior era ciega a esta sustitución, que es justo lo que afirma H1.
  Evidencia: `progress/int24_closure/red_cross_scope_TEST-ApiCredentialBearerAdmissionTest.xml`.
- **VERDE.** Restaurada la condición original con `git checkout --` (el código de
  producción no se tocó: era correcto, faltaba el oráculo). Reejecución:
  `tests="29" skipped="0" failures="0" errors="0"`.
- **REFACTOR.** Extraído el helper `admit(method, path, scopes…)` que ya duplicaban
  los dos tests del archivo, y con él el `record Admission` que devuelve los
  colaboradores. Las constantes `CREDENTIAL_ID` y `TOKEN` sustituyen a los
  literales repetidos. Verde tras el refactor.
- Cobertura del test: 11 filas, con las **dos literales del `.feature`**
  (`GET /api/v1/projects/{id}` con sólo `projects:write`, `PUT /api/v1/projects/{id}`
  con sólo `projects:read`) y al menos una por cada una de las seis familias de
  scope, para que ninguna sustitución trivial de la condición sobreviva. Oráculo:
  403, `application/problem+json`, cuerpo con `API_SCOPE_DENIED`, sin `Set-Cookie`,
  sin autenticación propagada al contexto, `verifyNoInteractions(quota, chain)`.

### Ciclo C2 — @s23: filas de `Examples` sin ninguna ruta representada

Tres filas más en el `@CsvSource` de
`ApiCredentialBearerTest.s23_allowlistDeniesOtherMethodsAndRoutesEvenWithAllScopes`,
sin lógica nueva: `POST /api/v1/projects/{p}/tasks/{t}/blocks` (crear o modificar
bloque), `GET /api/v1/work-sessions/active` (sesión de trabajo) y
`GET /api/v1/me/appearance` (preferencias).

- **ROJO demostrado** añadiendo temporalmente esas tres rutas a `PERMISSIONS`:

  ```
  s23_allowlistDenies… > [17] POST /api/v1/projects/p/tasks/t/blocks FAILED
  s23_allowlistDenies… > [18] GET /api/v1/work-sessions/active FAILED
  s23_allowlistDenies… > [19] GET /api/v1/me/appearance FAILED
  53 tests completed, 3 failed
  message="java.lang.AssertionError: Status expected:<403> but was:<500>"
  ```

  Fallan exactamente las tres filas nuevas y ninguna de las 16 previas.
  Evidencia: `progress/int24_closure/red_allowlist_rows_TEST-ApiCredentialBearerTest.xml`.
- **VERDE.** Revertida la ampliación temporal de `PERMISSIONS`:
  `tests="53" skipped="0" failures="0" errors="0"`.

### Ciclo C3 — H3: prefijos `sNN_` alineados con el `.feature`

Renombrado mecánico, sin tocar oráculos (`ApiCredentialBearerTest`,
`ApiCredentialBearerAdmissionTest`, `ApiCredentialHttpPersistenceTest`):

| Antes | Ahora | Verifica |
| --- | --- | --- |
| `s20_malformedOrInvalidBearerNeverFallsBackToCookie` | `s19_…` | @s19 |
| `s20_duplicateAuthorizationIsRejectedBeforeAuthentication` | `s19_…` | @s19 |
| `s19_bearerAuthenticatesAdmitsThenReadsAsOwnerWithoutCookie` | `s20_…` | @s20 |
| `s21_eachOperationRequiresItsOwnScopeBeforeQuota` | `s23_…` | @s23 |
| `s21_allowlistDeniesOtherMethodsAndRoutesEvenWithAllScopes` | `s23_…` | @s23 |
| `s22_authenticationPrecedesOriginAndOriginPrecedesScope` | `s24_…` | @s24 |
| `s21_eachIndependentScopeAdmitsItsDeclaredOperation` | `s22_…` | @s22 |
| `s21_bearerLogoutIsDenied…` y `s21_defaultFrameworkLogout…` | `s23_…` | @s23 |
| `s20_s31_realInvalidRevokedOrDisabled…` | `s19_s21_…` | @s19 y @s21 |

`s25_s27_admissionFailuresStopBeforeBusiness` se deja como está: H3 no lo señala
y su nombre ya declara los dos escenarios que ejercita.

### Ciclo C4 — A8: cabeceras de seguridad en la propia cadena

- **ROJO.** `adapter/SecurityHeadersTest.java`, 3 tests, fallaban 3/3 con
  `Response header 'Content-Security-Policy' expected:<default-src 'self'; …>`.
- **VERDE.** `SecurityConfiguration` gana un `headers(…)` compartido por la cadena
  Bearer y la de sesión, con la misma política que `deploy/nginx.conf:13-15`.
- Cubre la ruta admitida, el rechazo 401 y la cadena de sesión.

## Hallazgos de seguridad: estado

Origen: `progress/security_review_connectors.md` (en `main`).

| # | Estado | Detalle |
| --- | --- | --- |
| A8 | **hecho** | Ciclo C4. CSP y `Referrer-Policy` ya no dependen del ingress. |
| A9 | **hecho** | `.env.example` ponía `APP_PUBLIC_ORIGIN=` vacío con el comentario «Leave empty for local HTTP», pero `SessionCookiePolicy.create("")` lanza `IllegalArgumentException` porque `URI.create("").getHost()` es nulo. Ahora vale `http://127.0.0.1:8080` y el comentario dice que es obligatorio. |
| A1 | **bloqueado por el contrato — decisión del coordinador** | Ver abajo. |
| A2 | **bloqueado por el contrato — decisión del coordinador** | Ver abajo. |
| A3 | fuera de mi encargo | `deploy/nginx.conf`, lo lleva el coordinador. |

### A2 — mover el consumo de cuota fuera del `if (!openApi)`: **no lo he hecho**

El encargo pedía moverlo «con test rojo primero». **No lo he implementado porque
contradice literalmente el escenario aprobado @s32**, que dice:

```gherkin
  @s32
  Scenario: OpenAPI es una excepción de lectura exacta y privada
    Given una sesión propia o un Bearer válido sin cuota restante
    When solicita GET /api/v1/integration-openapi.json
    Then recibe el documento OpenAPI 3.1 validado de la allowlist y seis scopes
    …
    And no consume cuota ni ejecuta negocio; Bearer no consulta sesión ni emite Set-Cookie
```

El `Given` fija una credencial **sin cuota restante** y el `Then` exige que la
petición **no consuma cuota**. Consumirla rompería ambas cláusulas y volvería
inalcanzable el documento OpenAPI justo cuando la cuota está agotada. Hay tres
tests que lo fijan hoy: `ApiCredentialBearerTest.java:224`,
`ApiCredentialHttpPersistenceTest.java:235` y la batería de
`IntegrationOpenApiTest`.

Mi regla de artesano es no desviarme del `.feature` aprobado: **paro y pido cambio
de contrato**. La exposición real es acotada (lectura de un recurso estático, ya
autenticada, sin acceso a datos del propietario), así que la vía recomendada es
un contador barato separado del cupo de negocio —o el `limit_req` por IP de A3—,
que endurece la ruta sin tocar @s32. Si el coordinador prefiere consumir cuota,
hay que reescribir @s32 y pasar por la puerta de aprobación humana antes de
tocar el filtro.

### A1 — consumir cuota antes de la comprobación de scope: **no lo he hecho**

Igual que A2, y por instrucción explícita del coordinador, lo dejo señalado. Mi
lectura del contrato dice que **no** se puede consumir cuota en el rechazo por
scope, con dos citas:

```gherkin
  @s23
  Scenario Outline: Ni un prefijo compartido ni una cookie amplían los permisos
    …
    Then recibe 403 API_SCOPE_DENIED sin CSRF_INVALID, negocio ni consumo de cuota
```

y la última fila de @s24, que exige `403 API_SCOPE_DENIED` con «token válido sin
scope y **cuota agotada**»: si la cuota se consumiera (o se comprobara) antes del
scope, esa fila devolvería `429 API_RATE_LIMITED` y el escenario se rompe.

Recomendación: **no tocar el orden**. Un contador de rechazos separado del cupo de
negocio, o `limit_req` por IP en nginx (A3), cubre el abuso sin contradecir @s23
ni @s24. Cualquier alternativa exige cambiar el contrato primero.

## Mutación

Las tres campañas siguen siendo la medición vigente. Cifras **recalculadas por el
juez desde los XML/JSON originales**, no leídas de los informes
(`progress/judge_integration_api.md` sección 4):

| Campaña | Informe | Killed/Total | % estricto | Umbral |
| --- | --- | ---: | ---: | ---: |
| Núcleo (`integration_api-backend`) | `progress/mutation_integration_api_backend_final.md` | **180/188** | 95,7447 % | 80 % |
| HTTP (`integration_api-http-backend`) | `progress/mutation_integration_api_http.md` | **114/118** | 96,6102 % | 80 % |
| Frontend (`integration_api-frontend`) | `progress/mutation_integration_api_frontend_final.md` | **836/997** | 83,8516 % | 80 % |

`NO_COVERAGE`/`NoCoverage` permanecen en el denominador y el `RuntimeError` de
Stryker se conserva como error: sin reclasificaciones.

No se ha reejecutado ninguna campaña en esta sesión, y el juez indica que no hace
falta (punto 6 de sus cambios requeridos): los ciclos C1, C2 y C3 **sólo añaden y
renombran tests**, sin tocar fuentes de producto, así que sobre el mismo universo
de mutantes sólo pueden aumentar las muertes. El ciclo C4 **sí** añade producción
(`SecurityConfiguration`), fuera del universo medido por las tres campañas, y
queda a criterio del `mutation_tester` si procede ampliarlo.

Corregida además la ruta de los originales en
`progress/mutation_integration_api_frontend_final.md`: apuntaba al directorio
inexistente `progress/integration24_stryker_corrective/` y ahora apunta a
`progress/integration24_stryker_corrective_evidence.zip` (punto 5 del juez).

## Límites de esta sesión

Lo que **no** queda acreditado, y nadie debe dar por hecho:

1. **No hay despliegue productivo.** Ninguna imagen de la 24 está desplegada.
2. **No hay aceptación HTTPS live.** La API no se ha ejercitado sobre el dominio
   real con TLS.
3. **No se ejecutó ninguna campaña de mutación** en esta sesión (ver arriba).
4. **A1, A2 y A3 siguen abiertos.** A1 y A2 requieren decisión de contrato; A3 es
   del coordinador.
5. La evidencia UX previa sigue siendo automatizada y heurística
   (`progress/ux_integration_api.md`), y la evidencia axe es anterior a `01f80ab`.
6. **No marco `done` en `feature_list.json`**: corresponde al `judge` y al
   `mutation_tester` tras este dictamen.
