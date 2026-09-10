# Review final — feature 30 `automations` (los 22 motivos del panel)

**Veredicto:** CHANGES_REQUESTED — **8 condiciones** para `done`.

Árbol juzgado: `main` en **`807e7943`**. `git status` limpio. Ningún fichero de la
feature 30 se ha movido desde el merge del carril de supervivientes (`1d0899af`):
`git diff --stat 1d0899af..HEAD -- "*utomation*"` está vacío, así que este
dictamen describe el árbol de hoy y no otro.

Solo lectura: no he editado nada. **No** he lanzado `bin/harness init` ni la suite
entera de backend (nueve carriles, un PostgreSQL por clase), ni campañas de PIT o
Stryker. Lo que sí he ejecutado, y su resultado:

| qué | resultado |
| --- | --- |
| `node --test scripts/project.test.mjs` (las 95 guardas del arnés) | **95/95 verde** |
| `gradlew test --tests ArchitectureTest` | **verde**, y con él `compileTestJava` de todo el árbol |
| `gradlew test` de `ExecuteAutomationsTest`, `SimulateAutomationTest`, `AutomationTemplateTest`, `Slf4jAutomationAuditTest`, `AutomationMatcherTest` | **verde** |
| `vitest run` de `automations.test.tsx`, `automations-api.test.ts`, `automations-route.test.tsx` | **verde, 85 pruebas** |
| recuento propio de `backend/build/reports/pitest-automations/mutations.xml` | 607 mutantes, 581 KILLED, 14 SURVIVED, 12 NO_COVERAGE = **95,7166 %** |
| recuento propio de `frontend/reports/mutation-automations/mutation.json` | 884 mutantes, 804 Killed + 2 Timeout = **91,1765 %** |
| log de CI de la ejecución `34470737773` | leído para @s42 |

**Límite de método, dicho por delante.** La instrucción de solo lectura me impide
reacreditar los rojos rompiendo producción. He verificado cada rojo declarado
**por construcción**: leyendo el oráculo contra la línea de producción que la
bitácora dice haber mutado, y comprobando que la aserción depende de ella. En
todos los casos que revisé la dependencia se sostiene. Donde no he podido ir más
allá, lo digo en la ficha.

---

## 1. Los 22 motivos, uno a uno

`CERRADO` = existe el test, su oráculo puede fallar, y el arreglo de producción
—si lo hay— es correcto y mínimo.

### Lente cobertura del contrato (M1..M7)

