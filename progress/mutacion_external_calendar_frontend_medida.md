> # ⚠️ ACTA CADUCADA — no la uses
>
> Esta acta publica **91,32 %** y se midió con el **ámbito ancho**, el que
> puntuaba 16 mutantes de otras diez features. La cifra vigente es
> **92,54 % (620/670)** y vive en `progress/mutation_external_calendar_frontend.md`,
> junto con la lista nominal de los 50 sin matar.
>
> Se conserva porque su lista de supervivientes sigue siendo útil como historia,
> no como medida. Y porque el propio plan de cierre se puso la regla de «que no
> queden dos verdades»: borrarla dejaría sin explicar de dónde salía el 91,32 %
> que se publicó durante unas horas.
>
> Además citaba como base el commit `ec0a3b8`, **que no existe**
> (`git cat-file -t` no lo resuelve). Ese dato era falso y por eso se tacha.

# Mutación de frontend de la feature 28 — medida, 10 de septiembre de 2026

**91,32 %**, por encima del umbral de 0,80 de `harness.config.json`.

La cifra está **calculada**, no copiada de un HTML: 663 mutantes resueltos
(654 muertos + 9 por plazo agotado) sobre 726 con veredicto (los 663 más 59
supervivientes y 4 sin cobertura). Los 2 con error de ejecución quedan fuera del
denominador, que es como los cuenta Stryker. Se dice porque en la feature 30 se
publicó un 96,00 % que no estaba medido y el XML daba 95,72 %.

Campaña: `frontend/stryker.external-calendar.config.json`, 26 min 35 s.
Informe: `frontend/reports/mutation-external-calendar/mutation.json`.
~~Sobre `main` en `ec0a3b8`~~ (commit inexistente, ver aviso de arriba) (merge de los oráculos del carril, 83 -> 161 pruebas).

| Fichero | Puntuación | Muertos | Plazo | Vivos | Sin cobertura |
|---|---|---|---|---|---|
| `src/external-calendar-api.ts` | 94.48 % | 308 | 0 | 17 | 1 |
| `src/external-calendar.tsx` | 84.80 % | 212 | 0 | 35 | 3 |
| `src/App.tsx` | 100.00 % | 55 | 9 | 0 | 0 |
| `src/today-external-calendar.tsx` | 91.36 % | 74 | 0 | 7 | 0 |
| `src/workspace.tsx` | 100.00 % | 5 | 0 | 0 | 0 |

## Los 65 sin matar, nominalmente

El juez exige lista nominal, no un porcentaje. Aquí está entera: cada uno con su
fichero, línea, mutador y el reemplazo que sobrevivió. Lo que falta -y es el
trabajo siguiente- es el veredicto escrito de cada uno: muerto con prueba nueva,
o justificado con razón concreta.

