# Propuesta Stryker20

Pendiente de revisión root. No configuración modificada ni campaña iniciada.
Se reutiliza el patrón de revisión semanal: Vitest, perTest, ocho workers,
umbrales high90/low80/break80 y reportes propios. Sin plugins adicionales.

## Alcance concreto

Siete fuentes, nueve selectores propuestos para
`frontend/stryker.appearance.config.json`:

```json
[
  "src/appearance-api.ts",
  "src/appearance-state.tsx",
  "src/appearance.tsx",
  "src/App.tsx:19:8-19:44",
  "src/App.tsx:27:8-39:26",
  "src/App.tsx:42:7-74:7",
  "src/workspace.tsx:35:10-40:22",
  "src/session-gate.tsx:32:2-51:6",
  "src/use-session.ts:177:0-195:1"
]
```

Los tres archivos nuevos se incluyen enteros: decoder, contraste, errores,
captura de intención, abortos, Provider, incertidumbre compartida, tema
efectivo, recuperación y formulario. En compartidos se incluyen nodos
completos: declaración appearance, las dos decisiones de ruta/section,
RouteLink Apariencia, condición de autenticación y rama que monta Provider,
y reconocimiento de rutas privadas para retorno al login.

Selección derivada del AST TypeScript real (`14262d`), líneas desde1 y
columnas desde0, fin exclusivo. Se contrastó el diff contra0202129:
imports/tipos no mutables aparte, todos los deltas lógicos20 están dentro.
Los ternarios y función de rutas incluyen ramas heredadas: sus mutantes
adicionales se inventariarán como compartidos, no se ocultarán. No se
selecciona todo el login, creación de proyectos u otras pantallas sin cambios.
Un primer comando AST falló por quoting antes de leer/editar; el comando
por stdin produjo los rangos anteriores. No se atribuye ejecución Stryker.

## Candidatos y aislamiento

`vitest.configFile = "vite.config.ts"` mantiene todos los candidatos
`src/**/*.test.{ts,tsx}`; sin related/testNamePattern ni reducción al fichero
appearance. Esto incluye auth/App y los fixtures legacy revisados.
`coverageAnalysis = "perTest"` y `concurrency = 8`, iguales al patrón19.
Reporters clear-text/json/html; destinos
`reports/mutation-appearance/mutation.json` y mutation.html;
tempDirName `.stryker-tmp-appearance`. Conservar ignorePatterns heredado
`.stryker-tmp-availability-replay`; añadir sólo el temp nuevo a gitignore
si el patrón general no lo cubre. Nunca borrar temporales protegidos.

El selector permite medir las decisiones y lógica TS/TSX de20. No mide
SCSS, geometría, contraste de cada fondo renderizado, acceso remoto,
persistencia PG o eventos: siguen sus gates propios. No se presume score,
duración ni equivalencia de supervivientes. Guardar todos los estados y
denominador original, hashes de fuentes/tests/config antes/después.

## Dispatcher y TDD posterior a aprobación

Target nuevo explícito `appearance-frontend` en scripts/project.mjs:
`node scripts/project.mjs mutate appearance-frontend` delega a
`pnpm --dir frontend exec stryker run stryker.appearance.config.json`.
El harness existente ya interpola `{{target}}`; no hace falta editarlo ni
añadir combinado o cambiar packageManager. Backend20 permanece separado.

Primer oráculo Node: capture() del dispatcher debe resolver ese target y
el comando/cwd exactos, RED real antes del registro, mínimo GREEN después.
Segundo: contrato de configuración con nueve selectores, umbral80,
perTest, ocho workers, todos candidatos y reportes separados; comprobar
que los rangos contienen los nodos previstos contra fuentes del freeze.
Regresión Node del arnés y JSON/formato, sin ejecutar mutantes. Si B mueve
líneas antes del freeze, recalcular rangos por AST conservando los mismos
nodos y presentar el contenido final a root antes de campaña.
