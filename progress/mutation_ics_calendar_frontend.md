# Mutación frontend — feature 26 `ics_calendar` (Stryker)

**Veredicto: FAIL** — Score: **268/383 = 69.97 %**, frente al umbral del **80 %**. Faltan **39
mutantes** por matar para alcanzarlo (307/383 = 80.16 %).

El denominador es íntegro: 383 mutantes generados, 383 contabilizados. No he reclasificado ni
excluido ninguno, tampoco los 16 NoCoverage ni los cinco que declaro equivalentes: siguen contando
en el denominador y el 69.97 % los incluye.

## Ejecución

| | |
| --- | --- |
| Comando | `node scripts/project.mjs mutate ics_calendar-frontend` |
| Despacho real | `pnpm --dir frontend exec stryker run stryker.ics-calendar.config.json` |
| Checkout | `a6164e4` (`main`), árbol limpio salvo ficheros sin seguimiento de otro carril |
| Ventana | 2026-09-09T11:34:29Z entrada, 2026-09-09T11:45:49Z salida (11 min 18 s) |
| Código de salida | **1** (`Final mutation score 69.97 under breaking threshold 80`) |
| Ficheros mutados | 4 de 199; 383 mutantes instrumentados; 28,21 pruebas por mutante de media |
| Informes | `frontend/reports/mutation-ics-calendar/mutation.json` y `mutation.html` |

Campaña lanzada **después** de que la de backend terminara y los contenedores bajaran a 3 (todos de
otros carriles). Stryker no levanta contenedores.

## Tabla de estados (denominador íntegro)

| Estado | Total | calendar-feed-api.ts | calendar.tsx | App.tsx | workspace.tsx |
| --- | ---: | ---: | ---: | ---: | ---: |
| Killed | 268 | 92 | 163 | 8 | 5 |
| Survived | 99 | 30 | 69 | 0 | 0 |
| NoCoverage | 16 | 2 | 14 | 0 | 0 |
| Timeout | 0 | 0 | 0 | 0 | 0 |
| RuntimeError | 0 | 0 | 0 | 0 | 0 |
| CompileError / NonViable | 0 | 0 | 0 | 0 | 0 |
| Ignored | 0 | 0 | 0 | 0 | 0 |
| **Total** | **383** | **124** | **246** | **8** | **5** |

Score por fichero: `App.tsx` 100 %, `workspace.tsx` 100 %, `calendar-feed-api.ts` 74.19 %,
`calendar.tsx` **66.26 %**. Los dos ficheros compartidos (los rangos línea:columna de la entrada de
navegación y la rama de ruta) están perfectos; el agujero está entero en el cliente y la vista.

---

# Supervivientes de `src/calendar-feed-api.ts` (30 Survived + 2 NoCoverage)

Enumerados uno a uno, con línea:columna, mutador, sustitución y juicio individual.

1. **11:22 Regex** — `FEED_ADDRESS` pierde el ancla `^`. **Hueco real.** Ninguna prueba envía una
   url con basura antes del esquema. Falta: `createCalendarFeed` que recibe
   `url: "javascript:https://host/calendar/<43>.ics"` debe declararla incompatible.
2. **11:22 Regex** — `FEED_ADDRESS` pierde el ancla `$`. **Hueco real.** Falta:
   `url: "https://host/calendar/<43>.ics/../otra"` debe ser incompatible.
3. **26:19 StringLiteral** — el mensaje de `incompatible()` pasa a `""`. **Hueco real, menor.** El
   mensaje es observable en `Error.message` y ninguna prueba lo afirma. Falta: una aserción sobre el
   mensaje, o dejar escrito que el contrato no lo fija.
4. **33:43 ObjectLiteral** — `apiRequest(FEED, { signal })` pasa a `apiRequest(FEED, {})`. **Hueco
   real.** La petición deja de ser cancelable de verdad; sólo la tapan los `throwIfAborted`
   posteriores. Falta: afirmar que el fetch del GET recibe la señal.
