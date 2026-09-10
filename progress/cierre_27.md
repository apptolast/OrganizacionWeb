# Veredicto de cierre — feature 27-github-connector (features/github_connector.feature)

**NO APROBADA** — 11 condiciones, 6 bloqueantes. 10 de septiembre de 2026.

Dos verificadores independientes (cierres declarados / lo que sigue bloqueando) más
síntesis. Sin ejecutar nada: la máquina estaba midiendo.

## Resumen

RECHAZADA. De los 18 motivos crudos he confirmado 11 contra el disco, he descartado 1, he rebajado 2 a matiz de redacción, y he corregido el mecanismo de otro. Lo que queda: seis condiciones bloqueantes y cinco no bloqueantes.

Lo verificado uno a uno. (1) La puerta de mutación de frontend NO está pasada: el informe que sostiene el 85,06 % tiene mtime 2026-09-10 10:55:31 y contiene exactamente tres ficheros (github-connector-client.ts 246, github-connector.tsx 341, integrations-index.tsx 2 = 589), mientras que stryker.github-connector.config.json declara CUATRO en 'mutate' desde a458c793 (13:36) y la producción que cierra el @s41 es de e89e71ef (12:52). El módulo nuevo tiene cero mutantes medidos y el componente se remidió tras reescribirse. Los dos verificadores lo señalan y es el bloqueante principal. (2) El backend, en cambio, aguanta el recuento a mano: mutations.xml da 439 mutantes, 419 KILLED + 2 TIMED_OUT = 421, 12 SURVIVED + 6 NO_COVERAGE, 421/439 = 95,90 % exacto, con 33 clases mutadas y CERO de GitLab. Y corresponde al árbol que se cierra: `git diff 26a5a5eb..HEAD -- backend/src backend/build.gradle.kts frontend/src e2e features` sale VACÍO. (3) El @s41 fila 2 está declarado cubierto sin oráculo: no existe github-connector-draft.test.ts, y la única prueba de la fila ('starts from scratch when another person signs in') sirve una conexión valid, así que nunca se teclea repositorio, nunca se escribe borrador y `exact(draft,…)` corta antes de llegar a `draft.owner !== owner`; su única aserción relacionada es queryByText, que no lee el value de un input. (4) TOO_SHORT: PersonalAccessToken.java:19,29-30 impone MIN_LENGTH=5 con un código de rechazo que `grep TOO_SHORT features/` no encuentra en ninguna parte, que la tabla del @s6 (:94-105) no enumera, que la 29 hereda por upTo(), y que NO llegó a decisiones_pendientes.md (sus nueve puntos son otros). (5) El contrato se contradice: :384 dice «exactamente los once campos del recibo» mientras :146 y :165 dicen doce, el record ImportResponse tiene doce componentes y la prueba que cubre esa fila se llama s30_theOwnReceiptComesBackWithItsTwelveFields; la enmienda §1 cubre 146 y 165-166 pero no 384. (6) La documentación de supervivientes que el juez nombra en su condición 8 (progress/mutation_github_connector.md) describe la campaña caducada de 502 mutantes y 34 no muertos, y dos de sus veredictos están refutados por el XML de hoy: N3 (lambda$finish$1:144) se declara «CUBIERTO AHORA» y sigue NO_COVERAGE —además no es el read() de finish, es el proveedor del orElseThrow—, y N5 (githubUnavailable) sigue NO_COVERAGE tras el «a confirmar midiendo». Las líneas del propio documento ya no cuadran (missing:217 vs 229, githubUnavailable:214 vs 217).

Lo que he DESCARTADO. «PersonalAccessTokenTest:46-52 no discrimina el ternario que se retiró»: cierto como queja de nombre, irrelevante como hallazgo. El ternario está muerto por construcción —«a», «ab», «abc», «abcd» ahora lanzan TOO_SHORT en el constructor— y PIT no reporta NI UN superviviente en PersonalAccessToken en la campaña vigente: la propiedad está medida, sólo que por la otra prueba. No es condición de cierre.

Lo que he REBAJADO a matiz de redacción. «82 mutantes con verdugo de la 29»: mi recuento sobre el XML da 78, no 82, y PIT anota sólo al primer verdugo, así que el dato no prueba que ningún mutante quede sin dueño en la 27 —el propio verificador lo admite—. El denominador está verificado limpio (0 de 33 clases mutadas son de GitLab). Queda como matiz de la frase «el ámbito ya sólo mide lo que es suyo», no como puerta. Y el desanclaje de la condición 3 del juez (el escuchador de Escape ya no está en :149-159 sino en :161-173, verificado) no va como condición aparte: se disuelve con el informe nuevo de Stryker, y lo he metido dentro de esa condición.

