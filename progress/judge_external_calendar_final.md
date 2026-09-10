# Review de cierre — feature 28, calendario externo

**Veredicto: CHANGES_REQUESTED — 1 condición bloqueante, 5 no bloqueantes.**
10 de septiembre de 2026, sobre `main` en `b925f353`, árbol limpio.

De las 16 condiciones de `progress/cierre_28.md` quedan **6**, y de las **10
bloqueantes queda una**: la mitad del ámbito de Stryker que nadie tocó y que la
guarda del arnés sigue **obligando**. Todo lo demás está cerrado, y lo he
comprobado recomputando los artefactos, no leyendo las actas.

No apruebo, pero conviene decirlo con claridad: **este cierre es el más honesto
que ha pasado por mi mesa en esta feature**. Las dos previsiones aritméticas del
carril salieron **exactas al mutante** cuando las campañas se remidieron de
verdad. Eso no se finge.

---

## Lo que he recomputado yo, no leído

| Qué | Fuente | Mi cálculo | Lo publicado |
|---|---|---|---|
| Mutación backend | `pitest-external-calendar/mutations.xml` (19:34) | 445 KILLED / 467, **95,29 %**, 0 TIMED_OUT | 95,29 % ok |
| Por capa (8 capas) | el mismo XML | feed 40/47 **85,1**, net 19/22 86,4, application 80/87 92,0, domain 178/182 97,8, http 57/58 98,3, persistence 39/39, connectors 30/30, logging 2/2 | idéntico ok |
| Mutación frontend | `mutation-external-calendar/mutation.json` (19:45) | 636 muertos / 686 válidos = **92,71 %** (crudos 636/688; los 2 `RuntimeError` los excluye Stryker del denominador, como es su convención) | 92,71 % ok |
| Por fichero | el mismo JSON | external-calendar.tsx 220/250 **88,00**, today-external-calendar.tsx 77/81 95,06, api 310/326 95,09, App.tsx 24/24, workspace.tsx 5/5 | idéntico ok |
| Suite de backend | 205 XML de `backend/build/test-results/test/` (19:14) | **4201 pruebas, 0 fallos, 0 errores, 0 saltadas** | «backend completo verde» ok |
| Guardas del arnés | `node --test scripts/project.test.mjs`, ejecutado por mí ahora | **89/89 verde** | 89/89 ok |

Procedencia: el último commit que toca `backend/src` o `frontend/src` es
`fe5bc7ca`, **18:43:41**. Las pruebas son de las 19:14, PIT de las 19:34 y
Stryker de las 19:45. Las tres medidas son del árbol que se cierra, y los SHA que
citan las actas (`354f36dc`, `9b9c2dca`) **existen** — a diferencia del `ec0a3b8`
del acta vieja. La suite de frontend no deja artefacto, pero una campaña de
Stryker que **termina** implica que su corrida inicial fue verde: sin ella aborta.

---

## Cobertura de escenarios (@s ↔ test)

Los 40 `@s` tienen prueba en los ficheros de esta feature (24 clases de backend,
4 de frontend, 3 de E2E). Los que re-verifiqué uno a uno, por ser los señalados:

- **@s35** (`onlyIfStale`): **cerrado y medido**. `today-external-calendar.tsx:59`
  (`BooleanLiteral -> false`) sale **Killed** en la campaña nueva. Era el oráculo
  que no podía fallar: el doble de `fetch` apilaba método y URL pero nunca el
  cuerpo. Ahora afirma `{ onlyIfStale: true }`.
- **@s36**: `today:104` y `today:105` (el literal del aviso y el separador
  `{" "}`) salen **Killed**.
- **@s9** (`ConnectorKeyRing:55`): cerrado. El oráculo usaba
  `contains("APP_CONNECTOR_KEY")`, subcadena de `APP_CONNECTOR_KEY_PREVIOUS`, así
  que no podía distinguir las dos ramas del ternario. Hoy
  `ExternalCalendarWiringTest.java:43,47,63,65` afirma el paréntesis
  `(APP_CONNECTOR_KEY)` **y** que la otra variable no aparece, por los dos lados.
  El mutante sale muerto: `adapter.connectors` 30/30.
