# Review — feature 24 `integration_api`

**Veredicto:** REJECTED

Juez de cierre, 9 de septiembre de 2026. Checkout `main` en `f8570d7`, árbol de
trabajo limpio (`git status --porcelain` = 0 entradas). Sesión anterior cortada
por cuota: no había notas previas en este archivo, la revisión se rehízo entera.

El motivo del rechazo es acotado y barato de resolver: **la cláusula «no concede
otro scope implícitamente» de @s22/@s23 no tiene ningún test que la verifique**,
y no existe el mapa `@s → test` que `docs/verification.md` (Nivel 4) y
`CHECKPOINTS.md` (C6) exigen en `progress/tdd_<name>.md`. Todo lo demás —las tres
campañas de mutación, la CI, la corrección de `.brand`, el estado de `main`— está
verificado y en verde. Ver «Cambios requeridos».

---

## 1. Cobertura de escenarios (@s ↔ test)

Todos los archivos citados se comprobaron en el árbol real: existen en
`backend/src/test`, `frontend/src` y `e2e`. Ninguna cita es a un archivo
inexistente. Las rutas van sin el prefijo
`backend/src/test/java/com/apptolast/organization/`.

| @s | Test concreto (archivo:línea) | Estado |
| --- | --- | --- |
| @s1 | `application/CreateApiCredentialTest.java:167`, `:128`; `adapter/persistence/ApiCredentialPersistenceTest.java:586`; `adapter/ApiCredentialApiTest.java:488`; `adapter/persistence/ApiCredentialWiringTest.java:112` | [x] |
| @s2 | `application/CreateApiCredentialTest.java:19`, `:113`; `adapter/ApiCredentialApiTest.java:361`, `:378`, `:131`; `frontend/src/integration-api.test.tsx` ciclo 068 | [x] |
| @s3 | `application/CreateApiCredentialTest.java:146`; `adapter/persistence/ApiCredentialPersistenceTest.java:19` | [x] |
| @s4 | `adapter/ApiCredentialApiTest.java:394`, `:426`, `:441`, `:281` | [x] |
| @s5 | `application/CreateApiCredentialTest.java:79`, `:167` | [x] |
| @s6 | `application/CreateApiCredentialTest.java:52` | [x] |
| @s7 | `adapter/persistence/ApiCredentialPersistenceTest.java:137` | [x] |
| @s8 | `adapter/persistence/ApiCredentialPersistenceTest.java:175` | [x] |
| @s9 | `adapter/persistence/ApiCredentialPersistenceTest.java:49`; `adapter/ApiCredentialApiTest.java:459` | [x] |
| @s10 | `adapter/persistence/ApiCredentialPersistenceTest.java:106` | [x] |
| @s11 | `adapter/persistence/ApiCredentialPersistenceTest.java:175` | [x] |
| @s12 | `adapter/persistence/ApiCredentialPersistenceTest.java:561`; `adapter/ApiCredentialApiTest.java:224`, `:237`; `adapter/persistence/ApiCredentialWiringTest.java:100` | [x] |
| @s13 | `adapter/persistence/ApiCredentialPersistenceTest.java:528`, `:476`; `adapter/ApiCredentialApiTest.java:193`; `frontend/src/integration-api.test.tsx:485` | [x] |
| @s14 | `adapter/ApiCredentialApiTest.java:153`, `:337`, `:94`, `:113`; `adapter/persistence/ApiCredentialPersistenceTest.java:309` | [x] |
| @s15 | `adapter/persistence/ApiCredentialPersistenceTest.java:488`; `adapter/ApiCredentialApiTest.java:167` | [x] |
| @s16 | `adapter/persistence/ApiCredentialPersistenceTest.java:488`; `adapter/ApiCredentialApiTest.java:56`; `frontend/src/integration-api-client.test.ts` ciclos 026–028 | [x] |
| @s17 | `adapter/persistence/ApiCredentialPersistenceTest.java:256`, `:328`, `:407`; `adapter/ApiCredentialApiTest.java:304` | [x] |
| @s18 | `adapter/ApiCredentialSessionIsolationTest.java:27`, `:56`; `adapter/ApiCredentialApiTest.java:266` | [x] |
| @s19 | `adapter/ApiCredentialBearerTest.java:54` (6 variantes), `:195` (doble header); `adapter/persistence/ApiCredentialAuthenticationTest.java:15` (caducado/revocado/desconocido); `application/AuthenticateApiCredentialTest.java:14` | [x] |
| @s20 | `adapter/ApiCredentialBearerTest.java:234`, `:208`; `adapter/ApiCredentialSessionIsolationTest.java:83`; `adapter/ApiCredentialHttpPersistenceTest.java:288`; `adapter/persistence/ApiCredentialAuthenticationTest.java:56` | [x] |
| @s21 | `adapter/persistence/ApiCredentialAuthenticationTest.java:15`; `adapter/persistence/ApiCredentialWiringTest.java:42`; `adapter/ApiCredentialHttpPersistenceTest.java:78` | [x] |
| @s22 | `adapter/ApiCredentialBearerAdmissionTest.java:41` (las 18 operaciones, un scope propio cada una) | **[~] parcial** — el `And no concede otro scope implícitamente` no tiene oráculo |
| @s23 | `adapter/ApiCredentialBearerTest.java:171` (16 rutas/métodos fuera de la allowlist con los seis scopes y cookie SESSION presente), `:90` (18 operaciones con scopes vacíos); `adapter/ApiCredentialHttpPersistenceTest.java:127`, `:182` (logout) | **[ ] filas sin cubrir** — ver H1 |
| @s24 | `adapter/ApiCredentialBearerTest.java:105` (401 vs UNTRUSTED_ORIGIN), `:90` (403 sin Origin), `:124` fila `limited` (429) | [x] |
| @s25 | `adapter/ApiCredentialBusinessCompatibilityTest.java:57`, `:106` | [x] |
| @s26 | `adapter/persistence/ApiCredentialQuotaTest.java:422`, `:14`; `adapter/persistence/ApiCredentialPersistenceTest.java:380` | [x] |
| @s27 | `adapter/persistence/ApiCredentialQuotaTest.java:173` | [x] |
| @s28 | `adapter/persistence/ApiCredentialQuotaTest.java:371` | [x] |
| @s29 | `adapter/persistence/ApiCredentialQuotaTest.java:97`; `adapter/ApiCredentialBusinessCompatibilityTest.java:106`; `adapter/ApiCredentialBearerTest.java:124` | [x] |
| @s30 | `adapter/persistence/ApiCredentialQuotaTest.java:276`, `:51` | [x] |
| @s31 | `adapter/persistence/ApiCredentialAuthenticationTest.java:15`; `adapter/persistence/ApiCredentialQuotaTest.java:51` | [x] |
| @s32 | `adapter/IntegrationOpenApiTest.java:27`, `:78`, `:117`, `:162`, `:215`, `:273`, `:323`; `adapter/ApiCredentialBearerTest.java:221`; `adapter/ApiCredentialHttpPersistenceTest.java:235` | [x] |
| @s33 | `frontend/src/integration-api.test.tsx:15`; `e2e/integration-api.spec.mjs:5`; `e2e/integration-api-browser.spec.mjs:8` | [x] |
| @s34 | `frontend/src/integration-api.test.tsx:69` | [x] |
| @s35 | `frontend/src/integration-api.test.tsx:89`, `:122`, `:199`, `:434` | [x] |
| @s36 | `frontend/src/integration-api.test.tsx:277`, `:344`, `:398`, `:1145` | [x] |
| @s37 | `frontend/src/integration-api.test.tsx:154`, `:726`, `:748`, `:825`, `:867`, `:981`, `:1283`, `:1347`; `e2e/integration-api.spec.mjs:5`; `e2e/integration-api-browser.spec.mjs:8` | [x] |
| @s38 | `frontend/src/integration-api.test.tsx:227`, `:951` | [x] |
| @s39 | `frontend/src/integration-api.test.tsx:680` | [x] |
| @s40 | `frontend/src/integration-api.test.tsx:627`, `:902`, `:1444`; `e2e/integration-api.spec.mjs:5` | [x] |
| @s41 | `frontend/src/integration-api.test.tsx:485`, `:519`, `:771`, `:1021`, `:1057`, `:1388`; `e2e/integration-api-browser.spec.mjs:8`, `:272`, `:437`, `:570`, `:658` | [x] |
| @s42 | `adapter/persistence/ApiCredentialCompatibilityTest.java:17`; `e2e/integration-api.spec.mjs:5` | [x] |

