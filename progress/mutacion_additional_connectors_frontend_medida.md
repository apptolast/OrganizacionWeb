# Mutación de frontend de la feature 29 — medida, 10 de septiembre de 2026

**89.19 %** (660/740), calculado del `mutation.json`. Antes: **68,51 %**.

Sobre el SHA `43b979f7`, máquina drenada.

## Cierra el bloqueante B1 en el global, y el carril acertó su previsión

El bloqueante pedía pasar de 68,51 % a 80 %, o sea **85 muertes**. El carril acreditó
**153** a mano, aplicando cada mutante al fuente y restaurando, y previó **~89 %**.
Medido: **89.19 %**. La previsión era buena.

Y lo hizo **sin tocar una línea de producción**: los defectos que encontró quedaron
anotados y los arregló después otro carril, no éste.

## Pero un fichero queda por debajo del 80, y hay que decirlo

| Fichero | Puntuación | |
|---|---|---|
| `src/gitlab-connector.tsx` | 77.97 % (223/286) | ❌ |
| `src/connectors-catalog.tsx` | 87.34 % (69/79) | ✅ |
| `src/connectors-catalog-client.ts` | 97.62 % (82/84) | ✅ |
| `src/gitlab-connector-client.ts` | 98.28 % (286/291) | ✅ |

`gitlab-connector.tsx` va al **77.97 %**. El `break` de la configuración es
global y se cumple, así que la campaña pasa; pero el veredicto de la feature 28 fijó
**80 % por capa** y no sólo global, y sería incoherente medir a esta feature con una
vara distinta. **Queda declarado como abierto**, no como cerrado.

Contexto que importa antes de trabajarlo: el carril declaró **20 supervivientes
equivalentes por escrito**, uno a uno, y anotó que 29 mutantes más sólo serían
matables arreglando el {B}pending.current{B} sin abortar — arreglo que **ya se hizo**
después, en otro carril. Así que parte de esta cifra ya está caducada a la baja: hay
mutantes hoy vivos que el arreglo posterior vuelve matables.

## Los 80 sin matar, nominalmente

| Fichero | Línea | Mutador | Estado |
|---|---|---|---|
| `src/connectors-catalog-client.ts` | 93 | ArrowFunction | Survived |
| `src/connectors-catalog-client.ts` | 95 | ConditionalExpression | Survived |
| `src/connectors-catalog.tsx` | 103 | OptionalChaining | Survived |
| `src/connectors-catalog.tsx` | 104 | ArrayDeclaration | Survived |
| `src/connectors-catalog.tsx` | 112 | ConditionalExpression | Survived |
| `src/connectors-catalog.tsx` | 114 | ConditionalExpression | Survived |
| `src/connectors-catalog.tsx` | 114 | LogicalOperator | Survived |
| `src/connectors-catalog.tsx` | 116 | ConditionalExpression | Survived |
| `src/connectors-catalog.tsx` | 116 | LogicalOperator | Survived |
| `src/connectors-catalog.tsx` | 118 | StringLiteral | Survived |
| `src/connectors-catalog.tsx` | 126 | BooleanLiteral | Survived |
| `src/connectors-catalog.tsx` | 129 | ArrayDeclaration | Survived |
| `src/gitlab-connector-client.ts` | 76 | ConditionalExpression | Survived |
| `src/gitlab-connector-client.ts` | 88 | ConditionalExpression | Survived |
| `src/gitlab-connector-client.ts` | 101 | ConditionalExpression | Survived |
| `src/gitlab-connector-client.ts` | 178 | ArrowFunction | Survived |
| `src/gitlab-connector-client.ts` | 180 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 31 | StringLiteral | Survived |
| `src/gitlab-connector.tsx` | 34 | StringLiteral | Survived |
| `src/gitlab-connector.tsx` | 35 | StringLiteral | Survived |
| `src/gitlab-connector.tsx` | 49 | StringLiteral | Survived |
| `src/gitlab-connector.tsx` | 87 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 88 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 89 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 92 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 96 | OptionalChaining | Survived |
| `src/gitlab-connector.tsx` | 98 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 102 | ArrayDeclaration | Survived |
| `src/gitlab-connector.tsx` | 105 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 105 | LogicalOperator | Survived |
| `src/gitlab-connector.tsx` | 125 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 125 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 125 | EqualityOperator | Survived |
| `src/gitlab-connector.tsx` | 133 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 137 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 145 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 147 | ArrayDeclaration | Survived |
| `src/gitlab-connector.tsx` | 153 | ArrayDeclaration | Survived |
| `src/gitlab-connector.tsx` | 162 | OptionalChaining | Survived |
| `src/gitlab-connector.tsx` | 165 | BlockStatement | Survived |
| `src/gitlab-connector.tsx` | 165 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 167 | OptionalChaining | Survived |
| `src/gitlab-connector.tsx` | 172 | OptionalChaining | Survived |
| `src/gitlab-connector.tsx` | 175 | BlockStatement | Survived |
| `src/gitlab-connector.tsx` | 175 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 177 | OptionalChaining | Survived |
| `src/gitlab-connector.tsx` | 185 | StringLiteral | Survived |
| `src/gitlab-connector.tsx` | 186 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 186 | LogicalOperator | Survived |
| `src/gitlab-connector.tsx` | 186 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 186 | LogicalOperator | Survived |
| `src/gitlab-connector.tsx` | 197 | ArrayDeclaration | Survived |
| `src/gitlab-connector.tsx` | 200 | CallExpression | Survived |
| `src/gitlab-connector.tsx` | 203 | CallExpression | Survived |
| `src/gitlab-connector.tsx` | 206 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 210 | StringLiteral | NoCoverage |
| `src/gitlab-connector.tsx` | 210 | OptionalChaining | Survived |
| `src/gitlab-connector.tsx` | 213 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 217 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 219 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 226 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 239 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 243 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 245 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 247 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 260 | ObjectLiteral | Survived |
| `src/gitlab-connector.tsx` | 270 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 280 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 285 | BooleanLiteral | Survived |
| `src/gitlab-connector.tsx` | 288 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 294 | BlockStatement | Survived |
| `src/gitlab-connector.tsx` | 295 | CallExpression | Survived |
| `src/gitlab-connector.tsx` | 315 | CallExpression | Survived |
| `src/gitlab-connector.tsx` | 316 | StringLiteral | NoCoverage |
| `src/gitlab-connector.tsx` | 316 | OptionalChaining | Survived |
| `src/gitlab-connector.tsx` | 321 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 321 | LogicalOperator | Survived |
| `src/gitlab-connector.tsx` | 321 | ConditionalExpression | Survived |
| `src/gitlab-connector.tsx` | 321 | LogicalOperator | Survived |
| `src/gitlab-connector.tsx` | 351 | StringLiteral | NoCoverage |
