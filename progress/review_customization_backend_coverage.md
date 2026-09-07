# Revisión de cobertura backend de personalización21

Revisión de lectura de C, 2026-09-08. Contrato: `features/custom_views_fields.feature`, 42 escenarios. Los ejemplos Gherkin no son un conteo de pruebas. No se ejecutaron suites ni se modificaron fuentes durante esta auditoría. El mapa sigue las aserciones y los fixtures, no los prefijos de los nombres.

Dictamen acotado actualizado: las familias backend tienen evidencia en las capas correspondientes; los dos límites puntuales detectados en @s18/@s20 quedaron cubiertos por el refuerzo revisado debajo, sin cambios de producto. No quedan huecos concretos identificados en esta auditoría backend. @s27–28 son obligaciones del cliente y requieren la revisión de B; @s29–40 y el dictamen UX siguen fuera de esta aprobación. Esta revisión no declara terminada la feature ni sustituye la revisión independiente de los tests de C ya realizada por root.

## Evidencia y niveles

La regresión final de A, `customization_backend_final_results.json`, registra EXIT0 372d3e: 315 pruebas en 11 suites, cero fallos, errores u omisiones. Se verificaron independientemente los once hashes de XML de `customization_backend_final_xml` contra ese resumen. El manifiesto informa 428 entradas antes/después sin cambios. El log final tiene SHA256 `38B85093EA5E7FBC28CEEEF7BD07CD00A24AE1AB8D065229FC2434CE5C60F530`. Es regresión focal, no global ni mutación.

Abreviaturas del mapa:

- **Puras**: ReadCustomizationTest, SaveCustomizationTest, CustomFieldCommandsTest, ReadCustomFieldValuesTest, SaveCustomFieldValuesTest y CustomFieldValuesTest. Prueban reglas/callbacks y Clock; sus dobles no acreditan transacciones PostgreSQL.
- **PG**: CustomizationPersistenceTest (19) y CustomFieldValuesPersistenceTest (61), PostgreSQL Testcontainers y casos de uso reales. CustomizationWiringTest (4) acredita beans conectados.
- **MVC**: CustomizationApiTest (134), filtros y controlador con puertos mockeados; no prueba durabilidad.
- **HTTP+PG**: CustomizationHttpPersistenceTest (8), Spring completo, login y MockMvc con PostgreSQL/beans reales. No socket ni navegador.
- **E2E**: customization-persistence.spec.mjs, un recorrido Docker/HTTP real con respuesta perdida y reinicio exclusivo de API. No acredita la UI21, ausente en ese corte.

## Mapa de obligaciones

