# Handoff Gherkin 17: aviso y ampliación

Borrador contractual completo para revisión independiente; no aprobación de implementación ni modificación de estado. Fuente normativa: project-spec.md, sección17 congelada por C con SHA2F44F439F7ED17E19CAE470B911257A3C380321EF20E41E75F09B35FB10ACE02. Se incorporaron ambas superficies, identidad de activa de otra tarea, coordinación de decisiones/snapshots y temporizador largo con resto mínimo1ms. No se altera normativa ni código.

`features/end_time_notification.feature`:44 escenarios,132 filas expandidas; tags consecutivos @s1–@s44, un When por escenario y Examples no vacíos, comprobación estructural9a3e16. SHA256 E2680B4210D5278D7C2D679D250C379F4043A8BF69393979AFF2AB7A78EA13C6. El conteo no es evidencia de tests ejecutados; no se usa runnerBDD ni se añaden dependencias.

| Familia | Tags | Resultado contractual |
| --- | --- | --- |
| Ampliación y forma de entrada | s1–s5 | Fórmula exacta antes/después del fin, paused sin trabajo, acumulado sin límite1440, cantidad y conexión de protocolo heredado |
| Precedencia, intención y reloj | s6–s12 | Propiedad/token antes de replay, namespace de cambios, closed/nueva sesión, BIGINT, marca compartida con P/R/C, igualdadµs y desbordamiento |
| Consultas y almacenamiento | s13–s17 | Fallback sin writes, State6/DTOs históricos intactos, snapshotRR, fallos de cierre y rollback conjunto |
| Concurrencia y migración | s18–s22 | Mismo intento, decisiones distintas/revisión, colisión tras rollback, ausencia de locks ajenos, upgrade aditivo |
| Recuperación y publicación | s23–s25 | C/K históricos sin outbox, ACK perdido/reinicio/broker real, evento11 propio y compatibilidad |
| Decoder | s26–s27 | Shapes exactos, proyección aditiva, aritméticaµs y validación sólo de campos expuestos |
| Aviso y temporizador | s28–s35 | Dos superficies, confirmación deliberada, comprobación al vencer, fragmentos largos/fracciones, visibilidad y aviso persistente |
| Recuperación y privacidad UI | s36–s42 | Coordinación entre controles, intención incierta/CSRF/412, separación de recibo y actualidad, generaciones y acceso |
| Foco/UX | s43–s44 | Espera anunciada, foco condicionado y recorrido accesible en ambas superficies |

Reutilización explícita: seguridad, origen/CSRF, negociación, no-store, UUID/key, sintaxis de cabeceras y JSON de14–16 mantienen sus matrices y errores; s5 exige conexiones representativas a E/P, no copias completas. La identidad de token ajena a sesión propia conserva412 de15 (lectura ba2777), frente a la ausencia/propiedad404. State6, SessionStart7, P/R6 y CLOSE7 mantienen invariantes y recuperaciones; EXTEND sólo exceptúa la igualdad occurredAt/changedAt que su normativa indica.

La fixture cross-session de s8 usa A cerrada y B abierta, válida desde16; no hay dos sesiones abiertas propias inventadas. Las carreras no fijan ganador. El reloj interno adicional no se inventa como campo público ni se exige al decoder verificarlo. Los casos temporales de navegador describen observaciones acotadas, sin prometer alarma con navegador cerrado o dispositivo suspendido.

Sin firmas Java anticipadas: el contrato es de comportamiento. La posterior autoría deberá reutilizar puertos/recibos y entregar tipos compilables exigidos por TDD antes de paralelizar HTTP; esto no autoriza scaffolding ahora. Estado17 no modificado, no producción, tests ejecutables, migraciones, Git ni suites. Lecturas docs/gherkin/reviewB/patrón16 b0f8e9 y8fe1d7; normativa final129a02. Juez frontend avisado al freeze.

## Precisión final de revisión

Se mantiene el corte de 44 tags y 132 filas expandidas. En s2, tarea y proyecto completed son ahora un Given concreto para ambas filas; el Then comprueba su conservación, sin condicional. Delta4b65aa por revisión root/B, sin matriz nueva.

s20 describe la resolución de la colisión después del rollback, no afirma que sus tres filas sean carreras naturales nuevas. El lock de sesión serializa las decisiones sobre la misma sesión; no debe forzarse un ganador idéntico tardío como si fuese la única prueba PostgreSQL del camino. La evidencia posterior puede reutilizar la frontera de puerto/recuperación e inyección de fallo controlada para comparar ganador o ausencia, y la restricción owner/key real de PostgreSQL con A cerrada y B como única sesión abierta e intención distinta, conforme a16. Si un caso se acredita de forma compuesta se documentará así. No se fabrican dos sesiones abiertas propias, ni se atribuye aquí ejecución, concurrencia o cobertura todavía inexistente.

Root retiró sólo la línea vacía final detectada por diffcheck8dd909 después del commit contractual706f539. No cambia escenarios, tablas o aceptación. SHA final de formato: 6BC581725DC0FE4C7B548191A842882CE5A62FD0348CF789D1BCCD843ABE4309. Las revisiones anteriores corresponden al mismo contenido contractual antes de esa normalización de EOF.
