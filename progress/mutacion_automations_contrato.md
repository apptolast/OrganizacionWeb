# Carril «cobertura del contrato» — feature 30, automatizaciones (M1..M7)

Rama `claude/auto-contrato`, worktree `C:/Users/vhurt/ow-worktrees/auto-contrato`.
Origen del encargo: `progress/carriles/bloqueantes_30.md`, lente «Cobertura del
contrato». Lo que sigue está medido: cada rojo se acreditó rompiendo producción
a propósito con la prueba ya escrita, y el texto pegado es el del fallo real.

**Resultado: 6 motivos cerrados (M1, M2, M3, M4, M5, M7), 1 cerrado como
documentación con su premisa corregida (M6). 1 defecto de producto encontrado y
arreglado (M2).**

Nota de método: `bloqueantes_30.md` no existe en esta rama —se escribió en
`main` después de que naciera el worktree—. Lo leí desde el repo principal. No
he hecho `fetch` ni `rebase`; si hace falta poner la rama al día, lo decide el
centro.

Ficheros tocados en toda la rama:

    backend/src/test/.../application/SimulateAutomationTest.java   (M4)
    backend/src/test/.../application/ExecuteAutomationsTest.java   (M7b)
    backend/src/test/.../adapter/AutomationsApiTest.java           (M5, M7a)
    backend/src/test/.../adapter/config/AutomationWiringTest.java  (M1)
    frontend/src/automations.test.tsx                              (M2, M3)
    frontend/src/automations.tsx                                   (M2, único cambio de producción)

---

## M1 — @s4 fila 2: un proyecto `completed` propio sigue siendo destino válido

**Agujero.** La fila 2 de @s4 («C completed -> 201 y la regla se guarda») no
tenía oráculo en ningún sitio del árbol. La única producción que la decide es
el literal SQL del bean `automationTargets`
(`ApplicationConfiguration.java:672`), cuyo Javadoc dice «whatever its status»;
y el helper `project(String owner, String status)` de `AutomationWiringTest`,
que acepta un estado, jamás había recibido `"completed"`: sus tres llamadas
usaban `"active"`.

**Rojo acreditado.** Añadido a la cadena el ` AND status = 'active'` que el
motivo advierte, con la prueba nueva puesta:

    AutomationWiringTest > s4_aCompletedProjectOfTheOwnerIsStillAValidTargetAtSaveTime() FAILED
        com.apptolast.organization.application.AutomationTargetNotFoundException:
        El proyecto indicado no existe.

Los otros 4 tests de la clase siguieron **verdes** con el filtro puesto: era
exactamente lo que denunciaba el motivo.

**Qué lo cierra.** `AutomationWiringTest.s4_aCompletedProjectOfTheOwnerIsStillAValidTargetAtSaveTime`:
crea el proyecto con estado `completed`, guarda la regla contra los beans reales
sobre Postgres y relee que la regla existe, con versión 1 y con
`action.projectId` apuntando a ese proyecto. Es la fila que separa «error de
guardado» de «fallo determinista de ejecución», y por tanto la premisa de @s21
fila 1 y @s32 fila 1.

**Cerrado.**

---

## M2 — Guardar y el interruptor quedaban inertes para siempre (DEFECTO DE PRODUCTO)

**Agujero, y defecto.** `save()`, `simulate()` y `toggle()` comparten
`writeRequest.current`. Cuando una supera a otra, la perdedora sale por
`if (!mounted.current || writeRequest.current !== controller) return;` y su
`finally` —`if (mounted.current && writeRequest.current === controller)
setSaving(false)`— **no se ejecuta nunca**. `saving` (o `busyToggle`) se queda
en `true` de por vida: `disabled={saving}` en el botón Guardar y la guarda de
entrada `if (!editing || saving) return;` lo dejan muerto con el borrador
atrapado dentro del editor; igual con `disabled={busyToggle === rule.id}` y con
`if (busyToggle) return;`, que además bloquea a **todos** los demás
interruptores, no sólo al suyo.

