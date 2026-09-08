# Revisión de cortes parciales de importación

Estos cortes permiten integrar trabajo independiente; no acreditan la feature 23 completa ni sustituyen sus puertas finales.

## Puerto de vista previa y errores

El puerto nominal 1a902826 separa aplicación y consulta, delega propietario e InputStream originales y no introduce JDBC/JSON en el dominio. La prueba nominal comprueba delegación y no consumo; no acredita persistencia ni parsing. Sus siete huellas originales fueron verificadas en 1fefac. El XML trasladado mediante Git puede normalizar finales de línea; no se atribuye identidad binaria a esa copia.

De c2f55fe se aprueban para el adaptador HTTP únicamente ImportInvalidFileException e ImportTooLargeException: errores de aplicación simples con mensajes genéricos, sin datos privados. Root verificó ambas huellas y el XML externo original del corte parcial (d112f8430e60deeedd26dbe593cf30014e19825346168b2b479cbb3bff148f38). El lector parcial sólo acredita archivo vacío, límite real de bytes y clave raíz desconocida. No acredita aún esquema completo ni filas, y sigue bajo TDD de su autor.

## Primer cliente de vista previa

Freeze import_frontend_preview_freeze.json, SHA 7B224B591F6A1DB390AEE2A767191C2912B5A358EDF5A19EBF021A34E8BC8383: cinco huellas coincidentes, 26 pruebas verdes originales y checks EXIT 0. La lectura de código confirma transporte del File original mediante el cliente CSRF existente, sobre cerrado, propietario y longitud exactos, cantidades coherentes de catorce colecciones, hash SHA-256 de los bytes y cancelación tras cada frontera asíncrona. Reutiliza los validadores existentes y no interpreta otra vez las filas del archivo en el navegador.

Aceptado como corte parcial. Quedan positivos inclusivos de 32 MiB y 100000 registros y avisos RUNNING válidos; su autor los incorpora antes del cierre. No incluye todavía confirmación, recibos, recuperación, interfaz, navegador real, mutación ni prueba integral. La bitácora seguirá creciendo: el hash del freeze describe el instante de revisión, no futuras versiones de esa bitácora.
