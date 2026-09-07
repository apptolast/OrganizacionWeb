# Propuesta UX15 — pausar y reanudar

Mapa de implementación pendiente de la sección normativa15 y Gherkin; no añade endpoints, reglas de negocio ni código. Coordinado con el autor de especificación. Reutiliza `WorkSession`, `SessionFacts`, SessionGate, formulario/botones nativos, navegación y SCSS14. Ponytail full/Caveman lite: sin modal, biblioteca temporal, contador por segundo ni preferencias nuevas.

## Flujo mínimo

1. Al entrar en una tarea, consultar la actividad global del propietario. Una sesión de otra tarea propia es válida: mostrar contexto y enlace real, sin permitir otro inicio. Una sesión pausada sigue ocupando la plaza única. Pausa y reanudación gestionan la sesión ya abierta incluso si después se completó la tarea o el proyecto; no se reutiliza la guarda de inicio14.
2. Separar el recibo inmutable de inicio14 del estado operativo15. Mostrar «Inicio confirmado (hecho histórico)» cuando exista recibo y, en un bloque distinto, estado y hora de actualización consultados; recuperar un recibo nunca sustituye por sí solo la lectura actual.
3. La sesión actual running ofrece «Pausar»; paused ofrece «Reanudar». Estado desconocido, consulta fallida o una operación pendiente no autorizan una transición. Ningún montaje, refresh, foco, llegada del fin o recuperación transmite automáticamente un comando.
4. Mostrar el fin previsto original sin desplazarlo al pausar o reanudar. Explicar que no cierra automáticamente y que15 todavía no permite cerrar/completar sesión16.
5. Presentar el neto con etiqueta que describa exactamente el valor del DTO consultado, sin simular segundos ni convertir una instantánea en duración final. El autor de spec concreta un snapshot calculado hasta serverNow: en running incluye el tramo abierto y en paused permanece constante hasta otra transición. Etiqueta propuesta: «Tiempo neto registrado hasta la consulta»; no total final cerrado.
6. Al activar una transición, retener identidad, tipo, key y revisión de esa intención. Anunciar «Pausando sesión»/«Reanudando sesión» y bloquear efectos duplicados, conservando el foco en el botón mediante aria-disabled y una guarda real.
7. Confirmación compatible: conservar recibo del cambio como hecho histórico y consultar estado actual por la ruta de estado. Si la lectura falla, el recibo sigue confirmado; mostrar error propio y reintento de consulta, sin repetir el comando ni afirmar estado vigente desde un recibo antiguo.
8. Resultado incierto: «No podemos confirmar la pausa/reanudación» y «Comprobar cambio». Conservar intención, sin acción contraria o nueva key. Refresh puede mostrar datos más recientes, pero no acredita que aquella key se aplicó; la comprobación específica sigue disponible. Sólo las condiciones de reenvío del contrato autorizan repetir manualmente la misma intención.
9. Conflicto de revisión: ofrecer consulta del estado actual; no adaptar silenciosamente la revisión de una intención ya transmitida ni repetirla. Una nueva acción se decide después de conocer el estado y resolver la intención anterior conforme al contrato.

## Datos y presentación

| Elemento              | Presentación propuesta                                                      | Límite                                                                                                 |
| --------------------- | --------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| Estado actual         | «En marcha»/«En pausa», contexto y estado consultado                        | No deducirlo del último recibo ni de ausencia de respuesta.                                            |
| Inicio y fin previsto | Fechas, horas, zona histórica y duración14                                  | Fallback UTC explícito si Intl no admite la zona, sin cambiar los instantes.                           |
| Neto                  | Duración legible y explicación de su alcance temporal                       | Sin contador local, redondeo que acredite tiempo adicional ni total final ficticio.                    |
| Intervalos            | Lista simple con inicio/fin o tramo abierto, si el DTO normativo los expone | No inventar reconstrucción cliente ni nuevo historial general18.                                       |
| Última operación      | Tipo/instante y mensaje de hecho confirmado                                 | Puede diferir del estado actual por cambios posteriores; evitar dos bloques titulados «Estado actual». |
| Error de consulta     | Datos anteriores identificados como última consulta y reintento             | No presentar estado anterior como autorización nueva mientras consulta pendiente/fallida.              |

## Recuperación y privacidad

Reutilizar el tratamiento de JSON cerrado, códigos estables, CSRF manual de SessionGate y pérdida de ACK. La revisión BIGINT y el token `Work-Session-Revision: work-session-{uuid}-{revision}` sin comillas se conservan internamente como texto/BigInt, sin convertirlos a Number ni mostrarlos al usuario. Se valida su correspondencia exacta con state y se reenvía el token en POST; no se usa ETag/If-Match para el snapshot variable. Se mantienen prioridades y códigos428/400/412. Los recibos de intención conocida validan identidad/tipo/contexto; GETactive global acepta otra tarea propia. Recuperar un hecho no requiere que la tarea siga siendo elegible para iniciar.

Cada await necesita comprobar aborto e identidad/generación, también después de clasificar un error o recuperar CSRF. Cambiar tarea, cerrar sesión o retirar contexto aborta antes de que un401 obsoleto llegue al observer. Un GET iniciado antes de un comando confirmado no debe reponer running/paused antiguos ni reabrir el formulario de inicio. Consultas y comandos conservan guardas coordinadas; el último resultado que llegue no gana por llegar último.

Cerrar u ocultar el panel no revoca una transición transmitida: aviso visible cuando corresponda y descubrimiento posterior mediante actividad/estado. La intención en memoria no se persiste en almacenamiento del navegador sin contrato. No añadir reset, cierre administrativo ni eliminación para suplir16.

## Teclado, responsive y validación posterior

Botones nativos con Enter/Espacio y orden Tab/ShiftTab; anuncios role=status al consultar/enviar/comprobar, errores legibles con role=alert. Si desaparece el control que conservaba foco, enfocar el encabezado de la sección; no robar foco movido por la persona ni enfocar nodos retirados. Estado legible por texto, no sólo color.

Reutilizar las treinta filas de `docs/ux-requirements.md` aplicándolas a pausa, reanudación, consulta, incertidumbre y confirmación; no copiar una certificación14. Matriz posterior:320–2560 y fronteras CSS, altura reducida, contexto largo/Unicode, controles44px sin solapes, texto200%, zoom nativo, axe y tres motores. Sin dependencias de hover o drag. No se acreditan aquí dispositivos físicos, teclado virtual, lector de pantalla ni comprensión humana.

## Cierres pendientes del autor de spec

- Alinear forma/nombres exactos del DTO de estado y recibo15, disponibilidad de intervalos y clasificación de conflictos/reenvíos. Este mapa no los decide ni amplía el contrato.

Coordinación posterior al borrador: el autor confirmó neto hasta serverNow (tramo abierto incluido), conservación de microsegundos y pausa/reanudación permitidas tras completed. No había una propuesta previa en disco; se trabajó sobre UI14 y esas decisiones explícitas, a la espera del esquema normativo final.
