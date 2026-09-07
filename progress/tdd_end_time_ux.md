# UX17 — primer recorrido en ejecución

Se conserva el patrón de medición del runner16: 31 anchos de 320 a 2560 px, bordes de breakpoints y altura400 a768px; controles44px, solapes, overflow, capturas viewport/fullPage y axe con WCAG2.2AA y best-practice (incluye heading-order). No se atribuye todavía resultado de esas mediciones.

Caso único propio end-time-notification-ux.spec.mjs: formulario desde tarea, navegación Tab y Enter, POST real201 cuya entrega se retiene/perderá, feedback medido antes400ms, incertidumbre, recuperación por K, confirmación y formulario del lector URL16. No altera fechas SQL ni simula éxito. Snapshot nominal B d795f6, backend c2882fd. Texto200 y zoom nativo quedan sin ejecutar; no se certifican30principios por axe.

Se reutiliza por copia local la función de inspección16 sin modificar su archivo histórico. Sus opciones de texto/zoom todavía no se invocan en este primer caso; se revisará el resultado antes de ampliar casos. Run90038, log end_ux_initial.log.

Primer intento b8177f EXIT1 antes de geometría: expectativa impropia de autofocus del input al abrir. El iniciador Ampliar tiempo permanece, y s43 exige conservar foco en el iniciador; se corrige el test para verificarlo y alcanzar input con Tab real. No se modifica producción ni se presenta este fallo como defecto. Runner retirado, se espera COPY final controlado de B antes de ejecutar de nuevo.

## Matriz final del primer caso

COPY final fa051b recibido antes del runner60491. Resultado bd1fd1 EXIT0: 1/1 (50,1s), end_ux_final_initial.log. 155 medidas (31 por cada uno de cinco estados), cinco axe0 incluyendo heading-order, feedback1,7ms. Inputs antes/después iguales (0bcf73). Capturas propias .e2e-work/end-time-real/chromium/ux; inspección visual Reader320 confirma lectura de ambas decisiones y jerarquía. No se acredita aviso vencido dentro de esta matriz: está probado funcionalmente en el caso real1min, no medido geométricamente aquí.

Se extrae ahora el recorrido en función local para reutilizarlo en un segundo test individual de texto200, sin editar helpers históricos. El primer caso conserva todos sus oráculos. Texto se duplica desde estilos computados y se comprueba factor2; tres anchos320/768altura400/1440, archivos separados text200.

## Texto200 y zoom real

Texto200 ac5e71 EXIT0: caso propio7,8s, 15medidas/5axe0, feedback1,7ms. El filtro con espacios se interpretó como «text» en Windows y ejecutó también dos tests históricos verdes (3/3 total14,2s); no se atribuyen a17. Se conserva end_ux_text_initial.log y se usan tokens sin espacios en comandos posteriores.

Zoom nativo b3c2e8 EXIT0,1/1 en10s. Extensión aislada usa chrome.tabs.setZoom/getZoom2; DPR1,5→3 y viewport320CSS. Cinco estados, cinco axe0; feedback1,9ms. Evidencia .e2e-work/end-time-native/organizationweb-e2e-69292/evidence, incluye zoom.json y capturas. No equivale a un viewport simulado. Contexto de navegador y stack cerrados por su lifecycle. Log end_ux_native_initial.log.

Root inspeccionó dos capturas (13497e/d2634f): Reader320 y formulario tarea1440, legibles sin recortes apreciables. Skiplink fullPage1440 es artefacto documentado: focusedfalse, top-100,bottom-55,scrollY1535; no visible en viewport ni defecto. Esta revisión visual se limita a esas capturas, no a todos los dispositivos.

## Matriz de30 principios: resultado acotado

«Parcial» combina oráculos automáticos y revisión de capturas; nunca certifica facilidad de uso humana. Los límites de cada fila permanecen visibles.