| # | Veredicto | Lo comprobado |
| --- | --- | --- |
| **M1** | **CERRADO** | `AutomationWiringTest.java:131` crea el proyecto con estado `completed` contra Postgres, guarda la regla con los beans reales y relee `action.projectId`. El literal de `ApplicationConfiguration.java:672` no filtra por estado; añadirle `AND status='active'` hace caer esta prueba y ninguna otra. El helper `project(owner,status)` (`:45`) recibe ahora `"completed"`. Oráculo real, no autorreferencial. |
| **M2** | **CERRADO en su mitad grande, ABIERTO en la pequeña** | Arreglo correcto y mínimo: `savingRequest` y `toggleRequest` (`automations.tsx:184-185`, `:277`, `:308-309`, `:340`, `:358-359`) desacoplan el indicador de ocupado de la carrera de `writeRequest`, que sigue decidiendo quién toca los datos. Los oráculos **muerden**: `automations.test.tsx:1092-1094` y `:1156-1161` no se conforman con `toBeEnabled()`, vuelven a pulsar el control y exigen una **segunda** petición, así que un `disabled` que se quita sin que la guarda de entrada lo acompañe no pasaría. La reetiquetación @s43 a @s40 es correcta: verificado en `features/automations.feature:567-571` que los `Examples` de @s43 son los tres que dice, y las tres filas siguen teniendo prueba propia (`automations.test.tsx:514`, `:538`, `:1513`). Lo que queda: ver H1. |
| **M3** | **CERRADO** | `automations.test.tsx:12` introduce `CREATED_TASK` distinto de `PROJECT`, el fixture lo usa en `:474` y la aserción compara los dos segmentos por separado en `:503`. El oráculo ya discrimina un intercambio de identificadores. |
| **M4** | **CERRADO** | La causa de raíz estaba bien diagnosticada: el doble ignoraba el límite. Ahora `SimulateAutomationTest.java:23-41` lo honra como el adaptador real. Las dos pruebas nuevas (`:190`, `:214`) afirman el **100 literal**, no la constante: con `WINDOW=50` caen. `:214` nombra además el evento más antiguo excluido, así que fija el **borde** y no sólo el tamaño. La aserción autorreferencial de `:126` sigue ahí, pero ya no es el único oráculo, y eso está dicho. |
| **M5** | **CERRADO** | `AutomationsApiTest.java:679` cubre las dos mitades —Origin ajeno y CSRF ausente— sobre `/simulate`, con `verifyNoInteractions(simulate)`. Es la fila 5 de @s36 (`features/automations.feature:487`), que hasta hoy no tenía prueba propia. |
| **M6** | **CERRADO como documentación, y la corrección de premisa es cierta** | Verificado en el fichero: `.github/workflows/harness-ci.yml:52` corre `xvfb-run -a pnpm test:e2e` tras instalar chromium; `playwright.config.mjs:3` declara `testDir: "./e2e"` sin `testMatch`, y los tres specs de automatizaciones están en `e2e/`. Confirmado además en el log de CI: los seis tests de @s42 corren y pasan. Es un reparto de puertas, no un agujero de evidencia. Matiz nuevo en H6 de este dictamen. |
| **M7a** | **CERRADO** | `AutomationsApiTest.java:383`. El oráculo es `verify(create).create("owner", <draft esperado>)`, el draft que **sale del parser**, no el eco del stub. Las cadenas mezclan BMP y astrales (`codePoints`, `:367-369`), así que «2000 puntos de código» y «byte a byte» significan algo. Cubre las dos filas: el límite exacto y el criterio vacío. |
| **M7b** | **CERRADO en identidad y orden; ABIERTO en el instante, y lo dice** | `ExecuteAutomationsTest.java:168-199`: cada evento trae ahora su tarea y su título, y se afirma `containsExactly("Revisar Primera...","Revisar Segunda...","Revisar Cuarta...")`. Antes las tres tareas salían byte a byte iguales y cualquier permutación pasaba. El `createdAt` real sigue sin oráculo porque `AutomationEffect.CreateTask` no lleva instante: el carril lo declara abierto y lo remite a H3 del juez anterior. Correcto. El hallazgo colateral que reporta —que el doble de `AutomationFacts` devolvía un único título para cualquier `taskId`, lo que vaciaba también @s18— es real y queda cerrado por el mismo cambio. |

### Lente seguridad y datos (M8..M12)

**M8 — CERRADO, y el arreglo es correcto.** Las dos mitades, cada una contra su
pieza. Adaptador: `PostgresAutomationWork.java:189` lanza
`AutomationEndpointGoneException(notify.endpointId())` y el comentario `:185-188`
razona por qué `affected == 0` no puede ser un reclamo. Oráculo real contra
Postgres en `AutomationWorkPersistenceTest.java:261`, que además afirma que **no
quedó ni entrega ni fila de ejecución**, que es lo que da sentido a la distinción.
Ejecutor: `ExecuteAutomations.java:117-155`; `ExecuteAutomationsTest.java:418`
monta la carrera con el lookup diciendo «activo» y el `FakeWork` reventando en el
commit, y afirma las **dos** reglas del evento, el cursor y la bitácora.
**Terminación de la recursión, verificada a mano y no aceptada de palabra:**
`queue()` lanza con `notify.endpointId()`, y `settledFor` (`:144-155`) casa
exactamente sobre ese id, así que cada pasada resuelve al menos un `Notify` y la
lista mengua. El `FakeWork` (`ExecuteAutomationsTest.java:865-875`) lanza
**antes** de registrar nada, que es fiel a la reversión.

**M9 — CERRADO.** Dos pruebas nuevas con **dos propietarios en la misma base**:
`AutomationWorkPersistenceTest.java:401` (el evento del extraño ocurre **antes**
que el propio a propósito: sin el predicado sería el primero de la lista) y `:288`
(el endpoint del extraño está **activo** a propósito, para que el predicado de
estado no tape al de propietario). Los dos detalles de diseño son los correctos y
sin ellos las pruebas no discriminarían. Los otros dos predicados los cubrían ya
`684536dd` y `866426dc`, y los dos commits existen en el árbol.

