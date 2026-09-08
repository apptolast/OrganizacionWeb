# HTTP de export_data

## Preparación y propiedad

Contrato22 aprobado cc78396. Trabajo C limitado al adaptador HTTP y sus pruebas,
con el mínimo soporte de cabeceras de seguridad que un fallo real exija.
A conserva aplicación, dominio, persistencia y ApplicationConfiguration.
Ponytail full/Caveman lite: reutilizar MVC, seguridad y problemas existentes;
no crear puertos sustitutos ni otra serialización del documento.

A reserva Gradle para sus ciclos iniciales. Esta preparación sólo lee fuentes;
no ejecuta pruebas ni acredita GREEN. El puerto anunciado es
ExportDataUseCase.prepare(owner), con PreparedExport ya completo y acotado,
filename/contentLength/writeTo(OutputStream). Se esperará el corte compilable
y sus excepciones reales antes del primer test MVC. No copiar WIP ni inventar beans.

## Hallazgos del recorrido existente

- SecurityConfiguration emite 401 UNAUTHENTICATED antes del handler.
  La discrepancia AUTHENTICATION_REQUIRED del contrato fue comunicada a root,
  quien coordina su corrección documental. No requiere cambiar el código global.
- GET no exige CSRF. OriginGuard sólo comprueba escrituras; no se abre CORS.
- SessionFailureFilter se ejecuta antes del repositorio de sesiones. Ante fallo
  no comprometido hace reset y emite 503 SESSION_UNAVAILABLE con no-store.
  No debe convertirse en STORAGE_UNAVAILABLE, que corresponde a preparar datos.
- ApiErrors ya representa problemas y StorageUnavailableException sin datos
  privados. El fallback general convierte excepciones no tratadas en 500:
  HEAD/métodos y negociación22 necesitan oráculos explícitos, no herencia asumida.
- CustomizationController usa HeaderContentNegotiationStrategy y especificidad
  sin calidad para seleccionar la calidad efectiva JSON. Es el patrón local
  pertinente; exportación distingue Accept mal formado400 de no aceptable406.
- MVC de CustomizationApiTest importa SecurityConfiguration y usa puertos reales
  mockeados. Es evidencia HTTP con filtros, no socket, PostgreSQL ni proxy reales.

## Secuencia acotada de ciclos

1. Nominal s20: bytes exactos proporcionados por el puerto, owner del Principal,
   nombre seguro y longitud exacta UTF-8, attachment único, no compresión/ETag.
   Preparar debe terminar antes de comprometer200; HTTP no reconstruye el JSON.
2. s18/s21: autenticación antes de consulta, query incluso vacío, body no vacío
   frente a longitud cero. Verificar ausencia de interacción con prepare en
   rechazos; los problemas deben conservar códigos, privacidad y precedencia.
3. s19: Accept ausente, wildcard, q0 específico y admisión específica frente a
   wildcard0; sintaxis inválida separada. Accept-Encoding debe admitir identity
   implícita y rechazar su exclusión. Reutilizar parsers instalados, sin otro
   parser general ni cambio de negociación de rutas anteriores.
4. HEAD405/Allow GET y cuerpo vacío sin preparar; métodos restantes conservan
   primero seguridad. s33 no debe activar304 ni perder el documento por headers
   condicionales. Los casos irán uno a uno, sin matriz transversal duplicada.
5. Errores413 y503 desde excepciones reales de A, sin archivo ni información
   interna. Separar fallo previo de sesión de fallo de preparación.
6. Cabeceras no-store, private, no-transform y nosniff también en401/403/405/503
   previos al handler. Primero medir; si fallan, soporte limitado a la ruta exacta
   de exportación y capaz de conservarlas tras reset, sin alterar respuestas
   ajenas. Un advice de controlador solo no acredita errores de filtros.

Cada ciclo registrará test y comando focal, resultado real y cambio mínimo.
Un caso inicialmente GREEN se documentará así. Habrá una regresión del paquete
al freeze, coordinada con A; no campañas globales ni mutación en esta fase.
La conservación de Content-Length/no-transform en proxy requiere comprobación
integrada posterior y no se atribuirá al slice MVC.

## Ciclo 1 — s20, transporte nominal

Puerto real recibido del corte nominal de A: PreparedExport expone filename,
contentLength y writeTo(OutputStream) throws IOException. El primer test MVC
usa una implementación local de ese puerto de salida con bytes UTF-8 conocidos;
no duplica un modelo productivo ni valida aquí las catorce colecciones.

RED cb3faf EXIT1: ExportDataController aún no existe y el test no compila.
Log export_http_s20_red.log. GREEN f26b26 EXIT0, 1/1: controlador síncrono
delega prepare al Principal antes de escribir cabeceras o cuerpo y transmite
los bytes preparados sin reconstrucción. Oráculos: bytes exactos con Unicode
y long textual, longitud en bytes, nombre, attachment único, tipo, privacidad,
nosniff y ausencia de encoding/ETag. Log export_http_s20_green.log.

Comando de ambos: backend/gradlew.bat test --tests
com.apptolast.organization.adapter.ExportDataApiTest --console=plain.
Sin PostgreSQL, proxy ni socket acreditados. Validación, negociación y headers
de errores siguen pendientes; no declarar HTTP completo. Gradle quedó libre
tras GREEN y root pidió traslado a un worktree aislado antes de otro ciclo.

