# Anclaje a la dirección validada y prueba del TLS — carril 25 (webhooks)

Bitácora del encargo del 10 de septiembre de 2026: resolver la contradicción
normativa entre el carril 25 (que **revocó** el anclaje) y el 28 (que lo
**implementó**), decidida por el propietario a favor de **anclar en las dos y
probar el TLS**, que era la intención de la enmienda de seguridad B3 original.

Rama `claude/webhooks`, worktree `C:/Users/vhurt/ow-worktrees/webhooks`,
puesta al día con `main` (`6997f5d`) antes de empezar. `E2E_WEB_PORT=18090`
(no se ha necesitado: todo el encargo es de backend).

## Estado de partida, medido

`JdkWebhookSenderTest` + `HttpCalendarFeedTest` en verde antes de tocar nada
(`BUILD SUCCESSFUL in 21s`).

| Cuestión | Webhooks (25) | Calendario (28) |
|---|---|---|
| Ancla a la dirección validada | **no** (javadoc `0a68774` lo dice) | sí (`HttpCalendarFeed:119-131`) |
| Conserva `Host` y SNI | n/a | sí (`SNIHostName`, cabecera `Host`) |
| Oráculo de TLS: no confiable se rechaza | sí (`s25_aReceiverWithAnUntrustedCertificateIsATlsFailure`) | **no** (el arnés habla HTTP en claro) |
| Oráculo de TLS: válido para el nombre se acepta por IP | **no** | **no** |

Es decir: el punto que podía romper —conservar la validación por nombre al
conectar por dirección— no estaba probado **en ninguna de las dos**.

## Ciclos

### Ciclo 1 — @s25/B3: la petición viaja a la dirección validada, no al nombre otra vez

**Pruebas nuevas** (`JdkWebhookSenderTest`):

- `s25_b3_theRequestTravelsToTheValidatedAddressInsteadOfResolvingTheNameAgain`
- `s25_b3_theOriginalNameStillTravelsInTheHostHeader`

**Cómo se construye el oráculo sin montar una zona DNS de verdad.** El emisor ya
recibía el resolutor por constructor, así que la prueba fabrica una zona con un
solo nombre, `receptor.webhooks.invalid` (el TLD `.invalid` está reservado por el
RFC 2606, luego **ningún** DNS real puede contestarlo), que responde `127.0.0.1`.
No hace falta simular un segundo respuesta distinta: basta con que el nombre no
tenga respuesta ninguna fuera de la zona inventada. Si el cliente vuelve a
resolver por su cuenta, el envío no llega.

Comprobado que el nombre efectivamente no resuelve en esta máquina:

```
receptor.webhooks.invalid -> java.net.UnknownHostException: Host desconocido
no-existe.invalid         -> java.net.UnknownHostException: Host desconocido
```

**ROJO 1 (acreditado, sin tocar nada: es el estado que denunciaba el javadoc).**

```
s25_b3_theRequestTravelsToTheValidatedAddressInsteadOfResolvingTheNameAgain FAILED
  AssertionFailedError: the send reached the validated address, got errorClass CONNECTION
    ==> expected: <true> but was: <false>
s25_b3_theOriginalNameStillTravelsInTheHostHeader FAILED
  NullPointerException: ...HttpExchange.getRequestHeaders() ... AtomicReference.get() is null
```

O sea: la guardia validaba `127.0.0.1` y acto seguido el cliente salía a buscar
el nombre por su cuenta y no llegaba a ninguna parte. El receptor no recibió
nada (de ahí el `null` del segundo).

**Hallazgo lateral, medido, fuera de ámbito (REGLAS §9).** El rojo dio
`CONNECTION`, no `DNS`, aunque la causa raíz era un nombre que no resuelve: el
cliente del JDK envuelve el `UnknownHostException` en un `ConnectException`, y el
`catch (ConnectException)` de `send` va **antes** que `classify()`, que sí lo
habría clasificado `DNS`. Con el anclaje esto deja de importar, porque el cliente
ya no resuelve nunca: todo fallo de resolución nace ahora en la guardia, que
lanza `UnknownHostException` desnudo y se clasifica `DNS` correctamente. El
anclaje, de paso, hace *más* fiel la fila `DNS` del contrato.

