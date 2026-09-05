# edit_project — evidencia UI/UX

Revisor de navegador: integration_craftsman. Se reutiliza el fixture aislado con PostgreSQL y API reales. La matriz final incorpora 22 anchos: los 12 base (320, 360, 390, 480, 600, 768, 820, 1024, 1280, 1440, 1920 y 2560) y los bordes 359/361, 599/601, 699/701, 1099/1101 y 1599/1601 píxeles CSS. Los 22 anchos pasaron en la corrida final; incluye altura 400 a ancho 768 y texto de 120/4000 caracteres. Axe no detectó infracciones en las reglas WCAG A/AA seleccionadas. Todos los controles principales midieron al menos 44 por 44 píxeles CSS, dentro del ancho disponible. Edición y guardado por teclado confirmaron el foco de retorno; Cancelar funcionó mediante tap en contexto táctil emulado.

Zoom nativo de Chromium mediante extensión temporal y `chrome.tabs.setZoom(2)`, dentro de perfil propio: DPR 1,5 a 3; ancho interior 1426 a 713 con la misma ventana exterior de 1440. Ventana propia estrechada a 654 produjo 320 CSS, con documento 312/312 sin desbordamiento. Se guardó un cambio real a ese tamaño/zoom. Las capturas CDP compensan sus coordenadas de zoom y se toman con scroll superior, evitando que un elemento fixed fuera del viewport aparezca en la captura completa. Se inspeccionó visualmente la captura final estrecha: jerarquía, etiquetas, descripción, botones y confirmación legibles. El nombre largo mantiene el comportamiento nativo desplazable del input de una línea.

Evidencia en outputs: edit-project-desktop.png, edit-project-mobile.png, edit-project-real-zoom.png, edit-project-real-zoom-320.png y edit-project-real-zoom.json. La revisión visual independiente del coordinador queda separada de las mediciones y de la autoría de estas pruebas.

| Principio | Aplicación concreta | Evidencia y límites |
| --- | --- | --- |
| Atención selectiva | Guardar cambios es la acción principal; Cancelar mantiene menor énfasis. | Captura y orden DOM. Atención humana no medida. |
| Carga cognitiva | Dos campos y una decisión explícita para guardar. | Recorrido real y textos de ayuda visibles. |
| Estética-usabilidad | Paleta, tipografía y tarjeta compartidas con el resto de la web. | Captura final inspeccionada; preferencia estética no medida. |
| Posición en serie | Título, campos y acciones siguen el orden de lectura. | Teclado recorre nombre, descripción y guardar. |
| Tendencia a la meta | No se muestran porcentajes de avance. | No aplicable: edición sin planificación de objetivos. |
| Von Restorff | Guardar tiene fondo sólido; conflicto explica su consecuencia con texto. | Recorrido de conflicto y captura. |
| Zeigarnik | El borrador permanece tras errores recuperables. | E2E 400/503/500/red y 412; persistencia entre sesiones no prometida. |
| Fluir | No se inicia una sesión de trabajo ni un temporizador. | No aplicable a este corte. |
| Fragmentación | Nombre y descripción forman grupos con etiqueta, ayuda y error. | Semántica y axe; campos separados visualmente. |
| Memoria de trabajo | Precarga los datos y ofrece recarga explícita de la versión guardada. | Dos pestañas reales, conflicto y recuperación. |
| Navaja de Occam | Formulario y precondición existentes; sin historial adicional ni autosave. | Alcance del contrato, API y flujo visibles. |
| Conectividad uniforme | No representa relaciones entre entidades. | No aplicable. |
| Fitts | Controles principales de al menos 44 por 44 CSS. | Medición en 22 anchos y zoom nativo estrecho. Táctil físico pendiente. |
| Hick | Guardar o Cancelar; recarga aparece ante conflicto o fallo de carga conservando borrador. | Estado 412 probado. Tiempo de decisión humano no medido. |
| Jakob | Input, textarea, botón y enlace nativos. | Teclado, recarga de URL y cancelación mediante tap. |
| Semejanza | Controles y espaciado reutilizan el formulario de creación. | Código de estilos y capturas. |
| Miller | Dos grupos semánticos sin aplicar una regla arbitraria de siete. | Estructura del formulario; comprensión humana pendiente. |
| Parkinson | No asigna límites temporales al trabajo. | No aplicable. |
| Postel | Unicode admitido y HTML mostrado como texto; respuesta validada. | PUT real con Unicode/HTML literal y recarga. |
| Proximidad | Error de campo junto a su ayuda; respuesta global junto a acciones. | E2E 400 verifica aria-invalid; axe y captura. |
| Prägnanz | Etiquetas completas y título inequívoco. | Árbol semántico, foco y axe. |
| Región común | Campos y acciones están dentro de la misma tarjeta. | Capturas sin paneles ficticios. |
| Tesler | ETag y versión quedan a cargo del software. | Dos pestañas compiten sin exigir al usuario recordar una versión. |
| Modelo mental | Editar cambia palabras del proyecto; Cancelar vuelve al detalle. | Recorrido real; estado y fecha de creación conservados. |
| Usuario activo | Precarga inmediata de datos con espera explícita. | Acceso directo /editar; sin formulario vacío presentado como dato guardado. |
| Pareto | Se implementan los dos campos editables aprobados. | No se atribuyen porcentajes de uso ni se añade personalización especulativa. |
| Fin de pico | Confirmación sólo tras respuesta real; recuperación permite continuar. | E2E retrasó la respuesta real, luego comprobó guardado, reload y no-op. |
| Sesgo cognitivo | Sin rachas, culpa ni sobrescritura automática. | Mensaje de conflicto conserva borrador y explica recargar. |
| Sobrecarga de opciones | Sólo acciones vigentes y campos autorizados. | Inspección de formulario y estados de error. |
| Doherty | Guardando cambios se anuncia antes de confirmar la respuesta. | Medición con performance.now y MutationObserver: 3 ms en el ciclo medido; sin garantía universal de latencia. |

## Límites

No se han usado móviles/tablets físicos, teclado virtual ni lector de pantalla real. La emulación táctil no demuestra comportamiento de hardware. Axe no certifica WCAG completa. Las capturas y métricas comprueban este entorno; no sustituyen evaluación humana de comprensión, aprendizaje o preferencia. Las pruebas de errores interceptados verifican la UI; fallos reales de almacenamiento y autorización pertenecen a las pruebas backend. La frontera de crash del publicador no se repite al no cambiarla esta interfaz.

## Revisión visual y motores complementarios

El coordinador confirmó la revisión independiente de escritorio, móvil y zoom estrecho, con métricas coherentes y sin desbordamiento de página. El autor frontend inspeccionó además escritorio/móvil; sus observaciones no se presentan como revisión independiente. Firefox y WebKit ejecutaron cada uno el recorrido real de edición, versión/evento/no-op y recarga: 2 pruebas verdes en 10,8 segundos. Feedback observado de guardado: 2 y 13 ms. Esta comprobación no amplía a esos motores la matriz de 22 anchos ejecutada en Chromium.


Cierre congelado: 18 E2E verdes en 1,3 minutos, incluidos los 22 anchos de edición (16,8 segundos para ese recorrido) y regresión histórica. Guardado anunciado a los 2 ms en la corrida final. Capturas normales y zoom nativo refrescados sobre el build final; zoom repitió las mismas dimensiones y confirmó otro PUT real.