| Fichero | Línea | Mutador | Estado | Reemplazo |
|---|---|---|---|---|
| `src/external-calendar-api.ts` | 66 | StringLiteral | Survived | `""` |
| `src/external-calendar-api.ts` | 67 | StringLiteral | Survived | `""` |
| `src/external-calendar-api.ts` | 73 | StringLiteral | Survived | `""` |
| `src/external-calendar-api.ts` | 78 | StringLiteral | Survived | `""` |
| `src/external-calendar-api.ts` | 79 | StringLiteral | Survived | `""` |
| `src/external-calendar-api.ts` | 94 | ConditionalExpression | Survived | `false` |
| `src/external-calendar-api.ts` | 102 | ConditionalExpression | Survived | `false` |
| `src/external-calendar-api.ts` | 140 | ConditionalExpression | Survived | `false` |
| `src/external-calendar-api.ts` | 160 | OptionalChaining | Survived | `body.code` |
| `src/external-calendar-api.ts` | 164 | OptionalChaining | Survived | `body.code` |
| `src/external-calendar-api.ts` | 167 | OptionalChaining | Survived | `body.code` |
| `src/external-calendar-api.ts` | 167 | ConditionalExpression | Survived | `true` |
| `src/external-calendar-api.ts` | 170 | ArrayDeclaration | NoCoverage | `["Stryker was here"]` |
| `src/external-calendar-api.ts` | 172 | ConditionalExpression | Survived | `true` |
| `src/external-calendar-api.ts` | 172 | LogicalOperator | Survived | `entry || typeof entry === "object"` |
| `src/external-calendar-api.ts` | 173 | ConditionalExpression | Survived | `true` |
| `src/external-calendar-api.ts` | 177 | ConditionalExpression | Survived | `true` |
| `src/external-calendar-api.ts` | 178 | ConditionalExpression | Survived | `true` |
| `src/external-calendar.tsx` | 43 | Regex | Survived | `/\.\d+Z/` |
| `src/external-calendar.tsx` | 92 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/external-calendar.tsx` | 93 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/external-calendar.tsx` | 96 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/external-calendar.tsx` | 102 | BooleanLiteral | Survived | `true` |
| `src/external-calendar.tsx` | 103 | BooleanLiteral | Survived | `true` |
| `src/external-calendar.tsx` | 110 | OptionalChaining | Survived | `inFlight.current.abort` |
| `src/external-calendar.tsx` | 114 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/external-calendar.tsx` | 118 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/external-calendar.tsx` | 122 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/external-calendar.tsx` | 131 | ConditionalExpression | Survived | `false` |
| `src/external-calendar.tsx` | 137 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/external-calendar.tsx` | 139 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/external-calendar.tsx` | 148 | BooleanLiteral | Survived | `false` |
| `src/external-calendar.tsx` | 149 | ConditionalExpression | Survived | `true` |
| `src/external-calendar.tsx` | 167 | OptionalChaining | Survived | `inFlight.current.abort` |
| `src/external-calendar.tsx` | 168 | ArrayDeclaration | Survived | `[]` |
| `src/external-calendar.tsx` | 175 | OptionalChaining | Survived | `field.focus` |
| `src/external-calendar.tsx` | 196 | CallExpression | Survived | `;` |
| `src/external-calendar.tsx` | 197 | ConditionalExpression | Survived | `false` |
| `src/external-calendar.tsx` | 210 | ConditionalExpression | Survived | `false` |
| `src/external-calendar.tsx` | 214 | ConditionalExpression | Survived | `true` |
| `src/external-calendar.tsx` | 219 | ConditionalExpression | Survived | `false` |
| `src/external-calendar.tsx` | 223 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/external-calendar.tsx` | 234 | ConditionalExpression | Survived | `false` |
| `src/external-calendar.tsx` | 235 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/external-calendar.tsx` | 236 | ConditionalExpression | Survived | `true` |
| `src/external-calendar.tsx` | 237 | CallExpression | NoCoverage | `;` |
| `src/external-calendar.tsx` | 239 | ConditionalExpression | Survived | `true` |
| `src/external-calendar.tsx` | 252 | BlockStatement | Survived | `{}` |
| `src/external-calendar.tsx` | 253 | ConditionalExpression | Survived | `false` |
| `src/external-calendar.tsx` | 253 | ConditionalExpression | Survived | `true` |
| `src/external-calendar.tsx` | 254 | StringLiteral | NoCoverage | `"Stryker was here!"` |
| `src/external-calendar.tsx` | 255 | CallExpression | NoCoverage | `;` |
| `src/external-calendar.tsx` | 257 | ConditionalExpression | Survived | `true` |
| `src/external-calendar.tsx` | 291 | ArrowFunction | Survived | `() => undefined` |
| `src/external-calendar.tsx` | 294 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/external-calendar.tsx` | 319 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/today-external-calendar.tsx` | 27 | BooleanLiteral | Survived | `true` |
| `src/today-external-calendar.tsx` | 51 | BooleanLiteral | Survived | `true` |
| `src/today-external-calendar.tsx` | 59 | BooleanLiteral | Survived | `false` |
| `src/today-external-calendar.tsx` | 61 | ConditionalExpression | Survived | `false` |
| `src/today-external-calendar.tsx` | 71 | StringLiteral | Survived | `""` |
| `src/today-external-calendar.tsx` | 76 | ObjectLiteral | RuntimeError | `{}` |
| `src/today-external-calendar.tsx` | 76 | StringLiteral | RuntimeError | `""` |
| `src/today-external-calendar.tsx` | 80 | CallExpression | Survived | `;` |
| `src/today-external-calendar.tsx` | 105 | StringLiteral | Survived | `""` |

## Dónde se concentran, que es lo que dice qué falta

- **`external-calendar.tsx` es el punto flojo**: 84,80 %, con 35 vivos y 3 sin
  cobertura. Los otros tres ficheros van de 91 a 100.
- **20 `ConditionalExpression`** entre la pantalla y la capa API: son ramas que
  ninguna prueba distingue. Es el racimo que más sube la puntuación si se cierra.
- **15 `StringLiteral`**, y esto merece atención propia: PIT no muta literales,
  pero Stryker sí, y aquí salen 15 textos que se pueden cambiar sin que caiga
  ninguna prueba. Tres de ellos son mensajes de error al usuario -`:105` de
  `today-external-calendar.tsx` tiene tres pruebas que lo ejercitan y ninguna
  afirma el texto-, así que el aviso podría decir cualquier cosa.
- **8 `ArrayDeclaration`** en la pantalla: listas que se pueden vaciar sin
  consecuencia medida.
- **2 con error de ejecución** en `today-external-calendar.tsx`: no cuentan en el
  denominador, pero conviene mirar por qué revientan en vez de dar veredicto.