- **@s12**, las dos filas nuevas: el certificado que no vale para su nombre tiene
  oráculo en `HttpCalendarFeedTest.java:601`, y el cuerpo que gotea sin cerrarse
  en `:191`. La fila del proveedor que enmudece, en `:237`.
- **@s37**: la entrada de navegación puntúa 5/5 en `workspace.tsx:82`.

**Ningún `@s` queda sin oráculo.**

---

## Disciplina TDD

- **¿Producción sin test que la pida?** `git diff b11859ef HEAD` (el commit de mi
  veredicto anterior) sobre `frontend/src/external-calendar*.tsx|ts`,
  `today-external-calendar.tsx`, `today.tsx`, `adapter/feed`,
  `SyncExternalCalendar.java` e `IcsFeed.java`: **vacío**. Los 21 mutantes que
  pasaron de vivos a muertos se mataron **poniendo oráculo**, no cambiando
  comportamiento. Lo único que cambió en `adapter/connectors` son las **bajas** de
  las clases de las features retiradas.
- **¿Rojo -> verde -> refactor?** Sí, y con la acreditación más fuerte
  disponible: las dos campañas se remidieron después y **confirmaron las
  previsiones al mutante**. Backend previó 445/467 = 95,29 % y midió 445/467 =
  95,29 %, con las cuatro capas que debían subir subiendo lo previsto. Frontend
  previó `external-calendar.tsx` 220/250 y `today` 77/81, y eso midió. Una
  previsión que acierta el denominador **y** la identidad de cada superviviente no
  se escribe de memoria.

---

## C5 — veredicto por superviviente: cerrado, y verificado por identidad

No me basta con que el acta diga «uno a uno»; comparé **la lista medida** con la
lista razonada.

**Backend, 22 sin matar.** `progress/mutacion_external_calendar_backend.md` da
13 equivalentes + 3 de código muerto + 6 abiertos = 22, y **coinciden clase,
línea y mutador con el XML** uno a uno: `AnchoredConnection:48/57/76`,
`HttpCalendarFeed:159x2/202/203/207/210/218`, `IcsFeed$Property:144/148/153/158`,
`PublicAddressPolicy:55/60/63`, `$Cidr:67x2/70/78`,
`ExternalCalendarController:163`.

**Frontend, 52 sin matar.** `progress/mutacion_external_calendar_frontend.md` da
21 equivalentes + 29 abiertos + 2 `RuntimeError` = 52, y el conjunto de líneas
coincide **exactamente** con el JSON: api 16 (66, 67, 73, 78, 79, 94, 160, 164,
167x2, 170, 172x2, 173, 177, 178), external-calendar.tsx 30, today 6.

Dos correcciones del carril a mi veredicto anterior que **acepto y registro**,
porque las comprobé en el fuente: `today:105` no era el mensaje sino el separador
`{" "}` (sin él se lee «…calendario externo.Revisar el calendario externo»), y de
`:114`/`:118`/`:122` sólo `:118` es `setEvents([])`; los otros dos son arrays de
dependencias de `useCallback`. Y la objeción a la condición B1 es buena:
`HttpCalendarFeed:210` **no se puede matar** porque el `catch` de `:158` mapea la
`IOException` al mismo `FEED_UNREACHABLE`; la exigencia estaba mal redactada, no
faltaba prueba.

---

## C3 — el ámbito de PIT apunta a lo que tiene que apuntar

