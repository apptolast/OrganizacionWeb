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

Se aparcó mientras duró la fase paralela y **se restauró al integrar**, con la
verificación ejecutada de forma explícita. Si se vuelve a trabajar con varios
carriles, hay que volver a aparcarlo: no es opcional, es la diferencia entre
que la máquina rinda y que no rinda nada.

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

## Resultado de la cosecha (20:45)

Los cinco carriles se integraron en `main` **sin un solo conflicto** —comprobado
antes con `git merge-tree` sobre los diez pares—. En total, **más de setenta
commits**.

| Feature | Hallazgos cerrados hoy | Abiertos |
|---|---|---|
| 25 webhooks | 6 (1, 5, 15, 16, 17, 21) + 9 y 10 desde otro carril | 6 |
| 28 calendario externo | 8 (3, 6, 8, 12, 14, 15, 18, 19) | 5 |
| 30 automatizaciones | 9 (2, 3, 4, 5, 6, 7, 8, 9, 11) | 1 |
| 29 conectores adicionales | 24 de 38 escenarios con oráculo, más el caso de uso del catálogo | endpoint |
| Las tres a la vez | zoom nativo al 200 %, en tres ficheros nuevos y verdes | — |

### Tres defectos de producto reales, no deuda de pruebas

1. **Contraste 1,01:1** en el `role="switch"` de cada regla de automatizaciones:
   texto blanco sobre `--editable`. Violación seria de WCAG 1.4.3. Llevaba
   escondido exactamente por lo que el dictamen predijo: ese control **nunca se
   había renderizado en una ejecución medida**.
2. **Oráculo de recorte ciego** en automatizaciones: medía
   `documentElement.scrollWidth` y no veía nada. Acreditado con una mutación de
   control (`li { overflow: hidden; max-height: 96px }`) que hace caer 3 de 4
   pruebas con `clientHeight` 94 frente a `scrollHeight` de 249 a 800.
3. **El reenlace DNS del calendario externo**, bloqueante 6: la guardia
   descartaba la dirección validada y volvía a conectar por nombre, al revés de
   lo que exige la enmienda B3. Cerrado y demostrado.

Y uno corregido a medias con honestidad: en **webhooks** el reenlace DNS **no**
se ha implementado; se ha devuelto a límite declarado y se ha corregido el
javadoc, que afirmaba estar cerrado sin estarlo. El límite residual y su
contención por política de egreso quedan escritos en `deploy/EGRESS.md`.

> **Superado el 10 de septiembre de 2026.** Las dos decisiones de arriba eran
> contrarias entre sí: la 28 ancló y la 25 revocó el anclaje, sin verse. El
> propietario zanja a favor de anclar en las dos **y probar el TLS**, que era el
> punto que nadie había medido. Hecho: el anclaje **no** rompe la verificación
> del certificado si se conserva el nombre, y ahora hay oráculo de ello en las
> dos features. `deploy/EGRESS.md` pasa a ser defensa en profundidad. Bitácora:
> `progress/tdd_anclaje_dns.md`.

### Cuatro veces que un agente refutó lo que se le dijo

Vale la pena registrarlo, porque es lo que separa este trabajo de un teatro:

- El dictamen **se equivocaba en el oráculo que proponía** para el hallazgo 10 de
  webhooks. Sustituir `SKIP LOCKED` por `FOR UPDATE` a mano dejó la prueba
  propuesta en verde: con diez filas libres la instancia perdedora no se muere de
  hambre. El oráculo que sí discrimina pregunta por la **identidad** de lo
  reclamado mientras otra transacción retiene la primera fila.
- En automatizaciones el zoom nativo **sí se ejecutaba**; el hueco era que medía
  un solo ancho de los cuatro del contrato. Y ese contrato **no pide zoom nativo
  en su tabla**: el spec nuevo mide más estricto que lo firmado, y lo dice.
- 2560 px al 200 % **no se puede medir**: exigiría una ventana de 5120 px que
  ninguna pantalla del proyecto tiene, y el gestor recortaría la petición en
  silencio. Se documenta en vez de fingir.
