# Revisión del fallo E2E de CI — 2026-09-06

Rol: judge, sólo lectura de fuentes, tests y configuración. Corte examinado: `12f6fddc78fb204565a11cf5d7a19e6b0eb7ca18`. [Run 34042696689](https://github.com/apptolast/OrganizacionWeb/actions/runs/34042696689): **FAIL, 13 failed / 78 passed**. Diagnóstico mediante `gh run view --log-failed`, log de instalación filtrado y `git show` del corte; no se ejecutaron suites ni se modificaron fuentes, tests o configuración. No se evaluó backend13 ni el panel actualmente en TDD.

## 1. Navegación móvil: defecto de presentación compartida

Doce fallos corresponden a overflow. Cinco identifican directamente el `nav` mediante `navigationFits` (`e2e/availability.spec.mjs:437–441`):

| Viewport | clientWidth del nav | scrollWidth del nav |
| --- | --- | --- |
| 320 | 296 | 303 |
| 361 | 321 | 355 |
| 390 | 350 | 374 |
| 419 | 379 | 393 |
| 421 | 381 | 395 |

Los otros siete fallan al comparar el ancho del documento con el viewport: `complete-reopen-task.spec.mjs:422`, `create-task.spec.mjs:462`, `edit-project.spec.mjs:378`, `project-states.spec.mjs:362`, `read-projects.spec.mjs:320`, `split-task.spec.mjs:479` y `today.spec.mjs:233`. Today mide **375 > 361 durante carga**, antes de recibir la agenda: el problema no depende de títulos largos del fixture ni de la respuesta de Hoy. Los seis casos restantes comprueban overflow global; el log no identifica individualmente todos sus elementos causantes, por lo que su resolución tras corregir navegación debe verificarse.

En `frontend/src/workspace.tsx` la navegación contiene Hoy, Proyectos y Disponibilidad. `frontend/src/styles.scss:1556–1563` establece un flex horizontal sin wrap, con `gap: 8px`, ancho completo y enlaces `flex: 1; min-width: 0`. Esto reparte el espacio en tres partes iguales permitiendo que cada caja sea menor que su contenido. Los enlaces conservan texto, icono, gap y padding (`:92`, `:620`); no tienen una solución para el texto que desborda esa caja. A 360 px se ocultan iconos y disminuyen padding/fuente (`:673–687`), pero incluso a 320 el nav desborda. El wrap de `.sidebar` sólo distribuye los hijos de la barra; no distribuye los enlaces dentro del nav.

**Corrección mínima propuesta:** en la regla móvil, permitir wrap del nav y asignar a los enlaces una base intrínseca (`flex: 1 1 auto`, en lugar de base cero), para que puedan ocupar otra fila cuando no quepan sus textos. Mantener el mínimo de 44 px, las etiquetas completas y la indicación activa. La combinación concreta debe comprobarse con los oráculos existentes a 320, 361, 390, 419 y 421; no basta añadir wrap manteniendo bases cero. No usar recorte/overflow oculto, tamaños menores, eliminación de textos ni relajación de assertions.

La pila de fuentes empieza por Segoe UI y continúa con system-ui (`styles.scss:6`); una diferencia de métricas entre plataformas es plausible, pero no se capturó la fuente computada del CI. No se atribuye el fallo exclusivamente a Linux ni a una fuente concreta. El defecto confirmado es que la distribución compartida no contiene su contenido en los tamaños medidos. No hay un fallo de assertion de mínimo 44 px en este run.

## 2. Zoom nativo: falta servidor gráfico en el entorno

`e2e/today-native-zoom.spec.mjs:43–47` abre Chromium con `launchPersistentContext` y `headless: false`, necesario para el flujo de extensión que aplica y comprueba zoom nativo al 200 %. Falla al lanzar, antes de cualquier assertion de geometría: `Missing X server or $DISPLAY` y recomendación explícita de `xvfb-run` en el log de Playwright.

`.github/workflows/harness-ci.yml:48` instala Chromium con dependencias; el log confirma `xvfb is already the newest version (2:21.1.12-1ubuntu1.6)`. Sin embargo, `:51` ejecuta `pnpm test:e2e` sin iniciar Xvfb. `scripts/e2e.mjs:25,62` hereda el entorno y lanza Playwright en el host; los contenedores de la aplicación no proporcionan un DISPLAY para ese proceso.

**Corrección mínima propuesta, separada de CSS:** ejecutar el paso Linux bajo `xvfb-run -a pnpm test:e2e`. No hace falta añadir dependencia ya instalada, cambiar a headless, sustituir zoom nativo por CSS ni saltar el test. Este diagnóstico no acredita todavía que todas las assertions del zoom pasarán una vez pueda abrirse el navegador.

## Dictamen y alcance

**CHANGES_REQUESTED:** corregir distribución móvil y arranque gráfico de CI por separado. No se detecta en estos logs una causa de datos, backend13 o fixture feliz simulado. La causa del zoom está confirmada; la navegación explica directamente cinco fallos y es candidata común sustentada para los demás. Conservar los 13 fallos originales y verificar los afectados después del arreglo, sin presentar los 78 aprobados como ejecución exitosa de la suite completa.

Evidencia local de revisión: `493866` (fallos filtrados), `0a1026` (reglas CSS completas relevantes), `192409` (líneas del corte, headful, workflow y Xvfb instalado). Ningún rerun realizado por este reviewer.
