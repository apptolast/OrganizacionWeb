# TDD frontend — today

Contrato aprobado a127747; autoría frontend excepto E2E. Ponytail full/Caveman lite. No init global concurrente ni mutación. Baseline coordinado Init94736; no se reproduce como ejecución nueva.

## Ciclos

1. @s1/@s21 cliente GET privado y snapshot vacío: RED 8562df (módulo ausente); implementación mínima y GREEN focal 1 prueba. Sin dependencias nuevas.

## Pendiente

Validación cerrada, pantalla y actualización, privacidad, navegación y regresión. No se solicita revisión final todavía.
2. HTTP no200: RED9cc96b (3 respuestas confirmadas indebidamente), GREENf23200 (4).
3. Esquema superior cerrado: RED47678e (25), GREEN698903 (29). Reutilización por export de validadores existentes, sin modificar su comportamiento.
4. Items/DTO9: RED964631 (7), GREEN3065cf (36). Intento previo de extraer fixture tuvo error de script corregido antes del RED conductual; no se atribuye como evidencia de producto.
5. Coherencia de día vacío/presupuesto: REDee70be (17), GREEN412365 (55).
6. Intersecciones/referencias/resumen de agenda: RED85c5cd (7), GREEN114de5 (62).
7. Orden: RED55d189 (1), GREEN71f06e (63).
8. Positivos de agenda, zona histórica no disponible en cliente, fallback y fronteras current/next: inicialmente GREEN (69), comportamiento heredado de los ciclos anteriores. Se conserva createdAt posterior a serverNow como caso legítimo.
9. Enum sin coerción (hallazgo incremental root): REDec14a0, GREENfdd88c,70 API.
10. UI carga/vacío: REDd5c815 por módulo ausente, GREENbae9cb1.
11. Agenda legible/cierre real/texto seguro: REDaff180, GREENd6acdc2.
12. Fallback con capacidad desconocida: RED8a45d3, GREEN0190034.
13. Intl de zona efectiva/histórica ausente: RED033c82 (incluye excepción de render esperada), GREEN1d14e06.
14. Fallo inicial/reintento: RED6a7ed3 (rechazo sin manejar previo), GREEN34536b8.
15. Actualización manual/foco conservado: REDd279f1, GREEN73fd509.
16. JSON posterior a headers y401 entregado tras desmontaje: RED96f285, GREEN43dc7611; señal abortada impide observador de sesión obsoleto.
17. Visibilidad/foco coalescidos: REDd70588 (idle), GREEN2d5f4b14; manual/inicial ya conservaban una petición.
18. Frontera futura única: REDa6c4c9, GREEN9210d717.
19. Ocultar/recuperar con reloj monotónico: RED44b028, GREEN375c7e18.
20. Rollover sustituye JSON pendiente y retira día anterior, éxito/fallo: RED139bdc, GREEN9415a220.
21. Fallo de refresco explícitamente sin actualizar, sin reprogramar frontera vencida y recuperación foco: RED9d1a04; GREEN focal21.

## Integración de navegación y seguimiento

22. Raíz Hoy, captura explícita y rutas desconocidas: REDe94861 (5); primer ajuste dejó tres casos rojos por una sustitución textual incompleta, corregida; GREEN3cc31d26 UI.
23. Workspace con tres secciones y skip link conservado: REDb15792 (3), GREEN78f7b229 UI.
24. Enlace de captura en vacío: REDb70707; ambos href migrados; GREEN3b44ee35 pruebas anteriores de lectura. Secuencias posteriores desde lista llena y vacía inicialmente GREENbf216c33 UI.
25. Regresión de captura/autenticación: fixtures de captura arrancan ahora en /proyectos/nuevo, conservando aserciones de campos, POST, CSRF, errores y foco. El guard histórico de retorno no reconocía esa ruta (REDel primer corte ab35e4); se añadió la ruta exacta. GREEN976bf0 autenticación. Nueve expectativas de retorno inválido conservan raíz pero esperan Hoy, no el formulario movido.
26. Logout y nuevo login reales mediante SessionGate: inicialmente GREENf0b227100 API/UI, heredado del desmontaje y aborto ya comprobados. No se falsea RED.
27. Recuperación de pestaña después de medianoche: REDace92e; deadline monotónico del snapshot retira el día anterior antes de la lectura nueva; GREEN25958531 UI.
28. Agenda completa de21 reservas con cruces en ambos extremos y exceso: inicialmente GREEN1d442771 API; verifica intersecciones y cierre real sin truncamiento.
29. Primera regresión global:1305 PASS/5 FAIL, todos ligados a expectativas anteriores de captura en raíz o sección Proyectos activa en raíz. Cuatro retornos inválidos de disponibilidad esperan Hoy; el recorrido de salida de captura a disponibilidad arranca en /proyectos/nuevo. Conserva el resto de sus oráculos; foco80 disponibilidad GREENdc5361. Regresión completa posterior fc3e46:1313 PASS,0 FAIL,0 omitidos.
30. Aclaración del coordinador @s34/@s35: sesión ya autenticada conserva ruta local desconocida y App muestra404 sin consulta de entidad; login desde estado anónimo conserva normalización histórica a raíz. RED5cbbed para entrada autenticada, variante anónima inicialmente verde. La condición de normalización exige ahora sesión previa anónima. Los seis casos históricos de retorno parcialmente válido pasan explícitamente por login, conservando sus expectativas de descarte a raíz. GREENd52450:108 UI+autenticación. No se elimina el guard isPrivateRoute ni la comprobación de generación.

