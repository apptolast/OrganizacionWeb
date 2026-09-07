# Apariencia20: mapa backend previo a TDD

2026-09-07. Sólo lectura/propuesta sobre sección20 todavía en revisión. No código, pruebas ni migraciones ejecutadas. Scope: tres preferencias propias; ningún evento, recibo, historial, Clock de consultas, catálogo de zonas ni cambio a contratos1–19.

## Puertos y archivos mínimos propuestos

- domain/Appearance.java: fila inmutable id/owner/theme/accentLight/accentDark/version/updatedAt. No createdAt adicional: no lo exige DTO ni comportamiento. AppearanceRevision conserva UUID+long; ausencia mediante id null/version0, sin mezclar ETag de otra colección.
- domain/AppearanceValues.java (o tipo nominal equivalente exigido por primer test): enum Theme y dos colores canónicos. Una sola implementación de fórmula sRGB y seis fondos constantes por tema, con orden theme/light/dark y FieldError existentes. No biblioteca de diseño genérica ni colores dinámicos adicionales.
- application/ReadAppearanceUseCase.get(owner) → Optional<Appearance>; ReadAppearance delega a AppearanceQueries.find(owner). Defaults y ETag los mapea HTTP desde ausencia confirmada, sin INSERT ni captura de reloj.
- application/SaveAppearanceUseCase.execute(owner, expected, values) → Appearance; SaveAppearance usa AppearanceEditing.save(owner, Function<Optional<Appearance>,Appearance>) y Clock inyectado. Sigue puerto callback ya real AvailabilityEditing; no servicio que haga GET previo fuera de transacción.
- application/AppearanceConflictException para412 específico; reutilizar ValidationException/FieldError y StorageUnavailableException para400/503, sin nuevos envelopes globales.
- adapter/persistence/PostgresAppearanceStore implementa ambos puertos; wiring sólo factories ReadAppearance/SaveAppearance en ApplicationConfiguration. HTTP de C: AppearanceController/AppearanceApiTest, parsing estricto y DTO5/ETag. No tocar controller de disponibilidad para compartir un parser prematuramente.
- Migración siguiente disponible V19__appearance_preferences.sql (el número19 de Flyway no es feature19): tabla appearance_preferences, UUID PK, owner_id UNIQUE NOT NULL, theme/acentos/version/updated_at NOT NULL. CHECK enum, formato canónico #RRGGBB y version>=0. No FK ficticia a proyectos ni tablausuarios propia; mismo owner textual derivado de Principal que disponibilidad. No índice adicional: UNIQUEowner cubre GET/lock. Contraste se valida en dominio; evitar replicar cálculo flotante complejo en SQL.

Nombres son handoff previo, no tipos existentes ni autorización de implementación. Congelar bundle real compilable nominal antes de que C construya mocks/HTTP.

## Transacciones y precedencia

GET único SELECTowner devuelve fila y tag del mismo resultado; no necesita RR multipágina. Lectura JDBC de updated_at como OffsetDateTime, no copiar getTimestamp de disponibilidad para años extremos.

PUT en READ_COMMITTED explícito y transacción de escritura: SELECT owner FOR UPDATE; callback valida expected contra identidad/version propia; compara valores ya canónicos; no-op retorna exactamente fila sin UPDATE/Clock/UUID; cambio calcula addExact(version,1), captura Clock una vez, trunca microsegundos y conserva max(prior.updatedAt,now). Primera creación versión0. UPDATE predicado owner/id/version debe afectar exactamente1; cualquier supresión o fallo de commit503 y rollback. El resultado sólo vuelve tras commit.

Ausencia no admite lock de fila: INSERT ON CONFLICT(owner_id) DO NOTHING. Tras espera,0filas + fila propia visible→412;0filas sin fila durable→503 (trigger/supresión), siguiendo disponibilidad. No lock global ni reintento automático. Dos altas y dos actualizaciones con misma revisión deben tener un ganador, no prefijar cuál en test. UUID de otro owner bien formado devuelve412 sin exponer sus datos.

Orden de responsabilidades ya definido: seguridad/negociación→query→If-Match sintaxis→JSON/campos/contraste→revisión almacenada→no-op→incremento/reloj/escritura. Por tanto, validación de contraste no debe relegarse a después de revisión por el callback. Canonicalizar antes de comparar permite minúsculas como no-op. El If-Match fuerte es válido aquí porque todo el cuerpo permanece estable mientras la revisión permanece estable; no hay serverNow/neto dinámico como en sesión15.

## Precisiones propuestas antes del freeze

1. Especificar error temporal: para cambio real, Clock no representable en UTC0001–9999 o incapaz de entregar instante→503 STORAGE_UNAVAILABLE y cero escrituras. No-op de fila válida, incluso versiónMAX, no requiere Clock. La prosa actual fija rango y max, pero no el código ante Clock fuera de rango.
2. GET de fila persistida incompatible (enum/color/contraste/version/updatedAt) debe503, no defaults niDTO inválido. El CHECK simple no prueba contraste; mapper debe validar invariantes reutilizando dominio, con wrapper sin filtrar SQL. No reconstruir ETag válido para datos incoherentes.
3. Los seis fondos y defaults están cerrados, fórmula sin redondeo/tolerancia. Acordar vectores concretos compartidos Java/TS de aceptación/rechazo cerca de4.5, sin usar el mismo cálculo bajo prueba como oráculo esperado. La comprobación de negro/blanco del botón deriva del color; no persistir onAccent como cuarto campo. La inspección real de superficies/foco/forced-colors pertenece a UI/UX y no queda acreditada por tests Java.

Estas precisiones se comunicaron a B; no se ha editado su contrato. No exigen nueva matriz histórica ni ampliar personalización.

## Orden TDD acotado después de aprobación

Nominal GET vacío y puerto; valores válidos canónicos; PUT primera fila y GET; mismo contenido no-op; revisión inválida y precedencias; cambio/version/reloj; extremos yoverflow; contraste de ambos temas con oráculos independientes. Después PG real: owner y aislamiento, carrera alta/actualización con espera observada, supresión/rollback/commit503, reinicio y ausencia de eventos, GET readonly, upgrade aditivo sin alterar hechos. HTTP contra bundle real: query/If-Match/JSON/códigos/DTO, seguridad heredada focal. No duplicar toda disponibilidad ni investigar escenarios de sesiones ajenos.

Fixtures que limpien estado de preferencias deberán incluir appearance_preferences explícitamente cuando corresponda; no CASCADE genérico ni edición de migraciones V1–V18. Gates globales y mutación sólo tras freeze/revisión; ninguna campaña forma parte de este mapa.

Evidencia leída: project-spec.md sección20; AvailabilityController, SaveAvailability, AvailabilityEditing, PostgresAvailabilityStore y migraciones existentes (hastaV18). Limitaciones detectadas del precedente: versión+1 sin addExact, Clock consultado antes de no-op y JDBC Timestamp. Reutilizar patrón de transacción/errores; no trasladar esas limitaciones al nuevo contrato ni abrir refactor de disponibilidad sin necesidad20.
