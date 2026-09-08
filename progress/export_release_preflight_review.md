# Preflight documental release22

Lectura únicamente, 2026-09-08. Producto congelado e9350e97d88aebaab499b23aa0bf1917e5b665ee. No se construyó/publicó imagen, no SSH ni cambio infra. Init/build finales confirmados por root; mutación y aceptación del candidato final son gates separados, no acreditados por este informe.

## Publicación propuesta

Reutilizar checkout nuevo limpio, no COMMON ni el árbol con evidencias. Desde `work/OrganizacionWeb-export-data`, la creación futura sería `git worktree add --detach ../release22-candidate e9350e97d88aebaab499b23aa0bf1917e5b665ee`. Verificar `git rev-parse HEAD`, `git status --porcelain` vacío y hashes de todos los inputs antes/después. Si root fija otro commit final, sustituir la revisión en toda la operación, nunca relabelar una imagen construida de otro árbol.

Desde ese checkout nuevo, comandos PowerShell propuestos (secuenciales, detenerse ante cualquier EXIT no cero):

```powershell
$revision = 'e9350e97d88aebaab499b23aa0bf1917e5b665ee'
$proof = '../deployment-preparation'
$env:BUILDX_METADATA_PROVENANCE = 'max'
docker buildx build --platform linux/amd64 --provenance=mode=max --label "org.opencontainers.image.revision=$revision" --metadata-file "$proof/release22-$revision-api-metadata.json" --tag "ocholoko888/organizationweb-api:$revision" --file backend/Dockerfile --push backend *> "$proof/release22-$revision-api-publish.log"
if ($LASTEXITCODE -ne 0) { throw 'API publication failed; preserve evidence' }
docker buildx build --platform linux/amd64 --provenance=mode=max --label "org.opencontainers.image.revision=$revision" --metadata-file "$proof/release22-$revision-web-metadata.json" --tag "ocholoko888/organizationweb-web:$revision" --file deploy/web.Dockerfile --push . *> "$proof/release22-$revision-web-publish.log"
if ($LASTEXITCODE -ne 0) { throw 'Web publication failed; preserve evidence' }
```

Usar autenticación Docker ya configurada sin imprimirla. No tag `latest`. Los contextos son distintos: API `backend`, web raíz. Las bases siguen los Dockerfiles existentes; conservar sus digests resueltos en provenance, no afirmar que tags base mutables garantizan reproducibilidad idéntica.

Verificación remota para cada `api`/`web` (comandos de lectura, también comprobar EXIT):

```powershell
$kind = 'api' # repetir con web
$repo = "ocholoko888/organizationweb-$kind"
$metadata = Get-Content -Raw "$proof/release22-$revision-$kind-metadata.json" | ConvertFrom-Json
$index = $metadata.'containerimage.digest'
docker buildx imagetools inspect "${repo}:$revision" --format '{{json .Manifest}}'
docker buildx imagetools inspect "${repo}@$index" --raw
# Del manifiesto seleccionar exactamente el descriptor linux/amd64:
# $platform = digest de ese descriptor (no el de attestation unknown/unknown).
docker buildx imagetools inspect "${repo}@$platform" --format '{{json .Image}}'
docker buildx imagetools inspect "${repo}@$index" --format '{{json .Provenance}}'
```

Preservar salidas en archivos externos. Exigir digest remoto del tag igual a `containerimage.digest`; descriptor linux/amd64 presente; config con os=linux, architecture=amd64 y label `org.opencontainers.image.revision` exacto; attestation ligada al descriptor y materiales base registrados. Guardar por separado digest índice y digest plataforma. El catálogo usa el índice, como release21. Manifiesto final: SHA completo, tags, ambas referencias por digest, plataforma, label, EXITs, huellas de logs/metadatos e inputs antes/después.

Precedente verificado: `work/deployment-preparation/release21-dfac90e-{publish-plan,publish-results,registry-verification}.json`, los dos `*-metadata.json` y `*-publish.log`; registran revisión dfac90edcabdf04e442b906f0ab6db8894cbc4b2, Linux/amd64, attestation y materiales, EXIT 0, 1821 inputs sin cambios. Son evidencia de release21, no release22.

## Infra: cambio mínimo posterior

