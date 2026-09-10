# Review — feature 25 webhooks (`features/webhooks.feature`)

**Veredicto: CHANGES_REQUESTED** — **1 sola condición bloqueante**. Segunda pasada, 10 de
septiembre de 2026, tarde. Sustituye al veredicto de las 18:49 de este mismo fichero.

Encargo de esta pasada: comprobar si las **cuatro condiciones bloqueantes** del veredicto
anterior están cerradas de verdad, y verificar la corrección del recuento de guardas de
aborto. No se reaudita lo ya dado por bueno (las dos puertas de mutación, las cuatro
enmiendas ratificadas, los cierres 3, 12 y 13 del carril).

---

## Las cuatro bloqueantes: 3 cerradas, 1 cerrada con la mejor evidencia disponible

### Bloqueante 1 — el spec se contradecía con el contrato · **CERRADA**

`project-spec.md:2026` lleva ahora las **dos** enmiendas escritas en su sitio, cada una con
su nota fechada y su ratificación nombrada:

- AAD: «**AAD igual al `id` del propietario junto al `id` del endpoint** —enmienda del 10 de
  septiembre de 2026, ratificada por el propietario (**R10**)…». Coincide con
  `features/webhooks.feature:124-137` (el id del propietario junto al id del endpoint como
  dato adicional autenticado, más las dos filas de fallo cruzado) y con producción,
  `AesGcmWebhookSecrets.java:135-136` (`ownerId + "|" + endpointId`).
- Clave: «Clave **ausente**: la aplicación arranca —enmienda… (**R11**): decía "ausente o
  mal formada", y una clave **presente pero inválida** no deja arrancar ni debe hacerlo…».
  Coincide con `features/webhooks.feature:141-166`, cuyas seis filas del `Examples` dicen
  todas `ausente`, y con `AesGcmWebhookSecretsTest:87-102`.

Ya no hay dos verdades: la 2026 y la 2496 dicen lo mismo. **Recorrí la sección 25 entera**
(`project-spec.md:1993-2059`) buscando lo que se me pidió —cualquier otra afirmación que el
código desmienta—. El cuerpo normativo (rutas, DTO, códigos de error y prioridad, firma,
plazos, reintentos, cupo, SSRF, límites) resiste, y el `@Scheduled(fixedDelay = 1000,
initialDelay = 1000)` de `:2036` es justo lo que R5 usó para enmendar el plazo del `.feature`:
ahí el spec era el que tenía razón. **Pero encontré una promesa incumplida**, y es la única
condición que queda: ver «Lo que sigue abierto».

### Bloqueante 2 — el informe de mutación medía el árbol anterior · **CERRADA**

Verificado por mí sobre `frontend/reports/mutation-webhooks/mutation.json` (mtime
2026-09-10 18:50 local, 4 255 402 bytes):

| fichero | líneas en el informe | líneas en disco | ¿byte a byte? | sha256-16 |
|---|---|---|---|---|
| `src/webhooks.tsx` | 581 | 581 | **sí** | `bd2ca8e28edace13` |
| `src/webhooks-client.ts` | 274 | 274 | **sí** | `4944837c08a3d028` |

Y su copia de `webhooks-client.ts` **sí contiene** `SECRET_UNREADABLE` y
`webhookErrorClasses`. El informe caducado ya no está. Recomputado del JSON:
`Killed 592, Survived 33, NoCoverage 1, RuntimeError 1` → **592/626 = 94,57 %**, y por
fichero `webhooks.tsx` 312/340 = 91,76 % y `webhooks-client.ts` 280/286 = 97,90 %. La puerta
de frontend queda **demostrada sobre el árbol de `main`**, no por coincidencia.

Un matiz que hay que dejar escrito, no bloqueante: ver nota no bloqueante 1.

### Bloqueante 3 — la bitácora canónica publicaba las líneas de la campaña anterior · **CERRADA**

`progress/mutation_webhooks_frontend.md:53-58` publica ahora **73, 78, 102, 102, 115, 119**
para `src/webhooks-client.ts`. Recomputé la lista completa de no-matados del informe vigente
y coincide **fila a fila** con la tabla de la bitácora: 34 entradas (33 `Survived` + 1
`NoCoverage`), con el `RuntimeError` de `webhooks.tsx:517` fuera de la tabla y fuera del
denominador, que es lo correcto. Las 28 de `webhooks.tsx` siguen siendo correctas.

Residuo documental en el fichero hermano: ver nota no bloqueante 2.

### Bloqueante 4 — `harness init` sobre el árbol de cierre · **CERRADA con la evidencia disponible**

No encontré en el árbol el registro de la ejecución: no hay ningún fichero con «Entorno
listo», ni en `progress/` ni fuera, y `git status` está limpio. El registro que sí existe,
fechado e inmutable, son los **mensajes de commit** `17b823f9` (harness init acababa en rojo
por esto… Las suites pasaban: 2960 pruebas de frontend y toda la de backend. 89/89 guardas)
y `d94efb79` (368 patrones, 0 muertos. 89/89 guardas).

