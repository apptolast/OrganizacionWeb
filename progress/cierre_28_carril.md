# Cierre de la feature 28 — carril de bloqueantes

Worktree `cal-bloqueantes`, rama `claude/cal-bloqueantes`, `E2E_WEB_PORT=18108`.
Encargo: las cinco condiciones de `progress/cierre_28.md` asignadas a «carril», más
un defecto que el coordinador añadió a media sesión. 10 de septiembre de 2026.

## Marcador

| # | Condición | Bloq. | Desenlace |
|---|---|---|---|
| 9 | C5 (backend) — no existe acta de mutación de backend | SÍ | **CERRADA** |
| 8 | C5 (frontend) — falta el veredicto escrito de cada superviviente | SÍ | **CERRADA** |
| 3 | `@s35` «POST /sync con `onlyIfStale` true» sin oráculo que pueda fallar | SÍ | **CERRADA** |
| 14 | B5 — la rama de la cabecera `Host` restringida | no | **ABIERTA**, imposible en proceso: medido |
| 16 | Reserva por si `adapter.feed` no llega al 80 % | no | **NO APLICA**: 85,1 % medido |
| — | `ConnectorKeyRing:55`, añadido por el coordinador | — | **CERRADA** |

**Cuatro cerradas de cinco, y las tres bloqueantes entre ellas.** La única abierta no
es bloqueante y no tiene camino desde este carril.

De paso, **21 mutantes vivos pasan a muertos** (8 de backend y 13 de frontend), todos
con el rojo acreditado. **Ni una línea de producción cambia**: los 21 se matan
poniendo oráculo donde no lo había, que es exactamente lo que decía que faltaba.

## Método: cómo se acredita un rojo cuando no se puede relanzar la campaña

Las campañas son caras (PIT ~40 min, Stryker ~27 min) y hay otros tres carriles
trabajando, así que no relanzo ninguna. Para cada mutante que declaro muerto hago el
ciclo al revés y con el mismo rigor:

1. Escribo el oráculo y lo veo **verde** sobre la producción sana.
2. **Aplico el mutante al fuente de producción** —el que dice el informe, con su
   mutador y su línea— y vuelvo a correr.
3. Pego el **texto del fallo**.
4. Restauro la producción y confirmo con `git status` que no queda tocada.

Es más fuerte que volver a lanzar la herramienta, porque señala **la prueba exacta**
que cae y no sólo un contador que sube. Los 21 rojos están pegados en las dos actas.

## Condición 9 — C5 de backend · CERRADA

**Acta: `progress/mutacion_external_calendar_backend.md`.**

Recomputé la cifra del `mutations.xml` en vez de copiarla: 436 `KILLED` + 1
`TIMED_OUT` sobre 467 = **93,58 %**, al decimal con el acta de medida, y
`ApplicationConfiguration` no aparece ni una vez en el XML (el ámbito corregido es el
que se midió). Sobre eso, veredicto de los **30 sin matar**:

- **8 muertos** con rojo acreditado: `IcsFeed:121`, `IcsFeed$Property:148` y `:150`,
  `SyncSummary:17`, `SaveExternalCalendar:56`, `ConnectorKeyRing:55`,
  `ConnectorsGate:54`, `SyncExternalCalendar:64`.
- **13 equivalentes o inalcanzables**, cada uno con su razón medida.
- **3 de código muerto** (el `unmap` de `PublicAddressPolicy`), que se borra, no se
  prueba.
- **6 abiertos**, con lo que falta escrito.

Tres cosas que conviene que lea el juez:

1. **`IcsFeed:121` era un defecto de verdad**, no un hueco cosmético: un `SUMMARY`
   terminado en barra invertida leía más allá del final del texto y lanzaba
   `StringIndexOutOfBoundsException`, que no la recoge ni el `catch` de `IcsFeed:53`
   ni el de `SyncExternalCalendar:94`. Salía un 500, contra `feature:15`.
2. **La condición B1 pide algo inalcanzable.** Mandaba matar `:195`, `:196`, `:210` y
   `:223` de `HttpCalendarFeed`. Tres murieron con el oráculo del proveedor que
   enmudece. **`:210` no se puede matar**: es equivalente a través de `fetch()`,
   porque el `catch` de `:158` mapea la `IOException` al mismo `FEED_UNREACHABLE`. No
   falta prueba; la exigencia está mal redactada.
3. **`PublicAddressPolicy$Cidr:70` es un artefacto de la herramienta, no deuda.** Los
   rangos se construyen con `List.of(Cidr.of(...))`, y `List.of` prohíbe nulos: con
   ese mutante `AddressPolicyTest` caería entero. Sobrevive porque el valor se consume
   en el inicializador estático, que corre una vez por JVM y PIT no reejecuta.

Previsión sobre los mismos 467: **445/467 = 95,29 %**. Es aritmética, no medida.