Ruta exacta: `work/DockerSwarmInfrastrcture-organizationweb/config/organizationweb.yml`.
Sólo `organizationweb.release`, `organizationweb.images.backend`, `organizationweb.images.web`. Actualmente release21 y digests índice API `83e75c196f05054fde4370c9d7f30605acd12995e63fbbc85ca118d3084472fd`, web `9fd69f52549c7c764fcc36d9fa8eaa658f50e73f3465d22094352a29e2a79cb9`.
Documentar candidato en `docs/ORGANIZATIONWEB.md` y `CHANGELOG.md` sin afirmar live antes del apply. Mantener PG/Rabbit, secrets, red, recursos y edge. Gates existentes: `tests.test_organizationweb_contract` + `tests.test_organizationweb_tmpfs`, `scripts/lint.sh` y CI/validate del repo; ejecutar en runtime IaC establecido, no recrear herramientas.

Después de revisión/merge, operador usa únicamente el wrapper vigente: `./scripts/deploy-ansible.sh --local --playbook organizationweb --check` y después `--confirm-production`. Edge ya existe y no cambia: no repetir su apply para este release. Conservar commit/lock/operation ID y comprobar convergencia/imagen real API y web. Feature22 no añade migración; Flyway20 y datos anteriores deben permanecer. Conservar backup probado reciente y rollback por digests anteriores sin repair/ignore/borrado de datos.

## Aceptación mínima en producción, sin escrituras de negocio

Origen existente: `https://organizacion.apptolast.com`. Usar sesión normal del operador (credenciales locales nunca al informe), HTTPS válido/cookie Secure; el alta/login de sesión no se confunde con una escritura de negocio.

1. GET anónimo `/api/v1/me/export` con Accept JSON: 401 JSON cerrado/no-store, sin attachment. Con sesión existente autenticada, abrir `/exportacion` y confirmar que montar/navegar no inicia el GET.
2. Una pulsación «Preparar exportación»: exactamente un GET `/api/v1/me/export`, sin query ni CSRF requerido para esa lectura. 200, `application/json;charset=UTF-8`, Content-Length igual a bytes reales, sin Content-Encoding, Cache-Control no-store/no-transform, sin ETag/Last-Modified. Filename derivado del único exportedAt UTC de seis microdecimales: `organizationweb-export-v1-YYYYMMDDTHHmmssffffffZ.json`.
3. Parsear UTF-8 estricto localmente, comprobar envelope cerrado format/schemaVersion/owner/exportedAt/data/counts y las 14 colecciones; cada count coincide con su array. Comparar IDs/hechos de la cuenta ya existentes, BIGINT textual, null/false/0 y ausencia de outbox, credenciales y sesiones de autenticación. No volcar payload privado en logs: registrar sólo conteos, tamaño, hash y resultados.
4. Dos descargas explícitas del mismo enlace: mismos bytes/hash/nombre y ningún segundo GET. Salir de la vista o cerrar sesión retira enlace y revoca Blob URL. Con logout, nuevo GET vuelve a 401. No preparar límites masivos ni corrupción en producción; ya se prueban aisladamente.
5. Comparar antes/después hechos de negocio y sus versiones/timestamps y outbox sin forzar el publisher; Flyway/PG/Rabbit preservados. La lectura no crea apariencia/customización por defecto ni definiciones QA. Las descargas contienen datos personales: almacenamiento local privado y retirada posterior sólo del artefacto de aceptación propio.

Rutas contrastadas: `e2e/export-data.spec.mjs`, `backend/src/test/java/com/apptolast/organization/adapter/ExportDataHttpPersistenceTest.java`, sección22 de `project-spec.md`, y `work/deployment-preparation/organizationweb-customization-acceptance.json`. No ejecutar el E2E de fixture directamente contra producción: crea sus propios datos para el entorno aislado.

Referencias primarias para los comandos propuestos: [buildx build](https://docs.docker.com/reference/cli/docker/buildx/build/), [provenance](https://docs.docker.com/build/metadata/attestations/slsa-provenance/), [imagetools inspect](https://docs.docker.com/reference/cli/docker/buildx/imagetools/inspect/). La attestation acompaña al artefacto y no sustituye la comparación del checkout limpio/label/digest.