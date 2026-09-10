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

---

## Racimo 2 — las demás guardas de `decodeEndpoint` y la invariante de desactivado

**Causa común.** Las trece pruebas del cliente partían todas del **mismo**
endpoint válido y sólo variaban `status`. Cada campo del DTO cerrado tiene su
guarda y ninguna se ejercía: ni un id no-uuid, ni una url `http://` o no-cadena,
ni una descripción no-cadena o de 81 puntos, ni la frontera de 80.

Y la invariante de @s1 —«un endpoint desactivado lleva SIEMPRE razón e instante,
y uno activo NUNCA»— sólo se probaba con **las dos mitades fallando a la vez**
(`status: "disabled"` con ambos campos `null`). Ese caso no separa el `||` del
`&&`: hacen falta los **asimétricos**, uno por mitad.

Doce pruebas nuevas. Frontera exacta incluida: ochenta emojis son ochenta puntos
de código y ciento sesenta unidades UTF-16, así que el caso de 80 acredita a la
vez el `> 80` (y no `>= 80`) y que la medida es en puntos de código.

**Evidencia del rojo: 13 mutantes muertos**, con la prueba que cae en cada caso:

```
ROJO 3   uuid(value) -> true                  · rejects an id of thirty-six characters that is not a uuid
ROJO 5   identifier && -> ||                  · idem
ROJO 52  typeof url !== string -> false       · rejects a url that is not even a string
ROJO 57  "https://" -> ""                     · rejects a destination that is not https
ROJO 58  typeof description !== string -> false · rejects a description that is not a string
ROJO 61  description > 80 -> false            · rejects a description of eighty-one code points
ROJO 62  > 80 -> >= 80                        · accepts a description of exactly eighty code points
ROJO 64  [...description] -> []               · rejects a description of eighty-one code points
ROJO 98  invariante || -> &&                  · rejects a disabled endpoint with a reason but no instant
ROJO 99  mitad de la razón -> false           · rejects a disabled endpoint with an instant but no reason
ROJO 103 razón && -> ||                       · rejects a disabled reason outside the two the contract defines
ROJO 106 mitad del instante -> false          · rejects a disabled endpoint with a reason but no instant
ROJO 110 instante && -> ||                    · rejects a disabledAt that is not an instant
```

### Hallazgo: `rejects.toThrow("…")` no acredita el diagnóstico

El mutante 0 (`incompatible = () => new Error(…)` → `() => undefined`) **seguía
vivo con las trece pruebas anteriores y con las doce nuevas**. Motivo: en vitest
4, `await expect(p).rejects.toThrow("Confirmación incompatible")` **se cumple
igual cuando lo que se lanza es `undefined`** en vez de un `Error`. Es decir,
las ~25 aserciones de rechazo del cliente no comprobaban en realidad que hubiera
un diagnóstico; sólo que algo fallaba.

Es la misma familia que la aserción-que-no-puede-fallar de `webhooks.test.tsx`.
Se cierra con **un** oráculo que fija el valor rechazado entero, y que cubre a
toda la familia:

```
ROJO 0 ArrowFunction incompatible -> undefined
     1 rojas · @s36 rejects with a real Error carrying the diagnosis, not a bare throw
```

**Previsión de muertes del racimo 2: 14.**

Dos equivalentes descartados y no perseguidos:
- `identifier`: `(value as string).length === 36` → `true`. La expresión regular
  de `uuid()` ya fija la longitud en 36, así que la comparación es redundante.
- `value.disabledReason !== null` → `true` y `value.disabledAt !== null` → `true`.
  `includes(null)` e `instant(null)` devuelven `false` sin lanzar, así que el
  resultado de la conjunción no cambia nunca.
