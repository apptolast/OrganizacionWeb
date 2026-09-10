# Cierre de la puerta de mutación de frontend — feature 28, calendario externo

Carril `claude/external-calendar`, worktree `C:/Users/vhurt/ow-worktrees/external-calendar`,
partiendo de `origin/main` en `5b019343`. Puerto E2E asignado: 18092 (no usado: todo
el trabajo es de unitarias).

Ámbito de mutación (`frontend/stryker.external-calendar.config.json`):
`src/external-calendar-api.ts`, `src/external-calendar.tsx`,
`src/today-external-calendar.tsx` (más cuatro rangos de `App.tsx`/`workspace.tsx`
que no son míos).

Mapa de partida: `progress/prediccion_huecos_frontend.md`, sección
`28-calendario-externo`. Orden de ataque el del encargo: primero `sin_cobertura`
con posible defecto, luego `asercion_que_no_puede_fallar`, luego `oraculo_debil`.

Línea base antes de tocar nada: **83 pruebas verdes** en los cuatro ficheros del
ámbito (`external-calendar.test.tsx`, `external-calendar-api.test.ts`,
`external-calendar-route.test.tsx`, `today-external-calendar.test.tsx`).

---

## Racimo A — el catch de `loadEvents` (predicción: racimo 3, `sin_cobertura`, 9 mutantes)

`external-calendar.tsx:124-137`. La predicción decía que con un 500 o un 503 en
`GET /events` la vista cae al `else` de `:390` y afirma «No hay eventos en la
ventana guardada»: le dice al propietario que su calendario está vacío cuando en
realidad no ha podido leerlo.

### DEFECTO DE PRODUCTO 1 — la ventana vacía que nadie había leído

Confirmado ejecutando, no razonando. Prueba nueva
«@s36 no afirma que la ventana esté vacía cuando la lista falla con 500 sin cuerpo
reconocible»: el volcado del DOM del fallo contiene, literalmente,

```
No hay eventos en la ventana guardada.
```

con el `GET /events` respondiendo **500**. La causa: `refuse()`
(`external-calendar-api.ts:188`) relanza la **propia `Response`**, que no es
`instanceof Error`, así que la guarda

```ts
if (error instanceof Error && !(error instanceof ConnectorsDisabledError))
  setInvalidList(true);
```

no se cumplía nunca para un error de servidor. Igual con el 503
`CONNECTORS_DISABLED`, que sí es `Error` pero estaba excluido a propósito: también
acababa pintando la ventana como vacía.

**Arreglo** (`external-calendar.tsx:130-137`): toda lectura que no llega deja la
lista en «no se ha podido leer». La condición desaparece; queda solo el
cortocircuito de aborto, que sí tiene motivo (una lectura cancelada no es una
lectura fallida).

Es el mismo patrón que los cinco defectos de esta noche: **la interfaz afirma un
hecho que no conoce**.

### Rojo acreditado

Tres filas nuevas (`it.each`), una por forma de fallo, más una prueba positiva:

| Prueba | Antes del arreglo | Después |
|---|---|---|
| `@s36 … falla con 500 sin cuerpo reconocible` | ROJO: `Unable to find an element with the text: No se ha podido leer la lista de eventos.` y el DOM mostraba «No hay eventos en la ventana guardada.» | verde |
| `@s36 … falla con 503 de conectores` | ROJO, mismo mensaje | verde |
| `@s36 … falla con fallo de red` | ya pasaba (un `TypeError` sí es `Error`); queda como red de seguridad de la rama que antes funcionaba | verde |
| `@s37 dice que la ventana está vacía solo cuando la lectura sí ha llegado` | verde desde el principio; existe porque el literal «No hay eventos en la ventana guardada.» **no lo afirmaba ninguna prueba** (hueco «omitido» de la predicción) y sin él la aserción negativa de las tres filas anteriores no valdría nada | verde |

Mutantes que pasan a estar cubiertos: el literal de `:390`, el ternario
`events.length === 0`, y —por la vía del arreglo— desaparecen del denominador los
mutantes de la conjunción que se ha eliminado.

Estado tras el racimo: **87 pruebas verdes** en los cuatro ficheros.

### Acreditación por mutante (racimo A)

`node scripts/verificar-mutantes-external-calendar.mjs A` — cada mutante se aplica
al fichero de producción real, se ejecuta `src/external-calendar.test.tsx`, se anota
el rojo y se restaura.