## Condición 8 — C5 de frontend · CERRADA

**Acta: `progress/mutacion_external_calendar_frontend.md`** (sustituye a la previsión
pre-campaña que el panel declaró irreproducible; la vieja sigue en git).

Veredicto de los **65 sin matar**: **13 muertos**, 21 equivalentes con su razón, 29
abiertos agrupados en **cuatro racimos** (que es lo que los hace presupuestables) y 2
errores de ejecución explicados.

**Este trabajo no depende de las condiciones 4, 5 y 6** (estrechar `App.tsx` y
remedir), y la razón es aritmética: `App.tsx` puntúa **100,00 %**, con **cero** vivos y
cero sin cobertura. Estrechar su rango sólo puede quitar mutantes muertos, así que la
lista de supervivientes es idéntica antes y después. Lo que cambia es el porcentaje
publicado, no el trabajo.

Los 13 muertos incluyen toda la lista que el veredicto marcaba como «hueco real, hay
que matar»: `:291`, `:236`, `:237`, `today:105` y `today:27`. Y tres correcciones al
veredicto que hay que registrar:

1. **`today:105` no era el mensaje**, era el **separador** `{" "}` entre el aviso y el
   enlace. Sin él se lee «…calendario externo.Revisar el calendario externo».
2. **De `:114`/`:118`/`:122` sólo `:118` es `setEvents([])`**; los otros dos son
   arrays de **dependencias** de `useCallback`, que sólo cambian la identidad del
   callback. Y `:118` queda abierto con su razón: sólo es observable en el render
   intermedio entre `setSubscription` y la resolución de `loadEvents`, porque todo
   camino que repinta la ficha recarga la lista.
3. **Los dos `RuntimeError` de `:76` no son deuda**: con el mutante el render revienta
   y `@s36 sin suscripción la sección no se muestra` sí lo detecta. Lo que falta es
   que la herramienta sepa clasificarlo, no oráculo.

Dos hallazgos propios que valen la pena:

- **`api:102` y `api:140` sobrevivían teniendo ya filas adversariales.** `urlTail: 7`
  no basta porque, apagada la guarda de tipo, la de longitud sigue rechazando
  (`(7).length` no es 4): hace falta algo que no sea texto y **mida cuatro**. Y
  `configured: "sí"` no basta porque es **verdadero** y acaba en `subscriptionOf(null)`,
  que rechaza igual: hace falta un valor **falso** que no sea booleano, como `0`.
- **`:148` y `:149`: el primer render no lo fijaba nadie.** Con `:149` a `true`, una
  carga sin suscripción intenta leer la etiqueta de un `null`, revienta, cae en el
  `catch`… **y el formulario seguía saliendo**, así que la prueba pasaba igual.

Previsión: **676/726 = 93,11 %**; con el ámbito ya estrechado, **612/662 = 92,45 %**.

## Condición 3 — `@s35` con `onlyIfStale` · CERRADA

El doble de `fetch` de `today-external-calendar.test.tsx` apilaba método y URL pero
**nunca el cuerpo**, y la regla de frescura vive justo ahí. Ahora lo guarda, y la
prueba de orden afirma `{ onlyIfStale: true }`.

Rojo acreditado exactamente como pedía el veredicto, cambiando la producción a
`syncExternalCalendar(false, signal)`:

```
AssertionError: expected { onlyIfStale: false } to deeply equal { onlyIfStale: true }
-   "onlyIfStale": true,
+   "onlyIfStale": false,
```

Sin esto, cada carga de Hoy disparaba una descarga forzada del feed ajeno —lo
contrario de la regla— y la suite seguía verde.

## Condición 14 — B5, la cabecera `Host` restringida · ABIERTA, y por qué

**Confirmo la medición del carril anterior y no la rehago.** Confirmado en el árbol:

- `backend/build.gradle.kts:37` (`systemProperty` de `test`) y `:769` (`jvmArgs` de
  `pitest`) fijan `-Djdk.httpclient.allowRestrictedHeaders=host` en **los dos** JVM.
- `grep` de `unrestrictedHostTest` y de `registering(Test::class)`: **cero**.
- `HttpCalendarFeed:148` es el `return FeedFetch.failed(FEED_UNREACHABLE)` del
  `catch (IllegalArgumentException)`, y sale `NO_COVERAGE`.

Y **lo medí yo**, en vez de heredarlo, con dos programas de un solo fichero fuera del
repositorio. Arrancando el JVM **con** la propiedad:

```
1) con -Djdk...=host al arrancar -> ACEPTA Host
2) tras BORRARLA en caliente      -> ACEPTA Host
3) tras ponerla vacia en caliente -> ACEPTA Host
```

Y arrancando **sin** ella, la dirección simétrica:

```
1) sin la propiedad          -> RECHAZA Host (restricted header name: "Host")
2) tras ponerla en caliente  -> RECHAZA Host
```

