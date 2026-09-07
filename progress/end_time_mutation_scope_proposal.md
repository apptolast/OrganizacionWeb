# Propuesta de scopes de mutación17 e integración pendiente

Propuesta para revisión; no se ha editado configuración ni ejecutado campaña. Base de HTTP aislado0d48abb, publicación ya integrada2089cbd. Mantener umbral80, motores y concurrencia existentes, informes anteriores intactos y todas las exclusiones protegidas.

## PIT

Nuevo destino `end_time_notification-backend` → Gradle `-PmutationScope=end_time_notification`. Clases de aplicación nuevas: `ExtendWorkSession`, `ReadWorkSessionEnd`, `WorkSessionEnd`, `WorkSessionEndSnapshot`, `WorkSessionExtension`, `WorkSessionExtensionTransition`, `WorkSessionExtended`. Añadir los tipos compartidos realmente afectados: `WorkSessionTransitionReceipt`, `WorkSessionTransition`, `WorkSessionChanging`, `ChangeWorkSession`, `ReadWorkSessionState`, `ReadWorkSessionChanges`, `domain.WorkSessionState`, `domain.OutboxMessage`, `PostgresWorkSessionStore*`, `WorkSessionStateController*`, `RabbitBrokerPublisher`, `ApplicationConfiguration`. Todas bajo `com.apptolast.organization` y su paquete real. Las interfaces puras sin comportamiento no necesitan un objetivo artificial; ningún método compartido de estas clases se excluye por su resultado. A confirma `WorkSessionEnd` como guarda de marca/tiempo reutilizada.

Conservar todos los candidatos JUnit (`com.apptolast.organization.*`) como16, selección por cobertura medida, cuatro threads y umbral80. Revisar lista definitiva contra el diff final de A antes de configurar. Mantener scopes14–16 y agregar17 al default inclusivo. Informes propios `pitest-end-time-notification`; no sobrescribir originales.

## Stryker y dispatcher

Nuevo destino `end_time_notification-frontend` → `pnpm --dir frontend exec stryker run stryker.end-time-notification.config.json`. B confirma fuentes actuales `work-session-end-api.ts`, `work-session-end.tsx`, `use-work-session-decision.ts`, `work-session-state-api.ts`; integración prevista `work-session-state.tsx`, `work-session-reader.tsx`, `work-session.tsx`. La lista se cierra por hashes al freeze B. No añadir App/TaskReader/use-session/api-client si siguen intactos. SCSS no es objetivo del mutador JS.

Derivar config16 conservando8 workers, Vitest, análisis por test, umbral80 e `ignorePatterns` de `.stryker-tmp-availability-replay`. Cambiar únicamente objetivos y directorios propios del reporte/sandbox17. Dos pruebas individuales del dispatcher comprobarán comandos exactos y conservación de destinos inválidos/heredados, sin ejecutar mutación. No hay configuración nueva todavía.

## Validación integrada pendiente

Después de wiring y freeze A/B: repetir HTTP con puertos reales y PostgreSQL para POST E/EXTEND/recuperación C/K; verificar recepción de beans nuevos en los contextos acotados. A acredita precisión, marca, locks, rollback, carreras y upgrade en sus suites reales. E2E/smoke requieren ownership acordado: aviso y extensión en ambas superficies, respuesta perdida con ampliación posterior, reinicio y recibo original pese a retirada de outbox; publicación Rabbit real. UX de timer largo, visibility, feedback y decisiones hermanas pertenece al corte B. Sólo entonces gates globales y campañas sobre snapshot congelado; este documento no acredita esos resultados.
