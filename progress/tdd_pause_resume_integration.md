# Integración15 — dispatch y smoke

Common OrganizacionWeb-backend; propiedad scripts/project.mjs, project.test.mjs y posteriormente publisher-smoke.mjs. Root reserva CI/Git, A build.gradle y wiring; frontend su configuración Stryker. No se ejecutan campañas ni Docker antes de wiring/freeze confirmado. Ponytail full/Caveman lite.

## Dispatch cerrado por TDD

1. Backend: prueba del runner inyectado exige únicamente pitest con -PmutationScope=pause_resume_session. RED7759c7 (target inválido) → GREENacf306. Añadido destino exacto pause_resume_session-backend, sin cambiar rutas anteriores.
2. Frontend: prueba del runner inyectado exige pnpm --dir frontend exec stryker run stryker.pause-resume-session.config.json. REDcfc9d9 → GREEN086f4f. Añadido destino exacto pause_resume_session-frontend.

Regresión cdd825:32/32 Node, cero fallos/omitidos; ningún runner real invocado. El primer comando Prettier desde raíz5c041f no encontró el binario, sin cambios; ejecutado desde frontend con rutas explícitas:85abb7 y check069780 GREEN. Diffcheck6d0cc9.

SHA256:

- scripts/project.mjs:807AE7FD69B97CB886442829DF1DBD03B0C2FCB72C0672989CCBC87AF1DB3FD1
- scripts/project.test.mjs:A6BD706A26752FF64D217740CCC2BB4E695CF124F8EDDDCB528C68DFFDCAFA62

Este selector no acredita que los scopes/configuraciones de otros autores estén completos ni autoriza medir antes de su revisión. No se ha llamado gate15 con el scope14.

## Próximo caso real, aún no ejecutado

Extender el smoke existente después del inicio14 recuperado: PAUSE pierde respuesta sólo tras upstream201 real; conserva key, recupera recibo, RESUME posterior, publicación de ambos eventos12 por la décima ruta. Retirar únicamente esos outbox publicados y reiniciar backend conservando PostgreSQL; recuperar recibo PAUSE por C/K sin restaurar estado anterior ni crear filas. Reutilizar relay, login/CSRF, probes SQL/Rabbit y lifecycle del runner; sin respuestas felices simuladas ni nueva infraestructura. Espera wiring/freeze de A y autorización de ejecución coordinada por root.

Preparación del único recorrido escrita en `scripts/publisher-smoke.mjs`. Se extrajo el relay existente para compartirlo entre inicio14 y PAUSE15, conservando el requisito de upstream201 antes de descartar la respuesta. El mismo smoke acreditará la regresión de esa extracción al ejecutarse. La consulta outbox añade filtro de revisión sólo para el evento15, pues existen dos eventos del mismo agregado. El reinicio observa un nuevo StartedAt del backend y conserva identidad, StartedAt y montajes de PostgreSQL; el helper E2E existente exige otro prefijo de fixture y no se modifica para reutilizarlo aquí.

Preparación estática 982ce0: Prettier, `node --check` y diffcheck verdes. SHA256 del script en 06fabf: `E626D9C5FC6A1FCD4334EF58AC5C6CE81DA0E456D16540865E21C8AC1C298AB9`. No se ha ejecutado Docker ni se atribuye todavía RED/GREEN funcional; espera una única imagen coherente tras el freeze coordinado de backend y frontend.

## Primera ejecución real: GREEN

Tras autorización de root y freeze de producción, `pnpm test:publisher` terminó EXIT0 (0886c3). Primera ejecución del caso15 inicialmente verde; no se fabricó un RED. Se alcanzaron once anuncios PASS, incluidos los recorridos heredados y el nuevo @s25/@s26. La extracción del relay14 queda ejercitada por su recuperación y publicación anteriores al caso15.

El recorrido15 confirmó PAUSE201 real antes de perder la respuesta, recuperación por key, RESUME201 posterior, dos eventos originales pendientes con BROKER_UNAVAILABLE y luego publicados sin cambiar identidad/payload, JSON exacto de doce campos y entrega persistente por la décima ruta durable/quorum. Después retiró sólo esos eventos publicados, reinició el proceso backend (StartedAt distinto), conservó PostgreSQL con la misma identidad, StartedAt y montajes, y recuperó el recibo PAUSE original por ID/key aunque el estado actual siguiera en revisión3/running. Las consultas conservaron dos recibos, un intervalo cerrado y cero eventos tras su retirada; el recibo de inicio14 siguió idéntico.

El lifecycle aislado terminó también sin error. No se usó el stack8080 ni limpieza global. Log: `progress/pause_resume_publisher_smoke.log`, SHA256 `A02A97A5504B59A1C586445136336AE4F11B20E91A7AE304E11CE0A212B59A97`. Snapshot anterior/posterior en `pause_resume_publisher_before.json` y `pause_resume_publisher_after.json`: 283 archivos de producción y script, cero cambios (d1955e); ambos manifests SHA256 `24EE30D0A4949486F89B81E8ACE0588911EA4FD07D64D770204D0100C1D8CF43`. El hash del script continúa siendo E626D9…98AB9. Los manifiestos no incluyen tests ni configuración de mutación, que otros autores podían completar durante la ejecución.

Alcance: smoke real de @s25/@s26 y regresión del runner compartido. No acredita aún E2E de UI15, UX global, mutación15 ni cierre de la feature. Sin cambios de producción durante la prueba.
