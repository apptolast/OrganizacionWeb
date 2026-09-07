# Dictamen final provisional — aviso y ampliación de fin (17)

**Veredicto: PENDIENTE DE GATES FINALES.** Los paquetes implementados tienen aprobación independiente y el init integrado está verde. No se aprueba todavía la feature completa ni se solicita otro cambio de producto a partir de esta revisión documental. Build y UX medido también están aprobados. Permanecen abiertos Stryker y E2E global/CI del corte final.

## Contrato y trazabilidad

El contrato aprobado contiene 44 escenarios y 132 ejemplos, SHA256 `6BC581725DC0FE4C7B548191A842882CE5A62FD0348CF789D1BCCD843ABE4309`. El mapa `tdd_end_time_notification.md`, SHA `918400EFA812F8F74E39542E1432E71161835DA61C775CE4169319F0BEC9D8A0`, conserva las 44 filas, pruebas concretas o reutilización identificada y sus límites. Los 132 ejemplos no se equiparan a 132 tests ni se suman ejecuciones focales superpuestas.

- @s1–23: núcleo, PostgreSQL, migración, precedencias, precisión, marca compartida, carreras, rollback y consultas revisados en `review_end_time_backend.md`. La aprobación independiente corresponde a C; este juez participó en esa implementación y aquí consolida el dictamen, sin autoaprobarla como revisión nueva.
- @s24–27: HTTP, evento/publicador y cliente revisados en sus dictámenes de paquete. Las pruebas MockMvc de puertos no sustituyen PostgreSQL ni Rabbit; integración y smoke separados cubren esas conexiones.
- @s28–43: cliente/panel/coordinación, montaje Task y Reader aprobados en `review_end_time_frontend_panel.md`, `review_end_time_task_mount.md`, `review_end_time_reader_mount.md` y `review_end_time_frontend_integrated.md`. Este juez revisó fuentes ajenas y los deltas corregidos. El último F incompatible, después de S closed/E visible, retira también identidad y panel; RED c10ff6 y foco 47/47 GREEN a63304, con aprobación puntual preservada.
- @s44: `review_end_time_ux.md` APPROVED, integrado 311cd33. Root inspeccionó fuentes y capturas; consolidación a154e9: siete combinaciones de motor/modo, 515 medidas y 35 análisis axe sin violaciones. Firefox y WebKit 5/5 cada uno; Chromium incluye zoom nativo 200%. Feedback máximo medido 5ms. Se conservan los límites de la matriz: sin personas, dispositivos físicos ni lectores de pantalla reales; aviso vencido con evidencia funcional y error de cantidad con componente, sin geometría dedicada en este paquete.

No se ha identificado un defecto contractual adicional en esta consolidación. Se aceptan las reutilizaciones concretas del mapa; no se exige una matriz duplicada de 14–16 por cada combinación. @s31/32 son timers largo/fraccionario focales; el E2E de recuperación corresponde a @s36/37/39. La cota de feedback queda acreditada para los recorridos medidos; no se extrapola a todas las permutaciones de estado y dispositivo.

## Evidencia ejecutada y quién la acredita

| Frontera | Evidencia conservada | Alcance |
| --- | --- | --- |
| Backend focal | A 175/175 en 13 suites, dc5c90; C verifica 26 hashes y XML, 8bc66b | Incluye pruebas compartidas; no 175 nuevas de 17. |
| Frontend focal final | B 248/248 df8548; después delta F 47/47 a63304, formato/lint verdes | No sumar ambos ni atribuir 248 al cambio posterior. |
| Init integrado actual | Root85135 EXIT0 78e8c6: 2076 Java/89 XML, 1795 frontend/38 archivos y 40 Node | Resultado informado y acreditado por root. No se ejecutó otro init en esta revisión. Reemplaza el estado pendiente del mapa anterior para este gate. |
| Tres E2E propios | N bb7728, D a8fb59 y R a71eee; bitácora integrada bec6ea4 | Nominal API/PG, vencimiento real de un minuto y recuperación tras respuesta perdida. No son el E2E global final. |
| Smoke | ac63df EXIT0, 13 PASS, 249 hashes idénticos fb1437; review_end_time_smoke.md APPROVED | Broker/API reiniciados, segunda ampliación y cierre, C/K y replay originales conservados, evento real de once campos. |
| PIT original | 616 KILLED, 4 NO_COVERAGE sobre 620, cero S/timeouts/errores; EXIT0 b598f3 | 99,35483870967742%, gate 80 superado. 348 inputs idénticos. |
| Replay de publicador | 64/64 KILLED, EXIT0 1e6077; 349 hashes idénticos ee30d9 | Incluye firmas Outbox196/207 y 62 adicionales; informe separado, sin cambiar el original. |

Las pruebas y campañas anteriores pertenecen a sus autores/root; esta revisión sólo leyó documentación y dictámenes. Los incidentes de fixtures y resultados inicialmente GREEN siguen diferenciados de los RED funcionales en las bitácoras. No se detecta producción adicional exigida por este juicio ni alcance nuevo de la feature 18.

Build root93234 terminó EXIT0 d1f11e; `end_time_build_final.log` verificado por root668f6a. La aprobación UX posterior procede del dictamen integrado311cd33, leído en1c9ba1. Esta actualización documental no repitió comandos ni inspecciones de navegador.

## Límites aceptados de persistencia y mutación

La recuperación excepcional @s20 usa snapshot/lookup controlados con lock, UNIQUE, rollback y nuevo txid reales. El éxito de la misma intención no se presenta como carrera natural: FOR UPDATE normalmente lleva al segundo intento a replay antes de INSERT. No se fabrican dos sesiones abiertas. Upgrade preserva JSONB históricos y columnas anteriores; la siembra histórica explícita no ejecuta Store actual sobre un esquema parcialmente migrado.

El XML PIT original permanece SHA `5006D78E0A06DED7B153EC1E6480E5CD4703704ED5ABBB978B8A8275487FC681`; el replay, SHA `71F17C95236A770BA3C2724352062844EF52AD448F6964390780018B0BB464CC`. Los dos retornos de éxito del fallback heredado Store253/259 siguen NO_COVERAGE en el original y tienen límite justificado, no equivalencia universal ni kill ficticio. Los oráculos de tipo/calendario adicionales sí detectan las dos firmas Outbox correspondientes en el replay. No se combinan denominadores.

## Pendientes para emitir aprobación completa

1. Stryker root90337: activo sobre 129 entradas congeladas. Falta EXIT, informe original, estados/denominador, comparación de hashes y análisis de residuos; no se anticipa puntuación.
2. E2E global: pendiente tras integrar dos fixtures históricos corregidos, foco 7/7 acreditado por root. Ni ese foco ni los tres recorridos propios sustituyen la ejecución global.
3. CI del corte final: pendiente de resultado acreditado por root; no reutilizar un commit anterior como cierre del último delta.

Se mantienen congeladas fuentes, tests y configuración durante las campañas. No se modifica estado de feature ni se autoriza cierre automático. Root puede completar el dictamen con esos resultados; sólo un defecto concreto justificaría reabrir implementación o reforzar un oráculo.
