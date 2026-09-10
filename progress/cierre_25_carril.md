# Carril de cierre — feature 25 webhooks · rama `claude/wh-bloqueantes`

10 de septiembre de 2026. Worktree `C:/Users/vhurt/ow-worktrees/wh-bloqueantes`,
reiniciada sobre `main` (`8e246067`). Puerto E2E asignado: 18107 (no se usó: no
hay nada de extremo a extremo en este encargo).

**Nota de encargo:** el fichero `progress/carriles/encargo_25.md` que citaba la
instrucción no existe. Lo que hay es `progress/carriles/bloqueantes_25.md` (el
panel de precierre, B1..B10) y `progress/cierre_25.md` (el veredicto de cierre,
13 condiciones). Trabajé sobre el segundo, que es el vigente y el que reparte
por dueño. Las dos condiciones **de carril que no dependen del propietario** son
la 12 (15 min) y la 13 (25 min): 40 min, que es la estimación del encargo.
A media sesión el coordinador comunicó la decisión del propietario que desbloquea
la **condición 3** y la puso por delante; se hizo primero y también está aquí.

## Resumen

| Condición | Dueño | Desenlace |
|---|---|---|
| 3 — el decodificador rechaza `SECRET_UNREADABLE` y ciega el panel de entregas | carril | **cerrada** |
| 12 — el javadoc de `s35_evenAPoisonedCode…` afirma lo que no hace y sus seis `assertFalse` no pueden fallar | carril | **cerrada** |
| 13 — «no se resuelve DNS» (@s2) y «no se abre conexión saliente» (@s5, @s14) sin oráculo | carril | **cerrada** |
| campaña de frontend, por tocar `webhooks-client.ts` | campaña | ver §4 |

Producción tocada: **un solo fichero**, `frontend/src/webhooks-client.ts`.
`git diff backend/src/main` quedó **vacío** al terminar; los mutantes de mano se
aplicaron y se deshicieron uno a uno, con la clase en verde después de cada uno.

---

## Condición 3 — la octava clase de error deja ciego el panel de entregas

**Decisión que la desbloquea:** el propietario amplía el catálogo de `errorClass`
a ocho; `SECRET_UNREADABLE` se queda en producción. La enmienda de
`features/webhooks.feature` y de `project-spec.md:2038`, con su contrafirma, **la
escribe el coordinador**: yo no toqué el `.feature`.

### El rojo, primero

`frontend/src/webhooks-client.test.ts`, prueba nueva `@s25 decodes
SECRET_UNREADABLE, the class the backend already writes`: una entrega con
`errorClass: "SECRET_UNREADABLE"`, `httpStatus` y `latencyMs` nulos y estado
`exhausted`, que es exactamente la forma que escribe
`WebhookAttempt.unreadableSecret()` y admite la migración `V31`.

```
FAIL  src/webhooks-client.test.ts > @s25 decodes SECRET_UNREADABLE, the class the backend already writes
Error: Confirmación incompatible
 ❯ incompatible src/webhooks-client.ts:64:28
 ❯ decodeDelivery src/webhooks-client.ts:144:11
 ❯ decodeList src/webhooks-client.ts:153:28
 ❯ listWebhookDeliveries src/webhooks-client.ts:250:10
```

### El verde

`webhooks-client.ts`: la octava entrada en el catálogo, con el porqué escrito al
lado (V31, no hubo envío, ni respuesta ni tiempo transcurrido). Nada más.

### La prueba que fijaba el catálogo viejo

`@s40 rejects an error class outside the seven the contract defines` seguía
verde con «BOOM», así que no era falsa, pero contaba a mano —«the seven»— y no
afirmaba nada sobre lo que el backend sí escribe. Reescrita como `@s40 accepts
every error class of the catalogue and rejects any other`: recorre el catálogo
exportado y afirma que **cada una** se decodifica, y después afirma que un valor
inventado se rechaza, **habiendo comprobado antes que ese valor no está en el
catálogo**. Así sigue mordiendo y ya no caduca cuando el catálogo crezca (regla 2
de `REPARTO_NOCHE.md`: nada de constantes que dependan de algo que crece).

Rojo de esa reescritura, acreditado por la vía que las tres leyes admiten —no
compilar/no importar cuenta como fallar—, antes de exportar `webhookErrorClasses`:

