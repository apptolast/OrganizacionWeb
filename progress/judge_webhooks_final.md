# Review — feature 25 webhooks (`features/webhooks.feature`)

**Veredicto: APPROVED — sin condiciones.** Quinta pasada, 10 de septiembre de 2026, noche.
Las dos bloqueantes de la cuarta pasada (B1, el vector de firma que no verificaba; B2, las claves
de `SubtaskCreated.v1`) están **cerradas y comprobadas con cálculo, no de palabra**. Sustituye a
todos los veredictos anteriores de este fichero.

La 25 puede pasar a `done`.

---

## B1 — el vector de firma: CERRADA, verificada con HMAC real

`docs/webhooks.md:58-61` publica ahora el cuerpo en **una sola línea compacta**, precedida de
`<!-- prettier-ignore -->` para que el formateador no vuelva a romperlo. Recalculé desde el
fichero tal como está hoy, con la clave que la propia página declara:

| Comprobación | Resultado |
|---|---|
| Longitud de `docs/webhooks.md:60` | **209 bytes UTF-8**, como anuncia `:55-56` |
| ¿Idéntica al contrato? | **Sí, carácter a carácter** con `features/webhooks.feature:235` (salvo la sangría de 6 espacios del docstring de Gherkin) |
| `t=1788861600` | `47db42f51507bea71512fc26bef335a304b9382b45b590a774cfc977d6cd708f` |
| `t=1788861660` | `fc161fb2f63428f0680cae6871216284bb420af9917ee794c8159d0400abe460` |

Las dos coinciden **al carácter** con lo que el documento publica en `:71` y `:76`, con lo que el
`@s15` fija en `features/webhooks.feature:239,241`, y con lo que produce
`WebhookSignature.header` (clave = `secret.getBytes(UTF_8)` sobre la cadena entera,
`WebhookSignature.java:16`; mensaje = `t + "." + body`, `:17-18`).

**El ejemplo ya se puede usar para lo que dice que sirve.** Quien lo copie y obtenga `47db42f5…`
sabrá que su verificador está bien; antes obtenía `ff3d3fd0…` y habría ido a depurar código sano.

Y la corrección hace algo mejor que arreglar el número: `:63-66` explica **por qué** —«la firma es
sobre bytes, no sobre el JSON como estructura»— y lo enlaza con la lección que la página ya daba
en `:36-40` para el cuerpo que se recibe. La misma regla, dicha dos veces en los dos sitios donde
muerde. Eso es enseñar, no parchear.

## B2 — las nueve claves de `SubtaskCreated.v1`: CERRADA

`docs/webhooks.md:92-95` distingue ahora los dos tipos: `TaskCreated.v1` lleva **ocho** —los seis
comunes más `taskId` y `title`— y `SubtaskCreated.v1` lleva **nueve**, «porque añade además
`parentTaskId`». Es exactamente `OutboxMessage.java:45-55` más el `expected.add("parentTaskId")`
de `:78-81`, y la clave extra está además exigida en `:342-346` con formato de UUID y distinta de
`taskId`. El ejemplo de `:97-108` sigue siendo un `TaskCreated.v1` con sus ocho campos correctos,
y el aviso de `:110-112` contra las listas cerradas sigue en su sitio.

## El punto 5, ya fallado y sin cambios

La contrafirma pendiente de `project-spec.md:2044` **no bloquea**, por las tres razones que dejé
escritas en la pasada anterior: no toca ninguna puerta, corrige el documento hacia la verdad y es
una decisión que sobrevive a la feature. Vive por su carril en
`progress/decisiones_pendientes.md:48-62`. **No retiene el cierre.**

---

## Lo único que apareció al comprobar, y es mío: la cifra «238» está mal, son **234**

Me pediste saberlo ahora antes que cerrar mal, así que lo digo con todas las letras.
`docs/webhooks.md:63-65` dice «si lo reindentas, pasa de 209 bytes a **238**». **Son 234.** El
bloque reindentado que había antes mide 234 bytes; los 4 que sobran son la valla de cierre del
bloque de código y su salto de línea, que **se colaron en mi medición** de la cuarta pasada al
recortar el fichero por líneas. Publicaste 238 porque yo te di 238. El error es mío y lo firmo.

**No lo hago bloqueante, y explico por qué no me estoy siendo indulgente:**

1. **No puede engañar a nadie en la práctica.** Ningún receptor calcula nada con esa cifra: es
   ilustrativa dentro de una frase cuyo mensaje —reindentar rompe la firma— es **cierto** con 234
   igual que con 238. Lo que un integrador ejecuta es el vector, y el vector está exacto.
2. **La lección no depende del número.** La frase funciona entera si se quita la cifra.
3. **Bloquear cuatro veces una página por cuatro bytes que introduje yo sería mal criterio**, no
   rigor. El rigor era detectar que el ejemplo no verificaba; eso ya está hecho y arreglado.