Las tres pruebas que recorrían literalmente ese camino sólo afirmaban
`queryByRole("alert")` ausente y que el borrador o el texto seguían ahí.
Ninguna miraba el control.

**Rojo acreditado.** Añadidas las aserciones que faltaban, contra la producción
de antes:

    FAIL @s40 releases Guardar when another write supersedes the save
    Error: expect(element).toBeEnabled()
    Received element is not enabled:
      <button disabled="" type="button" />

    FAIL @s40 releases the switch when another write supersedes it
    Error: expect(element).toBeEnabled()
    Received element is not enabled:
      <button aria-checked="true" aria-label="Activar o desactivar Seguimiento"
              class="is-active" disabled="" role="switch" type="button" />

**Qué lo cierra.** Arreglo mínimo en `frontend/src/automations.tsx`: quien
enciende cada indicador de ocupado es su dueño y lo apaga, aunque haya dejado
de ser la escritura vigente (`savingRequest`, `toggleRequest`).
`writeRequest` sigue decidiendo quién puede **tocar los datos**; los flags de
ocupado ya no dependen de esa carrera. Las dos pruebas, además de mirar el
`disabled`, vuelven a pulsar el control y exigen una **segunda** petición: un
`disabled` que se quita sin que la guarda de entrada lo acompañe no bastaría.

**Etiquetas.** Las tres pruebas estaban bajo @s43, cuyos `Examples` son «navega
a /proyectos», «cierra sesión» y «cambia a otra regla»: «otra escritura la
supera» no está en ese Outline, así que inflaban la cobertura aparente de un
escenario con una situación que no le pertenece. Pasan a **@s40**, cuya fila 1
dice «el botón queda deshabilitado **HASTA** la respuesta» —hasta, no para
siempre—, que es justo lo que ahora miden. **No he tocado el `.feature`**; ver
la pregunta 1 al propietario, más abajo.

**Cerrado.**

---

## M3 — @s41: el enlace apuntaba a la tarea con el UUID del proyecto

**Agujero.** El fixture ponía `createdTaskId` igual al id del proyecto
(`PROJECT`, que es también `rule.action.projectId`) y afirmaba
`/proyectos/${PROJECT}/tareas/${PROJECT}`: los dos segmentos del `href` eran el
mismo UUID. Intercambiar los identificadores, o usar `projectId` en los dos, o
`createdTaskId` en los dos, pasaba igual. La cláusula «la fila con
`createdTaskId` contiene un enlace a la tarea creada» no estaba comprobada.

**Rojo acreditado.** Intercambiados los dos segmentos en producción:

    FAIL src/automations.test.tsx > @s41 loads the history in pages with textual state and a link to the task
    Expected the element to have attribute:
      href="/proyectos/11111111-1111-4111-8111-111111111111/tareas/77777777-7777-4777-8777-777777777777"
    Received:
      href="/proyectos/77777777-7777-4777-8777-777777777777/tareas/11111111-1111-4111-8111-111111111111"

**Qué lo cierra.** La tarea creada tiene ahora su propio UUID (`CREATED_TASK`)
y la aserción compara los dos segmentos por separado.

**Cerrado.** (La rama adyacente que el motivo menciona —`NOTIFY_WEBHOOK` con el
segmento de proyecto vacío— es la pregunta 2 al propietario.)

---

## M4 — @s31 filas 3 y 4: la ventana de 100 se afirmaba contra sí misma

**Agujero.** El único sitio que afirmaba el 100 era
`assertThat(requestedLimit).isEqualTo(SimulateAutomation.WINDOW)`: compara el
valor pedido contra la propia constante de producción. Y ningún fixture de
`SimulateAutomationTest` pasaba de 5 eventos, así que ni «evaluados 100 de 130»
ni «100 coincidencias, sin la más antigua» se recorrían jamás.