### H1 — Hallazgo bloqueante: la no-concesión implícita de scopes no tiene oráculo

`ApiCredentialBearerFilter.java:146-152` decide el permiso así:

```java
var permission = PERMISSIONS.stream().filter(rule -> rule.request().matches(request)).findFirst();
if (!openApi && (permission.isEmpty() || !access.scopes().contains(permission.get().scope()))) {
  problem(response, 403, "API_SCOPE_DENIED", "La credencial no permite esta operación.");
```

La suite ejercita solo dos configuraciones de scopes:

- `ApiCredentialBearerTest.java:90-93`: las 18 operaciones con `List.of()` (lista vacía).
- `ApiCredentialBearerTest.java:171-184`: 16 rutas/métodos fuera de la allowlist con los **seis** scopes.
- `ApiCredentialBearerAdmissionTest.java:41-56`: cada operación con **exactamente su propio** scope, y admite.

Nunca se pide una operación con un scope **distinto pero no vacío**. Consecuencia
demostrable: si la condición fuera `access.scopes().isEmpty()` en lugar de
`!access.scopes().contains(permission.get().scope())` —es decir, «cualquier
credencial con algún permiso puede todo lo que está en la allowlist»—, los tres
tests anteriores y el resto de la suite HTTP **seguirían verdes**. Es exactamente
el defecto de seguridad que @s23 pretende excluir. Buscar `API_SCOPE_DENIED` en
`backend/src/test` devuelve solo dos aserciones
(`ApiCredentialBearerTest.java:98` y `ApiCredentialHttpPersistenceTest.java:157`),
y ninguna es cruzada.

