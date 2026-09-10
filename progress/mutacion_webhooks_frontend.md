# Feature 25 · webhooks — cerrar la puerta de mutación del frontend

Medida de partida (10 de septiembre, campaña del orquestador):

| Fichero | Puntuación | Vivos | Sin cobertura | Total |
|---|---|---|---|---|
| `src/webhooks-client.ts` | 74,13 % | 69 | 5 | 286 |
| `src/webhooks.tsx` | 68,97 % | 82 | 17 | 319 |
| **Total** | **71,40 %** | **151** | **22** | **605** |

Umbral 80 %. Muertos hoy: 432 de 605. Para 80 % hacen falta 484, es decir
**52 muertes más**.

Regla de la noche: **no se toca producción para matar mutantes**, no se relaja
ni se borra ninguna prueba existente. Lo que falta son aserciones. Las
excepciones son los defectos de producto, que sí se arreglan y se anotan aquí.

## Cómo se acredita cada racimo

No se ejecuta Stryker (20 min, y hay otra campaña en curso). Cada mutante se
aplica **al fichero de producción real** con
`scripts/verificar-mutantes-webhooks.mjs`, se ejecuta la suite del fichero, se
comprueba el rojo y se restaura. El script termina siempre dejando
`git diff` vacío sobre producción.

---

## Racimo 1 — el catálogo de `eventTypes` en `decodeEndpoint`

**Causa común.** La expresión de `webhooks-client.ts:93-99` codifica **tres**
reglas distintas del contrato (@s3): que todo tipo esté en el catálogo, que no
haya repetidos y que lleguen en orden de catálogo. Todas las pruebas existentes
usan `eventTypes: ["TaskCreated.v1"]`, una lista de **un solo elemento**. Con
una lista de uno, `index > 0` nunca se cumple, así que **la mitad de la
expresión no se ejecuta jamás** (los tres mutantes «sin cobertura» de la línea
97) y `.some()` y `.every()` son indistinguibles.

**Ocho pruebas nuevas** en `webhooks-client.test.ts`, todas sobre listas de dos
o más tipos:

| Prueba | Qué separa |
|---|---|
| acepta los doce en orden de catálogo | el orden correcto no se rechaza |
| acepta dos tipos en orden | idem, caso mínimo |
| rechaza dos conocidos fuera de orden | la regla de orden existe |
| rechaza el mismo tipo repetido | `>=` y no `>` (el repetido empata) |
| rechaza un tipo fuera del catálogo, solo | la rama de catálogo en índice 0 |
| rechaza un desconocido escondido tras uno válido | `some` y no `every` |
| rechaza un endpoint sin ningún tipo | `length === 0` |
| rechaza `eventTypes` que no es array | `Array.isArray` |

**Evidencia del rojo** (`node scripts/verificar-mutantes-webhooks.mjs`):

```
ROJO 79 ConditionalExpression orden -> true
ROJO 80 EqualityOperator >= -> >
ROJO 81 EqualityOperator >= -> <
ROJO 74 ConditionalExpression index>0 && ... -> false
ROJO 75 LogicalOperator index>0 && -> ||
ROJO 78 EqualityOperator index > 0 -> index <= 0
ROJO 68 MethodExpression some -> every
ROJO 69 ArrowFunction predicado -> undefined
ROJO 71 ConditionalExpression !includes(type) -> false
ROJO 72 LogicalOperator !includes || -> &&
ROJO 66 ConditionalExpression eventTypes.length === 0 -> false
ROJO 65 ConditionalExpression !Array.isArray -> false

Muertos: 12 · Vivos: 0
```

**Previsión de muertes: 12** (3 de los «sin cobertura» de la línea 97 y 9
supervivientes de las líneas 90-99).

Nota sobre un equivalente: `indexOf` → `lastIndexOf` sobre `webhookEventTypes`
**es equivalente** y ninguna prueba puede matarlo, porque el catálogo no tiene
duplicados (y hay una prueba que fija la lista entera). No se persigue.
