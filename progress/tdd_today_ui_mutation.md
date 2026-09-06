# Seguimiento TDD de mutación Hoy — UI/App/Workspace

Autoría acotada a today.test.tsx; producción intacta. Checkpoint5fe9afc, inventario original521/418K/102S/1NC preservado. Root autorizó68 entradas (42UI+6App+20Workspace), API35 pertenece a otro autor. Ponytail full/Caveman lite. Init15059 previo1320GREEN; no init concurrente, sin replay ni configuración. Un comportamiento por ciclo; si implementación existente pasa inicialmente se registra GREEN sin fabricar RED.

## Ciclo1 — exceso conocido y capacidad desconocida

@s18/@s19: matriz presupuesto30 con1800s de exceso frente fallback null. Aserción del dd concreto en minutos/Desconocido y ausencia del aviso fallback con disponibilidad válida. Cubre candidatos465–470 y443/445; ejecución focal pendiente.

Ciclo1 inicialmente GREEN2 eee9c5. Sin cambio de producción; no atribuye muertes sin medición.

## Ciclo2 — identificación y lectura de cada reserva

@s18: dos reservas sin solape, actual/próxima dentro de su propio item y ausentes en el otro, intervalo inicio—fin legible y sin duplicar zona original cuando coincide. Candidatos479/481/484/486/487/491. Intento809e31 usó cwd frontend con rutas de repo: no escribió archivos y seleccionó cero casos; no se cuenta como test ni RED. Ejecución corregida abajo.

Ciclo2 inicialmente GREEN1 dda7e8.

## Ciclo3 — fallo/finally de generación sustituida

@s30: GET manual pendiente sustituido por frontera, respuesta503 antigua entregada mientras nueva lectura sigue pendiente. No altera alerta/estado Actualizando ni libera coalescing; nueva respuesta sí confirma agenda. Candidatos309/315,331/337/397 según flujo. Ejecución focal abajo.

Ciclo3 inicialmente GREEN1 c866ef.

## Ciclo4 — respuesta recibida mientras permanece oculta

@s26: primer GET confirma snapshot oculto; cero timers y cero refetch al cruzar frontera. Sólo volver visible inicia consulta. Candidatos378/380. Sin temporizadores artificiales ni producción.

Ciclo4 inicialmente GREEN1 d7ae73.

## Ciclo5 — reintento limpio y cierre vacío explícito

@s22: tras fallo inicial, status no anuncia éxito; al reintentar desaparece alerta antigua mientras consulta pendiente. Confirmación vacía conserva Sin bloques en cierre. Candidatos292/347/426/471.

Ciclo5 inicialmente GREEN1 8a9906.

## Ciclo6 — ruta desconocida y límites de coincidencia

@s35: prefijo/sufijo extra de editor/lector conserva404, enlaces de recuperación y ningún GET. Main programáticamente enfocable con tabindex-1; fuera del espacio/proyectos no activa nav y breadcrumb explicita Página no encontrada. CandidatosApp18/28/29/32/39 yWorkspace536. El caso bajo /proyectos conserva sección de espacio histórica, sin imponer cambio contractual a producción.

Ciclo6 inicialmente GREEN4 28d537.

## Ciclo7 — fronteras legibles de texto

@s18/@s19: fecha/hora separada de zona y explicación fallback separada del enlace de acción. No exige espacios junto a emdash ya existente; sólo evita concatenar palabras/tokens. Candidatos286/452.

Ciclo7 inicialmente GREEN1 555516.

## Ciclo8 — enlaces404 distinguibles

@s35: texto de recuperación mantiene separación entre Hoy y Proyectos, además de los roles/hrefs del ciclo6. CandidatoApp40; no inspecciona nodos internos.

Ciclo8 inicialmente GREEN1 105011.

## Ciclo9 — liberar suscripciones al salir

@s30: captura suscripciones realmente registradas a visibility/focus y exige su liberación al desmontar. No fija conteo de hooks ni forma de callbacks; evita fuga retenida aunque React ignore actualizaciones desmontadas. Candidatos366/368/370.

Ciclo9 inicialmente GREEN1 678d81.

## Ciclo10 — deadline fraccional de plataforma

@s28: reloj monotónico añade0,5ms entre recepción y programación; timeout convierte delay a entero como plataforma. La agenda se retira al callback del deadline aunque el reloj esté menos de1ms antes y nueva lectura siga pendiente. Dos entradas: frontera normal y reaparición visible. Candidatos399/412; el test no llama refresh ni accede a refs. No impone latencia del backend ni altera el reloj de servidor.