Las campañas de mutación no cubren este hueco: los mutadores DEFAULT de PIT no
sustituyen los literales `String` de `PERMISSIONS`, así que el 114/118 de HTTP no
acredita esta cláusula.

Filas concretas de los `Examples` de @s23 sin ningún test:

1. `GET de proyecto con sólo projects:write` — no existe.
2. `PUT de proyecto con sólo projects:read` — no existe.
3. `crear o modificar bloque` — la matriz de denegación de
   `ApiCredentialBearerTest.java:152-170` no incluye `POST`/`PUT` sobre
   `/api/v1/projects/{p}/tasks/{t}/blocks` ni `.../blocks/changes`.
4. `consultar sesión de trabajo fuera de la allowlist` — la matriz cubre
   `/api/session` (sesión HTTP), no `/api/v1/work-sessions/active`,
   `/api/v1/work-sessions/{id}` ni `/api/v1/work-session-changes/...`, que son
   las rutas de «sesión de trabajo» del producto.
5. `consultar preferencias` — cubre `/api/v1/me/export` y
   `/api/v1/me/import/preview`, pero no `/api/v1/me/appearance`,
   `/api/v1/me/customization/{scope}` ni `/api/v1/me/availability`.

Cubiertas sí están: `HEAD u OPTIONS`, `transición de estado de proyecto o tarea`,
`gestionar credenciales o cerrar sesión` y `ruta desconocida con prefijo parecido`
(incluidas las dos variantes de segmento vacío `/api/v1/projects/` y
`/api/v1/projects/p/tasks/`, que fueron un RED real del ciclo 36).

### H2 — No existe el mapa `@s → test` exigido por el arnés

`docs/verification.md` Nivel 4 y `CHECKPOINTS.md` C6 exigen el mapa en
`progress/tdd_<name>.md`. La cadena «Trazabilidad» aparece en 20 features
anteriores (`tdd_create_project_backend.md`, `tdd_import_data.md`,
`tdd_read_projects_backend.md`…) pero en **ninguno** de los siete archivos de la
24 (`tdd_integration_api_{backend,http,http_wiring,openapi,frontend,dispatcher,text200}.md`).
Esos archivos son bitácoras por ciclo, valiosas, pero no permiten comprobar la
cobertura de los 42 escenarios sin reconstruirla a mano, como he tenido que hacer
aquí. La tabla de la sección 1 puede reutilizarse como base.

### H3 — La numeración de los tests HTTP no corresponde a la del `.feature`

En `ApiCredentialBearerTest.java` los nombres van desplazados respecto a
`features/integration_api.feature`, lo que hace que el mapa parezca cubierto
donde no lo está:

