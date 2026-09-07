# Preparación de release20: Apariencia

Lectura local únicamente, 7 de septiembre de2026. No se ejecutaron SSH,
build, push, cambios IaC ni consultas de valores secretos. No acredita que
el candidato20 esté publicado ni sustituye sus gates pendientes.

## Fuente e imágenes

El catálogo anterior corresponde a aplicación
`4d9469a1a3db10e02b40de648202a3b97511feef`, IaC `770b7369c2b8c51ab2055dec6d3f63434b620a2d`.
Evidencia: work/deployment-preparation/organizationweb-dns-infra-review.md,
organizationweb-dns-acceptance.json y organizationweb-dns-final-*-publish.log.

Construir después desde el candidato limpio que prepare root:
- API: `backend/Dockerfile`, contexto `backend/`; compila bootJar desde src/main.
- Web: `deploy/web.Dockerfile`, contexto raíz; instala frontend con lockfile,
  pnpm10.21.0, compila y copia deploy/nginx.conf más dist.
- Conservar la corrección DNS de Nginx, proxy_next_upstream off, permisos1777
  de /run y /var/cache/nginx y restricciones Swarm/tmpfs del catálogo actual.

Precisión: los Dockerfiles anteriores NO fijan FROM por digest; usan tags.
BuildKit resolvió las siguientes bases, según los logs finales4d946:
- eclipse-temurin:25-jdk: e787e08ef76f4c16866108cd7f9fcd96a68eef3ac6cc76866897d4d02d5a2262
- eclipse-temurin:25-jre: f9e65324a37f28209ce7dd0e5149a7aa954520ed936fb87813cf6ded2400a112
- node:22.23.2-alpine3.23:46825fbbd4e996a78b7a2cdc08d75e38a5a505bdab95dcda55605359bf124bc6
- nginx:1.30.4-alpine3.24:dc5069ad14f19660b141b21236140b91656bf89bbc3e2417c70ae650cd66104c
No asumir que un tag resuelve hoy igual. Registrar las bases efectivas del
candidato y revisar cualquier diferencia; no retocar silenciosamente sus
Dockerfiles congelados. Si se exige pin explícito, debe quedar en candidato
revisado y OCI revision identificar ese commit exacto.

Las imágenes finales sí se publicaron/fijaron por digest en DockerHub:
API `ocholoko888/organizationweb-api@sha256:474db9687be9c083fc92f59c65f58798bf0efc797b4ff96a3ad02432486cf72a`;
web `ocholoko888/organizationweb-web@sha256:18d04ce974abbd00d15daa47e73258b4f742aac31f025d8784fb4cabea274d8f`.
El tag4d9469a es descriptivo; el catálogo usa el digest publicado del índice,
no el digest del config ni el manifest de attestation. Los Dockerfiles no
contienen LABEL: al construir20 se debe suministrar
`org.opencontainers.image.revision=<SHA completo del candidato>` y comprobar
la etiqueta recuperada del registry, Linux/amd64 y RepoDigests para ambas.
La revisión anterior acredita esa comprobación, no conserva aquí la orden
literal de build que añadió la etiqueta.

## Cambio exacto de catálogo y comprobaciones

En work/DockerSwarmInfrastrcture-organizationweb/config/organizationweb.yml,
sólo cambiar `organizationweb.release`, `organizationweb.images.backend` y
`organizationweb.images.web`; valores nuevos después de publicación/verificación.
Conservar postgres/rabbitmq, secretos referenciados, hostname, red y data_root.
CHANGELOG y evidencia de revisión acompañan el cambio, sin editar permisos.

Antes del apply: candidato limpio y CI/gates20 aprobados; bootstrap-tooling,
validate-iac y lint en ese orden para la revisión IaC; wrapper
`deploy-ansible.sh --playbook organizationweb --check` con el modo de acceso
revisado. Revisar diff y checksum, capacidad, identidad del único manager,
red dedicada cifrada, procedencia de referencias secretas y permisos de rutas.
El rol despliega sólo fuera de check; no incluye backup PostgreSQL automático.
No cerrar gates de host por inferencia desde documentos antiguos.

## V19 y recuperación

V19__appearance_preferences.sql crea una tabla nueva, sin modificar tablas
previas ni emitir eventos. Antes del primer arranque20, obtener backup fresco
PostgreSQL consistente, registrar hora/SHA/tamaño/permisos y validar archivo.
Ensayar restauración en destino vacío aislado con conservación de datos y
migraciones. La copia170234Z previa sólo acredita18migraciones y datos de ese
instante: SHA c0a92ee91b5ceb2bbac738a5d8fd749cf9a9497667c9a2cdf29044fef196299a,
46689bytes, restauraciónEXIT0 con recibos/intervalos coincidentes. No cubre
escrituras posteriores ni prueba copias externas automáticas.

Tras aplicar por wrapper: cuatro servicios1/1 saludables, API/web con digest
exacto y actualización completada, FlywayV19 válida, GET privado/defaults y
preferencia propia guardada/recargada. Contrastar datos de trabajo anteriores
y rutas legacy; no crear trabajo ficticio para comprobar Apariencia.

Rollback aditivo: nuevo catálogo revisado a los dos digests4d946 anteriores,
conservar PostgreSQL, appearance_preferences y la historia Flyway. Antes de
confiar en ese rollback, comprobar arranque de la versión anterior sobre una
copia conV19: no afirmar compatibilidad sólo por ser CREATE TABLE. No borrar
V19 ni restaurar la copia vieja sobre escrituras nuevas automáticamente.
Si no converge, conservar marker/evidencia y usar recuperación oficial tras
probar que el controlador terminó; nunca limpiar locks manualmente. Un restore
completo requiere decisión separada por la posible pérdida de datos posteriores.
