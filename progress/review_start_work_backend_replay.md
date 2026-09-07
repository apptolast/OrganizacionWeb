# Revisión del selector replay backend14

**APPROVED, alcance de soporte y selección.** Lectura independiente del diff completo en 3c8724 y del dispatch/configuración circundante en 07f35d. Sin ejecutar pruebas ni PIT en esta revisión. Ponytail full y Caveman lite.

El destino start_work_session-backend-replay sólo se admite para mutate. Invoca el Gradle existente con mutationScope=start_work_session_replay y retorna antes del destino amplio. Se conservan el comando por plataforma, directorio backend, destinos anteriores y rechazo de targets inválidos.

La nueva rama de targetClasses contiene exactamente com.apptolast.organization.application.WorkSessionStarted. targetTests conserva com.apptolast.organization.*; no recorta las pruebas que pueden detectar los accessors. El reporte usa pitest-start-work-session-replay, separado de la campaña original. Umbral80, cuatro threads, mutadores, timeout y formato HTML/XML permanecen intactos; no hay exclusiones adicionales ni cambio del scope amplio/default.

El test del runner inyectado comprueba comando/argumentos/directorio y los literales críticos del alcance y reporte. Autor registra RED55412c y GREENc99bff (30 scripts), formato bf07fd y dry-run Gradle e84f3a; son evidencias del autor, no ejecuciones repetidas por este juez.

Límite: este replay sólo puede medir el record. No acredita detección del nuevo caso de calendario de OutboxMessage ni resuelve/reclasifica el timeout original de Rabbit. Los reportes originales y esos estados deben conservarse al publicar el resultado.

Hashes del corte:
- scripts/project.mjs: AF9867949EDFE4F46D4DE12FA129D04849E5B15E819760F899F1414423D430A5.
- scripts/project.test.mjs: 66C6635A65E8A35C136D0F91A68EBFB3227C4C77A1EA05368BDF32064E5564E2.
- backend/build.gradle.kts: 4F11D8738307CF29E316AA808876972A9F6BAFA59D4ACE04188744D1BD76D2B7.

Sin hallazgos bloqueantes. El autor puede iniciar la medición acotada autorizada por root sobre su freeze.