| Escenario | Oráculo y capa acreditados | Límite relevante |
| --- | --- | --- |
| @s1 | ReadCustomization puro, PG ausencia sin INSERT, MVC defaults PROJECT/TASK y propietario autenticado. | Los defaults HTTP y la ausencia física se verifican en capas distintas. |
| @s2 | SaveCustomization conserva orden/definiciones; PG guarda vista; MVC representación cerrada y revisión exacta. | Sin atribución visual de columnas. |
| @s3 | Commands alta/UUID/tipo/orden y defaults TASK; wiring y PG crean definición; HTTP+PG crea y consume una definición. | La combinación de tipos completa procede de pruebas puras y E2E. |
| @s4–5 | Commands valida etiquetas, unicidad incluyendo inactivas, White_Space, 60 puntos Unicode, caso y formas compuesta/descompuesta distintas; MVC orden label antes de type. Ciclos A76 y anteriores. | No normalización Unicode inventada. |
| @s6 | Commands rechaza la decimotercera contando inactivas; PG ciclo77 permite la duodécima sin configurar TASK. | No se infiere el límite sólo del caso de rechazo. |
| @s7 | PG ciclo79 ejecuta desactivar/reactivar/renombrar/restaurar vista y conserva ID/tipo y fila de valores con xmin/ctid/revisión MAX; MVC actualización y E2E ocultación durable. | Conservación física y proyección activa comprobadas por separado. |
| @s8 | Puras: revisión anterior falla antes de no-op y Clock; revisión vigente idéntica incluso MAX no consume reloj. PG no-op conserva fila física. | MVC412 sólo acredita traducción. |
| @s9 | PG primeras altas/vistas ciclo83 y configuración existente ciclo75: espera advisory observada, un cambio/412 o dos no-op sin escritura. | No se prefija ganador en esas carreras. |
| @s10 | PG PROJECT/TASK activos, null sin fila, 0/false, inactivos ocultos; ciclo73 observa RR/read-only y un writer real entre lecturas. MVC proyección y ETag compuesto. | Snapshot por solicitud, no entre solicitudes. |
| @s11 | Puras validan cuatro tipos, extremos NUMBER, fechas públicas, null/0/false y espacios; PG precisión durable y TEXT de 1000 puntos; HTTP+PG y E2E confirman valores reales. | Transportar BigDecimal enorme en MVC no significa que el dominio lo acepte. |
| @s12 | Puras rechazan tipos, fracciones/rangos, fechas inválidas y texto NUL/surrogate/1001 puntos con índice original. Save valida antes de confirmar conjunto. | No se presenta cada ejemplo puro como una transacción PG independiente. |
| @s13 | Save exige conjunto activo completo y conserva inactivos/orden de definición; HTTP rechaza duplicados estructurales antes del puerto. | Inactivo/desconocido/incompleto se validan después de propiedad/revisión; duplicado fieldId es estructural según aclaración del contrato. |
| @s14 | Save compara números canónicos/null y orden sin Clock; PG no-op físico; HTTP+PG compara cuerpo, ETag y fila exactos para 1.0/1e0. | El nombre histórico s16_canonicalNumberNoOp corresponde aquí, no a carreras. |
| @s15 | Save compara los componentes de ambas revisiones antes de tipos; PG ciclo74 fuerza schema ganador, observa espera y conflicto; HTTP+PG cambia etiqueta y conserva valores ante412. | Ciclo74 es orden controlado, no ganador aleatorio. |
| @s16 | PG valores existentes cambio/no-op ciclo75 y primeras escrituras vacías ciclo62 observan bloqueo y resultados físicos. | No atribuir concurrencia al test HTTP+PG de no-op secuencial. |
| @s17 | PG propietario/contexto y definición ausente; HTTP+PG GET/PUT de proyecto ajeno, tarea ajena y tarea en proyecto propio incorrecto; MVC404 privado sin ETag. | El cuerpo puede contener problem+json, sin datos privados. |
| @s18 | PG completedTask conserva filas completas de tarea/proyecto, valores distintos de proyecto/subtarea y cinco tablas de hechos pobladas mediante comandos reales; HTTP+PG conserva tarea completa. | Refuerzo87 revisado; evidencia original no se reinterpreta. |
| @s19 | PG ciclo80 guarda 1000 puntos Unicode en proyecto completed, relee valor exacto y compara todas las columnas de proyecto; outbox sigue vacío. | El fallo inicial fue del INSERT del fixture, no del producto. Ya cubierto, no precisa otro caso duplicado. |
| @s20 | Puras Clock/rango/MAX; PG ciclos67–72/78/82/84–86 integridad JSON/metadata/UUID, precisión, tabla indisponible, error SQL, commit diferido y affected==0; refuerzo88 UPDATE de dos activos con inactivo previo; MVC503. | Corrupción por vistas/triggers es fault injection; no afirma que constraints permitan naturalmente esas filas. |
| @s21 | Puras y PG79: sólo cambia la revisión del agregado escrito; configuración o valores MAX no impiden no-op/cambio del otro agregado. | Se comprueba xmin/ctid de valores conservados. |
| @s22 | Save/Commands/Values monotonicidad; ciclo81 años0001/9999 y truncado microsegundos con una captura Clock. | No extrapolar relojes del navegador. |
| @s23 | MVC auth/CSRF/origen/Accept/Content-Type/query/ruta con no interacción en puertos; HTTP+PG login real y guardas sin INSERT. | Accept se rechaza antes del comando mediante negociación local Spring. |
| @s24 | MVC cabecera ausente, débil/repetida/otra familia/scope/entidad, canonicalidad y límites antes del cuerpo; HTTP+PG conflicto compuesto real. | Sin exponer IDs/revisiones internas en JSON. |
| @s25 | MVC JSON cerrado, duplicados, extras léxicos, orden de campos/índices y decimal exacto; duplicado fieldId con ETag antiguo no invoca puerto. | Campo UUID del body INVALID_VALUE, distinto de ruta INVALID_FORMAT. |
| @s26 | E2E pierde respuesta sólo tras upstream200 real, GET durable, cambia vista y desactiva, reinicia API y autentica sesión nueva; compara IDs/ETags/filas y negocio/outbox. | GET confirma estado durable, no autoría de intención; no afirma UI ni reinicio de PostgreSQL. |
| @s27–28 | Obligaciones de decoder y confirmación del cliente. Backend provee DTO/ETag exactos verificados en MVC/HTTP+PG. | Pendientes del dictamen frontend; no se consideran cubiertas por Java. |
| @s41 | MVC firstPutOfProjectDefaults y Save primera escritura materializan configured=true/revisión0 aun con defaults. | Ausencia previa no equivale a no-op configurado. |
| @s42 | PG primeras escrituras[] y sólo inactivos/MAX, wiring y MVC conjunto vacío; conserva valor oculto y no crea esquema. | El primer PUT configura valores; el posterior idéntico no escribe. |

