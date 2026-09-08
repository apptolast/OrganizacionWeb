# Scope propuesto frontend22

Dos módulos completos: export-data-api.ts y export-data.tsx. Seis nodos AST de
integración en export_frontend_scope_nodes.json, generados por el parser local
export_frontend_scope_parser.mjs, con texto, rango y SHA256 de cada fuente.

App: declaración de reconocimiento de ruta y ambos ConditionalExpression
completos (sección y montaje). Se incluyen las alternativas heredadas necesarias
para no excluir la guarda exportData && username. Desestructuración y tipos no
añaden otro cuerpo ejecutable. Workspace: RouteLink completo. SessionGate:
atributo username completo. useSession: comparación exacta de la nueva ruta.

Configuración propia stryker.export-data.config.json: mismo Vitest de todas las
suites, perTest,8workers, umbral80, mismos plugins/ignorePatterns, todos los
mutadores predeterminados. Sin exclusiones ni incremental. Reportes JSON/HTML y
temp separados; progress-append-only ya registrado en la instalación10.

Dispatcher sólo añade export_data-frontend. Test de selección RED570e12 y
GREEN79ec59; configuración ausente RED8b7082 y GREENdf6a20. No se ejecutó campaña.
Comando listo tras revisión/gates: node scripts/project.mjs mutate export_data-frontend.

Default aprobado por root: Workspace completo conserva como superconjunto los rangos anteriores; App y use-session ya estaban completos. Se añaden ambos módulos de exportación y el atributo username de SessionGate. Apariencia conserva sus seis nodos de integración, remapeados por AST y expectativas startsWith exactas: App 28:8-28:44, 39:12-51:28, 56:10-97:7; Workspace 75:10-80:22; SessionGate 32:2-52:6; use-session 177:0-196:1. No se modifican umbrales ni ejecutan campañas históricas. Arnés completo 63/63.
