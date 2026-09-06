# Revisión de recorridos reales y UX14

Aprobado para CI integrada, sujeto al dictamen final de UX y mutación. Root revisó cuatro scripts en c60573/6292f9/0fbf8d: tres recorridos funcionales con API/PG reales, cuatro estados responsive en31anchos, ampliación de texto200% y zoom nativo Chromium200% mediante perfil/extensión locales aislados. La pérdida de respuesta se inyecta sólo después del201 real y la recuperación usa GET real. No se simula persistencia.

Se verifican bounds,44px, ausencia de intersecciones entre controles, foco, feedback menor400ms y axe sin violaciones. El texto ampliado se limita al contenido principal; no se confunde con zoom nativo. Este último observa zoom2 y DPR duplicado, viewport320, con captura CDP para conservar el documento completo. CI ya dispone de xvfb para el contexto headful aislado.

Evidencia autor: nominal5d91ac, recuperacióne53689, otra tarea/logout2450d0, UX corregido540246; Firefox5/5a884d3 y WebKit5/5815ca6. Zoom nativo corregido3fb7661/1, cuatro estados/axe sin violaciones. Sintaxis y formato391b8a. Los hashes de los cuatro scripts están en start_work_e2e_freeze.json y root los verificó antes de integrar. Resultados detallados y límites en tdd_start_work_e2e.md y ux_start_work_session.md.

Root inspeccionó capturas móvil inicial, desktop confirmado y móvil corregido. Detectó ausencia obsoleta, resuelta mediante RED17bf9f y fixf745c94; el skiplink aparentemente superpuesto en fullPage estaba fuera del viewport según coordenadas y captura real. No se alteró CSS global por ese artefacto.

No se acreditan dispositivos físicos, teclado virtual, lectores de pantalla ni validación psicológica humana de las30leyes. La evidencia automatizada no es ausencia universal de errores. No se declara14 terminada antes de cerrar todos sus gates.
