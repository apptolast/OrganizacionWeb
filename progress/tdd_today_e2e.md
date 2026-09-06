# Hoy: integración E2E y revisión UX

Contrato a127747, feature12 en curso. Ponytail full y Caveman lite. Autoría limitada a e2e y este documento; API e interfaz pertenecen a otros autores. No se ejecuta init concurrente ni mutación.

## Corte preparado

Primer ciclo: `today.spec.mjs`, «root reads a real empty UTC fallback without writing preferences», vinculado a @s1/@s3/@s19/@s21 y la entrada raíz de @s32. Usa sesión, API y PostgreSQL reales mediante el runner aislado existente. Comprueba ausencia confirmada, nulls de capacidad, orientación visual y ausencia de escrituras. Todavía NO ejecutado: no se declara RED ni GREEN. Se espera un corte ejecutable de API/interfaz antes del runner Docker.

Migración mecánica de entradas de captura en create-project, read-projects y authentication: `/` pasa a `/proyectos/nuevo`; la expectativa del enlace de creación se actualiza a esa ruta. Se conservan las aserciones de creación, persistencia, seguridad, teclado, foco, axe y responsive. La raíz nueva se verifica en el smoke de Hoy. Estos recorridos existentes aún no se han repetido tras el cambio.

## Secuencia pendiente

Después del primer ciclo: agenda con nombres, resúmenes y enlace de tarea; navegación a captura desde Hoy y retorno; logout con retirada de datos. Un test nuevo por ciclo. Coordinar snapshot de producción con los autores antes de cada build del stack aislado; no tocar servicios ajenos ni rutas protegidas.

La revisión @s38 cubrirá las treinta filas de docs/ux-requirements.md con evidencias concretas, anchos y bordes de breakpoints, alturas reducidas, teclado, controles, axe, feedback y zoom nativo. Chromium, Firefox y WebKit se registrarán por separado. Por ahora toda evidencia de Hoy está pendiente: pruebas previas de otras features no certifican esta pantalla. Dispositivos físicos, teclado virtual, lector real y evaluación humana no se sustituyen con emulación.

## Matriz UX de Hoy: criterios y estado actual

Cada fila distingue la comprobación de interfaz de una certificación de facilidad de uso. Ninguna se declara verificada antes de ejecutar el recorrido.

