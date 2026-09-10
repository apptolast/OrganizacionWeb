# Mutación de frontend de la feature 25 — medida, 10 de septiembre de 2026

**94.57 %** (592/626), calculado del `mutation.json`. Antes: **71,40 %**.

Sobre el SHA `1239ad0e`, máquina drenada. Es la **décima y última** de las campañas.

## Cierra el bloqueante B1, y los dos ficheros pasan

| Fichero | Puntuación | |
|---|---|---|
| `src/webhooks.tsx` | 91.76 % (312/340) | ✅ |
| `src/webhooks-client.ts` | 97.90 % (280/286) | ✅ |

Es la subida más grande del día: **23 puntos**. El bloqueante pedía 52 muertes para
llegar al 80 %; el carril acreditó **145** y previó una banda de 88-93 %. El medido
queda en **94.57 %**, por encima de su propia previsión.

A diferencia de las features 27 y 29, aquí **ningún fichero queda por debajo del 80**,
así que no hay nada que declarar abierto por este lado.

## Lo que este carril encontró, que vale más que la cifra

Tres defectos de producto, con el rojo demostrado antes y después:

1. Al fallar **cualquier** acción el usuario leía «No se ha podido crear el webhook»,
   y «Ya tienes cinco webhooks» ante un límite, con el error encendiéndose en un
   campo ajeno vía `aria-describedby`.
2. El panel de entregas de un webhook **enseñaba las de otro**, con su «Reenviar»
   apuntando a la entrega ajena.
3. Nada ataba las etiquetas de evento con los tipos: quien marcara «Crear subtarea»
   podía suscribirse a otra cosa con la suite en verde.

Y uno de método que vale para todo el repositorio: **`rejects.toThrow("…")` en
vitest 4 se cumple contra un `throw undefined`**, lo que dejaba unas 25 aserciones
del cliente sin comprobar el diagnóstico.

## Los 35 sin matar, nominalmente

| Fichero | Línea | Mutador | Estado |
|---|---|---|---|
| `src/webhooks-client.ts` | 67 | ConditionalExpression | Survived |
| `src/webhooks-client.ts` | 72 | ConditionalExpression | Survived |
| `src/webhooks-client.ts` | 96 | ConditionalExpression | Survived |
| `src/webhooks-client.ts` | 96 | EqualityOperator | Survived |
| `src/webhooks-client.ts` | 109 | ConditionalExpression | Survived |
| `src/webhooks-client.ts` | 113 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 66 | ArrowFunction | Survived |
| `src/webhooks.tsx` | 67 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 86 | ArrayDeclaration | Survived |
| `src/webhooks.tsx` | 105 | ArrayDeclaration | Survived |
| `src/webhooks.tsx` | 111 | ArrayDeclaration | Survived |
| `src/webhooks.tsx` | 119 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 122 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 125 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 127 | ArrayDeclaration | Survived |
| `src/webhooks.tsx` | 130 | ArrayDeclaration | Survived |
| `src/webhooks.tsx` | 151 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 158 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 161 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 202 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 202 | StringLiteral | Survived |
| `src/webhooks.tsx` | 203 | StringLiteral | NoCoverage |
| `src/webhooks.tsx` | 216 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 227 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 256 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 269 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 273 | OptionalChaining | Survived |
| `src/webhooks.tsx` | 281 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 288 | ConditionalExpression | Survived |
| `src/webhooks.tsx` | 298 | OptionalChaining | Survived |
| `src/webhooks.tsx` | 303 | OptionalChaining | Survived |
| `src/webhooks.tsx` | 325 | StringLiteral | Survived |
| `src/webhooks.tsx` | 353 | OptionalChaining | Survived |
| `src/webhooks.tsx` | 433 | StringLiteral | Survived |
| `src/webhooks.tsx` | 517 | ConditionalExpression | RuntimeError |
