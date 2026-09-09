# Arreglo del oráculo: `s13` corte por tamaño de un cuerpo interminable

Fichero tocado: `backend/src/test/java/com/apptolast/organization/adapter/feed/HttpCalendarFeedTest.java`.
**Producción sin cambios** (`HttpCalendarFeed.java` queda byte a byte como estaba).

## 1. Diagnóstico

El test rojo en CI era `s13_abortsAnEndlessBodyLongBeforeTwoMebibytes`. La primera
aserción (el error es `FEED_TOO_LARGE`) pasaba; la que caía era la segunda:

```java
assertTrue(written.get() < 2L * HttpCalendarFeed.LIMIT, ...);
```

`written` cuenta lo que alcanza a escribir **el servidor**, no lo que lee el cliente.
Es una carrera entre el bucle de escritura del servidor y el momento en que el cierre
del cliente le llega como `IOException`, mediada por los búferes del socket y por el
planificador. No mide el producto.

**Demostración.** Con una sonda temporal —el mismo oráculo con el servidor escribiendo
en trozos de distinto tamaño— y con la producción **correcta** en todos los casos:

| trozo del servidor | bytes escritos antes del corte | margen hasta 2 MiB (2.097.152) |
|---|---|---|
| 8 KiB (el del test) | 1.482.752 | 8 % |
| 64 KiB | 1.769.472 | 15 % |
| 256 KiB | 1.310.720 | 37 % |
| 512 KiB | 1.572.864 | 25 % |
| 1024 KiB | 1.048.576 | 50 % |

La cifra se mueve entre 1,0 MB y 1,77 MB solo cambiando el tamaño del trozo, con el
producto intacto. Con la forma del test (8 KiB) el margen local es del 8 %: cualquier
runner con búferes de socket mayores o con el cliente algo más lento lo cruza. Subir la
constante habría escondido el problema y habría dejado el test sin morder (un cuerpo
interminable acaba fallando por otra vía aunque no exista corte por tamaño).

## 2. El oráculo nuevo

`s13_abortsAnEndlessBodyAfterReadingJustPastTheLimit`.

En vez de medir al servidor, se **acota por construcción lo que el cliente puede leer**:

- El servidor anuncia `Content-Length: 4 MiB` (`ENDLESS_LENGTH`).
- Solo emite `LIMIT + 64 KiB` (`EMITTED_BEFORE_GOING_SILENT`) y **enmudece**.
- Espera a que la prueba le confirme que el cliente ya abortó y entonces cuelga.

Así, la única forma de llegar a `FEED_TOO_LARGE` es dictar el veredicto habiendo
consumido a lo sumo ese prefijo. Quien agote el cuerpo se queda esperando los MiB que
faltan y termina en `FEED_UNREACHABLE`. La aserción ya no depende de búferes ni de
carreras: el servidor **no puede** entregar más, pase lo que pase en el entorno.

### El colgado final no es adorno

`HttpRequest.timeout` **no cubre la lectura del cuerpo** con
`BodyHandlers.ofInputStream`: `send()` retorna al llegar las cabeceras y el temporizador
se cancela. Verificado con volcado de hilos sobre la ejecución con el defecto A: el
cliente quedaba parado *para siempre* en

```
HttpResponseInputStream.read -> HttpCalendarFeed.read(HttpCalendarFeed.java:79)
```

y la compilación se comía >5 minutos sin fallar. Por eso el servidor, si el cliente no
aborta en `SILENCE_BEFORE_HANGING_UP_SECONDS`, corta la conexión: el defecto se
convierte en rojo en ~5 s en vez de en un cuelgue.

El cierre tiene un detalle fino. En el JDK, `ExchangeImpl.close()` solo cierra el socket
si `uos.close()` lanza; y `FixedLengthOutputStream.close()` solo lanza
`"insufficient bytes written to stream"` la **primera** vez. Si el handler cierra el
flujo antes (p. ej. con try-with-resources), la segunda llamada retorna en silencio y la
conexión queda viva por keep-alive — que es justo lo que hacía colgar al cliente. Por eso
el handler **no** cierra el flujo y llama directamente a `exchange.close()`.

## 3. Prueba de que el oráculo sigue mordiendo

Dos defectos inyectados a mano en `HttpCalendarFeed.read`, revertidos después:

| defecto | oráculo nuevo | oráculo viejo |
|---|---|---|
| **A** — sin corte por tamaño (`if (false)`) | **ROJO**: `expected: <FEED_TOO_LARGE> but was: <FEED_UNREACHABLE>` | cuelga para siempre (no falla) |
| **B** — corte 128 KiB tarde (`> LIMIT + 128*1024`) | **ROJO**: `expected: <FEED_TOO_LARGE> but was: <FEED_UNREACHABLE>` | **VERDE** (no lo detecta) |

El caso B es el importante: el oráculo nuevo no solo es determinista, es **estrictamente
más estrecho**. Fija el punto de corte a `LIMIT + 64 KiB` de consumo, mientras que el
viejo toleraba pasarse 128 KiB sin enterarse.

El límite exacto (`LIMIT` sí, `LIMIT + 1` no) lo siguen sujetando
`s13_readsABodyOfExactlyOneMebibyte` y `s12_aBodyOfOneMebibytePlusOneByteIsTooLarge`;
este test solo se ocupa de la propiedad "deja de leer".

## 4. Verde

`backend/gradlew.bat test --no-daemon -p backend --tests "...HttpCalendarFeedTest"`:
verde, y tres reejecuciones con `--rerun-tasks` también verdes (44 s, 42 s, 44 s).
Producción restaurada y sin residuos de sondas ni de defectos. `spotlessApply` no cambia
nada. No se ejecutó la suite completa (había otra corriendo), ni pitest, ni E2E.

## 5. Hallazgo para el lead (fuera de alcance)

El volcado de hilos deja a la vista un comportamiento real del producto, no del test:
**`HttpCalendarFeed` no tiene tope de tiempo para leer el cuerpo**. Un proveedor que
mande cabeceras y luego enmudezca a mitad del cuerpo bloquea `fetch` indefinidamente;
el corte por tamaño solo salva el caso en que el proveedor *sí* sigue emitiendo. No lo
he tocado porque se sale del `.feature`; si se quiere cubrir, hace falta contrato nuevo
(un deadline de lectura sobre el cuerpo, no solo `HttpRequest.timeout`).
