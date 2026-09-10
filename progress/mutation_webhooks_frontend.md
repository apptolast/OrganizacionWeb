# Mutación de frontend — feature 25, webhooks

**94.57 %** (592/626), recomputado del `mutation.json`. **Medido, no previsto.**

Este fichero existe con **este nombre exacto** porque lo exige por nombre el punto 1 de
«Cambios requeridos» de `progress/judge_webhooks_cierre.md`. Es la **bitácora canónica**
de la puerta de frontend; el acta de la campaña está en
`progress/mutacion_webhooks_frontend_medida.md` y el trabajo del carril, con sus 145
muertes acreditadas a mano, en `progress/mutacion_webhooks_frontend.md`.

## La cifra es medida, y la anterior era previsión

|                            | valor               | qué era                                      |
| -------------------------- | ------------------- | -------------------------------------------- |
| Medición anterior          | 71,40 %             | medida, y **en rojo** contra el umbral de 80 |
| Lo que declaraba el carril | ~90 % (banda 88-93) | **previsión**, rotulada como tal             |
| Esta                       | **94.57 %**         | **medida**                                   |

El carril fue honesto en rotular su número como previsión, y la medida quedó **por encima**
de su propia banda. Se deja escrito porque confundir previsión con medida es la falta que
produjo el «96,00 %» de otra feature.

## Fallo sobre las ocho guardas `if (!aborted)` — **aceptadas como equivalentes**

El carril midió **dos veces** que los mutantes de esas guardas no mueren, la segunda con la
prueba que el propio panel prescribía, y las declaró equivalentes: abortar y desmontar son
el mismo suceso en esta vista, y React ya descarta el `setState` sobre un componente
desmontado. El mecanismo de `@s38` **sí** tiene oráculo: borrarlo de las cuatro formas
posibles pone tres pruebas en rojo cada vez.

El juez de cierre aceptó el argumento **condicionado** a que la campaña real pasara el 80 %
sin contar esas guardas como matadas. **Comprobado y se sostiene:**

|                                | mutantes con veredicto | puntuación  |
| ------------------------------ | ---------------------- | ----------- |
| Contándolas como vivas         | 626                    | **94.57 %** |
| Descontándolas del denominador | 620                    | 95.48 %     |

Hay **6 mutantes vivos** en esas líneas. Aun contándolos todos en contra, la cifra
queda catorce puntos por encima del umbral: **no eran el margen**. El fallo del juez queda
firme y no revocado.

## Los 34 sin matar, nominalmente

| Fichero                  | Línea | Mutador               | Estado     |                        |
| ------------------------ | ----- | --------------------- | ---------- | ---------------------- |
| `src/webhooks-client.ts` | 67    | ConditionalExpression | Survived   |                        |
| `src/webhooks-client.ts` | 72    | ConditionalExpression | Survived   |                        |
| `src/webhooks-client.ts` | 96    | ConditionalExpression | Survived   |                        |
| `src/webhooks-client.ts` | 96    | EqualityOperator      | Survived   |                        |
| `src/webhooks-client.ts` | 109   | ConditionalExpression | Survived   |                        |
| `src/webhooks-client.ts` | 113   | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 66    | ArrowFunction         | Survived   |                        |
| `src/webhooks.tsx`       | 67    | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 86    | ArrayDeclaration      | Survived   |                        |
| `src/webhooks.tsx`       | 105   | ArrayDeclaration      | Survived   |                        |
| `src/webhooks.tsx`       | 111   | ArrayDeclaration      | Survived   |                        |
| `src/webhooks.tsx`       | 119   | ConditionalExpression | Survived   | guarda `if (!aborted)` |
| `src/webhooks.tsx`       | 122   | ConditionalExpression | Survived   | guarda `if (!aborted)` |
| `src/webhooks.tsx`       | 125   | ConditionalExpression | Survived   | guarda `if (!aborted)` |
| `src/webhooks.tsx`       | 127   | ArrayDeclaration      | Survived   |                        |
| `src/webhooks.tsx`       | 130   | ArrayDeclaration      | Survived   |                        |
| `src/webhooks.tsx`       | 151   | ConditionalExpression | Survived   | guarda `if (!aborted)` |
| `src/webhooks.tsx`       | 158   | ConditionalExpression | Survived   | guarda `if (!aborted)` |
| `src/webhooks.tsx`       | 161   | ConditionalExpression | Survived   | guarda `if (!aborted)` |
| `src/webhooks.tsx`       | 202   | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 202   | StringLiteral         | Survived   |                        |
| `src/webhooks.tsx`       | 203   | StringLiteral         | NoCoverage |                        |
| `src/webhooks.tsx`       | 216   | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 227   | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 256   | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 269   | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 273   | OptionalChaining      | Survived   |                        |
| `src/webhooks.tsx`       | 281   | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 288   | ConditionalExpression | Survived   |                        |
| `src/webhooks.tsx`       | 298   | OptionalChaining      | Survived   |                        |
| `src/webhooks.tsx`       | 303   | OptionalChaining      | Survived   |                        |
| `src/webhooks.tsx`       | 325   | StringLiteral         | Survived   |                        |
| `src/webhooks.tsx`       | 353   | OptionalChaining      | Survived   |                        |
| `src/webhooks.tsx`       | 433   | StringLiteral         | Survived   |                        |
