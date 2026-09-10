# Libro de ratificaciones del propietario

Cada cambio de contrato pasa por el propietario. Hasta hoy esa aprobación vivía
sólo en la conversación, así que un juez que lee el repositorio no podía
distinguir «ratificado» de «lo escribió el mismo carril que hizo la enmienda».
El panel de precierre de la feature 29 lo señaló con esas palabras, y tenía razón
**en la forma**: la decisión existía, el artefacto no. Aquí está el artefacto.

Cada entrada lleva la pregunta tal como se le hizo y la respuesta tal como la
eligió. No se anota nada aquí que no se pueda citar así.

---

## R1 — 10 de septiembre de 2026 — dos enmiendas alineadas con decisiones previas

**Pregunta:** «Dos carriles enmendaron su `.feature` para alinearlo con enmiendas
tuyas ya ratificadas. Según la disciplina del repo, todo cambio de contrato pasa
por ti. ¿Las ratificas?»

**Opción elegida:** «Ratifico las dos», descrita así en la propia pregunta:

> La de la feature 29 alinea su `@s31` con la enmienda ya ratificada del `@s31`
> de la 27 (las dos filas Bearer pasan a 403 `API_SCOPE_DENIED`); el juez la
> revisó y dijo que es correcta, no extralimitación. La de la 28 amplía su
> contrato con el plazo de lectura del cuerpo del feed, que cierra un agujero de
> recursos.

**Alcanza a:**

- `features/additional_connectors.feature:388` — `@s31`, las dos filas de
  credencial Bearer pasan de 401 `UNAUTHENTICATED` a 403 `API_SCOPE_DENIED`.
  Es la condición **C7** de `progress/judge_additional_connectors.md`.
- La ampliación del contrato de la 28 con el plazo de lectura del cuerpo del feed.

---

## R2 — 9 de septiembre de 2026 — el `@s31` de la feature 27

Ratificada en su día y ya escrita en el propio contrato
(`features/github_connector.feature:397`, «Enmienda del 9 de septiembre de 2026,
ratificada por el propietario»). Es la enmienda que R1 extiende a la 29.

## R3 — 9 de septiembre de 2026 — la fila «cadena vacía» de `APP_CONNECTOR_KEY`

Ratificada y escrita en `features/github_connector.feature:55`.

---

## Lo que NO está ratificado

Vive en `progress/decisiones_pendientes.md`. Nada de allí puede darse por bueno,
por razonable que parezca la enmienda.

---

## R4 — 10 de septiembre de 2026 — `latencyMs` se mide con cronómetro monótono

**Pregunta:** «El contrato de webhooks dice que `latencyMs` se mide "con el reloj
inyectado". Implementé un cronómetro **monótono** inyectado en vez del reloj de
pared, porque medir tiempo transcurrido con un reloj de pared da latencias
negativas si el sistema ajusta la hora. ¿Cómo lo cierro?»

**Opción elegida:** «Enmendar la línea al cronómetro», con esta vista previa
delante:

> ANTES: «latencyMs es un entero no negativo medido con el reloj inyectado»
> DESPUÉS: «latencyMs es un entero no negativo medido con el cronómetro monótono
> inyectado»

**Alcanza a:** `features/webhooks.feature:233` y `:320`, y `project-spec.md:2038`.
Es el bloqueante **B7** de `progress/carriles/bloqueantes_25.md`, que decía —con
razón en la forma— que la enmienda «no lleva contrafirma donde mira el siguiente
revisor». Ahora la lleva, en el propio `.feature`.

---

## R5 — 10 de septiembre de 2026 — el plazo del `@s32` sube a 2500 ms

**Pregunta:** «El escenario `@s32` dice "transcurren 1500 ms desde el arranque" y
exige dos ciclos del worker, pero el planificador está en `initialDelay=1000` y
`fixedDelay=1000`: a los 1500 ms sólo ha corrido uno. La cláusula es imposible.»

**Opción elegida:** «Subir el plazo a 2500 ms», descrita así: _el `When` pasa a un
plazo que sí cubre dos tics; la prueba de reflexión ya sujeta `initialDelay`,
`fixedDelay` y unidad, así que cualquier desvío se pone rojo al instante._

**Alcanza a:** `features/webhooks.feature:402`.

---

## R6 — 10 de septiembre de 2026 — el catálogo de `error_class` pasa a ocho

**Pregunta:** «El arreglo de hoy escribe una octava clase de error,
`SECRET_UNREADABLE`, cuando un secreto no se puede descifrar. El contrato cierra
el catálogo en siete, y el frontend la rechaza: deja ciego el panel de entregas
de ese endpoint. ¿Qué mando?»

