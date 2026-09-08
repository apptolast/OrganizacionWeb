# Exportación por socket HTTP y PostgreSQL

Clase propia `ExportDataHttpPersistenceTest`, checkout aislado HTTP. Spring RANDOM_PORT, Tomcat y PostgreSQL 17.9-alpine Testcontainers reales; beans de aplicación, sesión JDBC y exportación sin mocks. Cliente JDK HttpClient con cookie jar independiente y login mediante token CSRF obtenido de GET `/api/session`. Credenciales exclusivamente del fixture. No usa puertos host fijos ni servidores ajenos. A conserva el caso masivo/proxy en `ExportSocketTest`; estos casos pequeños no lo duplican.

## Ciclos individuales

1. Nominal UTF-8/propiedad: dos proyectos de propietarios diferentes, nombre con espacios y Unicode, versión superior a 2^53. Verifica 200, headers privados, charset UTF-8, Content-Length exacto en bytes, ausencia de codificación, nombre de archivo, una sola fila propia y outbox vacío. Primer intento b0abc7 falló exclusivamente por comparación sensible a mayúsculas del charset: Tomcat devuelve `utf-8`. XML `export_socket_nominal_initial.xml` y log original preservados. El oráculo corregido usa MediaType/Charset, sin modificar producción: GREEN f76983.
2. Anónimo con query y Accept no admitidos: 401 UNAUTHENTICATED antes de negociación, problem+json, sin attachment ni datos y con headers privados. Inicialmente GREEN 1c20b4.
3. Corrupción tardía: proyecto válido primero, definición durable `[1]` inválida en la colección de personalización posterior. El socket recibe sólo 503 STORAGE_UNAVAILABLE, sin attachment, sin envelope ni prefijo privado. Fila corrupta idéntica después y outbox vacío; limpieza por UUID propios. Inicialmente GREEN 1a8910. No se atribuye esta prueba a fallos de red durante el envío ni a un 200 interrumpido por el cliente.
4. Dos entradas Accept-Encoding: `gzip;q=0` mantiene identity por defecto; elementos vacíos alrededor de `identity;q=1` se aceptan. Ambos retornan JSON completo sin Content-Encoding y longitud exacta. Inicialmente GREEN 9e90e9. Estos comportamientos precisan dos límites observados en los residuos de PIT; no cambian sus estados originales ni su puntuación.

## Cierre y límites

Clase completa 5/5, sin fallos, errores ni skipped, y spotlessCheck EXIT 0 6032ed (20s); XML `export_socket_final.xml`, log `export_socket_final_green.log`. Tres fuentes mutadas intactas; todo el código productivo coincide con el inventario de la campaña válida. El HEAD al cierre es ed4136b, que incorpora el refuerzo de tests del mapper revisado por root. No se ejecutó otra mutación ni regresión global por estos tests nuevos.

Testcontainers retira su PostgreSQL; Spring cierra Tomcat y el pool. Los proyectos y la definición de cada caso se eliminan por UUID conocidos en finally. No se toca base de datos productiva. No acredita TLS, navegador, cancelación de descarga, Nginx ni volumen máximo; esas evidencias tienen sus propios recorridos. La condición de no respuesta parcial se prueba para un error de preparación tardío real en PostgreSQL, antes de la publicación HTTP.