Las **quince** clases obligatorias de `judge_external_calendar_cierre.md:229-247`
generan mutantes: `HttpCalendarFeed` 47, `PostgresExternalCalendarStore` 39,
`ExternalCalendarController` 39 (+6 en sus dos records), `IcsFeed` 54 (+32 en
`$Property`), `ExternalCalendarInput` 28, `SyncExternalCalendar` 23 (+4),
`AnchoredConnection` 21, `AesGcmSecretCipher` 17, `ConnectorsGate` 13,
`ExternalEventsRange` 13, `ConnectorKeyRing` 12, `PublicAddressPolicy` 11 (+20 en
`$Cidr`), `SaveExternalCalendar` 11, `ExternalCalendarSnapshot` 10,
`OutboundHostGuard` 6, `ReadExternalCalendarEvents` 2. `ApplicationConfiguration`
**no aparece ni una vez**. Cero `TIMED_OUT` en todo el informe, así que la prueba
del proveedor que enmudece no envenenó `:195/:196/:210/:223`, que era mi duda.

---

## B5 — la declaración de imposibilidad es honesta

Verificado en el árbol, no heredado:

- `backend/build.gradle.kts:37` fija `jdk.httpclient.allowRestrictedHeaders=host`
  en el JVM de `test`, y los `jvmArgs` de `pitest` (`:769`) hacen lo mismo.
- `HttpCalendarFeed.java:145-148` es el `catch (IllegalArgumentException)` del
  `HttpRequest.newBuilder(...).header("Host", ...)`. Sale `NO_COVERAGE`.
- La lista de cabeceras restringidas del cliente del JDK se congela en el
  **inicializador estático** de sus utilidades, la primera vez que alguien
  construye una petición: por eso no se puede quitar la propiedad en caliente. El
  propio comentario de `build.gradle.kts:31-36` lo explicaba antes de que nadie lo
  objetara, y los dos programas de una sola clase del carril miden la congelación
  **en las dos direcciones**.
- Y no hay segundo disparador: la URI ya está validada como `https` con host de
  nombre, así que ese `catch` sólo lo alcanza la cabecera restringida.

**Conclusión: la rama es inalcanzable en proceso; hace falta la tarea forkeada
`unrestrictedHostTest`.** Sigue no bloqueante, por lo mismo que antes: la mitad
TLS de C1(a) está cumplida (`HttpCalendarFeedTest:578`, `:601`, `:616`) y el otro
disparador del mismo `catch` tiene oráculo en `:501`.

---

## Las decisiones de contrato: cerradas, y las citas sostienen

El diff del contrato **desde la destilación aprobada** (`6be97616`) hasta hoy son
**exactamente dos enmiendas**, ni una más: la cabecera `:13-14` (`baf5ab1f`) y la
fila del certificado (`78dca3a6`). Las dos llevan **nota fechada dentro del
`.feature`** (`:15-21` y `:206-212`) y entrada en `progress/ratificaciones.md`
(R12 y R13) con la pregunta y la opción **citadas literalmente**. No hay ninguna
enmienda colada sin contrafirma.

Sobre si las citas alcanzan lo enmendado, que es lo que se me pide juzgar:

- **R13 sostiene sin discusión.** El propietario eligió «Anclar, y probar el
  TLS», opción cuyo texto dice «se escribe la prueba de TLS/SNI que hoy no
  existe». La fila añadida **es** esa prueba.
- **R12 sostiene, y explico por qué aunque la cita diga menos que la enmienda.**
  La opción hablaba «del plazo de lectura del cuerpo del feed»; la enmienda
  escribe «conexión, cabeceras y lectura del cuerpo». La parte de más no es
  contrato nuevo: la fila `| 200 tras 6 s sin enviar cabeceras | FEED_UNREACHABLE |`
  (`:213`) **ya venía de la destilación aprobada** —lo confirma `git blame`—, o
  sea que el plazo sobre las cabeceras ya estaba pactado. Lo único que la enmienda
  añade es justo lo que la cita nombra, con su mismo motivo («cierra un agujero de
  recursos»).
