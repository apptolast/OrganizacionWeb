# Preparación del despliegue de vistas y campos

8 de septiembre de 2026. No se ha desplegado la función 21. Lectura SSH
autorizada: los cuatro servicios OrganizationWeb están 1/1 y sus contenedores
healthy. El catálogo local limpio coincide con las imágenes vivas de la
release `ed00ad426842b4a85f2a0f849de014a8615ba76d`.

Se reutiliza el procedimiento de `release20_preparation.md`: candidato
limpio, imágenes Linux/amd64 con OCI revision exacta, digest publicado,
catálogo revisado y wrapper oficial con lock, primero check y luego apply.
Las referencias anteriores de ese documento son históricas; el rollback
de 21 debe apuntar a las imágenes actualmente vivas de 20:

- API: `ocholoko888/organizationweb-api@sha256:fae45cecc45c8a3feed715524dd0cbfba6ecfac9eabd1ef50be740f84332ceb6`.
- Web: `ocholoko888/organizationweb-web@sha256:3b939af19b1d66b05c8adef5649b9e5ecd3d8778aea0a3905c86e0c206be4d23`.

V20 añade customization_preferences y dos tablas de valores con FK a
proyectos/tareas. Obtener copia PostgreSQL fresca y ensayar restauración;
la copia previa a Apariencia no contiene V19 ni escrituras posteriores.
Probar API20 contra la base migrada a V20 antes de afirmar rollback compatible.
Conservar tablas, datos e historia Flyway; no borrar migraciones ni restaurar
una copia antigua encima de posibles escrituras nuevas automáticamente.

No consumir los doce slots de definiciones de la cuenta real con pruebas:
incluyen inactivos y no tienen borrado. Altas/tipos/límites/recuperación se
acreditan en entorno aislado. En producción comprobar lecturas y, si procede,
defaults o colección vacía con revisión vigente, usando baseline fresco de
los hechos del usuario. Confirmar salud, convergencia y rutas existentes.

Pendientes antes de seleccionar candidato: frontend final, mutación,
regresión integrada, matriz UX y CI sobre el corte exacto. La comprobación
de salud anterior no sustituye ninguna de esas puertas.

## Copia previa a la versión 21 verificada

Copia nueva bajo host_global_operation_lock.py, operación organizationweb-backup:
/var/backups/organizationweb/organization-20260907T233220Z-ab236d773885441f93c15ab05f3fe2ad.dump.
51.428 bytes, modo0600, SHA256
883d8dc1750f5ad2f17a1b364fcf1eff1d7ddd6a65b5b3bf9a67da7bfe49f798.
Transferencia en memoria con hash idéntico y restauración a PostgreSQL vacío
sin red, usuario70 y capacidades retiradas: EXIT0. Resultado:19 migraciones
(última19), un proyecto, una tarea, una sesión, nueve eventos y una preferencia
de apariencia; aún sin tablas21. Se retiraron contenedor/volumen propios y
se borró el buffer. Evidencia externa organizationweb-customization-backup-restore.json.
No acredita copia externa, RabbitMQ ni escrituras posteriores al snapshot.

Script operativo organizationweb-v20-rollback.mjs revisado por root (392fe2),
SHA7169BBE33A63AB1E040A26677EA7799CDF72D99C808F8E85113E01447AC1AB48.
Preparado para candidato digest/revisión exactos y API20 viva. Ensayo todavía
no ejecutado: comprobará API21/API20/API21 con ambos ámbitos, valores tipados,
definiciones inactivas, DTO/ETag y esquema/Flyway preservados. Recursos locales
propios, puertos efímeros de loopback, sin reparación ni borrado de migraciones.

## Candidato publicado y rollback probado

Candidato dfac90edcabdf04e442b906f0ab6db8894cbc4b2, imágenes publicadas con tagSHA40,
Linux/amd64 y OCI revision verificados. API digest83e75c196f05054fde4370c9d7f30605acd12995e63fbbc85ca118d3084472fd;
web digest9fd69f52549c7c764fcc36d9fa8eaa658f50e73f3465d22094352a29e2a79cb9.
Manifiesto externo release21-dfac90e-publish-results.json; checkout limpio,
1821 inputs intactos. Publicación no equivale a despliegue ni cierre de gates.

Ensayo operativo aprobado: API21→API20→API21 EXIT0 (9b43ce), revisado por root
bddbfa y hashes68e6c7. Conserva DTO/ETag de ambos ámbitos, cuatro tipos,
definiciones inactivas, vistas, apariencia y datos de negocio; snapshots de
columnas, constraints e historial Flyway idénticos. No repair/ignore/borrado.
Los cuatro recursos aislados se retiraron, inventario final vacío.
Resultado externo ow-v20-rollback-f5e857f8-785c-4c22-8975-cf8de24f16d6-result.json,
SHA276E5FE54899D5B651479EBC7AF6D4A704B21863785A7A11F733F0BFED7ED80D;
log SHA5C568E6D3EE0B61BFC99B75B88F135E1D835F4172AA35A3EE2905BD027EE262F.
Script aprobado sin cambios. No acredita Swarm, TLS, RabbitMQ ni interfaz de
rollback: esos límites permanecen separados de la compatibilidad API/PG.

## Check del servidor

Infra0bb939bc29ab6e78edb4b0e28d3d6fd01859f415 publicada en PR33, borrador.
Revisión independiente de seguridad sin hallazgos, quince pruebas focales y
lint local verdes; CI infraestructura34171772641 en curso. Checkout remoto
propio limpio en ese commit, sin modificar servicios.

Wrapper oficial --local --playbook organizationweb --check EXIT0:
27 ok,2 changed,11 skipped,0 failed/unreachable. Los dos cambios mostrados
son metadatos de despliegue; check-mode no aplica stack ni demuestra por sí
solo convergencia de las imágenes candidatas. Operación
f0fb9d4c4efd1ff35f8a3df4dd86a4c7b5c27f62a79f5388c27bba1480c14010
completada y lock liberado. Log externo organizationweb-customization-check.log,
SHA D1291F0D3BFC4FD424B007C47231901E73C93EC762FE1A694100397BD02F9DEC.
Apply pendiente de CI y mutación, seguido de aceptación real.