| Principio | Comprobación concreta de Hoy | Estado |
| --- | --- | --- |
| Atención selectiva | Hoy, horario actual y próximo inicio reconocibles también con aviso de error. | Capturas9324 y44224 inspeccionadas; evaluación humana pendiente. |
| Carga cognitiva | Nombres, objetivos, horarios y capacidad consultables sin memorizar otra pantalla. | Nombres/objetivo/contexto y enlace comprobados por smoke; comprensión humana pendiente. |
| Estética-usabilidad | Legibilidad de agenda larga, vacío y error entre anchos. | Capturas nominales y zoom inspeccionadas; bounds de cinco estados por motor verificados. |
| Posición en serie | Orden de resumen, agenda y Actualizar estable; teclado coherente. | Orden y conservación de foco tras error verificados en los tres motores; RED previo corregido. |
| Tendencia a la meta | Ningún porcentaje de avance ni crédito de trabajo por reservas. | Smoke confirma ausencia de cambio de tarea/outbox; sin métrica de trabajo ficticia visible. |
| Von Restorff | Horario actual, próximo y fallo se distinguen mediante texto además de color. | Etiqueta de horario actual verificada; diferenciación semántica y visual inspeccionada. |
| Zeigarnik | Salir y regresar recupera reservas persistidas sin presión para continuar. | Smokes vuelven a datos persistidos; texto sin presión inspeccionado. |
| Fluir | Fin previsto visible; consulta y actualización no inician una sesión de trabajo. | Fin real visible y consulta sin efectos verificados; sesiones fuera del alcance. |
| Fragmentación | Resumen y cada bloque agrupan contexto real de proyecto y tarea. | Agrupación resumen/reserva inspeccionada en9324 y44224. |
| Memoria de trabajo | Datos confirmados y marca temporal se conservan ante fallo dentro del día. | WebKit conserva agenda tras503; recuperación y vacío completan el flujo. |
| Navaja de Occam | Actualizar y enlaces existentes bastan; no formularios de planificación en Hoy. | Inventario visible limitado a lectura/actualización/enlaces; capturas revisadas. |
| Conectividad uniforme | Enlace de tarea corresponde al bloque mostrado. | Enlace del bloque abre su tarea real: smoke de agenda en tres motores. |
| Fitts | Acciones principales de al menos44×44 px y sin solapes. | 44px medidos en155 estados/anchos por motor y zoom nativo; dispositivos reales pendientes. |
| Hick | Entrada útil con agenda o guía vacía, sin pasos previos obligatorios. | Vacío y agenda útiles sin configuración obligatoria: smokes reales. |
| Jakob | Enlaces nativos, foco visible, retorno y acceso en rutas antiguas. | Navegación, retorno y foco tras Actualizar verificados en los tres motores. |
| Semejanza | Navegación y estados siguen componentes existentes. | Coherencia visual inspeccionada; no certifica todas las pantallas del producto. |
| Miller | Agrupación por bloque/contexto, sin limitar arbitrariamente número de reservas. | Textos largos verificados; agenda numerosa corresponde a API/UI, no este fixture de una reserva. |
| Parkinson | Hora de cierre es fin real; no se extiende ni acredita actividad. | Cierre real y ausencia de extensión/escritura verificados en lectura nominal. |
| Postel | Unicode y zonas históricas se conservan con esquema válido. | Unicode literal y nombre largo verificados; fallback de Intl corresponde a pruebas UI. |
| Proximidad | Avisos de carga/error y motivo del fallback están junto al contexto afectado. | Avisos en main y axe de cinco estados por motor sin infracciones. |
| Prägnanz | Vacío, fallo y capacidad desconocida tienen explicaciones distintas. | Vacío, fallo retenido y capacidad desconocida diferenciados en smokes/UX. |
| Región común | Cada reserva agrupa nombres, objetivo y horario en orden comprensible. | Tarjeta y resumen reflejan agrupación real: capturas inspeccionadas. |
| Tesler | Zona explícita y fallback UTC explicado sin errores internos. | UTC explícito y capacidad desconocida comprobados; sin errores internos en pantalla. |
| Modelo mental | Texto distingue reserva planificada de tarea y trabajo realizado. | Textos de planificación y tarea sin cambio automático comprobados. |
| Usuario activo | Vacío ofrece Proyectos; fallback ofrece Disponibilidad. | Primer smoke verifica ambos enlaces orientadores y ausencia de formulario en raíz. |
| Pareto | Consulta de agenda prioritaria; captura sigue accesible por nueva ruta. | Consulta/captura accesibles en smoke; prioridad de uso requiere evaluación humana. |
| Fin de pico | Actualización confirma resultado cierto; fallo mantiene opción de recuperación. | Confirmación, error recuperable y foco conservado verificados en los tres motores. |
| Sesgo cognitivo | Capacidad null no se muestra como cero; ausencia de presión o métricas ficticias. | Capacidad null distinta de cero y reserva distinta de trabajo verificadas. |
| Sobrecarga de opciones | Preferencias permanecen en Disponibilidad y vuelven a Hoy mediante navegación. | Enlace a Disponibilidad verificado; no añade personalización a Hoy. |
| Doherty | Carga/actualización visible antes de400 ms, sin fingir que terminó el GET. | Feedback final Chromium2,9ms/Firefox6ms/WebKit4ms; respuesta retenida, no latencia del servidor. |

Validación del corte preparado: `node --check e2e/today.spec.mjs` y `git diff --check` focal EXIT0, salida aac7eb. Son verificaciones de sintaxis/diff, no pruebas E2E ejecutadas.

## Primer ciclo real

`node scripts/e2e.mjs --grep=today`, stack propio organizationweb-e2e-60012, sesión63352. COPY backend#26 y frontend#27 DONE0c132c; ambos autores recibieron liberación de freeze inmediatamente. Build Docker backend/frontend GREEN. Test fallback1/1 PASS (2,4s; total4,6s), EXIT0b0f806. Es refuerzo de integración inicialmente GREEN sobre implementación existente; no se atribuye RED artificial. Runner retiró únicamente su stack y volumen. No interceptaciones de API ni reloj simulado en este ciclo.

Este resultado acredita ausencia confirmada, nulls/UTC, orientación vacía y ausencia de escrituras del smoke. No acredita todavía agenda con reservas, privacidad/logout, captura, matriz visual ni recuperación temporal. Root detectó después un caso de fallo en frontera intra-día que necesita conservar expiración al dayEnd; autor UI lo corregirá antes del próximo snapshot. El smoke vacío pasado no lo cubría ni se presenta como validación de ese comportamiento.

## Agenda real después del fix temporal

