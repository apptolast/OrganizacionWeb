# Cierre de la puerta de mutación de frontend — feature 28, calendario externo

Carril `claude/external-calendar`, worktree `C:/Users/vhurt/ow-worktrees/external-calendar`,
partiendo de `origin/main` en `5b019343`. Puerto E2E asignado: 18092 (no usado: todo
el trabajo es de unitarias).

Ámbito de mutación (`frontend/stryker.external-calendar.config.json`):
`src/external-calendar-api.ts`, `src/external-calendar.tsx`,
`src/today-external-calendar.tsx` (más cuatro rangos de `App.tsx`/`workspace.tsx`
que no son míos).

Mapa de partida: `progress/prediccion_huecos_frontend.md`, sección
`28-calendario-externo`. Orden de ataque el del encargo: primero `sin_cobertura`
con posible defecto, luego `asercion_que_no_puede_fallar`, luego `oraculo_debil`.

Línea base antes de tocar nada: **83 pruebas verdes** en los cuatro ficheros del
ámbito (`external-calendar.test.tsx`, `external-calendar-api.test.ts`,
`external-calendar-route.test.tsx`, `today-external-calendar.test.tsx`).

---

## Racimo A — el catch de `loadEvents` (predicción: racimo 3, `sin_cobertura`, 9 mutantes)

`external-calendar.tsx:124-137`. La predicción decía que con un 500 o un 503 en
`GET /events` la vista cae al `else` de `:390` y afirma «No hay eventos en la
ventana guardada»: le dice al propietario que su calendario está vacío cuando en
realidad no ha podido leerlo.

### DEFECTO DE PRODUCTO 1 — la ventana vacía que nadie había leído

Confirmado ejecutando, no razonando. Prueba nueva
«@s36 no afirma que la ventana esté vacía cuando la lista falla con 500 sin cuerpo
reconocible»: el volcado del DOM del fallo contiene, literalmente,

```
No hay eventos en la ventana guardada.
```

con el `GET /events` respondiendo **500**. La causa: `refuse()`
(`external-calendar-api.ts:188`) relanza la **propia `Response`**, que no es
`instanceof Error`, así que la guarda

```ts
if (error instanceof Error && !(error instanceof ConnectorsDisabledError))
  setInvalidList(true);
```

no se cumplía nunca para un error de servidor. Igual con el 503
`CONNECTORS_DISABLED`, que sí es `Error` pero estaba excluido a propósito: también
acababa pintando la ventana como vacía.

**Arreglo** (`external-calendar.tsx:130-137`): toda lectura que no llega deja la
lista en «no se ha podido leer». La condición desaparece; queda solo el
cortocircuito de aborto, que sí tiene motivo (una lectura cancelada no es una
lectura fallida).

Es el mismo patrón que los cinco defectos de esta noche: **la interfaz afirma un
hecho que no conoce**.

### Rojo acreditado

Tres filas nuevas (`it.each`), una por forma de fallo, más una prueba positiva:

| Prueba | Antes del arreglo | Después |
|---|---|---|
| `@s36 … falla con 500 sin cuerpo reconocible` | ROJO: `Unable to find an element with the text: No se ha podido leer la lista de eventos.` y el DOM mostraba «No hay eventos en la ventana guardada.» | verde |
| `@s36 … falla con 503 de conectores` | ROJO, mismo mensaje | verde |
| `@s36 … falla con fallo de red` | ya pasaba (un `TypeError` sí es `Error`); queda como red de seguridad de la rama que antes funcionaba | verde |
| `@s37 dice que la ventana está vacía solo cuando la lectura sí ha llegado` | verde desde el principio; existe porque el literal «No hay eventos en la ventana guardada.» **no lo afirmaba ninguna prueba** (hueco «omitido» de la predicción) y sin él la aserción negativa de las tres filas anteriores no valdría nada | verde |

Mutantes que pasan a estar cubiertos: el literal de `:390`, el ternario
`events.length === 0`, y —por la vía del arreglo— desaparecen del denominador los
mutantes de la conjunción que se ha eliminado.

Estado tras el racimo: **87 pruebas verdes** en los cuatro ficheros.
