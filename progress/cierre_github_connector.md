# Cierre de la feature 27 `github_connector` — noche del 9 al 10 de septiembre de 2026

Carril `claude/github-connector`, worktree `C:/Users/vhurt/ow-worktrees/github-connector`,
`E2E_WEB_PORT=18096`, migración reservada `V33`.

Encargo: (1) la regresión que deja la CI en rojo — cuatro pruebas de E2E que no
ven ningún contador en «Resultado de la importación»; (2) la puerta de mutación
de frontend, 75,55 % contra un umbral de 80.

---

## 1. La regresión de los contadores

### Diagnóstico

**Síntoma.** `e2e/github-connector.spec.mjs:185`, `:212`, `:375` y
`e2e/github-connector-native-zoom.spec.mjs:37` esperan «Creadas 1» / «Creadas 3»
en la región `aria-label="Resultado de la importación"` y no aparece nada.

**Causa, encontrada leyendo las dos orillas de la frontera HTTP y confirmada
ejecutando.** El commit `a347936` de la feature 29 («un solo caso de uso de
importación para GitHub y GitLab») cambió la forma del recibo que emite
`GithubConnectorController.ImportResponse`:

| | Antes (feature 27) | Hoy (feature 29, en `main`) |
|---|---|---|
| Campos | 11 | 12 |
| Identidad del origen | `repository: "octocat/Hello-World"` | `source: "github"` + `projectPath: "octocat/Hello-World"` |

`backend/.../GithubConnectorController.java:318-345` — el `record ImportResponse`
lleva hoy `source` y `projectPath`.

El **cliente de frontend no se enteró**.
`frontend/src/github-connector-client.ts:7-8` fija

    const RECEIPT_FIELDS =
      "id projectId repository status created skipped failed truncated errorCode startedAt finishedAt";

y `decodeReceipt` (`:72-94`) empieza por `!exact(value, RECEIPT_FIELDS)`. Como
`exact` exige el juego de claves **exacto**, el recibo real —que trae `source` y
`projectPath` y no trae `repository`— nunca pasa el filtro: `decodeReceipt`
lanza `Error("Confirmación incompatible")`, `startGithubImport` propaga, el
componente cae en la rama `catch`, deja `summary` en `null` y **la región de
resultado no se pinta**. De ahí que no haya «Creadas 1»: no es que el contador
valga cero, es que la sección entera no existe.

Encaja con todo lo comprobado antes de mi turno: las unitarias de importación
pasan (miden el caso de uso, no la frontera), y el arreglo del `TRUNCATE` es
posterior e inocente.

### Quién manda: el contrato de la 29 gana, y la 27 se alinea

No es un defecto del backend. `features/additional_connectors.feature:242` (@s20)
dice, sobre una importación de GitHub y otra de GitLab del mismo propietario:

    And los dos recibos tienen las mismas claves y difieren únicamente en id,
    source, projectId, projectPath e instantes

Es decir: la 29 **exige por contrato** que el recibo de GitHub lleve `source` y
`projectPath`. Las dos filas de `features/github_connector.feature` que aún
hablaban de «once campos» con `repository` (`:146` y `:165`) son las que han
caducado. Se alinean en este carril, con esta razón escrita (REGLAS §8): el
contrato manda sobre el código, pero cuando dos contratos se contradicen gana el
posterior y el anterior se corrige explícitamente, no se silencia.

El arreglo va, por tanto, **en el cliente de frontend**, que es fichero de este
carril.

### Ciclo Rojo → Verde

**ROJO, acreditado por ejecución.** Dos pruebas nuevas en
`frontend/src/github-connector-client.test.ts`:

- `@s12 decodes the receipt the HTTP boundary really emits, with source and projectPath`
- `@s12 a receipt still shaped like the old one no longer passes`

El juego de claves de la primera está escrito **a mano**, no derivado del
fixture, para que sea un oráculo independiente de la frontera. Ejecutadas
contra la producción de entonces:

    × @s12 decodes the receipt the HTTP boundary really emits… 7ms
      Error: Confirmación incompatible
    × @s12 a receipt still shaped like the old one no longer passes 6ms
      AssertionError: promise resolved "{ …(11) }" instead of rejecting

Es exactamente el motivo del fallo de E2E: el recibo real no pasa el filtro y
`summary` se queda en `null`.

**Evidencia de que el backend emite hoy esa forma, ejecutada y no leída.**
`GithubConnectorApiTest` (`@WebMvcTest`, sin contenedor) — **47/47 en verde**,
y entre ellas las de `:192` y `:408`, que afirman
`containsExactlyInAnyOrder(RECEIPT_FIELDS)` con `RECEIPT_FIELDS =
{id, source, projectId, projectPath, …}`.

**VERDE.** Tres cambios mínimos en `frontend/src/github-connector-client.ts`:

1. `RECEIPT_FIELDS` pasa a las doce claves reales.
2. El tipo `GithubImportReceipt` cambia `repository: string` por
   `source: string` y `projectPath: string`.
