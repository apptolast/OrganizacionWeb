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
