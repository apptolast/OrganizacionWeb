# Mutación de frontend — feature 28, calendario externo

**92.54 %** (620/670), calculado del `mutation.json` **con el ámbito ya**
**estrechado**.

Sobre el SHA `6b0ff2d1`.

## Por qué esta cifra es más baja que la anterior, y por qué es la buena

La medición previa daba **92,71 % (636/686)**. Incluía **16 mutantes que no son de esta
feature**: el tramo `App.tsx:53:12-75:38` llegaba hasta el `: null` del final del
ternario y arrastraba las ramas de otras diez features.

Y lo peor no era el ámbito: **era la guarda del arnés**. `scripts/project.test.mjs`
exigía que ese tramo terminara en `: null`, así que no vigilaba el ámbito — **lo**
**ensanchaba**. Ahora exige lo contrario: que el tramo **no** llegue al `: null` ni
toque los nombres de otras features.

|                        | antes   | ahora       |
| ---------------------- | ------- | ----------- |
| Mutantes con veredicto | 686     | **670**     |
| Puntuación             | 92,71 % | **92.54 %** |

Baja porque deja de contar trabajo ajeno. Una cifra que baja al dejar de medir lo que no
es tuyo es una cifra que mejora.

## Por fichero

| Fichero                           | Puntuación        |     |
| --------------------------------- | ----------------- | --- |
| `src/external-calendar.tsx`       | 88.00 % (220/250) | ✅  |
| `src/today-external-calendar.tsx` | 95.06 % (77/81)   | ✅  |
| `src/external-calendar-api.ts`    | 95.09 % (310/326) | ✅  |
| `src/App.tsx`                     | 100.00 % (8/8)    | ✅  |
| `src/workspace.tsx`               | 100.00 % (5/5)    | ✅  |

## Los 50 sin matar, nominalmente

| Fichero                           | Línea | Mutador               | Estado     |
| --------------------------------- | ----- | --------------------- | ---------- |
| `src/external-calendar-api.ts`    | 66    | StringLiteral         | Survived   |
| `src/external-calendar-api.ts`    | 67    | StringLiteral         | Survived   |
| `src/external-calendar-api.ts`    | 73    | StringLiteral         | Survived   |
| `src/external-calendar-api.ts`    | 78    | StringLiteral         | Survived   |
| `src/external-calendar-api.ts`    | 79    | StringLiteral         | Survived   |
| `src/external-calendar-api.ts`    | 94    | ConditionalExpression | Survived   |
| `src/external-calendar-api.ts`    | 160   | OptionalChaining      | Survived   |
| `src/external-calendar-api.ts`    | 164   | OptionalChaining      | Survived   |
| `src/external-calendar-api.ts`    | 167   | ConditionalExpression | Survived   |
| `src/external-calendar-api.ts`    | 167   | OptionalChaining      | Survived   |
| `src/external-calendar-api.ts`    | 170   | ArrayDeclaration      | NoCoverage |
| `src/external-calendar-api.ts`    | 172   | ConditionalExpression | Survived   |
| `src/external-calendar-api.ts`    | 172   | LogicalOperator       | Survived   |
| `src/external-calendar-api.ts`    | 173   | ConditionalExpression | Survived   |
| `src/external-calendar-api.ts`    | 177   | ConditionalExpression | Survived   |
| `src/external-calendar-api.ts`    | 178   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 43    | Regex                 | Survived   |
| `src/external-calendar.tsx`       | 92    | ArrayDeclaration      | Survived   |
| `src/external-calendar.tsx`       | 103   | BooleanLiteral        | Survived   |
| `src/external-calendar.tsx`       | 110   | OptionalChaining      | Survived   |
| `src/external-calendar.tsx`       | 114   | ArrayDeclaration      | Survived   |
| `src/external-calendar.tsx`       | 118   | ArrayDeclaration      | Survived   |
| `src/external-calendar.tsx`       | 122   | ArrayDeclaration      | Survived   |
| `src/external-calendar.tsx`       | 131   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 137   | ArrayDeclaration      | Survived   |
| `src/external-calendar.tsx`       | 139   | ArrayDeclaration      | Survived   |
| `src/external-calendar.tsx`       | 167   | OptionalChaining      | Survived   |
| `src/external-calendar.tsx`       | 168   | ArrayDeclaration      | Survived   |
| `src/external-calendar.tsx`       | 175   | OptionalChaining      | Survived   |
| `src/external-calendar.tsx`       | 196   | CallExpression        | Survived   |
| `src/external-calendar.tsx`       | 197   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 210   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 214   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 219   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 223   | StringLiteral         | Survived   |
| `src/external-calendar.tsx`       | 234   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 235   | StringLiteral         | Survived   |
| `src/external-calendar.tsx`       | 239   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 252   | BlockStatement        | Survived   |
| `src/external-calendar.tsx`       | 253   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 253   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 254   | StringLiteral         | NoCoverage |
| `src/external-calendar.tsx`       | 255   | CallExpression        | NoCoverage |
| `src/external-calendar.tsx`       | 257   | ConditionalExpression | Survived   |
| `src/external-calendar.tsx`       | 294   | StringLiteral         | Survived   |
| `src/external-calendar.tsx`       | 319   | StringLiteral         | Survived   |
| `src/today-external-calendar.tsx` | 51    | BooleanLiteral        | Survived   |
| `src/today-external-calendar.tsx` | 61    | ConditionalExpression | Survived   |
| `src/today-external-calendar.tsx` | 71    | StringLiteral         | Survived   |
| `src/today-external-calendar.tsx` | 80    | CallExpression        | Survived   |
