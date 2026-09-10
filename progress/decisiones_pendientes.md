# Decisiones que esperan al propietario

**Ninguna. La lista está vacía desde el 10 de septiembre de 2026, 19:15.**

Llegó a tener **quince**. Se cerraron así:

| Cómo se cerró                                                             | Cuántas |
| ------------------------------------------------------------------------- | ------- |
| Respondidas por el propietario hoy                                        | 6       |
| Ya estaban ratificadas y se cerraron **por cita**, sin volver a preguntar | 4       |
| Desaparecieron con las features retiradas (27, 29 y 30)                   | 4       |
| Resuelta por la propia cirugía de retirada                                | 1       |

Todas las respuestas y las citas están en `progress/ratificaciones.md`, entradas
R1 a R13, cada una con la pregunta tal como se hizo y la opción tal como se
eligió. Ése es el artefacto que un juez puede comprobar sin la conversación, y
existe porque un panel objetó —con razón— que una contrafirma escrita por el
mismo carril que hizo la enmienda no vale como aprobación humana.

## Las cuatro que se cerraron por cita, porque conviene saber cuáles fueron

No se volvió a molestar al propietario con ellas: se comprobó contra el
transcript que su decisión **ya las alcanzaba**.

- **R4** — `latencyMs` medido con cronómetro monótono (feature 25).
- **R10** — el dato adicional autenticado ata propietario y endpoint (feature 25).
- **R12** — el plazo de 5 s cubre el intercambio completo (feature 28). Lo cubría
  su «Ratifico las dos», cuya opción nombraba esta enmienda literalmente.
- **R13** — el certificado que no vale para su nombre (feature 28). **Es** la
  prueba de TLS que pidió al elegir «Anclar, y probar el TLS», opción que decía
  «se escribe la prueba de TLS/SNI que hoy no existe» para las dos features.

## La que resolvió la cirugía

El sitio de «Calendario externo» en el menú dependía de un orden que incluía
«Conectores» y «Automatizaciones». Las tres features que los ponían ahí se
retiraron, así que la pregunta dejó de existir: el orden lo fija ahora lo que
queda.

## Si vuelve a aparecer alguna

Se anota aquí con la misma forma que tenían las quince: **qué pasa**, **por qué se
hizo**, y **qué hay que decidir**, con recomendación cuando la haya. Nada se da
por ratificado sin cita.

---

## 1. Feature 25 — el enlace del formulario a la guía de firma, ya implementado

**Qué pasaba.** `project-spec.md:2044` decía que la ayuda del formulario de
webhooks **enlaza** a `docs/webhooks.md`. **No lo enlazaba**: no había ningún
enlace en `frontend/src/webhooks.tsx`, comprobado con `grep`.

**Cómo estaba esta mañana.** Se corrigió **el documento**, no el producto,
razonando —con el juez de cierre— que ningún `@s` pedía el enlace y que clavar
la ruta con un oráculo sería frágil.

**Lo que has mandado hacer, y está hecho.** Cerrarlo por el otro lado: el
formulario **enlaza de verdad**. Lleva su fila de contrato, el `@s43` de
`features/webhooks.feature`, y su prueba
(`frontend/src/webhooks.test.tsx`, rojo acreditado antes del enlace). La
objeción del juez se respeta: el oráculo afirma que hay **un enlace a la guía de
firma dentro del formulario, alcanzable con Tab y con nombre accesible que dice
de qué es**, y **no** que el `href` sea `docs/webhooks.md`. Si la guía se muda,
la prueba sigue verde; si el enlace desaparece, cae.

**Lo que falta:** tu contrafirma del `@s43`, porque es una ampliación del
contrato, y de la nueva redacción de `project-spec.md:2044`. El escenario está
marcado en el `.feature` como pendiente de contrafirma. Detalle en
`progress/enlace_docs_webhooks.md`.

**Consecuencia de gestión:** `frontend/src/webhooks.tsx` se ha tocado, así que la
cifra de mutación del frontend de la feature 25 (94,57 %) queda **pendiente de
remedir**.

---

## 2. Feature 25 — fila de contrato nueva: el enlace a la guía de firma

**Qué se ha hecho.** El formulario de creación de webhooks **ya enlaza** la guía
pública, y la fila `@s43` de `features/webhooks.feature` lo fija con su nota
fechada. Eso cierra por el lado correcto el hueco de la decisión 1: en vez de
corregir el documento hacia abajo, el producto hace lo que el spec prometía.

**Cómo está escrita, que es lo que importa.** La prueba afirma que dentro del
formulario hay un enlace **con nombre accesible que habla de la firma**,
alcanzable recorriendo con Tab y con destino no vacío. **No** afirma que el
`href` sea `docs/webhooks.md`.

Esa distinción es del juez de cierre, que desaconsejó expresamente clavar la
ruta: _«fijar una ruta de documentación en la interfaz es frágil y no mata
mutantes»_. La aserción elegida sigue siendo cierta si el documento se publica en
otro sitio, y sigue cayendo si el enlace desaparece.

**Lo que falta:** tu contrafirma de esa fila. Es una ampliación pequeña y del
mismo tipo que las cuatro que ratificaste hoy en esta feature. Si prefieres que
el contrato **no** hable del enlace, se revierte y volvemos a la decisión 1.