Lo que he CORREGIDO de un verificador. Dice que la prueba de la fila 2 usa `rerender`, que el inicializador de useState no se vuelve a ejecutar y que por eso la rama nunca se toma, insinuando además un fallo de producción con el propietario. Falso: github-connector.tsx:29 renderiza `<GithubConnectorScreen key={owner} …>`, así que cambiar de propietario SÍ remonta y readRepositoryDraft(owner) SÍ se vuelve a llamar. La producción está bien; lo que falta es el oráculo. La causa real del hueco es que en esa prueba nunca se escribe borrador. Lo dejo dicho porque, con la causa equivocada, el carril arreglaría producción sana en vez de escribir la prueba que falta.

Lo demás confirmado y bien cerrado: los cinco defectos de producto (M7 recibo colgado con oráculo que sella para otro propietario con el cifrador real; M10 hint() como substring sin rama; M11 filtro por source en el SQL; M12 canonical() con ConnectGithubTest afirmando también que no se guarda fila; M5 con el GREATEST atado al CHECK de la V30), el ámbito PIT limpio con GithubIssueConnections 4/4, Slf4jConnectorAudit 4/4 y AesGcmSecretCipher 17/17, el bloqueante del veredicto anterior levantado en e2e/github-connector.spec.mjs:197, la CI verde 34477162444 sobre 9d1e7d2e que sí incluye e89e71ef (`merge-base --is-ancestor` comprobado), y feature_list.json con la 27 todavía en in_progress. Nadie ha marcado done por su cuenta.

NADA de esto se ha ejecutado: máquina bloqueada por la campaña de PIT en curso. Todo lo de arriba es lectura de ficheros, del XML y del historial de git.

---

## Condiciones