```
AssertionError: the given combination of arguments (undefined and string) is invalid
for this assertion.
 ❯ src/webhooks-client.test.ts:504:35
    504| expect(webhookErrorClasses).not.toContain(invented);
```

Después, en verde, se quitó el alias interno `errorClasses` que había dejado el
paso mínimo: la lista exportada es ahora la única.

### El daño de verdad, medido en el panel

La condición describe el daño en la vista, así que ahí queda sujeto también.
Prueba nueva en `frontend/src/webhooks.test.tsx`, `@s25 shows the delivery whose
secret could not be read instead of going blind`. Con el catálogo devuelto a
siete a mano:

```
FAIL src/webhooks.test.tsx > @s25 shows the delivery whose secret could not be read instead of going blind
AssertionError: expected <p role="alert"></p> to be null
+ Received:
<p role="alert">
  No se han podido cargar las entregas.
</p>
 ❯ src/webhooks.test.tsx:1383:5
```

Ese es el alcance exacto del daño, y **no más**: el propietario **sigue viendo
sus endpoints** —`setLoadFailed` cuelga de `listWebhooks`, que no falla— y lo que
pierde es **el panel de entregas de ese endpoint**. La exageración de «no vuelve
a ver ninguna entrega de la cuenta» que corrigió el juez no se repite aquí.

Restaurada la octava clase, la prueba pasa y afirma la fila entera celda a celda
(`Código HTTP` y `Latencia` como guion, `Clase de error` `SECRET_UNREADABLE`,
estado `Agotada`).

### Verificación

- `pnpm --dir frontend exec vitest run src/webhooks-client` → **77/77**.
- `pnpm exec vitest run src/webhooks` (los tres ficheros) → **167/167**.
- `pnpm exec tsc --noEmit` → limpio. `prettier --write` sobre los tres ficheros
  tocados y `eslint` sin salida.

**Lo que NO hice y sigue abierto de esta condición:** la etiqueta en español de
`SECRET_UNREADABLE` en la tabla. No es un olvido: `webhooks.tsx:554` imprime
`{row.errorClass ?? "—"}` en crudo para las ocho, no hay mapa de etiquetas que
ampliar, y el juez descartó expresamente ese trabajo. Si el propietario quiere
traducirlas, es una condición nueva para las ocho, no para ésta.

---

## Condición 12 — seis aserciones que no podían fallar

### El rojo que acredita que la condición era real

Dos mutantes de mano sobre `Slf4jWebhookAudit`, con la prueba
`s35_evenAPoisonedCodeCannotPutAUrlOrASecretInTheTrail` **tal como estaba**:

**M1, el sujeto no registra nada** (los tres `LOG.*` comentados). La prueba falla
—pero mira **dónde**:

```
Slf4jWebhookAuditTest > s35_evenAPoisonedCodeCannotPutAUrlOrASecretInTheTrail() FAILED
    org.opentest4j.AssertionFailedError at Slf4jWebhookAuditTest.java:145
```

La 145 es `assertEquals(3, captured.list.size(), …)`. Las 139-144 son los seis
`assertFalse`, y **pasaron todos** con un sujeto que no escribió ni una línea.
Ésa es la demostración de que no dependían de la conducta del sujeto.

**M2, un texto libre copiado a un campo de más**:
`LOG.warn("outcome=worker_error code={} detail={}", code, code)`. La prueba queda
**entera en verde** (`BUILD SUCCESSFUL`). O sea: se puede duplicar un argumento
de texto libre en el rastro —que es exactamente por donde saldría una URL o un
secreto— y la prueba que dice guardar @s35 no se entera.

### El verde

Sustituida por `s35_b12_aPoisonedFreeTextIsEchoedOnceAndTheAdapterAddsNothing
OfItsOwn`, que **sí entrega al sujeto** los valores prohibidos (URL con
`?token=abc`, `whsec_…` y `v1=…`) por los cuatro parámetros de texto libre del
puerto, y afirma lo único que depende de esta clase y puede fallar:

1. tres llamadas, tres líneas;
2. **un eco por argumento entregado, ni uno más** (4 = 2 de `attempt` + 1 + 1);
3. descontados los ecos, lo que el adaptador pone de su cosecha no contiene nada
   prohibido;
4. ninguna línea adjunta un `throwable`, que es la otra vía por la que viaja una
   traza y que `loggedText()` no mira.

Rojo de la prueba nueva, con M2 todavía puesto:

```
org.opentest4j.AssertionFailedError: un eco por argumento entregado, ni uno más:
endpointId=2222…2222 eventId=1111…1111 status=https://example.com/hooks?token=abc
whsec_AAECAwQF…Hh8 v1=47db42f5…590a774 errorClass=https://example.com/hooks?token=abc …
expected: <4> but was: <5>
```

Quitado M2: `Slf4jWebhookAuditTest` **6/6 verde**, `git diff backend/src/main`
vacío.

### Lo que se dice ahora en el javadoc, y es verdad

Que este adaptador es un **formateador fiel y no censura**; que la ausencia de
URL, secreto, firma y cuerpo en el rastro real la sostienen **sus llamadores**, y
que eso se mide en `WebhookScheduleTest.s35_neitherASuccessfulNorAFailedWorker
Tick…`, que engancha un appender al logger **ROOT** y hace pasar de verdad por el
worker la URL envenenada, el secreto y el cuerpo. La condición ofrecía como
alternativa borrar el javadoc mentiroso; se hizo lo otro y además: envenenar de
verdad **y** dejar escrito dónde vive la garantía que aquí no vive.

---

## Condición 13 — «no se resuelve DNS» y «no se abre conexión saliente»

### La mitad de conducta: el contador del resolutor

`CreateWebhookTest` tenía un resolutor estático y mudo. Ahora lleva una lista
`resolved` con **cada host que la producción pide resolver, en orden**, y dos
pruebas afirman sobre ella:

- **nueva** `s2_anInvalidUrlIsRejectedBeforeResolvingAnyHost`: cinco urls
  inválidas del Examples de @s2, y `resolved` vacía. Es la mitad de @s2:26 que no
  medía nadie.
- **ampliada** `s5_blockedOrUnresolvableDestinationsAreRejectedWithoutInserting`:
  `resolved` es exactamente `[mixed.example, missing.example]`. Sólo los hosts
  **con nombre** llegan al resolutor —las cuatro direcciones literales se deciden
  sobre la propia dirección, sin preguntar a nadie— y cada uno **una sola vez**,
  así que el veredicto sale de una resolución de verdad.

**Rojo acreditado, y con un mutante que no caza nadie más.** El primero que
probé (adelantar `destinations.check(url)` por delante de la validación) también
lo caza `s34`, así que no demostraba que hiciera falta el contador. El bueno es
un «calentar el DNS» silencioso, que no cambia ni un código de error ni inserta
nada:

```java
try { destinations.check(url); } catch (RuntimeException ignored) { }
var intent = new WebhookIntent(url, description, eventTypes);
```

De las 6 pruebas de la clase fallan **exactamente las dos del contador**; `s34`
y las tres de `@s1` siguen verdes:

```
s2_anInvalidUrlIsRejectedBeforeResolvingAnyHost
  AssertionFailedError: una url inválida se rechaza sin resolver ningún host
  ==> expected: <[]> but was: <[example.com, example.com, example.com, example.com, example.com]>

s5_blockedOrUnresolvableDestinationsAreRejectedWithoutInserting
  AssertionFailedError: sólo los hosts con nombre llegan al resolutor, y cada uno exactamente una vez
  ==> expected: <[mixed.example, missing.example]>
      but was: <[mixed.example, mixed.example, missing.example, missing.example]>
```

Restaurada la producción: `CreateWebhookTest` **7/7**, `git diff
backend/src/main` vacío.

### La mitad estructural: el colaborador que no existe

Escrita donde la condición pedía —`progress/literales_sin_oraculo_webhooks.md`,
adenda del 10 de septiembre— y además sujeta por dos pruebas:

- `CreateWebhookTest#s5_creatingHasNoSenderToOpenAnOutgoingConnectionWith`
- `ManageWebhookTest#s14_thePingHasNoSenderToReachTheReceiverWithOnTheHttpThread`

Ninguno de los dos casos de uso declara `WebhookSender` entre sus colaboradores,
así que no tienen con qué salir a la red; el único que lo declara es
`DispatchWebhooks`, al que sólo llama `WebhookSchedule.tick()`.

Cada una lleva su **aserción de control** sobre `DispatchWebhooks` para no
repetir el defecto de la condición 12: una aserción de ausencia sólo vale si el
predicado encuentra la cosa allí donde sí está. Medido en la misma ejecución: el
mismo predicado da `false` en `CreateWebhook` y `ManageWebhook` y `true` en
`DispatchWebhooks`. **Aquí no hay mutante de producción**: cambiar la firma de un
constructor arrastra cuatro llamadores, y el control es la acreditación honesta
de que la aserción discrimina. Lo digo tal cual para que nadie lo lea como un
rojo de producción, que no lo es.