**M10 — CERRADO en el aislamiento; la semántica nueva pide ratificación.**
Frontera doble y bien colocada: `ExecuteAutomations.java:61-67` para lo que rompe
antes de haber evento, `:82-89` por candidato, con lo que el
`rows.forEach(work::record)` del catch queda dentro. El puerto gana
`cycleFailed(ownerId, eventId, category)` —sólo identificadores, y `eventId` null
cuando no hay evento que nombrar, que es mejor que callarse—. Dos rojos con
oráculo en `ExecuteAutomationsTest`. **Pero** el arreglo elige **detener** el
recorrido del propietario envenenado en vez de saltárselo, y eso deja una cuenta
parada indefinidamente. Eso no está en el contrato ni en un sentido ni en el otro.
El carril lo pregunta (su pregunta 4) y hace bien; falta la respuesta.
Condición 7.

**M11 — CERRADO.** `PostgresAutomationWork` recibe `Clock` y `queue()` sella con
`clock.instant()` (`:169`). El oráculo (`AutomationWorkPersistenceTest.java:320`)
afirma las tres marcas, y **discrimina**: `NOW` es `2026-09-09T08:30:00Z` y los
eventos de la clase viven en `T0 = 2026-09-08T10:00:00Z`. Con el sello viejo la
prueba cae.

**M12 — CERRADO.** `controlIdOf` (`automations.tsx:636-644`) deja de mentir: sin
el `?? "automation-name"`, devuelve `undefined` para el campo que el editor no
muestra. `pinToTheirFields` (`:259-266`) reparte: foco al primer campo **con
control**, y los demás a la página con su nombre delante (`UNSHOWN_LABELS`,
`:650-655`). El oráculo (`automations.test.tsx:257`) afirma las dos cosas —que
aparece la alerta y que Nombre **no** recibe foco ni `aria-invalid`—, así que una
regresión a la rama por defecto lo tumba. Verificado además que
`editing.projectId` y `editing.estimatedMinutes` sólo alimentan `actionOf`
(`:131`, `:136`) y no se pintan en ningún sitio: la clasificación de M22 se apoya
en eso y es cierta.

### Lente mutación (M13..M22)

**M13 — CERRADO para el backend, ABIERTO para el frontend.** Conté yo mismo el
XML: 607 mutantes, 581 KILLED, 14 SURVIVED, 12 NO_COVERAGE, y extraje los 26 no
muertos con clase, método, línea y mutador. **Coinciden uno a uno con la lista de
la sección 2 del documento, sin sobras ni faltas.** Eso es lo que la condición 2
pedía y no existía en ninguna parte. En el frontend no: el informe tiene **78** no
muertos (77 en `automations.tsx` y 1 en `automations-api.ts`) y el documento da
veredicto nominal a **11** —los diez de `editingOf` más el ancla `R8 223`—. El
propio carril lo declara abierto en su punto 5, con dos datos que el motivo no
tenía: 57 anclas sin veredicto en ninguna parte y 27 veredictos cuya ancla ya no
existe. Honesto, pero abierto. Condición 2.

**M14 — CERRADO.** `AutomationsApiTest.java:193`:
`verify(create).create(eq("owner"), eq(conditioned))`. Mira el draft que produce
el parser y no el que entra por el stub, que era exactamente el defecto. El
`jsonPath` del eco se conserva al lado, y está bien que se conserve.

**M15 — CERRADO, y bien construido.** `ExecuteAutomationsTest.java:585`. Un mismo
evento con ejecuciones previas de **dos** reglas, y la de R1 puesta **primera** a
propósito, que es la que `findFirst()` devolvería si el filtro dejara de
discriminar. La aserción compara la **identidad** de la fila (`SIBLING_RUN`) y el
número de intento, no sólo el estado: con el mutante, R2 hereda la fila de R1 y
desaparece del commit. Mata el mutante y mide la cláusula.

**M16 — CERRADO en los cinco kills; H6 del juez anterior sigue abierto y no
congelado.** `AutomationWorkPersistenceTest.java:424` escribe el cursor inicial y
comprueba que un `startCursor` posterior no tira de él hacia atrás: las dos caras
del `ON CONFLICT (owner_id) DO NOTHING`. `:451` comprueba que la fila sobrevive
fuera de la transacción y que el intento siguiente **renueva** la suya en vez de
duplicarla, con `count == 1`. Los dos contra Postgres real. Lo que más me importa:
`:451` afirma **sólo lo que el contrato promete** y no fija como esperado el
pisotón del `upsert` sin guarda de estado, pudiendo haberlo hecho y quedando más
verde. Es la decisión correcta, y es lo contrario de lo que M2 reprochaba en otro
sitio.