| Principio              | Evidencia17 y límite                                                                                                       |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Atención selectiva     | Parcial: jerarquía de las dos capturas revisadas; aviso funcional1min, sin captura geométrica de aviso aún.                |
| Carga cognitiva        | Parcial: cantidad vacía, fin original/acordado visibles en ambos recorridos; estudio con usuarios pendiente.               |
| Estética-usabilidad    | Parcial: lectura visual Reader320/tarea1440 y axe; preferencia estética no medida.                                         |
| Posición en serie      | Verificado en recorrido: Tab botón→cantidad→confirmar, Enter y recuperación; no todos los dispositivos físicos.            |
| Tendencia a la meta    | Verificado: EXTEND no añade intervalos/neto; SQL y recibo nominal.                                                         |
| Von Restorff           | Parcial: incertidumbre/confirmación por texto y semántica; aviso sólo comprobado funcionalmente.                           |
| Zeigarnik              | Verificado: ACK perdido, advertencia de salida y recuperación K/recarga; no estudio de presión percibida.                  |
| Fluir                  | Verificado en plazo real: aviso no mueve foco ni abre modal; extensión deliberada lo retira.                               |
| Fragmentación          | Parcial: panel, formulario y recibo distinguibles en capturas; prueba humana pendiente.                                    |
| Memoria de trabajo     | Parcial: cantidad conserva intención durante incertidumbre;412 no recorrido en estos E2E.                                  |
| Navaja de Occam        | Parcial: acciones de ampliar/cerrar/consultar/comprobar en capturas, sin controles extra.                                  |
| Conectividad uniforme  | Verificado: enlace desde tarea llega a URL sesión y fin durable.                                                           |
| Fitts                  | Verificado en155medidas base,15texto y5zoom: controles≥44px y sin solapes.                                                 |
| Hick                   | Verificado: cantidad se revela vacía al abrir; POST sólo tras confirmación.                                                |
| Jakob                  | Verificado: input/formularios/enlaces nativos y teclado real; teclado virtual físico pendiente.                            |
| Semejanza              | Parcial: misma ampliación en tarea/lector y presentación revisada.                                                         |
| Miller                 | Parcial: agrupación visible de contexto/decisión/recibo; capacidad de memoria no inferida.                                 |
| Parkinson              | Verificado: vencimiento no escribe, sólo confirmación crea EXTEND.                                                         |
| Postel                 | Parcial: límite1440 usado realmente; presentación de cantidad inválida no medida aquí.                                     |
| Proximidad             | Parcial: label/control próximos y sin recorte; error de validación pendiente de geometría.                                 |
| Prägnanz               | Parcial: incertidumbre explícita y paused probado por API; Reader no exige texto paused, cierre externo no recorrido aquí. |
| Región común           | Verificado: panel con formulario separado de cierre en Reader, axe/DOM del recorrido.                                      |
| Tesler                 | Parcial: fin original y acordado legibles; no se atribuye tratamiento de zonas históricas a este UTC real.                 |
| Modelo mental          | Verificado: EXTEND conserva estado, pausa posterior conserva fin y recibo histórico.                                       |
| Usuario activo         | Parcial: primera ampliación recorrida sin manual; estudio con usuarios pendiente.                                          |
| Pareto                 | Parcial: acciones directas y recuperación accesible; sin porcentajes inventados.                                           |
| Fin de pico            | Verificado: no éxito al perderACK; sólo K válido confirma.                                                                 |
| Sesgo cognitivo        | Parcial: pausa mantiene fin elegido, sin penalización añadida; percepción humana pendiente.                                |
| Sobrecarga de opciones | Parcial: revelación progresiva, sin alarmas externas ni personalización adicional.                                         |
| Doherty                | Verificado: feedback1,7ms base/texto y1,9ms zoom, con respuesta real retenida.                                             |

Firefox/WebKit se ejecutan a continuación sobre tests existentes. No se han probado dispositivos móviles físicos, teclado virtual ni lectores de pantalla reales. No se acredita totalidad del contrato sólo por esta matriz.

## Motores y freeze

Firefox c186be EXIT0: cinco casos existentes verdes en2,4min; WebKit5e40d3 EXIT0: los mismos cinco verdes en2,2min. Incluyen nominal, vencimiento real1min, recuperación+pausa+recarga, matriz y texto200. Se usó --browser del Playwright instalado, sin cambiar config ni adaptar oráculos; nativeZoom200 se excluyó porque lanza Chromium explícito. Logs end_e2e_firefox.log / end_e2e_webkit.log. Ambos stacks retirados,18080 libre.

Consolidación progress/end_ux_results.json:515medidas y35axe0 (155base+15texto por cada motor;5zoomChromium). Comprobación4b243d:343inputs del corte final sin diferencias; único crecimiento posterior del test UX por sus ciclos. Prettier de ambos archivos E2E GREEN. No nuevas fuentes productivas, campañas, limpieza global ni Git. El test funcional inicial permanece congelado; los motores sí repitieron sus tres casos sobre COPYfinal y los acreditan en ese corte.

Freeze para revisión: nuevo e2e/end-time-notification-ux.spec.mjs, progress/tdd_end_time_ux.md y resultados/manifiestos propios. Evidencia local conserva las capturas y JSON por motor/modo. No se declara feature17 done ni se sustituye gate integrado. Pendientes diferenciados: geometría del aviso vencido y del error de cantidad no medidos en este paquete; comportamientos de aviso sí tienen E2E real1min. La fila de motores anterior queda satisfecha en el alcance descrito; dispositivos y lectores reales siguen pendientes.
