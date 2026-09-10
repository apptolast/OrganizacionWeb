# Webhooks de OrganizationWeb — guía para quien recibe

Esta página es para quien va a **recibir** los webhooks: explica cómo verificar
que una entrega es auténtica y qué hacer con ella. Si buscas cómo darlos de alta,
eso está en la propia aplicación, en «Webhooks».

---

## Lo que tienes que hacer, en orden

Este orden **no es una recomendación de estilo**. Cada paso existe porque sin él
hay un ataque concreto, y se explican todos más abajo.

1. **Verifica la firma antes de leer el cuerpo.** Antes de parsear el JSON,
   antes de mirar cabeceras, antes de nada.
2. **Deduplica por el `eventId` que va DENTRO del cuerpo firmado**, nunca por
   la cabecera `X-OrganizationWeb-Event-Id`.
3. **Rechaza los instantes con más de 300 segundos de diferencia** respecto a tu
   reloj.
4. **Compara la firma en tiempo constante**, no con `==`.

---

## 1. Verifica la firma primero

Cada petición llega con:

```
POST tu-url
Content-Type: application/json; charset=utf-8
User-Agent: OrganizationWeb-Webhooks/1
X-OrganizationWeb-Event-Id: <uuid>
X-OrganizationWeb-Signature: t=<segundos unix>,v1=<hex minúsculo>
```

La firma es `HMAC-SHA256(secreto, t + "." + cuerpo)`, con el **cuerpo en bytes
exactos, tal como llega**. No lo reserialices para firmarlo: si tu framework
parsea el JSON y lo vuelve a generar, el orden de las claves o los espacios
pueden cambiar y la firma dejará de cuadrar aunque el contenido sea idéntico.
Guarda los bytes crudos.

### La clave del HMAC son los bytes de la cadena entera

Esto se equivoca a menudo, así que va aparte: la clave son los **bytes UTF-8 del
literal completo**, `whsec_` incluido — **no** los 32 bytes que resultan de
decodificar la parte de base64. Si decodificas, la firma no cuadrará nunca.

El secreto es el `whsec_…` que la aplicación te mostró **una sola vez** al crear
el endpoint, y no se puede volver a consultar. Si lo pierdes, **elimina el
endpoint y crea otro**: no hay rotación de secreto, y el contrato la deja
expresamente fuera de alcance.

### Un ejemplo que puedes usar para probar tu verificador

Con el secreto `whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8`, este cuerpo
de exactamente 209 bytes UTF-8:

<!-- prettier-ignore -->
```
{"eventId":"11111111-1111-4111-8111-111111111111","aggregateId":"22222222-2222-4222-8222-222222222222","ownerId":"owner-a","occurredAt":"2026-09-08T10:00:00.000000Z","schemaVersion":1,"type":"webhook.ping.v1"}
```

**Cópialo tal cual, en una sola línea.** La firma es sobre **bytes**, no sobre el
JSON como estructura: si lo reindentas, pasa de 209 bytes a 234 y las firmas de
abajo dejan de cuadrar aunque el contenido sea el mismo. Es el mismo motivo por
el que hay que firmar los bytes crudos de lo que recibes.

y `t=1788861600`, la cabecera es exactamente:

```
X-OrganizationWeb-Signature: t=1788861600,v1=47db42f51507bea71512fc26bef335a304b9382b45b590a774cfc977d6cd708f
```

Si reintentamos, el cuerpo es **idéntico byte a byte** y sólo cambia el instante:
con `t=1788861660` la firma pasa a
`fc161fb2f63428f0680cae6871216284bb420af9917ee794c8159d0400abe460`.

## 2. Deduplica por el `eventId` del cuerpo, no por la cabecera

Esto es lo más importante de esta página, y la razón por la que existe.

La cabecera `X-OrganizationWeb-Event-Id` **viaja fuera de la firma**. Está ahí
por comodidad, para que puedas registrarla sin abrir el cuerpo, y **no sirve para
deduplicar**: quien capture una entrega puede reenviártela con esa cabecera
cambiada, y el cuerpo y la firma seguirán siendo válidos. Si tu deduplicación
mira la cabecera, verás un evento nuevo donde hay una repetición, y actuarás dos
veces sobre el mismo hecho.

El cuerpo firmado lleva su propio `eventId`, y **ése sí** está cubierto por la
firma. Deduplica por él.

El cuerpo es el registro de la outbox **tal cual**, así que **los campos dependen
del tipo**. Los seis comunes están siempre; algunos tipos añaden los suyos. Por
ejemplo, `TaskCreated.v1` lleva **ocho** —los seis comunes más `taskId` y `title`—
y `SubtaskCreated.v1` lleva **nueve**, porque añade además `parentTaskId`:

```json
{
  "eventId": "11111111-1111-4111-8111-111111111111",
  "aggregateId": "22222222-2222-4222-8222-222222222222",
  "ownerId": "owner-a",
  "occurredAt": "2026-09-08T10:00:00.000000Z",
  "schemaVersion": 1,
  "type": "TaskCreated.v1",
  "taskId": "33333333-3333-4333-8333-333333333333",
  "title": "Preparar la propuesta"
}
```