**M17 — CERRADO.** Los seis accesores. `AutomationsApiTest.java:534` es @s12 fila
1 sobre el JSON, que era la capa que faltaba: el stub está sobre el puerto y no
sobre el mapper, así que la prueba sí ejerce `AutomationView`. `:783` es @s34 con
`deliveryId` y `createdTaskId` null. Dos de los cambios son correcciones al
contrato y no de gusto, y están bien argumentadas.

**M18 — CERRADO.** Recalculado por mí sobre el XML: **95,7166 %**. El «96,00 %»
era el entero del `index.html`. El 98,68 % va marcado como **proyección**, en
bloque destacado y con el aviso de que la única cifra medida es el 95,72 %. Las
dos cifras del frontend coinciden con mi recuento del JSON al cuarto decimal.

**M19 — CERRADO EN EL EFECTO, INCORRECTO EN LA CAUSA.** Ver H2. El hueco sí se
cierra: `Slf4jAutomationAuditTest` mide el adaptador directamente, usa claves
etiquetadas (`ruleId=`, `eventId=`, `outcome=`...) así que un intercambio de
argumentos cae, y cubre el caso `code=null`. Y `backend/build.gradle.kts:605` mete
la clase en el ámbito, con lo que H7 del juez anterior queda cerrado. Pero la
causa que el documento declara es falsa.

**M20 — CERRADO.** El veredicto es «muerto», que es mejor que un veredicto. El
razonamiento de por qué no se dejó en «justificado» —esa línea es la única huella
que sobrevive a la transacción revertida— es correcto y del tipo que este
repositorio pide.

**M21 — CERRADO.** Los tres arreglos del script son los que hacían falta, y el
segundo es un hallazgo de verdad: el script confundía *nadie falla* con *nadie
corrió* y anotaba `SOBREVIVE` cuando vitest no había llegado a arrancar. Eso
invalida retroactivamente cualquier veredicto del libro viejo, y el documento lo
dice en vez de taparlo. La restauración de fuentes en `exit` y en `SIGINT` cierra
además el riesgo de dejar producción mutada.

**M22 — CERRADO en veredicto; 2 kills abiertos por falta de fila de contrato.**
Verifiqué la partición contra el fuente y no contra la bitácora. El `114:7` es
**equivalente**: la rama verdadera es `(rule.action.criterionTemplate ?? "")` y
para una acción `NOTIFY_WEBHOOK` esa propiedad no existe, vale `undefined`, y el
`?? ""` da `""`, idéntico a la rama falsa (`automations.tsx:113-116`). Los siete
de `projectId` y `estimatedMinutes` son **inobservables**: comprobado que esos dos
valores sólo llegan a `actionOf`, que devuelve intacta la acción de una regla que
no es `CREATE_TASK` (`:126-128`), y que no se pintan en ningún sitio. Los dos
matables están bien identificados, y la negativa a escribir la aserción sin fila
de contrato es la disciplina correcta: sería fijar como esperado un comportamiento
que nadie ha aprobado.

**Recuento: 20 cerrados de verdad, 1 cerrado con la causa mal escrita (M19) y 1
cerrado a medias (M2). Ningún cierre declarado que sea falso de raíz.** Es un
resultado bastante mejor que el que este repositorio ha visto esta semana, y
conviene decirlo con las mismas palabras con que se dicen los fallos.

## 2. Las dos cifras de mutación

Fuera de mi encargo juzgar que estén caducadas. Lo que sí juzgo, y con esto queda
juzgado:

- **Salen del XML y del JSON, no de un HTML.** Confirmado reproduciendo los dos
  recuentos por mi cuenta, con un script propio: 95,7166 % y 91,1765 %, idénticos
  a los publicados.
- **Las líneas del XML casan con el fuente.** Verificado en los sitios que
  importan: `ExecuteAutomations:143` es el filtro `rule.id().equals(run.ruleId())`;
  `AutomationTemplate:36` es el `indexOf(CLOSE, start + OPEN.length())` de
  `render`, y la mutación `+` a `-` revienta de verdad con la plantilla de dos
  marcadores pegados que el carril añadió; y las cuatro líneas de
  `PostgresAutomationWork` (85, 126, 316, 341) caen donde la bitácora dice.