**VERDE.** `JdkWebhookSender` pasa a hacer lo mismo que `HttpCalendarFeed`:

- `guardDestination(url)` (void) se convierte en `validatedAddress(host)`, que
  **devuelve** la primera dirección validada. La validación de todas las
  direcciones no cambia.
- La petición se construye contra la dirección literal y lleva `Host` con el
  nombre y su puerto.
- El cliente se construye por envío, con `HTTP_1_1`, `Redirect.NEVER`, el plazo
  de conexión y los parámetros de TLS del destino, y se cierra al terminar.
- Nace `adapter/net/AnchoredConnection`, con las cuatro piezas del anclaje
  (`allow`, `literal`, `authority`, `sniFor`) para que las dos features compartan
  **una sola** copia de la decisión. Tenerla escrita dos veces fue justamente lo
  que dejó que los dos carriles decidieran lo contrario sin verse.

**ROJO 2 (acreditado a mano, para el oráculo del `Host`).** Sustituida la línea
`.header("Host", AnchoredConnection.authority(target, host))` por un comentario:

```
s25_b3_theOriginalNameStillTravelsInTheHostHeader FAILED
  AssertionFailedError: expected: <receptor.webhooks.invalid:59949> but was: <127.0.0.1:59949>
```

Es decir, el oráculo distingue «anclado conservando el nombre» de «anclado
perdiéndolo», que es el modo silencioso de romper esto. Restaurado y verde:
`BUILD SUCCESSFUL`, 16 de 16.

**Dos decisiones de diseño que el anclaje obliga, escritas y no escondidas.**

1. **HTTP/1.1 forzado.** Sobre HTTP/2 la autoridad la fija el pseudo-encabezado
   `:authority`, que sale de la URI —la dirección literal—, y la cabecera `Host`
   se ignora: el receptor vería la IP. La feature 28 ya había tomado esta misma
   decisión. El coste es real (sin h2 en las entregas salientes) y es el precio
   del anclaje.
2. **SNI sólo si el destino es un nombre.** `new SNIHostName("127.0.0.1")` el
   JDK lo **acepta** (medido), aunque el RFC 6066 prohíbe direcciones ahí, y
   `new SNIHostName("[::1]")` lo **rechaza** con `IllegalArgumentException:
   Contains non-LDH ASCII characters` (medido). `AnchoredConnection.sniFor`
   omite la indicación cuando el destino ya venía escrito como dirección, con lo
   que el certificado se verifica contra la dirección, que es lo correcto. De
   paso queda cerrada una grieta que `HttpCalendarFeed` tenía abierta: un destino
   IPv6 literal la habría hecho estallar (allí es inalcanzable porque la
   validación de URL prohíbe direcciones literales, pero el emisor de webhooks sí
   las ve en las pruebas).

### Ciclo 2 — @s25/B3: el TLS, que es el punto que podía romper

**La pregunta que había que contestar midiendo, no razonando.** Anclar a una IP
suele romper la verificación del certificado. El carril 25 revocó el anclaje
precisamente por eso, sin medirlo. **Respuesta: no lo rompe, si se conserva el
nombre.** Abajo, la evidencia.

**Fixture nuevo, compartido por las dos features:**
`backend/src/test/resources/tls/anchored-receiver.p12`. PKCS12 autofirmado,
`keytool` del JDK 25, RSA 2048, validez 36.500 días, contraseña `changeit`,
`CN=destino.anclado.invalid` y —lo que lo convierte en oráculo— **`SAN =
dns:destino.anclado.invalid` y ninguna `iPAddress`**:

```
Owner: CN=destino.anclado.invalid
SubjectAlternativeName [
  DNSName: destino.anclado.invalid
```

Un certificado sin nombre alternativo de tipo dirección **no puede** validarse
contra `127.0.0.1`. Así que si la conexión anclada lo acepta, es que el nombre
sobrevivió al anclaje. Ése es todo el truco.