Causa de raíz: el doble de `AutomationEventTail` **ignoraba** el límite que le
pedían (`return List.copyOf(tail)`). Con un doble que no honra el contrato del
puerto, la ventana no se puede medir por construcción.

**Rojo acreditado.** Bajado `WINDOW` a 50, con las dos pruebas nuevas puestas:

    SimulateAutomationTest > s31_looksAtTheHundredMostRecentEventsAndLeavesTheOlderMatchesOut() FAILED
        org.opentest4j.AssertionFailedError: expected: 100 but was: 50
    SimulateAutomationTest > s31_theHundredAndFirstEventFallsOutsideTheWindow() FAILED
        org.opentest4j.AssertionFailedError: expected: 100 but was: 50

`s30_previewsEveryMatchNewestFirstWithoutTouchingTheRules` —el que hacía la
aserción autorreferencial— siguió **verde** con `WINDOW=50`. Ahí está el motivo,
medido.

**Qué lo cierra.** El doble honra ahora el límite como el adaptador real
(`ORDER BY occurred_at DESC, event_id DESC LIMIT n`), sin cambiar el orden de
entrega para no mover ninguna prueba existente. Dos pruebas nuevas:
`s31_looksAtTheHundredMostRecentEventsAndLeavesTheOlderMatchesOut` (fila 3: 130
eventos, con los 100 más recientes que no casan) y
`s31_theHundredAndFirstEventFallsOutsideTheWindow` (fila 4: 101 eventos que
casan; nombra el más antiguo, así que fija el **borde** y no sólo el tamaño).

**Cerrado.**

---

## M5 — @s36 fila 5: `/simulate` frente al Origin ajeno y al CSRF ausente

**Agujero.** El único test de origen ajeno usaba `PUT
/api/v1/me/automations/{id}` y el único de CSRF ausente usaba `POST
/api/v1/me/automations`. La ruta `/simulate` —la única POST del contrato que no
escribe, y por tanto la candidata natural a que alguien la excluya del guardián
«porque sólo lee»— no se probaba contra ninguna de las dos.

**Rojo acreditado.** Excepcionada `/simulate` de las dos guardas
(`csrf(c -> c.ignoringRequestMatchers("/api/v1/me/automations/simulate"))` y
salida temprana por URI en `OriginGuard`):

    AutomationsApiTest > s36_simulateIsNotExceptedFromTheOriginGuardNorFromCsrf() FAILED
        java.lang.AssertionError: Status expected:<403> but was:<500>

El 500 es del doble de `SimulateAutomationUseCase` devolviendo `null`: la
petición había dejado de ser rechazada y llegaba al controlador, que es
exactamente el fallo que la fila prohíbe. Los otros **84** tests de la clase
siguieron verdes con las guardas quitadas.

**Qué lo cierra.** `AutomationsApiTest.s36_simulateIsNotExceptedFromTheOriginGuardNorFromCsrf`,
con `verifyNoInteractions(simulate)`. Cubre las dos: Origin ajeno (la fila del
contrato) y CSRF ausente (la otra mitad del guardián que tampoco se probaba).

**Cerrado.** Riesgo real bajo —hoy la protección es estructural—, pero la fila
del contrato existe para que siga siéndolo, y ahora la suite lo nota.

---

## M6 — @s42 y la puerta que lo ejecuta

**Corrección recibida del centro, y verificada aquí.** La premisa original de
M6 («toda la evidencia vive sólo en Playwright y no la ejecuta nadie») es falsa
a medias:

- **Cierto:** `bin/harness test` -> `scripts/project.mjs` corre las guardas del
  arnés, el backend y `pnpm --dir frontend test` (vitest). **No** corre
  Playwright. Los ficheros de `e2e/` sólo aparecen en la tarea `lint`, y allí
  como `node --check`.
- **Falso** que no se ejecute: `.github/workflows/harness-ci.yml:53` corre
  `xvfb-run -a pnpm test:e2e` en cada CI, tras `pnpm exec playwright install
  --with-deps chromium`. Verificado leyendo el fichero.

