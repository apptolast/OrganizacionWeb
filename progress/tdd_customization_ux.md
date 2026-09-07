# UX21: primer corte geométrico

Autoría CSS/test de C en aislado, revisión independiente de root pendiente. B mantiene autoría JS y cedió exclusivamente styles.scss; no se modifica COMMON ni TS/TSX. Este corte no declara la matriz30 completa.

Un único test en customization-ux.spec.mjs prepara cuatro definiciones PROJECT por API real y examina configuración abierta y valores tipados. Reutiliza authenticated-test, runner y axe existentes.31 anchos320–2560,62 mediciones de estado/ancho y868 cajas de control; sin overflow horizontal ni solapamiento, objetivos44×44. Axe completo en320 para ambos estados,0violaciones. No nuevo framework ni dependencias.

Primer RED EXIT1 de10a5, stack68480: guardia encontró checkbox13px. Su atribución como objetivo táctil era imprecisa: el objetivo incluye el label. La misma geometría preservada registra además textbox Etiqueta189×27 y select Tipo111×19, controles realmente menores de44px. Se conserva log/error y captura original en customization_ux_geometry_red_evidence. No se inventa una medida original del label.

CSS local incorpora grid minmax(0,1fr), espacios12, fieldsets/labels agrupados, inputs/selects/botones44px, texto adaptable y tokens actuales para temas, alertas y acciones. Primer pase provisional EXIT0 618fc3, stack18032,1/1: usaba checkbox44; se preserva íntegro en customization_ux_geometry_provisional_evidence y no se presenta como diseño final.

Precisión de root aplicada: checkbox nativo20px y label mínimo44px con padding; el oráculo mide el label asociado y conserva ancho del glifo por separado. Dos clics en el borde derecho del label alternan la casilla, acreditando el área clicable real. Los restantes controles mantienen medición de su propia caja. Mismo test final EXIT0 d31dff, stack27140,1/1 PASS7.2s/9.2sPW.578 inputs antes/después idénticos (498c10). Logs/snapshots separados customization_ux_geometry_target*. Lifecycle retiró sólo stack/red/volumen propios;18080 libre.

Fuente CSS SHA256 `04EC25C3A6718576CDABF06003472D22436A1D959D0F465660400496DF87661E`; test `69E66DFA526343E0F1ECAA5C7DE5B2D494D006F6B96B097FD5FD1D583A31F681`. Compilación de imagen y formato pasan en el runner; no se atribuye lint/global nuevo. Evidencia final copiada a customization_ux_geometry_final_evidence antes de cualquier nueva corrida.

Inspección C de typed-values-320: agrupación y campos legibles, sin recorte horizontal. La captura fullPage no acredita foco ni posición real del skip-link en viewport; el artefacto muestra ese enlace desplazado y requiere medición de viewport antes de atribuir o descartar oclusión. El oráculo de foco todavía está pendiente del JS final de B. No se extrapola una captura a otras superficies.

Pendientes: JS final y recuperación GETschema; validaciones/mensajes/foco de B; revisión independiente CSS de root; texto200/zoom nativo, temas/forced-colors/reduced-motion, Firefox/WebKit y matriz30 con referencias reales. Dispositivos físicos, teclado virtual y comprensión humana no quedan acreditados. No se repiten matrices históricas ni campañas globales.
