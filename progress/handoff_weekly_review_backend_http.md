# Handoff backend/HTTP19 y revisión de coherencia

Lectura parcial previa al freeze root, no autorización de código. Normativa
project-spec19 y borrador features/weekly_review.feature leídos. Las familias
backend @s1–24 son coherentes con plan proyectado, intervalos reales, legacy,
capacidad actual y lectura RR. Las precisiones25/28/30 ordenadas por root están
en edición de C; este documento no certifica ese archivo mutable como final.

Dos notas editoriales remitidas a C: @s11 añade un tramo terminado exactamente
al inicio de semana sin sembrarlo en Given; @s14 añade una consulta futura al
resultado de la consulta del día. Expresar esas reglas sin afirmar que el mismo
When ha observado dos consultas; no requieren nuevas familias ni producción.
No hallazgo backend adicional bloqueante. Orden obligatorio: validar entrada
estable antes de query; primera lectura establece RR, después único Clock.
La marca actual de una sesión seleccionada por intervalo antiguo también se
compara con serverNow, aunque ese intervalo ya esté cerrado.

## Firmas mínimas propuestas para primer bundle real

Son nombres propuestos, no archivos existentes ni stubs que C deba crear.
Se fijarán al nominal compilable exigido por test y se entregarán por root.

- `application.ReadWeeklyReviewUseCase`:
  `WeeklyReview get(String owner, LocalDate date, String zoneId)`.
  date/zoneId null significan omisión. Date real y estructura query se parsean
  en HTTP; normalización de semana, rango civil y pertenencia al catálogo son
  invariantes de aplicación antes de consultar PG.
- `application.ReadWeeklyReview` implementa el puerto; constructor
  `(WeeklyReviewQueries queries, Clock clock, ZoneCatalog catalog)`.
- `application.WeeklyReviewQueries`:
  `WeeklyReview read(String owner, Function<Optional<Availability>, WeeklyReviewWindow> window)`.
  Reutiliza la frontera callback de TodayQueries. El adaptador invoca window
  sólo después de la lectura de preferencia que establece snapshot RR. Así
  Clock permanece en aplicación y no hay doble transacción ni snapshot falso.
- `domain.WeeklyReviewWindow`: fechas/zona/capacidad y serverNow necesarios
  para seleccionar y sumar. Su forma interna se mantiene bajo propiedad A;
  HTTP no la importa ni crea ventanas.
- `domain.WeeklyReview`: record de once componentes normativos con LocalDate,
  Instant, String y List<Day>, Totals, long unquantifiedSessionCount. Day y
  Totals pueden ser records anidados: Day(date,startAt,endAt,long planned,
  long worked,Long capacity), Totals(long planned,long worked,Long capacity).
  Sus accessors públicos usarán los nombres completos plannedMicroseconds,
  workedMicroseconds,capacityMicroseconds. Sin JSON/JDBC/Spring en dominio.
- `domain.WeeklyReviewTimeOutOfRangeException`: excepción concreta sin código
  variable; C la traduce localmente al409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE.
  `ValidationException`/FieldError y `StorageUnavailableException` existentes
  mantienen400/503; no reutilizar excepción de comandos de sesión con
  significado ajeno ni cambiar sus handlers/títulos.

No Query record sólo para dos parámetros, servicio de calendario, fábrica ni
interfaces genéricas de reporting. Los datos de agregación internos aparecerán
únicamente cuando un test los exija; no forman parte del bundle HTTP.

## División de propiedad

A: dominio, puertos, caso de uso, PostgresWeeklyReviewQueries, wiring de los
beans y tests app/PG. Sin nuevas tablas previstas. Preserva V14 y metadata.

C: WeeklyReviewController y WeeklyReviewApiTest en su árbol aislado. Constructor
con ReadWeeklyReviewUseCase real; Principal suministra owner. Mapea dominio a
DTO cerrado11/day6/totals3; longs a strings, capacidad nullable, fechas/Instant
canónicos. GET sin body, sin propiedad editada, sin CSRF exigido. Contrato de
query exactamente date/zoneId; no reutilizar cursor18. Sin cálculos semanales
ni Clock en controller. No copiar implementación mutable ni inventar mocks de
puertos aún inexistentes: esperar bundle nominal revisado.

B: cliente/UI. La edición del formulario es borrador hasta «Mostrar semana»;
URL aplicada date/zoneId gobierna GET, Back/recarga. «Esta semana» omite date y
conserva zona. Estos ajustes ratificados por root necesitan reflejarse en la
normativa final además de Gherkin; A no modifica shared files durante esta
revisión read-only.

Contador y duraciones aceptan como máximo Long.MAX_VALUE antes de BigInt,
no números arbitrariamente largos. Date solicitada debe corresponder al lunes
normalizado devuelto; sin date la coherencia requiere metadata serverNow sin
recalcular fronteras mediante TZDB cliente. Su precisión exacta se cerrará en
la revisión de @s25 para no crear un oráculo imposible con catálogo distinto.