| Mutante | Veredicto | Quién lo mata |
|---|---|---|
| `setInvalidList(true)` → `false` (catch) | MUERE | las tres filas de `@s36 no afirma que la ventana esté vacía…` y `@s36 avisa de lectura inválida…` |
| `setEvents([])` → lista no vacía (catch) | **SOBREVIVE** | nadie: con `invalidList` en `true` la lista no se pinta, así que el estado de eventos es invisible por ese camino. Superviviente **equivalente**, declarado aquí antes de la campaña |
| `setInvalidList(false)` del camino feliz → `true` | MUERE | cinco pruebas |
| literal «No hay eventos en la ventana guardada.» → `""` | MUERE | `@s37 dice que la ventana está vacía solo cuando la lectura sí ha llegado` |
| `events.length === 0` → `!==` | MUERE | cinco pruebas |

---

## Racimo B — el catch del efecto de montaje (predicción: racimo 4, `sin_cobertura`, 8 mutantes)

`external-calendar.tsx:147-158`. Ninguna prueba hacía fallar el `GET` de montaje,
así que el catch entero —incluido el mensaje de conectores deshabilitados y el
`setLoaded(true)`— no se ejecutaba nunca.

**No hay defecto aquí**: las tres pruebas nuevas pasaron a la primera. Por eso, y
porque una prueba que pasa a la primera no demuestra nada, se acredita mutante a
mutante contra la producción real (regla 5 del reparto: ejecutar, no razonar).

Pruebas nuevas:

- `@s8 la carga con conectores deshabilitados lo dice, sin el mensaje genérico`
  (503 `CONNECTORS_DISABLED` en el `GET`) — afirma el texto propio **y la ausencia
  del genérico**, que es lo que distingue las dos ramas del ternario.
- `@s37 una carga que falla deja la pantalla usable, no un vacío permanente`
  (500) — genérico presente, el de conectores ausente, «Todavía no tienes ningún
  calendario externo.» visible y Guardar habilitado: eso fija `setLoaded(true)`.
- `@s37 mientras la carga no ha respondido no promete que no haya suscripción`
  (fetch que no resuelve) — fija la rama falsa del ternario `loaded ? … : null`,
  que nadie afirmaba: sin ella, mutarlo a `true` sobrevive.

| Mutante | Veredicto | Quién lo mata |
|---|---|---|
| ternario de conectores → siempre el genérico | MUERE | `@s8 la carga con conectores deshabilitados…` |
| ternario de conectores → siempre el de conectores | MUERE | `@s37 una carga que falla…` |
| literal genérico → `""` | MUERE | `@s37 una carga que falla…` |
| `setLoaded(true)` del catch → `false` | MUERE | `@s37 una carga que falla…` |
| `loaded ? … : null` → `true` | MUERE | `@s37 mientras la carga no ha respondido…` |
| literal «Todavía no tienes ningún calendario externo.» → `""` | MUERE | `@s37 una carga que falla…` |

Estado tras el racimo: **90 pruebas verdes**.

---

## Racimo C — reentrada y aborto de las tres escrituras (predicción: racimo 2, `sin_cobertura`, 16 mutantes)

`external-calendar.tsx:109-114, 160, 190, 203, 207, 212, 227, 232, 237, 246, 250`.

### DEFECTO DE PRODUCTO 2 — salir de la pantalla no cancelaba ninguna escritura

@s39 fila 3 dice: «navego a /hoy con una petición en curso → **la petición se
cancela** y su respuesta tardía no modifica la vista destino». Solo estaba probado
para el `GET` de montaje.

La limpieza del efecto era `return () => controller.abort()`, y `controller` es el
del **montaje**, capturado en el closure. `save`, `synchronise` y `confirmRemoval`
crean el suyo con `start()` y lo dejan en `inFlight.current`. Al desmontar se
abortaba un controlador ya resuelto y **la escritura en vuelo seguía viva**.

Rojo acreditado, `it.each` de tres filas
(`@s39 salir de la vista con un guardado / una sincronización / un borrado en curso…`):

```
AssertionError: expected false to be true // Object.is equality
   -> expect(signals[0].aborted).toBe(true)  tras view.unmount()
```

Las tres filas rojas, con el `PUT`, el `POST /sync` y el `DELETE` retenidos.