- `:54 s20_malformedOrInvalidBearerNeverFallsBackToCookie` verifica **@s19**.
- `:234 s19_bearerAuthenticatesAdmitsThenReadsAsOwnerWithoutCookie` verifica **@s20**.
- `:90 s21_eachOperationRequiresItsOwnScopeBeforeQuota` verifica **@s23**.
- `:171 s21_allowlistDeniesOtherMethodsAndRoutesEvenWithAllScopes` verifica **@s23**.
- `:105 s22_authenticationPrecedesOriginAndOriginPrecedesScope` verifica **@s24**.
- `ApiCredentialBearerAdmissionTest.java:41 s21_eachIndependentScopeAdmits...` verifica **@s22**.
- `ApiCredentialHttpPersistenceTest.java:127`, `:182` van etiquetados `s21_` y
  verifican **@s23**; `:78 s20_s31_...` verifica además **@s19** y **@s21**.

No es un fallo de comportamiento —todos esos oráculos son correctos y valiosos—,
pero es la razón por la que H1 pasó desapercibido: los nombres `s21_`/`s22_`
sugieren cobertura de @s21/@s22 cuando lo que ejercitan es otra cosa.

## 2. Disciplina TDD

- **¿Producción sin test que la pida?** NO. Se revisó el único cambio de producto
  posterior a la campaña de mutación: `01f80ab` toca exclusivamente
  `frontend/src/styles.scss` (+`progress/tdd_integration_api_text200.md`), y lo
  pide un E2E preexistente,
  `e2e/integration-api-browser.spec.mjs:272 "…text 200 percent and existing media modes @s41"`,
  que estaba rojo en CI 34253701367. `f8570d7` es solo documentación
  (`project-spec.md`, +51 líneas).
- **¿Evidencia de Rojo→Verde→Refactor?** SÍ, y con una honestidad poco común. Los
  siete `progress/tdd_integration_api_*.md` registran 41 ciclos de núcleo, 48 de
  HTTP, los de OpenAPI/wiring y 69 de frontend, con hash de log RED y GREEN por
  ciclo y originales fuera del checkout. Se declara explícitamente cuándo un caso
  fue **inicialmente GREEN** en vez de fabricar un rojo (p. ej.
  `tdd_integration_api_backend.md:57` ciclo 22, `:75` ciclo 28, `:99` ciclos 40–41;
  `tdd_integration_api_frontend.md:51` ciclo 032, `:76` ciclo 049;
  `review_integration24_refinements.md` los 15 refuerzos). Eso es lo correcto y lo
  doy por bueno.
- Los desvíos de proceso están declarados y son defendibles: el fixture con emoji
  recodificado del ciclo 4 (`tdd_integration_api_backend.md:29`) se conserva como
  fallo de entorno, no como RED de producto; el `060_failed_edit.log` y el
  `cycle27_adjustment` idem.

## 3. Calidad (lente de artesano)

- `ApiCredentialBearerFilter.java:27-102` — la tabla `PERMISSIONS` es declarativa,
  sin comodines ni permisos por prefijo, y usa `PathPatternRequestMatcher` con
  variables obligatorias tras el RED del ciclo 36 (dos rutas con segmento vacío
  que Ant admitía). Buen diseño. `doFilterInternal` tiene un solo motivo para
  cambiar por rama y el orden autenticación → Origin → allowlist/scope → cuota es
  legible y está aserido con `inOrder` en
  `ApiCredentialBearerAdmissionTest.java:65-68`.
- `ApiCredentialBearerFilter.java:149` — es la línea con el hueco de oráculo de
  H1. La implementación actual es correcta (`contains` exacto sobre la lista);
  lo que falta es la prueba que la fije.
- `ApiCredentialBearerFilter.java:173-179` `problem(...)` centraliza el contrato de
  error: `Cache-Control: no-store`, `application/problem+json` y cuerpo sin
  detalle interno. `ApiCredentialBearerTest.java:141-145` verifica que el mensaje
  privado `test-private-database-detail` no aparece en la respuesta. Correcto.
- Contrato de errores por canal: `STORAGE_UNAVAILABLE` (503) nunca se confunde con
  `SESSION_UNAVAILABLE`, acreditado contra PostgreSQL real retirando la tabla
  `api_credentials` en `ApiCredentialHttpPersistenceTest.java:196`. Es la clase de
  aislamiento real que pide `docs/verification.md`, no un mock.
