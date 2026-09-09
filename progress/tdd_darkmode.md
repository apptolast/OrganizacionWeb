# Corrección del modo oscuro — bitácora TDD

Rama `claude/darkmode` (worktree `C:/Users/vhurt/ow-worktrees/darkmode`).
Contrato de partida: `progress/darkmode_audit.md`, sección 6 «Correcciones
priorizadas». Puerto E2E propio: 18096.

**Estado: en curso.** Este archivo se completa al cerrar la verificación.

## Ciclos cerrados

| # | Hallazgo (auditoría) | Test rojo | Cambio | Commit |
|---|---|---|---|---|
| 1 | #1-#4: paneles de Hoy con blancos fijos, texto 1,04-1,41:1 | `theme-tokens.test.ts` (sin `#hex` en `today.scss`) + `e2e/today-dark.spec.mjs` (axe `color-contrast`) | `today.scss` pasa a `--panel`, `--line`, `--selection` | `02d4bd8` |
| 2 | #5: `theme-color` claro con la app oscura | `theme-color.test.tsx` (metas con `media` y repintado con `--canvas`) | dos metas en `index.html` + `paintThemeColor()` en `appearance-state.tsx` | `9c998a1` |
| 3 | #6: filtros de Historial con grises nativos | `theme-tokens.test.ts` (tokens editables en `history.scss`) | `history.scss` acota `--editable/--ink/--control-border` a `.history` | `c4c2aa5` |
| 4 | #7: `.empty-divider` con verde claro fijo | `theme-tokens.test.ts` (bloque `.empty-divider`) | `background: var(--line)` | `cb0ea07` |
| — | auditoría de partida sin versionar | — | se versiona `progress/darkmode_audit.md` | `6fb370f` |
| 5 | #8: arte decorativo con verdes fijos y sombras verdosas | 6 casos nuevos en `theme-tokens.test.ts` (6 rojos / 3 verdes antes del cambio) | `--seed-stem/-leaf/-leaf-alt/-soil` en los dos mixins; sombras `rgb(0 0 0 / x%)` | `a5c78bd` |

## Pendiente

- Corrección 6 (login recuerda el último tema en `localStorage`).
- Verificación completa: `pnpm --dir frontend test`, `tsc --noEmit`, pasada E2E
  en 18096 y capturas claras antes/después del arte decorativo.
