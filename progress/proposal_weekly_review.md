# Propuesta19 — revisión semanal

Propuesta acotada para revisión; no normativa aprobada ni implementación.
Sólo este documento. Autorización global vigente, Ponytail full/Caveman lite.
Root ejecuta init y resuelve el fallo heredado antes de producción19.

## Resultado y alcance

Una pantalla privada «Revisión semanal» compara el plan vigente con el trabajo
real de siete días y muestra capacidad actual y días sin presupuesto. Cuenta
trabajo de sesiones abiertas y cerradas: esperar a CLOSE ocultaría trabajo ya
realizado. La consulta no completa tareas, no modifica sesiones ni crea una
revisión persistida. Sin puntuación, racha, porcentaje de productividad,
recomendación automática, listado de hechos, exportación o personalización20.
No duplicar el historial18 ni su paginación.

## Semana y zona

GET `/api/v1/weekly-review` admite únicamente `date` y `zoneId`, opcionales y
no repetidos. date es fecha gregoriana exacta YYYY-MM-DD, año0001–9999; sirve
como día ancla y se normaliza al lunes de esa semana. Sin date, se usa el día
del único serverNow en la zona resuelta. zoneId explícita debe pertenecer al
ZoneCatalog existente; no offset arbitrario, alias inventado ni trim tácito.
Sin zoneId se reutiliza la preferencia vigente de disponibilidad; ausente o
ya no disponible en catálogo implica UTC con causa explícita.

Siete fechas lunes–domingo; ocho fronteras `date.atStartOfDay(zone)` calculadas
por Java Time del servidor, una por fecha, no sumar24h. Ventanas semiabiertas
[startAt,endAt). Día saltado por cambio de zona puede tener longitud0: suma0,
no error ni día inventado. Semana DST puede durar167/169h. Dos offsets de una
hora repetida cuentan como instantes distintos. En cambios históricos del
catálogo una nueva consulta puede redistribuir días; no se promete snapshot
permanente ni se modifican datos históricos.

date cuyo lunes o domingo salgan de0001–9999 se rechaza400 en date. Si una
frontera UTC, serverNow o su fecha local no cabe en rango público UTC
[0001-01-01,+10000-01-01), 409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE, sin fallback
para ocultar desbordamiento. El límite superior exclusivo de semana también
debe poder representarse públicamente; no devolver año10000. No limitar a
semanas pasadas: futuro presenta plan y trabajo0 hasta serverNow.

## Fuentes y sumas

Plan: `planned_blocks` con proyección vigente de `block_projections`, patrón
COALESCE de Hoy, sólo status planned. RESCHEDULED mueve el plan y CANCELLED lo
retira, incluso al revisar una semana pasada. Etiqueta «Plan vigente», nunca
«lo que habías planificado entonces». Proyecto/tarea completed no elimina
reservas ni trabajo; propiedad/contexto coherentes en todas las consultas.
No sumar originales más proyección, ni considerar planned_minutes de una
sesión o effective_end_at de EXTEND como reservas adicionales.

Trabajo: intervalos cerrados `work_session_intervals` y, sólo para running,
cola `[running_since,serverNow)`. Para running legado con running_since null
se usa started_at como hace el mapper15. paused/closed no tienen cola de
trabajo. Se interseca cada tramo con cada día y con el horizonte serverNow;
fin exacto en lunes00:00 no pertenece a la semana que empieza. Se cuenta una
sola vez: no sumar worked_microseconds a los intervalos. Este acumulado sirve
para comprobar integridad, no para distribuir fechas. workDate de CLOSE no
reparte trabajo: conserva su significado de atribución histórica16.

Descanso contemplado mediante días con presupuesto actual cero, sin castigo ni rachas. No calcular una métrica de pausas observadas ni inferir descanso real: los huecos entre intervalos simplemente no suman trabajo. Tiempo anterior al inicio y posterior al cierre tampoco cuenta.

