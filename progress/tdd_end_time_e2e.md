# Primer E2E17 — pendiente de integración UI

Árbol aislado end-http sobre wiring95a3881, HTTP real y PostgreSQL del checkpoint0d48abb. B confirmó labels y que Ampliar tiempo está disponible antes del fin; se usa inicio real25min y ampliación explícita15min, sin modificar fechas ni simular respuestas.

Único caso `e2e/end-time-notification.spec.mjs`: detalle de tarea → inicio201 real → abrir ampliación sin cantidad preaceptada ni POST → confirmar EXTEND201 → comparar State6/EXTEND7, fórmula µs en PostgreSQL, fin original intacto y ausencia de intervalos → navegar por enlace a URL16 → comprobar E y recuperar C, sin duplicar cambios. La segunda mitad permanece sin alcanzar hasta GREEN; no se presenta como evidencia ejecutada.

Prettier/nodecheck **ff9ab8 EXIT0**. Ejecución `node scripts/e2e.mjs e2e/end-time-notification.spec.mjs`, sesión94623, **c6568d EXIT1**. RED real: después del inicio201 y de ver En curso, el navegador espera el botón «Ampliar tiempo» inexistente en este frontend anterior. Localización44 del test, log `progress/end_e2e_01_red.log`, contexto propio en test-results/end-time-notification-end--2c115-n-session-detail-s1-s28-s29/error-context.md. No es un error de API ni un rechazo del POST17: aún no se transmitió esa decisión.

El runner retiró únicamente su stack `organizationweb-e2e-6720`, sus tres contenedores, red y volumen; comprobación por esa etiqueta sin contenedores restantes. Puerto18080 libre. No se tocó8080 ni se limpió `.e2e-work` global o rutas protegidas. Hashes del test y log preservados en `end_e2e_first_red_hashes.json`.

Freeze del único caso RED hasta recibir bundle B congelado. No más casos, cambios frontend, campañas ni resultados de ampliación atribuidos antes de ejecutar. El siguiente paso será repetir este mismo recorrido tras integración aprobada.

## Integración nominal y vencimiento real

Root incorporó el snapshot frontend de 14 archivos d795f6, sobre backend c2882fd. El primer intento alcanzó POST/SQL y falló por dos enlaces de cierre: fixture de selector global (689a5b), corregido acotando al panel Fin de la sesión, sin modificar producción. Nominal final bb7728 EXIT0, 1/1 (4,5 s), log end_e2e_nominal_final.log; todos los oráculos antes pendientes alcanzados.

Segundo caso individual, inicialmente GREEN a8fb59 EXIT0: end_e2e_deadline_initial.log, 1/1 (1,1 min). Inicio real de un minuto; GET E inicial anterior al fin y GET del temporizador posterior o igual, comparados como timestamptz PostgreSQL. Aviso visible sin mover el foco del enlace principal ni crear cambios/intervalos/eventos; ampliación explícita de un minuto confirmada y nuevo E futuro retira el aviso. No reloj simulado, actualización SQL ni polling añadido. Ambos runners retiraron exclusivamente sus stacks.

## Recuperación y composición

Primer intento 20ca32 EXIT1 alcanzó upstream201 perdido, K200 exacto y PAUSE201 revisión3. Falló una expectativa impropia de texto «En pausa» en Reader16, que muestra el fin pero no ese texto. Se conserva log end_e2e_recovery_initial.log y manifiestos antes/después; se sustituyó sólo ese oráculo por heading del lector, conservando E real con estado paused. Se corrigieron etiquetas erróneas s31/s32 a s36/s37/s39, sin atribuir timer largo ni fracciones a este caso.

Resultado final a71eee EXIT0, 1/1 (4,8 s), end_e2e_recovery_final.log. Recibo EXTEND completo por K, sin reenvío automático; pausa posterior201, navegación/recarga y E preservan fin; C mantiene recibo original, dos cambios totales y un solo evento EXTEND. Comparación 3558a6: cero diferencias de inputs final antes/después. Producción/runner respecto del primer manifiesto nominal también idénticos (554459). Se mantienen límites: no prueba aquí tiempo largo, carreras naturales ni cierre externo.

Preparación del tercer caso: el primer comando Prettier se lanzó en raíz sin ejecutable (822382); corregido usando pnpm --dir frontend, 8731bb GREEN. No fue un RED del producto.
