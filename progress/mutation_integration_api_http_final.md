# Mutación — feature 24 `integration_api`, campaña HTTP (reejecución de cierre)

**Veredicto:** PASS
**Score:** killed/total = 114/118 = **96,610169 %** (umbral: 80 %, `harness.config.json` → `mutation.threshold: 0.8`)

Reejecución exigida por `progress/judge_integration_api_final.md` §4 y §8 condición 1:
`SecurityConfiguration.java` cambió (ciclo C4, cabeceras CSP y `Referrer-Policy`) y está
en el universo de esta campaña, así que el 114/118 anterior ya no describía las fuentes.
Ahora sí las describe. **Campaña única**: no he repetido la de núcleo (180/188) ni la de
frontend (836/997), cuyas fuentes no cambiaron.

## 1. Comando exacto y salida relevante

```
node scripts/project.mjs mutate integration_api-http-backend
```

Que despacha (`scripts/project.mjs:153-155`) a
`backend/gradlew.bat pitest --no-daemon -PmutationScope=integration_api_http`.

Checkout: `5c84a87687961b1e1c199a22e56453f1ed2f9a88` (`main`). El juez firmó sobre
`926fb6a`; entre `926fb6a` y `5c84a87` solo hay `docs:` (ver §7, límite 2).

Ventana: 2026-09-09 04:38:56 → 04:54:49 (hora local de la máquina). Cobertura 563 s,
mutación 952 s. `BUILD SUCCESSFUL in 16m 3s`, **EXIT=0**.

Salida de PIT, literal (`progress/int24_http_remeasure/pit_http_run.log:93-99`):

```
- Statistics
>> Line Coverage (for mutated classes only): 311/315 (99%)
>> 543 tests examined
>> Generated 118 mutations Killed 114 (97%)
>> Mutations with no coverage 1. Test strength 97%
>> Ran 475 tests (4.03 tests per mutation)
```

Desglose de estados, sumado sobre las siete unidades del log y **recontado a mano
sobre el XML crudo**, no leído del resumen HTML:

| Estado | Recuento (log) | Recuento (XML) |
| --- | ---: | ---: |
| KILLED | 114 | 114 |
| SURVIVED | 3 | 3 |
| NO_COVERAGE | 1 | 1 |
| TIMED_OUT | 0 | 0 |
| RUN_ERROR | 0 | 0 |
| MEMORY_ERROR | 0 | 0 |
| NON_VIABLE | 0 | 0 |
| NOT_STARTED / STARTED | 0 | 0 |
| **Total generado** | **118** | **118** |

Recuento del XML reproducible con
`grep -o "status='[A-Z_]*'" progress/int24_http_remeasure/integration_http_pit_remeasure.xml | sort | uniq -c`
→ 114 KILLED, 1 NO_COVERAGE, 3 SURVIVED. Y `grep -c "<mutation "` → 118.

## 2. Cálculo estricto del score

**114 / 118 = 96,610169 %.** El denominador es el total real de mutantes generados.
No he excluido `NO_COVERAGE` (1) del denominador; no hay `TIMED_OUT` ni `RUN_ERROR` que
excluir, pero de haberlos los habría contado como **no muertos**. No he reclasificado
ningún mutante para acercarme al umbral: no hizo falta y no habría sido lícito.

**96,610169 % ≥ 80 % → SUPERA el umbral.**

### Chequeo de honestidad del informe (patrón de memoria organizacional)

Leído `.memoria-cache/patterns/testing/informe-de-mutacion-con-timeouts-miente.md`
**antes** de calcular. Su regla: antes de leer el score, lee la columna `# timeout`; si
no es 0, el score no vale. Aplicada:

- **`TIMED_OUT` = 0 en las siete unidades.** No hay enmascaramiento por timeout, que es
  el modo de mentira que infla el score. El `timeoutConstInMillis` de 15000 ms
  (`backend/build.gradle.kts:501`) no absorbió ningún mutante.
- **`tests per mutation` subió, no se desplomó**: 3,78 → 4,03 (446 → 475 tests
  ejecutados). Es el contraste del otro modo de mentira que el patrón describe (score
  falso **bajo** por cobertura desplomada). No se da.
- **Ninguna corrida solapada sobre el mismo repo.** El log no tiene esperas de bloqueo
  de Gradle; `reportDir` es propio de este scope
  (`build/reports/pitest-integration-api-http`, `build.gradle.kts:473`); no lancé nada en
  paralelo desde mi carril.