Para cada día, duración es la suma de intersecciones reales en microsegundos.
Usar aritmética exacta (seconds*1000000+nanos/1000 o equivalente), sin redondeo
por tramo ni Duration.toNanos sobre el rango completo. Valores agregados
representables en long; overflow o datos seleccionados incoherentes devuelven
503 STORAGE_UNAVAILABLE, nunca wrap, saturación o página parcial. No fusionar
silenciosamente intervalos corruptos/solapados. Para sesión cuantificable,
intervalos ordenados no solapados, dentro de su vida y suma cerrada igual a
worked_microseconds; before extremo y tramo abierto deben ser coherentes.

Compatibilidad14: closed con changed_at null y sin intervalos/recibos carece
de final observable. No inferir que trabajó plannedMinutes ni hasta ahora.
Excluir esa sesión de duraciones y contarla en unquantifiedSessionCount sólo
si empezó dentro de la semana. workedMicroseconds representa sólo trabajo cuantificable, no total completo si el contador es positivo. Aviso «Hay sesiones antiguas iniciadas esta
semana sin duración registrada». El contador no asegura que ninguna sesión
antigua iniciada fuera pudiera haber cruzado la semana. Preservar V14 y filas
sin backfill inventado. Otras inconsistencias no se disfrazan de este caso
legacy y producen503 cuando afectan datos seleccionados.

Capacidad: presupuestos diarios de la preferencia ACTUAL, no histórico.
Sólo mostrar si su zona sigue disponible y coincide con la zona del informe;
si se eligió otra zona o no hay preferencia válida, capacityMicroseconds=null.
No convertir presupuesto entre zonas ni ajustar por DST: es duración elegida,
no longitud civil. Cero significa «Sin tiempo presupuestado actualmente»,
no descanso efectivamente disfrutado. Sin presupuesto conocido no calcular
exceso ni deuda. No bloquea mostrar plan/trabajo.

## Snapshot, seguridad y errores

Una transacción read-only REPEATABLE_READ carga preferencia, proyecciones,
sesiones e intervalos. Captura un único Clock truncado a microsegundos dentro
del caso de uso durante esa lectura; cada suma comparte serverNow. Si la hora
capturada precede una decisión/intervalo confirmado seleccionado (incluido
last_decision_at de EXTEND, aunque changed_at no cambie), responder
409 WEEKLY_REVIEW_TIME_OUT_OF_RANGE: no recortar un tramo durable para ocultar
reloj atrasado. Escritor confirmado antes del snapshot se ve entero; después
no mezcla proyección/intervalos. Siguiente GET puede ver nueva revisión.

Principal determina owner. No parámetro owner/project/task ni filtros nuevos.
Sin autenticación401 antes de query; después query desconocida/repetida o
forma inválida400 VALIDATION_ERROR, códigos heredados por campo (date/zoneId o
query). Fechas y zona se validan antes de consultar datos; preferencia inválida
por catálogo usa fallback descrito, preferencia SQL ilegible no. GET no exige
CSRF y no acepta revisiones/keys como mecanismos de escritura. Almacenamiento,
deserialización o COMMIT read-only fallidos:503 sin datos parciales ni SQL.
No recursos explícitos que produzcan404. Error inesperado conserva500 común.
Sin caché privada persistente; no ETag, snapshot entre requests ni outbox.
No consumer nuevo, nuevas tablas o agregados materializados.

## DTO cerrado

200 JSON de exactamente11 campos:
`weekStart`, `weekEnd`, `zoneId`, `zoneSource`, `availabilityZoneId`,
`serverNow`, `startAt`, `endAt`, `days`, `totals`, `unquantifiedSessionCount`.
weekEnd es domingo inclusivo, endAt es lunes siguiente exclusivo. zoneSource:
EXPLICIT, AVAILABILITY, UNCONFIGURED o UNAVAILABLE; availabilityZoneId conserva
zona guardada o null aunque no se use, siguiendo Hoy.