- **La proyección va marcada como proyección**, y el documento repite que sólo el
  95,72 % está medido.
- **Los supervivientes del backend tienen veredicto nominal; los del frontend,
  no.**

## 3. Los cuatro arreglos de producción

Los cuatro son correctos, mínimos y no rompen nada que yo haya podido ver.
`ArchitectureTest` pasa **sin relajarse** —lo he ejecutado—: la excepción nueva
vive en `application`, el `Clock` es un tipo de `java.time`, y el logging sigue
confinado en `adapter.logging` detrás del puerto `AutomationAudit`. El
`compileTestJava` de todo el árbol compila, que es la señal de que los tres
carriles se integraron sin dejar vocabulario suelto.

Dos observaciones de diseño, ninguna bloqueante. `withoutReaching`
(`ExecuteAutomations.java:157-171`) conserva el `attempt` en vez de forzarlo a 1,
que es lo que pide `features/automations.feature:282`; en la práctica vale 1
porque la consulta al endpoint precede a cualquier reintento, y el carril lo
declara. Y la recursión de `confirm` recrea el efecto de las demás reglas en cada
pasada; es correcto porque la anterior revirtió, y sólo cuesta trabajo repetido en
un camino excepcional.

## 4. Cobertura de escenarios

Los 43 `@s` de `features/automations.feature` tienen al menos un fichero de prueba
que los referencia; ninguno se queda sin nada. El mapa cláusula a cláusula lo
hicieron el panel y el veredicto de cierre, y este dictamen no lo repite: se
limita a comprobar que los huecos que aquéllos encontraron están tapados, que es
lo que se me ha encargado. Las cláusulas que **siguen** sin oráculo, todas ya
registradas y ninguna nueva: el `createdAt` de las tres tareas de @s17 (H3 del
juez anterior), la fila 4 de @s12, y el instante que M7b declara abierto.

---

## Hallazgos nuevos

### H1 [MEDIA] M2 se declara cerrado describiendo un defecto que no se cerró

El propio carril escribe que el fallo incluye el `if (busyToggle) return;`, «que
**además bloquea a todos los demás interruptores**, no sólo al suyo»
(`progress/mutacion_automations_contrato.md:70`), y a continuación declara
**Cerrado**. La guarda sigue en `automations.tsx:336` y el `disabled` sigue siendo
`busyToggle === rule.id` (`:453`): mientras una regla está en vuelo, los
interruptores de las demás se pintan **habilitados**, reciben foco, se pueden
pulsar y no hacen nada, sin aviso.

Y está **congelado como conducta esperada** en una prueba anterior a estos
carriles, `automations.test.tsx:1175` («@s40 blocks the second switch while the
first one is still in the air», commit `7ea0fbac`), que afirma literalmente
`expect(second).toBeEnabled()` y después `toHaveLength(0)` sobre la petición del
segundo.

No lo subo de gravedad porque el motivo M2, tal como el panel lo redactó, va sobre
el indicador que se queda en `true` de por vida, y **eso sí está arreglado y bien
medido**. Pero un control habilitado que no responde es la misma familia que @s40
fila 1 prohíbe, y a un juez no le vale que quede congelado en verde.

### H2 [MEDIA] La causa que M19 declara es falsa, y `main` la contradice siete minutos después

`progress/mutation_automations.md:300-305` afirma: «**La causa es el interceptor
`FLOGCALL` de PIT**, activo por defecto y no desactivado en `build.gradle.kts`
(allí sólo se apaga `FRECORD`)». La misma tesis se repite en el Javadoc de
`Slf4jAutomationAuditTest.java:17-23`.

En el árbol de hoy eso no es cierto. `backend/build.gradle.kts:774-788` dice, con
el radio medido, que la causa era el **`avoidCallsTo` por defecto de PIT, que
suprime las llamadas a `org.slf4j`**, y declara la lista explícita **sin**
`org.slf4j`. Es el commit `4212e1ef` («los cinco adaptadores de bitácora dejan de
estar exentos»), de las **13:46**; el merge del carril de supervivientes es
`1d0899af`, de las **13:39**.

