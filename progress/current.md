# Estado actual — cierre de 24 y arranque paralelo de 25–30 (8 de septiembre de 2026, sesión nocturna)

Instrucción del usuario (8 de septiembre, 19:31): sincronizar con el remoto, completar todas las features restantes hoy con el máximo de agentes en paralelo, y verificar el modo oscuro. Esta instrucción explícita autoriza trabajar 25–30 en paralelo en worktrees aislados (una rama por feature) sin repetir puertas humanas; se conservan spec, Gherkin, TDD, revisión independiente y mutación por feature. La autorización global del 5 de septiembre sigue vigente.

## Hecho en esta sesión

- Local sincronizado con `origin/main` 4c7d558 (44 commits nuevos). Borradores locales antiguos del backend de replanificación apartados fuera del repo (ya estaban integrados por PR 5/6).
- Arnés Windows: `scripts/project.mjs` resuelve `gradlew.bat` con ruta absoluta porque `NoDefaultCurrentDirectoryInExePath=1` impide a cmd buscar en el directorio actual; `scripts/e2e.mjs` acepta `E2E_WEB_PORT` para levantar varias pilas E2E en paralelo. 70/70 tests del arnés verdes.
- Feature 24: único fallo de CI (34253701367, @s41 texto 200 % a 320 px, marca de 345 px) corregido en `codex/integration-api` con 01f80ab (`.brand` deja de ser `nowrap`). Verificación local: Vitest 2507/2507, tsc, E2E focal 5/5 y 10/10 de reflow. CI 34264153095 en curso. Pendiente: CI verde → marcar PR 29 lista → merge → despliegue y cierre 24.
- Briefs de arquitectura para implementadores: `progress/brief_backend.md`, `progress/brief_frontend.md`.
- Contratos 25–30: propuestas normativas en `progress/proposal_<name>.md`, integradas en `project-spec.md` (secciones «Feature 25»–«Feature 30»). Migraciones reservadas: V23 webhooks, V24 calendar_feed_tokens, V25 github_connector, V26 external_calendar, V27 additional_connectors, V28 automations. Gherkin en curso por seis `gherkin_author`.
- Modo oscuro: auditoría Playwright en curso (`progress/darkmode_audit.md`). Análisis estático previo: `frontend/src/today.scss` usa colores claros fijos sin variante oscura; `frontend/index.html` `theme-color` solo claro.
- CI de main 34235036356 (commit de docs) falló por un E2E de exportación con `Protocol error (Network.getResponseBody)`: fallo de infraestructura de Playwright, no de producto; se vigila si se repite.

## Corte por cuota y reanudación (9 de septiembre de 2026, 01:05 Madrid)

La cuota de sesión se agotó sobre las 00:30 y detuvo los seis agentes a la vez; ninguno perdió trabajo en disco. Al reanudar: `main` está en f8570d7 con la feature 24 integrada (PR 29 squash 0277c50) y CI 34271043131 SUCCESS. El squash borró la sección 24 de `project-spec.md` y se restauró en un commit propio. Los seis carriles se relanzaron desde su último commit con instrucción explícita de commitear cada ciclo y de escribir los archivos con las herramientas de edición, no con heredocs (los escapes de iCalendar y de las plantillas se corrompían).

Estado de los carriles al reanudar: webhooks con dominio/casos de uso y guardia de direcciones; automations con dominio de reglas y plantillas; darkmode con cuatro correcciones ya commiteadas (Hoy con tokens, theme-color, filtros de historial, separador); ics_calendar, github_connector y external_calendar con su dominio inicial sin commitear.

## Plan de carriles (worktrees en C:/Users/vhurt/ow-worktrees, ramas `claude/<feature>` desde `codex/integration-api`)

- A: 25 webhooks → después 30 automations fase 2 (worker compartido y NOTIFY_WEBHOOK).
- B: 26 ics_calendar.
- C: 27 github_connector → después 29 additional_connectors (depende de 25–28 integradas).
- D: 28 external_calendar.
- E: 30 automations fase 1 (reglas, plantillas, simulación, auditoría, UI) en paralelo; fase 2 tras 25.

Cada carril: tdd_craftsman → judge → mutation_tester → integración en main por PR con CI. Orden de integración previsto: 24, 25, 26, 27, 28, 30, 29.

## Límites operativos

COMMON/V14 protegido. Puertos 8080/18080/18081 reservados; los E2E paralelos usan `E2E_WEB_PORT` 18090+. Credenciales fuera de Git/logs/chat. No se declara `done` sin juez y mutación sobre el umbral.

## Despliegue: bloqueado por acceso, comprobado el 9 de septiembre de 2026

El despliegue productivo de 24–30 no puede hacerse desde este equipo. La comprobación SSH de solo lectura contra `admin@159.195.156.57` con BatchMode y verificación estricta vuelve a responder `Permission denied (publickey)`; en `~/.ssh/config` solo hay alias de GitHub y GitLab, ninguno del servidor. Los flujos de trabajo de `apptolast/DockerSwarmInfrastrcture` son únicamente `validate.yml` y `guard-sensitive-paths.yml`: validan, no aplican. La aplicación real la ejecuta quien tiene acceso al host.

La PR 40 de infraestructura, `codex/integration-release`, prepara la publicación de la API para integraciones y sigue en borrador con su validación verde. Queda a la espera de que el usuario facilite alias o clave SSH, o de que aplique él mismo. Las features se cierran igualmente por juez y mutación; «desplegado» es una puerta distinta de «done» y no se declarará sin evidencia.

