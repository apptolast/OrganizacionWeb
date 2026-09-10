# Libro de ratificaciones del propietario

Cada cambio de contrato pasa por el propietario. Hasta hoy esa aprobación vivía
sólo en la conversación, así que un juez que lee el repositorio no podía
distinguir «ratificado» de «lo escribió el mismo carril que hizo la enmienda».
El panel de precierre de la feature 29 lo señaló con esas palabras, y tenía razón
**en la forma**: la decisión existía, el artefacto no. Aquí está el artefacto.

Cada entrada lleva la pregunta tal como se le hizo y la respuesta tal como la
eligió. No se anota nada aquí que no se pueda citar así.

---

## R1 — 10 de septiembre de 2026 — dos enmiendas alineadas con decisiones previas

**Pregunta:** «Dos carriles enmendaron su `.feature` para alinearlo con enmiendas
tuyas ya ratificadas. Según la disciplina del repo, todo cambio de contrato pasa
por ti. ¿Las ratificas?»

**Opción elegida:** «Ratifico las dos», descrita así en la propia pregunta:

> La de la feature 29 alinea su `@s31` con la enmienda ya ratificada del `@s31`
> de la 27 (las dos filas Bearer pasan a 403 `API_SCOPE_DENIED`); el juez la
> revisó y dijo que es correcta, no extralimitación. La de la 28 amplía su
> contrato con el plazo de lectura del cuerpo del feed, que cierra un agujero de
> recursos.

**Alcanza a:**
- `features/additional_connectors.feature:388` — `@s31`, las dos filas de
  credencial Bearer pasan de 401 `UNAUTHENTICATED` a 403 `API_SCOPE_DENIED`.
  Es la condición **C7** de `progress/judge_additional_connectors.md`.
- La ampliación del contrato de la 28 con el plazo de lectura del cuerpo del feed.

---

## R2 — 9 de septiembre de 2026 — el `@s31` de la feature 27

Ratificada en su día y ya escrita en el propio contrato
(`features/github_connector.feature:397`, «Enmienda del 9 de septiembre de 2026,
ratificada por el propietario»). Es la enmienda que R1 extiende a la 29.

## R3 — 9 de septiembre de 2026 — la fila «cadena vacía» de `APP_CONNECTOR_KEY`

Ratificada y escrita en `features/github_connector.feature:55`.

---

## Lo que NO está ratificado

Vive en `progress/decisiones_pendientes.md`. Nada de allí puede darse por bueno,
por razonable que parezca la enmienda.
