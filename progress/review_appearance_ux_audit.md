# Auditoría UX20: resultado acotado

APPROVED para el alcance ejecutado sobre el producto congelado y el script
versionado en ed00ad4. Sin defecto observable pendiente en estos recorridos.
No sustituye los gates funcionales/seguridad/mutación ni certifica todos los
dispositivos o las treinta leyes como resultado universal.

| Corte final | Resultado real | Geometría / texto200 / modalidades | Axe | Feedback pendiente |
| --- | --- | --- | --- | --- |
| Chromium, stack33508 | EXIT0 d9bbbb, 6/6, 47.9s | 124 / 24 / 6 | 14 completos + 1 limitado | 4.5ms |
| Firefox, stack5744 | EXIT0 c780df, 4/4, 44.5s | 124 / 24 / 6 | 14 completos + 1 limitado | 17ms |
| WebKit, stack51444 | EXIT0 64eacf, 4/4, 40.7s | 124 / 24 / 6 | 14 completos + 1 limitado | 8ms |
| Zoom nativo Chromium, stack60724 | EXIT0 0b30aa, 1/1 | API zoom2, DPR1.5→3, ancho320 CSS | 1 completo | No aplica |

Cada axe final tiene cero violaciones dentro de su alcance. El limitado omite
únicamente color-contrast en colores forzados emulados. El original EXIT1
c407fc y sus tres avisos se conservan: axe leía los colores CSS del preview,
mientras Chromium pintaba blanco/amarillo sobre negro. Captura revisada por C
y root; no se afirma medición numérica de contraste de píxeles.

31 anchos entre320 y2560, a ambos lados de breakpoints, dos temas y estados
formulario/error; controles44px y límites horizontales. Texto duplicado mediante
fuentes calculadas, ocho combinaciones tema/ruta: Apariencia, sesión real,
historial y revisión semanal. SYSTEM responde al cambio de preferencia del
navegador sin PUT; reduced-motion medido. Teclado real Tab/Enter hasta Guardar,
outline visible y foco conservado; el tema global cambia tras200 y sólo una
escritura aun durante la espera. No generaliza ese teclado a todos los enlaces.

El PNG fullPage del zoom original quedó recortado por el mecanismo de captura;
permanece intacto y no acredita revisión visual. La captura viewport CDP
`organizationweb-e2e-60724/native-zoom/zoom200-native-viewport.png` muestra la
navegación/título/radios completos. Geometría y axe cubren los controles restantes;
no se atribuye a esa imagen contenido fuera de su viewport.

Las matrices y capturas están bajo `.e2e-work/appearance-audit/` en los stacks
citados, con manifest de hashes. Los cuatro cortes finales conservan348 entradas
de producto/scripts idénticas antes/después. La matriz de treinta principios
y el historial RED→GREEN están en `tdd_appearance_ux_audit.md`; capturas previas
copiadas a `appearance_ux_red_nominal`, `appearance_ux_red_geometry` y
`appearance_ux_red_text200`. No se sobreescriben resultados fallidos.

Chromium6/6 incluye el nominal de B con siembra independiente y limpieza.
Cada caso propio elimina sólo la preferencia de e2e-user mediante el SQL helper
que exige stack efímero y confirma count0. No API DELETE ni cambio productivo.
Todos los stacks propios fueron retirados;18080 libre. Sin nuevas ejecuciones.

Límites: emulación de medios y tamaños, sin móvil/tablet físicos, teclado
virtual, lector de pantalla real ni estudio con participantes. El zoom nativo
sólo se ejecutó en Chromium. No se repitió la suite global local; root coordina
el gate integrado de CI sobre el commit limpio.
