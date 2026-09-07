# Persistencia de apariencia: E2E real acotado

Estado: GREEN inicialmente, no se inventa un RED ni cambio de producción.
Nuevo oráculo en e2e/appearance-persistence.spec.mjs; root aprobó usar el
runner existente scripts/e2e.mjs porque scripts/e2e-smoke.mjs no existe.
Puerto18080 coordinado con B. No cambios en fixtures comunes ni V14.

Comando: `node scripts/e2e.mjs e2e/appearance-persistence.spec.mjs`.
EXIT0 e5b0cc. Log original progress/appearance_persistence_e2e.log.
Un caso real @s8/@s9: login/CSRF, defaults y ETag fuerte, PUTDARK200 real
interceptado mediante route.fetch y route.abort después del commit.
El navegador observa fallo de transporte, no confirmación. Se reinicia
únicamente API mediante helper existente que compara StartedAt y mantiene
proceso/montajes PostgreSQL. Otro contexto de navegador se autentica y
recupera exactamente valores, fecha y ETag persistidos; no escritura nueva.

Antes se crean proyecto, tarea, reserva y sesión cerrada por HTTP. Se
comparan filas propias de trabajo, intervalos y recibos, disponibilidad y
outbox completo del stack aislado, además de historia HTTP por proyecto.
Snapshots iguales tras PUT y tras reinicio. El relay está deshabilitado
por el compose existente; no se atribuye aquí publicación Rabbit.

Limpieza: sólo DELETE de la preferencia creada por este caso con UUID y
owner exactos, después el runner retira su stack/volumen efímero
organizationweb-e2e-6632. No se borran hechos históricos arbitrariamente
en una base compartida. Cleanup terminó y lista de contenedores propios
vacía. El test deja el esquemaV19 real migrado por API; no migra manualmente.

Límites: usa evaluación fetch real en navegador para aislar transporte y
persistencia, no valida el formulario final ni SCSS/UX. @s36/@s37 siguen en
el paquete de UI. No demuestra HTTPS/cookieSecure del despliegue remoto,
ni replay idempotente de PUT (no existe ese contrato). La evidencia es local.
