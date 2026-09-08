# Revisión de cortes parciales de importación

Estos cortes permiten integrar trabajo independiente; no acreditan la feature 23 completa ni sustituyen sus puertas finales.

## Puerto de vista previa y errores

El puerto nominal 1a902826 separa aplicación y consulta, delega propietario e InputStream originales y no introduce JDBC/JSON en el dominio. La prueba nominal comprueba delegación y no consumo; no acredita persistencia ni parsing. Sus siete huellas originales fueron verificadas en 1fefac. El XML trasladado mediante Git puede normalizar finales de línea; no se atribuye identidad binaria a esa copia.

De c2f55fe se aprueban para el adaptador HTTP únicamente ImportInvalidFileException e ImportTooLargeException: errores de aplicación simples con mensajes genéricos, sin datos privados. Root verificó ambas huellas y el XML externo original del corte parcial (d112f8430e60deeedd26dbe593cf30014e19825346168b2b479cbb3bff148f38). El lector parcial sólo acredita archivo vacío, límite real de bytes y clave raíz desconocida. No acredita aún esquema completo ni filas, y sigue bajo TDD de su autor.

El corte 8691edd entrega siete fuentes de aplicación y dos pruebas nominales para apply/lectura de recibo. Quince huellas coinciden con commands_partial_freeze.json; XML originales: 11 pruebas en cuatro suites, cero fallos/errores. Root aprueba trasladar esas siete fuentes y dos pruebas al HTTP aislado, conservando el reader parcial en el checkout de su autor. El Clock se entrega como callback evaluado dentro de la operación del store y truncado a microsegundos; todavía no acredita una transacción PostgreSQL ni el instante real del commit. La consulta delega propietario y clave sin ejecutar la importación. No hay beans provisionales.

## Primer cliente de vista previa

Freeze import_frontend_preview_freeze.json, SHA 7B224B591F6A1DB390AEE2A767191C2912B5A358EDF5A19EBF021A34E8BC8383: cinco huellas coincidentes, 26 pruebas verdes originales y checks EXIT 0. La lectura de código confirma transporte del File original mediante el cliente CSRF existente, sobre cerrado, propietario y longitud exactos, cantidades coherentes de catorce colecciones, hash SHA-256 de los bytes y cancelación tras cada frontera asíncrona. Reutiliza los validadores existentes y no interpreta otra vez las filas del archivo en el navegador.

Aceptado como corte parcial. Quedan positivos inclusivos de 32 MiB y 100000 registros y avisos RUNNING válidos; su autor los incorpora antes del cierre. No incluye todavía confirmación, recibos, recuperación, interfaz, navegador real, mutación ni prueba integral. La bitácora seguirá creciendo: el hash del freeze describe el instante de revisión, no futuras versiones de esa bitácora.

## Primer adaptador HTTP de vista previa

Freeze aislado import_http_preview_freeze.json, SHA DA9C0871BB1912746236A09937791F573F64B539F3088AA1B3B8B5AC553E7DBB: cinco huellas coincidentes y 18 pruebas MVC verdes. Usa puerto real mockeado en slice; no acredita socket ni PostgreSQL. El mapping 405 local agrupado sigue el patrón de exportación y conserva el error 500 anterior como RED real. Las fechas mantienen seis decimales y el stream pertenece al caller.

Revisión pendiente de un ajuste concreto: la guarda de query precede al charset/Content-Encoding dentro del handler, pero la sección 23 exige media antes de query. Root y autor identificaron el mismo caso combinado; se solicita un único RED y la corrección mínima, conservando el freeze original. No se integra el controlador en el contexto completo antes de existir wiring real. La fixture del mapper con RUNNING debe distinguirse de un contrato de negocio válido si mantiene counts cero.

Corrección posterior aprobada en d62a5c6: nuevo freeze aislado import_http_preview_reviewed_freeze.json SHA 2768753319BFD85B3B9420599A84188C5EF62933EB0C08D168A16C6E007EED78. Cinco huellas coinciden; 20 pruebas verdes originales. El RED combinado c802f7 se conserva y la comprobación de media precede ahora a query. La fixture RUNNING usa counts coherentes. El límite de MVC sin wiring integrado permanece.

## Cliente de confirmación, recibos e intención

Freeze import_frontend_client_freeze.json SHA B1F91AFD089FBF5BEAD0495E6DAC66A045D689611CC48130EEAC795B90925F94: ocho huellas coincidentes; 56 pruebas verdes en dos suites, tipos/ESLint y formato final correctos. Root revisó las cuatro fuentes. Confirmación verifica hash de File antes del POST; lectura de recibo usa una clave UUID y el decoder compartido comprueba campos cerrados, clave, hash, longitud, fecha y coherencia de resultado/cantidades. La intención conserva sólo propietario, clave y hash; su lectura descarta metadatos ajenos o inválidos.

Los positivos de 32 MiB/100000 y RUNNING null/histórico pasan con respuestas simuladas y constan como inicialmente GREEN. Aprobado como cliente e intención, todavía sin logout integrado, interfaz, renovación de snapshots, navegador real ni mutación. La integración debe manejar fallos de sessionStorage sin impedir logout ni reclasificar como fallida una importación cuyo recibo ya está confirmado.

## Contexto PostgreSQL nominal

Commit a21dc627c7adbfdc781d68e814c15561b55468a5 aprobado para integración nominal aislada. Las 18 huellas de pg_context_freeze.json coinciden; seis XML originales suman 27 pruebas, cero fallos, errores u omisiones. Root revisó store, wiring, reader y oráculos de persistencia. Hay tres casos de uso reales conectados a PostgreSQL, V21 nueva para recibos y errores 412/409 ejercidos. Los errores DataAccessException/TransactionException se traducen localmente a 503; un trigger que suprime la inserción no puede producir éxito falso. El replay conserva recibo, xmin/ctid y no vuelve a consultar Clock.

Se verifican snapshot RR nominal, límites SQL y presencia de dos advisory locks y quince locks de tablas dentro de la operación. Esto no acredita todavía carreras con escritores concurrentes ni atomicidad de filas no vacías. El store rechaza explícitamente toda fila no vacía; formato/versión completos, tipos, corrupción de recibos, staging/fusión, límites inclusivos y fallo COMMIT diferido siguen pendientes. El HTTP aislado recibe nueve paths completos del commit fijado para iniciar sockets nominales; no se copian fuentes en evolución ni se despliega este corte.