**Confianza.** Una prueba no puede añadir una autoridad al almacén del JDK, así
que `JdkWebhookSender` gana un constructor de paquete con un `SSLContext`, por el
mismo motivo por el que ya tenía uno con los plazos. Lo importante: el contexto
de prueba **confía en un certificado**, no apaga la verificación —por eso la
tercera prueba de abajo sigue rechazando al desconocido.

**Pruebas nuevas** (`JdkWebhookSenderTest`), las tres por el camino anclado:

| Prueba | Afirma |
|---|---|
| `s25_b3_aCertificateValidForTheNameIsAcceptedAlthoughTheConnectionGoesToTheAddress` | certificado válido para el nombre, conexión a la dirección → **200** |
| `s25_b3_theSameCertificateIsRejectedWhenTheNameAskedForIsAnother` | el mismo certificado, pedido otro nombre → **TLS** |
| `s25_b3_anUntrustedCertificateIsStillRejectedOnTheAnchoredPath` | receptor que nadie avala, por el camino anclado → **TLS** |

La segunda es el control de la primera: sin ella, «lo acepta» podría significar
«no comprueba nada». Con ella, lo que compra la aceptación es la coincidencia del
nombre.

**Resultado: `BUILD SUCCESSFUL`, 19 de 19.** El anclaje conserva el TLS. No hace
falta volver a la otra opción.

**ROJO acreditado, mutante A: quitar la indicación de servidor.** Comentada la
línea `if (!isAddressLiteral(host)) parameters.setServerNames(...)`:

```
s25_b3_aCertificateValidForTheNameIsAcceptedAlthoughTheConnectionGoesToTheAddress FAILED
  the handshake verified the certificate by name, got errorClass TLS
    ==> expected: <true> but was: <false>
```

Es exactamente el fallo que el carril 25 temía y por el que revocó la enmienda:
anclar **sin** conservar el nombre sí rompe el TLS. Queda demostrado que ocurre,
y que la prueba lo caza.

**MUTANTE B, que SOBREVIVIÓ, y lo que se ha hecho al respecto.** Quitada la línea
`parameters.setEndpointIdentificationAlgorithm("HTTPS")`, `JdkWebhookSenderTest`
siguió **entera verde**. Causa medida: el cliente HTTP del JDK impone por su
cuenta la verificación del nombre cuando el esquema es https, de modo que esa
línea es redundante *para este cliente*. No es inútil —la pieza compartida puede
usarse con un socket o un motor de TLS, donde por omisión no se verifica nada—,
pero al nivel de la descarga es incontrastable.

Un mutante que ninguna prueba puede matar es exactamente lo que este encargo vino
a corregir, así que se prueba al nivel en el que sí se puede afirmar: nace
`AnchoredConnectionTest` (9 pruebas puras, sin contenedor ni servidor) sobre las
cuatro piezas de `AnchoredConnection`. Con él:

```
MUTANTE B (sin setEndpointIdentificationAlgorithm):
  theParametersAskForCertificateVerificationByName FAILED
    expected: <HTTPS> but was: <null>
MUTANTE C (sin la guarda isAddressLiteral, ofreciendo la IP como SNI):
  anAddressIsNeverOfferedAsAServerName FAILED
    expected: <null> but was: <[type=host_name (0), value=127.0.0.1]>
```

**Previsión para la campaña de mutación** (la corre el orquestador, REPARTO §4):
se esperan muertos los mutantes de `setServerNames`, `setEndpointIdentification`,
la guarda `isAddressLiteral`, el paréntesis de IPv6 de `literal`, la raíz por
defecto de la ruta y el puerto de `authority`. El que puede sobrevivir es
`allow()` mutado a no hacer nada: `theRestrictedHostHeaderIsEnabled` lo mata sólo
si la propiedad no venía ya puesta por otra prueba del mismo JVM; la segunda
prueba, `enablingItDoesNotDropWhatTheDeploymentAlreadyDeclared`, sí lo mata en
cualquier orden porque fija la propiedad a `connection` antes de llamar.

