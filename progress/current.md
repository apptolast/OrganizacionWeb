# Estado actual

## Entrega publicada

Funciones1–21 desplegadas y aceptadas en https://organizacion.apptolast.com. Cierre de aplicación en main5a5464c; infraestructura PR34 fusionada en8b488544caf6220835d2bd4b33a35db9485e17a2 con CI34176136643 SUCCESS. Evidencia21 en judge_custom_views_fields.md y customization_live_acceptance.json. CI de main34176109030 también terminó SUCCESS.

## Exportación22

Checkout limpio OrganizacionWeb-export-data, rama codex/export-data desde5a5464c. Install/init oficiales verdes:2706 pruebas Java/113 suites,2241 frontend/54 suites y61 Node. Log externo export22-init.log SHA134A27C21FE35E082E52DF92D3550F7949BBF56E6E588CDC550D6C88BBF4B4A3.

Sección22 revisada: JSONv1 de catorce colecciones propias, snapshot consistente, datos durables e inactivos, sin infraestructura/secretos;100000 registros y32MiB inclusivos, memoria acotada y respuesta completa. La auditoría independiente corrigió el esquema histórico de los recibos de bloques. Gherkin de 33 escenarios y 79 ejemplos revisado y aprobado por root bajo autorización global; transición spec_ready a in_progress. SHA del contrato: 4F1EECB1810B5B8B4B5DA261A76176C3857943CBCCAB8005420B04DC06CB70B2. Empieza TDD en backend e interfaz con propietarios de archivos separados. Autorización global vigente, sin repetir puerta humana.

Backend de datos e interfaz trabajan en este checkout. HTTP trabaja aislado en OrganizacionWeb-export-http, rama codex/export-data-http, desde 9c0d038 para evitar interferencias entre ciclos RED de Java. Su install/init terminó verde (fab6e8); trasladó cinco archivos propios con hashes idénticos (d4e5c5) y retiró sólo los dos Java originales. La excepción 6b05a4e está integrada allí como c79b5e9. COMMON conserva V14 protegido y sus artefactos; no leer, copiar, restaurar ni modificar ese archivo. La lectura errónea anterior y el contraste válido con Git están documentados en spec_export_data.md. Ponytail full/Caveman lite vigentes; root coordina sin escribir producto/tests.

## Primeros cortes revisados

Root revisó el núcleo nominal: 7 huellas coincidentes y 2 pruebas verdes verificadas en XML. No acredita PostgreSQL ni exportación completa. El puerto PreparedExport mantiene filename/contentLength/writeTo; commit 5a1c534 y formato real verificado en 9c0d038. La primera invocación de formato no seleccionó archivos y su atribución se corrigió en la bitácora. La excepción ExportTooLargeException está fijada en 6b05a4e para integrar HTTP.

El adaptador HTTP completo de C está revisado e integrado en b8906a7: 25 pruebas MVC y dos del filtro verdes, cinco huellas verificadas. Incluye negociación, rechazo temprano, errores y conservación de cabeceras privadas. No acredita todavía socket ni PostgreSQL integrado.

Cliente, vista y UX revisados en 4666401: 2295 pruebas frontend, 63 del arnés, lint y build verdes. Las 170 entradas y ocho evidencias coinciden con sus hashes. El fallo real de foco al deshabilitar el botón se corrigió y comprobó en navegador. La matriz de treinta principios documenta tres motores, 768 medidas, 24 axe y zoom nativo Chromium con captura visible; API simulada y límites humanos indicados. Stryker terminó después con resultado aprobado, detallado abajo.

Persistencia nominal de catorce colecciones y wiring real revisados en 2b927e4; recibos integrados y refuerzo de fecha de creación en 32b9581. C validó el contexto integrado con 2818 pruebas Java y PIT de sus tres clases completas: 216/259, 83,3977%, sin errores ni NO_COVERAGE. Root comprobó el XML original y los hashes. Los refuerzos posteriores cambian sólo pruebas y conservan los 43 supervivientes originales, sin sumar detecciones no medidas.

