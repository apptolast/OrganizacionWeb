# Importación23: evidencia de navegador y revisión UX

Transporte de API **simulado** en `e2e/import-data-browser.spec.mjs`. Todas las rutas `/api/**` se interceptan; las ajenas a los fixtures se abortan y fallan el oráculo. Vite propio18123, sin backend ni puerto8080. Esta evidencia no demuestra la transacción SQL, proxy, conservación de14familias reales ni aceptación live.

## Resultados conservados

- `import_browser_initial.log`: RED físico, selector File294×24; input y botón nativo ampliados localmente a44px. Before/after76inputs iniciales idénticos antes del cambio, preservados en `import_browser_initial_integrity.json`.
- `import_browser_nominal_green.log`:1/1. `import_browser_keyboard_initial.log`:2/2, incluida cancelación y413 con foco de teclado.
- `import_browser_matrix_initial.log`:1/1, Chromium276 medidas y12axe sin violaciones. Anchuras320,359,360,361,390,480,600,699,700,701,768,820,1024,1099,1100,1101,1280,1440,1599,1600,1601,1920,2560. Estados inicial, validando, preview,413, incierto y confirmado; claro/oscuro. Breakpoints globales pertinentes360/700/1100/1600 con ambos lados. No se atribuyen pruebas de layouts de otras features a este recorrido.
- `import_browser_modalities_initial.log`: fallo del instrumento; cambiar sólo font-size raíz no amplía títulos enpx. `import_browser_modalities_second.log`:1/1, tamaños computados duplicados exactamente, reflow320/1280, claro/oscuro,2axe; foco voluntario hacia otro enlace seguido de blur conserva body. Forced-colors y reduced-motion activos; botón enfocado visible por hit-test, outline positivo, cero animaciones activas a768×360.
- `import_browser_other_engines.log`: Firefox4/4; WebKit3/4 y un fallo del interceptor: postDataBuffer null con File nativo. `import_browser_webkit_nominal.log`:1/1 después de limitar exclusivamente esa observación no disponible en WebKit. Se conserva comparación de bytes exactos en Chromium/Firefox; en WebKit se mantienen método/CSRF/hash, separación de gestos, foco y resultado. No se atribuye observación de bytes al motor que no los expone.
- `import_browser_native_zoom.log`:1/1, Chromium zoom real2, DPRdoble, viewport320CSS sin overflow. `zoom200-compositor.png` inspeccionada: texto, cantidades y Confirmar visibles; captura corresponde a la porción desplazada al botón, no a toda la página. PNG original y capturaCDP fromSurface:false se conservan, sin reescalado. Otros motores tienen texto200/reflow, no zoom nativo medido.

Artefactos originales bajo `.e2e-work/import-browser-*`: JSON de geometría/feedback/modalidades/zoom, capturas y contexto de fallos. La captura `dark-prepared-1280.png` fue inspeccionada: jerarquía coherente, Validar/Cancelar secundarios y Confirmar primario,14colecciones separadas, sin cortes. No se han realizado pruebas con lectores de pantalla, dispositivos táctiles físicos ni teclado virtual; no se infieren de emulación. Axe sin violaciones no certifica por sí solo accesibilidad integral. Las valoraciones cognitivas siguientes son revisión de diseño, no estudio de usuarios.

## Matriz de30principios