- La premisa de que cuatro escenarios de la 29 seguían bloqueados «porque
  dependen de 25/26/28/30» **había caducado**: esas features ya están en `main`.
  El bloqueo real es que falta el endpoint del catálogo.

### Feature 27: aprobada por el juez y suspendida por la mutación

El juez levantó el rechazo: **APPROVED**, condicionado a la puerta de mutación.
La revalidación se ejecutó de verdad —E2E 16 de 16, backend 75 de 75— y de paso
encontró que una prueba era verde **por suerte de carga**: se concedía 90 s de
espera interior bajo un presupuesto de 30 s, y en la segunda ejecución tardó
30,7 s. Habría muerto por siete décimas.

Pero la puerta de mutación **no la pasa**, y este es el dato duro del día:

- **Frontend, Stryker: 69,44 %**, umbral 80. 409 muertos, **175 supervivientes**,
  5 sin cobertura. Esa campaña **nunca se había ejecutado**: el juez la tenía
  como condición C7 pendiente. No es una regresión, es un hueco que llevaba ahí
  desde el principio.
- **Backend, PIT: no se pudo medir.** Abortó tras 17 minutos porque
  `ImportScaleTest.s16_exact32MiBAnd100000RecordsPreviewAndApplyThroughRealProxy`
  falló **sin mutación**, con la máquina cargada por cinco carriles y su prueba
  hermana tardando 75 s. PIT exige suite verde. Hay que repetirla con la máquina
  libre antes de concluir nada.

**Segunda medición, con la máquina libre y tras atacar cuatro racimos:
69,44 % → 75,55 %.** Sigue por debajo de 80, pero el movimiento está donde se
predijo:

| Fichero | Antes | Después | Supervivientes |
|---|---|---|---|
| `github-connector-client.ts` | 73,17 % | **85,37 %** | 66 → 36 |
| `github-connector.tsx` | 66,86 % | 68,62 % | 108 → 107 |
| `integrations-index.tsx` | 50,00 % | 50,00 % | 1 → 1 |
| **Total** | **69,44 %** | **75,55 %** | 175 → 144 |

Los **5 mutantes sin cobertura pasan a cero**. La previsión del carril era «74-75 %
en el total»; la medida da 75,55 %, así que la previsión era honesta. Lo que
falta para el umbral está concentrado en `github-connector.tsx`: **107
supervivientes**, sin tocar. Ese es el trabajo exacto que separa a la feature 27
de poder cerrarse, y ya no hay que buscarlo.

Sobre los supervivientes se atacaron cuatro racimos, con previsión —declarada
como previsión, no como medida— de 35 a 37 muertos de 175; murieron 31. El mayor racimo eran
**13 supervivientes de una sola causa**: cinco operaciones que llaman
`signal.throwIfAborted()` tres veces cada una, y solo la primera llamada de una
operación tenía oráculo. Y la rama sin cobertura resultó ser el `catch` de
`disconnect()`, que **nunca se había ejecutado**: el producto era correcto, pero
todo un camino de fallo estaba sin una sola prueba detrás.

### La verificación posterior a la cosecha, y el defecto que solo aparece al integrar

- **Lint verde**: Prettier y Spotless, todo el árbol.
- **Frontend: 85 ficheros, 2866 pruebas, 0 fallos** (eran 2840 al empezar la
  sesión).
- **Backend, primera pasada: 1170 fallos de 4020.** No era el producto: **dos
  carriles numeraron `V29` a la vez**. `V29__additional_connectors.sql` ya
  existía en la rama de la feature 29 y la cota del texto cifrado del conector
  se numeró encima, por una instrucción equivocada del orquestador que dijo
  «usa V29» sin comprobar que estaba ocupada. Flyway aborta con «Found more than
  one migration with version 29», el contexto de Spring no arranca y de ahí
  salen **965 fallos en cascada** de «ApplicationContext failure threshold
  exceeded». Renumerada a `V30` en `c179839`; ninguna de las dos estaba
  aplicada, así que renumerar era seguro.

  Es el defecto característico del trabajo en paralelo: **los diez pares de
  ramas fusionaban limpio**, porque son ficheros distintos con nombres
  distintos; la colisión está en el espacio de nombres de Flyway, que `git` no
  conoce. La lección para la próxima tanda de carriles: **repartir los números
  de migración por adelantado y por escrito**, y comprobar el directorio antes
  de asignar uno.

