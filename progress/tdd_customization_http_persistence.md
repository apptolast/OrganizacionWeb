# Integración HTTP y PostgreSQL de personalización21

Corte e617a09 en OrganizacionWeb-customization-integration. Única fuente nueva: CustomizationHttpPersistenceTest. No se modifica producción, configuración ni suites anteriores. SpringBootTest/AutoConfigureMockMvc con beans reales y PostgreSQL17.9-alpine de Testcontainers; Flyway ejecuta el esquema real. El publicador está desactivado en este fixture. No mocks de puertos ni reloj.

Cada caso autentica mediante POST /api/session con las credenciales de prueba y usa la cookie SESSION devuelta en las siguientes peticiones. CSRF válido se aporta mediante el helper Spring de pruebas; se verifica por separado el rechazo sin token y con origen ajeno. MockMvc ejercita filtros, controlador, casos de uso, Store y PostgreSQL; no acredita navegador, red HTTP, proxy/TLS ni cookies en un navegador. La limpieza se limita a las tres tablas privadas del owner del fixture en el contenedor efímero; proyectos/tareas tienen UUID nuevos. Testcontainers administra su ciclo de vida.

| Caso | Resultado individual | Alcance comprobado |
| --- | --- | --- |
| s3/s10/s11 PROJECT | Inicialmente GREEN b6c69e EXIT0 | Crear definición por HTTP, GET con null/ausencia, PUT de texto con espacios, GET idéntico y valor exacto en SQL |
| @s18 TASK terminada (nombre histórico s14) | Fallo de fixture d3e3e9: faltaba completed_at requerido por CHECK V9; corregido sólo fixture. GREEN 5f9fcd EXIT0 | BOOLEAN false persistido y todas las columnas de tarea, incluidas revisión/estado/fechas, idénticas |
| s15 esquema renombrado | Inicialmente GREEN 66cbbf EXIT0 | PUT con ETag anterior412, fila de valores íntegra, nuevo label visible, cambia sólo componente schema del tag |
| @s14 no-op (nombre histórico s16) | Inicialmente GREEN 2edffc EXIT0 | 1.0 guardado como entero, 1e0 posterior conserva cuerpo textual, ETag y fila SQL completos |
| s17 privacidad/contexto | 3/3 inicialmente GREEN f752fc EXIT0 | GET/PUT de proyecto ajeno, tarea ajena y tarea bajo proyecto propio incorrecto devuelven404; sin valores/ETag ni cambios en filas |
| s23 seguridad | Inicialmente GREEN ad1edc EXIT0 | GET sin sesión401, PUT sin CSRF403, origen ajeno403 y ausencia de filas creadas |

Regresión final del único archivo y formato focal real:8/8 sin fallos/errores/skips, EXIT0 08b955. Log progress/customization_http_pg_final.log. XML original backend/build/test-results/test/TEST-com.apptolast.organization.adapter.CustomizationHttpPersistenceTest.xml, copia preservada progress/customization_http_pg_final.xml. Los logs individuales customization_http_pg_* permanecen intactos, incluido el fallo del fixture. No se presenta ese fallo como defecto del producto ni se inventan RED en recorridos inicialmente verdes.

Límites: este corte contiene backend nominal; no acredita el delta posterior de integridad/corrupción en curso de A, carreras naturales ni reinicio de proceso. No se ejecutaron campañas globales, PIT/Stryker ni E2E. Ocho ejecuciones no equivalen a cobertura completa del contrato. Fuentes de producto permanecen idénticas al HEAD. Manifest separado customization_http_pg_freeze.json; Gradle detenido al freeze.

Corrección de trazabilidad tras revisión root: el método s14_completedTaskValuesKeepAllBusinessColumnsAndPersistFalse acredita @s18; s16_canonicalNumberNoOpPreservesExactBodyTagAndStoredRow acredita @s14. Sólo cambia este mapa; test, XML, logs y resultados originales permanecen intactos. No se ejecuta otra prueba por esta corrección documental.
