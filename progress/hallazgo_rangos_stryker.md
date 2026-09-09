# Hallazgo: los rangos fijados de Stryker se han desplazado — 10 de septiembre de 2026

**Dieciséis** `frontend/stryker.*.config.json` no mutan ficheros enteros: fijan
**rangos `línea:columna`** dentro de `src/App.tsx` y `src/workspace.tsx`, para
mutar sólo el trozo que añadió su feature. Ejemplo real:

```json
"mutate": ["src/App.tsx:47:8-47:51", "src/workspace.tsx:128:10-133:22", "src/automations-api.ts"]
```

El problema es evidente en cuanto se enuncia: **un rango es una constante que
depende de algo que crece**. Cada ruta nueva desplaza las líneas de `App.tsx` y
`workspace.tsx`, y el rango pasa a cubrir otra cosa. Y no avisa: Stryker muta lo
que haya en esas coordenadas y da una puntuación con toda naturalidad.

Es la misma familia que las cinco roturas de la integración de ayer y que el
patrón PIT muerto que dejó a `AesGcmSecretCipher` sin recibir un solo mutante.

## La evidencia

Imprimiendo el texto que cada rango cubre **hoy**:

| Configuración | Rango | Lo que cubre |
|---|---|---|
| `today.replay` | `App.tsx:40:39-40:41` | `io` |
| `today.replay` | `App.tsx:22:31-22:43` | `m "./externa` |
| `today.replay` | `App.tsx:35:10-35:41` | `eklyReview = /^\/revision-seman` |
| `today.replay` | `App.tsx:43:11-43:16` | `hooks` |
| `weekly-review` | `App.tsx:15:8-15:69` | `ExportData } from "./export-data";` |
| `custom-views-fields` | `App.tsx:17:24-17:49` | `"./calendar";` |
| `split-task.replay` | `App.tsx:10:0-10:200` | `import { Today } from "./today";` |
| `availability` | `App.tsx:11:0-11:200` | `import { Availability } from "./availability";` |

Fragmentos a media palabra —`io`, `hooks`, `m "./externa`— y **sentencias
`import`**. Una campaña que muta un `import` mide si el árbol compila, no si los
tests muerden.

## Qué NO significa

No significa que esas features estén mal hechas ni que sus tests no valgan. Lo
que significa es que **la evidencia de mutación que se declaró para ellas no
mide lo que dice medir**, al menos en la parte del rango desplazado. Cada una
tiene además su propio fichero completo en la lista (`src/today.tsx`,
`src/weekly-review.tsx`…), que sí se muta bien; lo desplazado es el trozo
compartido.

## Qué features afecta

A las ya cerradas: `today`, `today.replay`, `weekly-review`, `split-task`,
`split-task.replay`, `availability`, `history`, `appearance`,
`custom-views-fields`, `ics-calendar`, `export-data`, `import-data`,
`integration-api`.

De las cinco de esta noche, `external-calendar` y `automations` tienen rangos
**correctos ahora mismo** —se comprobó imprimiéndolos—, pero se desplazarán en
cuanto se añada una ruta más, y la feature 29 va a añadir la del catálogo.

## Qué se ha hecho esta noche

Una herramienta que imprime, para cada rango, el texto que cubre:
`scratchpad/rangos.mjs`, con la instantánea previa en `rangos-antes.txt`. Se
ejecuta antes y después de tocar `App.tsx` o `workspace.tsx`; si el texto
cubierto cambia, el rango se desplazó. Es la red mínima para que la ruta del
catálogo no rompa dieciséis ámbitos en silencio.

## Qué habría que hacer, y es decisión del propietario

Fijar rangos por coordenadas es frágil por construcción. Las dos salidas:

1. **Mutar los ficheros compartidos enteros** en cada ámbito y dejar que las
   pruebas de la feature maten sólo lo suyo. Sube el número de supervivientes
   —los trozos de otras features— y obliga a bajar el umbral o a excluirlos por
   otra vía. Honesto, pero ruidoso.
2. **Sacar de `App.tsx` y `workspace.tsx` lo que cada feature añade**, a un
   módulo propio por feature: una tabla de rutas y una de entradas de
   navegación. Entonces cada ámbito muta su fichero, sin rangos. Es más trabajo,
   pero elimina la clase entera de fallo, incluida la colisión de la enmienda de
   navegación de ayer.

Recomendación: la **2**. La **1** cambia el síntoma; la **2** quita la causa.

No se ha tocado nada de esto esta noche, a propósito: afecta a trece features ya
cerradas y reabrirlas sin tu decisión sería peor que el defecto.
