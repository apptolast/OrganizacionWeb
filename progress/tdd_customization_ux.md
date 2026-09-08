# UX21: primer corte geométrico

Autoría CSS/test de C en aislado, revisión independiente de root pendiente. B mantiene autoría JS y cedió exclusivamente styles.scss; no se modifica COMMON ni TS/TSX. Este corte no declara la matriz30 completa.

Un único test en customization-ux.spec.mjs prepara cuatro definiciones PROJECT por API real y examina configuración abierta y valores tipados. Reutiliza authenticated-test, runner y axe existentes.31 anchos320–2560,62 mediciones de estado/ancho y868 cajas de control; sin overflow horizontal ni solapamiento, objetivos44×44. Axe completo en320 para ambos estados,0violaciones. No nuevo framework ni dependencias.

Primer RED EXIT1 de10a5, stack68480: guardia encontró checkbox13px. Su atribución como objetivo táctil era imprecisa: el objetivo incluye el label. La misma geometría preservada registra además textbox Etiqueta189×27 y select Tipo111×19, controles realmente menores de44px. Se conserva log/error y captura original en customization_ux_geometry_red_evidence. No se inventa una medida original del label.

CSS local incorpora grid minmax(0,1fr), espacios12, fieldsets/labels agrupados, inputs/selects/botones44px, texto adaptable y tokens actuales para temas, alertas y acciones. Primer pase provisional EXIT0 618fc3, stack18032,1/1: usaba checkbox44; se preserva íntegro en customization_ux_geometry_provisional_evidence y no se presenta como diseño final.

Precisión de root aplicada: checkbox nativo20px y label mínimo44px con padding; el oráculo mide el label asociado y conserva ancho del glifo por separado. Dos clics en el borde derecho del label alternan la casilla, acreditando el área clicable real. Los restantes controles mantienen medición de su propia caja. Mismo test final EXIT0 d31dff, stack27140,1/1 PASS7.2s/9.2sPW.578 inputs antes/después idénticos (498c10). Logs/snapshots separados customization_ux_geometry_target*. Lifecycle retiró sólo stack/red/volumen propios;18080 libre.

Fuente CSS SHA256 `04EC25C3A6718576CDABF06003472D22436A1D959D0F465660400496DF87661E`; test `69E66DFA526343E0F1ECAA5C7DE5B2D494D006F6B96B097FD5FD1D583A31F681`. Compilación de imagen y formato pasan en el runner; no se atribuye lint/global nuevo. Evidencia final copiada a customization_ux_geometry_final_evidence antes de cualquier nueva corrida.

Inspección C de typed-values-320: agrupación y campos legibles, sin recorte horizontal. La captura fullPage no acredita foco ni posición real del skip-link en viewport; el artefacto muestra ese enlace desplazado y requiere medición de viewport antes de atribuir o descartar oclusión. El oráculo de foco todavía está pendiente del JS final de B. No se extrapola una captura a otras superficies.

Pendientes: JS final y recuperación GETschema; validaciones/mensajes/foco de B; revisión independiente CSS de root; texto200/zoom nativo, temas/forced-colors/reduced-motion, Firefox/WebKit y matriz30 con referencias reales. Dispositivos físicos, teclado virtual y comprensión humana no quedan acreditados. No se repiten matrices históricas ni campañas globales.

## Texto200 y foco sobre freeze funcional

Root aprobó CSS078155d. Copia final14JS B1126a1 (manifest535514…AB571) y único StorefinalB5D1…DDEB2 autorizado ddbea9. Texto200/12TEXT Unicode inicialmente GREEN5cc5c1:1/1,6.9s/8.9sPW, ambos temas y dos superficies a320/768/1440,4axe0. Crear/Guardar mantienen posición final enfieldset.578inputs iguales7913cc, evidencia preservada customization_ux_text200_evidence. Skiplink viewport0:computedtop-100,rectbottom-55,focusedfalse,activeH1; captura deviewport inspeccionada sin enlace superpuesto. No cambio CSS global.

Siguiente único caso teclado usa Tab/ShiftTab/Enter, PUT real retenido para medir espera y recuperar foco de Guardar vista, luego PUTvalores confirmado200 por servidor cuya respuesta se aborta, GET manual y foco al encabezado. Primer RED físico b76156: recuperación termina pero h2 nofocused. Guardar vista sí supera el oráculo.578inputs sin cambios; error-context preservado.