- La tercera «decisión» de mi lista anterior —la fila del proveedor que enmudece—
  resultó no ser una enmienda: es de `6be97616`, la destilación aprobada. Y la
  cuarta —el sitio en el orden de navegación— la disolvió la retirada de las tres
  features que discutían ese orden.

---

## La herencia de los conectores: sin hueco de contrato, con tres citas muertas

`SecretCipher`, `AesGcmSecretCipher`, `ConnectorKeyRing` y `ConnectorsGate` pasan
a ser código de la 28 y **el contrato los cubre**: la cabecera `:6-7` fija
AES-256-GCM, nonce de 12 bytes y `owner_id` como dato adicional autenticado;
`@s2` y `@s3` prueban la ida y vuelta y que dos cifrados de la misma URL difieren;
`@s8` las cinco rutas sin clave; `@s9` el arranque. En mutación: `connectors`
30/30 y `ConnectorsGate` 13/13. `ConnectorsGate.PREFIX` ya es sólo
`/api/v1/me/external-calendar`, o sea que la puerta se estrechó con la cirugía en
vez de quedarse ancha. **No hay hueco de oráculo.**

Lo que sí quedó son **citas a un contrato que ya no existe**:
`AesGcmSecretCipher.java:16` justifica el formato del criptograma con «@s1 de la
feature 27», `SecretCipher.java:7` habla del «token personal de la feature 27» y
`ExternalCalendarWiringTest.java:34` dice que el cifrado «lo provee la feature
27». Es papel, no comportamiento — pero es papel que apunta a un fichero borrado.

---

## Lo que falta

### 1. BLOQUEANTE — el ámbito de Stryker sigue puntuando diez features ajenas, y la guarda lo obliga · orquestador · 30 min, sin máquina

Es la mitad no hecha de la condición 4. El tramo de *render* sí se estrechó
(`102:10-161:7` -> `App.tsx:82:10-83:28`), y de ahí sale casi toda la bajada de 64
a 24 mutantes. El tramo de *section* **no**: `src/App.tsx:53:12-75:38` empieza en
`: externalCalendar` y termina en `: null`, o sea que se sigue tragando la cadena
ternaria entera. Medido en el `mutation.json` de hoy: de los 24 mutantes de
`App.tsx`, **sólo 8 son de esta feature** (`:43` la ruta x4, `:54` la etiqueta
«Calendario externo» x1, `:82` la rama de render x3). Los **16 restantes** son
«API para integraciones», «Importación», «Calendario», «Exportación»,
«Apariencia», `route === "/"`, «Hoy», «Revisión semanal», «Historial»,
«Disponibilidad», `route.startsWith("/proyectos")` y «Proyectos»: líneas 56, 58,
60, 62, 64, 65x4, 66, 68, 70, 72, 73x2 y 74.

Y no es un descuido que se arregle solo: `scripts/project.test.mjs:2146` sigue
afirmando `assert.match(section, /: null$/)`, que es **exactamente** la cláusula
que señalé, y **exige** el rango ancho. Lo acabo de ejecutar: la guarda pasa en
verde con el ámbito ancho puesto. Mientras esa línea siga ahí, ninguna medida
futura de la 28 podrá ser suya.

Por eso no lo doy por cerrado, aunque **no cambia la puerta**: excluyendo esos 16
—todos `Killed`— la cifra honesta es **620/670 = 92,54 %**, y `App.tsx` propio
queda 8/8. El acta `mutation_external_calendar_frontend.md:9-12` dice que los
rangos «se recalcularon derivándolos del texto que deben cubrir»; es verdad para
tres de los cuatro.

**El trabajo, exacto y sin campaña:**

1. `frontend/stryker.external-calendar.config.json`: `src/App.tsx:53:12-75:38` ->
   `src/App.tsx:53:12-54:35`, la forma estrecha de las hermanas (`ics-calendar`
   usa `71:22-72:36`).
2. `scripts/project.test.mjs:2146`: sustituir `/: null$/` por el final del tramo
   propio — `assert.match(section, /^externalCalendar\n\s+\? "Calendario externo"$/)`.
