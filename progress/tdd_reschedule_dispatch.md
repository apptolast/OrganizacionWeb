# Dispatch de mutación frontend13 — 2026-09-06

Propuesta local aislada y revisable en `codex/reschedule-frontend`, autorizada por el coordinador tras aviso de coordinación `76180b4`/`4cd0ab`. No constituye ACK de la otra pista ni autorización para integrarla en main. Autoría limitada a `scripts/project.mjs`, `scripts/project.test.mjs` y esta nota; Ponytail full/Caveman lite. Sin commit ni push.

## Único ciclo TDD

1. Añadido test `reschedule frontend scope runs only its fixed Stryker configuration`, reutilizando `capture()` con runner inyectado. Comprueba una única llamada exacta a `pnpm --dir frontend exec stryker run stryker.reschedule.config.json`.
2. RED real `d9772a`: `node --test --test-name-pattern="reschedule frontend scope" scripts/project.test.mjs`, EXIT1, un fallo `Invalid target: reschedule-frontend`. No se lanzó ningún proceso de mutación.
3. Implementación mínima: agregar el literal a la lista cerrada y una rama que invoca ese comando fijo y retorna. No se añade destino backend13 ni rutas arbitrarias.
4. GREEN `4e9869`: `node --test scripts/project.test.mjs`, **18/18 PASS**, cero fallos/omitidos: los17 anteriores más el nuevo. Conserva dispatch por defecto, destinos previos, rechazo de destinos inválidos y pruebas de política/umbrales existentes.
5. Prettier focal y diff check GREEN `a4b90a`. Diff de scripts:30 inserciones, sin modificaciones de casos anteriores ni configuración.

## Freeze para judge

SHA256: `project.mjs` `5C85EA26F8C2C4D1D0AFF445ECF2ACB2F390E3683114ADCCFE160D5AB791334F`; `project.test.mjs` `8D7907C70C342A7462B885E22EDA9F35335C6FACD39BD4C1F45FB702CED9F202`.

No se creó/editó `stryker.reschedule.config.json`: el coordinador preparará el scope sobre fuentes congeladas y revisadas. El test demuestra dispatch, no existencia o validez de esa futura configuración ni resultado de mutación. No se ejecutaron Stryker, frontend, backend, init global o E2E. Main sigue pendiente del acuerdo compartido.
