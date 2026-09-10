# Plan de cierre de las cuatro features — 10 de septiembre de 2026

Producido por el coordinador del panel de jueces, sobre los cuatro veredictos.

---

# PLAN ÚNICO DE CIERRE — features 25, 27, 28, 29

## Regla que gobierna todo el orden

Tres restricciones, y de ellas sale el plan entero:

1. **Una campaña a la vez, y ninguna campaña solapa con un carril.** Los carriles ejecutan vitest y gradlew; eso es máquina. Por tanto el día se parte en **ventanas de máquina** (campañas, en serie) y **ventanas de carril** (varios worktrees a la vez, cero campañas).
2. **Nunca se mide un árbol que va a cambiar.** Toda campaña de puerta va *después* del código. Corolario: hay **dos rondas** de medición, no una — una de diagnóstico (para saber qué supervivientes hay que matar) y una de cierre.
3. **El propietario no bloquea a nadie salvo el cierre de su feature**, pero sí bloquea al carril cuando la decisión es de contrato-que-cambia-código (25 SECRET_UNREADABLE, 29 B5). Esas dos van primero en su cola.

Estado verificado al arrancar: `feature_list.json` tiene 25, 27, 28, 29 y 30 en `in_progress` (nadie ha marcado nada por su cuenta). `progress/decisiones_pendientes.md` tiene **9 secciones**: 1 (f27), 2 (f29), 3-6 (f30), 7-8-9 (f25). **De la 28 no hay ni una**, confirmado. Y la 30 con sus 8 condiciones compite por la misma máquina: no la he metido en el plan, pero cuenta en la cola.

---

## FASE 0 — AHORA MISMO, con la campana de PIT viva (cero máquina)

Todo esto se hace mientras termina el PIT de la 28. **Los carriles NO arrancan en esta fase** (ejecutarían tests). Dos actores en paralelo:

### Propietario — 11 decisiones, ~230 min (cola priorizada)

Las dos primeras desbloquean carril; el resto sólo bloquean su propia feature.

| # | Feature | Decisión | Min | Desbloquea |
|---|---|---|---|---|
| P1 | 25 | Punto 9 — ¿se contrafirma la octava clase `SECRET_UNREADABLE` o se retira de producción? | 20 | **carril L1** |
| P2 | 29 | B5 — ¿`@s11` pasa a base de bucle local, o `GitlabApiBase` admite lista blanca autoalojada? | 30 | **carril L5** |
| P3 | 27 | Punto 1 ampliado: `:384` once → doce campos del recibo | 15 | cierre 27 |
| P4 | 27 | `TOO_SHORT` — fila nueva en `@s6` y en `additional_connectors.feature:118-121` | 25 | carril L2 |
| P5 | 25 | Punto 7 — `@s9`: ¿enmienda de las dos filas, o modo degradado real (+60 min de carril)? | 30 | cierre 25 |
| P6 | 25 | Punto 8 — `@s32`: plazo a 2500 ms, o cambiar el cableado del `@Scheduled` | 25 | cierre 25 |
| P7 | 25 | Nota de enmienda de `@s8:125-126` (`ownerId\|endpointId`) | 15 | cierre 25 |
| P8 | 29 | Punto 2 — `token_nonce` embebido en `token_ciphertext` | 20 | cierre 29 |
| P9 | 29 | C7 — contrafirma de la enmienda de `@s31` (401→403) fuera del carril que la escribió | 10 | cierre 29 |
| P10 | 29 | D-2 — ¿la 29 pinta el `status` del recibo? | 20 | cierre 29 |
| P11 | 28 | Contrafirma de las cuatro entradas del ciclo 4 | 20 | cierre 28 — **depende de O5** |

### Orquestador (yo) — 13 tareas de papel, ~325 min

Ninguna toca `src/` ni tests. Orden pensado para que O5 esté listo antes de la primera ventana de máquina.