Así que M6 no es un agujero de evidencia sino un **reparto de puertas**: la
puerta local no ejecuta @s42; la de CI sí.

**Lo que sí he verificado (lo que el centro pidió).** Los specs existen, entran
en la lista que corre `scripts/e2e.mjs` (`playwright.config.mjs` declara
`testDir: "./e2e"` sin `testMatch`, luego **todos** los `*.spec.mjs` del
directorio corren), y las cinco cosas de @s42 tienen aserciones que pueden
fallar:

| Cláusula de @s42 | Dónde | ¿Puede fallar? |
|---|---|---|
| Sin desplazamiento horizontal ni contenido cortado | `automations-ux.spec.mjs`, `assertUsable()`: `scroll <= client` y `clipped == []`, medido **por elemento y en los dos ejes** | Sí |
| Controles >= 44x44 px CSS | `assertUsable()` (cuatro anchos x dos temas) y `automations-native-zoom.spec.mjs`, `assertNoClipping()` con `MIN_TARGET = 44` | Sí |
| Alcanzables por teclado en orden lógico | `automations-ux.spec.mjs:456`: lee el **orden del DOM** y lo compara contra el recorrido real con `Tab`, ida y vuelta rotada | Sí |
| Foco visible en cada parada | El mismo test: `ringIsVisible()` exige `:focus-visible`, `outline-style: solid`, `>= 3px` y color no transparente, **una medida por parada**; y `assertFocusIsVisible()` en los cuatro anchos al 200 % | Sí |
| axe sin serious ni critical | `violations).toEqual([])` con `wcag2a/2aa/21aa/22aa` en cada ancho, cada tema, texto al 200 % y zoom nativo. Más estricto que la letra del contrato | Sí |
| Texto al 200 % y zoom nativo | El test de texto al 200 % dobla el `font-size` calculado **y verifica que se dobló**; `automations-native-zoom.spec.mjs` usa `chrome.tabs.setZoom(2)` y **falla** si el zoom no se aplica, en vez de saltarse | Sí |

No son «navegar y sacar captura»: las capturas son evidencia adjunta, las
aserciones son las de arriba. El `Given` de @s42 («lista, editor abierto y
resultados de simulación visibles») se reproduce de verdad en
`openDenseScreen()`, que siembra dos reglas y ejecuta una simulación.

**Lo que sí queda como hueco, dicho en su forma correcta (uno, menor):**

La comparación **recorrido de teclado contra orden del DOM** se hace a un solo
ancho (1280) sobre la pantalla densa. En 320, 768, 1440 y en el zoom nativo se
mide el **anillo** de cada parada, pero no se compara el orden contra el DOM.
Atenuante medido: el bloque `.automations` de `frontend/src/styles.scss` no usa
`order:`, `row-reverse` ni `column-reverse`, así que el orden visual **es** el
del DOM en los cuatro anchos; el riesgo es de regresión futura, no actual. No
lo he cerrado porque cerrarlo bien significa correr el recorrido completo en
cuatro anchos y eso cuadruplica un test que ya tarda, decisión que no es mía.

**Lo que NO apunto como hueco:** `automations-native-zoom.spec.mjs` mide el
editor sobre la **lista vacía**, no el estado denso del `Given`. El propio spec
lo declara por escrito en su cabecera («Queda dicho para que nadie lea esta spec
como cobertura de los tres») y remite a `automations-ux.spec.mjs`, que sí lo
cubre. Eso es honestidad documentada, no un agujero.

**Para el centro, decisión que no es mía:** no he tocado `scripts/project.mjs`
ni `harness.config.json`. Si se quiere que la puerta **local** ejecute @s42, es
un cambio de reparto de puertas con 95 guardas detrás. Mi lectura: no hace
falta, mientras la CI siga corriendo `pnpm test:e2e` y siga siendo bloqueante;
lo que sí conviene es dejar escrito, junto a la condición 4 del juez, que @s42
se cierra con la puerta de CI y no con `bin/harness verify`.

