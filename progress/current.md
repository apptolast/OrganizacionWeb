# Estado actual

## Importación 23 cerrada y publicada — 8 de septiembre de 2026

Funciones 1–23 desplegadas y aceptadas en https://organizacion.apptolast.com. Producto 4c74e183; aplicación PR27 fusionada en 3f4c3ef e infraestructura PR38 en 183293b. Fuente aplicada 7712fc8, EXIT 0: 38 correctas, cuatro cambios, cero fallos y cero omisiones. Operación liberada.

Root aprueba los 42 escenarios con init/build y CI verdes y campañas completas sobre el umbral: lector 135/161, HTTP 56/56, frontend 651/801 y persistencia 316/330 KILLED (95,7576 %). Los timeouts y errores siguen en sus denominadores. La campaña alternativa se retiró incompleta tras verificar la original; sus resultados no se combinan.

Aceptación live sólo mediante preview de una copia propia en memoria, sin confirmar importación ni crear recibos: 17 tablas conservadas, 20 servicios 1/1, 18 contenedores intactos, ocho rutas anteriores iguales. Chromium a 320/768/1280 sin overflow ni incidencias axe, teclado, cancelación, logout 204 y rechazo anónimo 401. Evidencia y límites en judge_import_data.md, import_live_acceptance.json y paquetes originales import.

## Siguiente trabajo

API para integraciones24: propuesta revisada y 42 escenarios preparados en OrganizacionWeb-integration-api. Todavía no se implementa ni se marca done. El baseline de ese checkout detectó una espera heredada de appearance.test.tsx; su reparación de sincronización está revisada, con validación global y CI posteriores todavía pendientes, sin cambios de producto. 25–30 siguen pendientes de contrato/implementación. Autorización global vigente, sin nuevas puertas humanas rutinarias.

COMMON/V14 y artefactos ajenos permanecen protegidos. El histórico de esta entrega está preservado en history.md. Este cierre cambia documentación/estado/evidencia e incorpora la sincronización revisada del test heredado de apariencia (16d60f8, foco 50/50); no cambia producto ni repite despliegue por esos archivos. La validación global posterior del arreglo y la CI de cierre se registran por separado.

## API 24: contrato aprobado e implementación iniciada

Root aprueba A155A48E4AF40CA7A0BBFD2979FD14A89BBE0EDDCD3A587C8E3861EF087C4B95: 42 escenarios, revisión independiente y autorización global vigente. Transición pending → spec_ready → in_progress, después del cierre real de 23. Backend, frontend y frontera HTTP trabajan con propietarios separados; root coordina y revisa.

Baseline corregido sobre 2bb20674, init oficial EXIT 0: 70 pruebas Node y 2424 frontend ejecutadas. Java UP-TO-DATE reutiliza 3215 pruebas de 135 suites ejecutadas en el baseline anterior. Root verificó cinco artefactos y 843 entradas antes/después idénticas. Metadatos externos integration24-corrective-results.json SHA a7ab08f98b16ae17b574a2f28bcf217da815e2a9dbe61fb834284bac52b110f2. El intento fallido original se conserva; no se atribuye una segunda ejecución Java. Este apartado sustituye el pendiente de baseline anterior. PR28 espera CI; infra PR39 fusionada, sin nuevo despliegue por documentación.