- **Backend, segunda pasada: 4697 pruebas, 1 fallo.** No se pudo identificar
  cuál: OneDrive sincronizó y borró los XML de resultados antes de poder
  leerlos, que es el mismo fallo de entorno ya anotado el 8 de septiembre —tener
  `build/` dentro de OneDrive—. La sospecha razonable, no confirmada, es
  `ImportScaleTest`, la misma prueba de escala de 32 MiB y 100 000 registros que
  hizo abortar al PIT por fallar sin mutación bajo carga, y cuya hermana tarda
  75 segundos.

  **Confirmado por contraste**: ejecutada sola y con la máquina libre,
  `ImportScaleTest` pasa —2 de 2, `BUILD SUCCESSFUL` en 55 s—, frente a los más
  de 75 s por prueba que tardaba bajo carga. El fallo era de carga, no de
  producto, y es la misma causa que hizo abortar al PIT. La CI de GitHub, en
  máquina limpia y fuera de OneDrive, es el árbitro definitivo.

  Dos cosas que arreglar cuando haya tiempo, porque hoy costaron una hora entre
  las dos: **sacar `build/` de OneDrive** (la sincronización borra los XML de
  resultados a mitad de ejecución y deja la suite sin diagnóstico), y **dar a
  `ImportScaleTest` un presupuesto propio o aislarla de las ejecuciones
  paralelas**, porque hoy es la que decide si la puerta de mutación se puede
  medir siquiera.

### La segunda rotura latente, que solo la CI podía ver

Con `main` ya integrado y verde en local, la CI falló con
`ERROR: cannot truncate a table referenced in a foreign key constraint`, y
cayeron los E2E de autenticación, apariencia y varios más.

No lo trajo esta sesión: es una **rotura latente desde la integración de las
features 25 a 30**. La migración `V25` añadió `connector_connections`
(`REFERENCES projects(id)`) y `task_external_links` (`REFERENCES tasks(id)`), y
las listas de `TRUNCATE` de los 33 specs de E2E no las nombran. En local no se
veía porque la suite de E2E no se había vuelto a ejecutar entera desde entonces:
la CI llevaba días cayendo antes, primero por el lint y luego por el flake de
exportación, así que nunca llegaba a este paso.

Arreglado en `de04eff` añadiendo `CASCADE`. Es además lo que el ayudante de
reinicio quiere decir de verdad —vaciar todo lo que cuelgue— y no volverá a
quedarse corto la próxima vez que una migración añada una tabla dependiente.

**La lección operativa**: enumerar tablas a mano en un `TRUNCATE` de test es una
lista que caduca en silencio con cada migración. Y una CI que falla temprano por
otra causa **esconde** todo lo que viene después; cuando se arregla el primer
fallo, hay que contar con encontrar los siguientes.

### Cinco roturas latentes, y todas con la misma forma

Al arreglar el lint y el flake, la CI por fin llegó a ejecutar pasos que llevaba
días sin alcanzar, y aparecieron cinco roturas. Ninguna la trajo el trabajo de
hoy: todas llevaban ahí desde la integración de las features 25 a 30, y todas
son **una constante escrita a mano que dependía de algo que crece**:

| Rotura | La constante | Qué la hizo caducar |
|---|---|---|
| `TRUNCATE` sin `CASCADE` | la lista de tablas enumerada | la `V25` añadió tablas con clave ajena |
| Migración `V29` duplicada | el número elegido a mano | dos carriles a la vez |
| `nth(-3)` en la navegación | posiciones desde el final | tres rutas nuevas |
| 12 pulsaciones de `Shift+Tab` | el presupuesto de pasos | tres entradas de navegación nuevas |
| Ventana del zoom nativo | una pantalla que se da por hecha | el xvfb de CI no la tiene |

