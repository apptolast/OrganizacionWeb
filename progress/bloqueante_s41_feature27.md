# Bloqueante @s41 y dos ámbitos de la feature 27 — 10 de septiembre de 2026

Carril `github-connector`, worktree `C:/Users/vhurt/ow-worktrees/github-connector`,
rama `claude/github-connector` reseteada a `origin/main` (`1bc1dca3`).

> Nota de arranque: la rama traía seis commits de carriles anteriores
> (`automations` @s17/@s22 y `fix(29)`) que el reset descartaba. Antes de mover el
> puntero los dejé a salvo en la rama local `backup/pre-s41-lane` (`bcf176f6`).
> No se ha perdido nada.

Encargo: tres puntos del `progress/panel_precierre_27_30.md`. Estado final:

| Punto | Estado |
|---|---|
| 1. `@s41` fila 1: el repositorio no sobrevive al remontaje | **cerrado** (oráculo + arreglo) |
| 2. `GithubIssueConnections` fuera de todo `targetClasses` | **delegado** (el fichero no es mío) |
| 3. Espacios Unicode de `@s14`/`@s15` | **ya estaba cubierto**: el rojo lo acredita |

---

## 1. `@s41`, primera fila — el bloqueante real

### La cláusula

`features/github_connector.feature:525`:

> `| navego a otra pantalla y vuelvo con la misma sesión | repository conserva "octocat/Hello-World" y el campo token está vacío |`

Dos obligaciones. La del token se cumplía y estaba medida. La del repositorio
**ni se cumplía ni la medía nadie**, y la prueba que la nombra en su título
—`@s41 forgets the token but keeps the repository when the screen is remounted`—
tras el `unmount()` + `render()` sólo tenía dos aserciones, las dos del token.

### ROJO 1 — el oráculo que faltaba

Añadido a `frontend/src/github-connector.test.tsx`:

```
expect(screen.getByLabelText(/repositorio/i)).toHaveValue("octocat/Hello-World");
```

Sin tocar producción, `pnpm --dir frontend exec vitest run src/github-connector.test.tsx`:

```
FAIL  src/github-connector.test.tsx > @s41 forgets the token but keeps the repository...
Error: expect(element).toHaveValue(octocat/Hello-World)
Expected the element to have value:
  octocat/Hello-World
Received:

  ❯ src/github-connector.test.tsx:557:49
Tests  1 failed | 61 passed (62)
```

Ese rojo **es el defecto**: no hubo que romper nada para provocarlo. Causa:
`App.tsx:90-99` renderiza `<GithubConnector>` dentro de la cadena de ternarios
por ruta, así que salir de `/integraciones/github` lo desmonta, y
`github-connector.tsx:61` volvía a nacer con `useState("")`.

### El mecanismo elegido, y por qué

Descartados:

- **Estado elevado a `App.tsx`.** Es la solución "natural" en React, pero
  `frontend/src/App.tsx` es del orquestador esta noche
  (`progress/carriles/REPARTO_NOCHE.md`, regla 3) y la regla dice pedirlo, no
  tocarlo. Además obliga a que el contenedor conozca un detalle interno del
  formulario del conector.
- **Variable de módulo en memoria.** Sobrevive al remontaje, sí, pero no muere
  con la pestaña ni se puede inspeccionar ni limpiar; y en la suite se filtraría
  de una prueba a la siguiente sin que `sessionStorage.clear()` pueda evitarlo.
- **Guardar el formulario entero.** Prohibido explícitamente: la misma fila
  exige que el token quede vacío, y `@s34` que no aparezca en ningún almacén.

Elegido: **`sessionStorage`, con alcance de propietario y sólo el repositorio**,
en un módulo nuevo `frontend/src/github-connector-draft.ts` calcado de
`frontend/src/import-data-intent.ts`, que es lo que el proyecto ya usa para el
caso equivalente (un dato de interfaz que debe sobrevivir dentro de la sesión y
no heredarse entre propietarios: clave única, validación defensiva al leer,
borrado cuando el `owner` no casa).

Dos razones para `sessionStorage` y no otra cosa:

1. El contrato dice literalmente **«con la misma sesión»**, y `sessionStorage`
   es exactamente eso: muere con la pestaña.
2. El repositorio es un nombre **público** (`octocat/Hello-World`), el mismo que
   ya se pinta en el `<dd>` de la conexión y viaja en cada recibo. El secreto es
   el token, y el módulo **no tiene forma de recibirlo**: su firma es
   `saveRepositoryDraft(owner, repository)`.

Reglas del borrador:

- Se escribe en cada pulsación, por el único camino `editRepository(value)`.
- Un valor vacío **borra** la clave: un borrador vacío no es un borrador. Por eso
  conectar con éxito (`submitConnection`) y desconectar (`confirmDisconnect`),
  que ya hacían `setRepository("")`, ahora limpian también la sesión sin ninguna
  línea extra.
- Al leer, si el `owner` no casa, se borra y se devuelve `""`: iniciar sesión
  como otra persona en la misma pestaña no hereda nada (`@s41`, fila 2).

### ROJO 2 — que el arreglo no se pase de listo

El riesgo del arreglo es justo el que el encargo señala: que alguien "arregle"
esto guardando el formulario entero. Oráculo añadido a la misma prueba:

```
expect(JSON.stringify(sessionStorage)).toContain("octocat/Hello-World");
expect(JSON.stringify(sessionStorage)).not.toContain("ghp_secreto123");
expect(JSON.stringify(localStorage)).not.toContain("ghp_secreto123");
```

**Acreditación (dos intentos, el primero no valía):**

- *Primer intento*: hacer que el `onChange` del token llamara a
  `saveRepositoryDraft`. Rojo, sí, pero en la línea **557** (`innerHTML` no
  contiene el token), porque el token acababa pintado en el campo Repositorio.
  Ese rojo lo daba una aserción vieja, no la mía: **no acredita nada**.
- *Segundo intento*, quirúrgico — el `onChange` del token escribe una clave
  aparte en la sesión (`organizationweb.github.form.v1`), sin ensuciar ningún
  campo. Dos rojos, los dos correctos:

```
FAIL > @s41 forgets the token but keeps the repository when the screen is remounted
AssertionError: expected '{"organizationweb.github.repository.v…' not to contain 'ghp_secreto123'
Received: "{"organizationweb.github.repository.v1":"{\"owner\":\"owner\",\"repository\":\"octocat/Hello-World\"}",
           "organizationweb.github.form.v1":"{\"owner\":\"owner\",\"token\":\"ghp_secreto123\"}"}"
  ❯ src/github-connector.test.tsx:564:46

FAIL > @s37 keeps the token field a password that never autocompletes nor survives a send
AssertionError: expected 1 to be +0
  ❯ src/github-connector.test.tsx:241:33   (expect(sessionStorage.length).toBe(0))
```

El segundo es **la prueba de `@s34` que el encargo pedía ejecutar**: salta sola
en cuanto algo se queda en el almacén tras conectar. Producción restaurada.

### VERDE

```
pnpm --dir frontend exec vitest run src/github-connector.test.tsx
Test Files  1 passed (1)
     Tests  62 passed (62)
```

`pnpm --dir frontend exec tsc --noEmit` sin salida; `prettier --check` conforme.

### Ficheros

- `frontend/src/github-connector-draft.ts` **(nuevo)**
- `frontend/src/github-connector.tsx` — `owner` baja a la pantalla, `useState`
  inicializado desde el borrador, `editRepository()` como único camino.
- `frontend/src/github-connector.test.tsx` — cuatro aserciones nuevas y
  `sessionStorage.clear()` en el `beforeEach` (precedente: `calendar.test.tsx:77`),
  para que el borrador no se filtre entre pruebas.

Ninguno de ellos es compartido: `frontend/src/github-connector*` es de este
carril (REPARTO_NOCHE, regla 3). **`App.tsx` no se ha tocado.**

### Fuera de ámbito, anotado y no ejecutado

`e2e/github-connector.spec.mjs` no se ha corrido: levantar la pila no entraba en
el encargo y las otras tres filas de `@s41` viven ahí. Merece una pasada del
orquestador antes de cerrar.