**Cerrado como documentación**, con la premisa corregida. No hay defecto.

---

## M7 — Cláusulas sin oráculo en @s6 y @s17

### M7a — @s6 filas 4 y 5 (criterio de 2000 puntos de código, y criterio vacío)

**Agujero.** Ninguna de las dos había viajado nunca por la API. El único test de
@s6 usaba la fila de las llaves sueltas; `AutomationDraftTest` sólo comprueba
que 2000 se acepta en el **constructor**, y el criterio vacío no se probaba en
ninguna parte, aunque `CreateTaskAction` sí lo permite (la guarda de `isEmpty()`
está sólo sobre `titleTemplate`).

Además, el test de @s6 que ya existía afirma sobre el **eco del stub**
(`jsonPath` sobre lo que el mock devolvió), no sobre lo que el parser produjo.
El oráculo nuevo es el draft que recibe el caso de uso:
`verify(create).create("owner", <draft esperado>)`.

**Rojo acreditado**, una mutación por mitad:

    codePoints() -> value.length()   (cuenta chars, no puntos de código)
      AutomationsApiTest > s6_carriesTheCriterionAtItsLimitAndTheEmptyOneUntouchedToTheUseCase() FAILED
        com.apptolast.organization.domain.ValidationException: Revisa los campos indicados.

    text() -> devuelve null cuando la cadena viene vacía
      AutomationsApiTest > s6_carriesTheCriterionAtItsLimitAndTheEmptyOneUntouchedToTheUseCase() FAILED
        org.mockito.exceptions.verification.opentest4j.ArgumentsAreDifferent:
        Wanted  ... criterionTemplate=<las 2000 unidades> ...
        Actual  ... criterionTemplate=null ...

En los dos casos falló **sólo** esta prueba: 85 de 86 verdes.

**Qué lo cierra.** `AutomationsApiTest.s6_carriesTheCriterionAtItsLimitAndTheEmptyOneUntouchedToTheUseCase`.
Las cadenas mezclan BMP y astrales (`"á".repeat(n/2) + "😀".repeat(n - n/2)`),
así que «exactamente 2000 puntos de código» y «byte a byte» significan algo: un
parser que recorte, trunque, cuente `char` en vez de puntos de código o
convierta `""` en `null` cae aquí.

**Cerrado.**

### M7b — @s17 «las 3 tareas creadas tienen createdAt en ese mismo orden»

**Agujero.** Sólo `assertThat(work.createdTasks()).hasSize(3)`. Las tres tareas
salían byte a byte iguales («Revisar Redactar informe en Marketing»), así que
cualquier permutación —o repetir tres veces la misma tarea— pasaba igual.

**Rojo acreditado**, dos mutaciones:

    1) el título deja de venir de la tarea de SU evento (AutomationRendering):
       ExecuteAutomationsTest > s17_walksInTupleOrderSkippingTheBlockedRowAndTheLateCommitBehindTheCursor() FAILED
       Expecting actual:
         ["Revisar Redactar informe en Marketing", x3]
       to contain exactly (and in same order):
         ["Revisar Primera en Marketing", "Revisar Segunda en Marketing",
          "Revisar Cuarta en Marketing"]

       Falló SÓLO s17: 18 de 19 verdes.

    2) el paseo recorre los eventos al revés (Collections.reverse):
       fallan s15, s17, s20 y s22.

**Qué lo cierra.** Cada evento del paseo trae ahora su propia tarea y su propio
título, y se afirma que las tres llegan en el orden E1, E2, E4 con el contenido
de su evento.

**Lo que sigue sin oráculo, y lo digo:** `AutomationEffect.CreateTask` **no
lleva instante**, así que el `createdAt` real de las tres filas no se compara
contra ningún reloj. Lo que se mide es la condición que lo produce: orden del
paseo y tarea correcta en cada posición. Cerrar el instante exigiría o meter un
reloj en el puerto (cambio de diseño) o una prueba con contenedor que relea
`tasks.created_at`. **Eso queda abierto: es la H3 del juez**, y sigue viva.

