# C5 (frontend) — veredicto de los 65 supervivientes de la feature 28

Cierra la **condición 8** de `progress/cierre_28.md`: «falta el veredicto escrito de
cada superviviente, y el acta lo confiesa». La confesión está en
`progress/mutacion_external_calendar_frontend_medida.md:27-29` («lo que falta —y es el
trabajo siguiente— es el veredicto escrito de cada uno»). Aquí está, uno a uno.

Carril `cal-bloqueantes`, 10 de septiembre de 2026.

> **Este fichero sustituye a su versión anterior.** Lo que había era la *previsión*
> pre-campaña («111 de 119 mueren») que el panel declaró irreproducible: el único
> artefacto máquina tenía 15 entradas y ninguna era un superviviente, o sea que el
> 87 % de lo publicado no se podía recomputar (`bloqueantes_28.md`, B3). Se reemplaza
> por el veredicto sobre la lista **medida**. La versión anterior sigue en el
> historial de git, que es donde le corresponde estar.

## Sobre qué lista se razona, y por qué vale aunque el ámbito esté por corregir

La medida es la del acta: **91,32 %**, 663 resueltos sobre 726 con veredicto,
`frontend/reports/mutation-external-calendar/mutation.json`. Las condiciones 4, 5 y 6
del veredicto mandan estrechar los dos rangos de `App.tsx` y **remedir**, y son de
otros (orquestador y campaña). Eso **no invalida este trabajo**, y la razón es
aritmética:

`src/App.tsx` puntúa **100,00 %** (55 muertos + 9 por plazo, **0 vivos, 0 sin
cobertura**). Estrechar su rango sólo puede quitar mutantes **muertos**: la lista de
supervivientes es exactamente la misma antes y después del arreglo de ámbito. Baja el
porcentaje publicado —de 91,32 % a 90,48 % (599/662)—, no el trabajo pendiente.

## Resumen del veredicto

| Veredicto | Mutantes |
|---|---|
| **Muertos** por oráculo nuevo de este carril | **13** |
| **Equivalentes o inalcanzables**, con la razón escrita | 21 |
| **Abiertos**, con el trabajo concreto | 29 |
| **Errores de ejecución**, fuera del denominador de Stryker | 2 |

Suman 65. Como en backend, ninguno de los 13 se declara de memoria: por cada uno
**apliqué el mutante al fuente de producción, vi el rojo y lo pegué**, y restauré.

## Los 13 que se matan

| Fichero | Línea | Mutador | Prueba que lo mata |
|---|---|---|---|
| `today-external-calendar.tsx` | 59 | BooleanLiteral → `false` | `@s35 sincroniza con onlyIfStale y luego pide el día completo, en ese orden` |
| `today-external-calendar.tsx` | 27 | BooleanLiteral `hour12` → `true` | `@s35 muestra el evento en hora local y la marca de la última sincronización` |
| `today-external-calendar.tsx` | 105 | StringLiteral → `""` | `@s36 un fallo de red dice que no se ha podido consultar, no que no se ha podido leer` |
| `external-calendar.tsx` | 291 | ArrowFunction → `() => undefined` | `@s38 anuncia Guardando, envía una sola petición y bloquea los controles` |
| `external-calendar.tsx` | 236 | ConditionalExpression → `true` | `@s38 un 500 al sincronizar deja el estado incierto sin retirar la suscripción` |
| `external-calendar.tsx` | 237 | CallExpression → `;` (`NoCoverage`) | la misma |
| `external-calendar.tsx` | 148 | BooleanLiteral → `false` | `@s37 muestra el formulario de alta cuando no hay suscripción` |
| `external-calendar.tsx` | 149 | ConditionalExpression → `true` | la misma |
| `external-calendar.tsx` | 93 | StringLiteral → `"Stryker was here!"` | la misma (y una segunda) |
| `external-calendar.tsx` | 96 | StringLiteral → `"Stryker was here!"` | la misma |
| `external-calendar.tsx` | 102 | BooleanLiteral → `true` | `@s37 muestra host y cola pero nunca la dirección completa` |
| `external-calendar-api.ts` | 102 | ConditionalExpression → `false` | `rechaza una suscripción con cola que no es texto pero mide cuatro` |
| `external-calendar-api.ts` | 140 | ConditionalExpression → `false` | `rechaza configured que es cero en vez de false` |

Lo que cada uno protegía, que es lo que importa:

- **`today:59`** es la **condición 3** del veredicto, la del `@s35`. El doble de
  `fetch` apilaba método y URL pero nunca el cuerpo, así que cambiar
  `syncExternalCalendar(true)` por `false` dejaba la suite verde y convertía cada
  carga de Hoy en una descarga forzada del feed ajeno. Rojo acreditado:
  `expected { onlyIfStale: false } to deeply equal { onlyIfStale: true }`.
- **`today:27`**: la marca se afirmaba con `/Según sincronización de 12:00/`, y con
  `hour12: true` el texto es «12:00 p. m.», que **contiene** esa subcadena. Es la
  misma trampa que la de `APP_CONNECTOR_KEY` en backend: una aserción de subcadena
  que casa con las dos ramas. Ahora es igualdad.
- **`today:105`** no era el mensaje, como decía el veredicto, sino el **separador**
  `{" "}` entre el aviso y el enlace. Sin él el párrafo se lee «…calendario
  externo.Revisar el calendario externo». Se fija el `textContent` entero.
- **`external-calendar.tsx:291`**: sólo se afirmaba que la **dirección** tecleada
  llegaba a la petición, no la **etiqueta**. El `onChange` de Etiqueta podía no hacer
  nada y la suscripción se guardaba con etiqueta vacía. Rojo:
  `expected { label: '', …(1) } to deeply equal { label: 'Trabajo', …(1) }`.
- **`:236` y `:237`**: el `catch` de `synchronise()` tiene dos ramas y ninguna prueba
  pasaba por la segunda. Con `:236` a `true`, **cualquier** fallo de sincronización
  retiraba la suscripción de la pantalla; `:237` (`else failed(error)`) no se
  ejecutaba jamás. Un 500 distingue las dos y las mata a la vez.
- **`:148` y `:149`**: el **primer render** no lo fijaba nadie. Con `:149` a `true`
  una carga sin suscripción intenta leer `snapshot.subscription.label`, revienta,
  cae en el `catch` y pinta un aviso de error —y el formulario **seguía saliendo**,
  así que la prueba pasaba igual. Con `:148` a `false` la nota «Todavía no tienes
  ningún calendario externo» no aparece nunca y tampoco lo notaba nadie.
- **`api:102` y `api:140`** son el caso fino: **ya había filas adversariales** y aun
  así sobrevivían. `urlTail: 7` no basta, porque apagar la guarda de tipo deja actuar
  a la de longitud (`(7).length` no es 4); hace falta algo que no sea texto y **mida
  cuatro**. Y `configured: "sí"` tampoco, porque es un valor **verdadero** y acaba en
  `subscriptionOf(null)`, que rechaza igual; hace falta un valor **falso** que no sea
  booleano, como `0`. Dos filas, dos mutantes.

## Los 21 equivalentes o inalcanzables, con su razón

### `external-calendar-api.ts` (7)

| Línea | Mutador | Razón |
|---|---|---|
| 66 | StringLiteral | El `message` de `ExternalCalendarValidationError` **no se pinta nunca**: la pantalla usa `error.fields`, no `error.message`. |
| 67 | StringLiteral | `this.name`. El despacho es por `instanceof`, no por nombre. |
| 73 | StringLiteral | `this.name` de `ConnectorsDisabledError`. Su `message` (`:72`) sí muere, porque ése sí se pinta: la asimetría confirma el criterio. |
| 78 | StringLiteral | El `message` de `ExternalCalendarNotConfiguredError` no llega a la pantalla: ese error se traduce en `forget()`. |
| 79 | StringLiteral | `this.name`. |
| 94 | ConditionalExpression | `typeof value.id !== "string"` es **redundante con la cláusula siguiente**: el regex de UUID rechaza igual cualquier cosa que no sea un UUID, sea del tipo que sea. |
| 167 | ConditionalExpression | Con `true`, toda respuesta de error entra en la rama de `VALIDATION_ERROR`; si no reúne campos reconocibles cae en el mismo `throw response` de `:188`. Sólo se distinguiría con un cuerpo que el backend no produce (un 500 que traiga `errors` bien formados). |

### `external-calendar.tsx` (10)