**No valides con una lista cerrada de seis campos.** Si lo haces, rechazarás
todas las entregas de los tipos que llevan campos propios. Lee los que necesites
e ignora el resto: es lo que permite que se añadan tipos sin romperte.

## 3. Rechaza los instantes viejos

Comprueba que el `t` de la cabecera de firma no se aleja más de **300 segundos**
de tu reloj. Sin esa ventana, una entrega capturada hoy sigue siendo válida
dentro de un año: la firma no caduca sola.

Los reintentos vuelven a firmar con un `t` nuevo, así que un reintento legítimo
nunca cae fuera de la ventana por antiguo.

## 4. Compara en tiempo constante

Usa la comparación en tiempo constante de tu plataforma
—`hmac.compare_digest` en Python, `crypto.timingSafeEqual` en Node,
`MessageDigest.isEqual` en Java—. Una comparación normal se detiene en el primer
byte distinto, y ese tiempo de más deja adivinar la firma byte a byte.

---

## Qué cuenta como entrega correcta

**Cualquier 2xx dentro del plazo.** No leemos el cuerpo de tu respuesta.

Si no respondes 2xx, reintentamos a los **1, 5, 30, 120 y 1440 minutos**. Al sexto
intento fallido la entrega queda **agotada** y el endpoint se **deshabilita**
solo, con motivo `DELIVERY_EXHAUSTED`. Lo verás en la aplicación y podrás
volver a habilitarlo.

Conviene que respondas **rápido y luego proceses**: encola el evento y devuelve
2xx. Si tardas más que nuestro plazo, contará como fallo aunque acabes
procesándolo bien, y recibirás un duplicado — que es justo lo que el punto 2 te
permite descartar.

## Los doce tipos a los que puedes suscribirte

| Tipo                         | Cómo aparece en la aplicación |
| ---------------------------- | ----------------------------- |
| `ProjectCreated.v1`          | Crear proyecto                |
| `ProjectUpdated.v1`          | Editar proyecto               |
| `ProjectStatusChanged.v1`    | Cambiar estado de proyecto    |
| `TaskCreated.v1`             | Crear tarea                   |
| `SubtaskCreated.v1`          | Crear subtarea                |
| `TaskStatusChanged.v1`       | Cambiar estado de tarea       |
| `BlockPlanned.v1`            | Planificar bloque             |
| `BlockChanged.v1`            | Cambiar bloque                |
| `WorkSessionStarted.v1`      | Iniciar sesión de trabajo     |
| `WorkSessionStateChanged.v1` | Cambiar estado de sesión      |
| `WorkSessionExtended.v1`     | Extender sesión               |
| `WorkSessionClosed.v1`       | Cerrar sesión de trabajo      |

Además, **en cualquier momento** puedes lanzar un **ping** de prueba a cualquier
endpoint activo, desde su botón en la aplicación. Llega como `webhook.ping.v1`,
con la misma firma y la misma forma que los demás, y sirve para comprobar tu
verificador sin esperar a que ocurra nada.

---

## Lo que nunca vas a recibir

- **Tu secreto**, en ningún cuerpo ni cabecera. Se guarda cifrado y sólo se
  muestra en el momento de crearlo.
- **Peticiones a direcciones privadas.** Comprobamos el destino antes de enviar y
  rechazamos redes internas, y volvemos a comprobarlo en el momento de conectar,
  no sólo al darlo de alta: así una URL que hoy resuelve a un servidor público y
  mañana a uno interno tampoco pasa.
- **Redirecciones seguidas.** Si tu URL responde 3xx, la entrega falla. Danos
  directamente la definitiva.

---

## De dónde sale esta página

La escribe la enmienda de seguridad **B4** del contrato, ratificada por el
propietario: «la documentación pública pasa a exigir que se verifique la firma
primero, que se deduplique por el identificador de evento que va dentro del
cuerpo firmado, que se rechacen instantes con más de trescientos segundos de
diferencia y que la comparación de la firma sea en tiempo constante».

El motivo, dicho en el propio contrato: la firma cubre el instante y el cuerpo,
pero **el contrato anterior pedía deduplicar por una cabecera que viaja sin
firmar**, de modo que quien capturase una entrega podía reproducirla cambiándola.
Los cuatro pasos del principio de esta página cierran ese agujero.

---

## De dónde se llega a esta página

El formulario de creación de `/webhooks` **enlaza aquí**, con el rótulo «Cómo
verificar la firma». Ese enlace no es decorativo: lo pide el `@s43` de
`features/webhooks.feature`, y una prueba comprueba que existe dentro del
formulario, que se alcanza con Tab y que su nombre dice de qué es.

Lo que esa prueba **no** fija es la ruta. Si esta guía se publica algún día en
otra dirección, basta cambiar el destino del enlace: el contrato seguirá
cumpliéndose, porque lo que promete es que desde el formulario se llegue a la
guía de verificación de firma, no que la guía viva en este fichero.

Aquí hubo, hasta el 10 de septiembre de 2026, un aviso de hueco conocido: el
producto no enlazaba y `project-spec.md:2044` decía que sí. Se cerró añadiendo el
enlace con su fila de contrato y su oráculo. El `@s43` queda pendiente de la
contrafirma del propietario; ver `progress/enlace_docs_webhooks.md`.
