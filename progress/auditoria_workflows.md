# Auditoría de los workflows — 11 de septiembre de 2026

Encargo: «alguno se ha quedado parado o en error o en estado de parada, si es así
soluciónalo». Se auditaron los cuatro workflows con agentes en paralelo y cada
arreglo propuesto pasó por tres escépticos independientes que reejecutaron las
citas. Lo que sigue son sólo los hechos que sobrevivieron a esa reejecución.

## Cuadro

| Workflow | Estado | Veredicto |
| --- | --- | --- |
| Application CI | 🟢 verde | Arreglado hoy (run 34532913927) |
| Prueba de mutación | 🔴 roto | Nunca ha terminado una campaña. Pide lo imposible |
| Evolución autónoma | ⚪ apagado | Correcto: es la guarda de plantilla |
| Guardián de rutas sensibles | 🟡 pegatina | Funciona, pero la cerradura que invoca no existe |

## 1. La mutación nocturna pide 11-17 h en un job que muere a las 4

`harness-mutation.yml` corre `harness verify`, que es init **más** la mutación
con objetivo vacío. Ese objetivo vacío entra por la rama `else` del `when` de
`targetClasses` y dispara **PIT de todo el backend y luego Stryker de todo el
frontend, en serie, en un solo job**.

Medido en el run cancelado `34322164783`: init verde en 10 min 39 s; PIT calculó
cobertura en 470 s; creó 254 unidades de mutación a las 07:24:57; y **3 h 41 min
26 s después seguía analizando** cuando el `timeout-minutes: 240` lo mató. El
frontend no llegó a arrancar nunca. Los tres cancelados (7, 8 y 9 de septiembre)
duran exactamente 4 h 00 m. El fallo del 10 de septiembre es distinto y ya no
aplica: corrió sobre el árbol previo a la cirugía, con la suite rota.

Doce de los ámbitos documentados en `progress/` suman **6 h 37 m** ellos solos.
El techo duro de un job hospedado son **360 min**. No cabe, y no por poco.

## 2. Y trocear por ámbitos con nombre mediría MENOS, no igual

Es la trampa que hacía falta destapar antes de «arreglar» nada.
`progress/medicion_ambito_mutacion.md` lo mide, y los resolvedores
(`medicion_ambito_backend.mjs`, `medicion_ambito_frontend.mjs`) lo reproducen:

- Universo del objetivo vacío: **456 clases** de backend.
- Unión de los 21 ámbitos con nombre alcanzables: **308** (67,5 %).
- **148 clases no las cubre ningún ámbito**, entre ellas `Project`, `Task`,
  `CreateTaskUseCase`, `ChangeTaskStatusUseCase`, `Availability` y
  `ValidationException`.

La causa es de una línea: `val core = setOf("...domain.*", "...application.*")`
sólo aparece en la rama `else`. Todos los ámbitos con nombre son listas cerradas
de clases. Poner la matriz obvia habría dejado un tercio del backend sin mutar
**con el semáforo en verde**, que es exactamente el fallo que este repo prohíbe
desde hoy por escrito en `CLAUDE.md`.

## 3. `noche_cinco` no existe, y por eso mataba la máquina

`scripts/project.mjs:192` despacha `-PmutationScope=noche_cinco`, pero en
`backend/build.gradle.kts` **no hay ninguna rama** `scope == "noche_cinco"`: la
única aparición de esa cadena es un comentario, en la línea 663. Un ámbito
desconocido **no falla**: cae al `else`. Así que «el atajo combinado de las cinco
features» era, literalmente, **la campaña de todo el backend**, y por eso murió
por memoria las cuatro veces que se intentó.

Es el patrón de fallo más caro del repositorio: un ámbito mal escrito se degrada
en silencio a «todo», en vez de gritar.

## 4. Lo que decide el propietario

- **`main` no tiene protección de rama.** `gh api .../branches/main/protection`
  responde `404 Branch not protected` y `.../rulesets` devuelve `[]`.
- **`CODEOWNERS` está inválido.** GitHub rechaza sus **tres** reglas:
  `@PhurtadoCenitDigital` no es colaborador del repo. `docs/autonomous.md` da esa
  puerta por hecha; no existe.
- **El troceado del CI de mutación** está diseñado y verificado (partición
  mecánica `-PmutationShard=k/N`, cobertura idéntica por construcción), pero es
  cirugía en `build.gradle.kts` y trae tres correcciones bloqueantes pendientes.
  No se aplica sin luz verde.
  **Actualización del mismo día:** aplicado en la rama local
  `codex/mutation-shards`, pendiente de calibración. El diseño original no se
  pudo recuperar, así que es una reconstrucción. Sus tres correcciones probables
  son exclusión en vez de inclusión, umbral agregado en vez de por trozo y
  completitud probada por manifiestos. Detalle en `progress/troceado_mutacion.md`.
  Sigue sin cron hasta medir que cada trozo cabe.

## 5. Lo que sí se ha hecho hoy

- El guardián de rutas sensibles ahora cubre `harness.config.json` —donde viven
  `mutation.threshold` y `require_mutation_to_close`—, `harness.schema.json` y
  `scripts/*.mjs`. Antes se podía bajar el umbral de mutación sin que dijera
  nada. Queda escrito en su cabecero que es una pegatina, no una cerradura.
- La medición del ámbito queda en el repo, reproducible, para que el troceado se
  diseñe con números y no con intuición.