## Aislamiento y ciclos posteriores

Install root/frontend EXIT0 112785. Init oficial del checkout export-http desde
9c0d038 EXIT0 fab6e8: Node61 y frontend2241/54, Java completo verde. Log externo
deployment-preparation/export-http-init.log. Traslado d4e5c5: cinco hashes
idénticos y retirada no recursiva de sólo los dos Java propios de export-data.
Se conservan allí los logs originales. Excepción real413 incorporada mediante
cherry-pick autorizado6b05a4e como c79b5e9. No más Gradle en primaria.

Todos los ciclos siguientes usan gradlew.bat test --tests con el método indicado
de ExportDataApiTest o ExportHeadersFilterTest; logs export_http_<tema>_*.log.
Cada fallo precede al cambio que lo resuelve. EXIT1/EXIT0 corresponden a RED/GREEN
de la tabla, salvo los intentos expresamente retirados que se detallan después.

| Oráculo | RED | GREEN / inicialmente GREEN |
| --- | --- | --- |
| Query cursor vacío antes de prepare | b34135 | c4472f |
| Autenticación antes de query, headers privados | 5c6d5c: Cache-Control heredado distinto | e99328 |
| SessionFailureFilter real conserva código y headers tras reset | 14804c | c3f283 |
| Ruta distinta no cambia headers | — | 0327ef inicialmente GREEN |
| Body GET con un espacio no es vacío | de0b6e | 196632 |
| HEAD405 sin prepare ni cuerpo | fc6f52 | 2a2825 |
| Accept JSON q0 específico contra wildcard positivo | 733b28 | dea2c5 |
| Accept mal formado400 | — | 0e144e inicialmente GREEN |
| Exclusión identity | 4298d4 | e932c9 tras intento descartado |
| Negociación positiva y body cero bytes | — | 9a2f51, tres ejemplos inicialmente GREEN |
| Encoding sólo coding/calidad, no charset | 4c6bdd | d051f2 |
| Excepción real ExportTooLargeException413 | f918b8 | f24897 |
| StorageUnavailable503 sin datos privados | — | 20c2bd inicialmente GREEN |
| POST válido405, no comando | 6f6176 | 9de974 |
| CSRF403 antes de método | — | 0a6669 inicialmente GREEN |
| Origin403 antes de método | — | 5efecd inicialmente GREEN |
| If-None-Match no produce304; Accept ausente válido | — | bf7632 inicialmente GREEN |
| Query precede body/Accept | — | 8a6f0f inicialmente GREEN |
| Métodos POST/PUT/PATCH/DELETE/OPTIONS | f01a00: cuatro PASS y OPTIONS200 | 53c10e, cinco PASS |
| Exclusión wildcard de encoding | — | 1c2adb, dos ejemplos inicialmente GREEN |
| Accept sólo CSV | — | 997f27, dos ejemplos inicialmente GREEN |

El primer intento de encoding, export_http_encoding_green.log, tiene EXIT1
f47f69; el nombre anticipado no acredita GREEN. Tomcat AcceptEncoding.parse
descarta q=0 (bytecode inspeccionado8c27cb), por lo que se retiró. El ajuste usa
MediaType de Spring para validar token/calidad, conserva cero y selecciona
identity explícito antes que wildcard. Sólo permite parámetro q; no añade
dependencias ni conserva el uso de Tomcat. El GREEN real es
export_http_encoding_final_green.log, e932c9.

La ampliación de métodos tiene log export_http_methods_initial_green.log con
EXIT1 f01a00: OPTIONS se resolvía automáticamente200. Su XML RED se conserva;
el GREEN real es export_http_methods_final_green.log. Ahora los métodos no
admitidos están declarados explícitamente en esta ruta, sin cambiar otras.

Root retiró su sospecha inicial de charset en Accept JSON tras contrastar
RFC8259 sección11. El ensayo export_http_charset_red.log/green.log
(baa75a/9d9c11) pertenece a esa premisa retirada, no a un defecto confirmado.
Se retiraron test de rechazo y filtro de charset. El caso positivo conservado
acepta application/json;charset=us-ascii y entrega UTF-8: cuatro ejemplos
nominales GREEN a20bd0, export_http_json_charset_accepted_green.log.

## Corte HTTP para revisión

Formato y regresión focal final EXIT0 929b24: ExportDataApiTest y
ExportHeadersFilterTest con spotlessCheck. Log export_http_final_green.log;
XML preservados con prefijo export_http_final_TEST-. No fuentes de A modificadas.
Los bytes de las fixtures MVC son muestras de transporte opaco, no un sustituto
del contrato JSON de catorce colecciones que valida A. La prueba del filtro
compone el SessionFailureFilter real y verifica reset sin alterar su código.
ExportHeadersFilter actúa sólo en la ruta exacta; no reescribe la seguridad global.

Pendiente de integración: bean real, preparación PostgreSQL, proxy sin compresión,
aceptación socket/navegador y mutación. El mapper de recibos se comenzará después
de este freeze bajo el reparto acordado, sin cambiar estos archivos durante review.