**Arreglo** (`external-calendar.tsx:160`): `return () => inFlight.current?.abort();`.
Es correcto y suficiente porque `start()` ya aborta la anterior antes de crear la
nueva, así que `inFlight.current` es siempre la única que puede seguir viva.

### El único solapamiento real de la pantalla, sin probar

Guardar está **habilitado** mientras la carga inicial sigue en vuelo. Prueba nueva
`@s38 guardar mientras la carga inicial sigue en vuelo la cancela y su respuesta
tardía no pisa lo guardado`: el `GET` de montaje retenido devuelve
`configured:false`, y si `start()` no lo abortara, esa lectura tardía borraría de
pantalla la suscripción recién guardada.

### Reentrada: el guardián que sí es alcanzable, y los dos que no

El botón «Sí, eliminar» del diálogo **no** lleva `disabled={locked}`, así que se
puede pulsar otra vez con el `DELETE` en vuelo: lo único que impide el segundo
borrado es `if (busy) return`. Prueba nueva
`@s39 un segundo Sí, eliminar con el borrado en vuelo no envía otro DELETE`.

Los de `save` y `synchronise` **no son alcanzables** desde la interfaz, y lo
comprobé ejecutando en vez de razonar: escribí una prueba sonda que, con el `PUT`
retenido, enfoca «Etiqueta» y pulsa Enter. Pasó **igual con el guardián puesto que
con `if (false) return`**: la submisión implícita no ocurre porque el botón de
envío está deshabilitado. Era una aserción que no puede fallar, así que **borré la
sonda** en lugar de dejarla (categoría 2 del encargo). Quedan declarados como
supervivientes equivalentes.

### Acreditación por mutante (racimo C)

| Mutante | Veredicto | Quién lo mata |
|---|---|---|
| limpieza del efecto → no aborta nada | MUERE | las cuatro pruebas de desmontaje |
| limpieza del efecto → aborta solo el controlador del montaje (**el código anterior**) | MUERE | las tres filas de escritura |
| `start()`: `inFlight.current?.abort()` → sin abortar | MUERE | `@s38 guardar mientras la carga inicial…` |
| montaje: `if (controller.signal.aborted) return` → `false` | MUERE | `@s38 guardar mientras la carga inicial…` |
| `confirmRemoval`: `if (busy) return` → `false` | MUERE | `@s39 un segundo Sí, eliminar…` |
| `confirmRemoval`: `setBusy("deleting")` → `""` | MUERE | idem |
| `confirmRemoval`: literal «Eliminando…» → `""` | MUERE | idem |
| `confirmRemoval`: literal «Suscripción eliminada.» → `""` | MUERE | idem |
| `save`: `if (signal.aborted) return` del catch → `false` | **SOBREVIVE** | equivalente: ver abajo |
| `save`: `if (!signal.aborted) setBusy("")` del finally → `true` | **SOBREVIVE** | equivalente |
| `synchronise`: `if (signal.aborted) return` del catch → `false` | **SOBREVIVE** | equivalente |
| `confirmRemoval`: `if (signal.aborted) return` del catch → `false` | **SOBREVIVE** | equivalente |
| `confirmRemoval`: `if (!signal.aborted) setBusy("")` del finally → `true` | **SOBREVIVE** | equivalente |

**Por qué esos cinco son equivalentes, declarado antes de la campaña.** Una
escritura solo puede abortarse en dos momentos: al desmontar, o porque `start()`
la cancele. Lo segundo exige empezar otra operación, y `if (busy) return` lo
impide. Queda solo el desmontaje, y ahí React 19 hace de cualquier `setState`
sobre un árbol desmontado un no-op silencioso: **no hay nada observable** en el
DOM, ni aviso en consola. Escribir un oráculo para ellos exigiría cambiar la
producción para permitir dos escrituras solapadas, que es justo lo contrario de lo
que pide @s38 («se envía exactamente una petición»). Son 5 de los ~16 previstos
para este racimo; los otros 8 mueren.

Estado tras el racimo: **95 pruebas verdes**.

---

## Racimo D — las aserciones que no pueden fallar (categoría 2 del encargo)

Cuatro sitios, todos denunciados por la predicción o por sus refutaciones.

### D.1 · Los dos mensajes de fallo de guardado eran indistinguibles

