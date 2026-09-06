# Revisión incremental — completar y reabrir tareas

Contrato aprobado en d65bba5, después de publicar split_task en 3675c36. Esta revisión incremental no autoriza mutación ni cierre antes de revisar el conjunto final y su trazabilidad. Ponytail full y Caveman lite activos.

## Núcleo liberado por el autor

Lectura de Task, TaskSnapshot, TaskRevision, ChangeTaskStatus y su puerto, TaskStatusEditing, TaskStatusChange y TaskStatusChanged, además de ChangeTaskStatusTest. Favorable: DTO8 conserva su forma y amplía explícitamente estados; Task.create sigue creando pending. El snapshot exige coherencia entre estado, versión y completedAt. La operación compara revisión antes de no-op y devuelve el mismo snapshot sin evento si la intención ya está satisfecha.

Una transición conserva contenido e identidad, incrementa versión y utiliza el máximo entre reloj truncado a microsegundos y fecha previa. El evento usa proyecto como aggregateId y tarea como taskId, con estados e instante de la misma operación. La reapertura limpia sólo la finalización actual; la retención del historial todavía debe verificarse en persistencia. No hay llamada al broker en aplicación.

Las pruebas del núcleo comprueban la forma del evento, la comparación previa al no-op y ambas direcciones con reloj igual o anterior. El autor informa ejecución verde; todavía no se ejecutó una verificación global de feature 9 por el coordinador. Adaptadores, historia, HTTP, cliente y UI siguen en implementación y no quedan aprobados por esta lectura.

## Cliente API liberado por integración

Revisión independiente favorable de task-status-api.ts, cambio acotado de tasks-api.ts y sus pruebas nuevas de estado/historia/compatibilidad. Los cuerpos tienen tres campos de estado o cuatro de transición, con páginas de veinte; los errores HTTP no se transforman en datos vacíos. Se comprueban fechas UTC reales, precisión hasta microsegundos, coherencia de completedAt y ETag fuerte de la tarea dentro de BIGINT. El cursor permanece opaco y el cliente reutiliza sesión/CSRF/cancelación sin reenvíos automáticos.

PUT exige que la respuesta confirme la intención, pero admite el mismo ETag para un no-op válido. Las cuatro lecturas aceptan completed conservando DTO8; POST de raíz o hijo exige pending explícitamente. Las pruebas comprueban errores controlados para formas incompatibles y preservan SyntaxError para JSON ilegible, sin confundirlos con TypeError accidental. El autor informa 209 pruebas seleccionadas verdes, ESLint y tsc correctos. Fuente final leída por el coordinador; no se repite su suite mientras la UI continúa en TDD. Su aprobación se limita al módulo y no habilita todavía la campaña conjunta de mutación.

## Preparación de integración

Lectura de los cinco E2E y del diff del publicador, todavía sin ejecución funcional. El smoke conserva las cinco rutas anteriores y añade la sexta a la misma caída/recuperación/reinicio, comprobando identidad e historia consultable. Dos ajustes pedidos al autor antes de ejecutar E2E: el recorrido de 21 transiciones necesita declarar el fixture page que utiliza; la recuperación después de reload debe comprobar tres entradas visibles, no sólo el encabezado del historial. La carrera HTTP concurrente de este recorrido no sustituye las carreras coordinadas mediante bloqueos PostgreSQL del contrato backend.

## Persistencia y UI en revisión incremental

La lectura de V9 y PostgresTaskStatusStore confirma el diseño esperado: restricciones de estado/fecha, historia con FK compuesta y unicidad por versión, lectura de snapshot en una consulta y cambio bajo FOR NO KEY UPDATE OF t. Las tres escrituras exigen una fila dentro de TransactionTemplate; el no-op sale sin insertar. La evidencia de carreras y rollback todavía corresponde al autor; no se infiere una ejecución desde esta lectura. El parser HTTP sigue en TDD, fuera de este dictamen parcial.

