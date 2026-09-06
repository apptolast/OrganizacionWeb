# Wiring HTTP14 con PostgreSQL

Árbol común OrganizacionWeb-backend, HTTP integrado en 7186fc3. Core/Store congelados y 63 pruebas verificadas por root. Ponytail full y Caveman lite; baseline vigente, sin init redundante.

Propiedad: ApplicationConfiguration, WorkSessionIntegrationTest y ajustes mínimos de fixtures de configuración tras RED real. No cambios en Store, núcleo ni controlador.

1. @s1, POST real: RED 5fb660; XML 454edd identifica StartWorkSessionUseCase ausente al construir el controlador. Tres factories mínimas registran una instancia concreta de PostgresWorkSessionStore y los dos casos de uso. GREEN 2f10db, 1/1. PostgreSQL 17.9-alpine y catálogo reales; sólo Clock.fixed de prueba. Se observan 201/Location/DTO7, truncado de nueve a seis decimales, sesión running, evento con agregado sesión y conservación de proyecto/tarea/preferencia/planificación.

El selector PIT pertenece a otro autor. Tras GREEN se cedió una ventana sin compilación para que lo actualice. No se ejecuta mutación aquí.

2. @s21/@s23, recuperación por ID/key/active: inicialmente GREEN, XML 6bb458 confirma 2/2 integradas. La misma ejecución 84c0ad detectó 19 fallos heredados en los contextos acotados: faltaba JdbcTemplate para construir la nueva factory. Se añadieron JdbcTemplate, PlatformTransactionManager y ObjectMapper sólo a esos fixtures de configuración. Ningún doble en la suite HTTP/PG. Regresión de los 25 casos GREEN 987837; el selector PIT actualizado también compiló sin error.

3. @s14, replay HTTP real: inicialmente GREEN 94aff5, un caso focal. Respuesta 200, Location original y DTO idéntico; las filas completas de sesiones y outbox no cambian.

## Entrega congelada

Spotless real focal en los cuatro Java propios: 16fb5f, dc6472, 91ec3b y 097e4d. Segunda pasada IS CLEAN: a854fe, a2279d, 2dbf91 y 2ef976. Modo spotlessIdeHook absoluto; no se atribuye un check global a los tasks Check omitidos por ese modo.

Regresión final 104b16 EXIT0, XML 4bfe94: 75 casos, cero fallos/errores/omitidos. Desglose: WorkSessionIntegrationTest 3, WorkSessionApiTest 49, ApplicationWiringTest 16, ProjectStateConfigurationTest 7. Incluye el nominal como parte de la regresión tras formato; no hubo otra ejecución nominal separada.

SHA256:
- ApplicationConfiguration.java: 17AF002A3FBB87E044014A9034E118D57A34DD158F239EC02E947E154C769FE3.
- WorkSessionIntegrationTest.java: 78CA31AE4B2A8CC69D47CD5427939DAAE35DAFE83FD5FE8116CBB36836AECB05.
- ApplicationWiringTest.java: EFF5176AACA29F1591193A7505B1C701F4976046320F3575B08E8F6FB3D09CCF.
- ProjectStateConfigurationTest.java: B15D49EF5976276A07B890C1A1F9B58B78D20231F4379DDFD8BD7A3AF73B865F.

Store y núcleo no se editaron. El freeze se notificó directamente a los autores de smoke/E2E. Sin init global, PIT, Git ni publicación de esta pista; root coordina las puertas posteriores. La integración prueba HTTP, seguridad, wiring y PostgreSQL; no acredita por sí sola el transporte Rabbit, la UI ni todas las carreras transaccionales.