- Arquitectura (`docs/architecture.md`): la separación se respeta. `application/`
  no importa nada de `adapter/`; el emisor del secreto entra como proveedor
  diferido y `CreateApiCredentialTest.java:167` comprueba el resultado del puerto,
  no una transacción. `PostgresApiCredentialStore` es el único que habla SQL.
- Sin números mágicos sueltos: 4096/4097 bytes, 80 puntos de código, 7/30/90 días,
  60/120 solicitudes y el cupo de 10 aparecen como fronteras nombradas en tests
  con su escenario, no repartidos por el código.
- Nit no bloqueante: `progress/mutation_integration_api_frontend_final.md` cita
  los originales como `progress/integration24_stryker_corrective/{...}`, un
  directorio que **no existe**; los once archivos están dentro de
  `progress/integration24_stryker_corrective_evidence.zip` (verificado: contiene
  esos 11 más `manifest.json`). Corregir la ruta del informe.

## 4. Mutación (verificada recalculando desde los artefactos originales)

No me limité a leer los porcentajes: los recalculé desde el XML/JSON crudo de
cada ZIP.

| Campaña | Informe | Killed/Total recalculado | % estricto | Umbral |
| --- | --- | ---: | ---: | ---: |
| Núcleo (`integration_api-backend`) | `progress/mutation_integration_api_backend_final.md` | 180/188 | 95,7447 % | 80 % |
| HTTP (`integration_api-http-backend`) | `progress/mutation_integration_api_http.md` | 114/118 | 96,6102 % | 80 % |
| Frontend (`integration_api-frontend`) | `progress/mutation_integration_api_frontend_final.md` | 836/997 | 83,8516 % | 80 % |

- Fuentes del recálculo:
  `integration24_core_pit_originals.zip → integration24-core-pit-report-original/mutations.xml`
  (`KILLED 180, SURVIVED 7, NO_COVERAGE 1`);
  `integration24_http_pit_portable.zip → integration_http_pit_original.xml`
  (`KILLED 114, SURVIVED 3, NO_COVERAGE 1`);
  `integration24_stryker_corrective_evidence.zip → mutation.json`
  (`Killed 836, Survived 158, NoCoverage 2, RuntimeError 1`). Los tres coinciden
  exactamente con lo declarado en los informes.
- **Sin reclasificación**: `NO_COVERAGE`/`NoCoverage` permanecen en el denominador
  y el `RuntimeError 712` de Stryker se conserva como error, no como timeout ni
  como muerte inferida; el informe distingue explícitamente el 83,85 % estricto
  del 83,94 % del motor. `TIMED_OUT`/`Timeout` son cero en las tres campañas: no
  hay nada que reclasificar.
- **Originales referenciados y presentes**: los tres ZIP existen en `progress/`
  con sus manifiestos (`integration24_core_pit_portable_manifest.json`,
  `integration24_http_pit_portable_manifest.json`,
  `integration24_stryker_corrective_freeze.json`). El original frontend fallido
  (780/997 = 78,2347 %, EXIT 1) se conserva íntegro en
  `progress/mutation_integration_api_frontend.md`, que es la práctica correcta.
- Residuos justificados uno a uno, sin declararlos equivalentes en bloque: cuatro
  en `ApiCredentialController` (`revoke:37`, `find:51`, `list:66`,
  `lambda$acceptable$1:90`) reconocidos como huecos de oráculo de negociación de
  contenido, no como equivalencias demostradas. Honesto.

## 5. Checkpoints

