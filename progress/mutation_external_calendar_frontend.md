# Mutación de frontend — feature 28, calendario externo

**92.71 %** (636/686), calculado del `mutation.json`.

Sobre el SHA `9b9c2dca`, máquina drenada, con el **ámbito ya corregido**.

## Por qué se remidió: el ámbito puntuaba código de otras features

El juez señaló que los tramos de `App.tsx` y `workspace.tsx` de este ámbito eran
demasiado anchos — `App.tsx:65:16-87:42` y `:102:10-161:7` abarcaban ramas de otras
diez features. Los rangos se recalcularon **derivándolos del texto** que deben cubrir, no
escribiéndolos a mano, y la retirada de tres features los desplazó además.

El efecto se ve en el número de mutantes que aportan los ficheros compartidos:

|                       | antes   | ahora       |
| --------------------- | ------- | ----------- |
| Mutantes de `App.tsx` | 64      | **24**      |
| Puntuación global     | 91,32 % | **92.71 %** |

Sube, y sube midiendo **menos código ajeno**: la puntuación anterior se apoyaba en 40
mutantes de ramas que no son de esta feature.

## Todos los ficheros pasan el listón

| Fichero                           | Puntuación        |     |
| --------------------------------- | ----------------- | --- |
| `src/external-calendar.tsx`       | 88.00 % (220/250) | ✅  |
| `src/today-external-calendar.tsx` | 95.06 % (77/81)   | ✅  |
| `src/external-calendar-api.ts`    | 95.09 % (310/326) | ✅  |
| `src/App.tsx`                     | 100.00 % (24/24)  | ✅  |
| `src/workspace.tsx`               | 100.00 % (5/5)    | ✅  |

## Los 52 sin matar, nominalmente

| Fichero                           | Línea | Mutador               | Estado       |
| --------------------------------- | ----- | --------------------- | ------------ |
| `src/external-calendar-api.ts`    | 66    | StringLiteral         | Survived     |
| `src/external-calendar-api.ts`    | 67    | StringLiteral         | Survived     |
| `src/external-calendar-api.ts`    | 73    | StringLiteral         | Survived     |
| `src/external-calendar-api.ts`    | 78    | StringLiteral         | Survived     |
| `src/external-calendar-api.ts`    | 79    | StringLiteral         | Survived     |
| `src/external-calendar-api.ts`    | 94    | ConditionalExpression | Survived     |
| `src/external-calendar-api.ts`    | 160   | OptionalChaining      | Survived     |
| `src/external-calendar-api.ts`    | 164   | OptionalChaining      | Survived     |
| `src/external-calendar-api.ts`    | 167   | ConditionalExpression | Survived     |
| `src/external-calendar-api.ts`    | 167   | OptionalChaining      | Survived     |
| `src/external-calendar-api.ts`    | 170   | ArrayDeclaration      | NoCoverage   |
| `src/external-calendar-api.ts`    | 172   | ConditionalExpression | Survived     |
| `src/external-calendar-api.ts`    | 172   | LogicalOperator       | Survived     |
| `src/external-calendar-api.ts`    | 173   | ConditionalExpression | Survived     |
| `src/external-calendar-api.ts`    | 177   | ConditionalExpression | Survived     |
| `src/external-calendar-api.ts`    | 178   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 43    | Regex                 | Survived     |
| `src/external-calendar.tsx`       | 92    | ArrayDeclaration      | Survived     |
| `src/external-calendar.tsx`       | 103   | BooleanLiteral        | Survived     |
| `src/external-calendar.tsx`       | 110   | OptionalChaining      | Survived     |
| `src/external-calendar.tsx`       | 114   | ArrayDeclaration      | Survived     |
| `src/external-calendar.tsx`       | 118   | ArrayDeclaration      | Survived     |
| `src/external-calendar.tsx`       | 122   | ArrayDeclaration      | Survived     |
| `src/external-calendar.tsx`       | 131   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 137   | ArrayDeclaration      | Survived     |
| `src/external-calendar.tsx`       | 139   | ArrayDeclaration      | Survived     |
| `src/external-calendar.tsx`       | 167   | OptionalChaining      | Survived     |
| `src/external-calendar.tsx`       | 168   | ArrayDeclaration      | Survived     |
| `src/external-calendar.tsx`       | 175   | OptionalChaining      | Survived     |
| `src/external-calendar.tsx`       | 196   | CallExpression        | Survived     |
| `src/external-calendar.tsx`       | 197   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 210   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 214   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 219   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 223   | StringLiteral         | Survived     |
| `src/external-calendar.tsx`       | 234   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 235   | StringLiteral         | Survived     |
| `src/external-calendar.tsx`       | 239   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 252   | BlockStatement        | Survived     |
| `src/external-calendar.tsx`       | 253   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 253   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 254   | StringLiteral         | NoCoverage   |
| `src/external-calendar.tsx`       | 255   | CallExpression        | NoCoverage   |
| `src/external-calendar.tsx`       | 257   | ConditionalExpression | Survived     |
| `src/external-calendar.tsx`       | 294   | StringLiteral         | Survived     |
| `src/external-calendar.tsx`       | 319   | StringLiteral         | Survived     |
| `src/today-external-calendar.tsx` | 51    | BooleanLiteral        | Survived     |
| `src/today-external-calendar.tsx` | 61    | ConditionalExpression | Survived     |
| `src/today-external-calendar.tsx` | 71    | StringLiteral         | Survived     |
| `src/today-external-calendar.tsx` | 76    | ObjectLiteral         | RuntimeError |
| `src/today-external-calendar.tsx` | 76    | StringLiteral         | RuntimeError |
| `src/today-external-calendar.tsx` | 80    | CallExpression        | Survived     |