## Mapa para revisión independiente

| Contrato | Evidencia frontend |
| --- | --- |
| @s1–9, s11 (representación de respuesta) | today-api.test.ts: GET privado, snapshot vacío, capacidad y fallback, fronteras current, DTO histórico, colección21 con cruces. Cálculo/aislamiento real PostgreSQL corresponde al autor backend; estos tests no lo autoaprueban. |
| @s15–17 | today-api.test.ts: rechazo HTTP, objeto15 cerrado, item3/block9 heredado, enum sin coerción, coherencia vacía y agenda, duplicados/orden/referencias/suma. Fallo de presentación se verifica en today.test.tsx. |
| @s18–20 | today.test.tsx: agenda legible con nombres/texto seguro y fecha de cierre real; dos fuentes fallback; Intl ausente para zona efectiva e histórica. |
| @s21–23 | Carga antes de vacío confirmado, errores de red/JSON y reintento sin persistencia, refresh manual conserva snapshot fechado. |
| @s24–27 | Tres estados de petición para coalescer foco/visibilidad; tres próximas fronteras; reloj del dispositivo alterado sin cambiar espera monotónica; ocultar cancela y recuperación reconstruye. |
| @s28–30 | Rollover sustituye JSON pendiente (después de headers), éxito/fallo del nuevo día, regreso tras medianoche, fallo dentro del día sin reprogramar frontera vencida. JSON/401 entregados tras desmontaje conservan siguiente pantalla y observador de acceso. |
| @s31 | SessionGate con logout pendiente, agenda retirada inmediatamente, nuevo login y respuesta de agenda nueva diferida. |
| @s32–35 | Rutas raíz/captura/desconocidas, enlaces desde lista/vacío y distinción de autenticación aclarada. Deep links anteriores conservan cobertura de authentication, availability, read-projects, edit-project, create-task y task-blocks. |
| @s36–37 | Navegación/breadcrumb/skip link; manual por Enter y enlace de tarea con foco elegido conservado tras respuesta; fallo de frontera mantiene foco. |
| @s38 | today.scss aplica agenda vertical, ancho de lectura acotado, texto con wrap y controles44px. Evidencia de dimensiones/zoom/motores/30 leyes a cargo del E2E independiente. JSDOM no acredita layout ni dispositivos físicos. |

## Límites deliberados y alcance

Sin dependencias, stores persistentes, polling, WebSocket ni reloj de trabajo. Se reutiliza el DTO9 y sus validadores exportados sin alterar sus reglas. La espera usa performance.now respecto a serverNow recibido, con una frontera vigente; no se promete observar ediciones externas antes de frontera, foco o actualización manual. La precisión del servidor se conserva hasta microsegundos según contrato heredado; no se compara createdAt con serverNow ni se consulta TZDB del navegador para aceptar datos.

Archivos nuevos: today-api.ts, today.tsx, today.scss, sus dos suites y today-fixture.ts (sólo helper de pruebas). Shared: región App anterior a CreateProjectScreen, Workspace, dos enlaces de project-reader, dos condiciones del guard de use-session y exports de schedule-block-api. navigation.tsx, backend, E2E, arnés, contratos y metadata no se modifican desde esta autoría. Cambios de tests anteriores limitados a la migración o aclaración de rutas descrita.

Pendiente de judge independiente y E2E/UX. No se ha ejecutado mutación ni se declara done.

## Corte entregado a judge

Producción congelada tras la aclaración @s35. Formato focal aplicado; ESLint de todos los archivos tocados sin errores. `pnpm build` (incluye tsc --noEmit) GREENc9040d:49 módulos, build Vite358ms. Regresión final `pnpm exec vitest run --reporter=json --outputFile=reports/today-frontend-regression.json`: EXIT0 996e1e,1315 PASS,0 FAIL,0 omitidos; reporte leído17597d. Las suites nuevas suman71 API y35 UI, las demás conservan/regresan contratos anteriores.

