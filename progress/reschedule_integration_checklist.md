# Cierre pendiente de Replanificar

Lista de coordinación, no dictamen final ni declaración de cobertura completa. Los registros TDD de cada autor contienen los casos ejecutados y sus límites.

| Bloque | Estado de la evidencia | Condición pendiente de cierre |
| --- | --- | --- |
| Movimiento directo | Revisión parcial aprobada, reloj e identidad comprobados | Incorporar a verificación conjunta y mutación |
| Publicador | Revisión parcial aprobada,205 casos verdes y octava ruta Rabbit real | Comprobar vínculo con cambio persistido y mutación |
| HTTP y persistencia | Nominales, replay inicial, kind/intención, estado read-only y retirada de cancelados comprobados por el autor | Completar lecturas de intervalos vigentes y resto de contratos HTTP |
| Capacidad y Hoy | Cancelación y movimiento entre días verdes; listado, detalle y capacidad usan proyección planned | Snapshot concurrente de Hoy y carrera creación/movimiento por solape |
| Recibos e historial | 11 PG, cinco app; HTTP ID/key, página20+1 y query desconocida verdes; decoder11 extraído con regresión173API11+3HTTP13 | Resto de conexión HTTP: privacidad, ausencia, cursor repetido, terminal20 y errores503; cierre final de revisión |
| Concurrencia | Replay tras lock de cancelación y movimiento 201/200; colisión entre bloques sin preferencia201/409; dos movimientos entre proyectos201/409 por presupuesto | s23 seis órdenes de estados/preferencia, s24 snapshot y demás filas del contrato aún sin evidencia específica |
| Atomicidad | Cuatro casos s20 verdes: supresión de cada escritura y fallo diferido real al commit; siete tablas intactas | Regresión final conjunta y mutación; no confundir colisión de key con fallo503 sin recibo ganador |
| V13 | APPROVED parcial;59 XML verdes verificados por root ad831a; integrado64d5174 | Regresión conjunta de comandos y recibos sobre las constraints; upgrade/rollback y conservación ya comprobados |
| Frontend | Stryker86,70%, sin Timeout y2 errores registrados; comparación2→8 idéntica y adoptada; seis oráculos nuevos revisados,203tests verdes | Replay focal de13 candidatos en curso; no adjudicar clasificación antes del resultado; integración y UX pendientes |
| E2E13 | Primer recorrido real en RED contra checkpoint anterior; helper11 verde | Integrar backend y alcanzar todas las assertions; después ACK perdido/reinicio, conflictos y UX30 |
| Entrega | Main con CI verde; funcionalidades1–12 cerradas | Init integrado, revisión final, umbral de mutación y evidencia E2E/UX antes de cerrar13 |

El commit local E2E `8e91436` no está publicado y conserva su RED de preview. V13 ya está integrada selectivamente en backend. No mezclar ese test todavía fallido en main como si fuera una entrega aprobada.

Actualización de coordinación: V13 final revisada y cherry-picked exclusivamente en64d5174. El árbol E2E contiene además snapshot Java d3ffecf de core469422/lecturas066d26 para que resume_review complete handlers de error compartidos en BlockController/ApiErrors con suite nueva. No integrar ese snapshot completo; traer sólo el diff exclusivo del autor para conservar avances posteriores de Store/recibos. Core y lecturas mantienen sus archivos y ventanas Gradle compartidas; errores usa build aislado.

Para la revisión final debe existir mapa de los41 escenarios del contrato a evidencia concreta, distinguiendo pruebas heredadas reutilizadas de nuevas ejecuciones. Conservar las desviaciones TDD iniciales y resultados fallidos documentados; no reescribirlos como ciclos individuales o comandos completamente verdes.

Corte posterior: handlers `92e83e6` integrados selectivamente con hashes `2b9537`; el snapshot completo no se incorporó. Core registra s20 `5309ee`, s41 `02d443`, replay move `29ad71` y presupuesto entre proyectos `f6785b`. Lecturas registra extracción de cursor `961758`, paginado `881ea0` y prioridad query `326d1f`. Root leyó bitácoras en `5da802` y Store en `1146a3`; esa lectura no constituye ejecución adicional ni dictamen final. Main publicado `9e9d916` tiene CI `34054091097` SUCCESS; estos avances backend siguen locales.