Las dos evidencias que el documento aporta —cero `removed call to org/slf4j` en
los seis informes, y el contraste del ternario de `Slf4jExternalCalendarAudit`—
las he reproducido y son ciertas, pero son igual de compatibles con
`avoidCallsTo`, que es literalmente el mecanismo «no mutes las llamadas a estos
paquetes». El documento acierta en el efecto y vuelve a errar en la causa, que es
exactamente el reproche que el propio M19 le hacía a la explicación anterior.

Importa porque ese documento hace de puerta, y porque la afirmación es
**falsable en la próxima campaña**: `progress/plan_campanas.md:34-38` ya la
programa —«si siguen a cero, el arreglo no funcionó»—.

### H3 [BAJA] `Slf4jAutomationAudit.cycleFailed` es producción nueva sin testigo directo

`Slf4jAutomationAudit.java:32-38`, escrita por el carril de datos para M10. No la
cubre `Slf4jAutomationAuditTest`, cuyas dos pruebas son de `runFinished`. Es
precisamente la clase que se creó para tapar el hueco «PIT no vigila esto», y la
línea nueva se queda fuera del remedio: un intercambio de `ownerId` y `category`
en el patrón no lo notaría nadie. Cuesta cinco líneas.

### H4 [BAJA] La condición 3 del veredicto anterior no se ha ejecutado

`progress/tdd_automations_fase2.md` sigue igual en los tres puntos:

- `:245-248` continúa presentando el sueño de 1,5 s como la evidencia de @s15, que
  es lo que H5 desmontó.
- `:473-474` continúa declarando abiertas las cinco filas `NOTIFY_WEBHOOK`, cuando
  el juez comprobó que cuatro estaban cerradas, y desde M1 y M5 hay aún más
  cerrado. Declarar abierto lo que se cerró es la misma infracción de
  `AGENTS.md:51` que declarar cerrado lo que no se midió, y así lo dijo el
  veredicto anterior.
- `:374-375` sigue sin la corrección de @s28 y @s29.

### H5 [BAJA] Un comentario que describe en presente un defecto ya arreglado

`automations.test.tsx:250-255`: «**Hoy** `controlIdOf` no tiene entrada para ese
campo y cae en su `?? "automation-name"`...». Desde `cd41d969` ya no. Es la misma
clase de deuda que H4, en pequeño, y en el sitio donde alguien la leerá primero.

### H6 [BAJA] @s42: el barrido de zoom nativo es parcial en CI y el título promete cuatro anchos

Encargo llegado a mitad de revisión. Lo he mirado, y **el motivo tal como llegó
primero se cae**, que conviene decirlo igual que si se sostuviera.

**Lo que NO es.** `e2e/automations-native-zoom.spec.mjs` **no** puede quedarse
vacío y verde. La guarda de no-vacío existe y está en `:242-245`:

    expect(fits, `ningún ancho del contrato cabe al 200 % en una pantalla de
    ${available} px`).not.toHaveLength(0);

más el `expect(evidence).toHaveLength(fits.length)` de `:292` y el registro de
`{ pantalla, medidos, omitidos }` en `evidence.json` (`:284-291`). Lo puso
`c1882200`, de anoche. El cuerpo del spec es honesto: `:233-238` explica por qué
en una pantalla pequeña hay anchos que no se pueden medir y por qué se declaran
omitidos en vez de fingirlos.

**Lo que sí es.** La guarda exige que quepa **al menos uno**, no los cuatro. Con
el xvfb de CI a 1280 px, `320*2 + cromo` cabe y `768*2 = 1536` no, así que en CI
se recorre **un solo ancho, 320**. El log de la ejecución `34470737773` lo
corrobora: el test tarda **2,1 s**, mientras que `automations-ux.spec.mjs:279`
—que mide expresamente un solo ancho, «zoom nativo de Chromium al 200 % sobre 320
px CSS»— tarda 2,4 s. En CI, la spec de zoom nativo mide lo mismo que su hermana y
su título dice otra cosa: `:150` promete «los cuatro anchos al 200 % de zoom
nativo», y su cabecera (`:14-17`) dice «Esta spec cubre los cuatro con el zoom
nativo puesto». También lo promete
`progress/mutacion_automations_contrato.md:227`, que da por medida
`assertFocusIsVisible()` «en los cuatro anchos al 200 %».