El autor E2E independiente retomó captura y matriz de30 filas en progress/tdd_today_e2e.md; esa evidencia todavía no se atribuye a esta ejecución. No se ejecutó mutación. Este corte solicita revisión, no autoaprobación ni done.

## Correcciones solicitadas por revisión independiente

31. Root detectó pérdida del rollover después de fallar una frontera de bloque. Test `a failed block boundary still retires the old agenda at midnight without focus`: RED14e63f, GREEN9541d4. El efecto conserva un timeout al dayEnd tras fallo; al reconstruir excluye fronteras ya transcurridas usando tiempo monotónico. La expectativa antigua de cero timers después del fallo era demasiado restrictiva y ocultaba el requisito del cambio de día: ahora exige uno y sigue comprobando que no se repite la frontera vencida. Suite UI36 GREENae8e18.
32. Judge backend identificó variante de la misma obligación con GET aún pendiente en lugar de rechazo. Test `a pending block-boundary request is replaced at midnight`: RED934607, GREENdc9f61. La nueva generación reconstruye el próximo timeout; no queda sin frontera mientras espera red. A medianoche cancela la petición anterior, retira snapshot y descarta su respuesta posterior. @s25 conserva el oráculo de un único timer y espera el próximo límite conocido mientras siga habiendo día vigente, cero después de retirar ese día.

Corte corregido:108 API/UI (71+37) GREEN8cb4bb; formato, ESLint focal y build+types GREEN294fcd,49 módulos y Vite278ms. Sólo cambiaron today.tsx y today.test.tsx respecto al corte1315 anterior; no se presenta esa regresión global previa como ejecución de este nuevo corte. Producción vuelve a estar congelada para re-review y E2E. La revisión backend independiente se cerró APPROVED en progress/review_today_backend.md sobre283 pruebas del autor, sin autoaprobar estas correcciones UI.

33. Último hallazgo independiente (a4834e), ocultar/volver con GET manual pendiente: RED388bcb conserva indebidamente el día anterior; GREENda5ae1. La recuperación de visibilidad reconstruye sólo el deadline de retirada sin duplicar GET ni reactivar horarios de bloque antiguos. El oráculo comprueba dos lecturas antes del vencimiento, ninguna al horario viejo, tercera al nuevo día, abort de la anterior y descarte de su entrega tardía.
34. Hermano @s27, recuperación visible inicia una lectura desde idle que sigue pendiente: RED36601f detectó rearmado prematuro de frontera del snapshot anterior. La marca de espera de snapshot nuevo se retira sólo al recibirlo y mantiene únicamente dayEnd mientras tanto. GREENfba8b4:110 API/UI (71+39). La misma comprobación conserva coalescing y cancela el deadline al volver a ocultar.

Nuevo freeze después de formato, ESLint focal y build+types GREEN7cf936 (49 módulos,293ms). Sólo today.tsx/tests respecto al checkpoint de integración. Init35422 global1317frontend/1415backend pertenece al corte anterior y no se presenta como rerun de estos cambios. Entregado a judge independiente y E2E para actualizar hash/captura; no mutación frontend autorizada todavía.

35. Foco real de Chromium @s37 (reproducción independientecc49ad): Enter en Actualizar dejaba BODY al aplicar disabled nativo, antes de resize/axe y también después de503. Se esperó la finalización de Stryker originale5ccd3; ningún archivo de campaña se cambió durante medición. Prueba adicional de semántica/coalescing RED3aac94 por ausencia de aria-disabled. Cambio productivo de un atributo: aria-disabled conserva el control en el orden de foco; pending.current ya ignora Enter/click repetidos. No se añade focus() ni se elige otro destino. La prueba verifica estado accesible, foco, dos Enter extra sin nueva petición y foco conservado tras503; no se afirma que JSDOM reprodujera el blur nativo de Chromium.

Corte de foco:111 API/UI GREENeeb6ae (71+40), formato y ESLint focal sin errores. El GREEN definitivo de foco real queda a cargo del E2E independiente; no se sobrescriben los informes originales de Stryker ni se añaden tests de mutantes en esta corrección.

Build/types final del corte de foco EXIT0 9f4c89:49 módulos,286ms. Producción y pruebas congeladas para judge y E2E; estilos y geometría no cambiaron. El autor E2E recibió freeze y actualizará la huella de Today para su siguiente captura.
