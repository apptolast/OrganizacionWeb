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
