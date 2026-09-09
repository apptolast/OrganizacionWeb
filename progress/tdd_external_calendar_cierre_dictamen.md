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
| 8 | alta | pendiente | — |
| 9 | alta | pendiente | — |
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