A continúa integridad y rendimiento: snapshot concurrente y fallo tardío pasan; 100000 registros/32 MiB exactos y un byte adicional se comprueban realmente. La primera lectura por fila tardaba 76,94 s y provocó 504 en Nginx local; lotes acotados de proyectos redujeron la descarga a unos 1,6 s. El caso hermano de 80000 tareas también pasó de 504 a 200 en unos 2 s. Estas mediciones locales no acreditan rendimiento en producción. C trabaja HTTP real pequeño y A conserva los casos masivos/proxy.

La incidencia de solapamiento del ciclo 17 de A permanece documentada con logs originales y comprobación serial posterior. El primer PIT sin bean real se conserva como intento inválido, separado del resultado posterior válido. Exportación sigue in_progress: faltan integridad restante, mutación del lector, aceptación integral y despliegue. COMMON/V14 siguen protegidos.

## Validación posterior

Stryker frontend terminó con EXIT 0: 261 Killed, 60 Survived, 4 NoCoverage y 1 Timeout, 326 en total. El cociente conservador es 80,06134969%, sin contar Timeout como Killed. Root verificó las once huellas del manifiesto original y las 170 entradas idénticas antes/después (c86461 y 4b7d2a). El informe distingue huecos de cobertura de defectos productivos; no se repite la campaña para perseguir 100%. B refuerza únicamente el solapamiento de cancelación y siguiente preparación, sin cambiar producto ni reclasificar supervivientes.

C añadió cinco pruebas por socket real con PostgreSQL, autenticación y error tardío; integradas en 4b90519. El soporte del scope PIT de persistencia está revisado e integrado en 3e42941: once huellas originales coinciden, 64 pruebas del arnés aislado, 66 del combinado y evaluación real del DSL verdes. Conserva nueve patrones completos, todos los candidatos JUnit y los controles previos. Pendiente de ejecutar sobre el corte final de A.

E2E nominal real revisado en db13626: producto fijo ee4ca8d, 1/1 GREEN tras corregir sólo el oráculo del nombre accesible del menú. Dos descargas iguales a los 1332 bytes de una sola petición, propietario correcto, datos ajenos excluidos, catorce cantidades coherentes, filas/eventos sin cambios y Blob URL revocada al salir. Diez huellas verificadas; entre 1913 entradas sólo cambia el test. El stack efímero se retiró. No acredita las guardas posteriores de A ni el despliegue de exportación.

La pasada completa de integridad está registrada en review_export_data_integrity.md. A cierra los últimos campos TEXT sin CHECK, estado de sesión y formato de offsets; no añade reinterpretación TZDB ni normalización histórica. Las reservas masivas pasaron de 504 con 55000 filas a 200 en 1175 ms mediante lotes calculados por máximo SQL, con memoria acotada y Nginx intacto. Quedan freeze final, regresión global, PIT de persistencia y publicación/aceptación.

## Corte final y puertas restantes

Producto congelado en e9350e9. Init oficial final y build terminaron con EXIT 0: 2844 pruebas Java en 123 suites, 2295 frontend en 56 archivos y 66 del arnés. Evidencia revisada en judge_export_data.md y export_final_init_results.json. No repetir estas campañas sin cambios o un motivo nuevo.

E2E completo original terminó con EXIT 1: 154 correctos, dos timeouts de 180 segundos en matrices responsive heredadas y un caso de zoom omitido por nombre de proyecto. Exportación real y simulada pasan. Root verificó las siete huellas del freeze y las 1924 entradas idénticas antes/después (7a3a91, 4e1a89). Los dos fallidos esperan una repetición dirigida sin cambios cuando PIT libere recursos; el resultado original se conserva. PIT de persistencia sigue activo y ha comunicado un MEMORY_ERROR, pendiente de resultado final y clasificación conservadora.

