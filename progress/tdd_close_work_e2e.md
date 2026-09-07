# E2E funcional16

Primer caso preparado, todavía **sin ejecución**. Propiedad exclusiva nuevo `e2e/close-work-session.spec.mjs` y bitácora; frontend/backend no se modifican. Se reutilizan authenticated-test, create/sql y saveTask, sin nuevos helpers, servicios ni respuestas de éxito simuladas. Los selectores fueron confirmados por B antes de redactar.

Caso1 running @s1/@s26/@s32: inicio25min desde UI; enlace de cierre establece la URL propia antes de cualquier POST; notas con espacios/salto de línea; POST201 y CLOSE7; fin previsto e inicio originales; trabajo exacto contrastado en SQL con intervalo y tiempo entre inicio/cierre; estado, recibo/evento únicos y tarea pending. Recarga consulta F y recupera el recibo completo por URL, sin otro POST. La comparación de microsegundos usa numeric/BIGINT de PostgreSQL, no Date/Number del navegador.

Node --check y Prettier verdes en preparación (f416b9 y cierre de formato posterior), sin atribuir GREEN E2E. No se arrancó Docker ni runner18080; root espera fuentes congeladas de A/B para autorizar la imagen real. No copiar snapshots a COMMON ni tocar datos fuera del lifecycle aislado del runner.

Pendientes después del primer resultado: un caso paused que conserva el neto y distingue una nueva activa; después un caso de ACK perdido únicamente tras POST real201 y recarga sin key. No se añaden antes de cerrar el primer caso. Smoke/publicación corresponden a A; matrizUX a B. Este archivo no acredita motores, responsive, pérdidas de respuesta ni gate global aún.

Primer resultado real: GREEN1/1 d73a29/26b9da, caso6.3s y Playwright10.0s, sin cambio al test para pasar. Log `progress/close_e2e_running.log`; runner aislado organizationweb-e2e-18452 retiró contenedores/volumen/red al terminar EXIT0. La producción integrada estaba congelada por root. Los oráculos se alcanzaron; sin atribuir paused/ACK/UX todavía. Root priorizó después corregir comprobaciones históricas de Today en el arnés, sin cancelar esta ejecución ya terminada.