| # | Checkpoint | Estado | Evidencia |
| --- | --- | :---: | --- |
| C1 | El arnés está completo | **[x]** | Los 10 ficheros base y docs presentes (`AGENTS.md`, `CLAUDE.md`, `CHECKPOINTS.md`, `harness.config.json`, `feature_list.json`, `progress/current.md`, `docs/{workflow,architecture,conventions,verification}.md`). `bin/harness init` **no se ejecutó en esta sesión** por la restricción de máquina; se apoya en CI 34271043131 SUCCESS sobre `f8570d7` (dato del coordinador) y en `node --test scripts/project.test.mjs` = **72/72 pass, 0 fail**, ejecutado ahora. |
| C2 | El estado es coherente | **[x]** | `feature_list.json`: única feature `in_progress` = 24. Las 25–30 están en `spec_ready`, coherente con la instrucción del usuario del 8 de septiembre de trabajarlas en worktrees fuera de este checkout. `progress/current.md` describe la sesión activa y el corte por cuota, sin basura previa. |
| C3 | El código respeta la arquitectura | **[x]** | Capas respetadas (`application/` sin dependencias de `adapter/`); allowlist declarativa sin comodines; sin dependencias runtime nuevas (OpenAPI es recurso estático, `tdd_integration_api_openapi.md`); sin logs de depuración ni TODO sin contexto en las fuentes de 24. |
| C4 | La verificación es real | **[x]** | Fixtures PostgreSQL reales (`postgres:17.9-alpine`, singleton por JVM, limpieza por propietario), no mocks de infraestructura; carreras que **exigen observar bloqueo real** antes de liberar la transacción (`ApiCredentialPersistenceTest.java:175`, `ApiCredentialQuotaTest.java:173`, `:276`); E2E en tres motores. Suite completa no reejecutada aquí a propósito. |
| C5 | La sesión se cerró bien | **[x]** | `git status --porcelain` = 0 entradas. `progress/history.md` tiene entrada de la última sesión. Feature 24 sigue en `in_progress`, que es su estado correcto mientras este dictamen no sea favorable. |
| C6 | Contrato Gherkin (BDD) | **[ ]** | Sección «## 24. API para integraciones» presente en `project-spec.md:1942-1992` (restaurada por `f8570d7`, 51 líneas) y `features/integration_api.feature` con 42 escenarios tagueados y `Then` medibles: esos dos boxes **sí**. Falla el tercero: @s22/@s23 sin oráculo de scope cruzado (**H1**) y sin el mapa `@s → test` (**H2**). El cuarto box (no hay producción sin test) **sí** se cumple. |
| C7 | Prueba de mutación | **[x]** | Tres campañas ≥ 80 % estricto, recalculadas desde los originales (sección 4). Supervivientes documentados individualmente. Nota de proceso: C7 es puerta del `mutation_tester`, ya emitida; la registro como verificada, no como aprobada por mí. |

## 6. Corrección `.brand` de `01f80ab` — no altera contrato ni contraste

Verificado leyendo el diff completo y el estado actual de
`frontend/src/styles.scss:457-482`:

```
-  white-space: nowrap;
+  min-width: 0;
+  overflow-wrap: anywhere;      (.brand)
+  display: inline-block;        (.brand-light)
```

- **Contrato**: no toca ningún archivo de `backend/`, ningún DTO, ninguna ruta,
  ningún nombre accesible ni el DOM. El diff completo del commit son dos archivos:
  `frontend/src/styles.scss` (4 líneas) y `progress/tdd_integration_api_text200.md`.
  Los contratos HTTP y el documento OpenAPI quedan intactos por construcción.
- **Contraste**: las tres declaraciones son de *layout* (`min-width`,
  `overflow-wrap`, `display`). No hay cambio de `color`, `background`,
  `font-size`, `font-weight` ni de ningún token de tema, así que las ratios de
  contraste calculadas no pueden variar. El texto se parte, no se recorta ni se
  oculta: `overflow-wrap: anywhere` frente a `nowrap` **aumenta** el contenido
  visible. La captura `light-text200-320.png` fue inspeccionada y así se recoge en
  `progress/ux_integration_api.md`.
- Límite declarado por el propio autor y que suscribo
  (`tdd_integration_api_text200.md`, sección «Límites»): la evidencia axe de
  `integration24_browser_modalities.log` es **anterior** a `01f80ab`; no se
  reejecutó axe después del cambio. Dado que ninguna propiedad afecta al color, el
  riesgo de regresión de contraste es nulo, pero la evidencia formal post-cambio
  no existe. Además queda un desbordamiento residual de 1 px en la pantalla de
  acceso `/` a 320 px con texto al 200 %, presente **antes y después** del cambio,
  ajeno a `.brand` y fuera del alcance de @s41 (ruta privada).

## 7. Límites explícitos de este dictamen

Lo que **no** cubre esta revisión, y que ningún lector debe dar por acreditado:

1. **No ejecuté `bin/harness init` ni la suite completa** (Java, Vitest o E2E):
   seis agentes están usando la máquina. C1/C4 se apoyan en la CI 34271043131
   SUCCESS sobre `f8570d7` reportada por el coordinador y en el único comando que
   sí corrí, `node --test scripts/project.test.mjs` (72/72).
