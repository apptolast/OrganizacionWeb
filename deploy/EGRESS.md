# Política de egreso — requisito de despliegue

Origen: hallazgo **B3** de `progress/security_review_connectors.md:40`. Este
fichero es la declaración de lo que el despliegue debe aportar.

**Cambio del 10 de septiembre de 2026.** Este documento decía antes que la
aplicación **no** cerraba la ventana de reenlace de nombres y que el cierre
efectivo era de infraestructura. Ya no es así: la aplicación la cierra. Lo que
sigue aquí abajo pasa de ser **la** contención a ser **defensa en profundidad**,
que es distinto y sigue siendo obligatorio.

## Qué cierra la aplicación

Los dos conectores cuyo destino lo elige el usuario —webhooks de la feature 25 y
calendario externo de la 28— resuelven el nombre del destino **una vez**, validan
**todas** las direcciones devueltas contra `AddressPolicy` y abandonan la petición
antes de abrir la conexión si una sola está bloqueada.

Y la petición viaja después **contra esa misma dirección ya validada**, no contra
el nombre. El cliente HTTP no vuelve a preguntar al DNS, de modo que la ventana
entre la comprobación y el uso (*DNS rebinding*) **no existe**: no hay una segunda
resolución que pueda contestar otra cosa.

El conector de GitHub **no** ancla, y no le hace falta por el mismo motivo por el
que no valida direcciones: su base no la elige el usuario, es configuración del
servidor validada al arrancar contra una lista fija (enmienda B11). Lo que el
usuario aporta es el nombre del repositorio, no el destino. Si algún día la base
pasara a ser dato de petición, este párrafo deja de valer y habría que anclarlo
igual.

Anclar a una dirección suele romper la verificación del certificado. Aquí no,
porque el nombre viaja con la petición: en la cabecera `Host` y en la indicación
de servidor de TLS, contra la que se verifica el certificado. Las tres piezas
viven juntas en `adapter/net/AnchoredConnection`, compartidas por los conectores,
y están probadas en los dos sentidos:

- Un certificado **válido para el nombre** se acepta aunque la conexión vaya a la
  dirección:
  `JdkWebhookSenderTest.s25_b3_aCertificateValidForTheNameIsAcceptedAlthoughTheConnectionGoesToTheAddress`
  y `HttpCalendarFeedTest.s12_b3_unCertificadoValidoParaElNombreSeAceptaAunqueSeConecteALaDireccion`.
- El **mismo** certificado se rechaza si el nombre pedido es otro, y un
  certificado que nadie avala se sigue rechazando:
  `…theSameCertificateIsRejectedWhenTheNameAskedForIsAnother`,
  `…anUntrustedCertificateIsStillRejectedOnTheAnchoredPath`,
  `s12_b3_elMismoCertificadoSeRechazaSiElNombrePedidoEsOtro` y
  `s12_b3_unCertificadoQueNadieAvalaSeRechaza`.

Lo acompañan **https obligatorio** en los destinos y `Redirect.NEVER`.

**Lo que el anclaje cuesta**, para que quien despliegue lo sepa:

1. La aplicación necesita `jdk.httpclient.allowRestrictedHeaders=host`, y la
   necesita **antes de que nada construya la primera petición HTTP**: el cliente
   del JDK lee esa lista una sola vez, al inicializar su clase de utilidades, y
   lo que se ponga después no tiene efecto. Por eso `OrganizationApplication.main`
   la declara como primera línea, antes de arrancar Spring, **añadiéndose** a lo
   que hubiera declarado el despliegue en vez de sustituirlo. Si el despliegue
   prefiere pasarla con `-D`, perfecto, pero que incluya `host`. Si no se cumple,
   el fallo no es sutil pero sí silencioso: **todas** las entregas de webhooks y
   **todas** las sincronizaciones de calendario fallan como «no alcanzable».
2. Las salidas hablan **HTTP/1.1**. Sobre HTTP/2 la autoridad la fija la URI —la
   dirección literal— y el receptor no vería el nombre.
3. Si un nombre resuelve a **varias** direcciones se usa la primera y no se
   reintenta con las demás. Todas estaban validadas, así que no es un agujero de
   seguridad: es menos tolerancia a fallos que la del cliente por defecto.

## Qué debe aportar el despliegue

La segunda barrera. Quien despliegue esta aplicación **debe** restringir el
egreso del contenedor de la API a lo estrictamente necesario:

1. **Denegar por defecto** todo el tráfico saliente del contenedor de la API y
   permitir sólo 443/tcp hacia internet.
2. **Bloquear en la red** los destinos que la política de la aplicación ya
   rechaza: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `127.0.0.0/8`,
   `169.254.0.0/16` (incluido el 169.254.169.254 de metadatos), `100.64.0.0/10`,
   `0.0.0.0/8`, `192.0.0.0/24`, `192.88.99.0/24`, `198.18.0.0/15`,
   `240.0.0.0/4`, y sus equivalentes IPv6 `::1/128`, `fc00::/7`, `fe80::/10`,
   `64:ff9b::/96`, `2002::/16`.
3. **No colocar servicios internos accesibles sin autenticación** en la misma
   red de salida del contenedor de la API.

En el repositorio de despliegue (`DockerSwarmInfrastrcture`) esto se materializa
con la red de salida del servicio y sus reglas de cortafuegos; aquí sólo se
declara el requisito, porque este repositorio no despliega.

## Si no se cumple

Se pierde la segunda barrera, no la primera. El reenlace de nombres sigue cerrado
en la aplicación. Lo que queda sin acotar es todo lo demás: un fallo en la
política de direcciones, un destino público que resuelva a algo que no debería, o
cualquier salida futura que no pase por estos conectores. Por eso el requisito
sigue siendo obligatorio aunque ya no sea lo único que sujeta el problema.
