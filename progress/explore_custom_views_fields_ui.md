# Exploración UI21: vistas y campos restaurables

Propuesta para A/root, no contrato ni implementación. Lectura de AGENTS,
Ponytail full, Caveman lite y docs/ux-requirements.md. Sin nuevos gates ni
pruebas: la tarea autorizada es documental. Feature20 aceptada según root;
progress/current.md conserva contexto histórico16/17 y no se usa como estado21.

## Integración útil y reutilización

La lista /proyectos vive en ProjectReader; ProjectTasks muestra tareas de un
proyecto y también subtareas. Ambas usan listas semánticas y paginación por
cursor, no una tabla configurable. ProjectReader y TaskReader ya son destinos
estables para leer/editar contexto. Recomiendo «Personalizar vista» junto al
encabezado de la colección, con un formulario desplegable local; sin otra
entrada principal ni un panel de ajustes que nunca afecte las listas reales.

La primera vista configurable puede seleccionar y ordenar metadatos visibles.
Nombre/título, estado y enlace al detalle permanecen accesibles. Estado no se
oculta en nombre de una lista compacta. Usar checkboxes y botones «Subir» /
«Bajar» para ordenar, sin exigir arrastrar. Mantener la paginación y orden de
servidor actuales si el contrato no añade explícitamente otro orden/filtro.
Nunca presentar un filtro aplicado sólo a la página recibida como búsqueda
de todos los proyectos o tareas. Configuración propia persistente por ámbito
proyectos/tareas; la definición del ámbito debe aclarar si incluye subtareas.

Para campos personalizados, el detalle ofrece una sección «Campos adicionales»
con lectura en dl y edición explícita. Una definición tiene identidad estable,
no el nombre como clave. El proyecto ofrece name/description/status y fechas;
Task ofrece title/completionCriterion/estimatedMinutes/status y fechas. Ninguno
contiene valores personalizados hoy. Añadir datos propios separados evita
reinterpretar los DTO cerrados actuales o sobrescribir los campos de negocio.
No alterar el formulario de creación por defecto: primero guardar el recurso,
después añadir valores en su detalle es un primer recorrido completo y sencillo.

## Tipos y restauración que el contrato debe distinguir

Propuesta de controles para tipos básicos que A fije: texto plano con límite
visible; número con representación/rango exactos acordados; fecha civil nativa
sin conversión de zona; booleano con «Sin valor / Sí / No» si es opcional.
No usar checkbox opcional cuando convierta ausencia en false. Vacío no equivale
a cero, false ni fecha actual. Mostrar «Sin valor» y conservar Unicode/multilínea;
no interpretar HTML, fórmulas, enlaces ejecutables o código. Si se admiten
opciones, sus identidades deben sobrevivir a cambios de etiqueta.

«Restaurar vista predeterminada» debe cambiar presentación, conservando campos
y valores. «Cancelar» sólo descarta borrador; «Guardar vista» confirma la
preferencia propia. Restaurar campos exige otra semántica: ocultar/archivar una
definición debe conservar valores para poder recuperarla; no prometer recuperación
si la operación los elimina. Recomiendo tipo inmutable tras crear la definición
para evitar conversiones silenciosas. Si A necesita cambio de tipo, explicitar
qué ocurre con valores incompatibles antes de ofrecer ese control.

Un campo personal «Avance» o «Horas» es información declarada, no cambia estado
de tarea, estimación, reservas, trabajo real ni métricas semanales. Separar
visualmente campos adicionales de esos hechos y no inferir porcentajes/logros.

## Estados, acceso y evidencia futura

GET fallido no significa configuración ausente: vista base puede ser provisional
pero no se presenta como guardada. Guardado explícito, espera anunciada, dedupe
de escritura y formulario protegido ante ediciones que la respuesta borraría.
Conflicto conserva borrador para una decisión manual; resultado incierto bloquea
otra escritura hasta consulta válida, incluso tras salir y volver al formulario.
No confundir esa consulta con la confirmación de la intención previa.

Montar datos sólo bajo el propietario y recurso actuales. Abortar y descartar
GET/JSON/401 tardíos antes del observador de acceso; limpiar valores al logout,
cambio de cuenta o retirada del recurso. El detalle no debe recuperar información
privada mediante una respuesta hermana anterior. Evitar GET por cada fila si los
valores se muestran en lista: el contrato debe ofrecer lectura acotada conjunta.

Etiquetas, ayudas y errores asociados; primario Guardar, secundarios Cancelar /
Restaurar. Conservar foco durante espera; devolverlo sólo cuando desaparece su
iniciador y el usuario no eligió otro destino. RouteLink conserva modificadores,
Back y URL existentes. Cambiar vista no remonta formularios de tareas ni pierde
borradores ajenos. Control44px, reflow320, texto/zoom200, temas20, contraste,
forced-colors y movimiento reducido siguen aplicando a todas las variantes.

La matriz de30 principios debe documentar aplicación/evidencia/límites al existir
el recorrido. Prioridades específicas: revelación progresiva, nombres estables,
restauración comprensible y ausencia de pérdida de datos. No se acredita UX21
por los resultados20; dispositivos físicos y evaluación humana no se inventan.

## Archivos candidatos, sujetos al contrato

- frontend/src/project-reader.tsx y project-tasks.tsx: entrada a personalizar y
  render de metadatos; task-reader.tsx y detalle de proyecto: valores adicionales.
- Un cliente nuevo para DTO/validación y componentes pequeños de vista/campos;
  nombres exactos después de acordar endpoints. Reutilizar apiRequest, no copiar
  un Provider global si sólo lo consumen estas superficies.
- read-projects-api.ts / tasks-api.ts y sus hooks sólo si la lectura conjunta
  requiere un contrato nuevo; conservar los validadores y oráculos existentes.
- styles.scss: clases locales con tokens20 y controles nativos. App/workspace
  no necesitan cambiar si la gestión queda contextual; navigation.tsx se reutiliza.

No dependencias, motor de formularios, router nuevo, grid editable ni lógica de
exportación/automatizaciones. Éstas no son necesarias para hacer útil este corte.
