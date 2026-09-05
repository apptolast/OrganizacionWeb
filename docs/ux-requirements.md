# Criterios UI/UX y responsive de OrganizationWeb

Requisito incorporado el 5 de septiembre de 2026 por indicación del usuario. Fuente: [Laws of UX en español](https://lawsofux.com/es/), catálogo consultado en esa fecha (30 principios). Las aplicaciones y comprobaciones siguientes son decisiones de este producto, no citas ni una certificación de la fuente.

## Regla de revisión

Toda feature con interfaz revisará las 30 filas: aplicación concreta, evidencia y resultado (verificado, pendiente o no aplicable con motivo). Ninguna fila se omite. Un principio cuyo recorrido aún no existe queda pendiente para esa feature; no se declara cumplido por existir esta tabla. Cualquier tensión entre principios se resuelve de forma explícita preservando accesibilidad, integridad de datos y el objetivo de constancia sin agotamiento. El juez revisa esa decisión. Las heurísticas cognitivas requieren también evaluación humana; una suite automática no demuestra por sí sola facilidad de uso.

## Matriz completa

| Principio de la referencia | Aplicación exigida en este producto | Evidencia que deberá aportar cada recorrido |
| --- | --- | --- |
| Atención selectiva | Objetivo y acción principal reconocibles; avisos secundarios discretos. | Revisión de jerarquía en cada ancho y estado de error. |
| Carga cognitiva | Solicitar solo datos necesarios para la acción actual. | Recorrido sin recordar información de otra pantalla. |
| Estética-usabilidad | Coherencia visual con texto legible y errores fáciles de resolver. | Capturas y ejecución de éxito/error, no solo valoración estética. |
| Posición en serie | Mantener acciones principales en posiciones previsibles. | Orden visual y de teclado coherentes al cambiar de ancho. |
| Tendencia a la meta | Avance calculado a partir de trabajo real y objetivo elegido. | Cálculo comprobable; sin progreso inventado ni presión para continuar. |
| Von Restorff | Diferenciar la acción o aviso relevante sin depender solo del color. | Una prioridad visual clara, etiqueta e indicador accesible. |
| Zeigarnik | Guardar el siguiente paso y permitir dejar trabajo pendiente en calma. | Recuperación tras interrupción; cierre sin mensajes culpabilizadores. |
| Fluir | Objetivo y duración de sesión visibles; interrupciones controlables. | Recorrido empezar/pausar/cerrar con hora de fin respetada. |
| Fragmentación | Separar proyectos, tareas, bloques e historial en grupos comprensibles. | Etiquetas y agrupaciones revisadas con contenido real. |
| Memoria de trabajo | Mostrar contexto y conservar lo escrito ante errores. | Recuperación de formulario y navegación sin reconstruir datos de memoria. |
| Navaja de Occam | Cada control tiene un propósito en el recorrido. | Justificación de controles y ausencia de pasos redundantes. |
| Conectividad uniforme | Usar líneas/conexiones solo para relaciones reales. | Correspondencia con dependencias reales; no relaciones decorativas ambiguas. |
| Fitts | Controles cómodos de pulsar y separados; acciones frecuentes accesibles. | Medir áreas interactivas, separación y operación táctil/teclado. |
| Hick | Ofrecer una decisión principal y opciones avanzadas cuando sean pertinentes. | Recorrido frecuente sin atravesar menús innecesarios. |
| Jakob | Formularios, navegación y calendario con convenciones reconocibles. | Comportamiento esperado de enlaces, botones, foco y regreso. |
| Semejanza | Apariencia consistente para funciones y estados equivalentes. | Revisión de componentes y variantes en todas las pantallas. |
| Miller | Agrupar por significado; evitar imponer siete opciones como regla. | Revisar comprensión de grupos, no un recuento arbitrario. |
| Parkinson | Bloques con comienzo y fin; ampliación deliberada. | Llegar a la hora de fin no extiende silenciosamente la sesión. |
| Postel | Admitir variaciones de entrada acordadas y explicar límites. | Unicode/espacios conforme al contrato; sin debilitar validación ni seguridad. |
| Proximidad | Etiqueta, ayuda y error junto a su campo. | Asociación visual y programática, también en móvil. |
| Prägnanz | Estados y formas simples, sin iconos ambiguos como única explicación. | Comprensión de estados con texto y estructura semántica. |
| Región común | Contenedores que agrupen una unidad funcional real. | Bordes/fondos y orden de lectura reflejan esa agrupación. |
| Tesler | Resolver complejidad técnica en el sistema y explicar decisiones humanas. | Hora/zona/solapes comprensibles; sin exponer errores internos. |
| Modelo mental | Distinguir tarea, bloque planificado y sesión realizada. | Terminar una sesión no completa automáticamente una tarea. |
| Usuario activo | Primer uso útil sin leer un manual. | Estado vacío orientador y ayuda contextual durante el recorrido. |
| Pareto | Facilitar las acciones frecuentes sin eliminar funciones necesarias. | Priorización inicial explícita, contrastada después con uso real; no asumir porcentajes medidos. |
| Fin de pico | Cierre claro con resultado y posibilidad de recuperación. | Confirmación cierta; fallos sin pérdida de datos ni falso éxito. |
| Sesgo cognitivo | Métricas comprensibles, opciones neutrales y valores modificables. | No confundir estimación con tiempo real; no penalizar descanso. |
| Sobrecarga de opciones | Personalización por categorías y revelación progresiva. | Valores iniciales útiles y retorno a configuración conocida. |
| Doherty | Respuesta visual temprana y espera honesta. | Medir objetivo de feedback <400 ms; no fingir guardado ni porcentaje de progreso. |

Matices consultados directamente: [Miller](https://lawsofux.com/es/ley-de-miller/), [Postel](https://lawsofux.com/es/ley-de-postel/), [Doherty](https://lawsofux.com/es/umbral-de-doherty/) y [Parkinson](https://lawsofux.com/es/ley-de-parkinson/). El objetivo de feedback no garantiza que red y servidor terminen en 400 ms. No se añadirán esperas artificiales ni mecanismos de enganche que contradigan la hora de cierre elegida.

## Responsive como condición de aceptación

La interfaz debe adaptarse continuamente al espacio disponible, también entre breakpoints. Conservar funciones, información y orden lógico en móvil, tablet, portátil y escritorio; pantallas grandes limitan el ancho de lectura. Un calendario denso ofrecerá agenda equivalente en espacios pequeños. No se exige a un móvil reproducir la distribución de escritorio.

Matriz mínima de revisión por recorrido (píxeles CSS): 320, 360, 390, 480, 600, 768, 820, 1024, 1280, 1440, 1920 y 2560; además a ambos lados de cada breakpoint y alturas reducidas/orientación horizontal. Probar zoom real al 200 %, reflow a 320 px CSS y texto ampliado. La emulación de ancho equivalente no sustituye toda la comprobación de zoom real.

Comprobar: sin solapes, recortes ni desplazamiento horizontal de página; textos largos y Unicode; vacío, cargando, error y éxito; foco visible y nunca oculto por cabeceras; navegación por teclado; táctil sin dependencia de hover; alternativa a arrastrar; teclado virtual y áreas seguras en dispositivos móviles. Objetivo interno para acciones táctiles principales: área de 44 × 44 px CSS como mínimo. Este tamaño es una decisión del producto, no una cifra atribuida a la ley de Fitts.

Accesibilidad: mantener objetivo WCAG 2.2 AA del proyecto; contrastes, nombres accesibles, anuncios de estado, movimiento reducido y semántica. Toda variante personalizable (tema, densidad, tamaño de texto, paneles) debe pasar sus verificaciones; combinaciones que oculten acciones o hagan ilegible el texto requieren corrección antes de entrega. Revisar Chromium, Firefox y WebKit y complementar con móvil/tablet reales; indicar qué entorno se ha probado, sin afirmar cobertura universal por emulación.

## Evidencia actual y trabajo pendiente

La primera entrega tiene pruebas Chromium a 320, 768 y 1440 px, más 720 px CSS como emulación del reflow de 1440 al 200 %. Cubren teclado, foco, ausencia de overflow y axe antes/después de creación; pruebas de interfaz cubren espera y errores. Ver e2e/create-project.spec.mjs y progress/tdd_create_project_integration.md. Esto acredita ese alcance anterior, no toda esta nueva matriz.

Pendiente de comprobar con evidencia nueva: anchos intermedios y grandes, límites de breakpoints, orientaciones/alturas pequeñas, zoom real, ampliación de texto, controles táctiles medidos, teclado virtual, Firefox/WebKit y dispositivos reales. El feedback <400 ms requiere medición. La facilidad de aprendizaje/carga cognitiva requiere revisión de uso. Planificación, sesiones y personalización se evaluarán cuando existan sus respectivos recorridos. No se atribuyen resultados de pruebas que todavía no se han ejecutado.

En cada futuro contrato UI, enlazar este documento y convertir las condiciones aplicables en escenarios concretos. En el informe de revisión, adjuntar la matriz de 30 filas con su evidencia y los dispositivos/estados probados. Este documento incorpora requisitos; no modifica el contrato ni los resultados históricos de create_project y no aprueba publish_outbox.