`external-calendar.test.tsx`, el `it.each` de dos filas (503 `CONNECTORS_DISABLED`
y fallo de red) usaba **la misma** aserción `/no sabemos si se guardó/i` para las
dos. Borrando entero el bloque de `ConnectorsDisabledError` de `failed()`
(`external-calendar.tsx:177-180`) las dos filas seguían verdes, porque el 503 cae
en el `setFailure(UNCERTAIN)` genérico y produce un texto que también contiene esa
frase. @s38 fila 3 pide un mensaje distinto del genérico.

Ahora cada fila lleva su **texto completo** y se afirma el `textContent` exacto del
`role="alert"`, así que cada una niega implícitamente el de la otra. Y de paso se
afirma que el `role="status"` queda vacío: si `setAnnouncement("")` del catch se
perdiera, la región viva seguiría diciendo «Guardando…» mientras la alerta dice que
falló.

### D.2 · El recuento de botones de la sección de Hoy

`today-external-calendar.test.tsx:96`,
`expect(within(section).queryAllByRole("button")).toHaveLength(0)`: la sección no
renderiza **ningún** botón en ninguna de sus ramas, así que la cuenta es cero por
construcción y ningún mutante puede tumbarla. Sustituida por dos aserciones que sí
caen: no hay enlace de rescate (lo que distingue el camino feliz del de fallo) y no
se anuncia «Sincronización pendiente.» (la rama falsa que nadie fijaba).

### D.3 · «ningún nodo del DOM contiene la URL completa»

`external-calendar.test.tsx:119`, `not.toContain("https://")`: estructuralmente
infalible, porque el DTO solo transporta `urlHost` y `urlTail` y la URL completa no
existe en el fixture. Se conserva como documentación del And de @s37, pero el test
ahora fija además la forma exacta del recorte
(`"calendar.google.com … .ics"`), que el separador esté fuera del árbol de
accesibilidad (`aria-hidden="true"`) y que la etiqueta llegue al campo.

### D.4 · La sincronización fallida que «conserva la lista» por casualidad

La prueba de @s38 fila 7 afirmaba que «Reunión» sigue en pantalla, pero el doble de
`fetch` reutiliza la última respuesta encolada: si el mutante quitara la guarda
`if (outcome.subscription.lastStatus === "OK") await loadEvents(signal)`, la
recarga devolvería lo mismo y la prueba pasaría igual. Ahora se **cuenta** la
petición: una sola lectura de `/events` tras una sincronización fallida, dos tras
una correcta. Y se afirma el anuncio «Sincronización fallida.», que no aparecía en
ninguna prueba del repositorio.

También se corrigió el comentario engañoso de la prueba del doble clic de
Sincronizar: quien impide el segundo POST es el atributo `disabled`, no el
guardián de reentrada.

### Acreditación por mutante (racimo D) — 12 de 12 mueren

| Mutante | Quién lo mata |
|---|---|
| `failed()`: se borra el bloque de `ConnectorsDisabledError` | `@s38 … con 503` |
| `failed()`: el prefijo de conectores se pierde al componer | `@s38 … con 503` |
| `save`: `setAnnouncement("")` del catch → se borra | las dos filas de `@s38 … estado incierto` |
| separador `" … "` → `""` | `@s37 muestra host y cola…` |
| `aria-hidden="true"` → `""` | `@s37 muestra host y cola…` |
| `setLabel(subscription.label)` → `""` | `@s37 muestra host y cola…` |
| literal «Sincronización fallida.» → `""` | `@s38 muestra el mensaje del código…` |
| ternario del anuncio → siempre «Sincronizado.» | `@s38 muestra el mensaje del código…` |
| guarda de recarga → recarga siempre | `@s38 muestra el mensaje del código…` |
| guarda de recarga → no recarga nunca | `@s38 sincroniza…` y `@s38 una sincronización correcta sí vuelve a pedir…` |
| Hoy: `pendingSync ? … : null` → `true` | `@s35 muestra el evento en hora local…` |
| Hoy: el ternario de fallo → `true` | siete pruebas |

Estado tras el racimo: **96 pruebas verdes**.

---

## Racimo E — los validadores de la capa API (predicción: racimo 1 `oraculo_debil` 24 mutantes, racimo 5 `oraculo_debil` 8, racimo 12 `sin_cobertura` 7, racimo 9 `oraculo_debil` 5, más cuatro huecos «omitidos»)

`external-calendar-api.ts`. La enfermedad es una sola y está en los **cuatro**
validadores del fichero, no solo en `subscriptionOf`: una cadena de guardas de la
que solo unas pocas tienen fixture roto; las demás se evalúan **siempre en falso**,
así que el mutante que las apaga sobrevive sin que nada se ponga rojo.