| Línea | Mutador | Razón |
|---|---|---|
| 43 | Regex `/\.\d+Z$/` → sin ancla | El valor viene de `toISOString()`, donde los milisegundos sólo pueden estar al final. El ancla no tiene nada más que anclar. |
| 92 | ArrayDeclaration | El estado inicial de la lista se sobrescribe antes de que la lista llegue a pintarse: la ficha sólo existe con suscripción, y para entonces `loadEvents` ya escribió. |
| 110 | OptionalChaining | `inFlight.current` se fija en el montaje (`:143`), así que nunca es nulo cuando corre `start()`. |
| 114 | ArrayDeclaration | Array de **dependencias** de `useCallback`, no de datos: cambia la identidad del callback, no el comportamiento. |
| 122 | ArrayDeclaration | Ídem, las de `forget`. |
| 137 | ArrayDeclaration | `setEvents([])` en el `catch` de `loadEvents`: da igual lo que se ponga, porque `setInvalidList(true)` gana al render (`:393` pinta el aviso en vez de la lista). |
| 139 | ArrayDeclaration | Dependencias de `loadEvents`. |
| 167 | OptionalChaining | Mismo motivo que `:110`. |
| 168 | ArrayDeclaration | Dependencias del efecto de montaje. `loadEvents` es estable (`useCallback` con `[]`), así que el efecto no se reejecuta ni con la lista ni sin ella. |
| 175 | OptionalChaining | `field?.focus`. `focusOn` sólo se arma en `failed()` para errores de validación, y en ese estado los dos campos están montados: la referencia nunca es nula ahí. |

**Corrección al veredicto:** decía «`:114`/`:118`/`:122` (`forget()` puede dejar de
vaciar la lista, `feature:542`)». Sólo **`:118`** es `setEvents([])`; `:114` y `:122`
son los arrays de dependencias de dos `useCallback`. Y `:118` tampoco es tan directo
como parece: ver «abiertos».

### `today-external-calendar.tsx` (4)

| Línea | Mutador | Razón |
|---|---|---|
| 51 | BooleanLiteral | `useState(false)` de `pendingSync`. Mientras `reading.kind === "hidden"` la sección devuelve `null`, y `setPendingSync(pending)` corre en el mismo paso que `setReading`: el valor inicial **nunca llega a pintarse**. |
| 61 | ConditionalExpression | `if (signal.aborted) return` en el `catch` de la sincronización. Es una guarda redundante: si no corta aquí, la lectura siguiente lanza sobre la señal abortada y vuelve a cortar en `:66`/`:79`. |
| 71 | StringLiteral | `kind: "events"` → `""`. El render sólo distingue `hidden` y el par `unreadable`/`unreachable`; cualquier otro valor pinta la rama de eventos exactamente igual. Es una diferencia de tipos, no de comportamiento. |
| 80 | CallExpression | `setPendingSync(pending)` en la rama de **error**. El aviso de pendiente sólo se pinta en la rama de eventos (`:117`), así que ese estado no llega nunca a la pantalla. |

## Los 2 con error de ejecución: qué les pasa

`today-external-calendar.tsx:76` (`ObjectLiteral → {}` y `StringLiteral → ""`) es
`: { kind: "hidden" }`. Con cualquiera de los dos mutantes el objeto deja de tener la
forma que el render espera, se cae por la rama de eventos y explota al leer
`reading.items.length`. Stryker lo etiqueta `RuntimeError` —y lo **saca del
denominador**— porque el mutante revienta el ejecutor en vez de hacer fallar una
aserción.

No es un hueco de oráculo: `@s36 sin suscripción la sección no se muestra ni avisa de
nada` sí detecta el cambio (afirma `container.textContent === ""`, y con el mutante
el render lanza). Lo dejo escrito para que el próximo juez no lo cuente como deuda:
**la producción está cubierta; lo que falta es que la herramienta sepa clasificarlo.**

## Los 29 abiertos, agrupados por hueco real

No son veintinueve problemas: son **cuatro racimos** y cuatro sueltos. Escribirlos así es lo
único que hace el trabajo presupuestable.

### Racimo 1 — el validador de `problem+json` no se ejerce deforme (9 mutantes)

`external-calendar-api.ts` `:160`, `:164`, `:167` (encadenamiento opcional), `:170`,
`:172` ×2, `:173`, `:177`, `:178`.

`refuse()` desarma el cuerpo de error del servidor, y **ninguna prueba le manda un
cuerpo deforme**. Los tres `body?.code` sobreviven porque no hay ningún 503, 404 ni
400 cuyo cuerpo no sea JSON (con el mutante, `body.code` sobre `null` lanza
`TypeError` en vez de caer al `throw response`). Los siete restantes son el bucle de
`errors`: falta un `errors` que no sea lista, una entrada nula, una que no sea objeto,
una con `message` no textual y una con `code` no textual.

**Cómo se cierra:** una tabla `it.each` en `external-calendar-api.test.ts`, del mismo
estilo que las dos que ya existen, con esos cinco cuerpos más tres respuestas de
cuerpo no-JSON. Es **una sola prueba parametrizada** y mata los nueve.

### Racimo 2 — el `catch` de `confirmRemoval()` no se ejerce (6 mutantes)

