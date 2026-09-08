# TDD — concurrencia de importación23

Clase propia ImportConcurrencyTest: PostgreSQL17.9 real con Testcontainers/Flyway y adaptadores/casos de uso reales. Sin mocks de persistencia, puertos fijos ni tráfico de producción. Cada caso usa propietario e identidades nuevos; el contenedor se retira al terminar. Barreras CountDownLatch y pg_blocking_pids acreditan espera real; los tiempos de polling no sustituyen el oráculo de bloqueo. No se modificó producto para estos casos.

Contexto inicial aprobado f3bed6f: Store/ImportRecordValidator copiados y fijados da98adf; ImportCounts.minus faltante causó compilación fallida f14f83, no RED contractual. Dependencia real del mismo corte copiada en72adb28. Contexto final aprobado d802f7d trasladado sólo sus dos fuentes en cafe6ee; incluye comparación de claves alternativas. Validador propio corregido V19 en0a37e67. No se copian tests de A ni se atribuyen guardas posteriores de su WIP.

| Grupo | Oráculo | Resultado |
| --- | --- | --- |
| @s24 writer de personalización ya leyó ausencia, PROJECT/TASK | Import espera advisory; writer confirma; import409 y conserva identidad ganadora, sin recibo | RED724303 en nominal:503 por clave alternativa; tras contexto corregido,2GREEN en repetición9bc67f (grupo tenía fallo de fixture de bloque separado) |
| @s24 writer iniciado durante import, PROJECT/TASK | Espera; SaveCustomizationView ve identidad/revisión importada y rechaza precondición antigua; conserva vista y recibo | Inicialmente2GREEN0bdbc6 |
| @s24 contención advisory | Espera acreditada; vence lock_timeout y devuelve503 sin datos/recibo | Inicialmente GREEN4c1274 |
| @s23 COMMIT rechazado por constraint diferida | Recorre inserción y creación de recibo; rollback de configuración, recibo y ningún evento | Inicialmente GREENc7db7f |
| @s24 cinco familias,6recorridos | CreateProject; ChangeTaskStatus con historial; CancelBlock con proyección/recibo; ChangeWorkSession.close con intervalo/recibo; SaveAvailability; SaveAppearance. Esperan import, luego conservan efecto/evento propio | Inicial4PASS/2errores de fixture; corregidos offsetsUTC y revisiones iniciales reales; final6GREEN99a9bf |

Errores de fixture preservados: PlanBlock exige offsets resueltos explícitos en este checkout; las revisiones iniciales de bloque/sesión son1, no0. Se usan offsetsUTC explícitos y revisión obtenida por adaptador público. No se cambia producto, el comportamiento de bloqueos ni los asserts de negocio. Los XML iniciales y la repetición intermedia se conservan.

Regresión final12/12, cero fallos/errores/skips y SpotlessJavaCheck EXIT0 e8b222,19s. Evidencia: import_concurrency_final.log/.exit/_xml.xml. Grupo de familias importa envelope vacío mientras retiene locks y acredita que no elimina datos ya existentes ni añade eventos; sólo el writer agrega el evento esperado. Esto prueba intercalaciones representativas, no todas las planificaciones posibles ni el cálculo completo de cuotas/unión que cubre A. El rechazo COMMIT es comprobable, no una simulación de pérdida indeterminada de respuesta.

## Refuerzo final tras revisión de @s24

El corte12/12 anterior sólo acreditaba locks con envelope vacío en el grupo de familias; no demostraba su Given de registros relacionados ni la cuota. Se preservan XML/log y freeze iniciales. Los seis recorridos ahora generan una exportación propia real mediante PrepareExportData/PostgresExportDataQueries, con proyecto+tarea relacionados (y bloque/disponibilidad/sesión en sus variantes); importan la réplica idéntica y conservan todos los oráculos de espera, cambios y eventos. Inicialmente6/6 GREEN564f8e. Un error de edición del helper de fixture provocó compilación fallida0ab624 antes de ese resultado; se corrigió sin cambios productivos y se conserva el log.

Caso adicional de cuota: exportación real con3activos+1idea, destino inicialmente sóloidea. Import inserta3activos y retiene locks; ChangeProjectStatus sobre PostgresProjectStatusEditing espera su FOR UPDATE y después cuenta3, rechaza ACTIVE_PROJECT_LIMIT, deja idea/revisión0 y cero eventos. Recibo acredita3insertados+1idéntico. Inicialmente GREENae5c19, sin sustituirlo por CreateProject ni simular el contador.

Regresión refinada13/13, cero fallos/errores/skips y SpotlessJavaCheck EXIT0 953002,21s. Evidencias import_concurrency_refined_final.log/.exit/_xml.xml. Fuentes contextuales d802f7d permanecen sin modificaciones propias. El test representa intercalaciones concretas, no una demostración exhaustiva de ausencia de deadlocks; el timeout y el rollback comprobado tienen sus oráculos separados.

## Cierre de @s27 y mapa definitivo

Se corrigen nombres y atribuciones: writer de personalización previo corresponde a @s25; writer iniciado durante import corresponde a @s26; contención corresponde a @s27. El grupo de familias y cuota sigue @s24. Las primeras tablas de esta bitácora describen resultados históricos con el prefijo anterior, no cobertura adicional por renombrar.

Tres ejemplos adicionales de @s27, sin tocar fuentes de A ni límites reales:

- Tabla projects ocupada después de que import ya adquirió appearance: lock_timeout real,503, ningún dato/recibo y ambos advisory+15tablas nuevamente adquiribles con NOWAIT. Inicialmente GREEN90f081.
- Orden inverso real: PostgresProjectStatusEditing retiene projects; import retiene appearance y espera projects; entonces SaveAppearance del writer espera appearance. PostgreSQL aborta import con SQLSTATE40P01, StorageUnavailable503, mientras writer confirma proyecto+apariencia y sólo su evento. Import no deja configuración/recibo. Se conservan ambos outcomes; no se ajusta deadlock_timeout ni se repite por azar. Inicialmente GREENccfeeb; XML imprime DEADLOCK import=40P01 writer=SUCCESS.
- Trigger de prueba en INSERT del recibo ejecuta pg_sleep(11); statement_timeout real10s cancela con SQLSTATE57014 y503. Revierte configuración/recibo, cero eventos y locks liberados. Inicialmente GREEN4ba15e. No se reducen los tiempos para acelerar el oráculo.

El helper final acredita liberación de locks también tras contención advisory. Regresión completa16/16, cero fallos/errores/skips y SpotlessJavaCheck EXIT0,28s. Evidencias import_concurrency_complete_final.log/.exit/_xml.xml. No campaña de mutación adicional. Las bitácoras de los cortes12 y13 se conservan como import_concurrency_partial_tdd_snapshot.md e import_concurrency_refined_tdd_snapshot.md; el primero recuperado por su hash exacto original.

Límite: el rechazo diferido @s23 de esta clase escribe configuración antes del recibo; la variante de varias colecciones pertenece a A y no se atribuye a este test. La concurrencia usa PostgreSQL real y adaptadores, no navegador/socket.
