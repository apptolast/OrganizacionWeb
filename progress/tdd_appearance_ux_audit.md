# Auditoría UX independiente de apariencia

C sólo ejecuta mediciones y scripts propios; B mantiene autoría de TS/SCSS
 y e2e/appearance.spec.mjs. No se cambian oráculos de B ni producción desde C.
Se aplican docs/ux-requirements.md y contrato appearance @s20–37.

## Primer recorrido nominal: RED real

Ejecutado el único E2E preparado por B en COMMON mediante
`node scripts/e2e.mjs e2e/appearance.spec.mjs`. EXIT1 1a40f9.
Stack propio organizationweb-e2e-17452, puerto18080. Build completado d9c6bd;
cleanup retiró exactamente sus contenedores, red y volumen. Puerto liberado.

PUT devuelve200 y html[data-theme=dark] pasa. Medición1440:
canvas rgb(248,249,245), texto rgb(35,57,47), sidebar rgb(238,241,233).
El oráculo esperaba canvas rgb(17,24,39); falla antes de la recarga.
No se atribuye GREEN a persistencia/recarga ni contraste de este recorrido.
B reconoce tokens SCSS pendientes y recibe el fallo para corregirlos.

390 entradas before/after idénticas en appearance_ux_nominal_before.json y
appearance_ux_nominal_after.json; log appearance_ux_nominal_initial.log.
Captura, JSON y error-context copiados sin borrar originales a
progress/appearance_ux_red_nominal. Esperar corte B y repetir sólo el mismo
nominal antes de nuevos ciclos de geometría/teclado/variantes.

## Secuencia pendiente

Después del nominal GREEN: medición de áreas44px, overflow, foco y contraste
sobre formulario/errores y rutas existentes con LIGHT/DARK/SYSTEM. Anchos
320..2560 y ambos lados de breakpoints; altura reducida. Luego texto200%,
zoom nativo200% a320CSS, Chromium/Firefox/WebKit, colores forzados y movimiento
reducido. Matriz de treinta principios con evidencia real y límites explícitos.
No equivaler axe o emulación a dispositivos físicos, lector de pantalla real,
teclado virtual o valoración de usabilidad humana. No repetir global E2E
hasta que root reciba el corte SCSS final.

## Matriz de treinta principios (estado inicial)

| Principio | Aplicación y comprobación concreta | Estado |
| --- | --- | --- |
| Atención selectiva | Título, tema guardado y acción principal distinguibles | Pendiente tras CSS |
| Carga cognitiva | Tres preferencias juntas, sin recordar valores de otra ruta | Pendiente |
| Estética-usabilidad | Temas y formulario legibles en cada ancho | RED: pintura clara con tema oscuro |
| Posición en serie | Guardar/restaurar/cancelar con orden visual y teclado estable | Pendiente |
| Tendencia a la meta | Sin porcentajes ni progreso inventado al personalizar | Pendiente revisión |
| Von Restorff | Guardado/error distinguibles por texto y no sólo acento | Pendiente |
| Zeigarnik | Borrador pendiente explicado y salida deliberada | Pendiente |
| Fluir | La personalización no interrumpe ni amplía sesiones | Pendiente recorrido de sesión |
| Fragmentación | Tema, acentos y muestras agrupados semánticamente | Pendiente tras CSS |
| Memoria de trabajo | Borrador conservado ante fallo, snapshot explicado | Pendiente |
| Navaja de Occam | Cada control tiene función y no añade pasos innecesarios | Pendiente |
| Conectividad uniforme | Muestras enlazadas a sus campos, sin relación ficticia | Pendiente |
| Fitts | Áreas táctiles 44x44 y separación medidos físicamente | Pendiente |
| Hick | Una acción principal de guardar, opciones secundarias explícitas | Pendiente |
| Jakob | Radios, colores, campos y navegación con conducta nativa | Pendiente |
| Semejanza | LIGHT/DARK conservan semántica en rutas existentes | Pendiente |
| Miller | Agrupar por tema/acento/muestra, sin recuento arbitrario | Pendiente |
| Parkinson | Tema no altera fin acordado ni duración | Pendiente revisión/recorrido |
| Postel | Texto inválido conservado, errores concretos, sin CSS arbitrario | Pendiente |
| Proximidad | Labels, ayuda y error asociados junto al campo | Pendiente |
| Prägnanz | Estados con texto claro y jerarquía de encabezados | Pendiente |
| Región común | Muestras y formulario como unidades visuales distintas | RED visual previo: falta estilo |
| Tesler | Conversión de tema/contraste a cargo del sistema | Pendiente |
| Modelo mental | Muestra/borrador distintos del tema guardado global | Pendiente |
| Usuario activo | Defaults y estado inicial permiten primer uso comprensible | Pendiente |
| Pareto | Guardar y volver accesibles; ninguna cifra de uso inventada | Pendiente |
| Fin de pico | Confirmación real y recuperación tras incertidumbre | PUT200 observado; recorrido completo pendiente |
| Sesgo cognitivo | Opciones neutrales, sin presión ni impacto en métricas | Pendiente |
| Sobrecarga de opciones | Tres temas, dos acentos y retorno a defaults | Pendiente |
| Doherty | Feedback pendiente medido antes de400ms con respuesta retenida | Pendiente |