- **Salvedad honesta:** la máquina **sí** estaba cargada por los otros dos carriles
  (49 contenedores Docker y CPU ~76 % al arrancar, 62 contenedores al terminar,
  19,1 GiB libres de 63,4). Que bajo esa carga salgan **cero** timeouts es el resultado
  tranquilizador, no el sospechoso: la carga habría empujado hacia timeouts espurios, y
  no los hubo. El patrón no exige repetir a concurrencia 1 cuando `# timeout` es 0.
  PIT corrió con `threads = 8` (`build.gradle.kts:508`), sin alterar límites para
  compensar carga.

## 3. Mutantes supervivientes

Tres supervivientes y un `NO_COVERAGE`, **los mismos cuatro** que en la campaña anterior,
idénticos en clase, método, línea y mutador. Era lo esperable: `ApiCredentialController`
no se ha tocado desde `c6121aa`.

| # | Clase : línea | Método | Mutador | Mutación aplicada | Estado |
| --- | --- | --- | --- | --- | --- |
| 1 | `ApiCredentialController.java:37` | `revoke` | `VoidMethodCallMutator` | removed call to `ApiCredentialController::acceptable` | SURVIVED |
| 2 | `ApiCredentialController.java:51` | `find` | `VoidMethodCallMutator` | removed call to `ApiCredentialController::acceptable` | SURVIVED |
| 3 | `ApiCredentialController.java:66` | `list` | `VoidMethodCallMutator` | removed call to `ApiCredentialController::acceptable` | SURVIVED |
| 4 | `ApiCredentialController.java:90` | `lambda$acceptable$1` | `PrimitiveReturnsMutator` | replaced int return with 0 (comparador de especificidad) | NO_COVERAGE |

### Cuáles son huecos reales del contrato y cuáles equivalentes

**Los cuatro son huecos de oráculo. Ninguno es equivalente.** No invoco la vía de
exclusión por equivalencia en este informe: no la necesito para superar el umbral y no
tengo prueba de equivalencia para ninguno.

- **1, 2 y 3 — hueco real, no equivalente.** `acceptable(http)`
  (`ApiCredentialController.java:77-98`) resuelve el `Accept` y, si ningún tipo es
  compatible con `application/json` o la calidad es 0, lanza `UnacceptableResponse`, que
  el manejador de `:102-108` convierte en **406 `application/problem+json` con cuerpo
  `NOT_ACCEPTABLE` y `Cache-Control: no-store`**. Al retirar la llamada, la petición sigue
  hasta la negociación por defecto de Spring, cuyo cuerpo y cabeceras **no son los que el
  contrato fija**. El comportamiento observable cambia; que ningún test lo note es un
  agujero de la red, no una equivalencia. La guardia existe en los tres caminos, pero solo
  `create` (`:178`) tiene oráculo de negociación previo al puerto.
- **4 — hueco real de cobertura, no equivalente.** El comparador de especificidad
  (`:88-91`) nunca se ejecuta con más de un tipo compatible, así que sustituir su retorno
  por 0 no cambia nada observable *en las entradas que la suite usa hoy*. Con dos tipos
  compatibles de distinta especificidad sí cambiaría cuál se selecciona y, por tanto, la
  evaluación de `getQualityValue() == 0` de `:93`. Es cobertura ausente, no equivalencia.

### Tests concretos que los matarían

No los escribo yo — son trabajo del `tdd_craftsman`, y deben nacer en rojo y volver a
pasar por el `judge`. Los dejo especificados:

1. **Para 1, 2 y 3:** extender el oráculo de negociación que ya existe para `create` a los
   otros tres verbos. Un test parametrizado sobre
   `PUT /api/v1/me/api-credentials/{id}/revocation`, `GET /api/v1/me/api-credentials/{id}`
   y `GET /api/v1/me/api-credentials`, cada uno con `Accept: text/plain` (y una fila con
   `Accept: application/json;q=0`), aserando las cuatro cosas juntas: **406**,
   `Content-Type: application/problem+json`, cuerpo con código `NOT_ACCEPTABLE` y
   `Cache-Control: no-store`. Aserar solo el 406 **no basta** para matarlos: hay que fijar
   el cuerpo y `Cache-Control`, porque la negociación por defecto de Spring también puede
   producir un 406. Tres filas, tres mutantes.