**Opción elegida:** «Ampliar el catálogo a ocho», descrita así: _contrafirmas la
ampliación y el carril añade `SECRET_UNREADABLE` al decodificador del frontend;
el propietario ve por qué falló esa entrega en vez de una pantalla ciega._

**Alcanza a:** `features/webhooks.feature` (`@s25`), `project-spec.md:2038`, y
`frontend/src/webhooks-client.ts` (que hoy la rechaza y deja el panel ciego).

---

## R7 — 10 de septiembre de 2026 — la feature 29 se retira del producto

**Pregunta:** «Dijiste que la feature 29 se puede quitar entera. Tiene 38 ficheros
de producción y sus piezas las comparte con el conector de GitHub (feature 27),
que sí quieres. ¿Qué quieres exactamente?»

**Opción elegida:** «**Borrarla entera del producto**», descrita en la propia
pregunta con sus costes delante:

> Cirugía: hay que des-unificar `ImportIssues`, devolver a la 27 su forma de
> recibo anterior (revirtiendo una enmienda de contrato), quitar
> `BoundedResponse` del origen de GitHub perdiendo el techo de 5 MiB, y revertir
> la migración V29. **Días, y con riesgo real de romper la 27.**

**Alcance:** desaparecen el catálogo de conectores y el conector de GitLab —su
producción, sus pruebas, sus specs de E2E, su contrato
`features/additional_connectors.feature` y su entrada en `feature_list.json`, que
pasa de 30 features a **29**—.

**Tres decisiones técnicas que tomo yo dentro del encargo, y por qué:**

1. **`BoundedResponse` se queda**, como código de la feature 27. Es el techo de
   5 MiB que impide que un proveedor agote la memoria del proceso, y lo usa
   `HttpGithubIssueSource`. Borrarlo reabriría ese agujero: el propietario pidió
   quitar una feature, no introducir una vulnerabilidad.
2. **`ImportIssues` se queda unificado**, sirviendo sólo a GitHub. Des-unificarlo
   es riesgo puro sin ganancia.
3. **La forma del recibo se queda** con `source` y `projectPath`. Los introdujo la
   29, pero hoy son la forma del recibo de la 27 y su contrato ya se enmendó para
   casarlos: revertirlo rompería justo lo que hay que proteger.

**Y una consecuencia que conviene tener escrita:** la migración `V29` no sólo creó
tablas, también renombró `issue_import_receipts.repository` a `project_path` y
añadió `source`. Esas dos **no se revierten** por lo dicho en el punto 3. La
migración de retirada se limita a `gitlab_connections` y al `CHECK` de
`task_external_links`.

---

## R8 — 10 de septiembre de 2026 — la feature 27 también se retira

**Instrucción del propietario, literal:** «ya no la quiero ni que se implemente ni
que exista», sobre la entrada `27 github_connector` de `feature_list.json`, que
pegó entera en el mensaje.

**Alcance:** desaparece el conector de GitHub y, con él, **toda la infraestructura
de conectores**: el caso de uso de importación, el puerto `IssueSource`, los
recibos, los enlaces externos, el token cifrado y sus adaptadores. Se retira junto
con la feature 29 en una sola cirugía, porque compartían casi todo y hacerlo en
dos rondas obligaría a conservar piezas que después habría que borrar.

`feature_list.json` pasa de 30 features a **28**. Quedan tres por cerrar: la
**25** (webhooks), la **28** (calendario externo) y la **30** (automatizaciones).

**Lo que se queda, verificado con `grep` y no de memoria:** `SecretCipher`,
`AesGcmSecretCipher`, `ConnectorKeyRing` y `ConnectorsGate`. **No son de los
conectores**: los usa la feature 28 para cifrar en reposo la URL del feed del
calendario externo — están en `SaveExternalCalendar` y `SyncExternalCalendar`.
Borrarlos habría dejado esa feature sin cifrado. Pasan a ser código de la 28.

También se conserva la variable `APP_CONNECTOR_KEY` y su cableado, por lo mismo.
El nombre queda desafortunado sin conectores, y se deja escrito aquí en vez de
renombrarlo: renombrar una variable de entorno es otra cirugía y toca el
despliegue.

---

## R9 — 10 de septiembre de 2026 — la feature 30 también se retira

**Instrucción del propietario, literal:** «ya no la quiero ni que se implemente ni
que exista», sobre la entrada `30 automations` de `feature_list.json`, pegada
entera en el mensaje.

