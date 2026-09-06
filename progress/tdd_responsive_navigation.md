# Corrección responsive de navegación — 2026-09-06

Autoría acotada: `frontend/src/styles.scss` y esta bitácora, rama aislada `codex/responsive-navigation`, base `12f6fdd`. Ponytail full y Caveman lite; contratos existentes de responsive y mínimo táctil 44 × 44 px conservados. Sin cambios en tests, workflow, runner, locks o panel13 en desarrollo.

## RED existente

CI [34042696689](https://github.com/apptolast/OrganizacionWeb/actions/runs/34042696689), mismo commit: 13 failed / 78 passed. Cinco fallos directos de navegación en `availability: responsive controls`: scrollWidth/clientWidth 303/296 a 320, 355/321 a 361, 374/350 a 390, 393/379 a 419 y 395/381 a 421. Otros siete overflow globales incluyen Today durante carga, antes de datos. El decimotercer fallo es ausencia de DISPLAY para zoom nativo; se aborda por separado, fuera de esta autoría.

El diagnóstico independiente previo se conserva en `progress/review_ci_reschedule.md` del árbol principal. No se inventa un RED local: la evidencia inicial es el run Linux real. La regla móvil usa tres enlaces con base flex cero, permitiendo cajas menores que su contenido. El wrap del contenedor sidebar no afecta al interior del nav.

## Preparación

- `node scripts/project.mjs install`: EXIT0 `bf1a79`, ambos installs `--frozen-lockfile`, sin cambios de lock. Aviso informativo de pnpm sobre build script de `@parcel/watcher`; no se modificó configuración.
- Init local obligatorio iniciado `5f6981`, sesión 17745; resultado pendiente antes de editar producción.
- Se reutilizan tests existentes: disponibilidad en 28 anchos (320–2560), comprobación de texto dentro de enlaces distintos, controles principales de 44 px, overflow de documento y axe. No se crean tests que reflejen propiedades CSS.

## Cambio mínimo y validación

Init terminó EXIT0 `a81a7a`: 1376 frontend/25 archivos, 17 scripts y lint GREEN. XML backend leído en `82f312`: 1415 tests, cero fallos, errores u omitidos. Producción se editó después de este resultado.

El cambio añade `flex-wrap: wrap` al nav móvil y cambia la base de los enlaces a `flex: 1 1 auto`. Mantiene padding, fuente, iconos, labels, mínimo 44 px, estado activo y reglas de escritorio. Prettier focal y `git diff --check` GREEN `82f312`.

Primer lanzamiento E2E `2e8a6f`, stack aislado 21148: ambos builds Docker GREEN, incluido TypeScript/Vite. La selección con alternativas `|` fue interpretada como tubería por el shell del runner Windows: `"navigation" no se reconoce...`, `pnpm exited with 255`, wrapper EXIT1 `fac4c2`. No ejecutó tests ni constituye un RED funcional. Stack retirado por el runner. Se conserva este fallo; se continúa con filtros simples admitidos por la misma CLI, sin editar runner ni tests.

Foco `node scripts/e2e.mjs --grep responsive` iniciado `e08f77`, stack 54692: **28/28 GREEN, EXIT0 `b69a0d`**, 1,6 minutos. Incluye los cinco anchos rojos en CI, tamaños intermedios y escritorio hasta 2560. El runner retiró únicamente su stack y scratch aislados.

Segundo foco `1873fe`, stack 3764, sesión 11499: selección de ocho tests existentes por archivo y línea, sin regex compuesto. Comprueba `availability.spec.mjs:472`, `complete-reopen-task.spec.mjs:380`, `create-task.spec.mjs:424`, `edit-project.spec.mjs:351`, `project-states.spec.mjs:332`, `read-projects.spec.mjs:293`, `split-task.spec.mjs:424` y `today.spec.mjs:184`. Son los siete recorridos con overflow global en CI y navegación junto al breakpoint. **8/8 GREEN `c2198c`, 2,8 minutos; EXIT0 y retirada del stack `a88d38`.**

Hash SHA256 del CSS congelado (`420749`): `F6CDB22BE4DBFA2C2BE01D40C76C86A023262D6DAC66F01C8714966CCCA34122`. Diff: dos inserciones y un borrado. No hay cambios de oráculos ni de fuentes distintas del CSS.

## Entrega para revisión

36/36 E2E focales GREEN en Chromium sobre host Windows y aplicación/API/PostgreSQL en Docker. Las assertions existentes de overflow, texto dentro de enlaces, controles de 44 px, teclado y axe se conservaron. Los cinco casos nav y los siete globales que fallaron en Linux pasan en este entorno; falta confirmar el nuevo corte en CI Linux. No se atribuye a esta ejecución una repetición del zoom nativo, Firefox/WebKit o validación universal de dispositivos.

Build Docker web (TypeScript y Vite) y backend GREEN; formato focal GREEN `82f312`; diff check final y hash sin cambios `a2ed65`. El init previo cubre el corte base; después sólo cambió CSS y se ejecutó el build y los E2E pertinentes. No se repite el backend completo sin cambio funcional nuevo.

Estado: **GREEN para judge**, fuentes congeladas. No commit, push, cambio de metadatos, mutación ni declaración de cierre13. El ajuste de DISPLAY en workflow pertenece a otra tarea.