De `external-calendar-api.test.ts` se pasa de 40 a **81 pruebas**. Lo añadido:

- **`subscriptionOf`**: 15 filas nuevas en el `it.each`, una por guarda muda
  (etiqueta vacía y no-texto, host y cola no-texto, id no-texto, último intento
  inválido, zona no-texto, truncado no-booleano, `updatedAt` inválido y los tres
  contadores que solo tenían fixture en `imported`). Y las **dos filas de anclaje
  del regex de uuid** —`x1111…` y `…5555x`—, que son las que en la campaña de
  automatizaciones sobrevivieron cuando todo lo demás murió.
- **`eventOf`**: `endAt` malformado, `uid` no-texto y `allDay` no-booleano. Ojo con
  el último: el fixture `{...item, allDay: undefined}` que ya existía **pierde la
  clave** al serializarse con `Response.json`, así que cae en `exact()` y no llega
  nunca al `typeof`. Confirmado por el refutador y por la ejecución.
- **El sobre de `readExternalEvents`**: `configured` no-booleano, `lastSyncAt`
  inválido, `lastStatus` desconocido e `items` que no es lista.
- **`snapshotOf`**: campo de más en la instantánea (el que ya había rompía el
  **subobjeto**, no el sobre) y `configured` no-booleano.
- **`syncExternalCalendar`**: campo de más y `subscription` ausente, para el
  `exact(body, "performed subscription")`.
- **`refuse()`**: seis casos que separan las dos mitades de cada conjunción
  estado+código (503/404/400 con otro código, y 500/400/404 con el código ajeno),
  más uno que vuelve a leer el cuerpo de la respuesta relanzada, que es lo único
  que fija el `.clone()` de `problem()`.
- **Cabeceras**: `Accept` en el GET, `Accept` + `Content-Type` + `X-CSRF-TOKEN` en
  el PUT, `Content-Type` en el POST `/sync`. **Corrijo aquí a la predicción**: su
  `posibleDefecto` («si el spread de `options.headers` se perdiera, el PUT viajaría
  sin `X-CSRF-TOKEN`») es falso, y las dos refutaciones tenían razón:
  `api-client.ts:22-27` construye `new Headers(options.headers)` y hace
  `headers.set("X-CSRF-TOKEN", …)` **después** de la mezcla, así que el token se
  añade pase lo que pase. El oráculo escrito sobre esa premisa habría dado verde
  con la producción rota. Lo que sí se pierde con ese mutante es el
  `Content-Type` —y eso es lo que afirman las pruebas nuevas—.
- **Cuerpo 200 que no es JSON**, **PUT que responde `configured:false`** (la única
  protección contra que `saveExternalCalendar` devuelva `null` y la vista deje al
  propietario en el formulario de alta tras un guardado con éxito) y las **dos
  señales abortadas del DELETE**, antes y durante.

### Acreditación por mutante (racimo E) — 40 de 41 mueren

Todos los de `subscriptionOf`, `eventOf`, el sobre de `readExternalEvents`,
`snapshotOf`, `exact` de sync, las tres conjunciones de `refuse`, el `.clone()` de
`problem`, las tres cabeceras, el `if (!snapshot.configured) invalid()` y los dos
`throwIfAborted` del DELETE: **mueren**, con la prueba concreta que los mata
anotada en `progress/verificacion_mutantes_external_calendar.json`.

Tres apuntes de método:

1. Los anclajes `^` y `$` del regex mueren **solo** por las dos filas de prefijo y
   sufijo. Sin ellas, las otras quince no los tocan. La predicción acertó de lleno.
2. Cuatro «supervivientes» de mi primera pasada eran artefacto **del mutante que
   escribí yo**, no de Stryker: apagar una guarda entera no es un operador que
   Stryker genere. Repetidos con los operadores reales
   (`!==` → `===` y `"string"` → `""`) los cuatro **mueren**, casi siempre por el
   camino feliz. Queda anotado para que la campaña no se lea mal.
3. **Superviviente equivalente declarado**: `response.json().catch(() => invalid())`
   mutado a `catch(() => undefined)`. Todos los llamadores revalidan el cuerpo
   (`snapshotOf`, `exact`, el sobre de eventos), así que `undefined` acaba lanzando
   exactamente el mismo «Respuesta de calendario externo inválida». No hay oráculo
   posible sin cambiar la producción.