5. **34:3 CallExpression** — `signal.throwIfAborted()` tras el GET pasa a `;`. **Hueco real.**
   Falta: abortar entre la llegada de la respuesta y el parseo, y exigir `AbortError`.
6. **35:7 ConditionalExpression** — `response.status !== 200` pasa a `false`. **Hueco real.**
   Falta: un 500 con cuerpo JSON válido debe lanzar la `Response`, no devolver el estado.
7. **36:55 ArrowFunction** — el `.catch(() => incompatible())` del JSON pasa a `() => undefined`.
   **Hueco real.** Falta: 200 con cuerpo que no es JSON debe ser incompatible.
8. **37:45 ConditionalExpression** — `typeof status.active !== "boolean"` pasa a `false`. **Hueco
   real.** Falta: `{active: "true", createdAt: null}` debe ser incompatible.
9. **52:3 CallExpression** — `signal.throwIfAborted()` tras el POST pasa a `;`. **Hueco real.**
   Igual que el 5, en `createCalendarFeed`.
10. **53:7 ConditionalExpression** — `response.status !== 201` pasa a `false`. **Hueco real.**
    Falta: una creación que responde 200 en vez de 201 debe lanzar la `Response`.
11. **54:53 ArrowFunction** — el `.catch(() => incompatible())` de la creación pasa a
    `() => undefined`. **Hueco real.** Igual que el 7, en la creación.
12. **57:5 ConditionalExpression** — `typeof link.url !== "string"` pasa a `false`. **Hueco real,
    exótico.** Queda casi enmascarado por `FEED_ADDRESS.test(link.url)`, que coacciona a texto: con
    `url: 123` el mutante moriría igual. Pero **no es equivalente**: con
    `url: ["https://host/calendar/<43>.ics"]` la coacción produce la cadena válida y el módulo
    devolvería un array donde promete un `string`. Falta esa fila.
13. **68:3 CallExpression** — `signal.throwIfAborted()` de `revokeCalendarFeed` pasa a `;`. **Hueco
    real.** Falta: abortar entre la respuesta y la comprobación del 204.
14. **75:47 ObjectLiteral** — las opciones de la descarga pasan a `{}`. **Hueco real.** Pierde a la
    vez la señal y el `Accept`. Falta: afirmar ambas cosas en la llamada.
15. **77:14 ObjectLiteral** — `headers: { Accept: "text/calendar" }` pasa a `{}`. **Hueco real.**
    Falta: afirmar que la descarga negocia `Accept: text/calendar`.
16. **77:24 StringLiteral** — `Accept: "text/calendar"` pasa a `""`. **Hueco real.** Igual que el 15.
17. **82:6 Regex** — el Content-Type pierde el ancla `^`. **Hueco real.** Falta:
    `application/json, text/calendar; charset=utf-8` debe ser incompatible.
18. **82:6 Regex** — el Content-Type pierde el ancla `$`. **Hueco real.** Falta:
    `text/calendar; charset=utf-8; boundary=x` debe ser incompatible.
19. **82:6 Regex** — `\s*` pasa a `\S*` antes del punto y coma. **Hueco real.** Falta:
    `text/calendarX;charset=utf-8` debe ser incompatible.
20. **82:6 Regex** — `\s*` pasa a `\s` tras el punto y coma, es decir, **obliga a que haya
    exactamente un espacio**. **Hueco real y el más grave de este fichero.** Todas las pruebas usan
    `"text/calendar; charset=utf-8"` **con espacio** (`calendar-feed-api.test.ts:28`), así que el
    mutante que exige el espacio no lo nota nadie. Y resulta que **producción no lo lleva**: el
    ciclo 14 del TDD documenta que Tomcat entrega `text/calendar;charset=utf-8` **sin espacio**. La
    forma real del despliegue no está cubierta por ninguna prueba unitaria, y este mutante es
    exactamente el código que la rechazaría. Falta: una fila con `"text/calendar;charset=utf-8"`
    sin espacio que **debe aceptarse**.
