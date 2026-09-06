# Destilación de reschedule — borrador revisable

Fuente: sección13 de project-spec.md, checkpoint5512b24 y dictamen APPROVED para destilar en review_reschedule_spec.md. Autorización global vigente; Ponytail full/Caveman lite. Rol independiente gherkin_author: no producción, tests ni ejecución de suites. Init78050 vigente comunicado por coordinador. Este documento no aprueba TDD.

Archivo: features/reschedule.feature. **41 escenarios estables @s1–s41, 28 Scenario Outline y156 casos representativos expandidos** (una fila de Examples = un caso; alternativas dentro de una celda no se presentan como ejecuciones realizadas). No contiene @approved. Feature13 pasa exclusivamente de pending a spec_ready;12 continúa done y14 pending.

## Mapa de cobertura

| Sección13 / comportamiento | Escenarios |
| --- | --- |
| Identidad estable, creación inmutable, ausencia de proyección, compatibilidad de recibo11 | @s1–s3, @s36 |
| Estado cerrado, ETag/BIGINT textual, headers y precedencia revisión/cancelación/agotamiento | @s4–s6, @s18–s19 |
| Preview cerrado, exclusión por ID, duración/zona editables, DST/rango/futuro y ausencia de cambio | @s7–s11 |
| Cancelación histórica/completed/sin preferencia, estado terminal y replay | @s6, @s12, @s15 |
| Recibos cerrados, reloj único microsegundos no monótono, revisión causal | @s11–s15, @s25, @s32 |
| Key por tarea compartida mover/cancelar y separada de creación; existencia antes de recibo | @s14–s15, @s19, @s21, @s41 |
| Historial20/21, orden estable, terminal20 sin continuación, cursores y privacidad | @s16–s18, @s39 |
| Atomicidad, migración sin reescribir creación, ausencia de estado materializado por GET | @s1, @s20–s25, @s41 y cabecera normativa |
| Carreras del mismo bloque/key, capacidad entre proyectos y owners independientes | @s21–s23, @s41 |
| Cambios de estados/preferencia en ambos órdenes; snapshots de preview y Today | @s23–s24 |
| Reinicio/ACK perdido y recibo independiente de retención de outbox | @s25, @s33–s36 |
| Evento13 campos, before/after4, octava ruta, zonas históricas y garantías previas | @s26–s27 |
| Editor inline, sin N+1, antes/después, consentimiento, Intl desconocido | @s28–s31 |
| Validación estricta de respuestas y confirmación frente a intención retenida | @s31–s32, @s36 |
| Incertidumbre,404 no rollback, reenvío manual, conflicto y CSRF | @s33–s35, @s38 |
| Confirmación histórica separada de lista/estado, errores independientes | @s36, @s39 |
| Sesión/ruta/generación, respuesta obsoleta antes de observer, foco y salida | @s37–s38 y herencia explícita11@s52–s53 |
| UX30, anchos/breakpoints/altura, zoom nativo, tres motores y recorrido real | @s40 |

Herencia precisa: schedule_block.feature @s4–s10 para límites temporales y resolución; @s11–s17 para presupuesto, solape y elegibilidad; @s19/@s23/@s62 para seguridad/JSON; @s37 para protocolo de publicación; @s40/@s46 para validación de preview/DTO; @s52–s53 para privacidad. Se explicitan las sustituciones de lectura vigente/cancelación y se conserva Today15/read-only12. No se duplican325 filas de creación ni se especifican features14+.

## Revisión del primer borrador

Root pidió corregir: fixture imposible de61 segundos en UTC con locales de minutos (sustituida por1441minutos y error endLocal OUT_OF_RANGE); aserción Block9 sólo cuando hay DTO; cierre del editor sin fingir recuperación montada; cancelación terminal sin proyección posterior imposible; Location exigido sólo en POST; carrera de misma key sobre bloques distintos sin preferencia; competencia de presupuesto entre dos movimientos. Se añadió únicamente @s41 por esa carrera. Revisión backend precisó diez campos de preview; ETag de bloque permanece header.

Escritura inicial falló antes de crear archivo por límite Windows206 de longitud del comando. Se usó patch por bloques, sin sortear rechazo de aprobación ni tocar rutas protegidas. Validación documental4b42de: numeración contigua41, un When por escenario, tablas con ancho consistente y placeholders declarados, conteo156 y sin @approved; JSON parse confirma sólo estado13 cambiado. Diff-check sin errores (aviso de normalización CRLF existente). Es validación estructural propia, no parser Gherkin ni pruebas de producto.

Pendiente revisión final del coordinador. No activar in_progress ni iniciar TDD desde esta entrega.
