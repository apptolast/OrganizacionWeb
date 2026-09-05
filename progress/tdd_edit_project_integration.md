# edit_project — integración de navegador

Contrato aprobado leído. Ponytail full y Caveman lite activos; sin producción backend/frontend a cargo de este agente. Se reutiliza el fixture E2E y se extraen los helpers existentes de PostgreSQL/creación a e2e/support/projects.mjs para compartirlos entre lectura y edición.

Primer recorrido preparado: edición real con If-Match, preservación de campos inmutables, outbox Updated exacto, no-op sin escritura, recarga y texto literal. Se espera disponibilidad de GET con ETag, PUT y formulario antes de ejecutar. Los ciclos TDD de producción corresponden a sus autores; no se fabrica un rojo de integración sobre código ya correcto.

Pendiente: conflicto real entre dos pestañas, recuperación/privacidad, matriz responsive y zoom nativo reutilizado; revisión independiente backend después del congelado. No se repite la batería de crash sin cambios en esa frontera.

## Primera verificación real

Fixture propio `organizationweb-e2e-62156`, PostgreSQL y API reales, publicador desactivado explícitamente. Primer build disponible de los autores. `node .e2e-work/edit-check.mjs --grep two.tabs`: 1 prueba verde en 4,4 segundos. Ambas pestañas cargaron la misma precondición; el primer PUT confirmó 200 y el segundo devolvió 412, manteniendo nombre y descripción del borrador. Snapshot SQL sin cambios por el rechazo. La recarga fue una acción explícita y el siguiente PUT usó el nuevo ETag y confirmó 200. No se generó un rojo artificial sobre código ya verde de sus autores.

`node .e2e-work/edit-check.mjs --grep failed.saves`: 1 prueba verde en 8,6 segundos. Respuestas de escritura 400/503/500 y pérdida de red se inyectaron únicamente en Playwright para verificar la recuperación visual; no se presentan como fallos reales de PostgreSQL. Se conservó el borrador, 400 marcó el campo, 401/404 retiraron los datos, y todos los snapshots SQL permanecieron intactos. Tras retirar la interceptación, una escritura real confirmó el contenido nuevo. La cobertura de fallos reales y seguridad del API pertenece a las pruebas backend.

## Navegación, persistencia y accesibilidad

Tras incorporar enlace desde detalle y estilos: recorrido de PUT real, evento exacto, no-op, recarga y Cancelar verde. Se añadió una comprobación de espera reteniendo únicamente la entrega de la respuesta real en Playwright: `Guardando cambios` apareció a los 3 ms desde submit, Guardar quedó deshabilitado y un segundo submit programático no generó otro PUT. Al liberar la respuesta se confirmó éxito; nunca se simula un 200. Comando `node .e2e-work/edit-check.mjs --grep real.update`, 1 verde en 4,9 segundos.

Matriz de 12 anchos, axe, teclado y tap emulado: 1 verde en 12,8 segundos con el build que incorpora foco inicial solicitado por el coordinador. Zoom nativo al 200 % y 320 CSS con PUT real: verde; métricas y revisión en progress/ux_edit_project.md. No se repiten pruebas de crash del publicador.

## Motores y revisión visual

`node .e2e-work/edit-check.mjs --config=e2e/cross-browser.config.mjs`: 2 pruebas verdes en 10,8 segundos, una en Firefox y otra en WebKit. Sólo el recorrido de edición real/persistencia/no-op/recarga, sin multiplicar la matriz completa. Feedback medido: 2 ms y 13 ms. La configuración opcional reutilizada admite ahora lectura y edición, y el argumento de archivo selecciona únicamente edición.

El coordinador inspeccionó escritorio, móvil y zoom nativo estrecho junto con sus métricas: contenido legible y sin desbordamiento de página. El autor frontend también inspeccionó escritorio/móvil; se distingue su valoración de autor de la revisión independiente del coordinador.

## Cierre de integración

Build final backend/frontend y ejecución conjunta Playwright: 18/18 verdes en 1,3 minutos, incluidos los 14 históricos y cuatro recorridos nuevos. La matriz de edición incorpora ahora 22 anchos: doce base más ambos lados de los breakpoints solicitados; verde en 16,8 segundos. Feedback de guardado final: 2 ms. Capturas normales y zoom nativo se refrescaron sobre el build final, con las mismas métricas 713/320 CSS y otro PUT real confirmado al 200 %.

Hueco detectado en revisión: worker desactivado no demuestra disponibilidad del PUT con broker caído. Se amplió scripts/publisher-smoke.mjs, reutilizando el fixture existente y filtrando consultas outbox por tipo. Con worker habilitado, el script detuvo únicamente su RabbitMQ, guardó la edición con 200 en menos de 4,5 segundos, observó Updated pendiente con BROKER_UNAVAILABLE y al menos un intento, recuperó el broker y recibió el mismo eventId/payload con metadatos persistentes en la cola Updated. `pnpm test:publisher`, sesión 31275: exit 0; se conservaron verdes las comprobaciones históricas del script. No se ejecutó de nuevo la matriz de muerte de procesos. El script elimina sus contenedores y volúmenes propios; el coordinador revisa independientemente esta ampliación para CI.

Revisión independiente backend en progress/judge_edit_project_backend.md: APPROVED, limitada al código de edición y su cobertura. La revisión de mis E2E/smoke y la decisión global pertenecen al coordinador.

Limpieza final: fixture organizationweb-e2e-62156 cerrado con exit 0, contenedores, red y volumen PostgreSQL propios eliminados. Se retiraron los archivos de estado/stop de trabajo. El perfil temporal de zoom se cerró y eliminó en su finally con validación del directorio. No se tocaron servicios del usuario ni el despliegue real.