**Bloque urgente (habilita Ronda A):**
- **O5** · 28 · Estrechar `frontend/stryker.external-calendar.config.json` (`App.tsx:65:16-66:38` y el tramo de `externalCalendar`, no `65:16-87:42` ni `102:10-161:7`), añadir `today.tsx:226-231` al `mutate`, y reescribir la guarda de `scripts/project.test.mjs` (`deepEqual` de 2104-2112 y el `/: null$/` de :2151). **40 min. Es precondición de la campaña A4.**
- **O1** · 28 · Cuatro entradas nuevas en `progress/decisiones_pendientes.md` (enmienda baf5ab1f, enmienda 78dca3a6, fila del proveedor que enmudece, sitio en `workspace.tsx`). **30 min. Precondición de P11.**
- **O2** · 29 · Cuatro secciones nuevas en `decisiones_pendientes.md` (B5, B10, B11, D-2). **25 min. Precondición de P2/P10.**

**Bloque de saneamiento documental (sin dependencias):**
- **O3** · 25 · `judge_webhooks_cierre.md`: quitar "la ÚNICA cláusula que el código incumple" (:86-87 y :233-241), recitar `webhooks.feature:320`, sustituir `JdkWebhookSender:123/209-211` por `:161-168`, anotar R4. **15 min**
- **O4** · 25 · Corregir línea 5 de `mutacion_webhooks_backend_medida.md` (SHA real `af581554`, con `git diff --stat 95cf64bf HEAD -- backend/` vacío como prueba). **10 min**
- **O6** · 25 · Registrar el **fallo sobre las ocho guardas `if (!aborted)`**: las acepto como equivalentes, condicionado a que la campaña real cierre el 80 % sin ellas. Si no, queda revocado. **10 min**
- **O7** · 27 · Consolidar supervivientes contra los 18 vigentes; marcar `mutation_github_connector.md` como caducado; corregir N3 (`lambda$finish$1:144` es el proveedor del `orElseThrow`, sigue NO_COVERAGE) y N5. **30 min**
- **O8** · 27 · `ux_github_connector.md:18-21` → 16 + 1 = 17 recorridos, citando CI 34477162444. **10 min**
- **O9** · 27 · Comentario de `V30__github_connector_ciphertext_bounds.sql`: mínimo real 12+5+16 = 33, CHECK en 29 como cota holgada a propósito. **10 min**
- **O10** · 27 · Matizar `mutacion_github_connector_backend_medida.md`: denominador limpio, numerador comparte verdugos con la 29. **10 min**
- **O11** · 28 · Fechar/tachar `current.md:190-201` (el 91,71 % se midió con `ApplicationConfiguration` dentro del ámbito). **10 min**
- **O12** · 28 · `mutacion_external_calendar_frontend_medida.md:13` — `ec0a3b8` no existe; poner el commit real (`0bf68914`). Y coordenadas: `build.gradle.kts:769`/`:37`, `project.test.mjs:2122`. Quitar el "MEDIDO" de B5 sin artefacto. **25 min**
- **O13** · 28+29 · Nota de `DeleteExternalCalendar` (no genera mutantes, no viola C3) + `a11y_conectores_29.md` (cuántos anchos caben de verdad en xvfb 1280) + copiar `.e2e-work/a11y-log/` a `progress/evidencia_a11y_29/`. **50 min**

---

## FASE 1 — RONDA A: medir la verdad (campañas, EN SERIE, ~175 min + lo que reste del PIT)

Nadie ejecuta nada más mientras corre cada una de estas.