Cada uno de los siete days tiene exactamente6 campos:
`date`, `startAt`, `endAt`, `plannedMicroseconds`, `workedMicroseconds`,
`capacityMicroseconds`.
Totales exactamente3: plannedMicroseconds, workedMicroseconds y capacityMicroseconds. Totales suman
los siete días; capacity total null si capacidad desconocida. Duraciones
como strings decimales canónicos no negativos, sin signo/ceros iniciales salvo
"0"; capacidad puede ser null. Contador como string decimal canónico también.
Fechas públicas YYYY-MM-DD; instantes UTC terminadosZ con precisión exactaµs,
convención14–18. No notas privadas, títulos, owner o recibos en este DTO.

Cliente valida forma cerrada, rangos, siete fechas consecutivas/lunes,
continuidad de fronteras, sumas con BigInt, duración no negativa, capacidad
null consistente y coherencia de la zona solicitada explícita. No recalcula
fronteras con TZDB/Intl ni con Date para validar aritméticaµs. Formato visual
puede usar Intl y fallbackUTC, sin alterar fecha/frontera/valores del servidor.
Dato incompatible es error de respuesta, nunca semana vacía o cero fabricado.

## UI y recuperación

Ruta privada `/revision-semanal`, enlace desde navegación existente. Selector
de fecha nativo, selector de zona con catálogo reutilizado y opción «Zona de
disponibilidad»; controles «Semana anterior», «Semana siguiente», «Esta
semana» y «Actualizar». No guarda preferencia20 ni fuerza elegir zona para
primer uso. Cambiar filtros inicia lectura nueva; selección permanece visible.
Resumen y siete filas/días accesibles, sin gráfico obligatorio ni paginación.
Textos permanentes explican plan vigente y capacidad actual. Días con presupuesto cero se rotulan descanso planificado actual, sin afirmar descanso real.

Mostrar «Datos consultados a…» con serverNow; no sumar tiempo localmente ni
polling/timer. Durante recarga misma selección puede conservar datos marcados
anteriores y loading; al cambiar semana/zona retirar el resumen anterior para
no atribuirlo a nuevos filtros. Error conserva selección y ofrece reintento
manual GET. Vacío válido muestra siete ceros, capacidad si conocida y enlace
a planificación; no POST/replay ni idempotencia nuevos.

Abortar cada solicitud sustituida antes de activar la siguiente generación;
tras cada await comprobar generación/acceso, incluida entregaHTTP401 antes
del observer. Logout o401 vigente elimina datos;401 obsoleto no revoca acceso
nuevo. JSON tardío/error clasificado tarde no restaura otra semana, identidad
ni datos privados. AbortController y generación reutilizados, sin localStorage
de datos. Un reintento pendiente no dispara duplicados. Foco en encabezado al
entrar; errores y carga anunciados sin mover foco en cada actualización;
controles44px, reflow y matriz30 de docs/ux-requirements.md se verifican para19.

## Corte mínimo y ejemplos para destilación

Reutilizar puertos/lector de transacción de Hoy e historial como patrón, con
un caso de uso y adaptador de consulta propios sólo cuando el test lo exija.
Dominio puro, Clock en aplicación, DTO HTTP separado. No devolver hechos para
que navegador calcule sumas ni cambiar comandos14–18. Para extremos0001 usar JDBC OffsetDateTime, como el lector de History, sin
getTimestamp/calendario híbrido. Índice sólo con necesidad
justificada; no nuevas capas de reporting o calendario.

Ejemplos medibles: tramo domingo23:30→lunes00:30 aporta30min a cada semana;
pausa intermedia resta sólo su intersección; CLOSE running incorpora último
tramo, paused no añade trabajo; EXTEND no suma plan ni neto; cancelación elimina
plan pero conserva trabajo. Madrid2026-03-29 01:30→03:30 son60min reales;
Madrid2026-10-25 01:30→03:30 son180min, ambos cruzan DST. Semana vacía y capacidad0
no afirman descanso; zona inválida explícita400, preferencia desaparecidaUTC;
clock atrasado409; datos ajenos excluidos; writer entre lecturas no mezcla
snapshot; reinicio/outbox retirado conservan resultado salvo serverNow live;
respuesta antigua tras cambio de semana o logout no repuebla la pantalla.

Propuesta cerrada para juez/root, sin promover project-spec ni feature status.

