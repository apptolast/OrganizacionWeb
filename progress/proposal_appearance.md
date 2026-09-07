# Propuesta 20: apariencia

Pendiente de revisión root, sin producción, tests ni estado spec_ready. La autorización global permite concretar el contrato sin otra pregunta al usuario.

Tres valores editables: tema claro/oscuro/sistema y dos acentos sRGB hexadecimales, uno por tema. Root eligió colores arbitrarios validados frente a la alternativa de un catálogo cerrado; conserva personalización amplia sin permitir CSS libre. Orden, densidad, fuentes, campos y vistas pertenecen a incrementos posteriores, no se dan por satisfechos con estos tres campos.

Persistencia PostgreSQL propia, GET/PUT `/api/v1/me/appearance`, revisión fuerte y concurrencia igual que disponibilidad. Se descarta localStorage como fuente de verdad: no sobrevive a cambio de dispositivo y podría mezclar propietarios. GET inicial sin fila devuelve defaults sin insertar. No evento artificial: disponibilidad ya documenta que una preferencia personal sin consumidor no se publica; apariencia tampoco fabrica un proyecto para entrar en outbox.

Ruta `/apariencia` desde Principal; formulario nativo, vista previa local rotulada y guardado explícito. Restaurar defaults prepara el borrador, Guardar los persiste. La apariencia confirmada vive durante navegación interna; no se vuelve a consultar por cada ruta. Fallos y respuestas antiguas no aplican colores ni revocan acceso actual. El modo Sistema sigue el sistema operativo sin escribir preferencias.

El acento sólo gobierna enlaces de acción, acción primaria, indicadores seleccionados y foco. Fondos/neutrales y colores semánticos de error/éxito se mantienen fuera de la personalización. Las superficies exactas de claro/oscuro y la fórmula de contraste están en la sección 20. Cambiar tema alcanza las pantallas existentes mediante tokens SCSS, sin modificar sus layouts o contratos funcionales. El contraste se valida antes de guardar y de aplicar respuestas; no se corrige silenciosamente el color elegido.

Alternativas: vista previa global inmediata o muestra local; se elige muestra local para no aplicar borradores a rutas ajenas ni simular persistencia. Guardado automático o explícito; se elige explícito con versión y recuperación manual. RESET por DELETE o PUT; se reutiliza PUT de defaults, evitando otra ruta y conservando control de concurrencia.

Implementación prevista después del gate: valor/caso de uso/puerto/store/controlador de preferencias conforme disponibilidad, migración aditiva y wiring; cliente/página de apariencia, integración de sesión/App/Workspace y tokens de styles.scss. Sin biblioteca de temas, router nuevo, store genérico, catálogo de colores ni utilidades preventivas.

Riesgo principal: inventario de colores literales existente. La implementación deberá cubrir roles y estados de las pantallas actuales en ambos temas, incluidos errores, foco y controles nativos. No basta colorear la nueva página. La evidencia UX revisará las 30 filas de docs/ux-requirements, con límites físicos honestos; no se afirma accesibilidad universal por una fórmula o axe.

Fuente primaria de la fórmula/umbral: [WCAG 2.2, contraste mínimo](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum). Las superficies, roles y umbral uniforme para acentos son decisiones de este producto.