| Slot | Qué | Comando | Min |
|---|---|---|---|
| **A0** | Dejar terminar el PIT de la 28 y **recomputar del `mutations.xml`** (global, `adapter.feed`, `adapter.logging`). Arbitra de paso `HttpCalendarFeed:203/:207` y comprueba que `:195/:196/:210/:223` no salen TIMED_OUT. **No relanzar nada.** | — (ya corre) | 40 |
| **A1** | Guarda del arnés con el ámbito estrechado (precondición de A4) | `node --test --test-name-pattern "external calendar Stryker" scripts/project.test.mjs` | 10 |
| **A2** | **27 frontend**, ámbito de cuatro ficheros, SHA de HEAD en la primera línea | `bin\harness.ps1 mutate github_connector-frontend` | 45 |
| **A3** | **28 frontend** con el ámbito nuevo (la cifra vieja 663/726 lleva 64 mutantes prestados de `App.tsx`) | `npx stryker run stryker.external-calendar.config.json` | 35 |
| **A4** | **29 frontend** — *diagnóstico*: hoy 68,51 %, hay 11,5 puntos que subir y hace falta la lista real de supervivientes sobre HEAD | `bin\harness.ps1 mutate additional_connectors-frontend` | 45 |
| **A5** | **25 frontend** — *diagnóstico, no puerta*: 71,40 % sobre árbol previo a `11e744d8`; se remide en Ronda B tras la octava clase | `node scripts/project.mjs mutate webhooks-frontend` | 40 |

**A2 es la única que puede salir ya como puerta definitiva** (el árbol de frontend de la 27 no ha cambiado desde `26a5a5eb`). Las demás son insumo de carril.

---

## FASE 2 — CARRILES EN PARALELO (5 worktrees, máquina sin campañas)

| Carril | Feature | Trabajo | Min | Espera a |
|---|---|---|---|---|
| **L1** | 25 | Octava clase en `errorClasses` (`webhooks-client.ts:22-30`) con rojo primero en `webhooks-client.test.ts` y reescritura de `:500`; envenenar de verdad `Slf4jWebhookAuditTest.s35_…` o borrar su javadoc; contador al resolutor de `CreateWebhookTest` (@s2/@s5) + garantía estructural de @s14 escrita | 85 | **P1** |
| **L2** | 27 | `github-connector-draft.test.ts` (3 ramas: propietario ajeno :39, repository no-cadena :40, JSON ilegible :33-35) + reforzar `github-connector.test.tsx:568`; 503 `GITHUB_UNAVAILABLE` en `GithubConnectorApiTest`; guarda del `setItem` de `github-connector-draft.ts:19-22`. **OJO: producción está bien, `key={owner}` en :29 sí remonta — falta la prueba, no un arreglo.** | 65 | P4 (sólo la fila TOO_SHORT) |
| **L3** | 28-front | Oráculo de `@s35` (que el doble de fetch guarde el cuerpo, afirmar `{onlyIfStale:true}`, acreditar el rojo con `false`); **C5 frontend**: veredicto por superviviente sobre la lista de A3 — matar `external-calendar.tsx:291`, `:236/:237`, `:114/:118/:122`, `today-external-calendar.tsx:105` y `:27`, y averiguar el RuntimeError de `:76` | 170 | **A3** |
| **L4** | 28-back | Escribir `progress/mutacion_external_calendar_backend_medida.md` con la cifra recomputada de A0 y la lista nominal; `unrestrictedHostTest` forkeada (snippet en `bloqueantes_external_calendar.md:454-472`); reserva de `HttpCalendarFeed:159` si `adapter.feed` no llega al 80 % | 140 | **A0** |
| **L5** | 29 | B10 (`correlationId` en `ConnectorAudit`, acuñado como en `ApiErrors.java:127`, + `GitlabTokenConfinementTest`); B6 (`$.lastActivityAt` por valor); `@s31` a las tres rutas de escritura; `@s15` con SELECT real de `task_external_links`; B11 `ratelimit-reset`; y matar los supervivientes de A4 | 175 + lo que pida A4 | **P2** |

**Turnos de Testcontainers:** L4 y L5 levantan PostgreSQL. No a la vez con L3 corriendo vitest pesado ni entre sí sobre la suite completa — filtrado por clase y por turnos, como ya está anotado en memoria.

