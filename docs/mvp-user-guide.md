# Usar el MVP para trabajar con un horario

Las funcionalidades 1–23, incluidos Historial, Revisión semanal, Apariencia, campos personales, Exportación e Importación, están disponibles en [OrganizationWeb](https://organizacion.apptolast.com). El estado de despliegue está en [el plan](mvp-delivery-plan.md). Las funcionalidades 24–30 todavía no están publicadas.

## Preparar el trabajo

1. Entra con la cuenta configurada para tu instancia. En **Disponibilidad**, elige tu zona horaria y los minutos que quieres dedicar cada día; puedes reservar días de descanso con cero minutos.
2. Crea un proyecto y añade una tarea con un resultado pequeño y comprobable. Puedes dividirla en subtareas y estimarlas por separado. Activa los proyectos en los que vayas a trabajar; el límite inicial de proyectos activos es tres y la instancia puede configurarlo.
3. Desde una tarea pendiente, abre **Planificar bloque** e indica objetivo, fecha, inicio y fin. Revisa el presupuesto antes de guardar. Una reserva organiza el horario; no registra por sí sola tiempo trabajado.
4. Abre **Hoy** para consultar las reservas del día. Si necesitas cambiar un hueco, replanifica o cancela la reserva desde su tarea. Los cambios conservan su historial.

## Trabajar y parar

Inicia una sesión desde el detalle de la tarea y elige su duración prevista. **Pausar** detiene la acumulación del tiempo neto; **Reanudar** continúa la misma sesión. La pausa no desplaza la hora de fin prevista.

Cuando llega el fin, el aviso permite decidir si cierras o usas **Ampliar tiempo**. Ampliar requiere una decisión explícita y no añade tiempo trabajado por sí mismo. El aviso depende de que el navegador pueda consultar el servidor: no es una alarma externa con la pestaña cerrada.

Usa **Cerrar sesión de trabajo** para registrar el cierre, anotar el avance y dejar el siguiente paso. Completar una tarea es una acción independiente. Cerrar la pestaña o llegar a la hora prevista no cierra automáticamente la sesión de trabajo.

Si una respuesta se pierde, utiliza la acción de comprobación que ofrezca la pantalla. La aplicación consulta el resultado guardado para recuperar una decisión; evita crear otra intención para resolver una respuesta incierta. La URL de una sesión permite volver a consultarla, incluso después de cerrarla.

## Consultar lo ocurrido

**Historial** reúne cinco tipos de hechos: reservas originales, cambios de reserva, cambios de estado de tarea, inicios de sesión y cambios de sesión. Una tarea reabierta conserva también su finalización anterior. Los detalles del cierre muestran sus notas, tiempo neto y día atribuido; los de una ampliación muestran el cambio de fin previsto.

Puedes entrar desde la navegación principal o desde el detalle de un proyecto o tarea. Los filtros visibles son categoría y fechas **Desde/Hasta en UTC**; se aplican al pulsar **Aplicar filtros**. Filtran la fecha en que ocurrió el hecho, no la fecha futura de una reserva ni el día atribuido al cierre. **Limpiar filtros** vuelve al historial global; **Quitar filtro de contexto** conserva la categoría y fechas.

Cada página contiene hasta20hechos. **Más antiguos** sustituye la página actual; **Volver a recientes**, Atrás y recargar conservan una navegación basada en la URL. Los nombres de proyecto y tarea son los actuales; los detalles guardados del hecho permanecen históricos. Si varios hechos coinciden en el instante, su orden visual no demuestra una relación causal.

Un error de consulta no significa que el historial esté vacío. El reintento sólo repite la lectura. Si caduca la sesión de acceso, vuelve a identificarte; la aplicación retira los datos privados de la pantalla.

## Comparar la semana

Abre **Revisión semanal** para ver siete días de lunes a domingo. La fecha y la zona se aplican al pulsar **Mostrar semana**; cambiar un campo no inicia una consulta. Los enlaces de semana anterior, siguiente y actual permiten moverte sin perder la zona elegida. Atrás y recargar conservan la selección aplicada.

**Plan vigente** muestra las reservas actuales, **Trabajo registrado** suma los intervalos efectivos y **Presupuesto actual** refleja tu disponibilidad configurada. Una reserva de una hora y treinta minutos de trabajo aparecen como cifras distintas. El trabajo no equivale a tareas terminadas.

Las semanas pasadas también reflejan cambios actuales de reservas o disponibilidad. Un presupuesto de cero indica descanso planificado, no demuestra que hayas descansado ni genera una deuda. Si la disponibilidad no puede usarse para la zona seleccionada, el presupuesto aparece como desconocido.

La hora de consulta indica hasta cuándo se ha contado una sesión en curso. Pulsa **Actualizar** para leer de nuevo. Durante la espera o un error, los datos anteriores están identificados; **Reintentar** repite la consulta sin modificar tus tareas, reservas o sesiones.

## Personalizar la apariencia

Abre **Apariencia** y elige **Claro**, **Oscuro** o **Sistema**. Sistema sigue la preferencia de tu dispositivo. Puedes elegir un color de acento para el tema claro y otro para el oscuro, mediante el selector o escribiendo su código de color. Ambos deben conservar un contraste legible, aunque sólo estés usando uno de los temas.

Las dos muestras permiten revisar enlaces y botones antes de aplicar cambios. Si un color no es válido, el campo conserva lo escrito y explica cómo corregirlo; la muestra mantiene el último color seguro. Editar sólo cambia el borrador. **Guardar apariencia** aplica los valores confirmados a la aplicación y los conserva para tu cuenta, también al volver a entrar.

**Restaurar valores predeterminados** prepara Sistema y los dos colores seguros iniciales; pulsa Guardar si quieres conservarlos. **Cancelar cambios** vuelve a la última apariencia confirmada sin guardar. Al salir de esta pantalla se descarta el borrador sin guardar.

Si no se puede confirmar un guardado, no lo repitas. Usa **Recargar versión guardada** para consultar qué valores conserva tu cuenta: esa acción reemplazará el borrador cuando la consulta termine correctamente. Después podrás decidir si haces otro cambio. Si falla la carga inicial, la aplicación usa temporalmente valores seguros y permite seguir trabajando o reintentar la consulta.

## Personalizar vistas y campos

En las listas de proyectos y tareas, **Personalizar vista** permite elegir qué datos secundarios mostrar y en qué orden. **Guardar vista** conserva esa configuración para tu cuenta. **Restaurar vista** prepara la selección predeterminada; debes guardarla para aplicarla. Ocultar un dato en la lista no lo borra de su proyecto o tarea.

Abre **Gestionar campos personales** para añadir información propia de proyectos o tareas: **Texto**, **Número entero**, **Sí o no** y **Fecha**. Cada ámbito admite hasta 12 campos, incluidos los desactivados. Elige el tipo al crearlo; después puedes cambiar su etiqueta o su estado **Activo**, pero no convertirlo a otro tipo.

En el detalle del proyecto o tarea, rellena sus valores y pulsa **Guardar campos**. Desactivar una definición oculta el campo y conserva los valores existentes; volver a activarla permite recuperarlos. Vaciar un valor y guardarlo es una acción distinta de desactivar el campo.

Si un guardado no puede confirmarse o los campos cambiaron en otra pestaña, utiliza **Recargar guardado** antes de decidir otro envío. Esa consulta reemplaza el borrador al recuperar la versión confirmada. No interpretes una consulta fallida como ausencia de datos.

## Exportar tus datos

Abre **Exportación** y pulsa **Preparar exportación**. Abrir la pantalla no consulta tus datos para exportarlos. Cuando termine, pulsa **Descargar archivo JSON** para guardar el archivo; prepararlo no inicia una descarga automática. Puedes descargar otra vez el mismo archivo preparado sin repetir la consulta.

**Cancelar preparación** interrumpe la espera. Si falla, reintenta explícitamente; **Preparar de nuevo** obtiene una instantánea nueva. Salir de la vista o cerrar el acceso retira el archivo preparado de la aplicación, pero no borra las copias que ya guardaste en tu dispositivo.

El JSON privado reúne catorce colecciones de tu cuenta, incluidos historial y campos desactivados. Guárdalo donde puedas proteger su contenido. La exportación admite hasta 100000 registros y 32 MiB; si supera un límite, no entrega un archivo parcial. Esta función no importa archivos ni sustituye una copia de seguridad del servidor.

## Importar una copia propia

Importación está publicada y permite validar una copia antes de confirmarla.

Abre **Importación** y selecciona un archivo JSON v1 obtenido con **Exportación** de tu misma cuenta. El archivo puede contener información privada: conserva la copia original protegida y sin editar. Se admiten hasta **32 MiB y 100000 registros**. Una copia de otra cuenta o un archivo incompatible no se puede importar.

1. Pulsa **Validar archivo**. Seleccionar el archivo o validarlo no incorpora datos. **Cancelar preparación** permite abandonar esta preparación.
2. Revisa la **Vista previa**, el propietario, la fecha y los recuentos. **Total** cuenta los registros de la copia; **Nuevos** indica los que se añadirán; **Iguales** son los que ya coinciden y no se duplicarán.
3. Pulsa **Confirmar importación** sólo cuando quieras incorporar los datos. La importación conserva lo existente: un conflicto impide aplicar toda la copia, sin sobrescribir ni incorporar una parte. La vista previa no reserva el resultado; un cambio posterior en tu cuenta puede producir un conflicto al confirmar.

Si la copia incluye una sesión de trabajo en curso, revisa el aviso: seguirá en curso desde su inicio histórico y el cómputo puede incluir el tiempo transcurrido. Importar no la pausa automáticamente.

Ante un rechazo confirmado, lee el motivo y usa **Elegir otra copia** cuando se ofrezca; después vuelve a validar. Si todos los registros ya coinciden, confirmar no añade duplicados.

Si se pierde la respuesta, usa **Comprobar resultado** antes de intentar otra importación. Que todavía no aparezca un recibo no demuestra que el envío anterior haya terminado. Sólo cuando la pantalla lo permita, selecciona el mismo archivo original sin modificarlo y pulsa **Reenviar la misma importación**. Tras recargar la página puede ser necesario volver a seleccionar ese archivo; la aplicación no conserva su contenido por ti. La consulta y el reenvío requieren tu acción explícita.

## Datos y servidor

La base de datos conserva proyectos, planificación y recibos de trabajo. El historial puede leerse sin depender de que el publicador RabbitMQ esté disponible. Las copias de seguridad y la restauración del servidor deben configurarse y comprobarse como parte del despliegue; tener un volumen de Docker no sustituye un respaldo.

Para ejecutar la instancia local consulta [el README](../README.md). Para la API, el contrato de lectura es `GET /api/v1/history`, autenticado con la sesión de la aplicación; sus parámetros y formato están en la sección18 de [la especificación](../project-spec.md). No se admiten notas privadas en URLs ni almacenamiento web para recuperar el historial.
