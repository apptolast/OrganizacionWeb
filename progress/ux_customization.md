# Revisión UX21: evidencia final acotada

CSS de C revisado por root078155d. JS final de B más corrección física164; Store final B5D1…DDEB2. Referencia de resultados, snapshots y límites: tdd_customization_ux.md. No es certificación universal de usabilidad.

Chromium tiene cinco recorridos focales verdes; Firefox4/4. WebKit3/4: geometría, texto200 y modalidades verdes; enlace del proyecto no alcanzado por teclado después de guardar vista. Ese fallo sigue preservado y no se atribuye recuperación completa a WebKit. CI incluye los cinco casos Chromium (zoom headed bajo xvfb-run); configuración de motores separada operacional. No se acredita aún CI del corte final ni una campaña global nueva.

| Principio | Evidencia y límite |
| --- | --- |
| Atención selectiva | Secundarios neutrales; Crear/Guardar siguen últimos tras Cancelar, oráculo texto200. |
| Carga cognitiva | Fieldsets por vista/definiciones/valores;12 campos Unicode reales sin desbordamiento. Sin ensayo cognitivo humano. |
| Estética-usabilidad | Capturas320/1440 revisadas; LIGHT/DARK y texto200. Sin afirmar preferencia estética universal. |
| Posición en serie | Orden de metadatos/definiciones conservado; teclado Chromium/Firefox. WebKit no alcanza enlace. |
| Tendencia a la meta | No se añade cálculo de avance; no aplicable como comportamiento nuevo. |
| Von Restorff | Primario y mensajes diferenciados; espera visible antes de respuesta retenida. |
| Zeigarnik | Respuesta perdida conserva borrador; GET manual durable, sin falsa confirmación. |
| Fluir | Nominales PROJECT/TASK conservan negocio/outbox; edición y recarga reales. |
| Fragmentación | Vista, definición y valores agrupados semánticamente; geometría de dos superficies. |
| Memoria de trabajo | Borrador e incertidumbre conservados; recuperación explícita observada Chromium/Firefox. |
| Navaja de Occam | Shell, rutas, tokens y controles nativos reutilizados; sin dependencias nuevas. |
| Conectividad uniforme | Etiquetas y fieldsets expresan relaciones; no nuevo sistema de conexiones visuales. |
| Fitts |31anchos320–2560,62mediciones/868cajas iniciales; objetivos44px, checkbox20 con label44 y clic en borde. Sin dispositivo táctil físico. |
| Hick | Gestión colapsable;12 campos presentes y legibles; no medida de tiempo humano de decisión. |
| Jakob | Controles nativos y acciones convencionales; teclado real Chromium/Firefox; límite WebKit explícito. |
| Semejanza | CSS común; ambos scopes nominales reales. Modalidades exhaustivas aquí PROJECT, no duplicadas TASK. |
| Miller | Agrupación funcional; sin inventar máximo cognitivo de siete. |
| Parkinson | Sin cambios de planificación ni límites temporales; no aplicable al cambio. |
| Postel | Unicode/espacios exactos,0/false/DATE conservados; validación local revisada en JS de B. |
| Proximidad | Etiquetas y errores próximos; formularios delimitados, geometría sin solapamientos. |
| Prägnanz | Espera, confirmación e incertidumbre distinguidas; GET manual no anuncia guardado perdido. |
| Región común | Bordes y espaciado agrupan controles; reflow320 y texto200 comprobados. |
| Tesler | IDs/ETags fuera de UI; usados sólo por los oráculos externos. |
| Modelo mental | Campos personales separados de estado/progreso; hechos de negocio preservados. |
| Usuario activo | Crear/guardar desde UI; no se atribuye comprensión espontánea a usuarios no observados. |
| Pareto | Defaults y personalización contextual; sin porcentajes de uso inventados. |
| Fin de pico | Confirmación sólo tras200 visible; recuperación al encabezado tras fallo físico corregido164. |
| Sesgo cognitivo | Sin puntuación ni presión añadida; mensajes distinguen dato durable e intención no confirmada. |
| Sobrecarga de opciones | Gestión colapsable y12campos; texto200 en ambos temas sin recortes. |
| Doherty | Feedback observado3.5ms con respuesta retenida (<400ms); no promesa de latencia de servidor. |

Evidencia complementaria: zoom nativo Chromium200 (getZoom2/DPR3/viewport320), axe completo0; SYSTEMdark/reduced-motion/forced-colors, axe completo sin exclusiones; texto200 dos temas, cuatro axe0 Chromium. Motores repiten esos oráculos salvo zoom explícitamente Chromium. El skip-link en viewport con scroll0 está fuera de pantalla (top-100,bottom-55), no hay cambio CSS global por el artefacto fullPage anterior.

Límites: WebKit teclado incompleto, zoom nativo sólo Chromium, sin Safari físico ni teléfonos/tabletas reales, teclado virtual/áreas seguras no medidos, sin lector de pantalla humano ni estudio de comprensión. Las capturas y axe no sustituyen esas verificaciones. Los errores No tests found son preparación de comando, no fallos de producto. No se transforma WebKit EXIT1 en GREEN.

Actualización posterior autorizada: único ensayo separado WebKit ea4a15 EXIT0,1/1. Acceso al detalle por clic exclusivamente win32+WebKit; el resto conserva teclado y demuestra recuperación164. El3/4 original permanece fallido; no acredita enlace conTab. Root revisó físicamente zoom y Firefox recovery-focus-viewport sin recorte y leyó el delta completo de scripts. Formato focal final0bf94b verde.