21. **83:47 StringLiteral (NoCoverage)** — el `?? ""` del Content-Type pasa a `?? "Stryker was
    here!"`. **Hueco real.** La rama del `??` no se ejecuta jamás. Falta: una descarga sin cabecera
    `Content-Type`, que debe ser incompatible.
22. **88:53 CallExpression (NoCoverage)** — el `incompatible()` del Content-Length pasa a `;`.
    **Hueco real.** Nunca se alcanza. Falta: una descarga con `Content-Length` ausente.
23. **88:7 ConditionalExpression** — toda la condición del Content-Length pasa a `false`. **Hueco
    real.** Igual que el 22.
24. **88:7 LogicalOperator** — el `o lógico` de la condición pasa a `y lógico`. **Hueco real.**
    Falta: `Content-Length` ausente, que mata el lado izquierdo por separado.
25. **88:21 Regex** — el Content-Length pierde el ancla `^`. **Hueco real.** Falta:
    `Content-Length: x12` debe ser incompatible.
26. **88:21 Regex** — el Content-Length pierde el ancla `$`. **Hueco real.** Falta:
    `Content-Length: 12x` debe ser incompatible.
27. **88:21 Regex** — `[0-9]*` pasa a `[0-9]`, es decir, exige exactamente dos dígitos. **Hueco
    real.** Todas las pruebas usan `String(bytes.length)`, que da un número de dos cifras. Falta:
    un cuerpo cuya longitud tenga una o tres cifras.
28. **90:3 CallExpression** — `signal.throwIfAborted()` tras leer los octetos pasa a `;`. **Hueco
    real.** Falta: abortar mientras se consume el cuerpo.
29. **92:45 ObjectLiteral** — las opciones del `TextDecoder` pasan a `{}`. **Hueco real.** Falta:
    los dos casos de los puntos 30 y 31.
30. **93:12 BooleanLiteral** — `fatal: true` pasa a `false`. **Hueco real e importante.** `fatal`
    es lo único que rechaza UTF-8 malformado; con `false` los octetos rotos se sustituyen por
    U+FFFD y el documento se acepta. Falta: una descarga con una secuencia UTF-8 inválida, que debe
    ser incompatible.
31. **94:16 BooleanLiteral** — `ignoreBOM: true` pasa a `false`. **Hueco real.** Con `false` el BOM
    se descarta y `startsWith("BEGIN:VCALENDAR")` pasa; con `true` el BOM queda y el documento se
    rechaza. Falta: una descarga con BOM al principio, que debe ser incompatible.
32. **79:3 CallExpression** — `signal.throwIfAborted()` tras la respuesta de la descarga pasa a
    `;`. **Hueco real.** Falta: abortar entre la llegada de la cabecera y la validación del
    Content-Type.

Recuento: 32 entradas para 30 `Survived` más 2 `NoCoverage`; los puntos 21 y 22 son los dos
`NoCoverage`.

---

# Supervivientes de `src/calendar.tsx` (69 Survived + 14 NoCoverage = 83)

## A. Equivalentes genuinos (5), con argumento individual

Los declaro uno a uno, con su razón, y **siguen contando en el denominador**: el 69.97 % los incluye.

A1. **67:9 ConditionalExpression** — `if (confirming)` pasa a `true`. **Equivalente.** El
`<div role="group" ref={confirmation}>` sólo se monta dentro de `{confirming && (...)}` (línea 282);
por tanto `confirmation.current` es `null` exactamente cuando `confirming` es `null`, y
`confirmation.current?.focus()` se queda en no-op por el encadenamiento opcional. Forzar la
condición a `true` ejecuta ese mismo no-op. Ninguna prueba puede separarlos sin romper la invariante
de renderizado, que ya está cubierta.

