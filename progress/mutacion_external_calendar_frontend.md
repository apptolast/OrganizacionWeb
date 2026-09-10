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
