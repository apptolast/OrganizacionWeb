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
- Sólo publica si el workflow que pasó es exactamente
  `.github/workflows/harness-ci.yml` (no otro fichero que se llame igual) y si
  `actor` y `triggering_actor` son el propietario (`PabloHurtadoGonzalo86`).
  Un push directo a `main` del bot autónomo no publica nada.
- Corre en el environment `dockerhub-publish` y, antes de nada, se niega a
  seguir si ese environment no está restringido exactamente a la rama `main`.
  Un environment inexistente se crea solo al primer uso y sin restricción; esa
  comprobación lo convierte en un fallo en lugar de en una puerta abierta.
- Antes de construir exige que el SHA sea antecesor o igual de la punta real de
  `main` (API `compare`): `head_branch` es texto libre y un tag llamado `main`
  también lo daría.
- Hace checkout del SHA exacto que pasó la CI, sin credenciales persistidas, y
  exige árbol limpio y legible por todos antes de construir.
- Los builds empujan sólo `:<SHA de 40>` (asa inmutable para volver atrás).
  Construye con `provenance: mode=max` y la etiqueta
  `org.opencontainers.image.revision=<SHA>`.
- Descarga cada imagen por digest y comprueba linux, amd64 y la revisión, igual
  que hará el rol de Swarm.
- Sólo después, en un único paso final, vuelve a comprobar que el SHA sigue
  siendo la punta de `main` y mueve los dos `:latest` a los digests ya
  verificados con `docker buildx imagetools create` (mismo índice, sin
  reconstruir). Después comprueba que cada `:latest` resuelve exactamente a su
  digest verificado. Si `main` avanzó durante los builds, no mueve nada y deja
  en el resumen el SHA que debería tener `:latest`.
- Mover los dos tags no es atómico. Va primero el API; si el web falla, intenta
  devolver el API a su digest anterior y falla en rojo. El resumen registra los
  digests anteriores de ambos para poder arreglarlo a mano.
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

Estado verificado de la configuración de Actions del repositorio: no hay
environments (`GET /environments` da `total_count: 0`), `main` no tiene
protección de rama (404) ni rulesets, `default_workflow_permissions` es
`write` y `sha_pinning_required` es `false`. Con ese estado, un secreto de
repositorio lo puede leer cualquier workflow de cualquier rama que empuje
alguien con escritura, bot autónomo incluido, y empujar `:latest` saltándose
todas las puertas de este workflow. Mover `:latest` es desplegar en
producción, así que ese token tiene de hecho permiso de despliegue.

Pasos bloqueantes, en este orden y todos antes de crear los secretos:

1. Crear el environment `dockerhub-publish` con política de ramas
   personalizada que permita sólo la rama `main` (ni «todas las ramas» ni
   «ramas protegidas»). El workflow comprueba esto y se niega a publicar si
   no se cumple.
2. Crear un ruleset sobre `main` que bloquee los pushes directos y exija PR,
   sin bypass para el bot autónomo ni para la app de claude-code-action. La
   lista de actores del workflow ya impide que el bot publique, pero sin el
   ruleset el bot sigue pudiendo escribir en `main`.
3. Opcional y recomendado: bajar los permisos por defecto de los workflows a
   lectura (`default_workflow_permissions: read`).
4. Crear `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN` **sólo como secretos del
   environment `dockerhub-publish`**, con lectura y escritura sólo sobre los
   dos repositorios. Nunca como secretos de repositorio con esos nombres: el
   environment no los oculta si también existen a nivel de repositorio. Sin
   ellos el paso de login falla y no se publica nada.

Además:

- Revisar el PR a mano: toca `.github/workflows/`, y la protección de rama y
  CODEOWNERS no están activos hoy (`guard-sensitive-paths.yml`).
- La lista de actores está escrita en el workflow. Si otra persona debe
  publicar, se cambia por PR revisado, no con una variable.
- Asumir el riesgo que queda: con el canal activo, cualquier push del
  propietario a `main` con la CI verde llega a producción. Los pasos 1, 2 y la
  lista de actores cierran el camino del bot a producción (riesgo G5 del
  diseño); no cierran el del propietario, que es la decisión tomada.

## Verificación

- `actionlint` 1.7.12 sin hallazgos sobre los cinco workflows.
- El YAML carga. Ya no es idéntico al borrador revisado: incorpora los
  hallazgos de la revisión (etiquetas `:latest` movidas al final desde los
  digests verificados, environment restringido a `main`, lista de actores,
  ruta del workflow de CI y SHA antecesor de `main`).
- Los scripts de los pasos nuevos se ejecutaron en local con `docker` y `gh`
  simulados: environment correcto, sin restricción, con comodín y con un tag
  llamado `main`; SHA antecesor, divergente y por delante de `main`; punta
  movida, publicación normal, primera publicación, fallo del web con
  restauración del API y fallo del API.
- No verificado aquí: una ejecución real en GitHub Actions ni el push a
  Docker Hub. Tampoco que `GITHUB_TOKEN` con `actions: read` pueda leer el
  environment y sus políticas de ramas (si no puede, el workflow falla
  cerrado), ni que `imagetools create` con una sola fuente conserve el digest
  del índice (si no lo conserva, la comprobación final falla en rojo).
