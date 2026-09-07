# Dictamen final de Historial (funcionalidad 18)

Estado: APPROVED técnico local con los límites documentados. Init de cierre33489 terminó EXIT0 (0b3990); producción, contrato, trazabilidad y refuerzos aprobados. La integración remota y el despliegue se acreditan por separado.

## Alcance y revisión

Contrato aprobado:39 escenarios y142 ejemplos en features/history.feature, SHAC9AB1D0486E98FB3F7B868EB5074DFD8C1EEE2EADC463B47C9032749F87B5D0D. No son recuentos de pruebas ejecutadas. La consulta autenticada reúne cinco fuentes durables, filtra contexto/categoría/fechasUTC, conserva precisión de microsegundos y pagina mediante cursor. Lee una transacción consistente por petición; no promete snapshot permanente entre páginas. Los nombres de proyecto/tarea son actuales y los recibos conservan los detalles históricos.

Los paquetes de backend, HTTP, cliente, navegación y UX tienen revisiones separadas en progress/review_history_*.md. La revisión independiente review_history_traceability_final.md cubre los39tags y conserva las reutilizaciones. Para @s24 se acepta evidencia compuesta de cinco fuentes, retirada de outbox, reinicioAPI conservandoPG y ausencia de dependencia del broker; no se atribuye una única ejecución literal que haya combinado publicación previa y caída del broker con esas cinco familias.

Root revisó los seis refuerzos Java7c9109/a824fc y los doce de frontend e4fc71/91fefc. Todos fueron inicialmente verdes sobre producción correcta; no se inventan ciclos RED. Las mediciones dirigidas posteriores demuestran su capacidad de detectar las mutaciones previstas. No se modificó producción durante esas campañas.

## Evidencia local

- Init de cierre:2.217 Java en93 suites sin fallos/errores/omitidos,1.899frontend y47Node; EXIT0 0b3990. Root contó los93XML y los preservó en history_closure_backend_xml (a695bb). Log history_init_closure.log SHAFC2006AFBC2135CBE0435BC2E08A5B0813E2576562F2646B1CC1E91200656972.
- Frontend final:1.899 pruebas/40suites, lint y build verdes b45d0c. Root verificó137 hashes antes/después y contra el árbol actual b1405a. Compilación completa anterior80876c y compilación frontend posterior al últimoCSS acreditada.
- E2E global:129/129,13,4min, EXIT0 2c5d89; log history_e2e_global_final.log SHAF54A994AE897C22A1E4F842287DFCD218C728435BE518F6FE91D667E08DA279C. Stack efímero propio retirado; no se tocó el puerto8080 del usuario.
- Publicador:13PASS, EXIT0 66a983; log history_publisher_verified.log SHA5AE112227DB7524F6834A71A8D85BCE7566271FB4F53526DD19BBE62B2D57E0A. Es regresión heredada; Historial no añade eventos.
- UX:495mediciones y27axe sin violaciones en evidencia compuesta. Chromium/Firefox acreditan teclado; WebKit Windows acredita geometría por clic y texto200, no teclado de enlaces. Zoom nativo200 acreditado en Chromium. Root revisó muestras visuales y copió141 artefactos con SHA verificado50cac4; no afirma inspección de todas las imágenes ni pruebas en dispositivos/lectores físicos o personas.

## Mutación y residuos

PIT original:284=263KILLED+19SURVIVED+2NO_COVERAGE, cero errores/timeouts;92,6056%, EXIT0 f7a179. XML D8E242C11763F095BDEAA23167184E4C77C441D3D430E74D285F0E08397610D5. Replay17=15K+2S,88,2353%, EXIT0 3a2a66. Root contrastó las seis firmas exactas objetivo, todasK, y376inputs idénticos551477. Los dos extras supervivientes son límites1/1440 de extensión; no se llaman equivalentes ni defectos de producto confirmados. Todos los residuos originales permanecen inventariados.

Stryker original:741=621Killed+110Survived+7NoCoverage+3RuntimeError. Score oficial621/738=84,1463%; con todos los estados621/741=83,8057%. History.tsx78,33% individual se conserva explícito; el umbral configurado se aplica al alcance de la campaña. Replay33=32K+1S,96,9697%, EXIT0 b57d3d:17/17firmas objetivoK, quince extrasK y un extra superviviente equivalente (el contador sólo dispara cambios, no depende de su signo). Root verificó mapping y raw b00293;137inputs idénticos. Los tres errores originales siguen siendo errores, no detecciones positivas.

Ambas campañas originales y ambos replays superan80. No se agregan campañas para fabricar un porcentaje mejor. Los refuerzos cubren prioridades observables de paginación, contexto/forma de recibos, integridad del DTO y recuperación. Los demás huecos y equivalencias contextualizadas se conservan en mutation_history_backend.md y mutation_history_frontend.md. No se exige100% ni se ocultan residuos.

## Límites de entrega

No hay hallazgo funcional bloqueante de este contrato. El objetivo de cierre técnico local no acredita por sí solo CI, fusión ni servidor. Las funcionalidades19–30 (personalización avanzada, conectores y automatizaciones) siguen fuera del MVP1–18 y pendientes de sus contratos. AccesoSSH/hostname del despliegue siguen solicitados; el último intento terminó Permission denied(publickey). No hay despliegue, HTTPS, capacidad o restauración productiva verificados.
