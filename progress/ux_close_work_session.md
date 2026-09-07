# Revisión UX del cierre de sesión16

Contrato16@s41 y docs/ux-requirements.md. La matriz aplica los30 principios al recorrido real y distingue observaciones verificadas de límites humanos o físicos. R=responsive de31anchos en Chromium/Firefox/WebKit; T=texto200 a320/768/1440 en esos tres motores; N=zoom nativo Chromium200 a320CSS. Cada modalidad recorre formulario, envío pendiente, incertidumbre, cierre recuperado y fallo de lectura.

| Principio | Aplicación y evidencia observable | Límite |
| --- | --- | --- |
| Atención selectiva | Formulario, confirmación y sesión abierta se distinguen; capturasR/T/N revisadas. | Atención humana no medida. |
| Carga cognitiva | Dos notas opcionales y contexto temporal visible; no repetir inicio/fin. | No estudio con personas. |
| Estética-usabilidad | Tipografía y controles existentes; geometría/axeR/T/N sin fallos finales. | Coherencia visual no prueba facilidad universal. |
| Posición en serie | Avance, siguiente paso y confirmar conservan ordenDOM; Enter y foco real comprobados. | No lector de pantalla físico. |
| Tendencia a la meta | Total del trabajo real, sin barra ni progreso inventado. | Sin nueva métrica de meta. |
| Von Restorff | Cierre/error escritos y anuncios semánticos, no sólo color;35axe0. | Axe no certifica todas las ayudas técnicas. |
| Zeigarnik | Siguiente paso guardado y recuperable por URL; notas largas recuperadas literalmente. | No historial global18. |
| Fluir | Cierre deliberado con inicio/fin visibles y salida sin culpa. | Sin avisos17 ni cierre automático. |
| Fragmentación | Formulario, recibo y consulta de abierta en grupos separados. | No lista histórica nueva. |
| Memoria de trabajo | Notas retenidas ante incertidumbre y412; K recupera las mismas notas reales. |412 cubierto en UI, no en este caso geométrico. |
| Navaja de Occam | Cierre directo, notas opcionales y recuperación contextual. | Sin preferencias nuevas. |
| Conectividad uniforme | Enlaces a tarea y sesión estable antesPOST; navegación funcional de C. | No diagrama de relaciones. |
| Fitts |515medidas verifican44px, límites y no solapes; defecto inicial21px corregido y revalidado. | No dispositivo táctil físico. |
| Hick | Confirmar como acción principal; comprobar sólo ante incertidumbre. | Tiempo de elección humana no medido. |
| Jakob | Textarea nativo, enlaces, Enter y recarga; foco visible en capturas. | No todas las ayudas técnicas. |
| Semejanza | .field/.task-form y anuncios reutilizan convenciones15. | Sin temas/densidades nuevos. |
| Miller | Contexto, notas y resultado son grupos semánticos. | No regla numérica universal de opciones. |
| Parkinson | Cerrar no cambia fin previsto ni inicia otra sesión automáticamente. | Sin ampliación17. |
| Postel | Unicode válido y plaintext; notas largas con espacios/saltos conservados y pre-wrap medido. | Validación2000CP cubierta por API/UI; no aceptación permisiva fuera de contrato. |
| Proximidad | Ayuda previa y etiquetas asociadas a textarea; capturasR/T/N legibles. | Sin evaluación perceptiva con usuarios. |
| Prägnanz | Cierre, incertidumbre y fallo de lectura descritos con texto. | Comprensión humana no certificada. |
| Región común | Lector específico y sección aparte para otra sesión abierta. | No contenedores decorativos nuevos. |
| Tesler | Keys/revisiones internas; fecha, zona y total legibles. | FallbackUTC componente; no TZDB cliente propia. |
| Modelo mental | Aviso explícito: cerrar no completa tarea; recibo separado de activa. | Estado de tarea real cubierto por funcional de C. |
| Usuario activo | Ayuda contextual antes del botón; notas opcionales sin manual obligatorio. | Aprendizaje no medido con personas. |
| Pareto | Cierre frecuente directo, recuperación disponible ante fallo. | Sin porcentajes de uso inventados. |
| Fin de pico | Cierre cierto sólo tras recibo válido; ACK sustituido por503 y K recupera resultado real. | No se presume rollback ante respuesta perdida. |
| Sesgo cognitivo | Neto sin penalizar descanso, notas no calculan progreso. | Sin métricas de productividad. |
| Sobrecarga de opciones | Dos campos opcionales, sin menús de ajuste. | No personalización nueva. |
| Doherty | Feedback desdeEnter antes de liberar respuesta: R3,4/3/6ms, T2/3/5ms, N2,2ms. | Medición local; no promete latencia de servidor. |

## Ejecuciones y artefactos

| Modalidad | Resultado real | Medidas / axe |
| --- | --- | --- |
| ChromiumR | ded1ca EXIT0,1/1 |155 /5sin violaciones |
| ChromiumT |36257a EXIT0,1/1 |15 /5sin violaciones |
| FirefoxR+T |3fc885 EXIT0,2casos |170 /10sin violaciones |
| WebKitR+T |3fc885 EXIT0,2casos |170 /10sin violaciones |
| ChromiumN |1ae8ae EXIT0,1/1 |5 /5sin violaciones |

Total:515 mediciones,35axe sin violaciones. Son tres casos de guion ejecutados en siete combinaciones pertinentes, no515tests. R usa31anchos y altura400a768; T verifica factor2 en cada elemento de texto. N usa chrome.tabs.setZoom=2, DPR1,5→3 y320CSS, confirmado6e0bde; no emulación de ancho presentada como zoom.

Rutas: .e2e-work/close-work-real/{chromium,firefox,webkit}/{ux,text200}/ conserva geometry.json, feedback.json, capturas viewport/fullPage, skiplink y axe; T añade font-scale.json. N está en .e2e-work/close-work-native/organizationweb-e2e-23576/evidence/. Capturas inspeccionadas: Chromium incertidumbre320, Chromium cierre con texto200, WebKit formulario1440, Chromium error a zoom nativo200. No se observaron recortes horizontales; notas largas requieren scroll vertical. Skiplink sin foco permanece fuera del viewport; no se infiere defecto desde fullPage.

Defecto inicial: Volver a la tarea medía21px. Se conserva geometry-initial-44px.json (SHA51817A12…004520A). Único cambio productivo de esta fase: regla propia .work-session.reader a con mínimos44px; SCSS SHA0E41A5408141BFE004548018AC35CF5C32E9B239955759FA18E304D7DB84FC80. TS/TSX intactos. Sintaxis y duración omitida en el primer guion fueron incidentes de preparación, detallados en tdd_close_work_ux.md.

Límites: no se certifican móviles/tablets físicos, teclado virtual, áreas seguras, lectores de pantalla reales ni facilidad de uso universal. No se implementan17/18. Funcionales de C cubren running/paused/ACK perdido y GETA anterior@s39; estas ejecuciones UX no se presentan como repetición independiente de todos esos oráculos.
