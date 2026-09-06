# Propuesta aislada de estabilidad CI — 2026-09-06

Rama `codex/responsive-navigation`, base607d2b6. Autorización de propuesta local comunicada en main7d6d834/0bb1f5; no constituye ACK ni acuerdo para integrar en main. Sólo se modifican `e2e/project-states.spec.mjs`, `.github/workflows/harness-ci.yml` y esta nota. Ponytail full/Caveman lite. Init/build previos vigentes, sin cambios de producción. Sin commit ni push.

## RED real y causa

[CI34044566475](https://github.com/apptolast/OrganizacionWeb/actions/runs/34044566475): **87 PASS / 4 FAIL**. Lectura filtrada `e794bd` confirma tres fallos strict en líneas originales244/322/397: `getByRole('status')` encuentra simultáneamente «Cambiando estado» y «Cargando tareas». Es ambigüedad del locator global, no evidencia de que deba desaparecer el anuncio legítimo de tareas.

El cuarto fallo ocurre al lanzar Chromium headful para zoom: `Missing X server or $DISPLAY`. La instalación previa ya confirmó Xvfb disponible; falta iniciarlo en el paso E2E.

## Corrección mínima

- Sólo los tres locators fallidos se acotan a `getByRole("region", { name: "Estado del proyecto", exact: true }).getByRole("status")`. `project-status-control.tsx:37–108` contiene esa región etiquetada y un único anuncio que evoluciona de «Cambiando estado» a «Estado actualizado» (`e6e2c1`). La assertion exacta `toHaveText("Estado actualizado")` se conserva, igual que concurrencia, persistencia, foco, tamaños44px, overflow y axe. Sin `.first()`, pausas, skips ni timeout añadido.
- Único cambio de workflow: `pnpm test:e2e` pasa a `xvfb-run -a pnpm test:e2e`. No se cambia headless, zoom nativo, dependencias, otras políticas o pasos. Linux pendiente de CI; el host Windows local no valida Xvfb.

## Ejecuciones

Primer lanzamiento `8b1a43`: builds del stack51064 GREEN, pero selección por líneas desfasada después de formato produjo `No tests found`, EXIT1 `69d848`; no ejecutó pruebas. Se preserva este error de invocación. El formato había expandido también un TRUNCATE previo: se restauró esa línea sin cambio semántico para mantener el diff restringido a los tres selectores.

Selección corregida: `node scripts/e2e.mjs e2e/project-states.spec.mjs:169 e2e/project-states.spec.mjs:263 e2e/project-states.spec.mjs:340`, stack27344, sesión80569. **3/3 GREEN, EXIT0 `2e1c0a`,50,7s**, Chromium local con API/PostgreSQL reales. Stack retirado por el runner. `node --check` y `git diff --check` GREEN `5f5f4c`; diff de tests sólo tres sustituciones de locator.

Estado: **GREEN focal, freeze para revisión**. Propuesta local; no se declara CI Linux verde ni se sustituye el zoom headful por esta prueba.

## Ampliación tras revisión independiente

Root `7027af/9a3b8f` detectó cuatro llamadas hermanas con la misma ambigüedad en el mismo archivo: líneas77 (Cambiando estado) y109/251/281 (Estado actualizado) del corte revisado. Alcance ampliado explícitamente para corregir la causa en todos los anuncios de estado del proyecto, incluidos caminos que un fallo previo de CI podía interrumpir. Se acotan a la misma región, conservando cada texto exacto. Los dos anuncios «Proyecto actualizado» del editor permanecen intactos. No hay test nuevo ni RED nuevo atribuido a estas cuatro llamadas: extensión sustentada por el RED real y la revisión de la misma causa.

El usuario detuvo la pista paralela y autorizó integrar todo; nota maina957d73 sustituye la espera anterior. El agente sigue sin realizar commits/push ni integración: root revisa y publica tras CI.

Archivo completo: `node scripts/e2e.mjs e2e/project-states.spec.mjs`, `ef82f4`, stack3960, sesión42800. **4/4 GREEN, EXIT0 `38e7b8`,37,3s**, stack retirado. Incluye transiciones explícitas/feedback, concurrencia de capacidad, conflicto de versión entre texto y estado, y matriz de controles/reflow. Sintaxis/diff check GREEN `bb31b9`. Sólo permanecen los dos locators globales del editor «Proyecto actualizado», fuera del alcance acordado. Workflow Xvfb sin más cambios. **Freeze renovado para judge**, pendiente CI Linux.