**Cerrado en su parte de identidad y orden; abierto en el instante (H3).**

---

## Hallazgo colateral, no pedido, para quien lleve la lente de mutación

Al acreditar el rojo de M7b con la mutación 1 —quitar del render el título de
la tarea del evento— falló **una sola** prueba de `ExecuteAutomationsTest`, la
que yo acababa de reforzar. Es decir: **antes de este carril, ninguna prueba de
esa clase notaba que `{{task.title}}` había dejado de resolverse con el evento
que se está procesando**. El doble de `AutomationFacts` devolvía un único
título (`taskTitle`) para cualquier `taskId`, así que la sustitución era
indistinguible de la identidad. @s18 («la plantilla se resuelve con los valores
vigentes en el instante de ejecución») se apoya en ese mismo doble. No entra en
M1..M7 y no lo he tocado; queda dicho.

---

## Preguntas para el propietario del contrato (no he enmendado el `.feature`)

1. **@s43 y la carrera entre escrituras.** Los `Examples` de @s43 son «navega a
   /proyectos», «cierra sesión» y «cambia a otra regla». «Otra escritura la
   supera» —el camino donde vivía el defecto de M2— no está en ese Outline ni
   en ningún otro; sólo lo roza @s40 fila 1 por el lado del botón. He movido las
   tres pruebas a @s40 porque ahí es donde el contrato dice algo aplicable
   («deshabilitado **hasta** la respuesta»). **¿Debe @s43 ganar una cuarta fila
   de `Examples` para la carrera entre escrituras, o basta con la lectura de
   @s40 fila 1?**
2. **El enlace del historial cuando la regla ya no crea tareas.** En
   `automations.tsx:577-585`, si la regla es `NOTIFY_WEBHOOK` el segmento de
   proyecto se resuelve a `""` y el `href` sale como `/proyectos//tareas/<id>`.
   Hoy es inalcanzable en el camino normal (una regla de webhook no crea
   tareas), pero **sí** es alcanzable tras un PUT que cambie una regla
   `CREATE_TASK` a `NOTIFY_WEBHOOK`: sus ejecuciones antiguas conservan
   `createdTaskId` y pintarían un enlace roto. @s41 dice «un enlace a la tarea
   creada» y no dice qué hacer aquí. **¿Ocultar el enlace, apuntar a la tarea
   sin proyecto, o algo más?** No he inventado comportamiento.

---

## Cómo reproducir lo verde

    cd backend
    ./gradlew test --tests "com.apptolast.organization.application.SimulateAutomationTest" \
                   --tests "com.apptolast.organization.application.ExecuteAutomationsTest" \
                   --tests "com.apptolast.organization.adapter.AutomationsApiTest" \
                   --tests "com.apptolast.organization.adapter.config.AutomationWiringTest"
    ./gradlew spotlessCheck

    cd frontend
    npx vitest run src/automations.test.tsx src/automations-route.test.tsx src/automations-api.test.ts
    npx tsc --noEmit && npx eslint src/automations.tsx src/automations.test.tsx
    npx prettier --check src/automations.tsx src/automations.test.tsx

Todo verde en el último pase: backend BUILD SUCCESSFUL (cuatro clases),
frontend 49 + 35 pruebas, `tsc`, `eslint` y `prettier` limpios.

---

## Lo que NO he hecho, a propósito

- No he lanzado PIT ni Stryker: los mide el centro.
- No he lanzado la suite entera de backend: sólo las cuatro clases tocadas, por
  turnos, para no acaparar contenedores de Testcontainers.
- No he tocado `scripts/project.mjs`, `harness.config.json`, `bin/harness` ni
  ningún `.feature`.
- No he hecho `push`, ni `fetch`, ni `reset`, ni he cambiado de rama.
- Ninguna credencial en pruebas, registros ni commits.
