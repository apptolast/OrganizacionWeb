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
