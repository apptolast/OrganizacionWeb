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

## Adaptador HTTP

Lectura completa de controlador, filtro, dos suites y bitácora (36e9db,
dffd5d). Cinco huellas coinciden y los XML conservados acreditan 25 pruebas MVC
y dos del filtro, sin fallos, errores ni skips (8be6d1). Aprobado el corte HTTP
7fbb347 e integrado como b8906a7. No acredita todavía socket, PostgreSQL o proxy.

La autenticación conserva sus códigos y precede a la consulta; los rechazos no
invocan prepare. El filtro se limita a la ruta exacta y mantiene cabeceras tras
reset del filtro real de sesión. HEAD no prepara datos; los métodos restantes
conservan primero CSRF/Origin. Negociación, respuestas 413/503, bytes completos
y ausencia de 304 están comprobados. No se añadieron dependencias ni cambios
a la configuración global de seguridad.

Root retiró su sospecha sobre charset en Accept JSON al consultar
[RFC8259, sección 11](https://www.rfc-editor.org/rfc/rfc8259.html#section-11).
Ese parámetro no define otra codificación de JSON; no se presenta el ensayo
retirado como un defecto confirmado. Los intentos previos de Tomcat y OPTIONS
se conservan con sus resultados reales en la bitácora de C.

## Vista y composición de sesión

Lectura completa de la vista, sus 22 pruebas y las diferencias de navegación,
sesión y SCSS (a317ff, cbf402, 12164c). Las quince huellas del corte coinciden
(e740b7); el resultado focal acredita 143 pruebas en cuatro archivos, sin fallos.
Tipos, lint, formato y build tienen resultados conservados en el manifiesto.

Se aprueba este corte funcional para continuar validación: identidad recibida
de SessionGate, revocación y aborto durante cleanup de layout, descarga mediante
enlace nativo y conservación de bytes. La respuesta tardía de una sesión cerrada
no retira la identidad nueva. La ayuda y el encabezado corresponden al contrato.
Los cambios de navegación no introducen almacenamiento o estados globales nuevos.

El foco está probado en DOM, pero el comportamiento físico de un botón disabled
todavía requiere navegador; no se atribuye al producto el fallo de precondición
de JSDOM del ciclo 51. SCSS conserva tokens existentes y controles de 44 píxeles
como intención de código, pendiente medición. Faltan E2E, matriz UX, integración
real, revisión final y mutación. Este corte no declara export_data terminada.