`ManageWebhookTest` **13/13**.

---

## 4. La campaña de frontend

Tocar `webhooks-client.ts` invalida el 94,57 % medido esta mañana, así que
relancé **una sola campaña**, la de frontend de la 25:

```
node scripts/project.mjs mutate webhooks-frontend
```

(`stryker.webhooks.config.json`, ámbito `src/webhooks-client.ts` y
`src/webhooks.tsx`, `break: 80`, sin tocar umbral ni ámbito). Ninguna otra
campaña se lanzó desde este carril. **La cifra se recomputa del `mutation.json`
—`Killed+Timeout` sobre el total—, nunca se lee del HTML.**

### Resultado, recomputado del `mutation.json`

```
global: {"Killed":592,"Survived":33,"NoCoverage":1,"RuntimeError":1}
recomputado: 592 / 626 = 94.57 %          (break: 80 — pasa)
src/webhooks-client.ts   Killed 280, Survived 6, NoCoverage 0   -> 97.90 %
src/webhooks.tsx         Killed 312, Survived 27, NoCoverage 1  -> 91.76 %
```

El denominador son `Killed + Timeout + Survived + NoCoverage`; el único
`RuntimeError` queda fuera, igual que en el recuento del juez (429+3 contra
151+22 = 605). Coincide con lo que imprime Stryker, pero la cifra que publico es
la recomputada. Duración: 12 min 57 s, 627 mutantes, informe en
`frontend/reports/mutation-webhooks/mutation.json`.

Los seis supervivientes de `webhooks-client.ts` están todos en los ayudantes de
decodificación (`identifier` L73, `whole` L78, el `exact` de L102 y las dos
guardas de `disabledReason`/`disabledAt` de L115 y L119) y **ninguno cae sobre lo
que toqué**. La cifra sale igual que la de esta mañana, 94,57 %, y ahora está
medida sobre un árbol que sí incluye la octava clase.

### Un límite que no oculto

Stryker genera **cero mutantes** sobre el catálogo: las líneas 1-45 de
`webhooks-client.ts` —donde viven `webhookEventTypes` y `webhookErrorClasses`—
no reciben ni uno (comprobado contando mutantes por línea en el informe). O sea
que la octava clase **no la protege la puerta de mutación**, la protegen los
oráculos, y por eso son dos y nombran la cadena literalmente:

- `@s40 accepts every error class of the catalogue…` **no puede** notar que
  alguien borre una entrada del catálogo: recorre esa misma lista, así que se
  encogería con ella. Lo que sí caza es que el decodificador deje de aceptar
  alguna, y que acepte una inventada.
- `@s25 decodes SECRET_UNREADABLE…` (cliente) y `@s25 shows the delivery whose
  secret could not be read…` (panel) **sí** la nombran por su literal, y las dos
  se pusieron rojas al devolver el catálogo a siete. Ésa es la pareja que sujeta
  la decisión del propietario.

La misma limitación vale para las otras siete clases, que nadie nombra por su
literal fuera del catálogo. No lo arreglo aquí porque no es lo que pedía la
condición 3, pero queda dicho: si alguien borra `TLS` del catálogo, hoy no se
entera nadie.

---

## Lo que queda abierto, y por qué

- **Condición 3, la etiqueta en español**: descartada por el juez, no hay mapa de
  etiquetas que ampliar. Si se quiere, es condición nueva para las ocho clases.
- **Enmiendas del contrato** (@s25 y el catálogo de ocho, @s32 a 2500 ms, @s9,
  @s8): del propietario y del coordinador. **No toqué `features/webhooks.feature`.**
- **Condiciones 1, 9** (campañas), **7, 8, 10, 11** (orquestador), **2, 4, 5, 6**
  (propietario): fuera de este carril.
- **Condición 13, lo que las dos pruebas estructurales NO dicen**: que la
  respuesta HTTP no espere a nada lento. Sólo que por esos dos caminos no sale
  una petición al receptor.
- **`bin/harness init` en verde**: no lo ejecuté; es la condición 9 y la corre el
  coordinador sobre el árbol integrado. Lo que sí está medido aquí, por clase y
  por fichero, es lo de arriba.
