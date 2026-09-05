# project_states — evidencia UI/UX

Responsable de integración: integration_craftsman. Esta matriz aplica los 30 principios de docs/ux-requirements.md al control nuevo de estados. Las observaciones de autor frontend y la revisión independiente del coordinador se identifican por separado. No se atribuye validez psicológica universal a estas decisiones.

## Evidencia disponible

Cuatro recorridos de navegador nuevos: transiciones con persistencia/lectura/edición, límite con dos peticiones concurrentes, conflicto compartido con edición y accesibilidad. Se midieron 22 anchos CSS: 320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099, 1101, 1280, 1440, 1599, 1601, 1920 y 2560. La altura 400 a ancho 768 comprueba una pantalla baja. Texto de 120/4000 caracteres sin espacios; sin desbordamiento horizontal. Botones de estado de al menos 44 por 44 CSS dentro del ancho disponible. Axe sin infracciones de las reglas A/AA seleccionadas. Se verificaron también Terminado/Pausado a 320 y Retomar mediante tap emulado.

Teclado: Tab alcanza Pausar, foco visible y Enter confirma. Al desaparecer ese botón, el foco pasa al encabezado Estado del proyecto. Feedback medido con performance.now y MutationObserver: 2 ms desde click. Retener la entrega de la respuesta real mantuvo Idea visible, mostró Cambiando estado y bloqueó doble envío; sólo tras confirmar cambió la representación. No se simuló éxito.

Zoom nativo de Chromium mediante extensión temporal y tabs.setZoom(2), perfil aislado: DPR 1,5 a 3; interior 1426 a 713 manteniendo ventana exterior 1440. Ventana estrecha propia de 654 produjo 320 CSS y documento 312/312. Botones midieron 244,33 por 45 y 45,60 CSS. Activación real confirmada a ese tamaño y zoom. No se confunde viewport emulado con zoom nativo.

El coordinador inspeccionó outputs/project-states-desktop.png, project-states-mobile.png, project-states-real-zoom-320.png y JSON: legibles, sin hallazgo visual bloqueante. Los datos son sintéticos y están identificados como prueba. El autor frontend inspeccionó escritorio/móvil como observación de autor. Firefox y WebKit ejecutaron el recorrido de transiciones real con persistencia/lista/edición/no-op: dos pruebas verdes en 24,4 segundos; feedback observado 2 ms en ambos. La matriz completa de anchos pertenece a Chromium.

| Principio | Aplicación concreta | Evidencia y límites |
| --- | --- | --- |
| Atención selectiva | Estado actual precede a sus acciones y a la descripción. | Capturas y árbol semántico; atención humana no medida. |
| Carga cognitiva | Se muestran únicamente las acciones permitidas desde el estado actual. | Recorrido real de cinco cambios; la tabla completa se verifica en aplicación. |
| Estética-usabilidad | Tarjeta, espaciado y controles reutilizan el lenguaje visual existente. | Capturas revisadas; preferencia estética humana no medida. |
| Posición en serie | Estado, explicación, acciones y confirmación tienen orden estable. | DOM y teclado, sin reordenación visual que cambie el sentido. |
| Tendencia a la meta | Terminado expresa una decisión, sin porcentaje ficticio. | No se infiere avance de tareas a partir del estado. |
| Von Restorff | Marcar terminado tiene menor énfasis visual que activar o pausar. | Capturas y etiquetas explícitas; significado no depende sólo del color. |
| Zeigarnik | Pausado permite retomar deliberadamente sin perder el proyecto. | Pausar/Retomar persisten y se leen después. No hay presión por rachas. |
| Fluir | Activar un proyecto no inicia un temporizador ni una sesión. | No aplicable al flujo temporal de trabajo, reservado a otro contrato. |
| Fragmentación | Estado y controles forman una región separada de la descripción. | Región accesible Estado del proyecto y estructura visual. |
| Memoria de trabajo | La versión se gestiona internamente; el usuario ve estado y opciones. | Conflictos reales en ambos sentidos con edición. |
| Navaja de Occam | Cuatro estados cerrados y acciones directas, sin menú personalizable. | Alcance inspeccionado, sin dependencias de interfaz nuevas. |
| Conectividad uniforme | No representa relaciones entre proyectos. | No aplicable. |
| Fitts | Botones de estado de al menos 44 por 44 CSS. | Medición en 22 anchos y zoom estrecho; hardware táctil pendiente. |
| Hick | Una o dos acciones válidas; recuperación sólo cuando corresponde. | Estados y errores probados, velocidad de decisión humana no medida. |
| Jakob | Botones nativos, enlaces reales y mensajes accesibles. | Tab/Enter, tap emulado y navegación a Elegir qué pausar. |
| Semejanza | Las etiquetas Idea/Activo/Pausado/Terminado coinciden entre vistas. | Transiciones, lista, detalle y edición con datos reales. |
| Miller | Agrupación por estado y proyecto, sin aplicar una cifra arbitraria de opciones. | Estructura revisada; comprensión humana pendiente. |
| Parkinson | La activación no asigna horarios ni duración. | No aplicable a planificación temporal en este corte. |
| Postel | API y cliente aceptan los cuatro valores definidos y rechazan representaciones incompatibles. | Tests backend/frontend y recorridos de los cuatro estados. |
| Proximidad | Error, conteos y recuperación aparecen junto al control de estado. | Límite real HTTP 409 y conflicto HTTP 412 en navegador. |
| Prägnanz | Estado textual y acciones con verbos precisos. | No se usa únicamente color o icono; axe y capturas. |
| Región común | La región de estado agrupa encabezado, ayuda y acciones. | Semántica y separación visible. |
| Tesler | El sistema cuenta capacidad y evita carreras; el usuario elige qué pausar. | Dos activaciones concurrentes dejan tres activos y un solo evento nuevo. |
| Modelo mental | Reabrir deja el proyecto en pausa; retomar es otra decisión. | Recorrido terminado a pausado; no activación automática. |
| Usuario activo | Acciones disponibles en el detalle, sin configurar reglas antes de usarlas. | Navegación real; facilidad de aprendizaje humano no medida. |
| Pareto | Estados y capacidad mínima cubren el corte aprobado. | No se atribuyen porcentajes de uso ni productividad. |
| Fin de pico | Confirmación tras respuesta real; errores ofrecen recuperación explícita. | Espera retenida, conflicto y límite sin éxito ficticio. |
| Sesgo cognitivo | Tres activos es un valor inicial configurable, no una recomendación psicológica probada. | Configuración de despliegue documentada y validada; sin ranking de usuarios. |
| Sobrecarga de opciones | No hay estados arbitrarios ni pausa automática de otro proyecto. | Límite dirige a elegir manualmente; snapshot SQL conserva los demás. |
| Doherty | Cambiando estado aparece antes de esperar a la API. | 2 ms medidos; cifra de este entorno, no garantía universal. |

## Límites

No se probaron móviles/tablets físicos, teclado virtual ni lector de pantalla real. Tap y viewport son emulación; axe no certifica WCAG completa. Las capturas y zoom prueban el entorno local indicado. La evaluación humana de comprensión o hábitos no se sustituye por automatización. No se repite la matriz de crash del publicador: el nuevo evento sí se verificó con broker detenido y recuperación real en el smoke.

Cierre congelado: suite de 22 E2E completa verde en 2,0 minutos. Recorrido de controles a 22 anchos: 17,8 segundos. Feedback de estado medido en la corrida final: 1 ms. No hubo cambios de comportamiento posteriores a las capturas revisadas por el coordinador.