| Principio | Aplicación y evidencia | Resultado |
| --- | --- | --- |
| Atención selectiva | Archivo, Validar y luego Confirmar; captura preview1280 con una acción primaria. | Revisado visualmente |
| Carga cognitiva | Un File, datos de propietario/cantidades visibles; no recordar claves. | Verificado recorrido |
| Estética-usabilidad | Tokens de tema,44px, errores legibles; matriz yaxe. | Verificado técnico; usabilidad humana limitada |
| Posición en serie | Hoy primero, Importación última; validación antes de confirmación y Tab/Enter. | Verificado |
| Tendencia a la meta | No porcentaje inventado; cantidades derivadas de preview y recibo. | Verificado |
| Von Restorff | Confirmar sólido; Validar/Cancelar secundarios en preview; alertas con texto. | Revisado |
| Zeigarnik | Intención mínima recuperable, sin archivo persistido ni POST automático; tests UI. | Verificado DOM; backend pendiente |
| Fluir | Espera interrumpible de preview; no se inicia/pausa una sesión desde importar. | Verificado; sesión de trabajo no aplicable |
| Fragmentación |14filas por colección y secciones preview/resultado. | Revisado |
| Memoria de trabajo | Propietario, tamaño y cantidades presentes; conflictos conservan intención. | Verificado |
| Navaja de Occam | Un selector y acciones contextuales; no opciones de merge/CSV ajenas. | Revisado |
| Conectividad uniforme | Líneas sólo separan registros comparables de la tabla. | Revisado |
| Fitts | File y botones≥44px; límites y ausencia de solapes visibles, teclado real. | Verificado emulado; tacto físico pendiente |
| Hick | Elegir, validar, confirmar; recuperación sólo cuando corresponde. | Verificado |
| Jakob | Input File nativo, botones y enlaces nativos, foco visible. | Verificado tres motores |
| Semejanza | Temas y estados usan tokens compartidos y semántica uniforme. | Verificado |
| Miller | Agrupación semántica por colección, no corte arbitrario a siete. | Revisado |
| Parkinson | No cambia runningSince ni duración; warning histórico en tests UI. | Verificado DOM; persistencia real pendiente |
| Postel | FileJSONv1/Unicode y32MiB; cliente valida envelope sin reinterpretar14registros. | Cliente verificado; backend separado |
| Proximidad | Label asociado al File; explicación/error en la misma vista. | Verificado axe/visual |
| Prägnanz | Estados en texto: Validando, preview, incierto, confirmado. | Verificado |
| Región común | Preview y recibo agrupan metadatos y cantidades. | Revisado |
| Tesler | Hash e idempotencia internos; usuario decide confirmar/revisar resultado. | Verificado DOM |
| Modelo mental | Importar incorpora sin sobrescribir; conflictos impiden toda importación. | Copy y UI verificados; TX separada |
| Usuario activo | Estado inicial explica copia propia y siguiente gesto. | Revisado, sin estudio de usuarios |
| Pareto | JSON propio y recorrido integral; sin configurador innecesario. | Priorización explícita, no porcentajes de uso |
| Fin de pico | Recibo cierto y tabla; fallo de refresh no deshace éxito. | Verificado tests UI |
| Sesgo cognitivo | Cantidades neutrales; no culpabiliza interrupción ni inventa progreso. | Revisado |
| Sobrecarga de opciones | Recuperación contextual y corrección manual de rechazo definitivo. | Verificado |
| Doherty | MutationObserver mide feedback<400ms con preview retenida en ambos temas. | Verificado en matriz, sin promesa de latencia de red |

Pendientes reales: integración API/PG/proxy del backend final y campaña frontend del scope aprobado. Las evidencias de navegador ya descritas no esperan backend y no se volverán a etiquetar como E2E real.

## Corrección visual final y gates

Root detectó roturas de palabras en la captura móvil inicial, aunque no había overflow. Se conservó esa captura y se cambió sólo presentación: tabla automática,46% para colección, cabeceras Total/Nuevos/Iguales y ayuda explícita. `import_browser_legibility.log` conserva6/6: matriz y texto ampliado/modos en los tres motores;828medidas y42axe en total. La nueva `light-prepared-320.png` fue inspeccionada por B y root: Disponibilidad/Personalización y cabeceras completas, saltos entre palabras y acciones44px. El criterio visual está aprobado. La captura de zoom anterior acredita el mecanismo nativo y corresponde al texto previo de cabeceras; no se presenta como captura del cambio posterior.

Regresión original `import_frontend_final_global.log`:2407/2408, único oráculo heredado de Exportación última. Se mantiene Hoy primero y ahora Exportación penúltima/Importación última, con demás aserciones intactas. `import_frontend_final_repaired_global.log`:2408/2408 en60suites, EXIT0. `import_frontend_final_lint.log`: ESLint y Prettier EXIT0. `import_frontend_final_build.log`: TypeScript y Vite EXIT0. No Java ni cambio de dependencias.
