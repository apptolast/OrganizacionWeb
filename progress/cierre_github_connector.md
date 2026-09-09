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

