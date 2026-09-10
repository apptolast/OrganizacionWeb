# Mutación de la feature 27, frontend — 10 de septiembre de 2026

Campaña `github_connector-frontend` sobre `main`, medida y no declarada.

| Fichero | Puntuación | Muertos | Vivos | Sin cobertura |
|---|---|---|---|---|
| `src/github-connector-client.ts` | 87,40 % | 215 | 31 | 0 |
| `src/github-connector.tsx` | 83,28 % | 284 | 57 | 0 |
| `src/integrations-index.tsx` | 100,00 % | 2 | 0 | 0 |
| **Total** | **85,06 %** | **501** | **88** | **0** |

Umbral 80. **Pasa.**

## Las tres condiciones del juez, una a una

1. **≥ 80 %**: 85,06 %. Cumple.
2. **Total de mutantes declarado y ningún módulo a cero**: 589 mutantes; los tres
   módulos con mutantes (246, 341 y 2). Ninguno a cero, que era la señal de que
   `coverageAnalysis: "perTest"` no había encontrado pruebas del fichero.
3. **Mutantes del escuchador de Escape (`github-connector.tsx:149-159`) muertos**:
   13 mutantes en ese rango, **13 muertos, 0 vivos**.

## El camino

- 69,44 % en la primera medición real. Esa campaña **nunca se había ejecutado**:
  el juez la tenía como condición pendiente, así que no era una regresión sino un
  hueco que llevaba ahí desde el principio.
- 75,55 % tras matar cuatro racimos del cliente.
- 85,06 % tras los 44 oráculos nuevos del artesano, verificados uno a uno
  aplicando el mutante real al fichero de producción y restaurando.

**Cero mutantes sin cobertura** en los tres módulos: no queda ninguna rama del
producto sin ejercer.