## Reducción a tres carriles — 9 de septiembre de 2026, 04:05 Madrid

Siete carriles simultáneos dieron rendimiento cero: dos ventanas consecutivas de veinte minutos sin un solo commit entre todos, con la CPU al 95 %, entre 130 y 170 contenedores de Testcontainers vivos y siete sesiones creando más de forma continua. Las instrucciones de filtrar pruebas y suspender el uso de contenedores no bastaron, porque un agente dentro de una ejecución larga no las lee hasta que termina.

Quedan activos tres carriles: el cierre de la feature 24, el modo oscuro y webhooks. Se aparcan cuatro: calendario ICS, conector GitHub, calendario externo y automatizaciones. Al aparcarlos se hizo un commit de resguardo por carril, marcado `wip(...)` y explícitamente no verificado, que preserva 35 archivos: 15 del calendario ICS, 5 del conector GitHub, 9 del calendario externo y 6 de automatizaciones. Quien retome cada carril debe compilar, arreglar lo que falte y rehacer el ciclo antes de seguir; ninguno de esos commits acredita nada.

Estado real de los aparcados en el momento de pararlos: el calendario ICS tenía 42 pruebas en verde sin commitear y estaba a punto de guardarlas; automatizaciones iba por el segundo ciclo de las operaciones de regla como casos de uso puros; el conector GitHub tenía el rojo confirmado de la validación de la base de la API y buscaba el verde; el calendario externo estaba añadiendo propiedades de configuración.

Los contenedores bajaron de 132 a 96 al aparcarlos. El plan es cerrar los tres carriles vivos, integrarlos, y relanzar los cuatro aparcados de uno en uno o de dos en dos, nunca los siete otra vez.

## Relanzamiento con plazo — 9 de septiembre de 2026, mañana

Instrucción del usuario: todas las features terminadas hoy, «de hoy no pasas,
por la mañana». Ha restablecido los límites de uso y ha instalado el arnés ECC
2.2.1 (`ecc@ecc`, ámbito de usuario, perfil de hooks estándar), que ya está
activo en esta sesión sin reiniciar.

### Causa raíz del colapso de anoche, encontrada y corregida

`.claude/settings.json` tenía un hook `PostToolUse` sobre `Edit|Write` que
ejecutaba `node .harness/harness.mjs test`, es decir la **suite completa** de
Java y frontend, que levanta del orden de veinte contenedores PostgreSQL. Con
siete carriles editando ficheros continuamente, ese hook se disparaba sin
parar. No fue que los agentes ignorasen la instrucción de filtrar sus pruebas:
el arnés les corría la suite entera por detrás después de cada escritura.
Retirado en `803dfe2`; la puerta de verificación se conserva en el hook `Stop`.

Dato de diseño que conviene tener presente: hay **49 clases de test con su
propio `PostgreSQLContainer` estático**, así que cualquier filtro amplio
levanta decenas de contenedores. Los filtros tienen que apuntar a clases
concretas.

### Feature 24 — estado de cierre

Juez final: **APPROVED** (`progress/judge_integration_api_final.md`). Las tres
campañas de mutación pasan: núcleo 180/188 = 95,74 %, HTTP 114/118 = 96,61 %
(reejecutada tras cambiar `SecurityConfiguration`), frontend 836/997 = 83,85 %.
Umbral 80 %.

De las tres condiciones de la sección 8 del dictamen:

1. Reejecutar la mutación HTTP — **hecha**, PASS.
2. **CI verde — pendiente.** La ejecución `34303295662` sobre `5c84a87` falló
   en `pnpm test:e2e`: `export-data.spec.mjs:13` con
   `Protocol error (Network.getResponseBody): No data found for resource with
   given identifier`, 1 fallo y 166 pasadas. Es el flake conocido de
   exportación, no un fallo de producto. Carril propio en marcha.
3. Árbol limpio — **hecha** en `803dfe2`: `.claude/settings.json` commiteado
   (conservando `ecc@ecc`) y `backend/bin/` ignorado.

### Modo oscuro — el usuario tenía razón

Medición propia sobre `main` con Chromium: 73 hallazgos, cinco defectos reales.
«Hoy» pinta dos paneles con colores claros fijos y el texto encima queda en
contraste 1,05 y 1,18 frente al mínimo de 4,5; `theme-color` sirve el lienzo
claro en las diez rutas. Detalle y falsos positivos en la sección 9 de
`progress/darkmode_audit.md`. Los arreglos ya existen en `claude/darkmode` pero
**no están en `main`**, y esa rama salió de antes de la feature 24: integrarla
por merge borraría todo el frontend de la API para integraciones. Va por rebase
o cherry-pick.

### Siete carriles activos

| Carril | Rama | Cometido |
| ------ | ---- | -------- |
| 25 | `claude/webhooks` | webhooks completos |
| 26 | `claude/ics-calendar` | calendario ICS |
| 27 | `claude/github-connector` | conector GitHub |
| 28 | `claude/external-calendar` | calendario externo |
| 30 | `claude/automations` | automatizaciones, fase 1 |
| — | `claude/ci-export-flake` | flake E2E que bloquea el cierre de 24 |
| — | `claude/darkmode` | rebase sobre main y cierre de D1–D5 |

La feature 29 (`additional_connectors`) depende de 25–28 y arranca después.
Orden de integración previsto: 24, 25, 26, 27, 28, 30, 29, con el modo oscuro
en cuanto esté rebasado.

Puertos ocupados: 18099 (pila de auditoría del modo oscuro, del orquestador),
18095 (pila del carril del flake). El resto de carriles no levanta pilas.