### Ciclo 3 — @s12/B3: el calendario externo, anclado igual y probado igual

El punto 3 del encargo. `HttpCalendarFeed` **sí** anclaba, pero su TLS no tenía
oráculo, y `docs/external-calendar.md:41-45` lo decía con todas las letras: «la
parte de SNI/certificado no tiene prueba propia: el arnés de esta clase habla
HTTP en claro contra `127.0.0.1`». O sea, la mitad que había decidido bien lo
había hecho sin red debajo.

**ROJO (no compila, que cuenta como rojo).** Las tres pruebas nuevas piden un
constructor con la confianza inyectada, que no existía:

```
HttpCalendarFeedTest.java:442: error: constructor HttpCalendarFeed in class
  HttpCalendarFeed cannot be applied to given types;
```

**VERDE.** Dos cosas:

1. `HttpCalendarFeed` gana el mismo constructor de paquete con `SSLContext` que
   el emisor, por el mismo motivo y con el mismo comentario.
2. Se queda con **una sola** copia del anclaje: su bloque estático, su `literal`,
   su `authority` y sus `SSLParameters` se sustituyen por
   `AnchoredConnection`. Adelgaza 30 líneas y, de paso, hereda la guarda de
   direcciones literales que le faltaba (un destino IPv6 literal la habría hecho
   estallar con `IllegalArgumentException`).

**Pruebas nuevas** (`HttpCalendarFeedTest`), hermanas exactas de las del emisor y
sobre el mismo fixture:

| Prueba | Afirma |
|---|---|
| `s12_b3_unCertificadoValidoParaElNombreSeAceptaAunqueSeConecteALaDireccion` | descarga correcta y `Host` con el nombre |
| `s12_b3_elMismoCertificadoSeRechazaSiElNombrePedidoEsOtro` | `FEED_UNREACHABLE`, sin petición |
| `s12_b3_unCertificadoQueNadieAvalaSeRechaza` | `FEED_UNREACHABLE`, sin petición |

La tercera no necesita un segundo fixture: es el mismo servidor visto por un
cliente con la confianza de la plataforma, que no lo avala.

**ROJO acreditado, dos mutantes.**

```
MUTANTE A (AnchoredConnection sin setServerNames):
  s12_b3_unCertificadoValidoParaElNombreSeAceptaAunqueSeConecteALaDireccion FAILED
    el certificado se verificó por nombre pese a conectar por dirección
      ==> expected: <FeedFetch.Downloaded> but was: <FeedFetch.Failed>
MUTANTE D (HttpRequest.newBuilder(target), es decir desanclar y volver al nombre):
  s12_b3_unCertificadoValidoParaElNombreSeAcepta... FAILED
  s13_connectsToTheValidatedAddressAndKeepsTheNameInHost FAILED
```

El mutante D confirma que el anclaje del calendario ya tenía su oráculo (la
prueba `s13_...`) y que ahora además tiene el del TLS.

**Contrato (REGLAS §8).** `features/external_calendar.feature` gana una fila en
los ejemplos de @s12: `| presenta un certificado que no es válido para su nombre
| FEED_UNREACHABLE |`. **Por qué:** el conjunto cerrado de códigos de la feature
(línea 15) no tiene una clase propia para TLS, y el adaptador mapea cualquier
fallo de entrada/salida a `FEED_UNREACHABLE`; la fila describe el comportamiento
que ahora está probado, en lugar de dejarlo como un caso que el contrato no
menciona. Se cubre en `HttpCalendarFeedTest`, que es donde ya viven otras filas
de ese mismo outline (`s12_anAbsentContentTypeIsUnsupported`,
`s12_aRefusedConnectionIsUnreachable`).

**Verde de las tres clases tocadas:** `HttpCalendarFeedTest` 37/37,
`AnchoredConnectionTest` 9/9, `JdkWebhookSenderTest` 19/19, y
`ExternalCalendarWiringTest` sigue verde con el constructor público intacto.