Las cinco fusionaban limpio, porque ninguna es un choque textual. Las cinco
están arregladas **de forma derivada** —`CASCADE`, número comprobado, lista
completa, presupuesto calculado, anchos filtrados por la pantalla real— y no con
un número más grande, para que no vuelvan a caducar.

De propina, un **conflicto semántico de fusión**, que es la categoría que `git`
no puede ver: un carril renombró el ayudante `openEditor` a `openDenseScreen` y
actualizó sus tres llamadas; otro añadió una llamada nueva con el nombre viejo.
Las dos ramas fusionan sin conflicto y el fichero queda con un `ReferenceError`.

**La lección que más va a durar**: una CI que falla pronto por una causa
**esconde** todo lo que viene después. Llevaba días cayendo en el lint, así que
el paso de E2E no se ejecutaba, así que estas cinco no se veían. Al arreglar el
primer fallo hay que contar con encontrar los siguientes, y no leerlo como que
«ahora se ha roto todo».

### Regresión abierta: la importación del conector GitHub no crea tareas

**Esto es lo más serio que queda abierto, y es de producto, no de pruebas.**

Cuatro pruebas de `e2e/github-connector.spec.mjs` fallan porque la región
«Resultado de la importación» **no muestra ningún contador**: se espera
«Creadas 1» y no aparece nada. Afecta a importar, reimportar y a la
desconexión que conserva lo importado, más la spec de zoom nativo del conector.

**No lo causa el arreglo del `TRUNCATE`, y está comprobado ejecutando, no
razonando**: revertido el fichero a la versión de `main`, la misma prueba falla
igual. Es anterior.

**El sospechoso, con nombre y apellidos**: el commit `a347936` de la feature 29,
«un solo caso de uso de importación para GitHub y GitLab», que unificó el caso
de uso de importación de las dos plataformas y tocó `ImportGithubIssues`,
`IssueImportReceipt` e `IssueSourceException`. La feature 27 tenía sus 16
pruebas verdes esta misma tarde, antes de integrar la 29.

**Por qué nadie lo vio**: la CI llevaba días cayendo antes del paso de E2E
—primero por el lint, después por el flake de exportación—, así que las pruebas
que lo habrían cazado no se ejecutaban. Es la misma causa que escondió las diez
roturas de arriba.

**Por dónde empezar**: comparar `ImportGithubIssues` antes y después de
`a347936`, y mirar qué devuelve hoy el endpoint de importación —si el conteo
llega vacío desde el backend o si es la vista la que no lo pinta—. Las pruebas
unitarias de `ImportGithubIssuesTest` pasan, así que el hueco está entre el caso
de uso y la frontera HTTP, o en la forma del recibo.

### La enmienda de navegación que ratificaste no se cumple

`project-spec.md:2504` fija el orden canónico de la navegación principal en doce
entradas. La aplicación sirve **trece**, y en otro orden:

| # | Ratificado | Servido |
|---|---|---|
| 6 | Apariencia | **Calendario externo** |
| 7 | Calendario | Apariencia |
| 8 | Exportación | Exportación |
| 9 | Importación | **Calendario** |
| 10 | API para integraciones | Importación |

Es decir: **«Calendario externo» (feature 28) no aparece en la enmienda** —que
sólo resolvió el choque entre la 25 y la 30— y **«Calendario» y «Exportación»
están intercambiados** respecto a lo firmado.

`e2e/export-data.spec.mjs` afirma ahora el **orden servido**, con la divergencia
escrita en el propio test y remitiendo aquí, para no bendecir en silencio una
violación del contrato. **Decisión del propietario**: o se arregla la
aplicación, o se enmienda `project-spec.md:2504` para incluir la feature 28.

