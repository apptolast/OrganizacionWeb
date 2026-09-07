# Preparación de wiring HTTP14

Lectura sin compilación ni cambios Java, mientras core termina su corrección de precedencia. Ponytail full y Caveman lite. HTTP aislado aprobado en 175878c; todavía pendiente de copia por root.

## Registro mínimo

Decisión posterior de root: registrar el Store mediante una factory concreta en ApplicationConfiguration, sin añadir anotaciones ni reabrir su paquete congelado. Tres factories en total; una única instancia implementa ambos puertos.

- ApplicationConfiguration necesita dos factories: StartWorkSession(WorkSessionStarting, Clock, ZoneCatalog) y ReadWorkSessions(WorkSessionQueries). Ambas clases ya implementan sus interfaces HTTP.
- PostgresWorkSessionStore implementa los dos puertos y recibe JdbcTemplate, PlatformTransactionManager y ObjectMapper. En el corte leído no tiene anotación de componente ni factory. Registrar una sola instancia; el patrón existente en PostgresTodayQueries permite añadir @Component, coordinando esa línea con el autor de Store. Alternativa si se reserva todo el registro a config: una factory explícita de Store, sin crear otra capa.
- Clock, ZoneCatalog, Jackson, datasource y transaction manager ya existen. No añadir sustitutos de infraestructura ni duplicar puertos.

## Primer oráculo integrado, después del freeze

Nueva WorkSessionIntegrationTest con @SpringBootTest, @AutoConfigureMockMvc y PostgreSQL 17.9-alpine real, siguiendo TodayApiTest. Preferir Clock.fixed de prueba mediante @TestConfiguration/@Primary; mantener catálogo, casos de uso y persistencia reales. Fixture SQL de proyecto/tarea propios y disponibilidad Europe/Madrid, sin alterar sus estados durante el nominal.

Primer caso: POST autenticado con CSRF por cabecera, Origin permitido y key propia devuelve 201, Location, no-store y siete campos. Un reloj de nueve decimales produce los seis microsegundos esperados. JdbcTemplate verifica una sesión running con esa key y un evento WorkSessionStarted.v1 cuyo aggregateId es la sesión; proyecto, tarea y planificación permanecen intactos.

Después de ese GREEN, añadir un caso de recuperación/replay: GET ID, key y active concuerdan con el inicio persistido; POST idéntico devuelve 200 y no aumenta sesiones/eventos. Cada comportamiento se incorpora por ciclo, sin una batería adelantada.

## Regresión necesaria

El cleanup explícito incluye work_sessions y las tablas heredadas, como el hotfix ya aplicado. ApplicationWiringTest y ProjectStateConfigurationTest construyen ApplicationConfiguration con puertos de prueba: al añadir factories requerirán declarar los dos puertos nuevos en esos contextos acotados. Esto no sustituye infraestructura en pruebas HTTP/PG.

Tras integración: foco nuevo, 49 HTTP y regresiones pertinentes de wiring/configuración; root coordina el gate global. Este plan no atribuye resultados ejecutados, privacidad transaccional ni aprobación completa de feature14.