| # | Condición | Bloq. | De quién | Min. |
|---|---|---|---|---|
| 1 | La puerta de mutación de frontend no mide la producción que cierra el @s41: el informe vigente es de las 10:55:31 con tres ficheros y 589 mutantes, y  | **SÍ** | campana | 45 |
| 2 | El @s41 fila 2 (otra persona inicia sesión y no hereda el repositorio de la primera) está declarado cubierto sin ningún oráculo que pueda fallar: no e | **SÍ** | carril | 35 |
| 3 | El arreglo de hint() añade un rechazo de contrato nuevo (TOO_SHORT, mínimo de cinco caracteres) que ninguna tabla del .feature nombra, que la feature  | **SÍ** | propietario | 25 |
| 4 | El contrato se contradice consigo mismo y la prueba que cubre la fila afirma lo contrario de lo que la fila dice: features/github_connector.feature:38 | **SÍ** | propietario | 15 |
| 5 | La condición 8 del veredicto vigente (todo superviviente documentado uno por uno) apunta a progress/mutation_github_connector.md, que describe la camp | **SÍ** | orquestador | 30 |
| 6 | La condición 5 del veredicto vigente (bin/harness init verde en manos del orquestador, C1 y C4) sigue sin ejecutar; el juez la dejó explícita: «si sal | **SÍ** | orquestador | 10 |
| 7 | El 503 GITHUB_UNAVAILABLE que produce el arreglo de M12 no tiene oráculo en la frontera HTTP: el manejador GithubConnectorController.githubUnavailable | no | carril | 20 |
| 8 | La bitácora de UX publica cifras que ya no se pueden recomputar: dice «15 pruebas» del spec del conector y «16 de 16 en verde», cuando e2e/github-conn | no | orquestador | 10 |
| 9 | El comentario de la V30 razona sobre un dominio que el arreglo de TOO_SHORT acaba de cambiar: justifica el mínimo de 29 octetos con «un token de un so | no | orquestador | 10 |
| 10 | saveRepositoryDraft escribe en sessionStorage sin guarda y se llama en CADA pulsación desde el onChange del campo repositorio (github-connector.tsx:39 | no | carril | 10 |
| 11 | La frase «el ámbito ya sólo mide lo que es suyo» es más ancha de lo verificado. El denominador sí está limpio —comprobado: 0 clases de GitLab entre la | no | orquestador | 10 |

## El trabajo, una por una

### 1. La puerta de mutación de frontend no mide la producción que cierra el @s41: el informe vigente es de las 10:55:31 con tres ficheros y 589 mutantes, y el módulo github-connector-draft.ts (creado a las 12:52, metido en el ámbito a las 13:36) tiene cero mutantes medidos, además de que github-connector.tsx se reescribió después del informe.

- **Bloqueante:** sí · **De:** campana · **Estimación:** 45 min

Relanzar Stryker con el ámbito de cuatro ficheros y publicar el informe nuevo anotando el SHA de HEAD en la primera línea: bin\harness.ps1 mutate github_connector-frontend (equivale a pnpm --dir frontend exec stryker run stryker.github-connector.config.json). El informe debe declarar el total de mutantes, la puntuación (umbral break 80), ningún módulo a cero —en particular github-connector-draft.ts— y re-anclar la condición 3 del juez al rango de hoy del escuchador de Escape, que está en github-connector.tsx:161-173, no en :149-159. Nota: el árbol de frontend no ha cambiado desde 26a5a5eb, así que la corrida mide exactamente lo que se cierra.

### 2. El @s41 fila 2 (otra persona inicia sesión y no hereda el repositorio de la primera) está declarado cubierto sin ningún oráculo que pueda fallar: no existe github-connector-draft.test.ts, y la prueba que lleva el nombre de la fila nunca escribe borrador, así que el guardián de propietario nunca se evalúa. Borrar `draft.owner !== owner ||` deja la suite verde.

- **Bloqueante:** sí · **De:** carril · **Estimación:** 35 min

Escribir frontend/src/github-connector-draft.test.ts cubriendo las tres ramas defensivas sin oráculo: borrador de otro propietario (github-connector-draft.ts:39), repository que no es cadena (:40) y JSON ilegible que entra por el catch (:33-35), afirmando en cada caso que devuelve "" y que la clave queda borrada. Y reforzar la prueba de la fila en github-connector.test.tsx:568: servir 404 para que se pinte el formulario, teclear "octocat/Hello-World" con el primer propietario, remontar con owner="otra-persona" y afirmar getByLabelText(/repositorio/i) toHaveValue("") más sessionStorage sin el repositorio ajeno. OJO: la producción está bien —github-connector.tsx:29 lleva key={owner}, así que el cambio de propietario sí remonta—; lo que falta es la prueba, no un arreglo.

### 3. El arreglo de hint() añade un rechazo de contrato nuevo (TOO_SHORT, mínimo de cinco caracteres) que ninguna tabla del .feature nombra, que la feature 29 hereda por el constructor compartido de upTo(), y que no llegó a progress/decisiones_pendientes.md pese a que la instrucción de sesión lo exige. Cambia una superficie HTTP pública enumerada en el contrato.

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 25 min

Anotar el punto en progress/decisiones_pendientes.md con la fila redactada tal cual ha de quedar (| token de 4 caracteres | 400 | VALIDATION_ERROR | TOO_SHORT |) y el motivo —la pista son los cuatro últimos caracteres y se guarda en claro en token_hint, así que un token de cuatro se publicaría entero—, y que el propietario la contrafirme. Con la contrafirma: añadir la fila a la tabla del @s6 (features/github_connector.feature:94-105) y la equivalente en features/additional_connectors.feature:118-121, y la fila de prueba en GithubConnectorApiTest. Pendiente de ejecución tras el cambio: gradlew.bat :backend:test --tests "*Gitlab*" --tests "*PersonalAccessTokenTest" --tests "*GithubConnectorApiTest".

### 4. El contrato se contradice consigo mismo y la prueba que cubre la fila afirma lo contrario de lo que la fila dice: features/github_connector.feature:384 sigue diciendo «exactamente los once campos del recibo» mientras :146 y :165 dicen doce, el record ImportResponse tiene doce componentes y la prueba se llama s30_theOwnReceiptComesBackWithItsTwelveFields. La enmienda §1 de decisiones_pendientes cubre :146 y :165-166 pero no :384.

- **Bloqueante:** sí · **De:** propietario · **Estimación:** 15 min

Ampliar el punto 1 de progress/decisiones_pendientes.md para incluir la tercera línea caducada (:384, once → doce, con la lista de los doce campos igual que en :165) y que el propietario contrafirme la enmienda completa; después corregir la línea del .feature. Es una línea de texto, pero es contrato: no la toca el carril por su cuenta.

### 5. La condición 8 del veredicto vigente (todo superviviente documentado uno por uno) apunta a progress/mutation_github_connector.md, que describe la campaña superada (502 mutantes, 34 no muertos, con GitlabApiBase y HttpGitlabIssueSource dentro) y no la que se ofrece a la puerta (439, 18 no muertos). Además dos de sus veredictos están refutados por el XML de hoy: N3 lambda$finish$1:144 se declara «CUBIERTO AHORA» y sigue NO_COVERAGE, y N5 githubUnavailable sigue NO_COVERAGE.

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 30 min

Consolidar la documentación de supervivientes contra los 18 de la campaña vigente (ya listados nominalmente en progress/mutacion_github_connector_backend_medida.md:36-55), dando a cada uno veredicto de MUERTO / EQUIVALENTE / ABIERTO, y marcar progress/mutation_github_connector.md como caducado con un encabezado que remita al informe nuevo. Corregir expresamente N3 —sigue NO_COVERAGE, GithubConnectorPersistenceTest mató 39 mutantes en esa misma corrida, y lambda$finish$1:144 no es el read() de dentro de finish sino el proveedor `() -> missing(importId)` del orElseThrow, que un cierre con éxito no ejecuta jamás— y N5, que se midió y sigue sin cobertura.

### 6. La condición 5 del veredicto vigente (bin/harness init verde en manos del orquestador, C1 y C4) sigue sin ejecutar; el juez la dejó explícita: «si sale rojo, este veredicto decae».

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 10 min

PENDIENTE DE EJECUCIÓN, con la máquina drenada: bin\harness.ps1 init. Adjuntar la salida en progress/ junto al resto de puertas. No lo he ejecutado por la campaña de PIT en curso.

### 7. El 503 GITHUB_UNAVAILABLE que produce el arreglo de M12 no tiene oráculo en la frontera HTTP: el manejador GithubConnectorController.githubUnavailable():215-218 sale NO_COVERAGE en la campaña vigente, y el único 503 que prueban las de API viene de IssueImportFailedException (GithubConnectorApiTest:496-519), no de esta excepción. La prueba de M12 es de caso de uso y sólo afirma la excepción.

- **Bloqueante:** no · **De:** carril · **Estimación:** 20 min

Añadir a GithubConnectorApiTest una fila que haga lanzar GithubUnavailableException al PUT de conexión y afirme 503 con código GITHUB_UNAVAILABLE y Cache-Control no-store. Mata de paso el NullReturnVals NO_COVERAGE de :217. Pendiente de ejecución: gradlew.bat :backend:test --tests "*GithubConnectorApiTest".

### 8. La bitácora de UX publica cifras que ya no se pueden recomputar: dice «15 pruebas» del spec del conector y «16 de 16 en verde», cuando e2e/github-connector.spec.mjs tiene hoy 16 tests y el recorrido con el de zoom nativo son 17.

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Actualizar progress/ux_github_connector.md:18-21 a 16 + 1 = 17 recorridos, citando la CI verde 34477162444 sobre 9d1e7d2e como la corrida que los incluye (verificado: e89e71ef es ancestro suyo). Si se quiere evidencia propia: node scripts/e2e.mjs e2e/github-connector.spec.mjs e2e/github-connector-native-zoom.spec.mjs.

### 9. El comentario de la V30 razona sobre un dominio que el arreglo de TOO_SHORT acaba de cambiar: justifica el mínimo de 29 octetos con «un token de un solo carácter, que el dominio admite», y hoy el token más corto son cinco caracteres, es decir 33 octetos. Un comentario que describe mal el presente es peor que ninguno.

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Corregir el comentario de backend/src/main/resources/db/migration/V30__github_connector_ciphertext_bounds.sql para que diga el mínimo real del dominio de hoy (12 + 5 + 16 = 33) y por qué el CHECK se deja en 29 —cota inferior holgada a propósito, no cálculo caducado—. No hace falta migración nueva: el rango sigue admitiendo todo lo legítimo. La prueba ya usa SHORTEST_TOKEN = "xxxxx", así que no está rota.

### 10. saveRepositoryDraft escribe en sessionStorage sin guarda y se llama en CADA pulsación desde el onChange del campo repositorio (github-connector.tsx:396); el módulo del que la bitácora dice estar calcado, import-data-intent.ts:17-19, sí comprueba la escritura. Un setItem que lance —cuota, modo privado— rompe el tecleo de la pantalla, y ninguna prueba lo ejerce.

- **Bloqueante:** no · **De:** carril · **Estimación:** 10 min

Envolver el setItem de github-connector-draft.ts:19-22 de modo que un almacén que rechaza escribir degrade en silencio (el borrador es una comodidad, no contrato) en vez de propagar la excepción al manejador de React, y añadir una prueba con sessionStorage.setItem simulado lanzando que compruebe que la pantalla sigue tecleando.

### 11. La frase «el ámbito ya sólo mide lo que es suyo» es más ancha de lo verificado. El denominador sí está limpio —comprobado: 0 clases de GitLab entre las 33 mutadas—, pero 78 de los 439 mutantes llevan como killingTest una prueba de la feature 29, concentrados en clases compartidas. PIT anota sólo al primer verdugo, así que ni prueba ni descarta que queden sin dueño propio.

- **Bloqueante:** no · **De:** orquestador · **Estimación:** 10 min

Matizar la frase en progress/mutacion_github_connector_backend_medida.md: el denominador está limpio, el numerador comparte verdugos con la 29 en clases compartidas y eso no se ha medido. Si se quiere cerrar con número (opcional, no es puerta): gradlew.bat :backend:pitest -PmutationScope=github_connector -PmutationTests="com.apptolast.organization.*Github*" y comparar los KILLED contra los 421 actuales.

