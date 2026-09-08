# Dispatcher oficial de integración API (24)

Sólo scripts/project.mjs y scripts/project.test.mjs registran integration_api-frontend, integration_api-backend e integration_api-http-backend. Fronteras acordadas: Stryker integration-api y mutationScope integration_api / integration_api_http. No cambia umbral, clases, candidatos, timeout ni directorios de reporte de los motores.

TDD: frontend RED 568ef3 (target no admitido), GREEN 44dd44; backend RED 058da5, GREEN fc3e32. Node final 72/72, EXIT 0 4cb4ff. Formato con Prettier instalado en frontend, EXIT 0; intento previo desde root sin instalación local registrado como fallo de setup, no RED de producto. No se ejecutó PIT ni Stryker.

Huellas antes de Git: project.mjs 25BFC7CE28BEBBA1DDD3279919C52872D7623AECC62CDA0440F5346158C83D3C; project.test.mjs 388BD3D1CF4AC2EE89A25882EA4F1FD46B55C3BE93A7AEAA97F6161A5CAD31E9. Logs/exit originales permanecen en este checkout y se incluyen en el commit selectivo.
