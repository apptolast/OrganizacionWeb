# Revisión UX final de Replanificar — alcance13

**Dictamen: APPROVED para el alcance UX de13 observado y documentado. Sin bloqueantes nuevos sustentados.** Se cierran las comprobaciones que estaban pendientes de ejecución en la matriz anterior; se conservan sus límites humanos y de dispositivos. Esto no declara feature13 done, no sustituye las campañas activas ni acredita por anticipado el E2E del corte común integrado.

Revisión independiente de UX, sólo lectura, el6 de septiembre de2026. No se ejecutaron suites, navegador, axe, mediciones nuevas ni comandos Git. No se modificaron fuentes/tests/configuración. Ponytail full y Caveman lite. Este dictamen no reabre ni reemplaza la revisión funcional de los componentes previamente escritos por distintos autores.

## Evidencia contrastada

- Requisito: docs/ux-requirements.md, sus30 filas y condiciones responsive; sin convertir heurísticas en certificación psicológica.
- Base: progress/review_reschedule_ux_principles.md y review_reschedule_ux_execution.md completos, más secuencia cronológica de tdd_reschedule_ux.md. Las frases antiguas «pendiente» conservan historia; sus resultados posteriores se aplican aquí sin reescribirlas.
- Nominal/recuperación:1/1 API/PostgreSQL993392 y1/1 ACK perdido/reinicio4f559f; hechos antes/después y tarea pending permanecen diferenciados. El reinicio corresponde al proceso/backend real documentado, no a recargar UI.
- Geometría Chromium:124 mediciones en31anchos y cuatro alturas400, cuatro informes axe vacíos, e1c698. Texto200:12medidas y cuatroaxe,627f29. Zoom nativo: getZoom2 y DPR1,5→3 con ventana real320CSS, cuatroaxe, fd7b5e/38da21. Son modalidades distintas, no reducción de viewport presentada como zoom.
- Condicional: errores reales de ambos offsets Madrid, elección por teclado, preview90min y consentimiento de exceso,201 final,8b0b99/d680b1. Nueve medidas y tresaxe; label del checkbox es objetivo clicable mayor de44×44, aunque la caja interna mida22×22.
- Firefox/WebKit: nominal y condicional2/2 por motor b0ed71/ed8d27; no extrapolarles31anchos, texto200, zoom nativo o reinicio.
- Feedback:3b3449/e526d3, preview real retenido,503 controlado y reintento200 real. Relectura propia de evidence.json en7bd58a confirma2,5ms de feedback DOM,2llamadas, estados pending/error y cero infracciones registradas. No mide latencia del servidor ni anuncio de lector de pantalla.
- Inspección propia de capturas error-320 y review-1440: jerarquía, etiquetas, error junto al botón y foco visible coherentes; el visor redujo ambas fullPage, por lo que no se usa la imagen reducida para medir tamaño físico. Fuente actual de reschedule-block/change-submit/block-confirmation/reschedule-history respalda semántica de estados y las dos observaciones P3, lecturas f6292b/7bd58a.
- Skiplink: JSON propio releído7bd58a indica sin foco,y=-100,alto45,bottom=-55. Su aparición en fullPage no acredita invasión del viewport ni robo de foco.

## Matriz final de30 principios

«Aprobado en alcance» es juicio del recorrido respaldado por la evidencia indicada, no una medición de facilidad universal. Los límites de la última columna permanecen visibles.