2. **Para 4:** una fila con `Accept` de **varios tipos compatibles de distinta
   especificidad**, del estilo `application/*+json, application/json`, y otra que combine
   especificidad y calidad, `application/*;q=0.9, application/json;q=0`. La segunda es la
   discriminante: con el comparador real gana `application/json` y la petición se rechaza
   con 406 por calidad 0; con el comparador mutado a 0 el orden de llegada decide y el
   resultado cambia. Cubre la línea y mata el mutante en el mismo golpe.

### Sobre el hueco anterior de esta feature (scope cruzado)

El encargo recuerda que el hueco previo de la 24 era exactamente un superviviente de este
tipo. Lo confirmo cerrado y lo doy por medido: los 34 mutantes de
`ApiCredentialBearerFilter` y los 2 de `ApiCredentialBearerFilter$Permission` están
**todos muertos** (36/36), y entre sus verdugos aparecen las 9 muertes de
`ApiCredentialBearerAdmissionTest` y las 24 de `ApiCredentialBearerTest`. No queda
superviviente en el filtro de admisión.

## 4. Comparación con el 114/118 anterior

| Métrica | Campaña anterior (`c6121aa`) | Esta campaña (`5c84a87`) | Δ |
| --- | ---: | ---: | ---: |
| Mutantes generados | 118 | 118 | 0 |
| KILLED | 114 | 114 | 0 |
| SURVIVED | 3 | 3 | 0 |
| NO_COVERAGE | 1 | 1 | 0 |
| TIMED_OUT / RUN_ERROR / MEMORY_ERROR / NON_VIABLE | 0 | 0 | 0 |
| Score estricto | 96,610169 % | 96,610169 % | 0 |
| Cobertura de línea (clases mutadas) | 304/308 | **311/315** | **+7 / +7** |
| Clases de test examinadas | 541 | **543** | +2 |
| Tests ejecutados | 446 | **475** | +29 |
| Tests por mutante | 3,78 | **4,03** | +0,25 |
| `SecurityConfiguration` | **53/53** líneas, 15/15 mutantes | **60/60** líneas, 15/15 mutantes | **+7 líneas, +0 mutantes** |
| Paquete `com...adapter.http` | 251/255 | 251/255 | 0 |

### Por qué las cifras coinciden, y qué significa exactamente

Las cifras coinciden, pero **no es la misma medición repetida**: es una medición nueva
sobre fuentes nuevas que aterriza en el mismo número. Lo he verificado por dos vías
independientes, porque un 118/114 idéntico es justo el resultado que uno debe sospechar.

**Prueba 1 — los números de línea se desplazaron.** Si PIT hubiera medido la fuente
antigua, las líneas serían las de antes. No lo son. Cotejo mutante a mutante de
`SecurityConfiguration` entre ambos XML:

| Método / mutador | Línea antes | Línea ahora |
| --- | ---: | ---: |
| `users`, NegateConditionals (x2) | 20 | **40** |
| `users`, NullReturnVals | 22 | **42** |
| `bearerSecurity`, NullReturnVals | 42 | **62** |
| `lambda$bearerSecurity$0`, BooleanTrueReturnVals | 42 | **62** |
| `lambda$bearerSecurity$0`, NegateConditionals | 42 | **62** |
| `lambda$security$0`, VoidMethodCall (setStatus / setContentType / writeValue) | 65 / 66 / 67 | **86 / 87 / 88** |
| `security`, NullReturnVals | 73 | **94** |
| `lambda$security$5`, VoidMethodCall | 89 | **111** |
| `lambda$security$6`, VoidMethodCall | 92 | **114** |
| `lambda$security$8`, VoidMethodCall | 99 | **121** |
| `sessionIdResolver`, NullReturnVals | 114 | **136** |
| `sessionCookie`, NullReturnVals | 120 | **142** |

El desplazamiento (+20 arriba, +22 abajo) corresponde exactamente al +23/-1 del ciclo
C4. `users` está hoy en la línea 40 del fichero y `sessionIdResolver` devuelve en la 136:
comprobado contra la fuente. La campaña **sí** midió `SecurityConfiguration` modificada.

**Prueba 2 — el universo de líneas creció.** 53/53 → 60/60 en esa clase, y 308 → 315 en
el total. Las siete líneas nuevas están **cubiertas al 100 %**: entran en la medición y
los tests las ejecutan. El paquete `adapter.http` se queda clavado en 251/255, lo que
confirma que el único cambio del perímetro es el esperado.