3. Republicar la cifra **recomputada del mismo `mutation.json`** restringido al
   ámbito nuevo: **620/670 = 92,54 %**, dejando escrito que los 16 excluidos
   estaban todos `Killed`. No es previsión: es una subselección de resultados ya
   medidos, que cualquiera recomputa del artefacto que está en disco. **No hay que
   relanzar Stryker.**

Y una constatación que cierra el otro trozo de la condición 4, esta vez **a
favor**: añadir `today.tsx:226-231` al `mutate` es innecesario. Ese montaje son
cuatro atributos JSX cuyos valores son accesos a propiedades; Stryker no genera ni
un mutante ahí. Y quitarlo entero lo detecta `today-external-section.test.tsx`,
que monta `<Today />` y exige la región «Calendario externo». **Retiro esa
exigencia.**

### 2. `progress/current.md` publica «remidiéndose» las dos puertas que ya están medidas · orquestador · 10 min

`:25` dice «remidiéndose | remidiéndose» y `:30-32` lo explica en presente. El
fichero es de las 19:24; PIT terminó a las 19:34 y Stryker a las 19:45. Poner
95,29 % y 92,71 % (o 92,54 % con el ámbito ya arreglado) con su acta.

### 3. Tres actas de frontend conviviendo, y una cita un commit que no existe · orquestador · 15 min

`progress/mutacion_external_calendar_frontend_medida.md:13` sigue diciendo «Sobre
`main` en `ec0a3b8`», que `git cat-file -t` no resuelve. Conviven
`mutacion_..._frontend_medida.md` (91,32 %), `mutacion_..._frontend.md` (los 65
supervivientes) y `mutation_..._frontend.md` (92,71 %), y ninguna dice cuál manda
— nótese que dos sólo se distinguen por `mutacion`/`mutation`. Marcar la primera
como **caducada** y que la vigente enlace a la de los veredictos por
superviviente, que es la que hace el trabajo. Es la condición 12, más el «que no
queden dos verdades» que el propio plan de cierre se puso.

### 4. Coordenadas que no recomputan · orquestador · 10 min

`progress/bloqueantes_external_calendar.md:210` dice `:764` y
`progress/carriles/bloqueantes_28.md:58` dice `:744` para los `jvmArgs` de pitest
(están en `backend/build.gradle.kts:769`), y el mismo fichero `:42` sitúa la
guarda de Stryker en `2144-2154` (empieza en `:2122`). Condición 13, sin hacer.

### 5. La excepción de `DeleteExternalCalendar` sigue sin registrar · orquestador · 10 min

No está en ninguna de las dos actas de backend. Sale con **cero mutantes** y hay
que dejar escrito por qué —su único cuerpo es una llamada no-void descartada, que
ningún mutador por defecto toca— y que **no viola C3**, porque no está en la lista
obligatoria del juez. Condición 15: si no se escribe, el próximo juez la abre otra
vez.

### 6. `PublicAddressPolicy.unmap`: código muerto medido, con un javadoc que promete lo que no hace · carril · 15 min, y arrastra remedida

`:48` lo llama y `:54-65` lo define; sus tres mutantes (`:55` `SURVIVED`, `:60` y
`:63` `NO_COVERAGE`) están medidos como inalcanzables porque el JDK ya devuelve
`Inet4Address` para la forma mapeada. El javadoc `:53` («Solo la forma mapeada se
normaliza») y `project-spec.md:2492` («normalizando antes las formas mapeada,
compatible, 6to4 y NAT64») describen una normalización que no ocurre.

**No lo hago bloqueante, y digo por qué**: no es alcance inflado sin contrato —la
fila `::ffff:10.0.0.1` de `@s11` (`feature:186`) existe y `AddressPolicyTest` la
ejerce; el código es una duplicación defensiva de algo que el JDK ya hace. Pero
borrarlo cambia el denominador de PIT (467 -> 464) y obliga a remedir, así que
**hágase junto a otro cambio de backend, no solo**. Si se decide no borrarlo, hay
que corregir las dos frases, que hoy son falsas.

