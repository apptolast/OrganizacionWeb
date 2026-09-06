# Revisión UX13 — treinta principios y evidencia disponible

**Dictamen: aprobación parcial del recorrido nominal observado; verificación UX global pendiente.** No se detecta un defecto bloqueante nuevo en las cuatro capturas inspeccionadas. Hay dos mejoras de presentación concretas para valoración del coordinador y límites de evidencia que impiden cerrar toda la matriz. Revisión independiente, sólo lectura: no se ejecutaron navegador, axe, tests ni cambios de fuentes/SCSS.

## Referencia y criterio

Se contrastan `docs/ux-requirements.md`, las30 filas de `tdd_reschedule_e2e.md` y la evidencia posterior de `tdd_reschedule_ux.md`. Referencia primaria consultada el6 de septiembre: [Laws of UX en español](https://lawsofux.com/es/). Se usan sus principios para evaluar decisiones, no para certificar efectos psicológicos. Las observaciones de la tabla son análisis de este producto, no citas de la fuente.

Se conserva agrupación significativa sin imponer siete opciones ([Miller](https://lawsofux.com/es/ley-de-miller/)); se valora reducir conversiones que deba hacer la persona ([Tesler](https://lawsofux.com/es/ley-de-tesler/)). La tolerancia de entrada no autoriza relajar identidad, seguridad o DTO ([Postel](https://lawsofux.com/es/ley-de-postel/)). La respuesta temprana es distinta del éxito de servidor ([Doherty](https://lawsofux.com/es/umbral-de-doherty/)); prevalece la decisión del producto de no fingir progreso ni añadir retrasos deliberados.

## Evidencia realmente leída

- E1: nominal API/PostgreSQL1/1 PASS993392 y fuente `e2e/reschedule.spec.mjs`; identidad/creación original, movimiento/cancelación/historial y tarea pending final explícito.
- E2: recuperación1/1 inicialmente GREENc6e234/4f559f y fuente `reschedule-recovery.spec.mjs`; ACK perdido después de201 real, reinicio material, cancelación posterior, recuperación histórica y conteos sin nueva escritura. No se confunde con recargar una página.
- E3: UX1/1 inicialmente GREENe1c698 y `reschedule-ux.spec.mjs`; cuatro estados×31anchos, cuatro alturas400 y cuatro resultados axe vacíos. Relectura de geometry.json a08035:124entradas, overflow máximo0, ancho mínimo65,4375 y altura mínima44 en los controles seleccionados. No son mediciones nuevas del juez.
- E4: inspección visual propia de `.e2e-work/reschedule-real/chromium/ux/move-320.png`, `review-1440.png`, `cancel-320.png`, `history-1440.png`. Son capturas fullPage; el visor las redujo para presentación. Se evalúan jerarquía/agrupación, no agudeza visual ni tamaño físico desde esa reducción.
- E5: fuentes actuales del árbol aislado `reschedule-block.tsx`, `change-submit.tsx`, `block-confirmation.tsx`, `reschedule-history.tsx`, `block-details.tsx`, composición TaskBlocks. La lectura de código respalda intención/semántica; no acredita por sí sola interacción real ni ausencia de carreras.

El autor confirma que texto200 y zoom nativo están en elaboración. No se atribuyen sus resultados futuros. La antigua frase de la matriz «toda evidencia pendiente» describe el primer RED; E1–E4 la superan parcialmente. Este documento establece el corte actual sin reescribir aquella historia.

## Matriz de30 observaciones

«Observado» significa sólo el alcance explícito de la celda. «Parcial» conserva comprobaciones pendientes; no es PASS psicológico.

| ID / principio | Observación concreta y evidencia | Resultado / límite |
| --- | --- | --- |
| U01 Atención selectiva | E4 diferencia título de tarea, bloque, editor y antes/después. Al abrir edición desaparecen las acciones de otras filas (E5). | Parcial: jerarquía nominal clara; falta captura de error/espera. Los botones secundarios comparten relleno con confirmar, sin demostrar por ello error de uso. |
| U02 Carga cognitiva | E4/E5 piden zona/inicio/fin en movimiento y sólo confirmación al cancelar; objetivo visible, un editor. | Observado nominal; no estudio de esfuerzo mental. Conversión minutos/segundos: F1. |
| U03 Estética-usabilidad | Paleta, bordes y tipografía coherentes entre tarjeta/editor/historial, Unicode conservado en E4; E3 no registra overflow. | Parcial: éxito revisado; texto ampliado/error/otros motores pendientes. Belleza no equivale a facilidad probada. |
| U04 Posición en serie | E4 mantiene zona, inicio, fin, revisar y confirmar en orden vertical. E3 activa acciones con Enter y comprueba dos destinos de foco. | Parcial: no recorrido Tab exhaustivo ni orden demostrado para cada ancho por una captura. |
| U05 Tendencia a la meta | E1 conserva tarea pending después de cancelar; E4 declara tiempo planificado y no presenta contador de trabajo realizado. | Observado para este recorrido; no hay progreso de sesiones aplicable a13. |
| U06 Von Restorff | E4 separa encabezados y antes/después; E5 usa alert/status con palabras para fallo, incertidumbre y confirmación. | Parcial: éxito visible; falta contraste visual del error real. No depende sólo del verde para explicar cancelación. |
| U07 Zeigarnik | E2 recupera intención tras pérdida real de ACK y reinicio; E4/E5 advierten que salir no revoca operación enviada. | Observado en recuperación; no prueba memoria humana. El borrador local no promete persistencia general tras cierre. |
| U08 Fluir | E5 ofrece salida deliberada y mantiene aviso de transmisión; no impone continuar trabajando. | Parcial: salida durante espera/privacidad requiere evidencia propia. Iniciar/pausar/cerrar sesión de trabajo no aplica a13. |
| U09 Fragmentación | E4 agrupa editor, revisión, confirmación histórica, estado actual e historial con encabezados y contenedores. | Observado; las repeticiones muestran hechos distintos, no se eliminan para abreviar sacrificando trazabilidad. |
| U10 Memoria de trabajo | E4 muestra contexto, antes/después y presupuesto junto a confirmar; E5 conserva valores de destino en recuperación. | Parcial: falta recorrido browser de rechazo y corrección; F1 dificulta comparación de unidades. |
| U11 Navaja de Occam | E5/E4 usan panel inline y controles nativos; consulta de cada estado histórico nace de botón explícito. | Observado diseño; sin calendario/modal/dependencia nueva. E3 no cuenta por sí sola todos los GET para certificar ausencia de consultas por fila. |
| U12 Conectividad uniforme | E1/E2 conservan blockId y E4 enlaza conceptualmente antes/después con encabezados; no hay líneas que inventen dependencias. | Observado; identidad técnica y agrupación visual no se confunden con relación entre tareas. |
| U13 Fitts | E3 conserva mínimo44px de alto, ancho mínimo65,4375 y sin intersección en controles seleccionados de cuatro estados. | Parcial: evidencia geométrica real Chromium; no se midieron selectores DST/consentimiento ausentes en nominal, dedos físicos ni teclado virtual. |
| U14 Hick | E5 exige revisar antes de confirmar y revela aceptación sólo si hay exceso; E4 muestra una propuesta. | Parcial: consentimiento con exceso todavía no observado aquí en browser. No medir tiempo de decisión por número de botones. |
| U15 Jakob | E4/E5 usan datetime-local, input/datalist de zona, botones y enlaces; E3 Enter/foco pasan. | Parcial: ni datalist abierto ni navegación Tab completa ni todos los motores están acreditados. |
| U16 Semejanza | E4 conserva tarjetas/controles para funciones equivalentes; E5 repite lenguaje de consulta/estado histórico. | Parcial: comparación nominal consistente, faltan variantes error/carga para cerrar. |
| U17 Miller | E4 organiza por significado: objetivo/destino, antes/después, recibo/estado actual. No se impone un máximo arbitrario de7. | Observado estructural; comprensión por personas sigue pendiente. |
| U18 Parkinson | E1 confirma exactamente destino y duración elegidos, E4 muestra inicio/fin; no existe ampliación automática en este panel. | Observado replanificación; timer y cierre de sesión de trabajo no aplican a13. |
| U19 Postel | E3 admite objetivo Unicode; E5 presenta horas/offsets explícitos y validación por campo. | Parcial: fixture UTC no acredita DST, catálogo no disponible ni todas las variaciones contractuales. Mantener validadores estrictos. |
| U20 Proximidad | E4 asocia etiquetas contiguas a inputs; E5 añade id/aria-describedby/aria-invalid a cada error. | Parcial: E3 axe vacío en estados nominales; falta geometría con errores largos/offsets visibles. |
| U21 Prägnanz | E4 usa «Antes», «Después», «Cancelado», «Confirmación histórica» y «Estado actual»; no iconos como única explicación. | Observado nominal; referencia UUID sin etiqueta es mejora F2, no ambigüedad del estado cancelado. |
| U22 Región común | E4 contiene cada bloque, confirmación y entrada de historial en unidades visuales reales. | Observado; página larga no implica defecto ni obliga a introducir modal/calendario. |
| U23 Tesler | E5 carga sugerencias de zona y servidor resuelve DST; E4 presenta zona/presupuesto. | Parcial: F1 deja conversión de unidades a la persona. Falta recorrido DST/error para juzgar explicación de ocurrencias. |
| U24 Modelo mental | E1 conserva creación histórica y tarea pending; E4 distingue reserva, confirmación y estado actual, incluso cancelado. | Observado; no equipara mover con trabajar ni cancelar con terminar tarea. |
| U25 Usuario activo | E4 tras cancelar muestra vacío, Planificar bloque e historial; E5 texto «Todavía no hay cambios» para historial vacío. | Parcial: orientación nominal visible, primer uso autónomo no ensayado por personas. |
| U26 Pareto | E5 ofrece mover/cancelar en la fila y consulta de historial deliberada; E1 completa acciones sin navegación a otra herramienta. | Observado como decisión de diseño, sin asumir porcentajes reales de uso. |
| U27 Fin de pico | E2 termina con hecho original recuperado y estado posterior; E5 conserva recibo si falla consulta de vigencia y ofrece reintento. | Parcial: recuperación real acreditada; falta fallo de consulta de estado en browser. No se infiere satisfacción emocional. |
| U28 Sesgo cognitivo | E4 dice «tiempo planificado, no trabajo realizado»; aceptación de exceso E5 no usa culpa ni productividad ficticia. | Observado lenguaje nominal; falta evaluar comprensión del presupuesto mixto (F1) y exceso visible. |
| U29 Sobrecarga de opciones | E5 un editor y revelación de ocurrencias/consentimiento según necesidad; catálogo bajo foco. | Parcial: comportamiento estructural; selección DST y reinicio por cambio de contexto no medidos en E3. |
| U30 Doherty | E5 anuncia «Revisando movimiento», «Procesando cambio», «Consultando estado actual» antes de resultado. | Pendiente medición<400ms con respuesta retenida. E1 rápido y presencia de role=status NO prueban esa latencia. |

## Mejoras concretas para valoración, no bloqueos inventados

- **F1 (P3, U02/U10/U23/U28): unidades mixtas en la decisión.** `reschedule-block.tsx:514` y review-1440 muestran presupuesto120 minutos frente a solicitado3600 segundos. La cantidad es correcta y preserva la precisión contractual, pero comparar capacidad exige convertir unidades mentalmente. Valorar una representación humana homogénea (por ejemplo minutos y segundos cuando proceda), conservando datos exactos; no cambiar DTO ni redondear capacidad. Es una mejora sustentada en la captura, no un fallo de cálculo ni solicitud de nueva funcionalidad.
- **F2 (P3, U21/U25): identificador sin etiqueta.** `block-confirmation.tsx:65` imprime change.id solo entre objetivo y fecha. E4 cancel/history lo confirma. Valorar «Referencia del cambio» para explicar su propósito; conservar ID exigido y no confundirlo con clave de reenvío. No bloquea recuperación ni significa que se haya expuesto un secreto.

## Pendientes y límites del dictamen

Error/espera/exceso/DST en navegador, feedback<400ms, texto200, zoom nativo, Firefox/WebKit y Tab completo no están certificados por esta revisión. Texto/zoom están en curso por el autor; registrar resultados nuevos cuando existan. Dispositivos reales, teclado virtual/áreas seguras, lector de pantalla y pruebas con personas siguen sin evidencia específica. No exigir estudios cognitivos como una prueba automática inventada ni presentar emulación como cobertura universal.

El skiplink aparece en algunas fullPage a una altura que depende del scroll al capturar. No se diagnostica overlay/robo de foco con esa imagen: haría falta comprobar su rectángulo respecto al viewport y estado de foco. El autor ya prepara esa comprobación; este juez no repite navegador.

Evidencia consultada33d33a/6584eb/8e0f3f/a08035/6d83d3. Se escribía este dictamen con fuentes/test/SCSS intactos y sin tocar rutas protegidas. La revisión respalda parcialmente el diseño observado; no aprueba UX completa ni feature13 done.
