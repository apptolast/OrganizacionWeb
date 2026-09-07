Permite consultar desde Historial la planificación, los cambios de tarea y las sesiones realizadas mediante una API de lectura autenticada. Una persona puede filtrar por categoría, proyecto/tarea y fechas UTC, abrir detalles y volver a páginas anteriores conservando la URL. Las reservas se distinguen del trabajo real; cerrar una sesión no completa una tarea.

La consulta reúne cinco fuentes durables de PostgreSQL, conserva precisión de microsegundos y usa paginación por cursor. Incluye aislamiento por propietario, coherencia de recibos, lectura transaccional, errores recuperables y retirada de datos privados tras perder la sesión. La navegación y los controles usan React y SCSS; no añade dependencias de interfaz.

Este es el corte de integración de la funcionalidad 18. El adaptador HTTP de la PR 18 ya está incorporado: esa rama es una referencia y no debe fusionarse aparte.

Validación local final: 2.217 pruebas Java, 1.899 frontend y 47 del arnés; compilación; 129 E2E y 13 comprobaciones del publicador. Campañas originales PIT 263/284 y Stryker 621/738, con tres RuntimeError explícitos fuera de su denominador oficial. Replays separados: PIT 15/17 (seis objetivos detectados) y Stryker 32/33 (diecisiete objetivos detectados). Residuos e identidad de cada mutación conservados en progress/.

La revisión UX conserva 495 mediciones y 27 análisis axe sin violaciones en evidencia compuesta. Teclado verificado en Chromium y Firefox; WebKit Windows sólo acredita geometría mediante clic y texto ampliado. Zoom nativo verificado en Chromium. Dispositivos y lectores de pantalla físicos siguen sin comprobarse.

Cierre técnico local aprobado en progress/judge_history_final.md; la CI remota debe pasar sobre este corte antes de fusionar. No hay despliegue productivo acreditado. Contrato en project-spec.md y features/history.feature; alcance y dependencias operativas en docs/mvp-delivery-plan.md.
