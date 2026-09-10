# Review — feature 25 webhooks (`features/webhooks.feature`)

**Veredicto: CHANGES_REQUESTED** — cuarta pasada, 10 de septiembre de 2026. Encargo acotado:
comprobar las **cinco correcciones de markdown** de la tercera pasada. **Tres están bien y
cerradas. Dos no**: la 1 se entregó a medias y la 4 se entregó **rota** — el vector de firma que
publica la página **no verifica**. Sustituye al veredicto de la tercera pasada de este fichero.

No se reaudita nada más. `git diff --name-only 17b823f9 HEAD` sigue sin devolver **ni un fuente
ni una prueba**: `backend/build.gradle.kts`, `docs/webhooks.md`, `project-spec.md` y cuatro
ficheros de `progress/`. Las puertas siguen acreditadas y ninguna se ve rozada por esto: backend
**92,81 %**, frontend **94,57 %**, `harness init` verde. No ejecuté nada pesado.

---

## Las cinco correcciones, una por una

### 2. Rotación del secreto — CERRADA

`docs/webhooks.md:48-51` ya no manda rotar: «elimina el endpoint y crea otro: **no hay rotación
de secreto**, y el contrato la deja expresamente fuera de alcance». Concuerda con
`project-spec.md:1999` (fuera de alcance), la decisión 8 de `:2054` y con las ocho rutas de
`WebhookController`, ninguna de las cuales rota nada. Bien.

### 3. El ping atado al alta — CERRADA

`:163-166`: «**en cualquier momento** puedes lanzar un ping de prueba a **cualquier endpoint
activo**, desde su botón en la aplicación». Es exactamente `POST /{id}/ping`
(`WebhookController:124-127`), el botón por fila de `frontend/src/webhooks.tsx:461-462` y el
«sólo se emite por acción explícita» de `project-spec.md:1999`. Bien.

### 5. La línea falsa del spec — CERRADA, y NO bloquea

`project-spec.md:2044` lleva la nota fechada, con el hecho, la comprobación (`grep` sobre
`frontend/src/webhooks.tsx`), el motivo de no clavarlo con un oráculo y la mención de la
contrafirma pendiente. `progress/decisiones_pendientes.md:48-62` la registra con la forma de
siempre. Es lo que pedí, en el fondo y en la forma.

**Respondo sin rodeos a tu pregunta directa: esa contrafirma pendiente NO bloquea el cierre de la
25.** Tres razones, y las dejo escritas para que nadie tenga que reconstruirlas:

1. **No toca ninguna puerta.** Las puertas de este repositorio son cobertura de los `@s`,
   disciplina TDD, `harness init` y umbral de mutación. La nota no mueve ni un `@s`, ni una
   prueba, ni una línea de `src/`: el árbol de código es idéntico al que ya aprobé.
2. **Corrige el documento hacia la verdad, no la aleja.** Antes `:2044` afirmaba un enlace que no
   existe; ahora dice lo que el árbol dice. El riesgo de dejarlo pendiente es cero: lo que espera
   contrafirma es la forma de cerrarlo, no un hecho en disputa.
3. **Es una decisión que sobrevive a la feature.** Si el propietario prefiere el enlace de verdad,
   eso será una fila de contrato nueva y su prueba —trabajo futuro con su propio ciclo—, no una
   reapertura de la 25. La 25 no promete ese enlace en ninguno de sus 42 escenarios.

Queda como asunto abierto de gobierno, viajando por su carril en
`progress/decisiones_pendientes.md`. No retiene la feature.

### 1. El cuerpo de ejemplo — entregada a medias

Lo principal está bien y lo doy por bueno: `:97-108` publica un `TaskCreated.v1` con los **ocho**
campos, y los ocho son **exactamente** los que exige `OutboxMessage.validationCode():45-55`
—`eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId, title`—, comprobados
uno a uno. Los valores pasan además el resto de la validación: `taskId` casa el regex de UUID
(`:338-341`), `schemaVersion` es el entero 1 (`:175`), `occurredAt` parsea a `Instant`
(`:176-178`) y `title` no está vacío, no llega a 160 puntos de código ni tiene blancos en los
extremos (`:363-367`). El aviso de `:110-112` —«no valides con una lista cerrada de seis
campos»— es el remedio correcto y está bien argumentado.