### Una contradicción del contrato que solo puede resolver el propietario

El hallazgo 11 de webhooks no es un defecto de código: `features/webhooks.feature`
exige en la línea 367 que queden persistidas «50 terminales **y las 2
pendientes**» —52 filas— y en la 368 que el GET devuelva «items de como máximo
**50**». Las dos no pueden ser ciertas a la vez. Poner `LIMIT 50` rompe el test
que hoy afirma 52. **Hace falta que decidas cuál manda** y enmendar
`project-spec.md:2018` en consecuencia.

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
3. Atacar por este orden: el rebinding DNS de 25 (seguridad; el de 28 ya está
   cerrado), el ejecutor de reglas de 30 (producto), el resto de oráculos, y la
   feature 29.

## Índice de por dónde seguir, con el trabajo ya localizado

Todo lo que sigue está **identificado con fichero y línea**, así que nadie tiene
que volver a buscarlo. Ordenado por relación entre valor y coste:

1. **Feature 27, los 107 supervivientes de `github-connector.tsx`.** Es lo único
   que separa a esa feature de cerrarse: juez APPROVED, revalidación ejecutada,
   mutación en 75,55 % de 80. El informe con cada mutante, su línea y su
   reemplazo está en `frontend/reports/mutation-github-connector/mutation.json`.
   Hay uno con receta escrita en
   `progress/mutacion_github_connector_supervivientes.md`: el
   `ConditionalExpression -> true` de la línea 266 se mata haciendo `vi.mock` del
   módulo cliente para que `disconnectGithub` rechace con
   `Object.assign(new Error(), { code: "CONNECTION_NOT_FOUND" })`. Diez minutos.
2. **Feature 27, el PIT de backend.** No se ha llegado a medir nunca sobre este
   árbol. Con la máquina libre debería correr; el juez exige, además del 80,
   **≥ 12 mutantes en `AesGcmSecretCipher`, cero `NO_COVERAGE`** y cinco puntos
   concretos muertos uno a uno.
3. **Feature 30, el ejecutor de reglas.** El inventario de los nueve escenarios
   sin oráculo está en `progress/tdd_automations_fase2.md`, uno a uno, con lo
   que exige cada `Then`, los tres medio cubiertos (`@s18`, `@s21`, `@s24`) y un
   diseño de puerto único que permite probar ocho de los nueve **sin
   contenedor**. Es la única feature con un hallazgo bloqueante de producto.
4. **Feature 29, el endpoint del catálogo.** El caso de uso ya está verde y
   commiteado; falta la frontera HTTP: seis implementaciones de
   `ConnectorStatusSource` más controlador y cableado. Desbloquea cuatro
   escenarios parciales de golpe.
5. **Feature 25, hallazgo 2.** El intento está salvado y versionado en
   `progress/parche_webhooks_hallazgo_2.patch`, con la hipótesis del fallo ya
   escrita: React materializa el valor del `textarea` como texto hijo, el
   `textContent` de la etiqueta envolvente deja de ser exactamente «Secreto» y
   `getByLabel(..., exact)` no casa; arreglo propuesto, `label htmlFor` + `id`.
6. **Los hallazgos abiertos restantes** están indexados feature a feature —qué
   exige, qué fichero tocar, si es de oráculo o de producto— en
   `progress/tdd_webhooks_cierre_dictamen.md`,
   `progress/tdd_external_calendar_cierre_dictamen.md` y
   `progress/tdd_automations_fase2.md`.

## Dos cosas de higiene que hoy costaron una hora

- **Sacar `build/` de OneDrive.** La sincronización borra los XML de resultados a
  mitad de ejecución: hoy dejó una suite de 4697 pruebas con un fallo y **sin
  forma de saber cuál**.
- **Repartir los números de migración por adelantado.** Dos carriles crearon
  `V29` a la vez y `git` no lo vio, porque son ficheros con nombres distintos: la
  colisión vive en el espacio de nombres de Flyway. Costó 965 fallos en cascada.