O sea: **la lista de cabeceras restringidas se congela en el primer uso del cliente
HTTP y no se puede cambiar en caliente en ninguna de las dos direcciones**. La
medición del carril anterior es correcta. Ninguna prueba en proceso puede alcanzar
`:148` mientras los dos JVM de prueba arranquen con la propiedad puesta.

**Lo que falta, concretamente:** la tarea forkeada `unrestrictedHostTest` en
`backend/build.gradle.kts` **sin** `-Djdk.httpclient.allowRestrictedHeaders`, más su
clase de prueba, y ver morir el mutante de `:148`. El *snippet* ya está escrito en
`progress/bloqueantes_external_calendar.md:454-472`. **La lleva el coordinador**, no
este carril. No es bloqueante: la mitad TLS de C1(a) está cumplida
(`HttpCalendarFeedTest:577`, `:600` y `:615`) y el otro disparador del mismo `catch`
ya tiene oráculo en `:501`.

## Condición 16 — reserva de `adapter.feed` · NO APLICA

Sólo se activaba si `adapter.feed` bajaba del 80 %. La campaña la midió en
**85,1 % (40/47)**, así que la reserva no se dispara y los dos mutantes de
`HttpCalendarFeed:159` quedan documentados como abiertos no exigidos, con su prueba
descrita (interrumpir el hilo durante un `fetch()` y afirmar que la marca de
interrupción sobrevive). Lo confirmo además desde el XML: ninguna capa baja del 80 %.

## Añadido del coordinador — `ConnectorKeyRing:55` · CERRADA

Llegó a media sesión, con el aviso de que las features 27 y 29 se retiran y
`ConnectorKeyRing` pasa a ser código de la 28 porque cifra en reposo la URL del feed.
**Ya lo había cerrado** en el primer commit de esta sesión, y por el mismo diagnóstico:
el único oráculo usaba `contains("APP_CONNECTOR_KEY")`, que es **subcadena** de
`"APP_CONNECTOR_KEY_PREVIOUS"`, así que pasaba con las dos ramas del ternario.

Atendiendo a la petición exacta —«afirmando el nombre completo y comprobando que la
otra variable **no** aparece»— he **reforzado** la aserción: además del paréntesis
(`(APP_CONNECTOR_KEY)`, que ya no casa con la hermana) ahora se afirma explícitamente
que la otra variable **no** está en el mensaje, por los dos lados. Rojo re-acreditado
con el ternario negado a mano:

```
ExternalCalendarWiringTest > s9_aMalformedKeyStopsTheStartupWithoutRevealingItsValue() FAILED
    org.opentest4j.AssertionFailedError at ExternalCalendarWiringTest.java:43
ExternalCalendarWiringTest > s9_aMalformedPreviousKeyNamesThePreviousVariableAndNotTheCurrentOne() FAILED
    org.opentest4j.AssertionFailedError at ExternalCalendarWiringTest.java:63
```

Es la **misma trampa** que encontré por mi cuenta en frontend con `today:27`
(`/…de 12:00/` casa con «12:00 p. m.»): una aserción de subcadena que no puede
distinguir las dos ramas. Vale la pena que el juez la busque como patrón.

## Lo que queda para otros, sin adornos

- **Condiciones 1, 2, 4, 5, 6, 7, 10, 11, 12, 13, 15**: no son de carril. Las bloqueantes
  de ahí (la contrafirma del propietario, el estrechamiento del ámbito y las dos
  campañas de remedida) siguen en pie y **la feature no se puede cerrar sin ellas**.
- **B5 (condición 14)**: la tarea forkeada, del coordinador.
- **El `unmap` muerto de `PublicAddressPolicy`**: borrar el método y corregir la frase
  del javadoc y la de `project-spec.md`. No lo hago aquí porque no es de mis cinco
  condiciones y mezclaría dos cosas en el mismo commit.
- **Los 35 mutantes que dejo abiertos** (6 de backend, 29 de frontend) están en las dos
  actas con lo que falta para cada uno. El más rentable con diferencia es el racimo 1
  de frontend: **una sola prueba parametrizada mata nueve**.

## Verificación antes de entregar

- Backend, por clase y nunca la suite entera: las seis clases tocadas, **verde**.
  Ninguna levanta PostgreSQL.
- Frontend: **91 ficheros, 3355 pruebas, verde**. Después, las tres tocadas otra vez
  tras `prettier --write`: verde.
- `spotless` aplicado al backend.
- `git status` sobre `src/main` (los dos lados): **limpio**. No queda ni un mutante
  puesto.
- No he lanzado ninguna campaña, así que no he estorbado a los otros tres carriles.
- Sin credenciales en pruebas, registros ni commits: las claves de los oráculos nuevos
  son `Base64` de 32 bytes a cero y la cadena literal `no-es-base64-de-32-bytes`, y se
  afirma expresamente que **no** aparece en el mensaje de error.