Ventana de carril ≈ **175-220 min de reloj** (manda L5, o L3 si A4 saca muchos supervivientes en la 29).

---

## FASE 3 — RONDA B: campañas de cierre (EN SERIE, ~315 min)

| Slot | Qué | Min |
|---|---|---|
| B1 | `bin\harness.ps1 init` (condición 5 de la 27, "si sale rojo el veredicto decae") | 10 |
| B2 | **27 frontend** re-run sólo si L2 tocó el ámbito | 45 |
| B3 | **25 frontend**, cifra recomputada de `mutation.json` (Killed+Timeout / total), nunca del HTML | 40 |
| B4 | **28 frontend** re-medida tras C5 | 35 |
| B5 | **29 frontend** re-medida tras L5 | 45 |
| B6 | **28 backend** PIT re-run (L4 añadió `unrestrictedHostTest` y quizá `:159`) | 40 |
| B7 | **29 backend** PIT re-run (B6/B10 cambian producción) | 35 |
| B8 | Suites de oráculos: 6 clases de la 28 + `*Webhook*` + `Slf4jWebhookAuditTest` + `*Gitlab*`/`*PersonalAccessTokenTest`/`*GithubConnectorApiTest` | 65 |
| B9 | **`bin\harness.ps1 verify` consolidado, uno solo para las cuatro** (no cuatro verifies separados: ahorra ~60 min) | 40 |

---

## FASE 4 — Cierre

Orquestador, sin máquina: publicar `progress/mutation_webhooks_backend.md` y `progress/mutation_webhooks_frontend.md` **con esos nombres exactos** (hoy no existen; están `mutation_webhooks.md`, `mutacion_webhooks_backend_medida.md`, `mutacion_webhooks_frontend.md`, y hay que consolidar para que no queden dos verdades). Confirmar el fallo O6 contra la cifra real. Marcar `done` en `feature_list.json` sólo lo que tenga juez aprobado + mutación sobre umbral. **25 min.**

---

## CAMINO CRÍTICO

```
PIT 28 en curso (40) → O5 ámbito Stryker (40) → A1 guarda (10) → A3 medir 28 (35)
   → L3 C5 frontend, 6 supervivientes (170) → B4 re-medir 28 (35) → B9 verify (40)
```
**≈ 370 min ≈ 6 h 10 min**, y va por la **28**, no por la 25 ni la 29.

El camino crítico de la 29 corre casi a la par: `O2 (25) → P2 B5 (30) → L5 B10+resto (175) → B5/B7 (80) → B9`. Si el propietario tarda en P2, la 29 pasa a ser el crítico.

Lo que **no** está en el camino crítico y por tanto no hay que apurar: todo el bloque documental O3-O13 (~180 min) y las decisiones P3, P5-P10.

**Suma de tiempo de máquina, en serie: 40 (A0) + 175 (Ronda A) + 315 (Ronda B) ≈ 530 min ≈ 8 h 50 min, en 15 ventanas.** Si se aceptan tres atajos legítimos —saltar A5 (diagnóstico de la 25), saltar B2, y no re-medir el backend de la 29 si L5 no toca producción— bajan a **≈ 7 h**. Y hay que reservar hueco aparte para las 8 condiciones de la 30, que compiten por la misma máquina.

---

## LA QUE ESTÁ MÁS CERCA DE CERRAR: **27-github-connector**, y no está reñido

Es la única de las cuatro cuya **puerta de backend ya está pasada, medida y recomputable**: 439 mutantes, 419 KILLED + 2 TIMED_OUT = 421, **95,90 % exacto**, con 33 clases mutadas y cero de GitLab en el denominador — y corresponde al árbol que se cierra (`git diff 26a5a5eb..HEAD -- backend/src backend/build.gradle.kts frontend/src e2e features` sale vacío). Los cinco defectos de producto (M5, M7, M10, M11, M12) están cerrados con oráculo, la CI está verde sobre `9d1e7d2e` incluyendo `e89e71ef`, y el bloqueante del veredicto anterior está levantado. **No necesita ningún cambio de producción.** Su carril son 65 minutos de pruebas y su máquina son dos ventanas.

