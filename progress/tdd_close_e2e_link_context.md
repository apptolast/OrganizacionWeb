# Ajuste de contexto en fixtures de cierre16

Producto17 final añade un segundo enlace de cierre deliberado. Reproducción focal histórica running.closes:010742 EXIT1, log close_e2e_link_red.log. El locator global de la región Sesión de trabajo resuelve dos enlaces; no es error de cierre HTTP.

Corrección mínima: cuatro llamadas en close-work-session.spec.mjs y una en close-work-session-ux.spec.mjs buscan el encabezado Estado de la sesión y su contenedor antes del enlace de cierre16. Ningún .first, eliminación de oráculo, timeout ni producto modificado. La aserción del encabezado Cerrar sesión de trabajo del lector sigue intacta.

Antes de ejecutar, copia de evidencia17 en progress/end_time_ux_evidence_preserved:260 archivos PNG/JSON de los tres motores y zoom real verificados por SHA256 (3aaf47), más reportes, bitácoras y copias de los dos tests17. No se movió ni borró evidencia; no se recorrieron rutas protegidas. El runner usa exclusivamente su lifecycle propio.

Foco de los dos archivos corregidos iniciado26374, siete casos esperados, log close_e2e_link_green.log. Resultado pendiente.

Resultado final66a9f7 EXIT0:7/7 en40,8s (519bb1). Pasan cuatro funcionales históricos y tres UX, incluido zoom nativo. Stack18696 retirado y18080 libre. Delta contrastado contra archivos originales comunes: únicamente las cinco acotaciones de contexto; encabezado y demás aserciones intactos. Sin nuevas suites/globales ni cambios productivos.
