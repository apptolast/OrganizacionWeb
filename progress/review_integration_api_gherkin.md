# Revisión independiente de Gherkin 24

Dictamen final: **APPROVED** tras la precisión acotada de s35. La lectura inicial y su único hallazgo quedan preservados a continuación. Leídos completos `features/integration_api.feature` SHA256 `408C38B204917833EAC8181445EF1BA66C458B7F770179AC8855F236AFD2478E`, propuesta `B948AB34566EA6CE1EED299B5CD7DF1DF42B3E22C56E13EA38F680A25CAE349B`, sección 24 de `e1f9807` y `docs/gherkin.md`.

Único ajuste solicitado a C: en **s35**, «no crea automáticamente otra id» no exige que la UI impida iniciar manualmente una nueva creación mientras la anterior continúa incierta. El contrato dice «No puede cambiarse de id antes de resolver la anterior». Añadir al mismo escenario un resultado observable: Crear otro intento permanece bloqueado tras incertidumbre/404/reload; siguen disponibles comprobación y reenvío deliberado con la misma id. No hace falta otro escenario ni repetir las combinaciones de negocio.

Resto revisado sin discrepancias bloqueantes:

- 42 tags estables, 42 escenarios y un When por escenario. Las tablas Examples agrupan fronteras homogéneas; no contienen pasos posteriores. Referencias a contratos de negocio existentes conservan su autoridad sin duplicar toda su matriz.
- s22 contiene las 18 operaciones exactas: proyectos 4, tareas 6, agenda 5 e historial 3. Incluye status de tarea y state de bloque; no hay scopes implícitos ni prefijos comodín. s23 y s32 separan denegaciones y excepción exacta OpenAPI.
- s18–24 preservan cookie/CSRF, seleccionan Authorization sin fallback, prueban sesión inaccesible y owner bootstrap diferente, y ordenan 401, Origin403, scope403 y cuota429. s25 conserva ETag/outbox y ausencia de idempotencia nueva en POST de negocio.
- s1–17 y s26–31 cubren secreto único después de commit, replay secret:null incluso con cupo completo, intención diferente/owner ajeno, carreras de cupo/cuota, caducidad exacta y frontera honesta de revocación. El resultado incierto de COMMIT no se afirma como rollback.
- s33–40 distinguen secreto transitorio en memoria de intención mínima en sessionStorage, retiro de owner/ruta, clipboard deliberado y recuperación sin PUT automático. No interpretan 404 como ausencia garantizada de commit ni prometen recuperar el secreto perdido. El ajuste de s35 cierra el bloqueo de nueva identidad, no cambia ese protocolo.
- s41 remite a la matriz UX30 y tres motores sin declarar evaluación humana por axe; s42 conserva privacidad y exclusión de credenciales de export/import.

Sin suites, código, estado ni commits. Esta revisión no afecta producto 23 ni sus campañas. Actualizar dictamen al recibir el delta puntual de C y verificar que sólo añade la prohibición ya contratada.

Cierre del delta: feature SHA256 A155A48E4AF40CA7A0BBFD2979FD14A89BBE0EDDCD3A587C8E3861EF087C4B95. Se añadieron exactamente dos And a s35: bloqueo de otra id mientras incierto, incluido 404/reload, y conservación de comprobación/reenvío deliberados con la misma id. Al retirar sólo esas dos líneas reaparece byte-exact el SHA408C38B2 original. Continúan 42 escenarios y 42 When; ningún otro cambio ni requisito pendiente de esta revisión. Sin código ni autorización de implementación implícita.
