# Dictamen final — disponibilidad

**APPROVED para cierre local de feature 10.** Contrato aprobado 3f9a293: 47 escenarios y 237 casos. El coordinador revisó implementación, pruebas, evidencias y correcciones; la revisión backend independiente y las revisiones API/UI están enlazadas en los informes de este alcance. Este dictamen permite al autor marcar done. No declara terminado el MVP ni desplegado el servidor.

La preferencia personal conserva zona y siete presupuestos diarios en PostgreSQL, con revisión propia y control de concurrencia. No crea reservas, acredita trabajo ni modifica outbox. El cliente separa borrador y snapshot, exige confirmación coherente y ofrece recuperación deliberada ante incertidumbre. Se conserva React/SCSS y la arquitectura hexagonal acordada, sin dependencias nuevas.

## Pruebas y mutación

- Init 8318 verificó lint, 984 pruebas backend sin fallos/errores/omisiones y 841 frontend. Tras los refuerzos y dos correcciones UI, init 11298 terminó EXIT 0: lint verde y 875/875 frontend en 19 archivos. Backend quedó UP-TO-DATE, conservando el corte verificado sin modificaciones posteriores; no se presenta como una segunda ejecución física de sus pruebas.
- PIT original: 130/130 KILLED, cero supervivientes, errores o timeouts. Raíz comprobó el XML independientemente.
- Stryker original: 517 Killed, 115 Survived, 3 NoCoverage y 2 RuntimeError; 81,42 % sobre 635 puntuables, EXIT 0. Los errores no cuentan como detección.
- Replay UI: 115/126 Killed, 91,27 %, sin errores ni timeouts. Raíz cotejó por archivo, ubicación ajustada, columnas, operador y reemplazo los 64 objetivos originales: 57 Killed y siete variantes de foco permitidas. Replay API: 3/3 Killed; el caso nuevo detecta la identidad original 31.
- Las 120 identidades originales no eliminadas quedan explicadas: 58 detectadas en seguimientos, 45 equivalencias justificadas, 15 variantes permitidas y dos errores diagnosticados con Vitest normal en copias aisladas. En esos dos casos las aserciones pasan, pero las excepciones no controladas hacen terminar el proceso con salida 1. El SHA256 de la fuente viva permaneció idéntico. No se reconstruye un porcentaje global mezclando campañas.

Se aceptan variantes de iconos/puntos decorativos y, bajo s39, diferencias entre devolver foco al encabezado o al control cuando BODY está activo, manteniendo el foco externo elegido y los errores de campo. No se las denomina equivalencias estrictas. La fuente final conserva el control de origen, demostrado con teclado real. Los perfiles originales/globales mantienen su alcance; las defensas redundantes no se eliminan para mejorar el resultado.

## Interfaz e integración

La suite original pasó 48/48. El solape de navegación entre 701 y 760 se corrigió alineando el breakpoint a 700 y se verificó con matriz/navegación 2/2. La matriz cubre 28 anchos y zoom nativo al 200 % con 320 píxeles CSS; raíz inspeccionó escritorio, móvil, navegación y zoom. Los controles medidos cumplen 44×44 y las reglas axe ejecutadas no detectaron violaciones. El informe UX recorre los treinta principios y conserva sus límites de evaluación humana.

Dos defectos posteriores quedaron corregidos mediante TDD y revisión: HTTP 400 sin mensaje útil y pérdida de foco al guardar con Enter. El bundle final CpU8JHCd, con CSS Codz1mIb, pasó los dos recorridos focales Chromium. La regresión de Enter pasó también Firefox 1/1 y WebKit 1/1, incluidos éxito, 503, 400 y foco externo seleccionado con Shift+Tab. No se extrapola a esos motores la matriz completa ni se suman cortes distintos como una sola ejecución.

La evidencia conserva persistencia tras reinicio, concurrencia, recuperación y separación de proyectos/outbox. No quedan hallazgos de producto abiertos. Siguen fuera del alcance la prueba en dispositivos físicos, teclado virtual, lector de pantalla real y la valoración de usabilidad por personas.

## Estado operativo

La CI de e1afc11 terminó por timeout durante Stryker, no verde. El commit local 704ff0f amplía su techo a 120 minutos sin reducir controles; la siguiente CI todavía debe ejecutarse. No hay despliegue productivo.

Permanecen temporales ignorados cuya limpieza fue rechazada automáticamente; no son entregables ni se incluyen en Git. Los rechazos documentales y de limpieza constan en los informes de coordinación/mutación. La propuesta auxiliar futura bloqueada no es requisito de esta función. No se autoriza reintentar acciones bloqueadas ni eliminar sus directorios ascendentes.

Ponytail full y Caveman lite se aplicaron durante implementación y revisión, conservando validación, accesibilidad, arquitectura y evidencia. Referencias: review_availability_backend.md, review_availability_frontend.md, review_availability_api.md, review_availability_ui_mutants.md, review_availability_runtime_mutants.md, mutation_availability_backend.md, mutation_availability_frontend.md y ux_availability.md.