**Y ahora la distinción que importa, porque cambia el veredicto de cobertura.**
@s42 **tal como el contrato lo escribe** empareja los cuatro anchos con «100 %» y
sólo 1440 con «texto 200 %» (`features/automations.feature:553-559`): el zoom
**nativo** no está en esa tabla. Y esos anchos sí se miden enteros en CI, porque
las specs que los recorren usan `setViewportSize` y no dependen de la pantalla
real. Verificado en el log:

- `automations.spec.mjs:106` corre como cuatro tests separados, uno por ancho:
  320, 768, 1280 y 1440, verdes, 1,7 s cada uno.
- `automations-ux.spec.mjs:134` barre los cuatro anchos por dos temas (8,8 s) y
  `:165` los cuatro anchos con texto al 200 % por dos temas (9,3 s), los dos con
  `setViewportSize` (`:143`, `:174`).

**Veredicto: @s42 queda CUBIERTO en lo que el contrato pide, y PARCIALMENTE
CUBIERTO en el excedente de zoom nativo**, que es más estricto que la letra de la
tabla y que en CI se mide sólo en los anchos que caben. No es evidencia inventada
ni vacía: es real para lo que mide.

Lo que hay que corregir es **la afirmación, no la prueba**. No pido forzar 1440 px
en un xvfb de 1280: no se puede, y fingirlo sería peor. El dato que hace falta ya
lo escribe el propio spec en `evidence.json`; sólo falta subirlo al título, a la
cabecera y a la bitácora, como hizo el carril de accesibilidad de la 29 con su
nota de límites. Condición 4.

---

## Condiciones para `done`

**No es «sólo las dos campañas».** Son ocho, y sólo la primera es larga.

**1 [BLOQUEANTE, máquina drenada]. Las dos campañas.** Las número **4** (PIT
automations) y **9** (Stryker automations) de `progress/plan_campanas.md:85,90`,
con sus tres condiciones de arranque y su verificación obligatoria completas:
cifra calculada del XML o del JSON, líneas contrastadas contra el fuente del SHA
anotado, y comprobación de que los tests escritos para matar aparecen como
`killingTest` —si un test existe y no aparece, la campaña es de otro árbol—.
Umbrales del veredicto anterior: 0,80 global y **0,90 sobre `ExecuteAutomations`**;
0,80 sobre `automations.tsx` y `automations-api.ts`. Antes de la de Stryker,
imprimir el texto que cubre cada uno de los cuatro rangos.

**2 [BLOQUEANTE, sale de la 1]. La lista nominal del frontend.** Hoy hay 78
mutantes no muertos y 11 con veredicto escrito. Cerrar los 67 restantes contra el
informe **nuevo**: las líneas actuales están desplazadas por los arreglos de M2 y
M12, así que rehacerlo sobre el informe viejo sería trabajo tirado. El instrumento
ya existe y ya es fiable después de M21.

**3 [BLOQUEANTE, minutos]. Corregir la causa de M19** en
`progress/mutation_automations.md:300-305` y en
`Slf4jAutomationAuditTest.java:17-23`: la causa era el `avoidCallsTo` por defecto,
arreglado en `4212e1ef` (`backend/build.gradle.kts:774-788`), no el interceptor
`FLOGCALL`. Y confirmar en la campaña de la condición 1 que
`Slf4jAutomationAudit` **ya recibe mutantes**; si sigue a cero, decirlo en vez de
dar la condición por cerrada. Es bloqueante porque ese documento hace de puerta y
porque el error es de la misma familia que M18, al que ese mismo documento
persigue.

**4 [no bloqueante, minutos]. Declarar el límite de @s42** (H6): el título de
`e2e/automations-native-zoom.spec.mjs:150`, su cabecera `:14-17` y
`progress/mutacion_automations_contrato.md:227` dejan de prometer «los cuatro
anchos» y pasan a decir «los anchos que caben en la pantalla, con los omitidos
registrados en `evidence.json`». Y dejar escrito, junto a la condición 4 del juez
anterior, que @s42 en su letra —cuatro anchos al 100 % y 1440 con texto al 200 %—
sí se cubre entero, y por la puerta de CI y no por `bin/harness verify`.

**5 [no bloqueante, minutos]. La condición 3 del veredicto anterior**, que nadie
ejecutó: los tres puntos de `progress/tdd_automations_fase2.md` (H4). Añadir el
cuarto: el comentario de `automations.test.tsx:250-255` (H5).

