# Remapeo de selectores históricos de Apariencia tras importación 23

El init original falló porque los rangos históricos apuntaban a líneas desplazadas; el resultado original permanece preservado por A. Se compararon nodos TypeScript AST con el commit histórico 83b02756ece65c9772421ee8523f1887d6cf0d74.

| Nodo | Rango actual |
| --- | --- |
| VariableDeclaration appearance | App.tsx:31:8-31:44 |
| ConditionalExpression section | App.tsx:45:14-57:30 |
| ConditionalExpression JSX | App.tsx:72:10-113:7 |
| JsxElement Apariencia | workspace.tsx:76:10-81:22 |
| IfStatement sesión | session-gate.tsx:32:2-52:6 (sin cambios) |
| FunctionDeclaration isPrivateRoute | use-session.ts:194:0-214:1 |

Los cinco primeros nodos conservan texto idéntico salvo formato. La función completa isPrivateRoute conserva sus rutas anteriores y añade /importacion. Se sincronizaron únicamente cinco literales de configuración y los mismos cinco del array esperado por el arnés; deepEqual, startsWith, umbral y todas las demás asserts permanecen intactos. A verificó independientemente los tres nodos de App.

Validación: node --test scripts/project.test.mjs, 70/70, EXIT0 (ef6ce4). Log original del pase en deployment-preparation/import23-appearance-remap-node.log; mapa de AST y originales previos preservados allí. SHA256 config: 48B1F653F5D24AD9F0B6BD4A29B25434CC59E98576E69CC78A26711077C7F551. SHA256 arnés: 0B0E36798B6EDA819C9ED305BE720DC941A5141B8EC9A59BB4FCFDE7106E3C84.

No se modificó producto ni configuración de importación. No se repitió frontend ni Stryker. El E2E aislado continúa sobre bb0064c sin estas ediciones operativas.
