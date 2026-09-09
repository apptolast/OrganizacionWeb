# Política de egreso — requisito de despliegue

Origen: hallazgo **B3** de `progress/security_review_connectors.md:40`, que ya
preveía esta salida: «Si se mantiene el límite, declarar la política de egreso
como requisito de despliegue en `deploy/`». El hallazgo 5 del dictamen de la
feature 25 la exige ejecutada. Este fichero es esa declaración.

## Qué queda abierto en la aplicación

Los conectores salientes (webhooks de la feature 25, calendario externo de la 28
y el conector de GitHub de la 26) resuelven el nombre del destino una vez,
validan **todas** las direcciones devueltas contra `AddressPolicy` y abandonan la
petición antes de abrir la conexión si una sola está bloqueada.

La petición viaja después **por nombre**, no por la dirección literal ya
validada. El cliente HTTP del JDK resuelve otra vez por su cuenta, de modo que la
ventana entre la comprobación y el uso (*DNS rebinding*) **no la cierra la
aplicación**. Se decidió no anclar la conexión a la dirección literal porque
hacerlo en este cliente obliga a `jdk.httpclient.allowRestrictedHeaders=host` y
rompe la verificación del nombre del certificado: cerraría una ventana estrecha
abriendo una peor.

Lo que acota el residuo dentro de la aplicación:

- **https obligatorio** en los destinos y `Redirect.NEVER`. Un servicio interno
  al que apuntase un nombre reenlazado tendría que presentar un certificado
  válido para el nombre del atacante. Que un certificado no confiable se rechaza
  está probado en `JdkWebhookSenderTest.s25_aReceiverWithAnUntrustedCertificateIsATlsFailure`.
- La **caché DNS de la JVM**, que hace que las dos resoluciones coincidan en la
  práctica dentro de su ventana. Es una mitigación real pero incidental: no se
  configura y no se debe contar con ella.

## Qué debe aportar el despliegue

El cierre efectivo es de infraestructura, no de aplicación. Quien despliegue
esta aplicación **debe** restringir el egreso del contenedor de la API a lo
estrictamente necesario:

1. **Denegar por defecto** todo el tráfico saliente del contenedor de la API y
   permitir sólo 443/tcp hacia internet.
2. **Bloquear en la red** los destinos que la política de la aplicación ya
   rechaza, para que un reenlace posterior a la comprobación tampoco tenga
   salida: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `127.0.0.0/8`,
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

El límite pasa de «aceptado y acotado» a «aceptado y sin acotar». No es una
vulnerabilidad explotable por sí sola —hace falta además un servicio interno que
presente un certificado válido para el nombre del atacante—, pero deja de haber
segunda barrera.
