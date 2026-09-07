# Soporte de Stryker20

Propuesta aprobada por root; configuración lista, sin campaña ejecutada.

1. Oráculo dispatcher añadido antes del registro: REDfc3bb5 por target
   appearance-frontend desconocido. Registro mínimo en project.mjs y
   GREEN1/1 (appearance_stryker_dispatch_green.log, leído8a49db).
2. Oráculo de configuración antes del archivo: RED8727a4/ENOENT, log
   appearance_stryker_config_red.log. Configuración con nueve selectores,
   candidatos completos y protocolos aprobados; GREEN1/1 en
   appearance_stryker_config_green.log.
3. Formato real Prettier y regresión Node completa EXIT0 6064e2:
   55/55, sin fallos u omisiones. Log appearance_stryker_harness_final.log.
   Sintaxis y diffcheck verdes8a49db.

El contrato Node verifica comandos, umbral80, perTest, ocho workers,
reportes separados, plugin existente y vite.config.ts sin filtros de
pruebas. Comprueba comienzo/contenido de los seis rangos compartidos
contra las fuentes presentes; los tres archivos nuevos se mutan enteros.
Los rangos son los nodos AST aprobados, no IDs de una campaña anterior.
No se ha cambiado harness.config.json, backend Java, producción frontend
ni pruebas de B. Sólo se ignora el temp dedicado nuevo.

Comando preparado:
`node scripts/project.mjs mutate appearance-frontend`.
La ejecución debe esperar freeze de fuentes/tests e init integrado. Si B
mueve líneas de App/Workspace/SessionGate/use-session, recalcular los mismos
nodos antes de campaña y volver a revisar selección; no usar filtros de
candidatos para evitar NO_COVERAGE.

Archivos funcionales congelados: scripts/project.mjs,
scripts/project.test.mjs, frontend/stryker.appearance.config.json y
.gitignore. El manifiesto appearance_stryker_support_manifest.json conserva
sus hashes y este informe. No se afirma resultado o score de mutación.
