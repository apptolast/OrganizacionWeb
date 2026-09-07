# E2E y UX de revisión semanal

## Preparación inicial (antes de ejecutar)

`e2e/weekly-review.spec.mjs` contenía un único caso de navegación Principal → Revisión semanal, GET real 200/no-store, siete días vacíos UTC sin disponibilidad, presupuesto desconocido y ausencia de escritura outbox (@s1/@s27). En ese corte no había respuestas interceptadas ni servidor arrancado. `node --check` y formato del archivo pasan (62c0aa); esto no acredita un recorrido ejecutado ni un RED.

La primera ejecución esperó integración del backend revisado y cesión del puerto 18080; el resultado se registra abajo. No se usa el stack 8080 ni se limpia ningún ancestro de evidencia.

## Ejecuciones individuales

Primer caso ejecutado sobre backend integrado `1301fa8`: inicialmente GREEN, 1/1 (5,1 s de suite), EXIT0 `17ea2a`. Log `weekly_review_e2e_nominal.log`; runner aislado `organizationweb-e2e-63444` retirado completamente. Navegación y datos proceden del backend real. No cambio productivo ni RED inventado.

Segundo caso @s24/@s28: primer intento falló en preparación (`edfeef`): la planificación rechaza correctamente fechas pasadas con IN_PAST. Se conserva `weekly_review_e2e_durable_fixture_failure.log`. El fixture crea la reserva futura por HTTP y traslada sólo sus fechas a 2020; tras CLOSE real traslada sesión, fin efectivo, intervalo y recibo de dominio coherentemente, con 30 minutos conocidos. No es un flujo HTTP puro de viajar al pasado. El oráculo válido pasó inicialmente GREEN 1/1, EXIT0 `2d5dc0`, 13,1 s; log `weekly_review_e2e_durable.log`.

Se exigen antes del reinicio plan positivo 3.600.000.000 µs, trabajo 1.800.000.000 µs y capacidad actual 50.400.000.000 µs, calculados desde el fixture. Se retira outbox por IDs propios, se reinicia realmente la API verificando que PostgreSQL conserva proceso/volumen, y se comparan sesión/intervalos/recibo/reserva y respuesta salvo serverNow posterior. La UI muestra 1 h/30 min y los conserva al recargar. Stack `organizationweb-e2e-7924` retirado. El test no intercepta respuestas W ni afirma observar tráfico Rabbit; sólo acredita que las lecturas no crean nuevas filas de outbox.

Pendiente: selección explícita y Back desde controles; recuperación de consulta con respuesta demorada y errores. Los hechos positivos, recarga y reinicio durable ya están acreditados por el segundo caso.

UX se centrará en la nueva página y el quinto enlace de navegación: siete días/totales legibles, lenguaje que separa reserva/trabajo/presupuesto, cero como descanso planificado actual sin inferir descanso real, carga/error/datos anteriores, controles nativos, teclado/foco y áreas de 44 px. Se comprobarán reflow a 320, puntos de cambio relevantes, texto 200 %, zoom nativo 200 % y motores sobre este cambio. No se repetirá la matriz completa de Historial ni se atribuirá cumplimiento de los 30 principios sólo por axe. El informe final distinguirá evidencia física, revisión semántica y límites no medidos.

Primer UX real RED `d7842e`: a 320 px, selector Zona horaria de 156×19 px; Ver planificación de 115,7×21 px también incumple 44 px. La página no desborda (scrollWidth=320), quinto enlace de navegación 296×44, fecha 290×54, botones 290×45 y navegación semanal 290×44. Se preservan JSON/PNG iniciales en `progress/weekly_review_ux_initial_artifacts/` y log `weekly_review_ux_initial_failure.log`. Todavía no se alcanzaron los demás anchos ni axe/teclado. Ajuste SCSS local propuesto a root, pendiente de liberar init integrado; no se cambió producción durante ese gate.

## Ampliación ejecutada tras el init integrado

- CSS local autorizado: selector con estilos de input y enlaces propios con área 44 px. Mismo caso RED→GREEN `224374`, 16 anchos iniciales y axe 0; root integró `24fd83b`.
- Recuperación diferida encontró un segundo RED real `f8dcab`: Reintentar desaparecía sin restaurar foco. RED DOM `07d860`, mínimo capturar botón aún enfocado; suite DOM 28/28 `018263`. E2E afectado GREEN `df3716`: conserva selección/datos anteriores, muestra pendiente/error y reintenta realmente. No se cambió el timeout ni se retiraron aserciones. Ver `weekly_review_ux_recovery_focus_red.log`.
- Texto 200 % inicialmente GREEN `8c52c6`: factor 2 por elemento verificado, tres anchos y tres axe 0.
- Zoom nativo 200 % inicialmente GREEN `fbf651`: extensión aislada, chrome.tabs.getZoom=2, DPR 1,5→3, viewport 320×453 CSS y axe 0. Evidencia única `.e2e-work/weekly-review-native/organizationweb-e2e-33332/evidence/`, sin limpieza del perfil/evidencia.
- Chromium final `8e11ba`, tres casos GREEN: 26 anchos nominales incluyendo mínimos y fronteras relevantes, 12 medidas de estados/rest-plan/pending/error/recovered y tres de texto200; ocho axe 0. El reintento se activa por Enter para acreditar el recorrido con foco explícito. Feedback final 4,6 ms antes de liberar respuesta 503 controlada; el GET posterior es real. La primera versión con click también había pasado en Chromium.
- Tercer funcional @s28 inicialmente GREEN `3da2d6`: editar fecha no consulta, Mostrar semana aplica URL, anterior/Back/recarga conservan selección, Esta semana retira date manteniendo UTC. Log `weekly_review_e2e_selection.log`. No mocks de resumen.

Todos esos runners retiraron sus propios stacks. Firefox/WebKit y cierre documental aún pendientes en este punto; no se reclama cumplimiento físico universal.

## Freeze final de pruebas y evidencia

Firefox `0f74d9` y WebKit `7dab95`: tres casos por motor, EXIT0; logs `weekly_review_ux_firefox.log` y `weekly_review_ux_webkit.log`. Incluyen nominal 26 anchos, recuperación/rest-plan/pending/error/recovered 12 medidas y texto200 tres medidas por motor. Cada motor aporta ocho axe sin infracciones; zoom nativo añade una medida/axe. Total real del inventario `f6db99`: **124 medidas, 25 axe, cero infracciones**. Feedback por motor 4,6/5/3 ms antes de liberar503; no se interpreta como latencia total de consulta.

Dos archivos E2E congelados: `weekly-review.spec.mjs` SHA `6E8260E5CADAFAB13D990A7A0AA357CF85BB2194FE11A4F4BBF0D1F303CC26CE` (tres casos funcionales ejecutados individualmente); `weekly-review-ux.spec.mjs` SHA `E19613060312C349B9B608FBCFC5A72FF1CB64678E5C7458C8AB84157705D558` (tres casos en cada motor y un zoom nativo). Node syntax y Prettier focal GREEN `f6db99`. No se afirma que los 34 escenarios contractuales sean 34 casos browser.

Mapa30 final en `ux_weekly_review.md`; hashes y conteos en `weekly_review_e2e_ux_freeze.json`. Parser de scope `weekly_review_mutation_scope_parser.mjs` se conserva explícitamente como herramienta documental reproducible, no como test ejecutable del producto; nodes actualizado al fix final. Los auxiliares y raw de evidencia se conservan localmente. Ningún runner activo ni reserva de18080;8080 intacto. No campaña Stryker iniciada por B.