**El hallazgo que sí hay que decir en voz alta:** las siete líneas nuevas están cubiertas,
pero **PIT no genera ni un solo mutante sobre ellas**. `grep -c securityHeaders` sobre el
XML nuevo devuelve **0**. Ni el literal `CONTENT_SECURITY_POLICY` (`:16-19`), ni el método
`securityHeaders` (`:24-34`), ni la llamada `.headers(...)` de la cadena Bearer (`:63`)
producen mutante alguno. La única entrada en la línea 94 es el NullReturnVals de
`security()`, que ya existía en la 73 y que solo anula el retorno de todo el constructor
de la cadena: no ejercita las cabeceras.

La razón es del catálogo de mutadores, no de la ejecución. Con los mutadores por defecto
de PIT 1.22.0 (`features = -FRECORD`, sin plugin Arcmutate, `build.gradle.kts:34-509`):
`VoidMethodCallMutator` **solo retira llamadas a métodos `void`**, y tanto
`csp.policyDirectives(...)` como `referrer.policy(...)` y `headers.contentSecurityPolicy(...)`
devuelven configuradores encadenables, no `void`; y no hay mutador de literales de cadena
en el conjunto por defecto, así que la política CSP no se puede mutar. El código nuevo es,
sencillamente, **invisible a la mutación** con esta configuración.

**Consecuencia, sin maquillaje:** la condición 1 del juez queda **formalmente cumplida**
—la campaña se ha reejecutado sobre las fuentes actuales y supera el umbral—, pero la
reejecución **no aporta ninguna evidencia de mutación nueva sobre A8**. La afirmación del
juez de que el CSP es «código que ningún mutante ha visitado nunca» sigue siendo cierta
**después** de remedir, y ahora sabemos por qué: no es que no se midiera, es que el
catálogo no produce mutantes ahí. La única red bajo esas cabeceras es
`SecurityHeadersTest`, que asería el literal exacto de la política en las dos cadenas y
que en esta corrida mató un mutante real (el `setStatus` de
`ApiCredentialBearerFilter:175`, vía `bearerRejectionAlsoCarriesTheSecurityHeaders`), lo
que acredita que la clase se carga y se ejercita de verdad. Quien quiera cobertura de
mutación sobre la política tendría que añadir un mutador de literales (Arcmutate) o
extraer la construcción de la cabecera a código con condicionales; **ninguna de las dos
cosas la decido yo, y ninguna es condición del umbral.**

## 5. Evidencia conservada

Directorio `progress/int24_http_remeasure/` (creado por mí, solo evidencia; no toca
producto):

| Fichero | SHA-256 |
| --- | --- |
| `integration_http_pit_remeasure.xml` (XML crudo, copia byte a byte de `backend/build/reports/pitest-integration-api-http/mutations.xml`) | `2C540D4166B93FF3CD8BA3D03DB77010550B815F29EEF293C9894F07415E083D` |
| `integration_http_pit_remeasure_html.zip` (informe HTML completo del directorio oficial) | `E266BEF9C26D323B9EA478438EEB4B16112B9A75845327F6E64167A481E1A5B6` |
| `pit_http_run.log` (salida completa del comando) | `EF31B728AEE2B038894273F160FAA7139A26630A6865650585C70105A0794C94` |
| `pit_http_exit.txt` | contenido `EXIT=0` |

Informe original vivo (se sobrescribirá en la próxima campaña de este scope):
`backend/build/reports/pitest-integration-api-http/` — `mutations.xml`, `index.html`,
`com.apptolast.organization.adapter.config/SecurityConfiguration.java.html` y
`com.apptolast.organization.adapter.http/`.

Evidencia de la campaña anterior, con la que he comparado, intacta y no modificada:
`progress/integration24_http_pit_portable.zip` (SHA-256
`E0C7D5ED89A9351D3855120F11F491B0E938E026519FAB5BD09FC045873D8CD1`, manifiesto en
`progress/integration24_http_pit_portable_manifest.json`). La descomprimí en un directorio
temporal fuera del repositorio; el ZIP no se ha tocado.

## 6. Perímetro medido

`targetClasses` del scope `integration_api_http` (`backend/build.gradle.kts:76-82`), las
cinco declaradas, todas presentes en el informe:

| Clase | KILLED / total |
| --- | ---: |
| `adapter.config.SecurityConfiguration` | 15/15 |
| `adapter.http.ApiCredentialBearerFilter` | 34/34 |
| `adapter.http.ApiCredentialBearerFilter$Permission` | 2/2 |
| `adapter.http.ApiCredentialController` | **52/56** |
| `adapter.http.ApiCredentialController$CreationRequest` | 3/3 |
| `adapter.http.ApiCredentialSessionIdResolver` | 7/7 |
| `adapter.http.IntegrationOpenApiController` | 1/1 |
| **Total** | **114/118** |

`targetTests = com.apptolast.organization.*` (`build.gradle.kts:448`): todos los JUnit son
candidatos, sin recortes de tests.

## 7. Límites de esta medición

Lo que **no** acredita este informe, y que nadie debe dar por probado:

1. **Solo he ejecutado una campaña.** El núcleo (180/188) y el frontend (836/997) los cito
   desde `progress/judge_integration_api_final.md`; **no los he reproducido** ni
   recalculado. Su vigencia descansa en el argumento del juez —sus fuentes no cambiaron—,
   no en medición mía.
2. **El checkout no es exactamente el que firmó el juez.** He medido `5c84a87`; el
   dictamen es sobre `926fb6a`. La diferencia son commits `docs:` que no tocan
   `backend/src`, pero existe y la declaro.
3. **El árbol de trabajo no estaba limpio.** Contiene ficheros sin seguimiento del carril
   paralelo de `reschedule` (20 clases de `backend/src/main`, 2 de `backend/src/test` y una
   migración `V12__block_changes.sql`), más ` M .claude/settings.json` y `?? backend/bin/`.
   Esos ficheros **se compilaron** en el baseline: de ahí las 543 clases de test frente a
   541. **No contaminaron el resultado**: ningún test `Reschedule*` figura como verdugo de
   ningún mutante (`grep -c Reschedule` sobre el XML → 0) y las clases nuevas no entran en
   `targetClasses`. Aun así, la corrida no es reproducible desde un checkout limpio de
   `5c84a87`, y esto es exactamente la condición 3 del juez sin cumplir.
4. **La máquina estaba cargada** por los otros dos carriles durante toda la corrida (49→62
   contenedores). Auditado en §2, pero no eliminado.
5. **No he ejecutado la suite completa ni `bin/harness init`.** El encargo lo prohibía
   expresamente. La cobertura de PIT ejercitó 543 clases de test en su fase de baseline,
   pero eso **no es** un veredicto de suite verde: PIT no falla la construcción por un test
   rojo del baseline, lo descarta como candidato. Que la suite esté verde sigue siendo
   afirmación de terceros.
6. **No he verificado los mutantes por sabotaje manual.** El patrón de memoria lo propone
   como contraprueba (aplicar a mano un mutante «Survived» y correr la suite); no lo he
   hecho porque habría exigido editar `src/`, que tengo prohibido, y porque con
   `TIMED_OUT = 0` el informe no presenta el síntoma que justifica la contraprueba.
7. **Mutación no es corrección.** Un 96,6 % no dice nada sobre OpenAPI, UX, E2E, rollback,
   CI ni aceptación real, ni sobre los riesgos A1/A2/A3 que el juez deja abiertos.
8. **No he editado código, tests, `feature_list.json` ni ninguna configuración.** Los
   únicos ficheros que he creado son este informe y los cuatro artefactos de evidencia
   de §5.

## 8. Conclusión

**PASS.** 114/118 = 96,610169 % estricto, sobre el total real de mutantes generados, con
cero timeouts y cero errores de ejecución que pudieran inflar la cifra. Supera con holgura
el 0,8 de `harness.config.json`.

Queda cumplida la condición 1 de `progress/judge_integration_api_final.md` §8. Advierto
—sin que altere el veredicto— que la reejecución **no añade evidencia de mutación sobre
las cabeceras del ciclo C4**, por la razón técnica de §4: el catálogo de mutadores no
genera mutantes ahí. Y recuerdo que las condiciones 2 (CI verde) y 3 (árbol limpio) del
juez **siguen abiertas**, y la 3 la he visto incumplida con mis propios ojos al medir.
Marcar la 24 como `done` es decisión del `craftsman_lead`, no mía.

Los cuatro residuos de `ApiCredentialController` no son equivalentes: son huecos de
oráculo con test propuesto en §3. Su cierre es trabajo del `tdd_craftsman`, no mío.