A2. **168:9 ConditionalExpression** — `if (!link) return;` en `copyLink` pasa a `false`.
**Equivalente bajo invariante de renderizado.** El botón «Copiar enlace» sólo existe dentro de
`{link && (...)}` (líneas 235 y 250), así que `copyLink` nunca se invoca con `link === null`: la
guarda es código defensivo inalcanzable desde la interfaz. Caveat honesto: es equivalencia
condicionada a esa invariante, no absoluta; si algún día se llamara a `copyLink` desde otro sitio,
dejaría de serlo.

A3. **170:9 ConditionalExpression** — `if (!clipboard?.writeText) return setCopy("manual");` pasa a
`false`. **Equivalente.** Sin la guarda, `await clipboard.writeText(link.url)` lanza `TypeError`
(portapapeles ausente, o `writeText` que no es función), el `catch` de la línea 174 lo recoge y
ejecuta `setCopy("manual")`: exactamente el mismo estado observable que producía la vía rápida. No
hay salida del DOM que los distinga.

A4. **85:11 ConditionalExpression** — `if (objectUrl.current)` en la limpieza de desmontaje pasa a
`true`. **Equivalente.** Cuando `objectUrl.current` es `null`, `URL.revokeObjectURL(null)` revoca una
url que nunca se registró: no lanza y no tiene efecto observable.

A5. **152:13 ConditionalExpression** — `if (objectUrl.current)` antes de crear el Blob pasa a `true`.
**Equivalente** por el mismo argumento que A4: en la primera descarga no hay url previa, y revocar
una inexistente es un no-op.

## B. Huecos reales (78)

### B1. El seguimiento del foco por `focusin` no lo sujeta nada (10)

1. **58:3 CallExpression** — el `useEffect` entero pasa a `;`.
2. **58:19 BlockStatement** — el cuerpo del efecto se vacía.
3. **59:42 BlockStatement** — el cuerpo del manejador `moved` se vacía.
4. **60:11 ConditionalExpression** — pasa a `true`: siempre olvida el iniciador.
5. **60:11 ConditionalExpression** — pasa a `false`: nunca lo olvida.
6. **60:11 EqualityOperator** — `event.target !== initiator.current` pasa a `===`.
7. **62:31 StringLiteral** — `addEventListener("focusin", …)` pasa a `""`: nunca se registra.
8. **63:47 StringLiteral** — `removeEventListener("focusin", …)` pasa a `""`: nunca se retira (fuga).
9. **63:12 ArrowFunction** — la función de limpieza pasa a `() => undefined`.
10. **64:6 ArrayDeclaration** — las dependencias `[]` pasan a `["Stryker was here"]`: el listener se
    registra de nuevo en cada render.

**Falta** (mata a los diez): una prueba que active un control que lanza una operación, mueva el foco
a otro elemento mientras está en vuelo y exija que al terminar el foco **no** vuelva ni al control ni
al `h1`; más su complementaria, en la que sin mover el foco sí vuelve. Es la mitad de `@s35` que hoy
no está demostrada.

### B2. La restauración del foco al terminar (7)

11. **72:9 ConditionalExpression** — `if (busy) return;` pasa a `false`.
12. **75:20 ConditionalExpression** — la condición del `if` pasa a `true`.
13. **75:9 LogicalOperator** — `control && document.activeElement === document.body` pasa a `o lógico`.
14. **76:32 CallExpression (NoCoverage)** — `control.focus();` pasa a `;`.
15. **76:11 ConditionalExpression** — `if (control.isConnected)` pasa a `false`.
16. **77:12 OptionalChaining** — `heading.current?.focus()` pasa a acceso sin llamada.
17. **67:21 OptionalChaining** — `confirmation.current?.focus()` pasa a acceso sin llamada: el grupo
    de confirmación nunca recibe el foco.