`external-calendar.tsx` `:252`, `:253` ×2, `:254`, `:255`, `:257`.

Ninguna prueba hace fallar el `DELETE`. Los dos `NoCoverage` (`:254` y `:255`) lo
dicen literalmente: `setAnnouncement("")` y `failed(error)` no se ejecutan jamás.

**Cómo se cierra:** una prueba calcada de la de `@s38 un 500 al sincronizar…`, con
`answer(ROUTE, "DELETE", {}, 500)`, afirmando que sale el aviso de estado incierto,
que la suscripción **sigue** en pantalla y que los controles se desbloquean.

### Racimo 3 — reentrada y aborto de las tres escrituras (8 mutantes)

`external-calendar.tsx` `:197`, `:210`, `:214` (guardar), `:219`, `:234`, `:235`,
`:239` (sincronizar), `:131` (lectura de eventos).

Las guardas `if (busy) return` y `if (signal.aborted) …` no las mide nadie porque los
botones se deshabilitan y las pruebas nunca fuerzan la segunda entrada ni el aborto a
mitad. Es la familia que ya mordió en `@s39` fila 3.

**Cómo se cierra:** dos pruebas con la respuesta retenida (la técnica de la promesa
`pending` que ya se usa en `@s38 anuncia Guardando…`): una que envíe el formulario dos
veces y afirme una sola petición, y otra que desmonte a mitad y afirme que no se toca
el estado después.

### Racimo 4 — los dos párrafos de error de campo vacíos (2 mutantes)

`external-calendar.tsx` `:294` y `:319`: `{fieldErrors.label ?? ""}` y su gemelo.
Nadie afirma que estén **vacíos** cuando no hay error, así que pueden decir cualquier
cosa mientras el formulario está limpio.

**Cómo se cierra:** dos aserciones en la prueba del formulario de alta.

### Sueltos (4 mutantes)

| Fichero | Línea | Qué falta |
|---|---|---|
| `external-calendar.tsx` | 118 | `setEvents([])` dentro de `forget()`. **Sólo es observable en el instante entre `setSubscription(saved)` y la resolución de `loadEvents`**, porque todo camino que vuelve a pintar la ficha recarga la lista. Se cierra reteniendo la respuesta de eventos tras un alta y afirmando que la lista está vacía en ese render intermedio. Es la prueba más cara de la lista y la dejo escrita, no hecha. |
| `external-calendar.tsx` | 103 | `useState(true)` de `invalidList` pinta «No se ha podido leer la lista de eventos» en el render intermedio de una carga que va bien. Mismo tipo de prueba que la anterior. |
| `external-calendar.tsx` | 196 | `event.preventDefault()`. En jsdom no hay navegación que impedir, así que nadie lo nota. Se cierra con un `submit` sintético afirmando `defaultPrevented`. |
| `external-calendar.tsx` | 223 | `setFailure("")` al empezar a sincronizar: falta encadenar una sincronización fallida y otra correcta y afirmar que el aviso anterior desaparece. Existe ese encadenamiento para **guardar** (`@s38 un reintento con éxito retira el aviso`), no para sincronizar. |

## Efecto esperado en la próxima campaña

13 mutantes pasan de vivos a muertos sobre los mismos 726 con veredicto:
**676/726 = 93,11 %**, desde 91,32 %. Con el ámbito ya estrechado (condición 4), que
quita 64 mutantes de `App.tsx` todos resueltos: **612/662 = 92,45 %**, en vez del
90,48 % que habría dado el estrechamiento por sí solo.

Por fichero, el que estaba flojo deja de estarlo: `external-calendar.tsx` sube de
**84,80 %** (212/250) a **88,00 %** (220/250) y `today-external-calendar.tsx` de
**91,36 %** a **95,06 %**. Es previsión aritmética, no medida: **la medida la da la
campaña**.

## Qué cambió en el árbol

Sólo pruebas, ni una línea de producción:

- `frontend/src/today-external-calendar.test.tsx` — el doble de `fetch` guarda también
  el cuerpo; `@s35` afirma `{ onlyIfStale: true }`; la marca horaria pasa a igualdad;
  el aviso de red afirma su `textContent` entero.
- `frontend/src/external-calendar.test.tsx` — el cuerpo del `PUT` se afirma entero;
  prueba nueva del 500 al sincronizar; el primer render queda fijado (nota, etiqueta
  vacía, región de estado callada, sin aviso de error, sin diálogo abierto).
- `frontend/src/external-calendar-api.test.ts` — dos filas adversariales nuevas.