**Se le dijo antes de ejecutar** —y aun así la mantuvo— que **era la más cerca de
terminada de las tres que quedaban**: sus dos campañas pasaban todos los umbrales
(98,09 % de backend con `ExecuteAutomations` al 95,08 % contra el 0,90 exigido, y
90,29 % de frontend), sus 22 motivos bloqueantes estaban cerrados y de las 8
condiciones de su juez ya había 4 cerradas.

**Alcance:** desaparecen las reglas de automatización, el ejecutor, la simulación,
la auditoría y su pantalla. Es más autocontenida que los conectores: tiene su
propia migración (`V28__automations.sql`) y **consume** de la feature 25
(`WebhookEndpointLookup`, el outbox) pero **nadie consume de ella** salvo el
cableado de `ApplicationConfiguration` y el mapeo de errores de `ApiErrors`.

`feature_list.json` pasa a **27 features**. Quedan **dos** por cerrar: la **25**
(webhooks) y la **28** (calendario externo).

**Consecuencia que conviene tener escrita:** con la 30 fuera, la feature 25 pierde
a su único consumidor interno. Los webhooks siguen sirviendo para lo que fueron
diseñados —notificar a terceros los eventos del outbox— pero ya no hay ninguna
regla dentro del producto que los dispare. No es un defecto; es un cambio en para
qué sirve la feature, y quien la lea dentro de seis meses merece saberlo.

---

## R10 — 10 de septiembre de 2026 — el dato adicional autenticado ata también al propietario

**Pregunta:** «El escenario `@s8` dice que el dato adicional autenticado del
cifrado es "el id del endpoint", pero producción usa el id del propietario más el
del endpoint. La línea del contrato, leída al pie, es falsa.»

**Opción elegida:** «Enmendar la línea», descrita así: _el contrato pasa a nombrar
propietario + endpoint; es la política que ya ratificaste en `project-spec.md`, y
el `.feature` nunca recibió su nota fechada. Producción no se toca: es más segura
que lo que el contrato pide._

**Alcanza a:** `features/webhooks.feature`, cláusulas del `@s8`. Se añade además
la fila que faltaba —descifrar con el id de **otro propietario** falla—, que es el
oráculo de la mitad que el contrato no cubría.

---

## R11 — 10 de septiembre de 2026 — una clave malformada impide arrancar

**Pregunta:** «El escenario `@s9` promete que "la aplicación queda disponible" con
una clave de cifrado malformada ("base64 de 31 bytes" y "texto no base64"). El
código lanza en los dos casos y el contexto ni arranca. Dos filas del contrato
describen algo que no ocurre.»

**Opción elegida:** «Enmendar las dos filas», descrita así: _el contrato pasa a
decir lo que el código hace: una clave **presente pero inválida** impide arrancar,
que es un error del operador y debe verse al instante. El modo degradado se
reserva para la clave **ausente**, que ya está descrito y ratificado._

**Alcanza a:** las dos filas del `Examples` del `@s9`, que pasan a `ausente`.

---

## R12 — 9 de septiembre de 2026 — el plazo de 5 s cubre el intercambio completo (feature 28)

**No hizo falta preguntar de nuevo: ya estaba ratificada.** Se cerró por cita el
10 de septiembre, al comprobar que la respuesta «Ratifico las dos» alcanzaba
también a ésta.

La opción que el propietario eligió describía esta enmienda con estas palabras:

> La de la 28 amplía su contrato con el plazo de lectura del cuerpo del feed, que
> cierra un agujero de recursos.

**Alcanza a:** `features/external_calendar.feature:13-14` y la fila del `@s12` del
proveedor que gotea el cuerpo. Introducida por el commit `baf5ab1f`.

---

## R13 — 9 de septiembre de 2026 — el certificado que no vale para su nombre (feature 28)

**Tampoco hizo falta preguntar: es la prueba de TLS que el propietario pidió.**

**Pregunta original:** «Dos carriles decidieron lo contrario sobre el reenlace
DNS: webhooks revocó el anclaje a la dirección validada (viaja por nombre) y el
calendario externo lo implementó. ¿Cuál manda?»

**Opción elegida:** «Anclar, y probar el TLS», descrita así:

> Se conecta a la dirección ya validada en **las dos features**, que es lo que
> pedía la enmienda B3 original, y **se escribe la prueba de TLS/SNI que hoy no
> existe**.

La fila que el commit `78dca3a6` añadió al `@s12` —un certificado que no es válido
para su nombre da `FEED_UNREACHABLE`— **es** esa prueba. Y es indispensable: si se
ancla la dirección, hay que comprobar que el certificado corresponde al nombre, o
el anclaje deja de ser una defensa y pasa a ser un agujero.

**Con R12 y R13, la feature 28 se queda sin decisiones de contrato pendientes.**