3. `decodeReceipt` sustituye `typeof value.repository !== "string" ||
   !value.repository` por `value.source !== GITHUB || !nonEmpty(value.projectPath)`.
   Se exige `source === "github"` y no un origen cualquiera: este cliente sólo
   habla con el extremo de GitHub, y un recibo de GitLab llegando aquí es una
   confirmación incompatible, no un caso a tolerar.

El componente **no se toca**: nunca leía `receipt.repository` (sólo
`connection.repository`, que sigue existiendo en `ConnectionResponse`).

**Verificación.**

| Ejecución | Resultado |
| --- | --- |
| `vitest run src/github-connector-client.test.ts` | 46/46 verde |
| `vitest run src/github-connector.test.tsx src/github-connector-routing.test.tsx` | 35/35 verde |
| `tsc --noEmit` | sin errores |
| `E2E_WEB_PORT=18096 pnpm test:e2e -- e2e/github-connector.spec.mjs` | **15/15 verde**, 1,8 min |

Las cuatro pruebas que el encargo daba por rotas están entre ellas: `:185`
(importando de verdad), `:212` (reimportar omite lo ya importado), `:375`
(desconectar conserva lo importado) y — pendiente de su propia ejecución — la
del zoom nativo.

### Contrato alineado

`features/github_connector.feature`:

- `:146` «con sus once campos» → «con sus **doce** campos».
- `:165` «exactamente id, projectId, repository, …» → «exactamente id,
  **source**, projectId, **projectPath**, …», y `:166` añade `source es
  "github"` y `projectPath es "octocat/Hello-World"` como aserciones nuevas: al
  quitar `repository` del recibo, sin esta fila el contrato dejaría de decir en
  qué repositorio se importó.

### Verificación final del punto 1

Los **dieciséis** recorridos de E2E de la feature, sobre la pila real en el
puerto 18096:

| Fichero | Resultado |
| --- | --- |
| `e2e/github-connector.spec.mjs` | **15 passed (1,8 min)** |
| `e2e/github-connector-native-zoom.spec.mjs` | **1 passed (16,2 s)**, con `zoom === 2` |

Commit `a1b0d20`.

---

## 2. La puerta de mutación de frontend

Punto de partida medido por el coordinador, sobre
`frontend/reports/mutation-github-connector/mutation.json`: **75,55 %** con
umbral 80. Recuento exacto, releído del informe por mí y no heredado:

| Fichero | Mutantes | Muertos | Supervivientes |
| --- | ---: | ---: | ---: |
| `src/github-connector-client.ts` | 246 | 210 | 36 |
| `src/github-connector.tsx` | 341 | 234 | **107** |
| `src/integrations-index.tsx` | 2 | 1 | 1 |
| **Total** | **589** | **445** | **144** |

445 / 589 = 75,55 %. Cuadra.

**Producción intacta.** `git diff` sobre `github-connector.tsx` e
`integrations-index.tsx` en este ciclo: vacío. Lo único que ha cambiado son
ficheros de prueba. Ninguna prueba existente se ha relajado ni borrado.

### Una corrección al parte anterior, que se descubrió ejecutando

`progress/mutacion_github_connector_supervivientes.md` dejaba escrita una receta
para el superviviente de la línea 266: «el `ConditionalExpression -> true` de la
266 es el ternario `error instanceof ConnectorError ? … : …`, y se mata con
`vi.mock` del módulo cliente». **Es falso, y lo dice el propio informe.** El
mutante ocupa las columnas `266:11-266:27`, dieciséis caracteres, que son
exactamente `live(controller)`: la **guarda de vigencia** del `catch`, no el
ternario. La receta del `vi.mock` habría añadido una prueba que no mata nada.

Lo que sí lo mata es ejercer la guarda: una desconexión **relevada** por una
importación posterior, que falla tarde y no debe hablar. Verificado rompiendo
la producción a mano (`if (live(controller))` → `if (true)`): la prueba nueva
falla.

Regla del reparto §5 aplicada al pie: gana la ejecución.

### Los racimos atacados, y qué mata cada uno

Los 107 supervivientes se reparten en **82 líneas**: no hay un racimo único
gigante, hay familias. Se han atacado nueve, con 24 pruebas nuevas en
`github-connector.test.tsx` y 1 en `github-connector-routing.test.tsx`:

1. **El escuchador de Escape (153-160), que el juez exigió expresamente** — 5
   mutantes. Cuatro pruebas: que no escucha si no hay confirmación abierta, que
   se desregistra al cerrarse, que sólo Escape la cierra y que el Escape queda
   consumido (`defaultPrevented`).
   El oráculo de las dos primeras es indirecto y hay que explicarlo: llamar a
   `cancelDisconnect()` de más no se nota en `confirming` —React abandona el
   repintado al escribir el mismo valor—, pero **sí deja pedido un movimiento de
   foco** en `focusDisconnect.current`, que el siguiente repintado ejecuta. Las
   pruebas provocan ese repintado con un `selectOptions` y afirman que el foco
   no se mueve. Es determinista, a diferencia de mirar `confirming`.