El 14, en `NoCoverage`, es el dato duro: **la rama que devuelve el foco a un control que sigue en el
DOM no se ejecuta en toda la suite**. **Falta:** tras una descarga terminada, afirmar que
`document.activeElement` es el botón «Descargar archivo .ics»; tras una revocación (que hace
desaparecer el botón que la inició), afirmar que el foco cae en el `h1`; y al abrir la confirmación,
afirmar que el `role="group"` tiene el foco, que es lo que `@s34` promete.

### B3. El foco inicial, el anuncio de carga y las dependencias de los efectos (4)

18. **55:5 OptionalChaining** — `heading.current?.focus()` del montaje pasa a acceso sin llamada: el
    `h1` no recibe el foco al entrar. **Falta:** afirmar que al montar el `h1` tiene el foco.
19. **56:6 ArrayDeclaration** — las dependencias `[]` de ese efecto pasan a `["Stryker was here"]`.
    **Falta:** lo mismo que el 18, comprobado tras un segundo render.
20. **41:49 StringLiteral** — `useState<Busy | null>("loading")` pasa a `""`: la vista arranca sin
    estado ocupado y sin anuncio. **Falta:** afirmar el `role="status"` «Cargando…» en el primer
    render.
21. **128:6 ArrayDeclaration** — las dependencias `[]` del efecto de carga pasan a
    `["Stryker was here"]`: relee el estado en cada render. **Falta:** afirmar que tras varios
    renders el GET a `/api/v1/me/calendar-feed` se ha hecho **exactamente una vez**. El comentario de
    la línea 126 lo promete y nada lo comprueba.

### B4. Las carreras de `run` (8)

22. **98:9 ConditionalExpression** — `if (pending.current) return;` pasa a `false`.
23. **105:11 ConditionalExpression** — la guarda de descarte tras el `await` pasa a `false`.
24. **105:11 LogicalOperator** — su `o lógico` pasa a `y lógico`.
25. **105:40 ConditionalExpression** — `pending.current !== controller` pasa a `false`.
26. **108:11 ConditionalExpression** — la guarda equivalente del `catch` pasa a `false`.
27. **108:11 LogicalOperator** — su `o lógico` pasa a `y lógico`.
28. **108:40 ConditionalExpression** — `pending.current !== controller` del `catch` pasa a `false`.
29. **111:11 ConditionalExpression** — `if (pending.current === controller)` del `finally` pasa a `true`.

Que el 22 sobreviva es lo más instructivo de la campaña: **sí existe** una prueba
`@s32 sends exactly one creation on a double activation` (`calendar.test.tsx:151`) que cuenta los
POST. Sobrevive porque `userEvent.dblClick` sobre el `fetch` simulado no llega a solapar dos llamadas
en vuelo —la primera resuelve antes de la segunda activación—, así que la guarda nunca se ejerce. La
prueba comprueba el resultado, no la carrera.

**Falta:** una prueba que retenga la primera respuesta con una promesa diferida, active el control por
segunda vez **con la primera aún en vuelo** y sólo entonces cuente un único POST. Y las de respuesta
tardía: dejar que una petición vieja resuelva **después** de haber sido sustituida y exigir que no
toque el estado. Es justo lo que `@s37` dice cubrir con el «401 tardío descartado», y los seis
mutantes de las líneas 105 y 108 dicen que no lo cubre.

### B5. Los efectos de cada operación sobre el estado (12)

30. **102:5 CallExpression** — `setFailure(null);` pasa a `;`: el fallo anterior no se limpia al reintentar.
31. **121:7 CallExpression** — `setLink(null);` de `load` pasa a `;`.
32. **134:7 CallExpression** — `setCopy(null);` de `generate` pasa a `;`.
33. **135:7 CallExpression** — `setConfirming(null);` de `generate` pasa a `;`.
34. **139:9 StringLiteral** — `run("revoking", …)` pasa a `""`.
35. **139:21 StringLiteral** — `run(…, "revoke", …)` pasa a `""`.
36. **140:17 ObjectLiteral** — `setStatus({active: false, createdAt: null})` pasa a `setStatus({})`.
37. **141:7 CallExpression** — `setLink(null);` de `revoke` pasa a `;`.
38. **142:7 CallExpression** — `setCopy(null);` de `revoke` pasa a `;`.
39. **143:7 CallExpression** — `setConfirming(null);` de `revoke` pasa a `;`.
40. **148:7 StringLiteral** — `"downloading"` pasa a `""`.
41. **149:7 StringLiteral** — `"download"` pasa a `""`.