Como eso es autorreporte, verifiqué yo los pasos baratos de `init` sobre el árbol de ahora,
que además son exactamente los que la cirugía de retirada rompió:

- **Guardas del arnés: 89/89 verdes.** `node --test scripts/project.test.mjs` → `pass 89,
  fail 0`, ejecutado por mí.
- **Lint verde.** `node scripts/project.mjs lint` → `spotlessCheck` BUILD SUCCESSFUL,
  `eslint` sin salida y `prettier --check` con «All matched files use Prettier code style!».
- **`feature_list.json` válido, 27 features**, 25 y 28 en `in_progress` (`harness status`).

Y el eslabón que cierra la cadena: `git diff --name-only 17b823f9 HEAD` devuelve
**`backend/build.gradle.kts` y nada más**. Desde el verde declarado no se ha tocado ni un
fuente ni una prueba; lo único que cambió son las declaraciones de ámbito de mutación, y ese
`build.gradle.kts` configura bien —mi propia ejecución de `spotless` lo prueba—. Doy la
condición por cerrada. Lo que faltó fue **escribirlo en un fichero**: un mensaje de commit no
es el sitio donde el siguiente auditor va a buscar el acta de la puerta.

Nota de riesgo que el veredicto anterior pedía dejar escrita y sigue vigente: la campaña de
backend corrió antes de que el carril reescribiera `Slf4jWebhookAuditTest`; aun perdiendo esas
tres muertes la puerta quedaría en 397/431 = 92,11 %. No hace falta remedir.

---

## La corrección del recuento de guardas de aborto: **verificada, y es correcta**

Localicé las guardas **por texto** sobre el `source` que el propio informe guarda, no por
coordenadas: todo superviviente cuya línea contiene `aborted`. Salen **exactamente 12**, en
**119, 122, 125, 151, 158, 161, 216, 227, 256, 269, 281, 288**, que son las doce que publica
`progress/mutation_webhooks_frontend.md:41`, y las doce son `ConditionalExpression` sobre
`if (signal.aborted) return` o sobre `if (!…aborted) …`. Ni una más, ni una menos.

La aritmética de la segunda tabla también sale: 626 − 12 = 614 y 592/614 = **96,42 %**;
contándolas todas en contra, 94,57 %, que son **catorce puntos** por encima del umbral. El
fallo condicionado queda firme y ahora se apoya en el número real. La bitácora deja escrito,
en `:20-24` y `:45-47`, que el número anterior era la mitad y por qué. Bien hecho: se corrige
la cifra sin fingir que la conclusión cambia.

## Y una que se cerró sola: `WebhookStatusSource`

Confirmado, ya no existe en `backend/src/main`. El no bloqueante 7 del veredicto anterior
—producción de la 25 que ningún `@s` de la 25 pedía— desaparece con la retirada de la 29.
**No queda producción sin test que la exija.**

---

# Lo que sigue abierto

## Condición única y bloqueante — `docs/webhooks.md` no existe, y la enmienda de seguridad **B4** consiste enteramente en ese documento

- **Bloqueante:** sí · **De:** orquestador · **Estimación:** 30 min (o 10, por la vía B)

`project-spec.md:2494`, dentro de «**Enmiendas de seguridad a las features 24–28** — 9 de
septiembre de 2026», dice:

> «**B4.** …La **documentación pública pasa a exigir** que se verifique la firma primero, que
> se deduplique por el identificador de evento que va dentro del cuerpo firmado, que se
> rechacen instantes con más de trescientos segundos de diferencia y que la comparación de la
> firma sea en tiempo constante.»

Esa documentación pública es `docs/webhooks.md`, y **no existe**. `ls docs/` devuelve 25
ficheros y ninguno es ése; sus hermanas sí están (`integration-api.md`, `ics-calendar.md`,
`external-calendar.md`, `import-data-api.md`, `outbox-publishing.md`). Consecuencias, las
tres comprobadas:

1. **B4 es la única de las seis enmiendas de seguridad de la 25 que sigue sin entregar.** B2,
   B3 (revocada con su nota y su `deploy/EGRESS.md`), B5 y B6 —éstas dos convertidas hoy en
   R11 y R10— están cerradas y verificadas. B4 no. Y no es cosmética: sin ese documento el
   receptor no sabe que debe deduplicar por el `eventId` **del cuerpo firmado** en vez de por
   la cabecera `X-OrganizationWeb-Event-Id`, que viaja sin firmar; ni que debe rechazar
   `|now − t| > 300 s`; ni que la comparación debe ser en tiempo constante. Es el material de
   `progress/security_review_connectors.md:41` y `progress/tdd_webhooks.md:46-50`.
