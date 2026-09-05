# Review — feature1 create_project

**Veredicto:** APPROVED

## Independencia y alcance

Este juez no escribió backend/ ni frontend/. Revisó sus fuentes, pruebas y bitácoras. Sí escribió el tooling/E2E de integración; esa parte fue revisada independientemente por el coordinador raíz, que comunicó ausencia de bloqueos en aislamiento/limpieza, credenciales, proxy sin reintento POST y comandos CI. No se presenta esa revisión como autoevaluación independiente.

## Cobertura de escenarios

- [x] @s1–@s28 localizados individualmente en [mapa consolidado](tdd_create_project.md).
- [x] Validación/normalización se verifican en dominio y PostgreSQL real, incluyendo todos los ejemplos Unicode/tipos del contrato.
- [x] Cada fallo de persistencia se provoca mediante trigger real de PostgreSQL; ambas tablas se comprueban vacías.
- [x] Persistencia tras recarga y reinicio se verifica contra filas SQL completas, no contra un mock ni estado React.
- [x] Estados pendientes, error, repetición, respuesta incierta, texto literal y teclado tienen aserciones observables.

## Disciplina TDD

- Producción sin test que la pida: no encontrada en el núcleo revisado.
- Evidencia rojo→verde→refactor: sí, separada por frontera en los tres informes enlazados.
- Regresiones que ya estaban verdes se identifican como tales; no se inventa un rojo.
- Autenticación/proxy y accesibilidad detectaron rojos de integración reales que exigieron correcciones concretas.

## Calidad

- Dominio sin framework; aplicación depende de dominio/puertos. ArchUnit verifica esas restricciones y el puerto de entrada del controlador.
- Identidad procede del Principal autenticado; ownerId/status/campos desconocidos del cliente se rechazan. Basic de bootstrap exige credenciales no vacías y no se guarda en frontend. Origin externo se rechaza y el endpoint requiere JSON.
- TransactionTemplate confirma proyecto/outbox antes de responder201; excepción de cualquiera de las escrituras revierte ambas. Reloj inyectado y precisión de microsegundos coherente con PostgreSQL.
- SQL parametrizado; evento no transporta descripción; respuestas de error no incluyen credenciales ni stack traces. El error inesperado devuelve correlación.
- React representa contenido como texto y conserva valores ante error; no reintenta POST ni muestra éxito antes de confirmación. Corrección de foco respeta un control elegido mientras espera.
- Sin funcionalidades o conectores ficticios fuera del corte aprobado.

## Hallazgo resuelto y verificación final

1. Hallazgo resuelto: backend/src/main/resources/application.properties:11 habilita fail-on-trailing-tokens. El responsable reprodujo HTTP201 indebido con JSON concatenado; ProjectApiTest.s14_rejectsTrailingJsonDocumentWithoutWrites comprueba ahora400 MALFORMED_JSON y ambas tablas vacías. Revisados configuración y test después de65 tests verdes comunicados.
2. Verificación congelada ejecutada por coordinador: `node .harness/harness.mjs verify` terminó con exit0 y «Todo verde» (sesión79769). Lint,65 tests backend,38 tests frontend y PIT36/36 verdes. La repetición final Stryker confirmó143 KILLED,5 SURVIVED,0 timeout/no coverage/errors:96.62%. Build y E2E exit0,8/8 en16.9s incluida regresión skip-link. Capturas finales regeneradas y revisadas visualmente por coordinador.
3. No quedan cambios de código requeridos. Se autoriza al coordinador a realizar el cierre de metadatos/history/feature_list y commits tras esta aprobación. Backend36/36 mutantes muertos100%; frontend143/148 muertos96.62%, cinco equivalentes documentados en mutation_create_project_frontend.md, sin exclusiones nuevas ni rebaja de umbral. Revisadas las justificaciones contra los consumidores reales: mantienen el comportamiento observable de este corte.

## Checkpoints

- C1 [x] Archivos y configuración presentes; init incluido en verify final exit0.
- C2 [x] Una feature in_progress y resto pendientes; aprobación humana registrada.
- C3 [x] Fronteras y dependencias justificadas, sin producción ajena al contrato.
- C4 [x] Tests reales:65 backend,38 frontend y8 E2E congelados verdes; PostgreSQL y navegador reales.
- C5 [x] No se observaron temporales sospechosos fuera de exclusiones; estado de sesión activo coherente. El cierre administrativo/history/done lo ejecuta el coordinador tras este veredicto, con todas las puertas técnicas superadas.
- C6 [x] Contrato aprobado,28 escenarios trazados y evidencia TDD registrada.
- C7 [x] Backend100% y frontend96.62% superan80%; cinco sobrevivientes frontend equivalentes justificados; informe global mutation_create_project.md revisado.
