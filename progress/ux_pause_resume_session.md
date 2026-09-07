# Revisión UX de pausa y reanudación

Criterio: docs/ux-requirements.md y contrato15@s38. Esta matriz documenta decisiones y evidencia observable; no certifica comprensión, rendimiento humano ni todos los dispositivos. Las medidas siguientes corresponden al corte15 corregido, sin trasladar resultados históricos14 a15.

| Principio | Observación y evidencia prevista | Límite |
| --- | --- | --- |
| Atención selectiva | Una transición disponible por estado; aviso independiente al fallar. | Capturas revisadas; comprensión humana no medida. |
| Carga cognitiva | Pausar/reanudar no pide repetir datos del inicio. | Sin estudio con usuarios. |
| Estética-usabilidad | Reutilización de tipografía, botones y agrupación14. | Coherencia no prueba facilidad de uso. |
| Posición en serie | Actualizar y transición mantienen orden DOM; foco al encabezado cuando desaparece iniciador. | Capturas y orden DOM; no lector de pantalla físico. |
| Tendencia a la meta | Neto del snapshot; no barra inventada ni tiempo final acreditado. | No función de meta avanzada15. |
| Von Restorff | Etiquetas de estado, role=status y role=alert; sin depender sólo del color. | Axe y geometría pasan; no se certifica accesibilidad universal. |
| Zeigarnik | Incertidumbre conserva intención y permite comprobar; salida sin culpa. | No se conserva key tras recarga ni se promete identificarla. |
| Fluir | Fin previsto inmutable; pausa y reanudación explícitas. | Cierre16 fuera de alcance. |
| Fragmentación | Sesión separada de tarea y bloques; encabezado propio de estado. | No historial global18. |
| Memoria de trabajo | Hora, zona y estado visibles; key/revisión retenidas internamente. | Evaluación cognitiva no realizada. |
| Navaja de Occam | Una acción principal y actualización manual; recuperación aparece cuando procede. | No preferencias nuevas. |
| Conectividad uniforme | Enlace a tarea real heredado14; sin conexiones decorativas. | No diagrama de relaciones. |
| Fitts | 515 medidas verifican44px y ausencia de solapes en controles visibles. | Sin prueba táctil física. |
| Hick | Pausar o reanudar según estado confirmado, sin menú nuevo. | No medición de tiempo de elección humano. |
| Jakob | Botones nativos, Enter, navegación existente, recarga y enlace de tarea. | Navegador/teclado, no todas las ayudas técnicas. |
| Semejanza | Controles y anuncios reutilizan convenciones del producto. | No variantes de tema/densidad nuevas. |
| Miller | Inicio histórico, estado fechado y recuperación son grupos semánticos. | No regla numérica de opciones. |
| Parkinson | Pausa/reanudación no cambia plannedEndAt; prueba nominal real. | No ampliación o cierre16. |
| Postel | DTO cerrado y precisiónµs; contexto largo Unicode en fixtures. | No normalización permisiva fuera del contrato. |
| Proximidad | Ayuda de salida, error y recuperación en sección de sesión. | Capturas320 revisadas, incluida ampliación. |
| Prägnanz | En curso/En pausa escritos, sin iconos como única explicación. | No test de comprensión humano. |
| Región común | Región Sesión de trabajo y encabezado Estado de la sesión. | Se reutiliza contenedor existente. |
| Tesler | Tokens, keys y revisiones internos; fechas/zonas y neto legibles. | UTC fallback cubierto en componente, no TZDB propia. |
| Modelo mental | Inicio histórico distinto de estado actual; fallo de consulta no revoca recibo. | No completar tarea automáticamente. |
| Usuario activo | Acción pertinente tras descubrimiento; carga y error explicados. | Aprendizaje sin manual no medido con personas. |
| Pareto | Flujo frecuente directo; recuperación disponible ante fallo. | Sin porcentajes de uso inventados. |
| Fin de pico | Confirmación cierta y consulta posterior separadas; recuperación real por key. | Sin afirmar éxito si respuesta incompatible. |
| Sesgo cognitivo | Neto sólo hasta actualización; descanso no penalizado ni fin desplazado. | No métricas de productividad. |
| Sobrecarga de opciones | Sin nuevas opciones avanzadas; una transición según estado. | No personalización15. |
| Doherty | Medición desde Enter hasta anuncio antes de liberar respuesta retenida. | Tiempo local, no promesa de latencia de red. |

Límites pendientes: dispositivos físicos, teclado virtual, áreas seguras móviles, lectores de pantalla reales y estudios con personas. La emulación y los motores de escritorio no certifican esos entornos.


## Evidencia de cierre

| Modalidad | Estados y medidas | Resultado |
| --- | --- | --- |
| Chromium responsive | running/pending/uncertain/paused/query-error,31 anchos cada uno:155 medidas | e14766,5axe0; feedback1,3ms |
| Firefox responsive | mismos5×31:155 | 3a67cc,5axe0; feedback2ms |
| WebKit responsive | mismos5×31:155 | 01ed88,5axe0; feedback3ms |
| Texto200 en los tres motores | 5estados×320/768/1440:15 por motor,45 total; factor2 verificado por elemento | 7f4b25/3a67cc/01ed88,15axe0 |
| Zoom nativo Chromium200 | 5estados a320CSS,zoom2,DPR1,5→3 | 01ed88 (contexto Chromium propio),5axe0 |

Total actual:515 mediciones geométricas y35 análisis axe sin violaciones en esos estados. Los recorridos se ejecutan contra API/PostgreSQL reales; espera y errores controlados alteran sólo la entrega HTTP para comprobar recuperación. Nominal iniciar→pausar→reanudar→recargar pasa en los tres motores. Los cuatro casos únicos del archivo tienen diez ejecuciones de modalidades/motores pertinentes; no se suman repeticiones preliminares para inflar el conteo.

Rutas de evidencia: `.e2e-work/pause-resume-real/{chromium,firefox,webkit}/{ux,text200}/` contiene geometry.json,feedback.json,capturas y archivos axe; texto añade font-scale.json. Zoom nativo final en `.e2e-work/pause-resume-native/organizationweb-e2e-41552/evidence/`:zoom.json,geometry.json,capturasCDP y cinco axe. La etiqueta de proyecto Playwright [webkit] en01ed88 no cambia el motor del cuarto caso: éste abre expresamente Chromium nativo aislado.

Se inspeccionaron viewport de incertidumbre320, pausa con texto200 y error a zoom nativo200. Texto, acciones y foco permanecen visibles al desplazarse, sin recorte horizontal observado. El primer PNG viewport nativo vacío era un artefacto de captura; CDP sin clip produce imagen real15713d, conservando el corte anterior. El skiplink de fullPage no se usa para inferir solapamientos: sus rectángulos medidos sin foco están fuera del viewport.
