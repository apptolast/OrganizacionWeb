# read_projects — matriz UI/UX del autor

Autor de frontend: frontend_craftsman. Observaciones sobre el código y tests propios; no constituyen revisión independiente. El responsable de integración revisa esta matriz y aporta ejecución de navegador. Referencia: docs/ux-requirements.md, sus30 filas completas.

Evidencia actual: frontend73 tests verdes y build/lint; lista y detalle auténticos obtenidos de API, sin almacenamiento persistente ni contenido de muestra. Integración comunica matriz Chromium de12 anchos en ambas vistas con textos120/4000, axe, teclado/foco y áreas44px. Firefox y WebKit recorrido básico+axe verdes. Zoom nativo Chrome2 mediante tabs.setZoom confirmado con DPR1.5→3 e innerWidth713/320; métricas sin overflow. Las primeras capturas de zoom eran parciales por coordenadas de captura y algunas mostraban espera: integración corrigió captura CDP ajustada a zoom y esperó dato real. El autor inspeccionó las capturas completas finales de lista y detalle320: sin recortes observados, HTML literal, jerarquía y lectura conservadas. Capturas y JSON en outputs/read-projects-real-zoom*. No se confunde emulación de viewport con zoom nativo.

| Principio | Aplicación concreta | Evidencia / resultado y límites |
| --- | --- | --- |
| Atención selectiva | Nombre de proyecto enlazado y acción Crear separada de datos secundarios. | Jerarquía en JSX/SCSS; matriz navegador comunicada. Pendiente valoración de uso humano. |
| Carga cognitiva | Lista solo con nombre/estado/creación; descripción completa al abrir. | Tests lista/detalle y contrato DTO. Comprensión humana no medida. |
| Estética-usabilidad | Paleta y Workspace compartidos con captura; error legible separado del vacío. | SCSS, tests de estados y capturas finales de lista/detalle inspeccionadas; legibilidad observada. Preferencia estética humana no medida. |
| Posición en serie | Crear arriba, paginación después de registros y regreso antes del detalle. | Orden DOM y teclado; responsive sin reordenar contenido significativo. |
| Tendencia a la meta | No se calculan objetivos ni avance. | No aplicable: lectura sin progreso de tareas. |
| Von Restorff | Crear usa acción sólida; error incorpora texto y borde, no solo color. | SCSS y roles alert/status. No se presupone comprensión por color. |
| Zeigarnik | Volver recupera proyectos de PostgreSQL sin depender de resultado local. | E2E creación/lista/recarga comunicado verde. Sin recordatorios culpabilizadores. |
| Fluir | No se inicia una sesión de trabajo. | No aplicable: comenzar/pausar/cerrar corresponde a otro contrato. |
| Fragmentación | Un li por proyecto, detalle en article y metadatos en dl. | Semántica inspeccionada y pruebas de lista/encabezados. |
| Memoria de trabajo | URL recuperable, nombre visible, retry misma ruta y retorno explícito. | Tests navegación/paginación/errores; no exige recordar ids. |
| Navaja de Occam | Abrir, crear, más antiguos, inicio y retry son los únicos controles. | Cada control lleva a una acción existente; sin filtros ni menús adelantados. |
| Conectividad uniforme | No se dibujan relaciones entre proyectos. | No aplicable: sin dependencias visualizadas. |
| Fitts | Enlaces de tarjeta y acciones tienen mínimo44px. | Medición navegador comunicada por integración; no ensayo en dispositivo físico. |
| Hick | Abrir un proyecto es la acción por registro; paginación directa. | Estructura inspeccionada. Velocidad de decisión humana no medida. |
| Jakob | Enlaces reales conservan href/modifiers, History API/popstate y botón retry. | Tests modifiers y navegación; E2E persistencia/recarga. |
| Semejanza | Misma etiqueta Idea, fechas UTC y controles equivalentes en ambas vistas. | Componentes compartidos, SCSS y aserciones de fecha/estado. |
| Miller | Agrupación por proyecto y metadatos; página contractual20. | No se aplica una regla arbitraria de siete. Comprensión de grupos pendiente de uso. |
| Parkinson | No hay planificación temporal. | No aplicable: sin bloques ni duración de sesiones. |
| Postel | Texto Unicode intacto; respuesta de servidor validada, cursor opaco. | Tests literalHTML, tipos/fechas incompatibles e identidad de detalle; servidor valida parámetros. |
| Proximidad | Estado/fecha bajo nombre; error y retry en misma sección. | Estructura DOM/SCSS y tests error→retry. |
| Prägnanz | Estado textual Idea y encabezados claros; iconos decorativos aria-hidden. | Semántica inspeccionada, axe comunicado verde. |
| Región común | Tarjeta por resumen, región distinta para descripción y metadatos. | Agrupaciones de producto reales, sin paneles ficticios. |
| Tesler | URL gestiona cursor; fechas dicen UTC; errores propios sin SQL/stack. | Tests GET/paginación y errores malformados. Preferencia horaria personal pendiente de otra feature. |
| Modelo mental | Lista de proyectos y detalle de idea; no se equipara proyecto con sesión. | Etiquetas revisadas; no estados de productividad inventados. |
| Usuario activo | Vacío confirmado orienta Crear proyecto existente. | Test@s15 y E2E vacío; facilidad de primer uso pendiente de evaluación humana. |
| Pareto | Recientes primero como decisión del contrato. | E2E paginación/orden real. No se atribuyen porcentajes observados de uso. |
| Fin de pico | Resultado persistente, regreso claro y fallos recuperables sin falso vacío. | Tests401/404/red/503/500, reload real. |
| Sesgo cognitivo | Orden cronológico explícito sin clasificar productividad ni presionar. | Texto del recorrido inspeccionado; sin rankings/rachas. |
| Sobrecarga de opciones | Página20 y navegación mínima; personalización no adelantada. | Contrato/JSX inspeccionados; no afirma validación humana de preferencias. |
| Doherty | Estado inicial de espera renderizado sin temporizador artificial. | Tests síncronos status antes de fetch; medición real<400ms corresponde a integración. |

## Límites que permanecen explícitos

No se ha probado móvil/tablet físico, teclado virtual, lector de pantalla real ni todas las combinaciones posibles de sistema operativo. Axe no certifica WCAG completa. Las pruebas automáticas de12 anchos, zoom nativo y motores de navegador se registran por entorno; la evaluación humana de comprensión/aprendizaje no se sustituye por ellas. La personalización todavía no implementada no hereda supuesta conformidad de estas vistas.