Ciclo10 inicialmente GREEN2 6179e9.

## Ciclo11 — igualdad exacta del deadline al volver visible

@s28: timer cancelado por ocultación, GET manual pendiente; reaparece exactamente al deadline, antes de ejecutar otro timer. Exige abortar consulta antigua y retirar agenda inmediatamente. Candidato329. Evita que avance1001ms deje sin observar el borde >=.

Ciclo11 inicialmente GREEN1 151c76. Los once ciclos añaden16 casos a los40 anteriores; no se afirma un RED de producto ni muertes nuevas sin replay.

## Entrega congelada y mapa68

Formato focal+56/56 GREEN49fb9f. Typecheck102b0c detectó únicamente incompatibilidad entre overload DOM(number) y Node(Timeout) del adaptador de setTimeout en la prueba; no defecto de producto. Se tipó explícitamente el retorno del mismo timer sin cambiar su ejecución. Formato y56/56 GREEN b7ba6a; tsc y ESLint del archivo EXIT0 ae1ea7. Diff-check76b7d4 limpio. Fuentes y configuración intactas; sólo today.test.tsx y esta bitácora durante autoría.

| Ciclo | IDs originales NE atendidos | Oráculo |
| --- | --- | --- |
| 1 | 443,445,465,466,467,468,469,470 | Exceso en minutos/null; no fallback con disponibilidad válida |
| 2 | 479,481,484,486,491 | Actual/próximo dentro del item correcto y sin duplicar zona |
| 3 | 309,315,331,337,397 | Aborto en frontera; antiguo503/finally no altera lectura vigente |
| 4 | 378,380 | Respuesta oculta no programa refresh |
| 5 | 292,347,426,471 | Alerta/status del reintento y cierre vacío explícito |
| 6 | App18,28,29,32,39; Workspace536 | Anchors404, ausencia GET/nav activa, foco/breadcrumb |
| 7 | 286,452 | Separación legible hora/zona y explicación/acción |
| 8 | App40 | Enlaces404 no concatenados |
| 9 | 366,368,370 | Liberación real de suscripciones |
| 10 | 399,412 | Deadline fraccional con GET pendiente |
| 11 | 329 | Reaparición exactamente al deadline |

Son39 entradas observables atendidas mediante16 casos, no39 pruebas espejo. El ciclo2 comprueba también lectura de intervalos, pero no necesita matar487: la raya ya mantiene una frontera legible aunque desaparezca el espacio siguiente.

Las29 candidatas E quedan justificadas individualmente por juez independiente en [review_today_mutation_candidates.md](review_today_mutation_candidates.md): UI291,336,338,350,351,371,411,435,487,493; Workspace506–511,517,518–523,529–534. Sus razones distinguen guards redundantes bajo flujo actual, delimitadores que permanecen y dots decorativos con aria-current/borde/fondo/breadcrumb independientes. Esta autoría no las autoaprueba ni altera el catálogo o score; root conserva la decisión final. No se escriben pruebas para fijar un árbol interno decorativo.

Pendiente judge de cambios y, sólo después de autorización, medición separada. El reporte original521 y sus418K permanecen intactos; esta entrega no declara feature done ni ajusta el score original80,23%.

## Ciclo12 — precisión del juez sobre292

El juez señaló que ciclo5 esperaba el rechazo inicial, cuando failure ya vale true en ambas versiones. Se corrige la atribución:292 queda atendido reforzando la prueba existente @s21 `loads then confirms the empty day` con ausencia de alerta **antes** de resolver su GET deferred. No añade caso espejo ni modifica producción. Inicialmente GREEN1 2f0a3a; formato focal y regresión56/56 GREEN98844f, ESLint/tsc/diff-check sin errores al cierre de la sesión3855. Ciclo5 conserva347/426/471; mapa total sigue39 observables y29E,68 entradas.

Root ratificó las29E UI/shared del informe independiente (mensaje de cierre:37dbdbf5/6b537e). Se mantiene cada identidad y razón en review_today_mutation_candidates.md; aprobación contractual no equivale a identidad visual universal de dots/espacios. Los39NE son objetivos cubiertos por oráculos, **no kills medidos**. Sin replay ejecutado por esta autoría. Freeze renovado tras este único refuerzo.