## Huecos y límites para el cierre

**Cierre de los puntos1/2 siguientes:** A reforzó únicamente CustomFieldValuesPersistenceTest, SHA256 `335116E06584D8BFF3CBB5D7F2DD46F5138E704849107B1E9A399FEA0C7983DC`. Se leyeron los dos oráculos finales: @s18 usa StartWorkSession/ChangeWorkSession.close/ChangeTaskStatus reales y exige cinco tablas no vacías antes de comparar su contenido íntegro; conserva además colección PROJECT propia y subtarea. @s20 parte de dos activos y un inactivo guardados, falla AFTER UPDATE y compara proyección, fila con xmin/ctid, esquema y proyecto. Clase62/62 EXIT0 35d7e9; XML `customization_backend_refined_green.xml` verificado con SHA256 `99C1CD3BCDD7B860E272A76085AACF815E369C70290E37C627AE7490676BAA8D`. No se suma este foco al315 anterior ni se atribuye otra regresión global. Los puntos históricos se conservan a continuación como origen del refuerzo, ya no como pendientes.

1. **@s18, fixture literal incompleto.** `s18_completedTaskWritePreservesBusinessFactsAndSeparateSubtaskValues` siembra valores de subtarea, pero afirma cero filas PROJECT en vez de sembrar y preservar valores propios distintos del proyecto. Tampoco siembra intervalos, historial o recibos anteriores. Nuestro E2E conserva filas proyecto/tarea y outbox real, pero tampoco llena esos hechos. Se comunicó a A/root para comprobar si existe otro oráculo o reforzar sólo este fixture. No se recomienda una matriz nueva.
2. **@s20, alcance del rollback.** El trigger SQL de valores demuestra rollback de INSERT en ambas tablas y el de configuración falla en commit diferido real. La colección de valores del fixture es vacía. La atomicidad de varios valores y conservación de inactivos se prueba en Save, pero no debe describirse ese trigger como rollback de una actualización previa de varios valores poblados. Es un límite de evidencia para revisión, no un defecto observado.
3. El E2E inicialmente verde usaba `->>` con helper trim y no acreditaba espacios externos del texto inactivo. Se preservó ese resultado; el refuerzo usa `->` y JSON.parse, compara exactamente `  texto privado  `, y pasó el mismo recorrido: EXIT0 67906e, log `customization_persistence_e2e_exact.log`, 569 entradas before/after iguales. No se reinterpretó la primera evidencia.

La bitácora HTTP+PG ya corrige el método histórico s14_completedTaskValues a @s18 y s16_canonicalNumberNoOp a @s14. Los nombres s9 de lecturas MVC se corrigieron a @s10 en refactor verde. Los logs anteriores conservan sus nombres originales.

Fuentes de trazabilidad: `tdd_custom_views_fields.md` ciclos1–86; `tdd_custom_views_fields_http.md`; `tdd_customization_http_persistence.md`; `tdd_customization_persistence_e2e.md`. La UI sigue en corrección coordinada y el informe `review_customization_ui.md` permanece preliminar. No se acredita mutación, CI global, matriz UX, WebKit ni dispositivos reales mediante estos resultados backend.