**Corrígelo cuando toques el fichero por cualquier otro motivo:** «pasa de 209 bytes a 234», o
sencillamente «crece» sin cifra. No hace falta un commit para esto solo, y no condiciona el `done`.

## Las otras dos no bloqueantes que quedan vivas

- **`:68` arrastra un «y» huérfano.** El párrafo nuevo de `:63-66` se metió entre el bloque del
  cuerpo y la frase «y `t=1788861600`, la cabecera es exactamente:», que ahora empieza una sección
  de texto con una conjunción colgando. Cosmético.
- **El bloque «Hueco conocido» de `:198-208` sigue desfasado.** Su última frase —«Cerrarlo bien
  pide una fila de contrato y su oráculo»— es justo la vía que `project-spec.md:2044` y
  `progress/decisiones_pendientes.md:54-58` descartaron por escrito con mi ratificación. Ya no hay
  hueco que declarar: el spec dice la verdad. El bloque sobra o se reduce a un puntero.

Siguen vigentes, sin cambios, la nota del plazo (5 s de conexión y 10 s de intercambio,
`JdkWebhookSender.java:48-49`, que la página no cifra) y las no bloqueantes 1 a 5 de la segunda
pasada.

---

## Estado de las puertas

**Ninguna necesita repetirse, y esto no es fiarme: es comprobable.** Desde el verde acreditado
`17b823f9`, `git diff --name-only` devuelve `backend/build.gradle.kts`, `docs/webhooks.md`,
`project-spec.md` y cuatro ficheros de `progress/`. **Ni un fuente, ni una prueba, ni un `.feature`.**
`git status --porcelain` está limpio.

Y comprobé lo único que un cambio de markdown **podría** haber roto: la puerta de lint. El `lint`
del arnés es `node scripts/project.mjs lint` = `spotlessCheck` de Gradle, `node --check` sobre
siete `.mjs` listados, y `pnpm --dir frontend lint` (`eslint . && prettier --check .`), que corre
**dentro de `frontend/`**. `docs/` no entra en ninguno de los tres. El Prettier que reindentó la
página era el del editor, no una guarda. La edición no puede teñir el lint.

- **Mutación backend:** 400/431 = **92,81 %** (umbral 80 %).
- **Mutación frontend:** 592/626 = **94,57 %** (umbral 80 %).
- **`harness init`:** verde entero — lint sin errores, 89/89 guardas, backend completo, 2960
  pruebas de frontend, «Entorno listo».

## Checkpoints

- **C1** [x] — lint y 89/89 guardas verificados sobre este mismo árbol de código, intacto desde
  `17b823f9`, y `docs/` queda fuera del alcance del lint.
- **C2** [x] — 27 features; 25 y 28 en `in_progress`, permitido por `one_feature_at_a_time: false`.
- **C3** [x] — ninguna producción de la 25 sin test que la pida.
- **C4** [x] — misma base que C1.
- **C5** [x] — `git status --porcelain` vacío.
- **C6** [x] — **los 42 `@s` tienen test y la guía pública ya no afirma nada que el código
  desmienta.** Era el último en rojo.
- **C7** [x] — las dos mutaciones por encima del umbral, recomputadas por mí sobre informes que
  coinciden con el árbol.

## Cobertura de escenarios (@s ↔ test)

- @s1..@s42: **[x]**. Sin cambios: las tres últimas entregas son markdown puro.

## Disciplina TDD

- **Rojo-Verde-Refactor:** SÍ.
- **¿Producción sin test que la pida?** **NO.** Y lo subrayo por última vez: la decisión de **no**
  colar el `<a>` en `webhooks.tsx` para hacer verdadera una línea de prosa fue disciplina bien
  aplicada. Se corrigió el documento, que era lo que estaba mal.

---

## Resumen

Cierro la 25. Costó cinco pasadas y la última tuvo su gracia: la bloqueante final fue un ejemplo
de firma que no verificaba porque un formateador lo reindentó al guardar, en una página cuyo tema
central es **firmar los bytes exactos y no reserializar**. El documento se tropezó con su propia
lección; ahora la enseña dos veces y trae el `<!-- prettier-ignore -->` que impide que vuelva a
pasar.

Lo que queda entregado es una guía pública que dice la verdad en todo lo comprobable: las cuatro
exigencias de la enmienda B4 con el número 300 y su porqué, los doce tipos en su orden canónico
con las etiquetas reales de la interfaz, la escalera de reintentos, el sexto intento y la
desactivación, las cabeceras exactas, la clave del HMAC —el dato sin el cual nada de esto se puede
verificar— y un vector que **cualquiera puede reproducir en treinta segundos y le cuadrará**.

Detrás siguen las dos puertas de mutación por encima del umbral, `harness init` verde y los 42
escenarios con test. La contrafirma del propietario sobre `project-spec.md:2044` viaja aparte y no
retiene nada. La única corrección que dejo abierta es una cifra ilustrativa de cuatro bytes que
introduje yo, y que se arregla la próxima vez que alguien abra el fichero.

**APPROVED. Sin condiciones.**