El intento69052 construyó correctamente el corte frontend294fcd/backend formateado, pero el filtro con espacios no llegó intacto al subprocess pnpm de Windows: EXIT1 «No tests found»4486db. No ejecutó tests y no es un RED de aplicación. Se corrigió sólo la invocación (`--grep=^today.*persisted`), sin modificar runner ni filtros de la suite. Docker reutilizó las capas ya construidas.

Stack9324/sesión44280: agenda1/1 PASS71817d (3,1s; total6,1s), inicialmente GREEN. La fixture crea preferencia, proyecto, tarea y reserva mediante API real; desplaza únicamente el horario de esa reserva persistida alrededor del serverNow mediante SQL. No altera el reloj de la aplicación ni intercepta respuestas. Verifica intersección y resumen, current/next/cierre, nombres literales, objetivo, enlace a tarea y ausencia de cambios de estado/outbox al consultar.

Capturas reales conservadas fuera del directorio efímero del runner: `.e2e-work/today-real/9324/today-agenda-320.png` y `today-agenda-1440.png`. Inspección de320: navegación3 opciones visible, foco inicial en encabezado, resumen y tarjeta sin recorte; no acredita todavía matriz completa de anchos ni contraste automático. Tercer ciclo preparado: captura desde Proyectos y vuelta a Hoy, logout retira nombres/enlaces privados, API devuelve401 y login hace una lectura nueva. Pendiente ejecución; se espera freeze tras revisión de otra frontera temporal.

## Captura y privacidad confirmadas

Sobre freeze7cf936 y hash actualizado (13 scripts GREEN38e58c), stack12568 falló dos tests por una corrupción de encoding introducida al extraer la fixture con Python sin especificar UTF-8: selectores «Presupuesto del día» y «Cerrar sesión» quedaron mojibake. Error933167, no defecto del producto; las capturas DOM mostraban textos correctos. Se corrigieron únicamente cinco cadenas y se retiró esa lectura implícita. Repetición stack48440/sesión85954:3/3 PASS9b00ec,11,7s. El tercero acredita captura por `/proyectos/nuevo`, vuelta a Hoy, retirada de datos tras logout204, rechazo401 de lectura anónima y lectura nueva tras login. La retirada antes de recibir204 pertenece a las pruebas UI, no se atribuye a este test.

## UX: fallo de foco reproducido y conservación del oráculo

El cuarto ciclo prepara31 anchos (320–2560 y ambos lados de360/420/600/700/1000/1100/1600), altura400 a768, cinco estados, controles44px, axe, texto200% y feedback de teclado con respuesta real retenida y503 deliberado. Primer intento23376 falló preparando una fixture cuyo nombre superaba120 caracteres, restricción real `projects_name_check`30a9d9. Fixture corregida a tres repeticiones dentro del límite; no se atribuye ese fallo a la UI.

Stack68584: RED474dc0 en conservación de foco tras error. Las medidas de31 anchos y axe de carga/agenda/actualización/error pasaron antes del fallo; vacío/texto200% todavía no se ejecutaron y no se declaran cubiertos. Instrumentación temporal stack42636 confirmó `activeElement.tagName=BODY` inmediatamente después de Enter y del anuncio «Actualizando Hoy…», antes de resize/axe; tras503 seguía BODY antes de inspeccionar error (cc49ad). El botón Actualizar permanece visible pero disabled durante la espera. Final EXIT1 3e75c6 confirma el mismo oráculo. Se retiraron ambos logs temporales y node --check pasó149a36; la expectativa de foco permanece intacta. Root recibió reproducción y coordina corrección después de la campaña de mutación en curso, sin cambiar fuentes durante ella.

Las once migraciones de rutas fueron revisadas y recogidas en commitf568f6e, NO eran once tests ejecutados. La atribución informal de GREEN por otro agente se corrigió antes de esta bitácora. Regresión real de create-project/read-projects/authentication iniciada ahora en stack58752/sesión7851; pendiente resultado. No se omiten esas suites por una revisión documental.

Regresión histórica final: `node scripts/e2e.mjs e2e/create-project.spec.mjs e2e/read-projects.spec.mjs e2e/authentication.spec.mjs`, **19/19 PASS**,57,2s, EXIT0c437d1. Incluye creación/persistencia/reinicio, límites/Unicode, autenticación/CSRF/logout, lista/detalle/paginación y accesibilidad/teclado/táctil previos. Esta es la evidencia de ejecución de las rutas migradas; el commit no lo era. Stack58752 retirado por su runner. Se inicia Firefox para los tres smokes de Hoy; la prueba UX con foco conocido permanece roja y se informa separadamente.