**6 [no bloqueante, minutos]. Oráculo para `cycleFailed`** en
`Slf4jAutomationAuditTest` (H3): que la línea nombre propietario, evento y
categoría por sus claves, y que un `eventId` null salga escrito y no en silencio.

**7 [no bloqueante, puerta humana]. Las decisiones de contrato pendientes**, todas
ya planteadas por los carriles y ninguna inventada por mí. Van juntas porque son
una sola visita al propietario:

1. **@s43 y la carrera entre escrituras**: ¿cuarta fila de `Examples`, o basta la
   lectura de @s40 fila 1? (carril de contrato, pregunta 1).
2. **@s41 y el enlace cuando la regla ya no crea tareas**: `/proyectos//tareas/<id>`
   tras un PUT que cambie `CREATE_TASK` a `NOTIFY_WEBHOOK` (pregunta 2).
3. **@s37 y lo que muestra el editor al abrir una regla de webhook**: el texto ya
   está propuesto en `progress/mutation_automations.md:384`. Desbloquea los dos
   mutantes matables de `editingOf`.
4. **M10 y detener el recorrido del propietario envenenado**, que deja una cuenta
   parada hasta que alguien arregle la fila (carril de datos, pregunta 4). Es la
   única semántica nueva que estos carriles introducen sin respaldo del contrato.
5. **M12 y el editor que compone campos que no enseña** (`action.projectId`,
   `action.estimatedMinutes`): preguntas 1 y 2 del carril de datos. Cerrarlo
   liquida además los siete mutantes inobservables de M22.
6. **H6 del veredicto anterior**, el `upsert` sin guarda de estado: guarda de
   estado o «el último que escribe manda». Se pidió por escrito y sigue sin
   respuesta; la reproducción exacta está en
   `progress/mutation_automations.md:254-264`.
7. **H8 y H9 del veredicto anterior**: decisión escrita, arreglar o justificar.

**8 [no bloqueante, minutos]. La segunda mitad de M2** (H1): o el interruptor en
vuelo deja de bloquear a los demás, o se declara por escrito que bloquearlos es lo
correcto y `automations.test.tsx:1175` pasa a decirlo con esas palabras y con
`aria-disabled`. Hoy la prueba congela un control habilitado que no responde.

---

## Checkpoints

- **C1** [x] — arnés completo. `bin/harness init` no ejecutado por instrucción
  (nueve carriles, un PostgreSQL por clase), pero sus 95 guardas de
  `scripts/project.test.mjs` están verdes y `compileTestJava` compila el árbol
  entero.
- **C2** [x] — una sola feature en `in_progress`: la 30, `automations`.
- **C3** [x] — `ArchitectureTest` verde sin relajar. Sin logs de depuración
  sueltos en los ficheros tocados.
- **C4** [x] — verde en todo lo que he ejecutado. La suite completa queda por
  ejecutar en máquina drenada.
- **C5** [x] — árbol limpio; el carril de mutación no dejó una línea de producción
  tocada.
- **C6** [ ] — los 43 `@s` tienen prueba, pero **falta** la respuesta del
  propietario a las siete decisiones de contrato de la condición 7, dos de las
  cuales (@s37 y @s43) son filas de `Examples` que el contrato no tiene.
- **C7** [ ] — las dos cifras vigentes no describen el árbol, y la lista nominal
  del frontend está al 14 %.

---

## Cierre

Los tres carriles han hecho un trabajo honesto. Lo que me convence no es que los
22 estén marcados como cerrados, sino tres cosas que sólo se ven mirando de cerca:
que M9 pusiera el evento del extraño **antes** que el propio y el endpoint ajeno
**activo** —los dos detalles sin los cuales esas pruebas no discriminarían—; que
M16 escribiera el oráculo de `record` **sin** fijar el defecto de H6 como
esperado, pudiendo haberlo hecho y quedando más verde; y que M22 se negara a matar
dos mutantes matables antes que inventarse una fila de contrato. Ésa es la
disciplina que este repositorio dice tener, y hoy la tiene.

Lo que no firmo hoy no es el trabajo de los carriles. Son dos campañas que aún no
se pueden lanzar, dos tercios de una lista nominal que falta, una causa mal
escrita en el documento que hace de puerta, y siete decisiones que no son de
ningún carril: son del propietario del contrato.