Diagnóstico mismo caso, sin cambiar aserciones ni producto: EXIT1 14c758. Eventos en customization_ux_keyboard_diagnostic_evidence/recovery-diagnostic.json: keydown/click de Recargar guardado, luego focusout de botón disabled=true yconnected=true,activeBODY; ningún foco voluntario ydocument.hasFocus=true. B debe distinguir ese blur automático del movimiento voluntario. Se informaron root/B, no se editó TS/TSX. Stack65264 retirado. El caso queda RED hasta corrección; modalidades posteriores no se atribuyen todavía.

## Cierre de modalidades y motores (8 de septiembre)

B corrigió foco164 con matches(':disabled'), sin cambiar CSS. Copia de dos TSX 7fbe7f registrada en customization_focus164_copy.json; el manifiesto anterior de14 archivos sigue siendo histórico, no representa esos dos bytes nuevos. Mismo caso de teclado GREEN accb98, stack35092,1/1,578 inputs iguales903426. Espera visible en3.5ms antes de liberar respuesta; recuperación manual devuelve foco a Campos personales (outline3px), conserva el borrador confirmado durable y no anuncia guardado tras respuesta perdida. GET acredita datos, no atribución de intención. Se conserva el RED anterior.

Zoom nativo Chromium200: GREEN bdd7de, stack69452,1/1. chrome.tabs.getZoom=2 y DPR1.5→3, viewport320; sin CSSzoom. Captura CDP de viewport y axe completo0 preservados en customization_ux_zoom_evidence. No screenshot fullPage. La prueba headed queda incluida en CI: workflow existente usa xvfb-run. No se acredita zoom nativo Firefox/WebKit.

Modalidades Chromium: GREEN89155f, stack45968,1/1. SYSTEM oscuro, reduced-motion y forced-colors a320/1440, seis mediciones; BOOLEANfalse conservado. Tres axe completos0, sin excluir color-contrast. Evidencia customization_ux_media_evidence.

Firefox: dos ejecuciones de preparación EXIT1 f979fe/12bf96 terminaron No tests found; no son RED de producto. El shell Windows dividía el filtro con espacios. Mismo mecanismo run() con filtro de una palabra native enumera cuatro casos (c9157b). Ejecución efectiva f54c3a EXIT0, stack52476,4/4 en25.5s;579 inputs iguales. Geometría, texto200, modalidades y teclado completo pasan. Evidencia customization_ux_firefox_evidence; originales de preparación intactos.

WebKit: a0a618 EXIT1, stack56260,3/4 en25.7s. Pasan geometría, texto200 y modalidades. Teclado alcanza botones/casilla, guarda vista y devuelve foco a región; luego tabTo no alcanza el enlace del proyecto (línea363). No acredita teclado completo ni recuperación de valores en este motor. Error-context y log originales preservados en customization_ux_webkit_evidence. Sin traza completa de los100Tab no se atribuye una causa universal del navegador. No se alteró producto ni se añadieron skips, reintentos o timeouts. El resultado sigue fallido y requiere aceptación explícita de este límite por revisión.

CI usa playwright.config.mjs por defecto: los cinco casos UX21, incluidos teclado y zoom Chromium, permanecen incluidos, junto a los dos nominales UI. customization-browser.config.mjs es configuración operativa separada para motores; Firefox/WebKit se ejecutaron con --grep-invert native porque zoom usa explícitamente Chromium. No se presenta esa exclusión de modalidad como validación de zoom en otros motores. No hubo campaña E2E global local nueva. Todos los lifecycle retiraron sus recursos;18080 libre.

## Ensayo separado autorizado de recuperación WebKit

Antecedente leído en history-ux.spec.mjs25–30 y autorización root: sólo en win32+WebKit se abre el enlace de proyecto con clic, anotado limitation. Botones, checkbox, edición y recuperación mantienen teclado y todos sus oráculos, sin focus() ni skips. Original a0a618 y originalspec intactos. Único ensayo del mismo caso: EXIT0 ea4a15, stack52540,1/1 PASS4.2s/5.4sPW;579 inputs antes/después idénticos c7801b. Evidencia customization_ux_webkit_recovery_evidence. Acredita recuperación164 en WebKit mediante recorrido parcialmente de teclado; NO navegación del enlace conTab. No se suma al3/4 original ni se lo renombra GREEN.

Formato focal final0bf94b EXIT0 usando Prettier ya instalado en COMMON, sólo lectura sobre los dos scripts del aislado. Los intentos previos desde raíz/ frontend aislado no encontraron ese ejecutable, no son pruebas de producto. Root revisó independientemente zoom y Firefox recovery-focus-viewport (legibles, foco visible), y el delta completo de scripts sin otro hallazgo.18080 y stack52540 liberados. Sin nueva campaña global.