La evaluación cognitiva de esta tabla es revisión técnica limitada, no un
estudio de usabilidad con participantes. Dispositivos reales no acreditados.

Nominal repetido tras tokens de B: EXIT0 3f3c9e, 1/1 en2.7s, stack60544
retirado.390 entradas del nuevo corte intactas fb6bde; logs/manifiestos
appearance_ux_nominal_green*.json/log separados del RED original. La recarga
real y pintura oscura sí quedan acreditadas en este segundo recorrido.

Primer test propio preparado: e2e/appearance-ux-audit.spec.mjs. Dos temas con
acentos libres #0000FF/#00FFFF guardados vía API real, formulario y error de
contraste.31 anchos y áreas interactivas medidas; radios usan el label
realmente pulsable, no se exige44px al círculo visual aislado. Axe de cada
estado después de geometría. Sin resultado todavía; B coordina corte TS.

## Primer oráculo geométrico

EXIT1 72fda6, stack69624 retirado. A320px no hay overflow, pero el label
pulsable Claro mide57.86x21px y falla el mínimo44px. Mismo JSON captura
Oscuro/Sistema21px, color pickers50x27, campos189x27 y enlacespreview128.53x21.
347 entradas de producto/script propio idénticas (57377c); se excluyen del
snapshot los tests UI de A que no intervienen en el build de producto.
Evidencia copiada a appearance_ux_red_geometry. Axe y anchos posteriores no
se alcanzaron y permanecen pendientes. B recibe medidas para corregir SCSS;
C no cambia fuentes, fixture ni umbral.

Primer geométrico tras corrección local SCSS: GREEN a05964,124 mediciones
(31 anchos x 2 temas x formulario/error) y4axe sin violaciones.347 inputs
intactos67df7f; stack57112 retirado. Capturas LIGHT-form-320 y DARK-error-1440
revisadas visualmente: campos separados, muestras claras/oscuras diferenciadas,
texto legible y sin recorte visible en esas dos imágenes. No se extrapola a
otros motores ni a todas las rutas. B cierra ahora picker UI; siguiente caso
propio comprobará teclado y feedback con PUT real retenido.

Teclado/feedback: inicialmente GREEN c47fbb,1/1. Tab real desde el campo
oscuro hasta Guardar; control290x45 visible en viewport700, outline3px y
separación4px. Aviso pendiente5.1ms con PUT retenido, tema anterior hasta200,
foco conservado y sólo una escritura.347 inputs iguales42fa2f; stack37512
retirado. No se atribuye ese caso a recuperación412 ni navegación de enlaces.

Texto200: RED371ff8 en primer estado Apariencia LIGHT/320. scrollWidth408;
campos/botones393px y previews361px exceden viewport. Captura revisada confirma
mínimo intrínseco de contenido y título que sale del contorno.347 inputs
iguales8212bc; evidencia copiada appearance_ux_red_text200. B recibe fallo.
No se alcanzaron sesión/historial/semana, tema oscuro ni axe en este caso.

