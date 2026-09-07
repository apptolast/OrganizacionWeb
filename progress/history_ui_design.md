# Diseño de Historial18

Propuesta de interfaz previa al contrato; no aprueba DTO, implementación ni evidencia UX. Aplica Ponytail full/Caveman lite y docs/ux-requirements.md. No se ejecutaron suites ni navegador. Coordinada con C: fecha de filtro UTC del hecho, límites inclusivos, cinco familias durables y sin agregados.

## Una vista y navegación mínima

- Una ruta `/historial`, con parámetros de filtro/cursor en URL. App reconoce esta ruta antes del fallback; Workspace añade sección y RouteLink Historial con aria-current. Hoy/Proyectos/Disponibilidad y rutas16 permanecen iguales. Workspace actualmente no tiene placeholder Historial.
- Reutilizar useRoute/RouteLink: ya conservan pathname+search, Back/Forward y clic modificado nativo. No añadir router, store global ni shell alternativa. El main conserva id `proyectos` para que funcione el skiplink existente, con h1 Historial.
- Filtros mínimos: tipo de hecho (Todos + familias contractuales), Desde y Hasta (`input type=date`), Aplicar filtros y Limpiar filtros. No pedir IDs ni añadir catálogo global de proyectos/tareas. C admite projectId/taskId mediante enlaces contextuales «Ver historial de este proyecto/tarea» desde filas; la vista muestra el filtro aplicado y permite retirarlo sin escribir un UUID.
- El formulario edita un borrador; sólo Aplicar navega/consulta y reinicia cursor. La URL contiene filtros aplicados, nunca notas, títulos o keys. Back/recarga recuperan consulta; no guardar datos privados en almacenamiento web.
- Paginación acordada con C, según patrón de Proyectos: una página por URL, «Más antiguos» con cursor opaco y «Volver a recientes» conservando filtros. Reemplaza la página, sin scroll infinito ni reconstrucción de todas las páginas al recargar. El cursor queda vinculado a filtros y propietario conforme al contrato.
- No números de página, total de resultados ni porcentaje de avance sin soporte contractual. nextCursor nulo retira continuación; cursor inválido ofrece volver a recientes, sin convertir error en vacío.

## Contenido, fechas y tiempo

- Lista ordenada semántica de hechos, con una unidad visual por hecho. Encabezado breve: «Sesión cerrada», «Tarea completada», «Reserva planificada» u otra etiqueta del tipo contractual. Proyecto/tarea sirven de contexto, enlazados sólo cuando el DTO entregue sus identidades autorizadas.
- «Resumen» significa frase breve del propio hecho; no suma ni panel de métricas, incluso dentro de la página. La planificación inicial es una reserva, nunca trabajo realizado. Una reapertura conserva la finalización histórica sin afirmar que la tarea sigue completada.
- Fecha principal: instante ocurrido, en español y UTC visible, con time/datetime original. Etiquetas Desde/Hasta incluyen ayuda «Fecha del hecho en UTC»; el cliente no transforma el día elegido según zona del navegador. Límites exactos los fija el servidor.
- Cierre: mostrar tiempo neto real final mediante enteros exactos y unidad legible, nunca estimación, duración ampliada o descanso como trabajo. Mostrar aparte «Fecha atribuida al trabajo» con workDate y closeZoneId persistidos; no recalcular workDate con Intl ni usarla como filtro de hechos.
- Las notas son texto plano, mantienen saltos/Unicode y permiten texto largo. «Sin avance anotado»/«Sin siguiente paso anotado» describen vacíos sin juicio. Detalle inline con details/summary nativos evita una segunda pantalla por tipo; no interpretar notas como markdown/HTML.
- Resumen visible suficiente sin desplegar: tipo, instante, contexto y, si corresponde, neto final. Detalle muestra notas, intervalo o antes/después del hecho cuando existan en el contrato; no inventar valores ausentes.
- Enlace «Ver sesión» reutiliza `/proyectos/{p}/tareas/{t}/sesiones/{s}` para consultar situación actual deliberadamente. Ningún GETstate/active/end por fila; el historial no ejecuta pausa/cierre/ampliación ni confunde recibo con estado actual.

## Estados, teclado y privacidad

- Carga inicial y cada consulta: role=status temprano, sin porcentaje ficticio. Mantener controles de filtros y su borrador. Resultados anteriores no deben aparecer como respuesta a nuevos filtros: retirar lista al cambiar consulta o etiquetar inequívocamente su consulta anterior; preferencia mínima, retirarla.
- Vacío inicial: «Todavía no hay hechos registrados». Vacío filtrado: «No hay hechos con estos filtros», con Limpiar filtros. Con contexto pero sin filas, etiqueta genérica de proyecto/tarea y enlace al detalle; no inventar nombre histórico. Error503/red/JSON no es vacío ni ausencia de trabajo; Reintentar repite exactamente filtros/cursor y no descarta borrador.
- Aplicar/Reintentar conserva foco si sigue el mismo control. Si paginar elimina el iniciador que todavía tenía foco, dirigirlo al encabezado de resultados al comenzar espera; nunca robarlo si la persona eligió otro control. No copiar flags interacted permanentes de componentes viejos.
- Mientras petición pendiente, aria-disabled con guardia real coalesce doble activación; cambiar filtros/navegar aborta consulta anterior. Después de cada await/JSON/clasificación comprobar contexto vigente, antes del observador401. Una respuesta vieja no restaura filas ni revoca acceso nuevo.
- 401 vigente retira lista/notas/borradores y deja actuar SessionGate. Un contexto filtrado ajeno/ausente, si el contrato lo permite filtrar, no filtra nombres ni distingue propiedad por mensajes. Errores de detalle en ruta destino no reescriben el hecho histórico.
- Anchos continuos: una columna de lectura en móvil, controles de filtros con wrap y ancho mínimo0, notas overflow-wrap/pre-wrap; escritorio limita ancho. No tabla horizontal obligatoria ni timeline decorativa. Controles y summary/enlaces de acción44×44; h1→h2 resultados→encabezados de hechos coherentes.