Pero la frase que los presenta introduce un dato falso nuevo. Ver B2.

### 4. La clave del HMAC y el ejemplo verificable — la mitad buena, la mitad rota

**La clave: correcta y cerrada.** `:42-46` lo dice aparte y con su propio encabezado: los **bytes
UTF-8 del literal completo, `whsec_` incluido**, no los 32 que esconde el base64. Es literalmente
`WebhookSignature.java:16`, `secret.getBytes(StandardCharsets.UTF_8)`, y lo confirma el javadoc de
la clase, «keyed by the whole whsec_ secret». Cierra la duda abierta 3 de
`progress/gherkin_webhooks.md:66` y la decisión del 8 de septiembre (`:87`), que asignó por escrito
este dato a este fichero.

**El ejemplo verificable: no verifica.** Ver B1. Es lo que más me preocupa de toda la pasada.

---

# Lo que sigue abierto — dos correcciones, ambas de markdown

## B1. BLOQUEANTE — `docs/webhooks.md:53-77`: el vector de firma que se publica no cuadra, y la página promete que sí

`:55-56` anuncia «este cuerpo de **exactamente 209 bytes UTF-8**» y a continuación, en `:58-67`,
imprime el JSON **reformateado, con saltos de línea y sangría de dos espacios**. Ese bloque no
mide 209 bytes: **mide 238**. Lo medí, y calculé el HMAC de las dos formas con la clave que la
propia página declara:

| Cuerpo | Bytes | t=1788861600 | t=1788861660 |
|---|---|---|---|
| La línea compacta de `features/webhooks.feature:235` | **209** | `47db42f5...d6cd708f` | `fc161fb2...00abe460` |
| **El bloque tal como está impreso en `:58-67`** | **238** | `ff3d3fd0e716164a1c8d6e2c1130c34e58763bc91552ad0c1bdd86931f39c38e` | `083f5bcf6fa387f5b4e69142b71e60778c4d9cb4ecfc619073a8c29313261e75` |

Las dos firmas publicadas en `:72` y `:77` son las de la **primera** fila: correctas, bien copiadas
del `@s15` (`features/webhooks.feature:239,241`) y coincidentes con `WebhookSignature.header`. El
cuerpo impreso es el de la **segunda**. El ejemplo se contradice consigo mismo.

Por qué es bloqueante y no cosmético, en tres pasos:

1. **El encabezado dice para qué existe:** «Un ejemplo que puedes usar para **probar tu
   verificador**». Quien lo use copiará el bloque de `:58-67`, obtendrá `ff3d3fd0...` donde la
   página promete `47db42f5...`, y concluirá que su verificador correcto está mal. Un ejemplo que
   falla es peor que no dar ejemplo: manda a depurar código sano.
2. **Contradice la advertencia que está treinta líneas más arriba, en esta misma página.**
   `:36-40` ordena: «el cuerpo en **bytes exactos, tal como llega**. No lo reserialices para
   firmarlo: si tu framework parsea el JSON y lo vuelve a generar, el orden de las claves o los
   espacios pueden cambiar y la firma dejará de cuadrar. **Guarda los bytes crudos**». La página
   comete en su propio ejemplo el error del que avisa: reserializó el vector al maquetarlo. Es la
   peor forma posible de fallar aquí.
3. **No cierra la promesa que decía cerrar.** `project-spec.md:2058` promete «ejemplo de
   verificación de firma». Un ejemplo cuyas firmas no se reproducen desde el cuerpo que enseña no
   es un ejemplo de verificación.

**Remedio, y es de una línea:** imprimir el cuerpo tal como está en el contrato, en **una sola
línea compacta**, copiada literalmente de `features/webhooks.feature:235`, sin reformatear; y, ya
que la página avisa de esto, decir en una frase que el salto de línea final del bloque no forma
parte del cuerpo. Si se quiere conservar la versión legible, que vaya aparte y rotulada como **no
firmable**, nunca bajo la promesa de los 209 bytes.

