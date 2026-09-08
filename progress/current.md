# Estado actual — API para integraciones (24)

Las features 1–23 están desplegadas en https://organizacion.apptolast.com. Feature 24 permanece in_progress con los 42 escenarios aprobados. Las features 25–30 están pendientes; no se promete un 100 % sin validación. Autorización global de implementación, revisión, publicación y despliegue vigente. Ponytail full/Caveman lite; root coordina revisión/Git/ops, agentes escriben producto y tests.

## Validación confirmada

Backend final 8d7d8b2: 122 tests focales y 87 entradas verificadas. HTTP/wiring 5b10287/1466f07: 138 tests y nueve casos PG con Clock fijo. Compatibilidad de negocio 20c3ac3: dos casos de ETag/outbox y errores que consumen cuota. OpenAPI final 32d3390: 18 operaciones, 181 referencias resueltas, 28 schemas, metavalidación oficial offline verde. E2E real 1/1 y 16 ejecuciones UX simuladas conservadas en ZIPs originales 6c77b6b; alcance y límites en ux_integration_api.md.

Init final aislado 360c7636: Node72, frontend2492/65 suites, Java3446/149 XML sin fallos, errores ni omisiones; 1039 entradas before/after idénticas. Incluye las dos correcciones de expectativas Basic heredadas fe6f87f. OAS final queda fuera de ese checkout; tiene validación focal independiente. CI final aún pendiente.

Stryker original terminó por debajo del umbral: 780/997 Killed, 208 Survived, 8 NoCoverage, 1 RuntimeError, 0 Timeout; estricto 78,2347 %. Original íntegro preservado. Se revisaron 15 casos adicionales de permisos, Unicode, clipboard e incertidumbre, inicialmente GREEN; foco83/83. No cambian producto ni config, no se atribuyen nuevas muertes todavía.

## Trabajo activo

A ejecuta una campaña oficial PIT núcleo (8 workers). C escalona PIT HTTP tras el baseline y comprobación de recursos. B prepara una única campaña correctiva Stryker con el mismo universo/config, tras doble revisión favorable. Root ensaya API24→23→24 aislado con helper V22 revisado, sin tocar producción. Imágenes candidatas fe6f87f publicadas y descargadas por digest, linux/amd64 y revisión verificadas; 580 entradas de construcción idénticas antes/después, V14 excluido antes de lectura.

Faltan resultados de mutación >=80 % estricto, CI final, ensayo de rollback, revisión de infraestructura, backup y aceptación HTTPS. Ninguna imagen24 se ha desplegado todavía. Plan externo integration24-release-checklist.md. La pregunta opcional sobre proveedores calendario/tareas no bloquea24.

## Límites operativos

COMMON/V14 protegido: no lectura/hash manual, copia, modificación o restauración. Excluir V14 de nuevas enumeraciones de contenido. Puertos8080/18080/18081 reservados. No forzar Git, parar procesos ajenos ni borrar artefactos históricos. Credenciales privadas fuera de Git/logs/chat. QA de escritura sólo efímero; aceptación live24 prevista sin escrituras de negocio. Evidencia UX es comprobación automatizada/heurística, no estudio con usuarios ni garantía universal.