---

## Checkpoints

- **C1** — [x] arnés completo. `bin/harness init` no lo relanzo yo, pero sus tres
  piezas están verdes por artefacto: lint (commit `17b823f9`), 89/89 guardas
  (ejecutadas por mí ahora) y 4201 pruebas de backend sin un fallo.
- **C2** — [ ] hay **dos** features en `in_progress` (25 y 28). Es estado de
  proyecto, no defecto de ésta; la 28 sigue correctamente **sin** marcar `done`.
  `progress/current.md` describe la sesión viva, pero con dos líneas caducadas
  (condición 2 de arriba).
- **C3** — [x] capas y dependencias respetadas; la cirugía estrechó
  `ConnectorsGate` en vez de dejarlo ancho.
- **C4** — [x] verificación real: 4201 pruebas de backend verdes; 30 ficheros y
  1124 pruebas de frontend inventariadas por Stryker sobre este ámbito.
- **C5** — [x] árbol limpio (`git status` vacío), sin mutantes olvidados en
  producción.
- **C6** — [ ] los 40 `@s` tienen prueba y no hay producción sin oráculo que la
  pida; **falla sólo por el mapa**: `progress/tdd_external_calendar.md` no
  incorpora los oráculos nuevos del último carril (@s35, @s12 TLS, @s9). Se cierra
  con la condición 3 de arriba.
- **C7** — [x] las dos puertas por encima del umbral, recomputadas por mí de los
  artefactos, con acta nominal y veredicto por superviviente en las dos.

---

## Marcador de las 16 condiciones anteriores

| # | Condición | Bloq. | Hoy |
|---|---|---|---|
| 1 | Las cuatro decisiones, donde el propietario lee | SÍ | **CERRADA** por cita (R12/R13) + dos disueltas |
| 2 | Contrafirma de la ampliación de contrato | SÍ | **CERRADA**: notas fechadas en el `.feature`, citas literales |
| 3 | `@s35` sin oráculo que pueda fallar | SÍ | **CERRADA**: `today:59` sale `Killed` |
| 4 | Ámbito de Stryker que puntúa otras features | SÍ | **ABIERTA a medias**: render sí, section no, guarda intacta |
| 5 | Guarda del arnés verde con el ámbito nuevo | SÍ | **CERRADA**: 89/89 ejecutado por mí |
| 6 | Remedir el frontend | SÍ | **CERRADA**: 92,71 % recomputado del JSON |
| 7 | C3, PIT con el ámbito corregido, 80 % por capa | SÍ | **CERRADA**: 95,29 %, la peor capa 85,1 % |
| 8 | C5 frontend, veredicto por superviviente | SÍ | **CERRADA**: 52 de 52, verificado por identidad |
| 9 | C5 backend, acta inexistente | SÍ | **CERRADA**: 22 de 22, verificado por identidad |
| 10 | Los seis oráculos nuevos en verde en `main` | SÍ | **CERRADA**: suite completa, 0 fallos |
| 11 | `current.md` publica una puerta que se desmontaba | no | **ABIERTA** (ahora al revés: dice «remidiéndose») |
| 12 | Acta que cita un commit inexistente | no | **ABIERTA** |
| 13 | Coordenadas que no recomputan | no | **ABIERTA** |
| 14 | B5, la cabecera `Host` restringida | no | **ABIERTA**, imposibilidad **verificada y honesta** |
| 15 | Excepción de `DeleteExternalCalendar` | no | **ABIERTA** |
| 16 | Reserva de `adapter.feed` | no | **NO APLICA**: 85,1 % |

**Con la condición 1 hecha —30 minutos de papel y configuración, sin una sola
campaña— esta feature se cierra.**
