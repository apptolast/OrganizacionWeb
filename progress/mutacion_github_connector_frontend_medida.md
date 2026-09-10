# Mutación de frontend de la feature 27 — remedida, 10 de septiembre de 2026

**84.86 %** (527/621), calculado del `mutation.json`. Antes: **75,55 %**.

Sobre el SHA `d53bd11f`, máquina drenada. Con el ámbito **corregido hoy**.

## El fichero más flojo es el que se añadió hoy al ámbito

| Fichero | Puntuación | |
|---|---|---|
| `src/github-connector-draft.ts` | 75.86 % (22/29) | ❌ |
| `src/github-connector.tsx` | 83.72 % (288/344) | ✅ |
| `src/github-connector-client.ts` | 87.40 % (215/246) | ✅ |
| `src/integrations-index.tsx` | 100.00 % (2/2) | ✅ |

`github-connector-draft.ts` va al **75.86 %** (22/29), y es **producción
nueva de hoy**: el borrador en `sessionStorage` que hace que salir de la pantalla y
volver conserve el repositorio, o sea el arreglo del bloqueante del `@s41`.

Esta mañana **no estaba en la lista a mutar**. Se añadió al ámbito precisamente
porque medir sin él habría repetido el defecto que el panel cazó siete veces:
producción de la feature fuera del ámbito que la puntúa. La campaña confirma que
hacía falta: es el fichero peor probado de los cuatro, y sin añadirlo la feature
habría cerrado con ese hueco invisible.

**Queda declarado abierto**, con la misma vara que la feature 29: el `break` global
se cumple y la campaña pasa, pero el veredicto de la 28 fijó 80 % por capa y no se
mide a unas features con una vara y a otras con otra.

## La cifra sube nueve puntos

| | antes | ahora |
|---|---|---|
| Puntuación | 75,55 % | **84.86 %** |
| Mutantes | — | 621 |

Sube por los oráculos del carril, y sube **pese a** haberle añadido un fichero nuevo
que puntúa por debajo de la media. Sin ese fichero la cifra sería mejor y diría
menos.

## Los 94 sin matar, nominalmente

| Fichero | Línea | Mutador | Estado |
|---|---|---|---|
| `src/github-connector-client.ts` | 55 | StringLiteral | Survived |
| `src/github-connector-client.ts` | 66 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 73 | LogicalOperator | Survived |
| `src/github-connector-client.ts` | 73 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 73 | LogicalOperator | Survived |
| `src/github-connector-client.ts` | 74 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 75 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 86 | StringLiteral | Survived |
| `src/github-connector-client.ts` | 95 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 95 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 95 | EqualityOperator | Survived |
| `src/github-connector-client.ts` | 95 | StringLiteral | Survived |
| `src/github-connector-client.ts` | 96 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 97 | LogicalOperator | Survived |
| `src/github-connector-client.ts` | 98 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 98 | EqualityOperator | Survived |
| `src/github-connector-client.ts` | 105 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 105 | LogicalOperator | Survived |
| `src/github-connector-client.ts` | 105 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 109 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 115 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 116 | Regex | Survived |
| `src/github-connector-client.ts` | 116 | Regex | Survived |
| `src/github-connector-client.ts` | 117 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 133 | ArrowFunction | Survived |
| `src/github-connector-client.ts` | 135 | ConditionalExpression | Survived |
| `src/github-connector-client.ts` | 144 | CallExpression | Survived |
| `src/github-connector-client.ts` | 163 | CallExpression | Survived |
| `src/github-connector-client.ts` | 191 | CallExpression | Survived |
| `src/github-connector-client.ts` | 204 | CallExpression | Survived |
| `src/github-connector-client.ts` | 205 | ConditionalExpression | Survived |
| `src/github-connector-draft.ts` | 12 | StringLiteral | Survived |
| `src/github-connector-draft.ts` | 33 | BlockStatement | NoCoverage |
| `src/github-connector-draft.ts` | 34 | CallExpression | NoCoverage |
| `src/github-connector-draft.ts` | 35 | StringLiteral | NoCoverage |
| `src/github-connector-draft.ts` | 39 | ConditionalExpression | Survived |
| `src/github-connector-draft.ts` | 40 | ConditionalExpression | Survived |
| `src/github-connector-draft.ts` | 42 | CallExpression | Survived |
| `src/github-connector.tsx` | 83 | BooleanLiteral | Survived |
| `src/github-connector.tsx` | 84 | BooleanLiteral | Survived |
| `src/github-connector.tsx` | 90 | OptionalChaining | Survived |
| `src/github-connector.tsx` | 91 | BlockStatement | Survived |
| `src/github-connector.tsx` | 92 | BooleanLiteral | Survived |
| `src/github-connector.tsx` | 96 | ArrayDeclaration | Survived |
| `src/github-connector.tsx` | 105 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 105 | LogicalOperator | Survived |
| `src/github-connector.tsx` | 115 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 118 | BooleanLiteral | Survived |
| `src/github-connector.tsx` | 118 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 118 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 118 | EqualityOperator | Survived |
| `src/github-connector.tsx` | 120 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 122 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 122 | LogicalOperator | Survived |
| `src/github-connector.tsx` | 123 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 126 | CallExpression | Survived |
| `src/github-connector.tsx` | 128 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 133 | ArrayDeclaration | Survived |
| `src/github-connector.tsx` | 141 | ArrayDeclaration | Survived |
| `src/github-connector.tsx` | 148 | StringLiteral | Survived |
| `src/github-connector.tsx` | 149 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 149 | LogicalOperator | Survived |
| `src/github-connector.tsx` | 149 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 149 | LogicalOperator | Survived |
| `src/github-connector.tsx` | 158 | ArrowFunction | Survived |
| `src/github-connector.tsx` | 159 | ArrayDeclaration | Survived |
| `src/github-connector.tsx` | 178 | BooleanLiteral | Survived |
| `src/github-connector.tsx` | 179 | OptionalChaining | Survived |
| `src/github-connector.tsx` | 182 | BooleanLiteral | Survived |
| `src/github-connector.tsx` | 192 | CallExpression | Survived |
| `src/github-connector.tsx` | 193 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 196 | BooleanLiteral | Survived |
| `src/github-connector.tsx` | 197 | CallExpression | Survived |
| `src/github-connector.tsx` | 200 | ObjectLiteral | Survived |
| `src/github-connector.tsx` | 203 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 207 | CallExpression | Survived |
| `src/github-connector.tsx` | 208 | CallExpression | Survived |
| `src/github-connector.tsx` | 210 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 216 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 220 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 220 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 220 | EqualityOperator | Survived |
| `src/github-connector.tsx` | 234 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 238 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 251 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 253 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 253 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 253 | EqualityOperator | Survived |
| `src/github-connector.tsx` | 274 | StringLiteral | Survived |
| `src/github-connector.tsx` | 275 | StringLiteral | Survived |
| `src/github-connector.tsx` | 282 | BlockStatement | Survived |
| `src/github-connector.tsx` | 283 | ConditionalExpression | Survived |
| `src/github-connector.tsx` | 330 | ArrowFunction | Survived |
| `src/github-connector.tsx` | 410 | ArrowFunction | Survived |
