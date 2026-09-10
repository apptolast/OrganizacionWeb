# Mutación de frontend — feature 25, webhooks

**94.57 %** (592/626), recomputado del `mutation.json`. **Medido, no previsto.**

Este fichero existe con **este nombre exacto** porque lo exige por nombre el punto 1 de
«Cambios requeridos» de `progress/judge_webhooks_cierre.md`. Es la bitácora canónica de la
puerta de frontend.

## Dos correcciones de esta acta, hechas el 10 de septiembre a las 20:10

Las cazó el juez de cierre y las dos tenían **la misma causa raíz**, que es la que este
repositorio lleva un día entero corrigiendo: **coordenadas escritas a mano que dependen de
algo que se mueve.**

1. **El informe que había en el repositorio medía el árbol de ANTES del arreglo.** Su copia
   de `webhooks-client.ts` no contenía `SECRET_UNREADABLE` —265 líneas contra 273 en
   disco—, así que su 94,57 % coincidía **por casualidad** con el bueno. El informe correcto
   vivía sólo en el worktree del carril. Ya está en el repositorio, y se comprobó que su copia
   del fuente coincide **byte a byte** con `frontend/src/`.
2. **El recuento de las guardas de aborto estaba a la mitad.** Esta acta publicaba **6**
   mutantes vivos; son **12**. El error fue filtrar por una lista de líneas escrita a mano,
   tomada del informe caducado: al añadirse el bloque de la octava clase de error, todo lo de
   debajo se desplazó seis líneas. Ahora se localizan **por texto** —buscando `signal.aborted`
   en el fuente— y no por número de línea.

## Fallo sobre las guardas `if (!...signal.aborted)` — **aceptadas como equivalentes**

El carril midió dos veces que esos mutantes no mueren y las declaró equivalentes: abortar y
desmontar son el mismo suceso en esta vista, y React ya descarta el `setState` sobre un
componente desmontado. El mecanismo de `@s38` **sí** tiene oráculo: borrarlo de las cuatro
formas posibles pone tres pruebas en rojo cada vez.

El juez lo aceptó **condicionado** a que la campaña pasara el 80 % sin contarlas como matadas.
**Comprobado con la cifra correcta, y se sostiene con holgura:**

|                                | mutantes con veredicto | puntuación  |
| ------------------------------ | ---------------------- | ----------- |
| Contándolas todas como vivas   | 626                    | **94.57 %** |
| Descontándolas del denominador | 614                    | 96.42 %     |

Son **12 mutantes vivos** en las 12 líneas de guarda (119, 122, 125, 151, 158, 161, 216, 227, 256, 269, 281, 288).
Aun contándolos todos en contra, la cifra queda **catorce puntos por encima** del umbral: no
eran el margen ni de lejos. El fallo queda firme.

Se deja escrito que el número publicado antes era la mitad del real **y que aun así la
conclusión no cambia**, porque una conclusión correcta sostenida en un número equivocado sigue
siendo un número equivocado, y el siguiente que lo lea merece saberlo.

## Los 34 sin matar, nominalmente

| Fichero                  | Línea | Mutador               | Estado     |                  |
| ------------------------ | ----- | --------------------- | ---------- | ---------------- |
| `src/webhooks-client.ts` | 73    | ConditionalExpression | Survived   |                  |
| `src/webhooks-client.ts` | 78    | ConditionalExpression | Survived   |                  |
| `src/webhooks-client.ts` | 102   | ConditionalExpression | Survived   |                  |
| `src/webhooks-client.ts` | 102   | EqualityOperator      | Survived   |                  |
| `src/webhooks-client.ts` | 115   | ConditionalExpression | Survived   |                  |
| `src/webhooks-client.ts` | 119   | ConditionalExpression | Survived   |                  |
| `src/webhooks.tsx`       | 66    | ArrowFunction         | Survived   |                  |
| `src/webhooks.tsx`       | 67    | ConditionalExpression | Survived   |                  |
| `src/webhooks.tsx`       | 86    | ArrayDeclaration      | Survived   |                  |
| `src/webhooks.tsx`       | 105   | ArrayDeclaration      | Survived   |                  |
| `src/webhooks.tsx`       | 111   | ArrayDeclaration      | Survived   |                  |
| `src/webhooks.tsx`       | 119   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 122   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 125   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 127   | ArrayDeclaration      | Survived   |                  |
| `src/webhooks.tsx`       | 130   | ArrayDeclaration      | Survived   |                  |
| `src/webhooks.tsx`       | 151   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 158   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 161   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 202   | ConditionalExpression | Survived   |                  |
| `src/webhooks.tsx`       | 202   | StringLiteral         | Survived   |                  |
| `src/webhooks.tsx`       | 203   | StringLiteral         | NoCoverage |                  |
| `src/webhooks.tsx`       | 216   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 227   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 256   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 269   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 273   | OptionalChaining      | Survived   |                  |
| `src/webhooks.tsx`       | 281   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 288   | ConditionalExpression | Survived   | guarda de aborto |
| `src/webhooks.tsx`       | 298   | OptionalChaining      | Survived   |                  |
| `src/webhooks.tsx`       | 303   | OptionalChaining      | Survived   |                  |
| `src/webhooks.tsx`       | 325   | StringLiteral         | Survived   |                  |
| `src/webhooks.tsx`       | 353   | OptionalChaining      | Survived   |                  |
| `src/webhooks.tsx`       | 433   | StringLiteral         | Survived   |                  |