## Motores y zoom nativo

Firefox26860: tres smokes3/3 PASS332deb,11,7s. WebKit28032: tres smokes PASS y UX FAIL sólo por foco, c1dcb0/EXIT1bb03f9,25,2s totales. Se cambió únicamente esa aserción a `expect.soft`, autorizado por root: conserva el fallo y el EXIT1, permite terminar el resto de medidas y guardar evidencia. WebKit completó155 combinaciones (31 anchos por cinco estados), cinco axe sin infracciones, altura400, controles44px, texto200% tanto agenda larga como vacío, feedback9ms. JSON/capturas/error-context preservados en `.e2e-work/today-real/28032-webkit/`. No es un resultado global verde.

`today-native-zoom.spec.mjs`:1/1 PASS c492dc (4,8s; total6,8s), stack44224. Reutiliza fixture API/PG mediante `support/today.mjs`, con nombre largo dentro de120 caracteres. Chromium con perfil propio y extensión local mínima usa chrome.tabs.setZoom(2), sin modificar perfiles del usuario ni usar CSS zoom. Baseline: ventana1440, inner1426, DPR1,5. Al200%: misma ventana1440, inner713, DPR3. Ventana654: inner320, client312 y scroll312. Controles44px y axe sin infracciones sobre agenda larga. Captura completa nativa inspeccionada sin recorte; datos y PNG en `.e2e-work/today-real/44224-native/`. Perfil aislado conservado en `.e2e-work/today-native-zoom/organizationweb-e2e-44224/`; no se limpia ni se atribuye limpieza global. Esta prueba no activa actualización y no acredita resolver su fallo de foco.

Firefox UX10444: EXIT1 4969f8 exclusivamente por la misma conservación de foco, con la expectativa soft todavía fallando. Se completaron155 medidas, cinco axe, textos200% y feedback3ms; JSON, capturas y error en `.e2e-work/today-real/10444-firefox/`. WebKit y Firefox acreditan esos cálculos de layout y feedback, pero ambos recorridos permanecen rojos hasta corregir foco. Chromium tiene cuatro estados medidos antes de su aserción dura anterior; no se extrapola el quinto estado desde otro motor. Diff --check focal EXIT0ce4756. No quedan logs temporales en E2E.

Pendiente técnico acotado: autor UI corrige foco después de conservar resultado de la mutación original; actualizar hash/manifest mediante coordinación, repetir UX en los tres motores y confirmar smokes tras extracción compartida. Dispositivos físicos, teclado virtual, lector real y facilidad de uso humana siguen fuera de esta evidencia automatizada. Rutas históricas19/19 y zoom nativo no requieren repetirse por cambios exclusivos de foco sin efecto en sus recorridos, salvo hallazgo de revisión.

## Verificación del fix de foco

Autor UI cambió únicamente disabled por aria-disabled en Actualizar, conservando la guarda de petición vigente. Entrega111 API/UI GREENeeb6ae, lint/build/types9f4c89; este autor sólo actualizó hashToday y verificó13 scripts GREENadd52f. El RED E2E previo se conserva arriba. No cambió geometría/CSS ni se debilitó la aserción de foco.

Chromium18396: cuatro tests de `e2e/today.spec.mjs` **4/4 PASS6b8014**,16,2s. UX155 combinaciones/axe5 y feedback2,9ms; el mismo oráculo de foco pasa. Captura de error320 inspeccionada: outline visible de Actualizar, aviso junto a la acción y agenda conservada, sin recorte. Artefactos en `.e2e-work/today-real/18396-chromium-final/`.

Firefox24900: **4/4 PASS862700**,19,7s; UX155/axe5, feedback6ms. Artefactos en `.e2e-work/today-real/24900-firefox-final/`. Ambos resultados incluyen smokes después de extraer la fixture compartida. Se espera resultado final WebKit antes de declarar completa la matriz técnica en tres motores.

WebKit51348: **4/4 PASSd7349f**,16,7s; UX155/axe5, feedback4ms. Artefactos finales en `.e2e-work/today-real/51348-webkit-final/`. Resultado final:12 ejecuciones verdes (cuatro tests en tres motores), sin eliminar el historial de fallos previos. Foco corregido; la matriz actual ya refleja esta verificación. Se conservan además19/19 históricos y1/1 zoom nativo como mediciones separadas, sin convertirlos en pruebas de otros escenarios. Todos los stacks fueron retirados exclusivamente por sus runners; no se limpian perfiles ni rutas protegidas.
