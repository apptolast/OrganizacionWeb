# Publicación automática de las imágenes api y web (canal `:latest`)

11 de septiembre de 2026. Cambio de configuración de CI, fuera de `src/` y de
los tests: no hay feature SDD que recorrer.

## Qué cambia

`.github/workflows/publish-images.yml` publica
`ocholoko888/organizationweb-api` (contexto `backend`, `backend/Dockerfile`) y
`ocholoko888/organizationweb-web` (contexto `.`, `deploy/web.Dockerfile`) para
linux/amd64. Es la misma receta que hasta hoy se ejecutaba a mano
(`progress/export_release_preflight_review.md`), ahora automatizada:

- Se dispara con `workflow_run` cuando «Application CI» termina en verde sobre
  un push a `main` del propio repositorio. Nunca con PRs ni forks.
- Hace checkout del SHA exacto que pasó la CI, sin credenciales persistidas, y
  exige árbol limpio y legible por todos antes de construir.
- Empuja siempre `:<SHA de 40>` (asa inmutable para volver atrás) y mueve
  `:latest` sólo si ese SHA sigue siendo la punta de `main`.
- Construye con `provenance: mode=max` y la etiqueta
  `org.opencontainers.image.revision=<SHA>`.
- Tras el push descarga cada imagen por digest y comprueba linux, amd64 y la
  revisión, igual que hará el rol de Swarm.
- Acciones fijadas por SHA de commit, comprobados contra el tag real de cada
  proyecto: `actions/checkout` v6.1.0, `docker/setup-buildx-action` v4.3.0,
  `docker/login-action` v4.6.0 y `docker/build-push-action` v7.3.0.

## Por qué

Decisión explícita del propietario del 11 de septiembre de 2026: todo servicio
del Swarm se actualiza solo desde etiquetas de canal revisadas, y `:latest` es
el canal de las imágenes propias. El repositorio de infraestructura consumirá
este canal en una fase posterior; hasta entonces producción sigue fijada por
digest y este workflow no despliega nada.

## Lo que falta y sólo puede hacer el propietario

- Crear los secretos `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`, con lectura y
  escritura sólo sobre los dos repositorios. Sin ellos el paso de login falla
  y no se publica nada.
- Revisar el PR a mano: toca `.github/workflows/`, y la protección de rama y
  CODEOWNERS no están activos hoy (`guard-sensitive-paths.yml`).
- Asumir el riesgo: con el canal activo, cualquier fusión a `main` con la CI
  verde llega a producción, incluidas las del bot autónomo.

## Verificación

- `actionlint` 1.7.12 sin hallazgos sobre los cinco workflows.
- El YAML carga, y es idéntico al borrador revisado salvo los comentarios.
- No verificado aquí: una ejecución real en GitHub Actions ni el push a
  Docker Hub.