2. **La sección 25 afirma dos veces algo que el árbol desmiente**, que es justo lo que se me
   mandó comprobar al cerrar el bloqueante 1: `:2044` describe el formulario con «casillas por
   tipo de evento con "Seleccionar todos" y **ayuda que enlaza `docs/webhooks.md`**» —
   `frontend/src/webhooks.tsx:402` tiene el "Seleccionar todos" y **ninguna** cadena `docs/`
   ni `href` en todo el fichero—; y `:2058` promete «**Documento para receptores
   `docs/webhooks.md`** con ejemplo de verificación de firma y tolerancia recomendada de 5 min
   para `t`».
3. **Hay una decisión de contenido ya tomada que sólo vive ahí**:
   `progress/gherkin_webhooks.md:66` y `:87` fijan que la clave HMAC son los **bytes UTF-8 de
   la cadena `whsec_…` completa**, no los 32 bytes decodificados, y anotan que
   `docs/webhooks.md` lo documenta. Hoy ese dato —sin el cual ningún receptor puede verificar
   una firma— no está publicado en ninguna parte legible por un tercero.

**Digo con claridad de dónde sale esto y por qué aparece ahora**, para que no parezca que
muevo la portería: **no lo vi en mi pasada anterior y debí verlo.** Pero no es un hallazgo
inventado hoy ni una exigencia nueva: está registrado desde antes de la integración en
`progress/auditoria_colisiones_25_28_30.md:846-867`, que comprobó los hechos uno a uno,
refutó que fuera una colisión de fusión —con razón— y escribió textualmente que
«pertenece a la lista de **pendientes para cerrar la feature 25**» y que «**la puerta de
`done` del coordinador lo atrapará de todos modos**». Esa puerta soy yo. Si apruebo sin esto,
la deuda que aquel análisis dejó deliberadamente para este momento no la atrapa nadie nunca.

**Trabajo — dos vías, cualquiera cierra:**

- **Vía A (entregar):** escribir `docs/webhooks.md` con lo que B4 exige (verificar firma
  primero; deduplicar por el `eventId` del cuerpo firmado; rechazar `|now − t| > 300 s`;
  comparación en tiempo constante), el formato de `X-OrganizationWeb-Signature`
  (`t=<unix>,v1=<hex de HMAC-SHA256(secreto, t + "." + body)>`), **la clave HMAC = bytes UTF-8
  de la cadena `whsec_…` completa** y un ejemplo verificable. Y después, o bien añadir el
  enlace de ayuda que `:2044` promete, o bien enmendar `:2044` con nota fechada si se decide
  que la ayuda no lleva enlace.
- **Vía B (retirar, con nota):** si el propietario decide que el documento queda fuera del
  alcance de la 25, enmendar con nota fechada y ratificación las **tres** líneas —`:2044`,
  `:2058` y `:2494`— para que dejen de prometerlo, y declararlo en
  `progress/ratificaciones.md`. Lo que no vale es la tercera vía, que es la de hoy: dejar el
  documento prometido tres veces y marcar la feature `done`.

---

## No bloqueantes (no retienen el `done`; conviene cerrarlas de paso)

**1. El informe de frontend está en `.gitignore`, y la bitácora dice que ya está en el
repositorio.** `git check-ignore -v frontend/reports/mutation-webhooks/mutation.json` →
`.gitignore:59: reports/`. El fichero está en **esta copia de trabajo**, no en el árbol: un
clon limpio no lo tiene. `progress/mutation_webhooks_frontend.md:18-19` dice «Ya está en el
repositorio, y se comprobó que su copia del fuente coincide byte a byte con `frontend/src/`».
La segunda mitad es cierta —la verifiqué—; la primera, no. No lo hago bloqueante porque el
estándar es el mismo que acepté para backend (`backend/build/reports/pitest-webhooks/mutations.xml`
también es salida de build ignorada, y la di por buena recomputándola). El arreglo es una
frase: corregir «está en el repositorio» y añadir lo que pedía el bloqueante 2 —ruta exacta,
marca de tiempo y los dos hashes—, que ya están calculados en la tabla de arriba.

**2. `progress/mutacion_webhooks_frontend_medida.md` sigue publicando las líneas caducadas y
no está marcado como superado.** Su `:5` dice «Sobre el SHA `1239ad0e`» y sus `:41-46` siguen
dando **67, 72, 96, 96, 109, 113**. Sus cifras globales y por fichero son idénticas a las
vigentes —lo comprobé: 592/626, 312/340, 280/286—, así que el daño es sólo de coordenadas, y
la bitácora canónica ya se declara canónica en su `:5-7`. Basta un banner de «superado por
`progress/mutation_webhooks_frontend.md`» en su cabecera.