Estado tras el racimo: **135 pruebas verdes** (83 de partida → 135).

---

## Racimo F — la presentacion de /calendario-externo (predicción: racimos 7, 8, 10, 11 y 13, más cinco huecos «omitidos»)

- **`formatMoment` y `formatClock`** (racimo 7 + el gemelo que añadió el refutador):
  ninguna prueba fijaba una fecha formateada. Ahora hay dos: con
  `snapshotZoneId: "Asia/Tokyo"` las dos fechas de la ficha valen exactamente
  «7 ene 2030, 20:00» y «7 ene 2030, 21:00» y el evento sale «17:00–18:00»; y con
  `snapshotZoneId: null` las horas coinciden con las del navegador, derivadas con
  `new Date(...).getHours()` en vez de con literales.
  **Se eligió Asia/Tokyo a propósito**: la máquina que ejecuta la suite está en
  `Europe/Madrid`, así que un fixture en Madrid habría hecho indistinguible «zona de
  la instantánea» de «zona del navegador» aquí y distinguible en otra máquina. Es
  justo la clase de constante caduca que prohíbe la regla 2 del reparto.
- **Los siete mensajes de `FEED_MESSAGES`** (racimo 8): `it.each` de siete filas,
  cuatro de las cuales no se renderizaban en ninguna prueba, afirmando un fragmento
  distintivo **y** que el texto no se agota en él (un `role="alert"` vacío es peor
  que no mostrarlo, @s12). Más dos filas negativas —`OK` con código, `FAILED` sin
  código— que fijan la conjunción de `:352`.
- **`failed()`** (racimo 10): un 400 `VALIDATION_ERROR` en `label` que afirma la
  asociación, el `aria-invalid` de los **dos** campos y el foco en Etiqueta; y un
  PUT 500 que afirma que la suscripción **no** se retira, que es lo que cae si el
  `&& error.status === 401` se relaja.
- **Resumen vacío** (racimo 11): un evento con `summary: ""` en las dos vistas,
  afirmando el `<li>` completo («Sin título 09:00–10:00»), que además fija que la
  fila no queda sin nombre accesible. @s19.
- **`truncated` en falso**, **Cancelar cierra el diálogo**, **`aria-busy` del
  formulario** en los dos sentidos y **el `<h2>` que cambia con la suscripción**
  (racimo 13 y tres «omitidos»).
- **Los reinicios de estado**: dos pruebas de error-y-reintento, una para
  `setFieldErrors({})` y otra para `setFailure("")`, que no existían.
- **`forget()` cierra el diálogo**: el `setConfirming(false)` sobrevivía porque al
  borrar desaparece la sección entera. La prueba que lo mata recorre el camino
  completo —eliminar, confirmar, volver a suscribirse— y afirma que el diálogo de
  eliminación **no reaparece solo**. Sin ese `setConfirming(false)` el propietario
  se encontraría, tras dar de alta un calendario nuevo, con el diálogo de borrado
  abierto sin haberlo pedido.

Acreditación: **24 de 24 mutantes mueren** (dos exigieron una prueba más, escrita
después de ver el superviviente; un tercero tenía el ancla ambigua porque el
ternario de zona aparece dos veces, y se desambiguó).

## Racimo G — la sección de Hoy (predicción: racimo 6, ya refutado dos veces)

Las dos refutaciones tenían razón: el racimo valía 2-3 mutantes, no 8, porque
`today-external-calendar.test.tsx:194-210` ya mataba el mutante que apaga el
`startsWith`. Lo que de verdad no tenía oráculo, y ahora sí:

- El literal «No se ha podido consultar el calendario externo.»: la prueba de fallo
  de red solo miraba el `href` del enlace, que los **dos** mensajes comparten. Ahora
  afirma su texto y niega el del otro.
- El `catch` de `clock()`: una zona no resoluble (`Marte/Base`) devuelve el instante
  crudo. Nunca se ejecutaba.
- La rama de `lastSyncAt` nulo: el único fixture con `lastSyncAt: null` pertenecía a
  la prueba de desmontaje, que nunca llega a pintar.
