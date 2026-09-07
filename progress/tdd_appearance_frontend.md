# TDD frontend 20 — Apariencia

Contrato f4697a6, autorización de TDD 0202129 y gate previo init25384 EXIT0/14625f. No se repite init para empezar. Ownership sólo frontend/documentación propia; Java/HTTP pertenecen a otros autores. Ponytail full/Caveman lite: se reutilizan apiRequest, exact e instant, sin modificar sus cuerpos ni añadir dependencias.

## Cliente: un caso por ciclo

Logs `progress/appearance_client_<n>_red.log`, `_green.log` o `_initial_green.log`. Los casos que pasan inicialmente acreditan comportamiento heredado de ciclos anteriores; no se presenta RED ficticio. Cada fila se escribió y ejecutó antes de la siguiente.

| Ciclo | Frontera observable | Evidencia |
| --- | --- | --- |
| 1 | GET privado con defaults, ETag, no-store y signal | RED c0d7cc import inexistente; GREEN 7a88b4 |
| 2 | HTTP503 se conserva, no defaults | RED fd081f; GREEN 72474c |
| 3 | JSON con extra se rechaza | RED 79d136; GREEN final40956d |
| 4 | Ausencia debe contener defaults coherentes | RED d1bf45; GREEN04d264 |
| 5 | Configurada con versión BIGINT exacta textual | Inicialmente GREEN0cfcd3 |
| 6 | configured sólo booleano | REDdba2a7; GREEN643c60 |
| 7 | Enum tema cerrado | REDe1b31b; GREENc75b3a |
| 8 | Timestamp máximo microsegundos | RED1482a3; GREENd1a1b1 |
| 9 | ETag configurado con BIGINT acotado | REDa9bcdc; GREEN4589ee |
| 10 | Contraste claro aunque tema oscuro | RED76e730; GREENbc78e4 |
| 11 | Contraste oscuro aunque tema claro | RED9b63af; GREEN501ea1 |
| 12 | Color legible en blanco pero no en hover | Inicialmente GREENe501b3 |
| 13 | Contraste menor a4,5 no se redondea para aceptar | Inicialmente GREEN418568; fixture afinado GREEN371d93 |
| 14 | Acentos libres válidos azul/cian | Inicialmente GREENfca11c |
| 15 | HTTP401 tardío antes del observador | RED284a18; GREENc84a84 |
| 16 | JSON termina después de aborto | RED033f9a; GREEN4c2108 |
| 17 | PUT cerrado con If-Match y CSRF | RED3f1cb1; GREEN3eb379 |
| 18 | Confirmación válida pero tema distinto de intención | RED6871d2; GREEN782502 |
| 19–24 | Tag débil, timestamp ausencia/configurada, color no canónico/tipo incorrecto y campo ausente, uno a uno | Inicialmente GREEN670d3b/0da7a9/7f1b30/93aa20/462a30/71172f |
| 25–26 | Confirmación con otro acento claro/oscuro, separadamente | Inicialmente GREENcc2269/1b9438 |
| 27 | Intención capturada no cambia con mutación del argumento | Inicialmente GREENa6c634 |
| 28 | PUT no puede confirmar recurso no configurado | Inicialmente GREEN5fbd38 |
| 29 | La fila GET401 anterior se conserva y se añade PUT401 | Inicialmente GREEN8a8da8, dos filas parametrizadas |
| 30 | PUT412 conserva respuesta para recuperación manual | Inicialmente GREEN1cb62c |

Incidentes: en ciclo3 se pasó inicialmente un array a exact, cuya firma existente requiere cadena de claves. Fallo real keys.split preservado en `_3_green.log` (el nombre no acredita éxito); se corrigió sólo la llamada y el GREEN real es `_3_final_green.log`. Vitest devuelve su diagnóstico real antes de un mensaje PNPM accesorio “vitest not found” en algunas salidas fallidas; no se instaló ni cambió ninguna herramienta.

El primer fixture13 #777777 fallaba por contrastes adicionales y no aislaba redondeo. Se sustituyó antes del freeze por #645F61: su peor contraste sobre #D0DFC9 es aproximadamente4,499799974, que redondeado parecería4,50; la aceptación compara valor completo. Ambas ejecuciones inicialmente verdes se preservan. No se calcula la expectativa del test usando el helper de producción.

## Alcance y pendientes

Dos archivos nuevos: appearance-api.ts y appearance-api.test.ts. Los 30 casos no equivalen a los112 ejemplos Gherkin. exact/instant mantienen oráculos heredados de estructura/calendario y sólo se prueba conexión y fronteras relevantes de20, sin duplicar toda su matriz.

PUT captura tres primitivas antes del await; el decoder es compartido con GET y comprueba configured/defaults, campos, tema, acentos/contraste, timestamp y ETag. La clasificación detallada de problem+json y estado de recuperación se completará con los ciclos UI; actualmente los errores HTTP se conservan como Response sin inventar códigos. Estado compartido, navegación, formulario, roles de color y tokens SCSS, recuperación/foco/privacidad, pruebas reales y campañas todavía pendientes. No se anuncia cierre de feature20 ni se cambian fixtures globales.
