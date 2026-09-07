# Aislamiento del E2E nominal de Apariencia

El fixture de autenticación inicia sesión, pero no borra preferencias. El
nominal suponía SYSTEM al entrar; la auditoría que lo precede guarda DARK.
Es contaminación entre pruebas, no defecto del producto.

Reproducción real con el runner existente y un solo stack: caso geométrico
de appearance-ux-audit.spec.mjs seguido del nominal de appearance.spec.mjs.
El primero pasó y el segundo falló en Sistema no seleccionado. EXIT1
e75983, diagnóstico03f7bf, log appearance_nominal_isolation_red.log.
No se simularon respuestas ni se cambiaron tiempos, retries o aserciones.

Corrección: el nominal obtiene la revisión actual y prepara SYSTEM con los
dos acentos predeterminados mediante PUT autenticado/CSRF/If-Match real.
Después se ejecuta el recorrido original completo, incluidas pintura oscura
y recarga. El mismo par pasó2/2 (18,5s), EXIT0ccf134, log
appearance_nominal_isolation_green.log. Stack48868 y volumen retirados.

Root pidió además no contaminar suites posteriores. Se añade afterEach con
el helper sql existente, que exige proyecto E2E y archivo de entorno aislado:
DELETE FROM appearance_preferences WHERE owner_id = 'e2e-user'. Sólo retira
la preferencia de esa cuenta de prueba; no toca disponibilidad, ancestros,
eventos ni otras cuentas. C añade el mismo límite a su auditoría por separado.
El cleanup es posterior al pase2/2: su ejecución se validará en el próximo
runner de C junto al nominal, evitando otro build duplicado. No se atribuye
como validado antes de ese resultado.

Archivo propio único: e2e/appearance.spec.mjs.
SHA final060D2E670E7D665429557BADAF593E8ABDDF9A35D3AF8A48432CA151B36F987C.
Formato040654. Manifiestos appearance_nominal_isolation_before.json y
appearance_nominal_isolation_after.json conservan el corte anterior al
cleanup; no se sobrescriben. No TS/SCSS/testsVitest/config tocados durante
Stryker. No campaña ni gate global repetido.
