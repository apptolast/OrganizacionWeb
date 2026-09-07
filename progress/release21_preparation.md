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
