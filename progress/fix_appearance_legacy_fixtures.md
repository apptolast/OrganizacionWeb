# Fixtures legacy ante GET global de apariencia

Revisión acotada autorizada por root a ocho suites, tras freeze de B.
RED real: ocho suites, 5 fallos y430 verdes, EXIT1 `631d27`, log
appearance_legacy_red.log. Los fallos están en create-task (2), today (2)
y work-session-integration (1); GETappearance consumía el mock secuencial
o aparecía como petición inesperada/de negocio. Los otros cinco archivos
no necesitaron cambios y permanecen intactos.

Corrección test-only en esas tres suites: GET exacto
/api/v1/me/appearance con método GET o ausente retorna snapshot
unconfigured y su ETag. Todas las demás solicitudes se delegan al mock
original. No se sustituyen Provider/apiRequest, no se relajan aserciones
ni contadores de negocio/auth. create-task conserva retorno del mock para
configurar mockResolvedValueOnce y restaura globals en afterEach.

Incidente de edición: primera herramienta esperaba stubGlobal en
create-task, pero éste usa spyOn. Abortó antes de escribir, y la suite
repetida siguió roja (`8c2d71`, appearance_legacy_green.log); no se acredita
como GREEN. Adaptación corregida al patrón real.

GREEN final: ocho suites435/435, EXIT0 `44c7c6`, log
appearance_legacy_green_actual.log. ESLint y Prettier de tres archivos
EXIT0 `c394f0`. No producción, scripts, Java ni tests de B modificados.
Regresión conjunta con B se coordina aparte; esto no es initglobal.
Revisión estructural TypeScript AST: 133 aserciones create-task, 169 today y 12 work-session-integration idénticas a HEAD (03cb39), sin inferirlo sólo del GREEN. Comentarios propios corregidos a ASCII: cero U+FFFD. Formato reaplicado después del cambio textual. Conjunta autorizada por B: EXIT0 5fee26; log appearance_legacy_joint.log, incluye sus tests appearance/auth/App congelados.