**3. `features/webhooks.feature:455` cita un fichero que ya no existe.** Respondo a la
pregunta directa: **la cita sigue en pie**. La nota de enmienda del `@s33` dice «con la misma
lectura ya ratificada por el propietario para la feature 26 (`github_connector.feature:398`)»
—y `features/github_connector.feature` se borró hoy con la retirada de la 27—. Dos defectos
en una línea: el fichero no existe y el número de feature está mal (era la **27**, no la 26).
No bloquea porque la **sustancia sigue anclada en artefactos vivos**:
`progress/ratificaciones.md:23-31` (R1) y `features/integration_api.feature:263` y `:285`,
donde una credencial Bearer sin scope responde 403 `API_SCOPE_DENIED`. Reapuntar la cita a
esos dos.

**4. El javadoc de `WebhookScheduleTest:224-230` sigue declarando abierto lo que R5 cerró.**
Sin cambios desde el veredicto anterior: sigue diciendo que el When de @s32 dice «transcurren
**1500 ms**» y que la cláusula temporal de @s32 «sigue declarada abierta», cuando
`features/webhooks.feature:438` dice 2500 ms desde hoy y `progress/decisiones_pendientes.md`
está vacío. Es un javadoc que afirma lo que no es, en el fichero del que iba la enmienda.

**5. Sigue abierto y bien declarado** el hueco de
`progress/literales_sin_oraculo_webhooks.md:149-153`: el `ORDER BY d.next_attempt_at, d.id` de
`PostgresWebhookWork.java:67` no tiene oráculo y PIT no lo ve por vivir dentro de un literal.
Y `features/webhooks.feature:155-157` sigue declarando un hueco **mayor del que hay**: el
arranque que falla con clave malformada sí tiene oráculo propio en
`AesGcmWebhookSecretsTest:87-102`; lo que falta es sólo el nivel de cableado.

---

## Checkpoints

- **C1** [x] — ficheros base y docs presentes; lint y las 89 guardas verdes verificados por
  mí sobre este árbol; `feature_list.json` válido. La suite completa, acreditada por
  `17b823f9` con el árbol de código intacto desde entonces.
- **C2** [x] — 25 y 28 en `in_progress`, permitido por `one_feature_at_a_time: false`.
  27 features.
- **C3** [x] — ya sin salvedad: `WebhookStatusSource` desapareció con la retirada de la 29.
  Ninguna producción de la 25 sin test que la pida.
- **C4** [x] — misma base que C1.
- **C5** [x] — `git status --porcelain` **vacío**, verificado al abrir y al cerrar esta
  revisión. Rectifico el `[ ]` del veredicto anterior: el árbol está quieto.
- **C6** [ ] — los 42 `@s` tienen test y el contrato ya no se contradice sobre AAD ni sobre
  la clave, pero la sección 25 promete tres veces un documento público que no existe. Ver la
  condición bloqueante.
- **C7** [x] — las dos puertas por encima del 80 %, recomputadas por mí de sus informes:
  backend 400/431 = **92,81 %**, frontend 592/626 = **94,57 %**, ésta ya sobre un informe que
  coincide byte a byte con `frontend/src/`. Supervivientes nominados, y los 12 de las guardas
  de aborto contados correctamente.

## Cobertura de escenarios (@s ↔ test)

- @s1..@s42: **[x]**, sin cambios respecto de la pasada anterior. Ninguna de las cuatro
  correcciones tocó fuentes ni pruebas: `git diff --name-only fe5bc7ca HEAD` no incluye ni un
  fichero de `backend/src`, `frontend/src` o `e2e`. Nada de lo verificado se rompió al
  corregir.

## Disciplina TDD

- **Rojo→Verde→Refactor:** SÍ, sin cambios. Ninguna de las cuatro correcciones tocó código.
- **¿Producción sin test que la pida?** **NO.** La única que había, `WebhookStatusSource`,
  se fue con la retirada de la 29.

---

## Resumen

Las cuatro bloqueantes están cerradas, y tres de ellas con evidencia que pude recomputar yo:
el spec ya dice lo que el código hace, el informe de frontend mide el árbol de `main` byte a
byte, y la bitácora publica las líneas de la campaña vigente. La quinta comprobación —el
recuento de guardas— es correcta: son doce, están donde dice, y la conclusión aguanta con
catorce puntos de margen. Las correcciones no rompieron nada.

Queda **una sola cosa**, y no es de las que se arreglan mirando para otro lado: la feature
lleva desde el 9 de septiembre una enmienda de seguridad ratificada, **B4**, cuya entrega
completa es un documento público que nadie ha escrito, y la sección 25 lo promete tres veces.
El análisis de colisiones lo dejó por escrito para que lo atrapara esta puerta. La atrapo.
Escribirlo —o retirarlo con su nota— es media hora, y entonces la 25 pasa a `done` sin
condiciones.