## B2. BLOQUEANTE — `docs/webhooks.md:95`: `SubtaskCreated.v1` no lleva ocho campos, lleva nueve

La frase dice: «Por ejemplo, `TaskCreated.v1` y `SubtaskCreated.v1` llevan **ocho**».

Cierto para `TaskCreated.v1`. **Falso para `SubtaskCreated.v1`:** `OutboxMessage.java:78-81` toma
el conjunto de ocho y le **añade `parentTaskId`** —nueve—, y `:342-346` lo exige presente, con
formato de UUID y **distinto de `taskId`**. Una fila de subtarea con ocho claves no se entrega
jamás: `expected.equals(payload.keySet())` falla en `:170` y sale `INVALID_EVENT`.

Es el mismo error que la corrección 1 vino a arreglar —anunciar un recuento cerrado que el código
desmiente—, reintroducido una línea por encima del arreglo. El aviso de `:110-112` lo amortigua,
pero no lo vuelve verdadero, y esta página se lee como referencia.

**Remedio:** dejar el ejemplo sólo con `TaskCreated.v1`, o decir «nueve, porque además lleva
`parentTaskId`».

---

## Nota no bloqueante nueva

**El bloque «Hueco conocido» de `:198-208` quedó desfasado y ahora contradice al spec.** Su última
frase —«Cerrarlo bien pide una fila de contrato y su oráculo»— es justo la vía que
`project-spec.md:2044` y `progress/decisiones_pendientes.md:54-58` descartaron por escrito, con mi
ratificación. Ya dije en la pasada anterior que, enmendado el spec, este bloque «sobra o se reduce
a un puntero». Sigue sin bloquear, pero conviene alinearlo en la misma edición que B1 y B2.

Siguen vigentes, sin cambios: la página no da la cifra del plazo —5 s de conexión y 10 s de
intercambio, `JdkWebhookSender.java:48-49`— y las no bloqueantes 1 a 5 de la segunda pasada.

## Checkpoints

- **C1** [x] — sin cambios; desde `17b823f9` no se ha tocado ni un fuente ni una prueba.
- **C2** [x] — sin cambios.
- **C3** [x] — ninguna producción de la 25 sin test que la pida.
- **C4** [x] — misma base que C1.
- **C5** [x] — `git status --porcelain` vacío al abrir esta revisión.
- **C6** [ ] — los 42 `@s` tienen test, B4 está entregada y tres de las cinco correcciones están
  cerradas, pero la guía pública publica un vector de firma que no verifica (B1) y un recuento de
  campos falso para `SubtaskCreated.v1` (B2).
- **C7** [x] — backend 400/431 = **92,81 %**, frontend 592/626 = **94,57 %**. Nada las invalida:
  desde entonces sólo cambió markdown.

## Cobertura de escenarios (@s ↔ test)

- @s1..@s42: **[x]**, sin cambios. Esta entrega es markdown puro.

## Disciplina TDD

- **Rojo-Verde-Refactor:** SÍ, sin cambios.
- **¿Producción sin test que la pida?** NO.

---

## Resumen

Tres de las cinco están cerradas y bien cerradas: la rotación, el ping y la enmienda del spec. La
cuarta trae la mitad que más importaba —la clave del HMAC, dicha aparte y exacta contra
`WebhookSignature.java:16`— y la primera trae los ocho campos correctos de `TaskCreated.v1`,
verificados uno a uno contra `OutboxMessage`, con el aviso adecuado contra las listas cerradas.

Lo que la retiene son dos frases. La del vector es seria: la página publica un cuerpo de 238 bytes
bajo el rótulo de 209 y con las firmas del de 209, de modo que **el único ejemplo comprobable de
todo el documento falla si alguien lo comprueba**, y falla por reserialización, que es justo lo que
la página prohíbe treinta líneas antes. La otra es un recuento de campos que el código desmiente
para `SubtaskCreated.v1`.

Ni una línea de `src/`, ni una prueba, ni una campaña que repetir. **La contrafirma pendiente del
propietario NO retiene nada.** Corregidas B1 y B2, la 25 pasa a `done` sin condiciones.
