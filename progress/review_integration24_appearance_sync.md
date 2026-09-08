# Sincronización del oráculo heredado de apariencia

Fallo real previo: init24 original de A, frontend 2423/2424; el caso @s24 fijo esperaba colorScheme dark y recibió cadena vacía. Originales conservados fuera del checkout en `deployment-preparation/integration24-init-original.log`, `.exit`, `integration24-baseline-before.json`, `integration24-baseline-failed-after.json` y paquete/resultados asociados. No se fabricó un RED ni se modificó producto para provocarlo.

Causa en el orden observable: el test esperaba únicamente radio.checked tras resolver la lectura. `appearance-state.tsx:125` aplica dataset, colorScheme y acento desde un useEffect dependiente de snapshot. El radio renderizado puede satisfacer la espera antes de que ese efecto publique colorScheme. El valor vacío no demuestra un error de selección de tema: falta sincronización del primer oráculo de pintura.

Cambio exclusivo de prueba: envolver el primer `expect(document.documentElement.style.colorScheme).toBe(effective)` en `await waitFor`, con timeout predeterminado y exactamente la misma igualdad. Conserva ambos ejemplos LIGHT/DARK, radio.checked, cambios del sistema/visibilitychange, comprobaciones finales de dataset/colorScheme/acento y GET único. No cambios en Provider, App, producción, scope Stryker ni código24.

Verificación: `pnpm --dir frontend exec vitest run src/appearance.test.tsx` termina EXIT0, **50/50**, 11,68 s. Log y exit en `progress/integration24_appearance_sync_focal.log` y `.exit`. ESLint del archivo pasó; primer check de formato señaló saltos de línea introducidos por la edición. Prettier aplicado sólo a ese archivo, después ESLint y Prettier check EXIT0; diff final sigue siendo tres líneas añadidas por una eliminada, sin cambios de oráculos. `git diff --check` correcto. No se repite suite por normalización de formato ni init antes de revisión root.

Fuente final `frontend/src/appearance.test.tsx` SHA256 `DD21DFD9C521849D067C8EA38AB00DDCFCCB5BC38D07B1C0551EFEE1E1A62F3E`. Corte quieto para revisión; no commit propio.
