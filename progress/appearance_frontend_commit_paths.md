# Paquete frontend de Apariencia para integración selectiva

Lista de B; no incluye archivos backend, arnés o auditoría propios de A/C.
Root conserva el control de Git. No añadir capturas grandes ni raw de mutación
por extensión o glob; las rutas de evidencia se conservan en los informes.

## Producto y pruebas

- frontend/src/appearance-api.ts (el test API ya está versionado, sin delta).
- frontend/src/appearance-state.tsx
- frontend/src/appearance.tsx
- frontend/src/appearance.test.tsx
- frontend/src/App.tsx
- frontend/src/workspace.tsx
- frontend/src/session-gate.tsx
- frontend/src/use-session.ts
- frontend/src/styles.scss
- frontend/src/authentication.test.tsx
- e2e/appearance.spec.mjs

Los tres fixtures legacy pertenecen al paquete independiente de A; no se
incluyen por accidente en esta lista. Los scripts UX pertenecen a C.

## Documentación y manifiestos de B

- docs/mvp-user-guide.md
- progress/tdd_appearance_frontend.md
- progress/appearance_contract_evidence.md (mapa iniciado por A y consolidado por B).
- progress/appearance_shared_freeze.json
- progress/appearance_shared_recovery_freeze.json
- progress/appearance_frontend_final_freeze.json
- progress/appearance_frontend_text200_freeze.json
- progress/appearance_frontend_commit_paths.md

Los manifiestos históricos no se reemplazan con hashes nuevos. El último
acredita que el único delta posterior al freeze del producto fue SCSS.
Los informes de revisión, gate frontend y configuración de mutación tienen
ownership A/root y se integran con sus propios inventarios.

## Resumen propuesto para la revisión

Apariencia incorpora tema claro, oscuro o del sistema y dos acentos propios
validados. El borrador sólo afecta a las muestras; el guardado confirmado
aplica la preferencia a toda la sesión. Un resultado incierto bloquea otra
escritura incluso al navegar, hasta consultar deliberadamente lo guardado.
Se preservan aislamiento entre cuentas, controles nativos y recuperación
sin reenvíos automáticos.

Validación:78 pruebas focales,2046 frontend globales y lint/build del CSS
final verdes. El recorrido real de guardado/recarga, geometría, teclado y
texto200 tiene evidencia acotada en los informes. Auditoría UX completa y
resultado Stryker todavía pendientes al redactar; no se declara despliegue
ni cierre20 por este paquete.
