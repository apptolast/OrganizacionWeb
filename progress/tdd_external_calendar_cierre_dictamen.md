# Cierre del dictamen de la feature 28 (calendario externo)

Carril B, worktree `C:/Users/vhurt/ow-worktrees/external-calendar`, rama
`claude/external-calendar`, base `c5f9f93`. `E2E_WEB_PORT=18092`.

Fuente del encargo: `progress/carriles/dictamen_f28.md` (19 hallazgos). Ya
estaban cerrados en la base los hallazgos **2, 7, 16 y 17**; no se tocan.

Reglas de operación aplicadas: `progress/carriles/REGLAS.md`. Backend siempre
por clase concreta, nunca la suite entera.

## Estado

| # | Gravedad | Estado | Ciclo |
| --- | --- | --- | --- |
| 1 | bloqueante | pendiente | — |
| 2 | bloqueante | ya cerrado en la base | — |
| 3 | bloqueante | pendiente | — |
| 4 | bloqueante | pendiente | — |
| 5 | bloqueante | pendiente | — |
| 6 | bloqueante | pendiente | — |
| 7 | bloqueante | ya cerrado en la base | — |
| 8 | alta | **CERRADO** | ciclo 2 |
| 9 | alta | ya cerrado en la base (es el hallazgo 2) | — |
| 10 | alta | pendiente | — |
| 11 | alta | pendiente | — |
| 12 | alta | pendiente | — |
| 13 | alta | pendiente | — |
| 14 | alta | pendiente | — |
| 15 | media | pendiente | — |
| 16 | media | ya cerrado en la base | — |
| 17 | media | ya cerrado en la base | — |
| 18 | media | **CERRADO** | ciclo 1 |
| 19 | baja | pendiente | — |

---

## Ciclo 1 — hallazgo 18: el gate de mutación no muta la ruta ni la entrada de navegación (@s37)

**Qué decía el dictamen.** `frontend/stryker.external-calendar.config.json`
sólo listaba los tres módulos propios del carril. Los cuatro tramos que la
feature 28 introdujo en ficheros compartidos —la ruta, su rama de `section`, su
rama de render y el `RouteLink` con el `aria-current` que @s37 exige por
nombre— quedaban fuera del alcance, contra la convención viva de
`stryker.appearance.config.json` y `stryker.ics-calendar.config.json`.

**ROJO.** Se escribió primero el oráculo en `scripts/project.test.mjs`:

1. se amplió `external calendar Stryker configuration mutates only its own files`
   con los cuatro rangos esperados;
2. se añadió una prueba nueva,
   `external calendar Stryker ranges still cover the route and the navigation entry (@s37)`,
   que **no se cree los números**: recorta `frontend/src/App.tsx` y
   `frontend/src/workspace.tsx` por cada rango `linea:columna-linea:columna`
   (0-based en columnas, fin exclusivo, como el resto del repositorio) y
   comprueba que lo recortado es de verdad ese tramo.

Ejecución antes de tocar la configuración:

```
node --test --test-name-pattern "external calendar Stryker" scripts/project.test.mjs
# tests 2 / pass 0 / fail 2
# "faltan tramos de App.tsx o workspace.tsx"  0 !== 4
```

**VERDE.** Se añadieron al array `mutate` los cuatro rangos, recalculados sobre
el árbol actual (no copiados de la bitácora vieja, que estaba desfasada):

```
src/App.tsx:46:8-46:58        externalCalendar = route === "/calendario-externo"
src/App.tsx:59:14-81:40       su rama de la cadena de `section` hasta `: null`
src/App.tsx:92:10-151:7       su rama de la cadena de render
src/workspace.tsx:81:10-86:22 el RouteLink completo, con el ternario del aria-current
```

`# tests 2 / pass 2 / fail 0`.

**Prueba de que el oráculo discrimina** (rojo provocado a mano sobre producción,
no sobre el test). Se desplazó un solo rango una línea,
`src/App.tsx:46:8-46:58` → `src/App.tsx:47:8-47:58`, y la prueba nueva falló por
el motivo correcto:

```
+ actual: 'automations = route === "/automatizaciones";'
- expected: 'externalCalendar = route === "/calendario-externo"'
```