TaskState y TaskHistory mantienen confirmación, error y paginación separados. Las peticiones se cancelan al desmontar y los rechazos 401/404 se comunican al detalle. Se pidió al autor identificar explícitamente la zona horaria de fechas, actualizar la etiqueta de las listas con el estado confirmado y probar recuperación de foco sin desplazar un control elegido. La UI continúa en TDD; no hay aprobación conjunta ni mutación autorizada todavía.

Hallazgo UI para resolver antes del freeze: si otro cliente completa después de que la historia inicial vacía haya cargado, el PUT local devuelve 412 y Consultar estado vigente puede confirmar completed sin refrescar esa historia. Se pidió prueba determinista y recarga independiente del historial tras la consulta deliberada, sin reenvío de PUT ni consulta inicial duplicada innecesaria. El E2E existente de recuperación verificará una entrada tras ese recorrido. No se declara resuelto hasta recibir prueba y fuente final.
# Verificación global del corte congelado

Verificación final frontend después de refuerzos y espacio UTC: pnpm test del coordinador, chunk a52526, EXIT 0, 646/646 en 17 archivos, 7,75 s de Vitest. Se añaden once casos UI y diez API al corte normal de 625. Leídas sus aserciones de recuperación, doble confirmación, callbacks obsoletos, paginación, errores controlados y espacios legibles. No se suman a los resultados de mutación: original y replays conservan sus propios denominadores.

La CI de la entrega anterior split_task, ejecución 34007601179 sobre 3675c36, terminó SUCCESS; contrastado mediante GitHub CLI. No se atribuye este resultado a complete_reopen_task, todavía local.

2026-09-06, sesión 58990 de init.ps1: EXIT 0. Lint global verde; 798 pruebas backend, cero fallos, errores u omisiones, sumadas por el coordinador desde los XML antes de nuevas ejecuciones parciales. Frontend 625/625 en 17 archivos, 8,16 s de Vitest. La lectura de historia tiene dictamen independiente en judge_complete_reopen_task_history.md; la interfaz en judge_complete_reopen_task_frontend.md y las transiciones backend en judge_complete_reopen_task_backend.md.

Se autoriza ajustar únicamente el ciclo de vida de cuatro fixtures históricas PostgreSQL para evitar endpoints obsoletos durante PIT; producción sigue congelada. Esa modificación requiere una ejecución normal de las cuatro clases y revisión antes de autorizar PIT. Las pruebas E2E, UX y mutación siguen pendientes; esta verificación normal no las sustituye.

La revisión del smoke detectó una inserción errónea: la comprobación de reinicio del backend había quedado después de destruir los contenedores, dentro del cleanup. Se corrigió antes de ejecutar, moviéndola al try principal después de probar retención con backend detenido y antes de dispose/down. El coordinador releyó el contexto completo corregido. Se registra como hallazgo de revisión, no como RED funcional ni resultado ejecutado.

Integración informa E2E 83167 EXIT 0, 43/43 sobre web C1K_ahR7; smoke corregido 88526 EXIT 0 con seis rutas y recuperación del mismo estado/historia/ETag/sesión tras reinicio. Leída su bitácora. El coordinador inspeccionó capturas desktop/mobile y JSON de zoom nativo 2: innerWidth 320, scrollWidth y clientWidth 312, controles medidos de al menos 44 px. Distribución legible, sin solapamientos visibles. Confirmado defecto textual de UTC concatenado en TaskState, corregido por frontend y pendiente de nueva captura. No se usa la imagen anterior como evidencia de la corrección.

Leída la matriz UX de treinta principios: distingue aplicación observable, límites y funcionalidades aún pendientes; no equipara axe a certificación WCAG ni emulación a dispositivos físicos. Se mantienen separados los 43 recorridos Chromium y los dos recorridos Firefox/WebKit.

Capturas finales de web CAKNgGJM inspeccionadas por el coordinador: escritorio, móvil y zoom nativo a 320 px. Espacio UTC confirmado, controles y texto sin solapamientos visibles. El cambio respecto al corte E2E C1K_ahR7 es únicamente ese separador; no se atribuye la suite de 43 casos a la nueva imagen. Se conserva el recorrido real de guardado en el zoom final y la evidencia separada de construcción/capturas.
