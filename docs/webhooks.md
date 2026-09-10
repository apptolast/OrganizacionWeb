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

El secreto es el `whsec_…` que la aplicación te mostró **una sola vez** al crear
el endpoint. No se puede volver a consultar: si lo pierdes, hay que rotarlo.

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

```json
{
  "eventId": "…",
  "aggregateId": "…",
  "ownerId": "…",
  "occurredAt": "2026-09-08T10:00:00.000000Z",
  "schemaVersion": 1,
  "type": "TaskCreated.v1"
}
```

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

Además, al crear un endpoint puedes enviarte un **ping** de prueba: llega como
`webhook.ping.v1`, con la misma firma y la misma forma que los demás.

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

## Hueco conocido, dicho aquí para que no se descubra tarde

`project-spec.md:2044` dice que la ayuda del formulario de creación **enlaza a
este documento**. Hoy no lo hace: `frontend/src/webhooks.tsx` no contiene ningún
enlace. Comprobado con `grep`.

No se ha añadido el enlace al escribir esta página, y el motivo es deliberado: el
contrato ejecutable (`features/webhooks.feature`) **no** pide ese enlace en
ningún escenario, así que añadirlo sería producción nueva sin ninguna prueba que
la exija — exactamente lo que la disciplina de este repositorio prohíbe. Cerrarlo
bien pide una fila de contrato y su oráculo, no un `<a>` colado.