2. **La tabla de mensajes (37-47)** — 4 mutantes. Una tabla de cinco códigos
   contra su texto exacto.
3. **La región de error de importación (416-424)** — 6 mutantes: que un token
   rechazado no se repite ahí, que sólo una conexión inválida ofrece
   «Reconectar» —y no dos veces—, y que sólo una importación en curso ofrece
   «Consultar estado».
4. **El hueco del mensaje del formulario (405-407)** — 2 mutantes: que es
   `role="alert"` cuando hay error y que está **vacío y sin papel** cuando no.
5. **La región `aria-live` (456)** — 1 mutante: nace callada.
6. **El resumen heredado del último recibo (474) y su enlace (478)** — 4
   mutantes: una importación en curso no adelanta contadores; el enlace nombra
   el proyecto **del recibo**, no el primero de la lista; y un proyecto que ya
   no está en la lista enseña su identificador en vez de reventar.
7. **El arranque de la pantalla (60, 66, 80, 132, 284-285)** — 6 mutantes: no
   se enseña el formulario mientras se consulta; sin lista de proyectos el
   selector no ofrece ninguna opción; el foco arranca en el encabezado; el
   conector deshabilitado deja de pedir la lista; `main` y `h1` llevan
   `tabindex="-1"`.
8. **La importación: guardas y limpieza (207-236)** — 7 mutantes: sin proyecto
   no se lanza importación; tras un token rechazado el formulario vuelve a estar
   operativo; una importación nueva borra resultado y error de la anterior; y un
   fallo de almacenamiento sin contadores los da por cero sin hablar de
   truncamiento.
9. **La desconexión: qué limpia (260-261) y a quién obedece (258, 266-271)** — 8
   mutantes, con las dos pruebas de relevo descritas arriba.

Y 1 mutante más en `integrations-index.tsx:9` (`tabIndex={-1}`), cubierto desde
`github-connector-routing.test.tsx`, que ya renderiza `/integraciones`.

### Verificación: 44 de 44, rompiendo la producción a mano

No se ha ejecutado Stryker (regla 4 del reparto). En su lugar,
`scripts/verificar-mutantes-github-connector.mjs` **aplica cada mutante
superviviente al fichero de producción, ejecuta la suite y restaura**. El
resultado completo, prueba a prueba, queda en
`progress/verificacion_mutantes_github_connector.json`.

    TOTAL: 43 mueren de 43

El de `integrations-index.tsx` se comprobó aparte, a mano:
`tabIndex={-1}` → `tabIndex={+1}` hace caer la prueba nueva y sólo esa.

Los cuatro mutantes **nuevos** que introduce el arreglo del punto 1 en
`github-connector-client.ts` también se comprobaron uno a uno:

| Mutante | Pruebas que caen |
| --- | ---: |
| `value.source !== GITHUB` → `===` | 8 |
| `const GITHUB = "github"` → `""` | 8 |
| `value.source !== GITHUB \|\|` → `false \|\|` | 1 |
| `!nonEmpty(value.projectPath) \|\|` → `false \|\|` | 1 |

Ningún oráculo nuevo es incapaz de fallar.

### Previsión para la campaña del orquestador

Con el denominador prácticamente igual —el arreglo del cliente quita cuatro
puntos mutables (`typeof value.repository !== "string"` y `!value.repository`) y
pone otros tantos—:

| | Mutantes | Muertos | Puntuación |
| --- | ---: | ---: | ---: |
| Antes | 589 | 445 | 75,55 % |
| **Previsión** | ≈589 | **≈489** | **≈83,0 %** |

Es decir: **44 muertes nuevas**, 17 por encima de las 27 que hacían falta para
cruzar el 80. El margen es deliberado, porque una previsión no es una medida.

**Supervivientes que quedan y por qué no se atacan** (para que la campaña no
sorprenda): unos 63 en `github-connector.tsx` y 36 en el cliente. Entre los que
quedan hay varios que **creo equivalentes**, y lo dejo escrito para que nadie
gaste el turno en ellos:

- `90`, `121`, `129`, `147` — `ArrayDeclaration` sobre las listas de
  dependencias de `useCallback`/`useEffect`. `["Stryker was here"]` se compara
  igual a sí misma en cada repintado, así que el efecto no se vuelve a ejecutar:
  se comporta como `[]`.
- `137` (los cuatro) — la guarda `aborted || !mounted || !("items" in page)`
  vive dentro de un `try/catch` que se traga cualquier excepción, así que el
  camino sano (salir) y el mutado (reventar al filtrar) dejan la pantalla
  idéntica.
- `262`, `263` — `setRepository("")` y `setToken("")` de la desconexión. Ambos
  estados ya valen `""` en todo camino que llegue al botón «Desconectar»: la
  sección con ese botón no se pinta mientras el formulario está abierto.
- `270`, `271 -> false`, `204`, `239` — no vaciar `pending.current` en el
  `finally` no se observa: la siguiente operación lo sobrescribe y abortar un
  controlador ya terminado es inocuo.
- `77`, `78` — `useRef` inicial que el efecto de montaje reescribe acto seguido.

