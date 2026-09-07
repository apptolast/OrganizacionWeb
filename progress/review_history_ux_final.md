# Revisión final UX del historial

Paquete C cf351e4 integrado en8092443. Root revisó diff de estilos y metodología9fdcea/3897b6/0ce192 y verificó los cuatro hashes del manifiesto. El único ajuste productivo posterior al primer UX es scroll-margin-block:16px en enlaces y summary del historial; corrige la ocultación observada de foco en Firefox sin cambiar navegación o datos.

Evidencia compuesta:495 mediciones y27 análisis axe sin violaciones. No equivale a una campaña global ni a dispositivos físicos. Chromium y Firefox acreditan teclado real; WebKit Windows acredita geometría mediante clic y texto200, pero sus intentos de Tab/AltTab no acreditaron navegación de enlaces. La limitación está anotada en el runner y en ux_history.md; no se cambian tabindex ni se simula foco para conseguir un resultado verde. El zoom nativo200 sólo se acredita en Chromium, con getZoom2 y DPR duplicado.

Root inspeccionó visualmente empty-320-viewport.png del zoom final: texto y controles legibles, sin recorte horizontal en esa captura; las primeras muestras details320/list1440 ya fueron revisadas. No se afirma inspección visual de todas las imágenes. Los141 artefactos del manifiesto se copiaron desde el árbol aislado a COMMON, con comprobación de SHA antes y después (50cac4), sin borrar originales ni tocar directorios protegidos.

Dictamen: paquete aceptado con límites explícitos de port WebKit, dispositivos reales, lector de pantalla y estudios de uso humano. La matriz de30 principios documenta decisiones y evidencia aplicable, no una certificación universal. El E2E global de integración queda pendiente y separado de este dictamen.