**Falta:** aserciones de estado *después* de cada operación, no sólo del camino feliz. Que tras
revocar desaparecen el enlace, el aviso de copia y la confirmación, y que el estado queda
`{active: false, createdAt: null}` y no un objeto vacío. Que el anuncio en vuelo dice «Revocando
enlace…» y «Preparando archivo…»: los de `loading` y `creating` sí están afirmados —sus mutantes de
las líneas 22 y 23 murieron— y los de `revoking` y `downloading` no. Y una prueba de reintento que
exija que el `role="alert"` anterior desaparece al empezar el nuevo intento.

### B6. Texto visible, Blob y formato de fecha (7)

42. **24:13 StringLiteral** — `revoking: "Revocando enlace…"` pasa a `""`. Mismo hueco que B5.
43. **25:16 StringLiteral** — `downloading: "Preparando archivo…"` pasa a `""`. Mismo hueco que B5.
44. **154:47 ObjectLiteral** — las opciones del `new Blob([...], {type})` pasan a `{}`.
45. **155:19 StringLiteral** — `type: "text/calendar;charset=utf-8"` del Blob pasa a `""`.
    **Falta** (44 y 45): afirmar el `type` del Blob que se ofrece a descargar.
46. **16:42 StringLiteral** — `new Intl.DateTimeFormat("es", …)` pasa a `""`.
47. **17:14 StringLiteral** — `dateStyle: "medium"` pasa a `""`.
48. **18:14 StringLiteral** — `timeStyle: "short"` pasa a `""`.

**Anomalía que dejo anotada sin resolver, porque no la sé explicar y no la voy a maquillar.** Los
tres últimos (46, 47 y 48) son precisamente los que hacen que `new Intl.DateTimeFormat(...)` lance
`RangeError` al evaluar el módulo (comprobado: `new Intl.DateTimeFormat("")` lanza «Incorrect locale
information provided»). El mutante hermano del mismo sitio que **no** lanza —`16:48`, el objeto de
opciones a `{}`— **sí murió**, igual que los literales de `ANNOUNCEMENTS` y `CONFIRMATIONS`, así que
los mutantes de nivel de módulo se activan sin problema. La explicación más plausible es que un
mutante que revienta la *importación* del módulo deja el fichero de pruebas sin ejecutar ninguna
prueba, y Stryker lo anota «Survived» en vez de «Killed» o «RuntimeError». Si es así, estos tres no
son ni equivalentes ni un hueco de las pruebas, sino un punto ciego del corredor. **No los excluyo
del denominador**: el 69.97 % los cuenta. Refuerza la hipótesis que la prueba `@s32` de la línea 165
sí afirma la fecha formateada con su propio ayudante `readable()`, que usa la misma configuración: la
aserción existe y debería haberlos matado.

### B7. El mapeo del 413 y la lógica de reintento (8)

49. **162:9 ConditionalExpression** — pasa a `true`: todo fallo de descarga se trata como «limit».
50. **162:38 ConditionalExpression** — pasa a `true`: cualquier `Response` se trata como «limit».
51. **162:9 LogicalOperator** — `error instanceof Response && error.status === 413` pasa a `o lógico`.
52. **187:21 ConditionalExpression** — pasa a `true`.
53. **187:41 ConditionalExpression** — pasa a `true`.
54. **187:21 LogicalOperator** — `failure !== null && failure !== "limit"` pasa a `o lógico`, que es
    una tautología: el botón de reintentar se muestra siempre.