### Lista exacta de lo que le falta a la 27

**Bloqueante (6):**
1. **[campana, 45 min]** Relanzar Stryker con el ámbito de cuatro ficheros: `bin\harness.ps1 mutate github_connector-frontend`. El informe vigente es de las 10:55:31 con tres ficheros y 589 mutantes; `github-connector-draft.ts` (creado 12:52, metido en el ámbito a las 13:36) tiene **cero mutantes medidos** y `github-connector.tsx` se reescribió después. El informe nuevo debe declarar SHA de HEAD, total, puntuación contra break 80, ningún módulo a cero, y **re-anclar la condición 3 del juez a `github-connector.tsx:161-173`** (el escuchador de Escape ya no está en :149-159).
2. **[carril, 35 min]** `frontend/src/github-connector-draft.test.ts` con las tres ramas defensivas (`:39` propietario ajeno, `:40` repository no-cadena, `:33-35` catch de JSON ilegible), afirmando `""` y clave borrada; y reforzar `github-connector.test.tsx:568` (404 → formulario, teclear `octocat/Hello-World`, remontar con `owner="otra-persona"`, `toHaveValue("")`). **Producción está sana** — `github-connector.tsx:29` lleva `key={owner}`.
3. **[propietario, 25 min]** Contrafirmar `TOO_SHORT` en `decisiones_pendientes.md` con la fila redactada (`| token de 4 caracteres | 400 | VALIDATION_ERROR | TOO_SHORT |`) y el motivo (la pista son los 4 últimos caracteres en claro en `token_hint`); luego la fila en `github_connector.feature:94-105`, la equivalente en `additional_connectors.feature:118-121`, y la fila de prueba en `GithubConnectorApiTest`.
4. **[propietario, 15 min]** Ampliar el punto 1 de `decisiones_pendientes.md` a `github_connector.feature:384` (once → doce campos) y contrafirmar la enmienda completa.
5. **[orquestador, 30 min]** Consolidar la documentación de supervivientes contra los 18 vigentes, marcar `mutation_github_connector.md` como caducado, y corregir N3 y N5 (los dos siguen NO_COVERAGE).
6. **[orquestador, 10 min]** `bin\harness.ps1 init` verde, con la salida pegada en `progress/`.

**No bloqueante (5):** 503 `GITHUB_UNAVAILABLE` en `GithubConnectorApiTest` [carril 20]; `ux_github_connector.md:18-21` → 17 recorridos [orq 10]; comentario de la V30 → 33 octetos [orq 10]; guarda del `setItem` de `github-connector-draft.ts:19-22` [carril 10]; matizar la frase del ámbito en `mutacion_github_connector_backend_medida.md` [orq 10].

**Orden de las otras tres, por riesgo residual, no por volumen:** 25 (71,40 % → 8,6 puntos de carril desconocido, 4 decisiones de propietario), 28 (mucho trabajo pero mecánico; su frontend ya sale 90,48 % sin los mutantes prestados) y 29 la última (68,51 %, el peor número, más un hueco de implementación real en B10 y dos pulsos de contrato sin resolver).

---

## PENDIENTE DE EJECUCIÓN — nada de esto lo he corrido

Todo lo anterior sale de lectura de ficheros y git de sólo lectura. Las 15 ventanas de máquina de las fases 1 y 3 quedan **declaradas como pendientes de ejecución** con su comando exacto en las tablas. La primera que puede dispararse, en cuanto la campana viva libere la máquina, es **A0** (recomputar el `mutations.xml` de la 28, que no requiere relanzar nada).
