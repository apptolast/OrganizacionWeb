# Estado actual — 9 de septiembre de 2026, sesión de tarde con plazo duro

El estado anterior, con toda la historia del 8 y del 9 por la mañana, está
archivado en `progress/sesion_2026-09-08_09.md`. Este documento lo sustituye.

**Instrucción del usuario:** «tienes solo esta sesión, la siguiente la quiero
para otro proyecto, llevo como 7 días con esto» y, a mitad de sesión, «la tienes
que terminar todo en 1 hora y 20 minutos, que es cuando se resetea la sesión y
te voy a cortar». Plazo: 20:04 → 21:24 (Madrid).

## Lo que se cerró de verdad en esta sesión

### 1. La CI de `main` estaba roja, y no por el producto

`bin/harness init` fallaba en el paso de lint: Prettier señalaba formato en ocho
`frontend/stryker.*.config.json`. Todo lo demás estaba verde —2840 pruebas de
frontend pasaban en la misma ejecución—. Corregido en `9696109`, comprobando
que el JSON resultante es **idéntico** al anterior: solo cambia el formato.

Fallo distinto y aparte, del mismo día: la ejecución `34386220549` murió en
`playwright install --with-deps chromium` con `Some index files failed to
download` y código 100. Es la red del runner de GitHub, no el repositorio.

### 2. Ninguno de los cuatro workflows está parado ni colgado

`Application CI`, `Prueba de mutación`, `Guardián de rutas sensibles` y
`Evolución autónoma del arnés`. Los tres últimos no han fallado nunca. De las
últimas 47 ejecuciones de `Application CI`, 13 fallaron, y todas por dos causas:
el lint de arriba y el flake de exportación de abajo. No hay ninguna ejecución
en cola ni encallada.

### 3. El flake de exportación, que bloqueaba la feature 24

Integrado `claude/ci-export-flake`. El test pedía los bytes con
`response.body()`, que se los pide a la caché del inspector de Chromium; bajo
carga esa caché ya ha desalojado el cuerpo que la página consumió en streaming,
y salía `Protocol error (Network.getResponseBody)`. Ahora los bytes se capturan
en el **transporte**, con `page.route` y `route.fetch()`, y de paso se afirman
`content-type`, `content-length` y `content-disposition` del transporte, y que
la petición se hace **una sola vez**.

Con esto queda cerrada la condición 2 de las tres del dictamen de la feature 24.

### 4. El hueco de mutación de la rama SYSTEM del tema

Integrado `claude/darkmode`: oráculo para la rama SYSTEM del pintado del tema en
`appearance-state.tsx`, más la simplificación que la prueba deja demostrar.

### 5. La puerta `Stop` estaba saboteando la sesión

`.claude/settings.json` tenía un hook `Stop` que ejecuta `node
.harness/harness.mjs init` —la suite **completa**, con sus 49 clases de test con
`PostgreSQLContainer` estático— al final de **cada turno del orquestador**. En
CI ese paso tarda 14 minutos. Con varios carriles en paralelo es exactamente lo
que colapsó la máquina el 8 de septiembre, y es hermano del hook `PostToolUse`
que ya se retiró por lo mismo.

Aparcado en `hooks_disabled_during_parallel_lanes` mientras dura la fase
paralela, con la nota del porqué dentro del propio fichero. **Hay que
restaurarlo** cuando se acabe de trabajar en paralelo; la verificación se
ejecuta explícitamente al integrar.

## Los cinco carriles

Worktrees en `C:/Users/vhurt/ow-worktrees`, ramas `claude/<nombre>` desde `main`.
Reglas de operación en `progress/carriles/REGLAS.md` (nunca la suite completa,
backend por clase concreta, un `E2E_WEB_PORT` por carril, rojo demostrado,
commit por ciclo).

El dictamen del juez está partido por feature para que cada carril lea solo lo
suyo: `progress/carriles/dictamen_f25.md`, `dictamen_f28.md`, `dictamen_f30.md`.

| Carril | Feature | Puerto E2E | Encargo |
|---|---|---|---|
| A | 25 webhooks | 18090 | 15 hallazgos abiertos del dictamen, 5 bloqueantes |
| B | 28 calendario externo | 18092 | 15 hallazgos abiertos, 5 bloqueantes, más el plazo de lectura del cuerpo del feed |
| C | 30 automatizaciones | 18094 | 10 hallazgos abiertos, 4 bloqueantes |
| D | 27 conector GitHub | 18096 | revalidar el único bloqueante del juez, ya corregido en `d418a5d` |
| E | 29 conectores adicionales | 18098 | terminar el ciclo a medias, inventario de oráculos por escenario |

## Lo que NO cabía en el plazo, dicho sin adornos

El dictamen `progress/dictamen_final_25_28_30.md` confirmó **53 hallazgos, 19 de
ellos bloqueantes**. Se cerraron 13 antes de esta sesión (los once «de minutos»
más las tres puertas de mutación). Quedaban **~40 abiertos**, y entre ellos hay
piezas de producto, no de prueba:

- **Feature 30, bloqueante 1: no existe el ejecutor de reglas.** La feature se
  integró como «fase 1» —reglas, plantillas, simulación, auditoría y UI— sin el
  motor que dispara las reglas ante eventos reales. Nueve escenarios completos
  del contrato aprobado no tienen ningún oráculo porque no hay nada que probar.
  Es trabajo de horas. En esta sesión se ha ordenado **no empezarlo** y dejar en
  su lugar el inventario preciso de esos nueve escenarios, porque un ejecutor a
  medias vale cero y el inventario sí permite retomarlo.
- **Feature 25, bloqueante 5 y feature 28, bloqueante 6:** el cierre del
  rebinding DNS. En webhooks no está implementado aunque el código afirma que
  sí; en el calendario externo la guardia descarta la dirección ya validada y
  vuelve a conectar por nombre, justo lo contrario de lo que exige la enmienda
  B3 aprobada. Son defectos de seguridad reales, no de oráculo.
- **El zoom nativo del navegador al 200 %** no se ejecuta en ninguna de las tres
  features, aunque los tres contratos lo nombran. Hay precedente que copiar en
  `e2e/github-connector-native-zoom.spec.mjs`.
- **Feature 29** tiene 18 commits de conector GitLab y sigue en `spec_ready`:
  falta el inventario de qué escenarios tienen oráculo y cuáles no.

## Puertas que siguen sin cumplirse

Ninguna feature se marca `done` sin juez aprobado y mutación sobre 0,80. Hoy:

- **24 integration_api**: juez APPROVED, tres campañas de mutación por encima
  del umbral. Le faltaba CI verde; el flake que la tumbaba ya está corregido.
- **25, 28, 30**: integradas en `main`, con dictamen del juez **con
  bloqueantes abiertos**. No pueden cerrarse.
- **27 github_connector**: juez REJECTED por un solo defecto ajeno al artesano
  —una aserción de E2E que asumía 49 octetos de texto cifrado cuando la
  unificación de `SecretCipher` los dejó en 48—. El defecto está corregido en
  `d418a5d`; falta la **evidencia ejecutada** que el juez exige para levantarlo.
- **29 additional_connectors**: `spec_ready`.

Auditoría de coherencia hecha de paso, y **cerrada en falso positivo**: una
búsqueda por nombre de fichero no encontraba puerta para 14 `start_work_session`
ni para 15 `pause_resume_session`. Las dos la tienen, con otro nombre. Detalle
en `progress/auditoria_puertas_14_15.md`:

- 14: `judge_start_work_final.md:3` APPROVED; PIT 340/347 = 97,98 %
  (`mutation_start_work_backend.md:3`); Stryker 483/539 = 89,61 %
  (`mutation_start_work_frontend_final.md:3`); ámbito declarado en
  `frontend/stryker.start-work-session.config.json` y en `scripts/project.mjs`.
- 15: `judge_pause_resume_final.md:3` APPROVED; PIT 523/525 = 99,62 %; Stryker
  741/861 = 86,06 %, y 738/861 = 85,71 % tras descontar de forma conservadora
  tres muertes atribuidas a un fixture inestable
  (`review_pause_resume_mutation_frontend.md:21`), que sigue por encima de 80.

La causa era de nomenclatura: `CLAUDE.md:64` fija el patrón
`progress/judge_<name>.md` y `progress/mutation_<name>.md`, y los agentes
añadieron sufijos `_final`, `_backend` y `_frontend` porque cada feature tuvo
campañas separadas por capa. **Las 25 `done` tienen sus dos puertas.** Conviene
unificar los nombres, o el próximo barrido volverá a dar el mismo susto.

## Despliegue: sigue bloqueado por acceso, y no por trabajo

Sin cambios respecto al 9 por la mañana. `admin@159.195.156.57` responde
`Permission denied (publickey)`; no hay alias del servidor en `~/.ssh/config`;
los workflows de `apptolast/DockerSwarmInfrastrcture` solo validan, no aplican.
La PR 40, `codex/integration-release`, sigue en **borrador** con su validación
verde, esperando que el propietario facilite clave o aplique él mismo.
«Desplegado» es una puerta distinta de `done` y no se declara sin evidencia.

## Lo primero que hay que hacer al retomar

1. Restaurar el hook `Stop` en `.claude/settings.json`
   (`hooks_disabled_during_parallel_lanes` → `hooks`) y ejecutar
   `bin/harness init` una vez, sin carriles en paralelo.
2. Leer las bitácoras de los cinco carriles en `progress/` para saber qué
   hallazgo quedó cerrado y cuál no.
3. Atacar por este orden: el rebinding DNS de 25 y 28 (seguridad), el ejecutor
   de reglas de 30 (producto), el resto de oráculos, y la feature 29.