Texto200 corregido: GREEN 380077, 24 medidas y 8 axe completos sin violaciones,
347 inputs iguales (ada021), stack24712 retirado. No cambios del oráculo.
Modalidades originales: EXIT1 c407fc, stack17148 retirado, 347 inputs iguales.
Sistema light→dark y reduced-motion pasan. Forced-colors emulado genera tres
avisos axe de contraste en preview claro: usa colores CSS #23392f/#0000ff,
pero captura forced-colors-1440.png revisada muestra blanco/amarillo sobre negro.
No se atribuye medición numérica de píxeles. Root autoriza omitir únicamente
color-contrast en forced-colors; conserva JSON original y contraste normal
íntegro. Ninguna corrección de producto, nuevo resultado aún pendiente.

Modalidades ajustadas: GREEN fa6b8c, 1/1; seis medidas, dos axe completos
sin violaciones y uno sin regla color-contrast exclusivamente forced-colors.
347 inputs iguales (467a2c). Stack38624 retirado. El EXIT1 c407fc y sus tres
avisos permanecen originales; no se reclasifican. Revisión visual de texto200
LIGHT-Apariencia320: reflow legible, título partido sin recorte, controles y
muestras dentro del viewport. No medición de contraste por píxeles.
Nuevo caso zoom nativo preparado reutilizando extensión efímera del runner19;
pendiente ejecución. 18080 cedido a B para independencia del nominal.

## Matriz revisada sobre evidencia Chromium (corte parcial)

La tabla inicial conserva la situación RED de partida. Esta revisión técnica
se apoya en G=geometría124/4axe a05964; T=texto20024/8axe380077;
K=teclado/feedbackc47fbb; M=modalidadesfa6b8c; N=nominal3f3c9e.
No evalúa aprendizaje con participantes ni generaliza a todas las pantallas.

| Principio | Evidencia y límite concreto |
| --- | --- |
| Atención selectiva | T: título y Guardar distinguibles; secundarios separados. |
| Carga cognitiva | T: tema/acentos/muestras juntos; juicio de estructura, no ensayo humano. |
| Estética-usabilidad | G/T: pintura LIGHT/DARK y reflow; legibilidad revisada en capturas concretas. |
| Posición en serie | K: Tab llega a Guardar y foco se conserva tras200; no todos los órdenes medidos. |
| Tendencia a la meta | K: aviso pendiente honesto, sin porcentaje inventado. |
| Von Restorff | G/K: error y confirmación expresados mediante texto. |
| Zeigarnik | G/T: aviso de muestra aún no guardada; recuperación de borrador se remite a oráculos B. |
| Fluir | T: lector de sesión sigue ofreciendo acciones al cambiar tema; no se ejecuta otro cierre. |
| Fragmentación | T: fieldset Tema y dos muestras con encabezados. |
| Memoria de trabajo | K: valores permanecen durante espera; conflictos e incertidumbre son cobertura B. |
| Navaja de Occam | T: controles nativos y acciones explícitas; juicio técnico limitado. |
| Conectividad uniforme | G/T: muestras claras/oscuras etiquetadas; no estudio de comprensión. |
| Fitts | G/T: cajas44x44 y límites horizontales; dispositivo táctil físico pendiente. |
| Hick | T: Guardar primario, restaurar/cancelar secundarios; no tiempos de decisión humana. |
| Jakob | K: Tab/Enter nativos; G: radios con label pulsable. |
| Semejanza | T: Apariencia/sesión/historial/semana en ambos temas; otras rutas no generalizadas. |
| Miller | T: grupos semánticos, sin regla artificial de siete elementos. |
| Parkinson | T: lectura mantiene el fin original visible; no acredita una nueva prueba de tiempo. |
| Postel | G: blanco inválido conserva texto y muestra error de contraste. |
| Proximidad | G/T: etiqueta y control próximos; axe revisa asociaciones semánticas. |
| Prägnanz | G/T: jerarquía y estados con texto; sin iconos únicos obligatorios. |
| Región común | T: muestras separadas y formulario agrupado; revisión visual. |
| Tesler | M: cambio SYSTEM por preferencia del navegador sin escritura PUT. |
| Modelo mental | K: tema global anterior durante espera, cambia sólo tras200. |
| Usuario activo | N: preferencias iniciales y guardado explícito; no validación con usuarios nuevos. |
| Pareto | T: acciones frecuentes accesibles; no porcentajes de uso medidos. |
| Fin de pico | N/K: confirmación real y recarga; incertidumbre cubierta por B, no este recorrido. |
| Sesgo cognitivo | T: temas neutrales, sin puntuaciones o presión; revisión de copy. |
| Sobrecarga de opciones | T: tres temas y dos acentos; retorno visible a valores conocidos. |
| Doherty | K: feedback5.1ms antes de liberar PUT; no promesa de latencia de red. |