55. **187:53 StringLiteral** — `failure !== "limit"` pasa a `failure !== ""`.
56. **187:21 ConditionalExpression** — segunda variante que fuerza `retriable` a `true`.

**Falta:** una prueba de descarga que falle con algo que **no** sea 413 (por ejemplo 503) y exija el
mensaje genérico **y la presencia** del botón «Reintentar»; y su pareja con 413, que exija el mensaje
del límite **y la ausencia** del botón. Hoy sólo una de las dos caras está sujeta, y por eso
`retriable` puede forzarse a `true` sin que nadie proteste.

### B8. El reintento sólo está probado para el fallo de estado (13)

57. **189:9 ConditionalExpression** — `if (failure === "status")` pasa a `true`.
58. **190:9 ConditionalExpression (NoCoverage)** — pasa a `true`.
59. **190:9 ConditionalExpression (NoCoverage)** — pasa a `false`.
60. **190:9 EqualityOperator (NoCoverage)** — `failure === "generate"` pasa a `!==`.
61. **190:21 StringLiteral (NoCoverage)** — `"generate"` pasa a `""`.
62. **191:9 ConditionalExpression (NoCoverage)** — pasa a `true`.
63. **191:9 ConditionalExpression (NoCoverage)** — pasa a `false`.
64. **191:9 EqualityOperator (NoCoverage)** — `failure === "revoke"` pasa a `!==`.
65. **191:21 StringLiteral (NoCoverage)** — `"revoke"` pasa a `""`.
66. **192:9 ConditionalExpression (NoCoverage)** — pasa a `true`.
67. **192:9 ConditionalExpression (NoCoverage)** — pasa a `false`.
68. **192:9 EqualityOperator (NoCoverage)** — `failure === "download"` pasa a `!==`.
69. **192:21 StringLiteral (NoCoverage)** — `"download"` pasa a `""`.

Doce de los dieciséis `NoCoverage` de toda la campaña están aquí: **las tres ramas de reintento de
generación, revocación y descarga no se ejecutan nunca**. `@s35` dice «repite el paso fallido y nada
más» y sólo está demostrado para el estado. **Falta:** tres pruebas. Fallar la creación, pulsar
«Reintentar» y exigir un segundo POST y ningún DELETE ni GET de descarga; lo mismo para la revocación
(DELETE) y para la descarga (GET a `/calendar.ics`).

### B9. Limpieza de las object URL (5)

70. **85:30 CallExpression** — `URL.revokeObjectURL(objectUrl.current);` del desmontaje pasa a `;`.
71. **85:11 ConditionalExpression** — la condición del desmontaje pasa a `false`: nunca revoca.
72. **88:5 ArrayDeclaration** — las dependencias de esa limpieza pasan a `["Stryker was here"]`.
73. **152:13 ConditionalExpression** — la condición previa al Blob pasa a `false`: nunca revoca la
    url anterior antes de crear la nueva.
74. **152:32 CallExpression (NoCoverage)** — la revocación de la url anterior pasa a `;`.

El 74 sin cobertura significa que **nadie descarga dos veces** en toda la suite, así que la fuga de la
url previa no está protegida. **Falta:** espiar `URL.revokeObjectURL` y exigir que se llame al
desmontar después de una descarga, y que se llame con la url vieja al preparar una segunda.

### B10. Accesibilidad y detalles del DOM (4)

75. **197:35 UnaryOperator** — `<h1 ref={heading} tabIndex={-1}>` pasa a `tabIndex={+1}`.
76. **287:21 UnaryOperator** — el `tabIndex={-1}` del grupo de confirmación pasa a `{+1}`.
77. **248:22 ArrowFunction** — `onFocus={(event) => event.currentTarget.select()}` pasa a no-op.
78. **297:15 CallExpression** — `setConfirming(null);` dentro del botón de confirmar pasa a `;`.