- `summary: ""` en Hoy.
- **La guarda de aborto de después de leer el cuerpo** (`today:66`). El refutador la
  daba por código muerto; **no lo es**, y lo demuestro ejecutando: `json()` hace su
  `throwIfAborted` y **luego** espera a `response.json()`, así que una cancelación
  que llega durante la lectura del cuerpo vuelve por el camino feliz. La prueba
  nueva retiene el `json()` (no el `fetch`), rerenderiza con otra revisión —lo que
  aborta la anterior con la sección **viva**, así que sí es observable— y afirma que
  la respuesta tardía no repinta «Vieja».

**Superviviente equivalente declarado**: la guarda de aborto del `catch` de la
sincronización (`today:61`). Quitándola, el flujo sigue a `readExternalEvents`, que
lanza en su primer `throwIfAborted` **antes de pedir nada**, y el `catch` siguiente
vuelve a cortar por `signal.aborted`. No hay diferencia observable: ni pintado, ni
petición de más.

Acreditación: **6 de 7 mutantes mueren**.

---

## Racimo H — el foco, la asociación del error y el salto al contenido

Cinco mutantes que ninguna prueba distinguía y que son contrato de @s38 y @s40:

- `focusOn.current = null` en el efecto de foco. Sin él, cada cambio de
  `fieldErrors` vuelve a mover el foco: al reintentar con éxito, el foco saltaría
  del botón que el propietario acaba de pulsar al campo de dirección. @s40 pide
  que los estados se anuncien **sin mover el foco**.
- El `aria-describedby` del campo y el `id` del párrafo de error. «El error se
  asocia al campo» (@s38 fila 2) es una asociación programática, no una proximidad
  visual: la prueba comprueba que el mensaje está entre los elementos que describen
  al campo.
- El `id="proyectos"` y el `tabIndex={-1}` del `<main>`. El «Saltar al contenido»
  del espacio de trabajo apunta a `#proyectos`; si esta vista perdiera el id, el
  salto no llevaría a ninguna parte. La prueba vive en
  `external-calendar-route.test.tsx`, que es donde existe el enlace, y **deriva** el
  destino del `href` en vez de escribirlo a mano (regla 2 del reparto).

**5 de 5 mueren.**

## Racimo I — el andamiaje de la pantalla

Textos y cableado de accesibilidad que no afirmaba nadie: la promesa «solo se lee:
esta aplicación nunca escribe en tu proveedor», los nombres accesibles de las dos
secciones (`aria-labelledby` más los `id` de sus encabezados), el `type`,
`autoComplete` y `spellCheck` de los campos, el `aria-atomic` de la región viva, la
ayuda de Google Calendar como descripción del campo de dirección, el
`<h3>Eventos</h3>`, el `role="note"` y el texto del aviso de truncado, el nombre y
el texto del diálogo de confirmación, y en Hoy el `aria-live` de la sección y su
`<h2>`.

**15 de 15 mueren.**

---

## Previsión de puntuación — es una PREVISIÓN, no una medida

**No he ejecutado Stryker**: había dos campañas corriendo y la mide el orquestador.

Lo que sí está medido: **119 mutantes aplicados a mano al fichero de producción
real, ejecutados contra las pruebas y restaurados**, con el detalle en
`progress/verificacion_mutantes_external_calendar.json`. De ellos **111 mueren** y
**8 sobreviven**, y los ocho están declarados abajo con su razón.

**Previsión: 88-94 %**, con lo que quede vivo concentrado en dos sitios conocidos.

### Los ocho supervivientes que conozco, y por qué son equivalentes

| Mutante | Razón |
|---|---|
| `setEvents([])` del catch de `loadEvents` | con `invalidList` en `true` la lista no se pinta: el estado es invisible por ese camino |
| `save`: `if (signal.aborted) return` del catch | una escritura solo se aborta al desmontar —el guardián de reentrada impide la otra vía— y en React 19 un `setState` sobre un árbol desmontado es un no-op silencioso |
| `save`: `if (!signal.aborted) setBusy("")` del finally | ídem |
| `synchronise`: `if (signal.aborted) return` del catch | ídem |
| `confirmRemoval`: `if (signal.aborted) return` del catch | ídem |
| `confirmRemoval`: `if (!signal.aborted) setBusy("")` del finally | ídem |
| `json()`: el `catch` que llama a `invalid()` | todos los llamadores revalidan el cuerpo, así que `undefined` acaba lanzando el mismo error |
| Hoy: `if (signal.aborted) return` del catch de la sincronización | quitándolo, `readExternalEvents` lanza en su primer `throwIfAborted` antes de pedir nada y el catch siguiente vuelve a cortar |

