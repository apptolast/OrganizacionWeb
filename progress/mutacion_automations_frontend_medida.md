# Mutación de frontend de la feature 30 — medida de nuevo, 10 de septiembre de 2026

**90.29 %** (818/906), calculado del `mutation.json`, no leído de un HTML.

Sobre el SHA `7fa4e9ee`, con el árbol limpio y todos los carriles ya integrados.
Campaña de 21 min 44 s. Es la primera medición **posterior** al trabajo de los tres
carriles de la feature y a los arreglos de M2 y M12.

## La cifra BAJÓ, y está bien que baje

| | antes | ahora |
|---|---|---|
| Puntuación | 91,18 % | **90.29 %** |
| Mutantes con veredicto | 884 | 906 |
| Muertos | 806 | 818 |

Se matan **doce mutantes más** que antes. Lo que pasa es que hay **22 mutantes
nuevos**: los arreglos de M2 —el interruptor que quedaba inerte para siempre— y de M12
—el error que le robaba el foco al campo de al lado— añadieron producción, y producción
nueva es superficie nueva que mutar. Una cifra que sube después de arreglar defectos
sería la sospechosa.

## Contra los umbrales que fijó el juez

| Objetivo | Umbral | Medido | |
|---|---|---|---|
| global | 0,80 | 90.29 % | ✅ |
| `src/automations.tsx` | 0,80 | 83.52 % | ✅ |
| `src/automations-api.ts` | 0,80 | 99.46 % | ✅ |

| Fichero | Puntuación | Muertos | Plazo | Vivos | Sin cobertura |
|---|---|---|---|---|---|
| `src/automations.tsx` | 83.52 % | 436 | 0 | 82 | 4 |
| `src/App.tsx` | 100.00 % | 5 | 3 | 0 | 0 |
| `src/automations-api.ts` | 99.46 % | 369 | 0 | 2 | 0 |
| `src/workspace.tsx` | 100.00 % | 5 | 0 | 0 | 0 |

## Los 88 sin matar, nominalmente

Es la **condición 2** del veredicto final. Las líneas de aquí son las del informe NUEVO:
las del informe viejo están desplazadas por los arreglos, así que trabajar sobre aquél
sería tirar el trabajo.

| Fichero | Línea | Mutador | Estado | Reemplazo |
|---|---|---|---|---|
| `src/automations-api.ts` | 119 | ArrowFunction | Survived | `() => undefined` |
| `src/automations-api.ts` | 236 | ArrowFunction | Survived | `() => undefined` |
| `src/automations.tsx` | 100 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/automations.tsx` | 110 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 110 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/automations.tsx` | 112 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/automations.tsx` | 114 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 116 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/automations.tsx` | 118 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 118 | LogicalOperator | Survived | `rule.action.type === "CREATE_TASK" || rule.action.estimatedMinutes !==` |
| `src/automations.tsx` | 118 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 119 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 121 | StringLiteral | Survived | `"Stryker was here!"` |
| `src/automations.tsx` | 186 | BooleanLiteral | Survived | `false` |
| `src/automations.tsx` | 193 | BooleanLiteral | Survived | `true` |
| `src/automations.tsx` | 194 | OptionalChaining | Survived | `live.current.abort` |
| `src/automations.tsx` | 198 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/automations.tsx` | 202 | OptionalChaining | Survived | `live.current.abort` |
| `src/automations.tsx` | 207 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 207 | LogicalOperator | Survived | `!mounted.current && live.current !== controller` |
| `src/automations.tsx` | 207 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 211 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 211 | LogicalOperator | Survived | `!mounted.current && live.current !== controller` |
| `src/automations.tsx` | 211 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 214 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/automations.tsx` | 218 | CallExpression | Survived | `;` |
| `src/automations.tsx` | 228 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 228 | LogicalOperator | Survived | `!mounted.current && live.current !== controller` |
| `src/automations.tsx` | 228 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 233 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 233 | LogicalOperator | Survived | `mounted.current || live.current === controller` |
| `src/automations.tsx` | 233 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 235 | ArrowFunction | Survived | `() => undefined` |
| `src/automations.tsx` | 236 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/automations.tsx` | 240 | StringLiteral | Survived | `""` |
| `src/automations.tsx` | 242 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 242 | LogicalOperator | Survived | `mounted.current || "items" in page` |
| `src/automations.tsx` | 246 | ArrayDeclaration | Survived | `["Stryker was here"]` |
| `src/automations.tsx` | 249 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 250 | OptionalChaining | Survived | `document.getElementById(invalid.current).focus` |
| `src/automations.tsx` | 261 | MethodExpression | Survived | `named` |
| `src/automations.tsx` | 263 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 263 | EqualityOperator | Survived | `shown.length >= 0` |
| `src/automations.tsx` | 265 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 265 | EqualityOperator | Survived | `unshown.length >= 0` |
| `src/automations.tsx` | 273 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 273 | LogicalOperator | Survived | `!editing && saving` |
| `src/automations.tsx` | 292 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 292 | LogicalOperator | Survived | `!mounted.current && writeRequest.current !== controller` |
| `src/automations.tsx` | 292 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 295 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 296 | ArrayDeclaration | NoCoverage | `[]` |
| `src/automations.tsx` | 308 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 308 | LogicalOperator | Survived | `mounted.current || savingRequest.current === controller` |
| `src/automations.tsx` | 308 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 314 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 325 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 325 | LogicalOperator | Survived | `!mounted.current && writeRequest.current !== controller` |
| `src/automations.tsx` | 325 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 350 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 350 | LogicalOperator | Survived | `!mounted.current && writeRequest.current !== controller` |
| `src/automations.tsx` | 350 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 352 | ArrayDeclaration | NoCoverage | `["Stryker was here"]` |
| `src/automations.tsx` | 358 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 358 | LogicalOperator | Survived | `mounted.current || toggleRequest.current === controller` |
| `src/automations.tsx` | 358 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 365 | OptionalChaining | Survived | `editing?.rule.id` |
| `src/automations.tsx` | 365 | OptionalChaining | Survived | `editing.rule` |
| `src/automations.tsx` | 367 | OptionalChaining | Survived | `live.current.abort` |
| `src/automations.tsx` | 372 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 372 | LogicalOperator | Survived | `!mounted.current && live.current !== controller` |
| `src/automations.tsx` | 372 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 377 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 377 | LogicalOperator | Survived | `!mounted.current && live.current !== controller` |
| `src/automations.tsx` | 377 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 389 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 389 | LogicalOperator | Survived | `!mounted.current && runsRequest.current !== controller` |
| `src/automations.tsx` | 389 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 393 | ConditionalExpression | Survived | `false` |
| `src/automations.tsx` | 395 | ArrayDeclaration | NoCoverage | `["Stryker was here"]` |
| `src/automations.tsx` | 395 | OptionalChaining | Survived | `current.items` |
| `src/automations.tsx` | 575 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 600 | ConditionalExpression | Survived | `true` |
| `src/automations.tsx` | 602 | StringLiteral | NoCoverage | `"Stryker was here!"` |
| `src/automations.tsx` | 651 | StringLiteral | Survived | `""` |
| `src/automations.tsx` | 653 | StringLiteral | Survived | `""` |
| `src/automations.tsx` | 654 | StringLiteral | Survived | `""` |
| `src/automations.tsx` | 660 | StringLiteral | Survived | `""` |