Los dos `tabIndex` positivos son un defecto de accesibilidad real (orden de tabulación forzado) que la
suite de Vitest no ve. El E2E con axe de `@s38` probablemente lo marcaría, pero **ese recorrido no
forma parte de esta campaña** y no puedo acreditarlo aquí. **Falta:** afirmar
`toHaveAttribute("tabindex", "-1")` en el `h1` y en el grupo; afirmar que al enfocar el campo de url
su contenido queda seleccionado; y afirmar que al confirmar desaparece el grupo de confirmación.

**Cuadre de `calendar.tsx`:** 5 equivalentes (A1-A5) + 78 huecos (B1-B10) = **83**, que son los 69
`Survived` más los 14 `NoCoverage` de la tabla de estados. Ninguno queda sin enumerar.

---

## Qué hace falta para pasar del 80 %

Hacen falta **39 muertes más** (307/383 = 80.16 %). Ordenadas por retorno:

1. **B8, las tres ramas de reintento** (13 mutantes, todos sin cobertura). Tres pruebas.
2. **B1 + B2, el contrato de foco de `@s34` y `@s35`** (17 mutantes). Dos o tres pruebas.
3. **B4, las carreras de `run`** (8 mutantes). Dos pruebas con promesas diferidas.
4. **B5, el estado posterior a revocar y los anuncios en vuelo** (12 mutantes). Dos pruebas.

Sólo con los cuatro primeros bloques se superan los 39 sin tocar el cliente. Los 32 supervivientes de
`calendar-feed-api.ts` suben ese fichero del 74 % al entorno del 95 % con media docena de filas de
tabla sobre cabeceras y cuerpos malformados, y **la número 20 debería escribirse aunque no hiciera
falta para la cifra**, porque cubre la forma del `Content-Type` que producción emite de verdad.

No propongo recortar el alcance ni relajar el umbral. Nada de esto es trabajo mío: le corresponde al
`tdd_craftsman`, en rojo primero, y el `judge` debe volver a pasar antes de reejecutar esta campaña.

## Qué NO he verificado

- **La campaña de backend no produjo score alguno**; ver `progress/mutation_ics_calendar_backend.md`.
  La calidad de las 95 pruebas del backend de esta feature sigue sin medir.
- No he ejecutado la suite completa de Vitest, ni Playwright, ni axe. La mutación parte de su propia
  ejecución en seco y no acredita el estado del resto del frontend.
- No he abierto el informe HTML; he trabajado sobre `mutation.json` y el resumen de texto.
- No he llegado al fondo de la anomalía de los tres mutantes de `Intl.DateTimeFormat` (B6): doy la
  hipótesis más plausible y la dejo abierta.
- Los rangos línea:columna de `App.tsx` y `workspace.tsx` los he dado por buenos porque
  `scripts/project.test.mjs` los guarda; no los he recalculado a mano. Ambos ficheros salieron al
  100 %, así que la parte mutada de ellos sí está sujeta.
- No he tocado código, pruebas, `.feature` ni `feature_list.json`.

## Nota sobre el desplazamiento del checkout

La campaña corrió íntegra sobre `a6164e4` (11:34:29Z a 11:45:49Z). El merge `a08d3be` de la
feature 27 entró después (13:47:50 local, es decir 11:47:50Z), así que **no la contaminó**.
Comprobado además que los dos ficheros que aportan el 100 % de los supervivientes,
`src/calendar.tsx` y `src/calendar-feed-api.ts`, **no han cambiado** entre `a6164e4` y el `HEAD`
posterior (`git diff --name-only` vacío para ambos): las 115 entradas de este informe siguen
siendo válidas tal cual.

Lo que sí cambió con ese merge es `src/App.tsx` y, con él, los rangos línea:columna que
`stryker.ics-calendar.config.json` usa para mutar los ficheros compartidos (el propio config se
tocó en el merge). Esos rangos aportaron 13 mutantes, los 13 muertos. Al relanzar la campaña esa
parte del denominador puede moverse ligeramente; el agujero del 69.97 %, no.