A ellos hay que sumar los dos guardianes de reentrada de `save` y `synchronise`
(`if (busy) return`), no alcanzables desde la interfaz porque sus botones llevan
`disabled={locked}`. Lo comprobé con una sonda que **borré** por ser una aserción
que no puede fallar.

### El suelo que no he intentado tocar, y por qué

**~26 mutantes de `className`.** Stryker muta cada literal de `className` a cadena
vacía. La única forma de matarlos en vitest es afirmar el nombre de la clase de
vuelta, y **ninguna prueba de este repositorio lo hace**: `grep toHaveClass` en
`frontend/src` da cero ficheros. Fijar la lista de clases de un subárbol es además
la clase de constante caduca que prohíbe la regla 2 del reparto: caduca en cuanto
alguien añade un elemento. Lo dejo declarado en vez de subir la puntuación con
aserciones que solo se repiten a sí mismas. Si se prefiere lo contrario, son 26
líneas y media hora.

Intenté un oráculo mejor —«toda clase que se pinta tiene regla en la hoja de
estilos», derivado del `.scss` y por tanto no caduco— y **no se puede escribir hoy,
porque fallaría contra la producción actual**: ver el hallazgo siguiente.

### Hallazgo fuera de mi ámbito (regla 9: lo anoto y sigo)

Cuatro clases que la vista pinta **no tienen ninguna regla** en `styles.scss` ni en
`today.scss`:

- `danger`, en el botón «Eliminar suscripción»: la acción destructiva se ve
  exactamente igual que las demás. Es el único de los cuatro con consecuencia de
  producto.
- `external-calendar-counters`, `external-calendar-when` y
  `today-external-calendar-stamp`: ganchos muertos, inofensivos.

No los toco: la hoja de estilos no es de mi carril y el brief pide no refactorizar
de paso.

---

## Resumen

| | |
|---|---|
| Racimos atacados | 9 (A-I), que cubren los 13 racimos de la predicción y sus 20 «omitidos» |
| Defectos de producto encontrados y arreglados | **2** |
| Pruebas | 83 → **161** |
| Mutantes acreditados a mano | 119 aplicados: **111 mueren**, 8 equivalentes declarados |
| Producción tocada | solo por los dos defectos, ambos en `external-calendar.tsx`. `git diff` de producción **vacío** al terminar |
| Puertas | `vitest` 161 verdes · `tsc --noEmit` limpio · `eslint src` limpio · `prettier --check` limpio |

### Los dos defectos, en una línea cada uno

1. **La ventana vacía que nadie había leído**: con un 500 o un 503 en `GET /events`
   la pantalla decía «No hay eventos en la ventana guardada», porque `refuse()`
   relanza una `Response` y la guarda exigía un `Error`. Le decía al propietario que
   su calendario estaba vacío cuando no lo había podido leer.
2. **Salir de la pantalla no cancelaba ninguna escritura**: la limpieza del efecto
   abortaba el controlador del montaje, capturado en el closure, y no el de
   `inFlight`. Un `PUT`, un `POST /sync` o un `DELETE` en vuelo seguían vivos al
   navegar a otra ruta, contra @s39 fila 3.

### Correcciones a la predicción, todas comprobadas ejecutando

- **Racimo 9 (cabeceras), `posibleDefecto` falso**, como decían sus dos
  refutaciones: perder el spread de `options.headers` no puede quitar el
  `X-CSRF-TOKEN`; quita el `Content-Type`. El oráculo que la predicción proponía
  habría dado verde con la producción rota.
- **Racimo 2, «el doble clic de synchronise ya tiene oráculo»: falso.** Lo garantiza
  el atributo `disabled`, no el guardián. El guardián alcanzable de verdad es el de
  `confirmRemoval`, cuyo botón **no** está deshabilitado.
- **La guarda de aborto de `today:66` no es código muerto**, contra lo que sostenía
  el refutador: se alcanza cuando la cancelación llega mientras se lee el cuerpo,
  porque `json()` ya pasó su `throwIfAborted`. Tiene prueba, y observable, con
  rerender en vez de desmontaje.
- **Racimo 3, oráculo (a): el refutador tenía razón** —con un 500 no aparece ningún
  aviso—, solo que eso no era una corrección al oráculo: era el defecto.

Estado final: **161 pruebas verdes** en los cuatro ficheros del ámbito (83 de
partida). `tsc --noEmit`, `eslint src` y `prettier --check` limpios.