Preflight remoto de infraestructura: bootstrap, validate-iac y lint terminaron correctamente sobre checkout limpio 0bb939bc29ab6e78edb4b0e28d3d6fd01859f415. La validación tuvo dos intentos interrumpidos: primero por reinicio del runtime local y después por un paginador interactivo. Ambos marcadores se recuperaron con la herramienta oficial, que verificó controlador detenido y archivó evidencia; no se retiraron manualmente. La ejecución final usa GIT_PAGER=cat, PAGER=cat y TERM=dumb. Logs externos en deployment-preparation/export22-infra-*.log. No se ha publicado ni desplegado exportación. La web actual responde HTTP 200.

Actualización posterior: el replay sin cambios dejó una matriz verde y otra en timeout. La reparación revisada del arnés sustituye únicamente comparaciones geométricas síncronas de Playwright por node:assert/strict, conservando todos los criterios. Ambas matrices pasan ahora en 8,3 segundos cada una; nueve huellas y 1924 entradas sin cambios comprobadas en 229739. Los intentos anteriores conservan sus resultados originales.

CI manual 34187626169 valida 0030513fa402502b5db87efa87d97fb979171b4e y está en E2E, después de init/build verdes. Imágenes candidatas publicadas desde ese checkout limpio, 1932 entradas sin cambios: API índice sha256:1276d6e618f5ab6aadfa816244f5022a2393cd46d2c8c09a6d5bde1e8ec9f49b y web índice sha256:2568a6bf4c2347171df4433f50d5127ac1e4537385ad4b067c595c889eaf8d92. Tag, plataforma linux/amd64 y label exacto verificados; evidencia externa release22-0030513-*. La reparación E2E posterior no cambia el producto de esas imágenes. A prepara catálogo candidato en nuevo DockerSwarmInfrastrcture-export-data, desde main8b488544. No hay apply ni aceptación productiva22; PIT sigue pendiente.

## Cierre de validación y publicación preparada

PIT final EXIT 0: 356/359 KILLED, 99,164345 % conservador; un superviviente, uno sin cobertura y un error de memoria preservados. Root verificó veinte huellas y 453 entradas idénticas. Originales portables en export_persistence_original_evidence.zip. Dos refuerzos test-only pasan con la clase completa 39/39; no replay ni nuevas eliminaciones atribuidas.

E2E final completo EXIT 0: 156 correctos, cero fallos y un skip con evidencia dedicada previa; 1924 entradas idénticas. CI productiva0030513 run34187626169 SUCCESS, incluidos 156 E2E y publisher. PR25 abierto en borrador. Su primera CI34189142299 falló en una espera del test de apariencia heredado; ajuste mínimo revisado, suite50/50 y frontend2295/2295 verdes. Espera el próximo gate del PR; el producto de las imágenes permanece idéntico.

Infra PR35 fusionada como fba78f495796cea69217812e6634a35d8bfc63b8; CI34189520466 SUCCESS. Fuente revisada/apta para operación5a2b8607a80624fbd0c410a9a3a81955afa564a1, checkout remoto limpio. Bootstrap/validate/lint y seguridad del candidato aprobados. Check UTF-8:27ok/2changed/11skipped/0failed, operación ca70b68b33ab1e1868ab7ae222a63cf5e7f0641967bb07a30f7f4da6e6c08c9b liberada. Primer check falló antes de tareas por LC_ALL=C; recuperación oficial archivada y nueva ejecución C.utf8 correcta. No se ha aplicado aún.

Baseline remoto nuevo:20 servicios1/1, mismos contenedores21,16 tablas de negocio/Flyway/outbox con cantidades y huellas sin contenido personal. Evidencia externa export22-live-*-before. Restore PostgreSQL previo acreditado sólo hasta snapshot7sep23:32/esquema19; no se presenta como restore20 ni se borran escrituras posteriores. Revisión operativa aprobada para esta entrega sin migración, pendiente apply/HTTPS. No quedan procesos de pruebas locales salvo nuevos gates expresamente indicados.
