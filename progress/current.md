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

## Deuda identificada, aplazada a propósito — 9 de septiembre de 2026

### La mutación nocturna nunca ha terminado

`.github/workflows/harness-mutation.yml` corre `node .harness/harness.mjs verify`,
que es `init` más la puerta de mutación. Como `harness.config.json` deja
`mutation.targets` vacío, esa puerta muta **el repositorio entero** en un único
job: unas trece horas de trabajo medidas (los alcances rondan los veinte minutos
cada uno y hay más de cuarenta) dentro de una ventana de cuatro.

Las tres ejecuciones programadas —7, 8 y 9 de septiembre— acabaron `cancelled` a
los **240 minutos exactos**, que es su `timeout-minutes`. GitHub reporta el
agotamiento del plazo como cancelación, y por eso parecía un relevo y no un
fallo. Nunca ha llegado a generar un mutante.

**Arreglo previsto:** matriz de un job por objetivo de mutación con
`fail-fast: false`, derivando la lista del propio `scripts/project.mjs` en vez de
copiarla en el YAML —duplicarla ahí la condenaría a pudrirse, que es el mismo
vicio que hoy rompió la guarda de esquema aditivo y las fixtures—. Eso exige
extraer a una constante exportada el array que hoy vive dentro de
`createProject`. Conviene además revisar la cadencia: cuarenta jobs cada noche
para una red de seguridad cuya puerta real se corre en local antes de cada cierre
es mucho; semanal parece más proporcionado, pero es decisión del propietario.

**Por qué se aplaza:** `scripts/project.mjs` es uno de los tres ficheros donde ya
chocan las features 25, 28 y 30, pendientes de fusionar. Tocarlo ahora añadiría
un cuarto conflicto a tres integraciones en curso. La nocturna no bloquea ninguna
puerta: `docs/verification.md` y `CHECKPOINTS.md` C7 exigen la mutación por
alcance ejecutada por el `mutation_tester`, no esta. Se arregla en cuanto las
tres estén dentro.
## Carril `claude/automations` — feature 30, fase 1

Rebasado sobre `origin/main` (7ea682d) replayando sólo los 20 comits del carril
desde `01f80ab`; un `git rebase origin/main` directo intentaba 118 comits porque
la rama salió de `codex/integration-api`, no de main.

Fase 1 completa: reglas (crear, leer, reemplazar, borrar, cupo de veinte con
carrera real), plantillas, evaluador, simulación en seco, auditoría paginada, API
HTTP, migración V28 y la página `/automatizaciones`. Bitácora, trazabilidad
@s → test, comandos y límites en `progress/tdd_automations.md`.

No demostrado: la fase 2 entera (worker y `NOTIFY_WEBHOOK` real, dependen de 25).
El punto de extensión `WebhookEndpointLookup` responde false y tiene test de
contrato; ningún escenario de fase 2 se declara verde. Sin mutación lanzada.

## Cierre de la integración — 9 de septiembre de 2026, última sesión

El usuario avisó de que esta es la última sesión dedicada al proyecto tras siete
días. Lo que sigue es el estado real, sin adornos, para que quien lo retome sepa
exactamente qué está acreditado y qué no.

### Los cinco merges están hechos

`main` contiene ahora las features 24, 25, 26, 27, 28 y 30. En orden:

| Commit | Qué integra |
| ------ | ----------- |
| `fa49fd7` | feature 27, conector GitHub, versión corregida tras su rechazo |
| `bdbeafc` | feature 25, webhooks |
| `5b9937e` | feature 28, calendario externo, con la unificación de `AddressPolicy` |
| `444ce24` | feature 30, automatizaciones fase 1 |
| `57608a8` | feature 26, calendario ICS, versión aprobada en tercera lectura |

El integrador que hacía este trabajo murió al agotarse el límite de sesión, con
el merge de la 28 resuelto pero **sin commitear**. No se perdió nada: no había
conflictos pendientes, se verificó la resolución de `AddressPolicy` a mano y se
commiteó. Los merges cuarto y quinto se hicieron desde el orquestador.