Es decir: si App.tsx se reordena y los rangos dejan de apuntar al tramo de la 28,
la puerta se pone roja en vez de mutar código ajeno en silencio. Se restauró el
rango y se volvió a verde.

**Ficheros compartidos tocados** (aviso de REGLAS.md §6): `scripts/project.test.mjs`
—sólo el bloque de esta feature, líneas 2101-2160— y
`frontend/stryker.external-calendar.config.json`, que es propio del carril.
`frontend/src/App.tsx` y `frontend/src/workspace.tsx` **no** se han modificado:
sólo se leen desde la prueba.

**Lo que este ciclo NO cierra.** El dictamen pedía además una prueba de @s37 para
la tercera entrada, «retorno tras iniciar sesión». Va aparte, con el hallazgo 15,
que también vive en la transición de sesión.

---

## Ciclo 2 — hallazgo 8: @s38 filas 6, 7 y 8, «Sincronizando» y el bloqueo de controles

**Qué decía el dictamen.** El Then de @s38
(`features/external_calendar.feature:515`) exige para las tres filas de
«Sincronizar ahora» que «aparece "Sincronizando" antes de 400 ms, se envía
exactamente una petición y los controles quedan bloqueados hasta la respuesta».
Ninguna de las tres pruebas de sincronización retenía la respuesta, así que
ninguna podía observar el estado intermedio: dos mutantes sobrevivían.

**ROJO (dos mutantes, provocados a mano sobre producción).**

1. Comentar `setAnnouncement("Sincronizando…")` en
   `frontend/src/external-calendar.tsx:215`:
   `TestingLibraryElementError: Unable to find an element with the text: Sincronizando…`
   → `Tests 1 failed | 24 passed`.
2. Comentar `setBusy("syncing")` en `:214`:
   `Error: expect(element).toBeDisabled()`
   → `Tests 1 failed | 24 passed`.

Ambos eran exactamente los dos mutantes que el dictamen señalaba como
supervivientes. Restaurada la producción: `Tests 25 passed (25)`.

**VERDE.** Prueba nueva
`@s38 anuncia Sincronizando, envía una sola petición y bloquea los controles`:
retiene la respuesta del `POST /sync` con una promesa liberada a mano y, con la
petición en vuelo, afirma

- `findByText("Sincronizando…")`;
- «Sincronizar ahora», «Eliminar suscripción» y «Guardar» `toBeDisabled()`;
- «Etiqueta» y «Dirección secreta iCal` con atributo `readonly`;
- un **segundo** clic sobre «Sincronizar ahora» y
  `calls.filter(POST)).toHaveLength(1)` — esta es la aserción que mata el mutante
  de `setBusy`, porque sin él ni el botón se bloquea ni el guardián de reentrada
  de `:212` guarda nada;
- tras liberar: «Sincronizado.», botón habilitado y campo sin `readonly`.

Además se añadió el recuento de POST a las otras dos filas del Examples (la de
`lastStatus FAILED` y la de `404 EXTERNAL_CALENDAR_NOT_CONFIGURED`), que
inspeccionaban el cuerpo con `find` sin contar nunca las peticiones.

`pnpm --dir frontend exec vitest run src/external-calendar.test.tsx` →
**25 pruebas, 25 en verde**. Medido, no declarado: el fichero declara 24 bloques
`it(`/`it.each(` (`grep -cE "^\s*it(\.each)?[(\[]"`), uno de ellos un `it.each`
de dos filas, de donde salen las 25 que ejecuta vitest. Eran 24 antes de añadir
la prueba nueva y con los mutantes de arriba el marcador fue
`1 failed | 24 passed`.

**Pendiente asociado, anotado y no silenciado.** La línea 84 de
`e2e/external-calendar-ux-audit.spec.mjs`
(`await expect(page.getByRole("status")).not.toHaveText("Sincronizando…")`) es
una espera negativa que pasa de inmediato si el anuncio no aparece jamás. Se
sustituye al reescribir la auditoría (hallazgos 1/4/5/10/12/13), no aquí.

**Decisión de contrato registrada.** El formulario declara
`aria-busy={busy === "saving"}` y no hay equivalente para `syncing`. El Then de
@s38 exige «los controles quedan bloqueados», que es lo que se ha medido
(`disabled` y `readonly`); `aria-busy` no lo pide el contrato, así que no se ha
tocado producción para añadirlo.