## Reutilización real y límites

- App.tsx/workspace.tsx/navigation.tsx: ruta/enlace/selección de sección. Nueva vista y cliente18 mínimos; no modificar controles de sesiones para renderizar el historial.
- TaskHistory y BlockChangeHistory ofrecen patrones de lista/estados/paginación, pero no montarlos dentro de18: consultan rutas locales y el segundo permite consultas de estado por fila. Reutilizar semántica y estilos, no duplicar sus fetches ni adoptar sus limitaciones de foco.
- ProjectReader/useReadProjects muestran URLcursor y reintento; reutilizar patrón de URL, no su hook de DTO distinto. API18 usa apiRequest, guardas y validadores compartidos que realmente correspondan; un cursor opaco no se ordena/decodifica en navegador.
- SnapshotTime y seconds de work-session-state, BlockTime de block-details: reutilizables cuando campos y semántica coincidan. SnapshotTime sirve instantes con fallbackUTC explícito; no sirve para reinterpretar workDate. seconds conserva cálculo exacto, pero presentación neta18 debe ser legible sin perder significado por redondeo oculto.
- SCSS existente reader/task-form/field/task-list/closure-note como base; sólo estilos propios mínimos si la medición los exige. No nueva dependencia, cards interactivas completas ni widgets gráficos.

## Matriz30 de diseño (toda evidencia18 pendiente)

| Principio | Aplicación prevista |
| --- | --- |
| Atención selectiva | Tipo, fecha y contexto primero; detalles secundarios desplegables. |
| Carga cognitiva | Tres filtros visibles y sin IDs que memorizar. |
| Estética-usabilidad | Tipografía/espaciado/errores coherentes con reader. |
| Posición en serie | Filtros arriba, resultados y continuación después. |
| Tendencia a la meta | Hechos reales; sin indicador de progreso ni metas inferidas. |
| Von Restorff | Diferenciar tipo con texto, sin depender de color. |
| Zeigarnik | Siguiente paso histórico legible, sin exigir retomarlo. |
| Fluir | Lectura no interrumpe ni modifica sesión activa. |
| Fragmentación | Una unidad por hecho y grupos de filtros comprensibles. |
| Memoria de trabajo | URL de consulta, contexto visible y borrador conservado. |
| Navaja de Occam | Una vista/lista; sin dashboard, segunda ruta o agregados. |
| Conectividad uniforme | Enlaces sólo a relaciones reales autorizadas. |
| Fitts | Medir44×44 en filtros, detalle y paginación. |
| Hick | Aplicar como decisión principal; detalles a demanda. |
| Jakob | Inputs nativos, enlaces, historial del navegador y paginación. |
| Semejanza | Mismos tipos mantienen etiqueta/estructura. |
| Miller | Agrupar por significado, sin regla arbitraria de siete. |
| Parkinson | Mostrar fin del hecho sin extender tiempo ni invitar a seguir. |
| Postel | Fechas/Unicode según contrato, sin relajar decoder. |
| Proximidad | Ayuda/error unidos visualmente y por aria-describedby. |
| Prägnanz | Etiquetas directas; ninguna acción sólo con icono. |
| Región común | Filtros, resultados y cada hecho delimitados por función. |
| Tesler | Servidor resuelve cursor/fechas; UI no expone tokens ni revisiones. |
| Modelo mental | Reserva, trabajo real y completado son hechos diferentes. |
| Usuario activo | Vacíos orientan sin manual ni culpas. |
| Pareto | Revisar lo reciente y acotar fechas primero; sin porcentajes supuestos. |
| Fin de pico | Consulta termina con resultado o error recuperable claro. |
| Sesgo cognitivo | No sumar completados como logros vigentes ni neto como rendimiento. |
| Sobrecarga de opciones | Familias fijas contractuales; sin personalización avanzada. |
| Doherty | Medir feedback<400ms durante consulta diferida, sin falsa finalización. |

Validación posterior: matriz de anchos/breakpoints y alturas de docs/ux-requirements, Chromium/Firefox/WebKit, texto200%, zoom nativo200%, teclado/foco y axe en vacío/carga/error/lista/notas largas. Diseño no acredita esas pruebas, ni dispositivos físicos, teclado virtual o comprensión humana. Quedan fuera estadísticas, objetivos, personalización y features19–30.