### La resolución que importaba: `AddressPolicy`

Las features 25 y 28 añadían cada una `application/AddressPolicy` con el mismo
nombre cualificado, sin base común en `main`, con la polaridad invertida
—`isBlocked` frente a `allows`— y, lo que es peor, **con conducta distinta**
sobre las formas IPv6 que encapsulan una IPv4: la 25 las normalizaba a la IPv4
embebida, la 28 bloquea `2002::/16`, `64:ff9b::/96` y `::/96` como rangos
enteros. Cada suite fijaba la respuesta contraria, así que ninguna unión de sus
pruebas pasaba: exigía una decisión de seguridad, no un renombrado.

Sobrevive la forma de la 28 —interfaz, no clase estática— y la conducta más
restrictiva. El envío de webhooks queda **deliberadamente fuera** del interruptor
`app.connectors.allow-private-addresses`: esa válvula existe para el perfil de
extremo a extremo del calendario externo, y un webhook firmado no debe quedar a
su merced. La trampa de polaridad se evita **por tipo**: los consumidores reciben
un `AddressPolicy` en vez de un `Predicate` cuyo `true` significaba BLOQUEADA, de
modo que el compilador impide confundir los sentidos.

Esta colisión la encontró la auditoría previa (`progress/auditoria_colisiones_25_28_30.md`),
no el choque. Una fusión apresurada habría dejado la guarda contra SSRF invertida
—permitiendo solo direcciones privadas— sin que ningún test lo detectase.

### Rangos de Stryker: recalculados y con un defecto real corregido

Se recalcularon **todos** contra el `App.tsx` fusionado, con el oráculo de
contenido. Dos guardas (`ics calendar` y `appearance`) **ya fallaban en `main`
antes de esta integración**, comprobado ejecutándolas en un worktree sobre
`5b9937e`. Y salió un defecto que no es cosmético: el alcance de `export-data`
apuntaba a `use-session.ts:190:6-190:29` cuando el nodo congelado está en
`221:6-221:29`. Un rango desfasado no falla: muta el código equivocado y la
campaña lo bendice igual.

`scripts/project.test.mjs`: 91 de 91 en verde.

### Lo que NO está hecho, y es lo que falta para el 100 %

1. **Ninguna de las features 25, 26, 27, 28 y 30 está en `done`.** Siguen en
   `in_progress` y así deben quedarse. `done` exige juez aprobado **y** mutación
   sobre el umbral, y eso no se ha completado para ninguna salvo lo que sigue.
2. **Feature 26:** juez **APPROVED** en tercera lectura, con una condición
   explícita que **no** está cumplida: `calendar.tsx` cambió después de la
   campaña de 340/384 y está entero en el alcance, así que **hay que reejecutar
   `node scripts/project.mjs mutate ics_calendar-frontend`** sobre este árbol.
   Su campaña de backend nunca llegó a correr porque PIT aborta si la suite no
   está verde, y lo estuvo hasta hace poco.
3. **Feature 27:** su juez la rechazó, el artesano cerró los cinco bloqueantes y
   esa versión ya está integrada, pero **no ha vuelto a pasar por el juez**.
4. **Features 25, 28 y 30: sin juez y sin mutación.** El barrido de pre-juicio
   (`wu4x7ut0o`, 174 agentes) dejó hallazgos verificados que el juez encontrará
   igual; conviene leerlos antes de convocarlo. Para la 25, ocho bloqueantes,
   entre ellos que el zoom nativo al 200 % no se ejecuta nunca pese a que
   `features/webhooks.feature:518` lo exige, y que su oráculo de teclado es un
   umbral —«hubo más de cinco»— y no un recorrido.
5. **Feature 29, conectores adicionales: prácticamente sin empezar.** Su carril
   murió por el límite de sesión al poco de arrancar. La rama
   `claude/additional-connectors` existe, salida de `99d3e64`.
6. **La mutación nocturna sigue rota** por la causa ya diagnosticada, y su
   arreglo —matriz por objetivo derivando la lista de `scripts/project.mjs`— ya
   no choca con nada, porque las integraciones están hechas.
