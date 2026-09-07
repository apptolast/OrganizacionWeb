# E2E y UX14 — recorridos reales

Corte inicial: producto frontend congelado y Stryker inicial84,80%. Esta fase encontró y corrigió un único párrafo contradictorio, descrito abajo con su RED/GREEN; el resto de UI permanece igual. Backend wiring75/75 GREEN104b16 y freeze del autor; smoke reinicio/publicador es trabajo separado de backend y no se duplica aquí.

Runner autorizado `node scripts/e2e.mjs`, PostgreSQL/API/web propios, puerto18080 verificado libre antes del primer arranque. Contexto protegido excluido mediante `.dockerignore` existente; no acceso ni limpieza de destinos protegidos. El runner retira sólo su stack y scratch propios. Snapshot294archivos en `start_work_e2e_snapshot.json`, captura23be1c; incluye producción Java, frontend, build y test nominal. Docker COPY/context DONE con caché del contenido actual, no compilación Gradle local.

## Ciclos individuales

1. `e2e/start-work-session.spec.mjs`: inicio explícito25min, DTO7/Location, fila PG running/duración exacta, GETid/key iguales al recibo y recarga descubre activa sin nuevoPOST. Primera ejecución real GREEN5d91ac:1/1,4,1s test/7,1s Playwright, EXIT0 runner. La implementación ya existía: no se declara RED artificial. Log `start_work_e2e_first.log`, stack63284 retirado correctamente.
2. Pérdida de ACK: interceptar sólo entrega de respuesta DESPUÉS de POST real201, conservar duración/key, comprobar mediante GET real y no repetirPOST. En ejecución al escribir este corte, sesión93388; log `start_work_e2e_recovery.log`. No se simula persistencia ni se atribuye el fallo de transporte al servidor.

Pendientes de esta fase: activa de otra tarea, errores/recuperación visibles, matriz30principios con mediciones/capturas, ampliación y motores. El informe unitario `ux_start_work_session.md` conserva esos límites hasta que exista evidencia navegador. No se atribuyen dispositivos físicos, teclado virtual ni comprensión humana a estas pruebas.

Ciclo2: primera invocación4808ed no seleccionó pruebas por filtro con espacios; no ejecutó oráculo ni representa RED de producto. Filtro corregido lost.acknowledgement ejecutó además accidentalmente un caso heredado11 de reinicio: e53689,2/2 (heredado18,6s + nuevo14recuperación4,8s). La evidencia de14 es sólo su caso; futuras invocaciones incluyen archivo explícito para acotar selección.

Ciclo3: activa propia de otra tarea, enlace al contexto original y logout confirmado retira datos sin nuevoPOST. Primera ejecución real GREEN2450d0,1/1,2,8s. La retirada inmediata antes de response está acreditada por integración SessionGate, no por este E2E después de logout confirmado.

Ciclo4: e2e/start-work-session-ux.spec.mjs, flujo ausencia→envío retenido→503de entrega→check real→confirmado;31anchos incluyendo ambos lados de breakpoints existentes, altura400 a768,44px/bounds/nointersecciones,4axe y8capturas previstas. Timeout120s declarado desde diseño como los recorridos UX13; no aumento posterior para ocultar fallo. POST se confirma realmente antes de inyectar503 de transporte; no se declara error producido por PG. En ejecución56081 al escribir, log start_work_e2e_ux.log; todavía sin resultado.

Ciclo4 inicial GREEN0ef7d6: 1/1,124 medidas y4 análisis axe sin violaciones; feedback5,7ms. La captura incertidumbre reveló una contradicción real: ausencia previa al POST junto a resultado incierto. Se conserva íntegra en `.e2e-work/start-work-real/chromium/ux-initial`. Reviewer reprodujo RED17bf9f con petición pendiente y503; mínimo de producción sólo condiciona el párrafo de ausencia a !busy&&!uncertain, sin cambiar elegibilidad, clave ni envío. GREENcfe3a8 del oráculo unitario completo. El score84,80% corresponde al corte anterior; no se atribuye al nuevo render sin medición posterior.

Repetición del mismo flujo UX después del arreglo: GREEN540246,1/1,124 medidas/4axe sin violaciones,feedback6,5ms. Capturas y JSON en `.e2e-work/start-work-real/chromium/ux`. La captura viewport incertidumbre confirma retirada del mensaje anterior. Skiplink sin foco tiene top=-100,bottom=-55 (viewport900,scrollY765), fuera del viewport: su aparición en captura fullPage no demuestra solapamiento visible y no motivó cambios globales de CSS. Root inspeccionó viewport y aceptó esta conclusión. Runner64780 retirado.

Ciclo5: texto del contenido principal al200%, mismo flujo público en320/768/1440; factor tipográfico2 comprobado, no se presenta como zoom nativo del navegador ni escalado de navegación global. En ejecución15238 al escribir, log `start_work_e2e_text.log`; sin resultado aún.

Ciclo5 inicialmente GREEN13e501:1/1,12medidas/4axe, factor2 del texto principal verificado. Capturas reales `chromium/text200`, incluida incertidumbre320 inspeccionada.

Ciclo6 zoom nativo: primer oráculo GREEN57e4f9,4estados/4axe, zoom2 y DPR1,5→3 con viewport320. La inspección detectó que Playwright recortaba la captura fullPage al zoom2 y algunos viewport quedaban vacíos tras scroll; no se interpretó como recorte del producto, cuyas medidas pasaban. Se reutilizó el capturador CDP ya presente en13 y scrollTo(0,0) para obtener evidencia fiable. Repetición GREEN3fb766,1/1; capturas finales `.e2e-work/start-work-native/organizationweb-e2e-4660/evidence`, confirmed320 completo936×10251 inspeccionado sin recorte. Intento inicial58244 conservado. No cambio de aserciones de diseño, foco, zoom ni axe.

Regresión de motores con selección explícita de archivos funcional/UX/texto: Firefox5/5GREENa884d3 (1,1min) y WebKit5/5GREEN815ca6 (38,3s). Cada motor aporta124medidas UX+12texto y8axe sin violaciones. Native se ejecuta sólo Chromium. Comandos completos en `start_work_e2e_firefox.log`, `start_work_e2e_webkit.log`, `start_work_e2e_native_capture.log`. Todos los stacks propios retirados por runner, sin tocar servicios ajenos ni protegidos.

Freeze de código:4specs, node--check y Prettiercheck GREEN391b8a, hashes SHA256 en `start_work_e2e_freeze.json`. Matriz30 actualizada en `ux_start_work_session.md`, con geometría final412medidas y28axe, límites explícitos. No se cuentan repeticiones de evidencia como nuevos casos funcionales, ni se atribuye comprensión humana o dispositivo físico a emulación. Padre/reviewer mantienen congelados frontend/tests/config para Stryker final; no se han modificado. No nuevos globales, campañas de mutación ni cambios backend en este paquete.
