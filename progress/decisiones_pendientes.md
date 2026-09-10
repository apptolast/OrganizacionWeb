# Decisiones que esperan al propietario — 10 de septiembre de 2026

Nada de esto bloquea el trabajo: los carriles siguen. Bloquea el **cierre** de la
feature que se nombra en cada punto, porque son cambios de contrato y la
disciplina del repo dice que los firmas tú.

---

## 1. Feature 25 — el `@s9` promete modo degradado y el código muere al arrancar

**Qué pasa.** `features/webhooks.feature:129-142` dice «Sin clave de cifrado
válida la aplicación arranca degradada / Then la aplicación queda disponible» y
lista tres filas de `<clave>`: `ausente`, `base64 de 31 bytes` y `texto no
base64`, las tres con resultado `503 CONNECTORS_DISABLED`.

La producción hace otra cosa en dos de las tres:
`AesGcmWebhookSecrets.from` (`backend/src/main/java/.../adapter/webhook/AesGcmWebhookSecrets.java:54-59`)
devuelve `null` **sólo** si la clave está ausente y **lanza**
`IllegalStateException` en los dos casos malformados; `ApplicationConfiguration:451`
construye el bean con ella, así que el contexto no refresca y **la aplicación no
arranca**. `AesGcmWebhookSecretsTest.s9_b5_aMalformedCurrentKeyFailsFastInsteadOfDegrading`
afirma exactamente lo contrario del contrato.

Y no es un despiste: es la política que **ya ratificaste** en la enmienda B5/B6
de seguridad, escrita en `project-spec.md:2496`: «fallo rápido al arrancar si la
clave está mal formada, modo degradado con `CONNECTORS_DISABLED` solo si está
ausente». El código obedece esa política; el `.feature` nunca se tocó.

Lo mismo, menor, en `@s8:125-126`: el contrato dice que el dato adicional
autenticado es «el id del endpoint» y la producción usa `ownerId + "|" +
endpointId`, que es la enmienda **B6** del mismo `project-spec.md:2496`.

**Mi lectura.** Lo que caducó es el `.feature`, no el código. Pero el efecto
visible para un operador no es cosmético: quien se equivoque copiando
`APP_CONNECTOR_KEY` recibe una aplicación muerta, no el modo degradado que el
contrato describe. Merece que lo decidas mirándolo, no que lo alinee un carril.

**La pregunta, exacta:** ¿enmiendo el `.feature` para que diga lo que fija la
política ya ratificada —las dos filas malformadas de `@s9` pasan a «la aplicación
no arranca y registra `CONFIGURATION_ERROR`», y la línea `@s8:125-126` pasa a
«el propietario y el id del endpoint como dato adicional autenticado»—, con su
nota de enmienda fechada dentro del fichero al modo de la de `@s33`? ¿O prefieres
lo contrario, que la producción degrade también con clave malformada, lo que
revocaría la enmienda B5 de la revisión de seguridad en las tres features que la
comparten?

**No lo he tocado.** Es el bloqueante **B6** de
`progress/carriles/bloqueantes_25.md` y queda declarado abierto en
`progress/mutation_webhooks.md`.

---

---

---

---

---

## 2. Feature 28 — el plazo de 5 s pasa a cubrir el intercambio completo

**Qué cambió, sin contrafirma.** El commit `baf5ab1f` reescribió
`features/external_calendar.feature:13-14`. Antes: «Descarga con redirecciones
deshabilitadas, **timeout de 5 s**, Accept text/calendar…». Después, el plazo pasa
a ser del **intercambio completo** —conexión, cabeceras y lectura del cuerpo— y se
añade esta fila al `@s12`:

> `| 200 text/calendar que envía las cabeceras y luego gotea el cuerpo sin cerrar | FEED_UNREACHABLE |`

**Por qué se hizo.** Sin ese plazo total, un proveedor que manda las cabeceras y
luego gotea el cuerpo sin cerrarlo **retiene un hilo indefinidamente**. Es un
agujero de recursos real, y el propio texto lo dice: «ninguna descarga puede
retener un hilo más de 5 s».

**Nota:** esto puede ser lo que ya ratificaste como «la de la 28 amplía su
contrato con el plazo de lectura del cuerpo del feed» en la entrada R1 de
`progress/ratificaciones.md`. Si es así, dilo y lo doy por cerrado citando esa
ratificación; lo pregunto porque **no lo puedo afirmar yo**.

---

---

---

---

## 3. Feature 28 — el certificado que no vale para su nombre

**Qué cambió, sin contrafirma.** El commit `78dca3a6` añadió esta fila al
`@s12`:

> `| presenta un certificado que no es válido para su nombre | FEED_UNREACHABLE |`

**Mi lectura.** Es la consecuencia directa de la decisión que **sí** tomaste
—«Anclar, y probar el TLS»— aplicada al calendario externo: si se ancla la
dirección, hay que comprobar que el certificado corresponde al nombre, o el
anclaje no sirve de nada. Pero es una fila nueva del contrato y la firma es tuya.

---

---

---

---

## 4. Feature 28 — el sitio de «Calendario externo» en el menú

**Qué pasa.** La enmienda de navegación que ratificaste fija el orden de las
entradas del menú, y «Calendario externo» aparece en `workspace.tsx` en una
posición que esa enmienda no nombra explícitamente.

**Lo que hay que decidir:** confirmar la posición actual, o decir cuál es la
correcta. Es de dos minutos y cierra la última puerta humana de esta feature.