7. **`one_feature_at_a_time` está en `false`** en `harness.config.json`.
   Devuélvelo a `true` cuando el proyecto vuelva a un solo carril.

## Punto final de la sesión — 9 de septiembre de 2026

### Dónde queda el proyecto: 25 de 30 en `done`

La feature 26 (calendario ICS) se cerró con sus dos puertas: juez **APPROVED** en
tercera lectura y mutación **PASS, 345/390 = 88,46 %**. Es la única de las seis
últimas que ha completado el ciclo entero.

### Un aviso que ahorra horas: el proyecto vive dentro de OneDrive

La suite de backend falló dos veces seguidas con
`java.nio.file.NoSuchFileException` sobre el fichero de resultados **en curso** de
Gradle. No es un test roto: es que el fichero desaparece mientras Gradle escribe
en él. El repositorio está en `C:\Users\vhurt\OneDrive\...`, y el sincronizador
toca `build/` bajo los pies del proceso. La ejecución del arnés que reportaba «Hay
tests rotos» **sin nombrar un solo test roto** y dejando 3 informes en vez de
sesenta y pico encaja con lo mismo.

**CORRECCION del 9 de septiembre, tarde.** Esa atribucion a OneDrive NO esta
probada. El error se repitio tres veces, pero las tres hubo agentes ejecutando
`gradlew test` filtrado en el mismo directorio, y dos invocaciones de Gradle sobre
el mismo proyecto se pisan `build/test-results`. No he conseguido una ejecucion
limpia de verdad para separar las dos hipotesis.

Lo que si esta establecido: **el problema es LOCAL**. La CI sobre Linux nunca ha
mostrado este error; sus fallos han sido siempre reales y accionables. Asi que la
CI es la senal fiable y la suite local, con este sintoma, no lo es.

Antes de tocar OneDrive, prueba lo barato: ejecutar la suite completa con NADA mas
corriendo —ni agentes, ni otra invocacion de Gradle— y comprobar `docker ps` y los
procesos java a cero. Si aun asi falla, entonces si: excluye `backend/build/`,
`backend/.gradle/` y `frontend/node_modules/` de la sincronizacion, o saca el
repositorio de OneDrive.

### Una lección sobre los rangos de Stryker, aprendida a golpes

Se intentó validar automáticamente que todos los rangos `línea:columna` anclan en
fronteras de token. El validador marcó 37 rangos en ocho configuraciones, y al
«corregir» dos de ellas se rompió una guarda que estaba verde: la referencia
elegida era demasiado antigua y relocalizó a líneas sin sentido. Se revirtió.

La conclusión honesta: **la convención de columnas que asume ese validador
probablemente no es la de Stryker**, así que sus 37 avisos no están probados y no
deben tomarse como defectos. Lo que sí está probado es lo corregido con el oráculo
de contenido durante la fusión, verificado por las 91 guardas de
`scripts/project.test.mjs`, que están en verde.

### Lo que falta, por orden de cercanía

1. **Feature 27:** un solo bloqueante del juez, ya corregido en `d418a5d`. Le
   falta **volver a pasar por el juez** y su campaña de mutación.
2. **Features 25, 28 y 30:** integradas y verdes, **sin juez y sin mutación**. El
   workflow `wf_f417b520-44f` estaba produciendo la lista verificada de sus
   bloqueantes cuando terminó la sesión; si no llegó a registrarse, hay que
   repetirlo o convocar al juez directamente.
3. **Feature 29:** prácticamente sin empezar. La rama
   `claude/additional-connectors` existe, salida de `99d3e64`. Su contrato tiene
   477 líneas y dos mitades: el conector GitLab, que solo depende de la 27, y el
   catálogo de los seis conectores, que ya puede hacerse porque 25, 26, 27, 28 y
   30 están todas en `main`.
4. **La mutación nocturna** sigue rota y su arreglo ya no choca con nada.
5. **`one_feature_at_a_time` está en `false`.** Devuélvelo a `true` al volver a un
   solo carril.
