# project_states — integración

Contrato aprobado leído; Ponytail full y Caveman lite activos. Se reutilizan fixture, helpers SQL/creación/snapshot, autenticación y controles de navegador. El primer recorrido preparado recorre activación, pausa, reanudación, finalización y reapertura; verifica persistencia, evento exacto de ocho campos, lectura/lista, no-op y edición que conserva estado.

Preparación inicial: se esperó al primer build de API y frontend antes de ejecutar. Los ciclos rojos de producción corresponden a sus autores; no se inventa RED sobre código ya correcto. Plan inicial: última plaza con peticiones concurrentes, conflicto compartido con edición, recuperación, matriz de 22 anchos y zoom nativo en controles nuevos, y extensión acotada del smoke de publicación. La matriz de muerte de procesos no se repite si su frontera no cambia.


## Primera ejecución sobre API y web disponibles

Fixture propio organizationweb-e2e-57468, PostgreSQL y API reales, worker desactivado explícitamente en el stack de navegador. `node .e2e-work/states-check.mjs`: tres pruebas verdes en 27,8 segundos. Se verificaron cinco transiciones, snapshot de proyecto, evento exacto de ocho campos, lista/detalle/edición, no-op y conservación de estado al editar texto. Dos PUT enviados concurrentemente produjeron HTTP 200/409, exactamente una activación adicional y tres activos; la UI explicó el límite y permitió elegir una pausa antes de reintentar. El conflicto entre texto y estado se probó en ambas direcciones, con recuperación deliberada y sin pérdida de cambios.

La primera transición se amplió para retener únicamente la entrega de la respuesta real: Cambiando estado apareció a los 2 ms desde click, el estado visible siguió siendo Idea hasta confirmar y los botones quedaron deshabilitados. Un doble click produjo un solo PUT. Ciclo ampliado verde en 12,3 segundos; no se simuló HTTP 200.

## Controles y publicación

`node .e2e-work/states-check.mjs --grep controls.reflow`: matriz de 22 anchos verdes en 20,6 segundos. Nombre de 120 caracteres y descripción de 4000, ambos sin espacios; sin overflow horizontal, controles principales al menos 44 por 44 CSS y axe sin infracciones seleccionadas. Teclado confirmó pausa y trasladó foco al encabezado cuando desapareció el botón; se revisaron además estados Terminado/Pausado a 320 y Retomar por tap emulado.

Zoom nativo mediante el helper de perfil aislado y tabs.setZoom(2): DPR 1,5 a 3, interior 713/320 CSS, documento 705/705 y 312/312 respectivamente. Se confirmó una activación real al 200 % y 320 CSS. Capturas/JSON en outputs/project-states-*. El coordinador inspeccionó escritorio, móvil y zoom estrecho sin hallazgo visual bloqueante; el autor frontend comunicó también su inspección, identificada como observación de autor.

`pnpm test:publisher`, sesión 9474: exit 0. Se reutilizó el smoke existente para activar un proyecto mientras su broker estaba detenido y el worker seguía habilitado. HTTP 200 en menos de 4,5 segundos, evento pendiente con al menos un intento y BROKER_UNAVAILABLE. Tras recuperar RabbitMQ se recibió el mismo eventId y JSON de ocho campos, sin nombre ni descripción, con metadatos persistentes; cola durable quorum y binding de StatusChanged comprobados. Created/Updated y las comprobaciones existentes del script permanecieron verdes. No se repitió la matriz de muerte de procesos. El script eliminó su propio stack y volúmenes al terminar.

## Configuración y estado de cierre

La revisión del coordinador detectó que Compose no propagaba el límite. Se añadió APP_MAX_ACTIVE_PROJECTS con default 3 a docker-compose.yml y se documentó .env.example. Configuración renderizada con credenciales sintéticas: valores 3 y 2 correctos; sin imprimir secretos. Los fixtures fijan 3 explícitamente para no heredar preferencias del entorno. Hallazgo de revisión y comprobación verde; no se presenta como rojo ejecutado previamente.

Init raíz 51375 comunicado por el coordinador: verde, 328 pruebas backend, 171 frontend y lint. Pendientes de cierre: smoke de dos motores en curso, suite E2E conjunta final, informes UX/backend y limpieza del fixture de navegador. Sin hallazgos bloqueantes hasta este punto.

## Cierre definitivo

Firefox/WebKit: dos recorridos reales verdes en 24,4 segundos, sólo la prueba nueva de transiciones/persistencia/edición; feedback 2 ms en ambos. Suite Chromium sobre build congelado: 22/22 verdes en 2,0 minutos, incluidos los 18 históricos y cuatro nuevos. La matriz de 22 anchos pasó en 17,8 segundos y el feedback de estado final fue 1 ms. No se duplicaron pruebas Gradle locales del coordinador.

Informe UX completo en progress/ux_project_states.md. Revisión independiente de backend APPROVED en progress/judge_project_states_backend.md; XML inspeccionados directamente y PIT 163 KILLED. Mis pruebas y herramientas requieren revisión independiente del coordinador. La validación de configuración no imprime el config completo ni credenciales; fixtures fijan el límite en 3 para aislamiento.

Limpieza: fixture organizationweb-e2e-57468 cerrado con exit 0; eliminados contenedores, red y volumen propios. Se retiraron archivos de estado/stop de trabajo y los perfiles temporales de zoom se limpiaron mediante finally acotado. El smoke ya había eliminado su stack. Sin procesos propios abiertos ni cambios en servicios del usuario.
