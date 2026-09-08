# Revisión de export_data

## Estado: revisión parcial; funcionalidad en desarrollo

Contrato de 33 escenarios aprobado bajo autorización global. Esta revisión no
declara la función terminada, desplegada ni por encima del umbral de mutación.
Root coordina y revisa; A, B y C escriben producto y pruebas mediante TDD.
Ponytail full y Caveman lite conservan arquitectura, privacidad y legibilidad.

## Núcleo nominal del backend

Lectura completa de siete fuentes y comprobación de sus huellas (9f5dc8), sin
discrepancias. Los XML acreditan dos pruebas y cero fallos/errores/skips
(d2d783). El puerto PreparedExport expone sólo nombre, longitud y envío de bytes;
el reloj queda delegado a la futura lectura transaccional. Aprobado para el
handoff HTTP, sin acreditar todavía datos reales, límites ni snapshot PostgreSQL.
El formato real posterior fue comprobado (4dc30f); la atribución incorrecta del
primer comando de formato queda corregida y su evidencia original conservada.

## Cliente de descarga

Lectura completa de export-data-api.ts y sus 32 pruebas (468307, e99b48,
8a93e7). Las siete huellas de fuentes, bitácora y resultados coinciden; el log
final registra 32/32 en 3,43 segundos. Tipos, lint y formato tienen EXIT 0.

Se conservan los bytes originales, la identidad, el esquema exterior cerrado,
las catorce colecciones/counts, la correspondencia temporal del nombre y los
enteros textuales sin reinterpretar registros. La lectura tiene presupuesto
explícito y longitud exacta; UTF-8 inválido, BOM, truncamiento y respuestas
incompatibles no producen un archivo. Los abortos liberan el reader y una
respuesta 401 de una petición cancelada no revoca la identidad vigente.

El hallazgo de recursos abiertos al rechazar cabeceras se corrigió mediante
RED 2b83cd y GREEN 44ccde. La prueba comprueba cancelación sin solicitar payload;
Response no200 se conserva para el flujo existente. Aprobado este corte del
cliente. Quedan UI, Blob/ObjectURL, integración real, UX, gates y mutación.