| ID / principio | Juicio al corte final | Evidencia y límite |
| --- | --- | --- |
| U01 Atención selectiva | Aprobado en alcance: editor, revisión y error se distinguen; una propuesta activa. | Capturas revisión/error y estados anunciados. No estudio de atención. |
| U02 Carga cognitiva | Aprobado con mejora P3: objetivo/contexto visibles y sólo campos necesarios. | Nominal más DST/consentimiento; unidades mixtas correctas requieren conversión mental. |
| U03 Estética-usabilidad | Aprobado en alcance: estilo consistente y recorrido conservado con ampliación/error. | Geometría, texto200, zoom real y motores; no demuestra satisfacción estética humana. |
| U04 Posición en serie | Aprobado en los recorridos comprobados: orden destino/revisión/confirmación estable. | Enter, Tab/ShiftTab, Space y focos condicionales reales; no Tab exhaustivo en cada combinación de ancho/estado. |
| U05 Tendencia a la meta | Aprobado: mover/cancelar no fabrica progreso ni completa la tarea. | Nominal conserva pending; tiempo etiquetado como planificado. Sesiones realizadas no aplican a13. |
| U06 Von Restorff | Aprobado: fallo/espera/confirmación tienen texto y semántica, no sólo color. | Error320, role=alert/status y feedback retenido. No inferencia psicológica. |
| U07 Zeigarnik | Aprobado para recuperación: hecho/intención incierta recuperables y salida sin culpa. | ACK perdido/reinicio; aviso de que cerrar editor no revoca operación. No promete autosave general. |
| U08 Fluir | Aprobado para replanificar: acción deliberada, espera clara y salida disponible. | Nominal/feedback/recuperación. Iniciar, pausar y cerrar trabajo pertenecen a14–17, no requisitos omitidos de13. |
| U09 Fragmentación | Aprobado: destino, antes/después, recibo, estado actual e historial son grupos distintos. | Capturas y estructura semántica; DST aparece donde se necesita. |
| U10 Memoria de trabajo | Aprobado con P3 de unidades: mantiene borrador/contexto durante fallo y reintento. | Feedback503→200 y recuperación durable; no prueba memoria de personas. |
| U11 Navaja de Occam | Aprobado: panel inline, controles nativos y consultas deliberadas. | Composición y E2E; no calendario/modal/dependencia adicional. Geometría no se usa como prueba de conteos GET. |
| U12 Conectividad uniforme | Aprobado: relaciones antes/después corresponden al mismo bloque, sin dependencias visuales inventadas. | Identidades nominales/recuperación y encabezados. |
| U13 Fitts | Aprobado para objetivos medidos, incluidos offsets y label de consentimiento. | Geometría44×44, no solapes; no certifica dedos físicos, teclado virtual ni cada combinación zoom×DST×motor. |
| U14 Hick | Aprobado: revisar antes de confirmar; ocurrencias y exceso aparecen sólo cuando proceden. | Flujo condicional real y consentimiento explícito. No se mide esfuerzo decisorio por contar botones. |
| U15 Jakob | Aprobado en recorridos de teclado/motores: controles nativos y regreso reconocible. | Tab/ShiftTab/Home/ArrowDown/End/Space/Enter. Datalist y todos los dispositivos no certificados. |
| U16 Semejanza | Aprobado: mismos patrones de tarjetas, controles y mensajes en nominal/error/espera. | Capturas y componentes compartidos; sin reclamar todas las futuras personalizaciones. |
| U17 Miller | Aprobado como estructura: agrupación por significado, sin regla artificial de siete. | Editor/revisión/recibo/estado/historial. Comprensión humana no ensayada. |
| U18 Parkinson | Aprobado para reservas: inicio/fin y cambio son explícitos, sin ampliación automática. | Nominal y destino DST90min confirmado. Aviso/cierre de sesión no aplican a13. |
| U19 Postel | Aprobado en alcance: Unicode y ocurrencias explícitas, errores recuperables sin relajar identidad/DTO. | Fixtures Unicode, DST real y validación común. No afirmar que un E2E agota todas las entradas. |
| U20 Proximidad | Aprobado: etiquetas y errores asociados al campo, incluido selector de ocurrencia. | Source aria-describedby/aria-invalid, condicional y axe. Lector de pantalla real pendiente. |
| U21 Prägnanz | Aprobado con P3: «Antes», «Después», «Cancelado» y «Estado actual» expresan estados distintos. | Fuente/capturas; UUID sin etiqueta es mejora de explicación, no estado falso. |
| U22 Región común | Aprobado: cada contenedor corresponde a una unidad funcional real. | Capturas nominal/error/historial, reflow y ampliación. Página larga no exige modal. |
| U23 Tesler | Aprobado con P3: servidor resuelve DST, UI ofrece elecciones y presupuesto explícitos. | Recorrido condicional real; conversión minutos/segundos aún mejorable, cantidades correctas. |
| U24 Modelo mental | Aprobado: reserva, creación histórica y vigencia se mantienen separados; no se acredita trabajo. | Nominal/recuperación y confirmación histórica con consulta de estado actual. |
| U25 Usuario activo | Aprobado como orientación: vacío y acciones principales explican cómo continuar. | Tras cancelación, planificación e historial accesibles. Primer uso autónomo con personas no probado. |
| U26 Pareto | Aprobado como prioridad de diseño: mover/cancelar en contexto y acceso a historial. | Recorridos completos sin herramienta externa; no porcentajes de uso inventados. |
| U27 Fin de pico | Aprobado en integridad de cierre: confirmación cierta y recuperación cuando falla consulta/respuesta. | ACK perdido/reinicio y separación hecho/vigencia en fuente; no se infiere satisfacción emocional ni browser de todo fallo de state. |
| U28 Sesgo cognitivo | Aprobado con P3: plan no equivale a trabajo y exceso requiere consentimiento neutral. | Texto y condicional90min; no penalización por descanso ni culpa. Comprensión de unidades no estudiada. |
| U29 Sobrecarga de opciones | Aprobado: un editor y revelación progresiva de offsets/consentimiento. | Nominal y condicional en tres motores. No extender a futuras preferencias. |
| U30 Doherty | Aprobado para medición realizada:2,5ms antes de respuesta, error/reintento honestos. | DOM Chromium retenido<400ms, sin guardado ficticio; no SLA universal ni latencia del lector/servidor. |

## Dos mejoras P3 conservadas

1. reschedule-block.tsx:514–516 mezcla presupuesto en minutos y reserva/solicitud/exceso en segundos. Las cantidades y unidades son explícitas, correctas y compatibles con la precisión contractual. Homogeneizar presentación facilitaría comparación; no constituye error funcional ni autoriza redondear DTO o cambiar cálculos.
2. block-confirmation.tsx:65 presenta change.id sin «Referencia del cambio». Añadir esa etiqueta sería más claro. El estado/tipo/fecha ya están separados y el ID no es una key de recuperación ni secreto. No bloquea confirmación, historial o privacidad.

No se exige corregir ambas durante campañas congeladas ni se inventa un RED para justificarlas. No hay otro bloqueante observable en la evidencia inspeccionada.

## Límites que siguen vigentes

Dispositivos físicos, teclado virtual/áreas seguras, lector de pantalla real y estudios de comprensión/esfuerzo/satisfacción no se han ejecutado aquí ni se declaran certificados. No se exige fabricar resultados ni equiparar axe con WCAG completa. Tampoco se afirma el producto cartesiano de todos los anchos, motores, ampliaciones y estados: el alcance ejecutado está enumerado.

El paquete UX queda aprobado para la revisión general del coordinador. La ejecución en el corte común, mutación y cierre funcional permanecen en sus puertas propias; esta lectura no repite esas pruebas ni adelanta sus resultados. Único archivo escrito: progress/review_reschedule_ux_final.md.