2. **No ejecuté ninguna campaña de mutación.** Verifiqué los resultados
   recalculando desde los XML/JSON originales conservados, no reproduciéndolos.
3. **No hay despliegue productivo.** Ninguna imagen de la 24 está desplegada;
   `progress/history.md` lo confirma («Ninguna imagen24 se ha desplegado todavía»).
4. **No hay aceptación HTTPS live.** No se ha ejercitado la API sobre el dominio
   real con TLS. El ensayo de rollback entre imágenes y la restauración V21
   (17 tablas) constan como PASS en `review_integration_api_checkpoints.md:95`,
   pero no acreditan recuperación Swarm completa ni aceptación en producción.
5. **La evidencia UX es automatizada y heurística**, no un estudio con usuarios;
   `progress/ux_integration_api.md` lo declara y no lo contradigo. `color-contrast`
   de axe se omite solo bajo colores nativos forzados, límite ya declarado.
6. **No juzgo las features 25–30.** Se trabajan en paralelo en worktrees fuera de
   este checkout por instrucción explícita del usuario del 8 de septiembre; en
   `main` solo la 24 está `in_progress`, que es lo que exige C2.
7. **No edité código, tests ni `feature_list.json`.** Este archivo es el único
   producto de la revisión.

## 8. Cambios requeridos

Bloqueantes (los tres son el motivo del rechazo):

1. **Cerrar el oráculo de scope cruzado de @s22/@s23.** Añadir a
   `ApiCredentialBearerTest.java` (o a `ApiCredentialBearerAdmissionTest.java`, que
   es más barato) un test parametrizado que, con una credencial que tiene **un
   scope válido pero distinto del requerido**, pida la operación y verifique
   `403 API_SCOPE_DENIED`, sin `Set-Cookie`, sin interacción con `quota` ni con el
   caso de uso de negocio. Como mínimo las dos filas literales del `.feature`:
   `GET /api/v1/projects/{id}` con solo `projects:write`, y
   `PUT /api/v1/projects/{id}` con solo `projects:read`. Criterio de aceptación
   del arreglo: sustituir a mano
   `!access.scopes().contains(permission.get().scope())` por
   `access.scopes().isEmpty()` en `ApiCredentialBearerFilter.java:149` debe poner
   la suite **en rojo**; hoy la deja verde.
2. **Completar las filas de `Examples` de @s23 que no tienen ninguna ruta
   representada** en la matriz de `ApiCredentialBearerTest.java:152-170`: una
   escritura de bloque (`POST /api/v1/projects/{p}/tasks/{t}/blocks`), una
   consulta de sesión de trabajo (`GET /api/v1/work-sessions/active`) y una de
   preferencias (`GET /api/v1/me/appearance`). Son tres filas más en el
   `@CsvSource` existente, sin lógica nueva.
3. **Escribir el mapa `@s1–@s42 → test` exigido por `docs/verification.md`
   Nivel 4 y C6.** Ubicarlo en un `progress/tdd_integration_api.md` (o en una
   sección «## Trazabilidad» de `tdd_integration_api_backend.md`). La tabla de la
   sección 1 de este documento sirve de punto de partida; hay que actualizarla con
   los tests de los puntos 1 y 2.

No bloqueantes, recomendados antes de cerrar:

4. Renombrar los métodos desalineados de `ApiCredentialBearerTest.java`,
   `ApiCredentialBearerAdmissionTest.java` y `ApiCredentialHttpPersistenceTest.java`
   para que el prefijo `sNN_` coincida con el tag del `.feature` (detalle en H3).
   Es un rename mecánico, sin cambio de oráculos, y evita que el próximo revisor
   repita mi reconstrucción manual.
5. Corregir en `progress/mutation_integration_api_frontend_final.md` la ruta de los
   originales: apuntar a `progress/integration24_stryker_corrective_evidence.zip`
   en vez de al directorio `progress/integration24_stryker_corrective/`, que no
   existe.
6. Tras el punto 1, no hace falta repetir la campaña de mutación completa: los
   tests nuevos solo pueden aumentar las muertes sobre el mismo universo. Basta
   dejar constancia de que el 836/997, el 180/188 y el 114/118 siguen siendo la
   medición vigente y que los tests añadidos no cambian fuentes de producto.

Una vez hechos 1, 2 y 3, la feature queda lista para un nuevo dictamen: todo lo
demás que exige el arnés está verificado y en verde.