También revisada DARK-session-320 de T: texto ampliado, enlaces y acciones
legibles; no recorte horizontal visible en esa captura. Dispositivos físicos,
teclado virtual, lector de pantalla real y estudios con personas siguen sin
acreditar. Zoom nativo y otros motores todavía pendientes a este corte.

Primer intento zoom35d65b EXIT1 antes de Playwright: el wrapper Windows interpretó
la alternancia del filtro como pipe (explicit no reconocido). Ningún test
se ejecutó;348 inputs iguales, stack47348 retirado. Se corrige sólo invocación:
archivo completo de auditoría más nominal B, sin filtro compuesto. Esto valida
el nuevo zoom y la geometría final tras CSS texto200, además del cleanup.

Corte Chromium final: progress/appearance_ux_chromium_final.log y .exit=0,
EXIT propio d9bbbb;6/6 en47.9s. Stack33508 retirado,348 inputs iguales754bb5.
Incluye mis5 casos más nominal B con cleanup posterior. Mi afterEach comprueba
count0 tras DELETE limitado al owner e2e-user en el stack efímero guardado.
Geometría y texto200 se repiten sobre el CSS final; no suite global histórica.
Zoom nativo: API2, DPR1.5→3, innerWidth320, dimensiones y axe pasan. PNG fullPage
sale recortado por captura/DPR; se conserva y NO acredita inspección visual.
Root autoriza captura viewport CDP fromSurface:false; ciclo captura pendiente,
sin cambio de producto. El log zoom_initial EXIT1 es anterior y no se reemplaza.

Captura zoom corregida únicamente en mecanismo de evidencia: EXIT0 0b30aa,
348 inputs iguales59c992. CDP Page.captureScreenshot fromSurface:false produce
viewport nativo completo; captura stack60724 revisada, navegación/título/radios
legibles sin recorte. No se extrapola esa imagen al contenido fuera del viewport;
geometría de todos los controles y axe se conservan en el mismo caso.
Script congelado04CC8D…13C81; rutas de evidencia ahora nombran browserType real
para distinguir Firefox/WebKit, sin alterar oráculos. Se ejecutan los cuatro
casos no nativos por motor; el zoom queda explícitamente Chromium.

Firefox final4/4 GREEN c780df (44.5s),348 inputs iguales; stack5744 retirado.
Mismos oráculos de geometría/texto/teclado/medios; feedback17ms. No zoom nativo
Firefox ni dispositivo físico acreditados. Root publicó script final ed00ad4;
no hay cambios posteriores de fuentes. WebKit ejecuta último foco pendiente.

## Freeze final de auditoría

WebKit4/4 GREEN64eacf (40.7s),348 inputs iguales414575; stack51444 retirado.
Teclado hasta Guardar también pasa aquí; no se extrapola a enlaces históricos.
Feedback final: Chromium4.5ms, Firefox17ms, WebKit8ms. Por motor124 medidas de
formulario/error,24 de texto200 y6 de medios;15axe (14 completos,1 sincontraste
sólo forced-colors). Zoom nativo60724 añade1axe completo. Todos cero dentro
alcance declarado. Matriz anterior se extiende a estos tres motores en esas
mediciones, conservando límites cognitivos y de dispositivos reales.
Informe final review_appearance_ux_audit.md. No nuevos hallazgos, fuentes quietas,
18080 libre. Root coordina CI integrado; C no ejecuta global redundante.
