# create_project: integración

## Autorización y alcance
El coordinador ejecutó init y confirmó aprobación humana e in_progress antes de delegar. Este trabajo conecta React SCSS, Spring Boot y PostgreSQL para la única feature aprobada. No incorpora broker/publicador, listado ni pantallas de roadmap.

## Ciclo 1
- Rojo: `pnpm exec playwright test`, navegador Chromium instalado: `page.goto: net::ERR_CONNECTION_REFUSED` contra 127.0.0.1:18080. La aplicación todavía no estaba servida. Un intento anterior detectó navegador ausente; se instaló y se repitió para obtener el rojo real.
- Implementación de integración: nginx sirve React y reenvía /api/ sin reintentar POST; compose inicia PostgreSQL con volumen y backend. Único puerto publicado loopback. Credenciales obligatorias en entorno, sin valores de producción predeterminados.
- Runner E2E usa proyecto Compose único, credenciales exclusivas de prueba y volumen aislado; limpia únicamente ese proyecto al finalizar.
- Verde pendiente del trabajo de frontend y backend; no se declara verificación completada.

## Trazabilidad
- @s1, @s22: e2e/create-project.spec.mjs confirma HTTP 201 real y representación del servidor en navegador.
- @s2..@s21: pruebas de backend con PostgreSQL real y Testcontainers (responsable backend).
- @s22..@s28: pruebas de interfaz y posteriores comprobaciones de navegador (responsable frontend/integración).

## Herramientas verificadas
Dependencias fijadas: pnpm 10.21.0, Playwright 1.63.0 y axe-core/playwright 4.13.0 (registro npm consultado). Imágenes verificadas en las páginas oficiales Docker Hub: node:22.23.2-alpine3.23, postgres:17.11-alpine3.23 y nginx:1.30.4-alpine3.24. CI usa JDK25 y Node22. Cada frontend/backend conserva su propio arnés de dependencias.

## Comprobaciones de configuración
- node --check scripts/project.mjs y scripts/e2e.mjs: correctos.
- docker compose config --quiet con credenciales sintéticas: correcto.
- nginx auth_request usa GET /api/session autenticado para solicitar credenciales desde la navegación inicial. /healthz solo comprueba nginx; runner espera también API 401.
- El anuncio accesible confirma guardado; el identificador se comprueba por separado en la tarjeta para no imponer una estructura visual que el contrato no exige.

## Ciclo integrado 2
- Build real de ambas imágenes correcto. Chromium encontró nginx500 al autenticar la navegación: el endpoint /api/session acordado aún no estaba implementado en el snapshot backend.
- Se solicitó corrección al responsable backend, sin tocar su código. Readiness del runner ahora exige GET /api/session autenticado204: una respuesta401 a usuario anónimo no demuestra que exista el endpoint.
- El test comprueba HTTP200 al abrir documento para fallar inmediatamente ante errores del proxy.
- Limpieza del stack/volumen exclusivo E2E completada al fallar.

## Verificación incremental real
- Autenticación document/API y POST: verde (1 caso).
- Persistencia PostgreSQL/outbox tras recarga y reinicio real del backend: verde (2 casos acumulados, 8.9s). Misma representación SQL antes/después; evento pendiente sin descripción. RabbitMQ ausente no bloquea creación.
- Texto literal `<b>`/`<script>` sin nodos interpretados ni diálogos: verde (3 casos,9.9s).
- Descripción Unicode de4001 puntos de código: HTTP400 real conserva ambos campos, error asociado y corrección posteriorHTTP201: verde (4 casos,11.2s).
- Viewports320/768/1440 y reflow720 CSS px (equivalente de1440 al200%): rojo axe por contraste .gentle-note>p4.42 y sidebar-label/small4.48, umbral4.5. Sin overflow horizontal inicial. Se remiten selectores y colores al responsable frontend para corregir SCSS. Comprobación de teclado/success queda pendiente porque el scan inicial falla.
- Corrección SCSS del responsable frontend: axe inicial verde en los cuatro viewports.
- Siguiente rojo real @s28: después del POST201 por Enter y anuncio de éxito, Chromium pierde foco al deshabilitar submit y no lo recupera. El foco previo y outline estaban correctos. Se solicita restauración al finalizar, respetando cualquier cambio de foco intencional del usuario.

## Verde integrado
- Chromium completo: **8 casos verdes en18.6s**, después de corregir el contraste y la pérdida de foco detectados en navegador real.
- @s1,@s22 → real browser saves an idea through the same-origin API.
- @s16,@s19,@s20 → project and outbox survive reload and backend restart (SQL exacto, eventos pendientes, sin broker).
- @s26 → server-confirmed markup remains literal text.
- @s24 → server validation preserves both fields and permits correction.
- @s27,@s28 → accessible keyboard creation, cuatro ejemplos:320/768/1440 y reflow720 CSS px, equivalente de viewport1440 al200%. La prueba reproduce el ancho CSS de zoom; no acciona el menú nativo de zoom del navegador.
- axe WCAG2A/AA,2.1AA y2.2AA: cero infracciones automáticas tanto antes como después del guardado en esos cuatro ejemplos. Esto no equivale a una auditoría manual completa de accesibilidad.
- En todos los casos el frontend usa API real, Basic vía proxy y PostgreSQL persistente. No hay mocks de red en esta suite.
- Capturas de escritorio/móvil entregadas al coordinador para revisión visual fuera del repositorio, con nombre de proyecto de prueba explícito.
- Archivos de herramientas formateados con Prettier existente. Falta la última repetición sobre el árbol congelado cuando responsables frontend/backend terminen su handoff; el coordinador la dirige.

## Verificación final sobre árbol congelado

El coordinador ejecutó `pnpm build` y `pnpm test:e2e` tras congelar ambas fronteras: exit0, **8/8 casos verdes en16.9s**. Incluye regresión geométrica del enlace «Saltar al contenido»: fuera de foco su borde inferior queda<=0; con el primer Tab aparece dentro de pantalla y recibe foco. Se conserva el recorrido de teclado hasta guardar.

Las capturas finales de escritorio y móvil se regeneraron y el coordinador las revisó visualmente: desapareció la franja causada por el skip-link. Los textos, campos, confirmación y foco se ven correctamente. El coordinador también comprobó65 tests backend sin fallos desde XML y38 frontend verdes; la verificación global con mutación se registra por separado.

No quedan cambios de integración pendientes. Los logs anteriores describen los ciclos observados; el estado final es esta ejecución congelada.

Verificación global final del coordinador: `node .harness/harness.mjs verify`, sesión79769, exit0 «Todo verde».65 backend y38 frontend; PIT36/36 (100%); Stryker143/148 (96.62%), cinco equivalentes documentados, cero errores/timeouts/sin cobertura. El informe del juez aprueba el núcleo; la revisión independiente del tooling corresponde al coordinador.
