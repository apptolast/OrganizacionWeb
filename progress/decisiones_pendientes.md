# Decisiones que esperan al propietario — 10 de septiembre de 2026

Nada de esto bloquea el trabajo: los carriles siguen. Bloquea el **cierre** de la
feature que se nombra en cada punto, porque son cambios de contrato y la
disciplina del repo dice que los firmas tú.

---

## 1. Feature 27 — enmienda del recibo, sin contrafirma

**Qué pasó.** El commit `a1b0d20b` (10 de septiembre, 00:19) cambió dos filas de
`features/github_connector.feature` **sin pasar por ti**. Lo caza el panel de
precierre (motivo M13 de `progress/carriles/bloqueantes_27.md`).

**Qué cambió, exactamente:**

- Línea 146: «con sus **once** campos» → «con sus **doce** campos».
- Líneas 165-166: el recibo pasa de contener `id, projectId, repository, status,
  created, skipped, failed, truncated, errorCode, startedAt, finishedAt` a
  contener `id, **source**, projectId, **projectPath**, status, created, skipped,
  failed, truncated, errorCode, startedAt, finishedAt`. Y la cláusula siguiente
  pasa a exigir `source` es "github" y `projectPath` es "octocat/Hello-World".

**Por qué se hizo.** Al unificar la importación de GitHub y GitLab en la feature
29, el recibo HTTP dejó de llevar `repository` y pasó a llevar `source` +
`projectPath`. El `@s20` de `features/additional_connectors.feature:242` exige
que **los dos recibos tengan las mismas claves**. Las dos filas de la 27 habían
caducado, y mientras tanto la región «Resultado de la importación» **no pintaba
ningún contador** en el producto: `decodeReceipt` rechazaba el recibo entero por
`exact(value, RECEIPT_FIELDS)` y el componente caía al catch. O sea que no es un
contador a cero: la sección no existía.

**Mi lectura.** Es coherencia con un contrato que ya existía, no una ampliación:
sin ella, dos contratos aprobados se contradicen. Es del mismo tipo que las dos
enmiendas que ya ratificaste («alinear el `@s31` de la 29 con el de la 27»).
Pero es un cambio de contrato y no lo firmo yo.

**Lo que hay que decidir:** ratificarla, o revertirla y arreglar el desacuerdo
por el otro lado (que el recibo de GitHub vuelva a llevar `repository`, lo que
rompería el `@s20` de la 29).

---

## 2. Nada más, de momento

Los carriles están destapando cosas. Cuando alguno encuentre otro cambio de
contrato, se apunta aquí y se pregunta todo junto, para no interrumpirte por
goteo.

---

## 2. Feature 29 — el `@s9` nombra una columna que no existe

**Qué pasa.** `@s9` de `features/additional_connectors.feature` dice «token_nonce
de 12 bytes». Esa columna **no existe**:
`V29__additional_connectors.sql:37` declara sólo `token_ciphertext`, con el nonce
embebido dentro del criptograma. Lo caza el panel de precierre de la 29.

**Mi lectura.** El nonce embebido es el diseño correcto y el que usa el resto del
repositorio; lo que caducó es la línea del contrato, que describe una forma de
guardar que nunca se implementó. No es un agujero de seguridad: el nonce está,
sólo que dentro del mismo campo.

**Lo que hay que decidir:** enmendar la línea del `.feature` para que describa el
nonce embebido (y contrafirmarla), o añadir la columna `token_nonce` separada y
migrar, que es trabajo real y sin ganancia de seguridad que yo vea.

---

## 3. Feature 27 y 29 — dónde vive la ratificación

Ya no hace falta preguntarte por esto: las ratificaciones que sí diste están
ahora escritas con su cita exacta en `progress/ratificaciones.md`, para que un
juez que sólo lee el repositorio pueda comprobarlas sin la conversación.

---

## 4. Feature 30 — `@s43` no describe la carrera entre escrituras

**Qué pasa.** Los `Examples` de `@s43` son «navega a /proyectos», «cierra sesión»
y «cambia a otra regla». El camino donde vivía un defecto **real** que se arregló
hoy —«otra escritura la supera», que dejaba `Guardar` y el interruptor inertes
para siempre con el borrador atrapado— **no está en ese Outline ni en ningún
otro**; sólo lo roza `@s40` fila 1 por el lado del botón.

El carril movió las tres pruebas a `@s40`, porque ahí es donde el contrato dice
algo aplicable («deshabilitado **hasta** la respuesta»), y **no enmendó** nada.

**Lo que hay que decidir:** ¿gana `@s43` una cuarta fila de `Examples` para la
carrera entre escrituras, o basta con la lectura de `@s40` fila 1?

---

## 5. Feature 30 — el enlace del historial cuando la regla ya no crea tareas

**Qué pasa.** En `automations.tsx:577-585`, si la regla es `NOTIFY_WEBHOOK` el
segmento de proyecto se resuelve a `""` y el `href` sale `/proyectos//tareas/<id>`.

Hoy es inalcanzable por el camino normal —una regla de webhook no crea tareas—
pero **sí** es alcanzable tras un PUT que cambie una regla `CREATE_TASK` a
`NOTIFY_WEBHOOK`: sus ejecuciones antiguas conservan `createdTaskId` y pintarían
un enlace roto. `@s41` dice «un enlace a la tarea creada» y no dice qué hacer
aquí.

**Lo que hay que decidir:** ¿ocultar el enlace, apuntar a la tarea sin proyecto, o
algo más? El carril **no inventó comportamiento**, que es lo correcto.
