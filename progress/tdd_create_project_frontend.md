# Frontend create_project — TDD en curso

2026-09-05. Contrato aprobado según coordinador; init correcto antes de delegación. No declara done.

## Ciclos observados

Cada fila se añadió individualmente y se ejecutó antes de producción.

| Ciclo | Rojo observado | Verde |
| --- | --- | --- |
| @s27 formulario | Import App inexistente | Campos etiquetados y botón; 1 test |
| @s22 confirmación | fetch no llamado | POST same-origin, guardando y datos 201; 2 tests |
| @s23 envíos repetidos | botón habilitado | disabled, readOnly y guard; 3 tests |
| @s24 validación | aria-invalid ausente | error asociado, foco descripción, valores exactos; 4 tests |
| @s25 red | role alert ausente, rechazo sin capturar | incertidumbre sin retry, datos conservados; 5 tests |
| @s21 servicio | errors undefined rompe render | error sin lista y título mostrado; 6 tests |
| @s26 texto seguro | Verde ya por escaping React; sin producción nueva | 7 tests |
| @s28 teclado | Verde ya por controles nativos; sin producción nueva | 8 tests |
| 201 incompleto | falsa confirmación | validar forma de representación; 9 tests |
| Proxy no JSON | mensaje erróneo de red | fallo servicio, extracción adaptador API; 10 tests |
| Navegación y captura | h1 inexistente | presentación accesible, estados vacíos y SCSS responsive; 11 tests |

API extraída en projects-api.ts, estado local extraído en use-create-project.ts durante refactor verde. React 19, Vite 8, TS 6 compatible con typescript-eslint, pnpm 10, SCSS sin Tailwind. No contraseñas/localStorage, conectores ni datos ficticios.

## Verificación ampliada

- Fecha inválida 201: test API rojo (se clasificaba created); añadido parse finito de createdAt; verde.
- HTTP401: test rojo (fallback genérico); explicación de autenticación y copiar valores antes de recargar; verde. Navegador recibe challenge mediante proxy configurado por integración.
- Casos adicionales de caracterización ya verdes, sin añadir producción: errores de nombre y recuperación, quitar confirmación anterior al enviar segunda captura, HTTP403/415/500/503 sin lista errors, 12 representaciones 201 inválidas, filtrado de errores de campo mal formados.
- 32 tests en 2 archivos verdes con pnpm test. Include explícito src/**/*.test.{ts,tsx} evita ejecutar copias temporales de Stryker.
- pnpm build verde: TS typecheck y Vite production, JS gzip 62.62kB y SCSS gzip 2.87kB. Sin fonts ni CDNs externos.
- pnpm lint verde: ESLint y Prettier.
- Stryker primera ejecución: runner vitest no descubierto por glob implícito en pnpm; resolución oficial de configuración explicitando plugins @stryker-mutator/vitest-runner. Segunda ejecución en curso con 133 mutantes.
- Stryker muta API y hook de estado completos; JSX declarativo, SCSS y entry se verifican con tests de comportamiento y E2E responsive/axe. No se excluyen ramas de lógica mediante comentarios.
- Resultado inicial de mutación: 53.38% con Vitest5, pero mutantes como eliminar setFailure o argumentos POST sobreviven pese a aserciones explícitas. Evidencia de filtrado incompatible de tests anidados: casi todos los mutantes muertos vienen de tests top-level. El paquete Stryker10 desarrolla/prueba su runner con Vitest4.1.10; fijada esa versión compatible, 32 tests siguen verdes; nueva medición en curso.
- Error con title vacío: test rojo (mensaje vacío); fallback a explicación de servicio. 33 tests verdes, build/lint repetidos verdes después del cambio.
- Stryker/Vitest4 confirma 90.23% (120/133): los tests anidados ahora matan los mutantes de POST y mensaje. El cambio de versión resuelve el problema de herramienta; no se rebaja umbral.
- Se cierran huecos reales encontrados en sobrevivientes: no alert inicial, default del submit cancelado, error asociado solamente al campo afectado, resultado completo de red, título no textual y respuesta tras desmontaje. Cada test/aserción añadida separadamente con suite verde, sin producción nueva.
- Navegador real/axe: rojo por contraste 4.42–4.48 en textos secundarios; SCSS oscurecido. Integración informa verde sin violaciones en cuatro viewports y estado inicial/guardado.
- Navegador real @s28: rojo al perder foco por disabled. Reproducido en test con foco body durante pending; añadido useLayoutEffect para devolver foco al botón previo solo si sigue en body. Verde. Otro test confirma que no roba foco a enlace elegido mientras espera.
- 38 tests en 3 archivos verdes, build/lint verdes. Medición final Stryker en curso: 148 mutantes, incluye nueva lógica de foco y fallback de título vacío.
- Revisión visual PNG detecta franja del skip-link en top-left fuera de foco. SCSS usa top:-100px y :focus top:8px en lugar de porcentaje transformado. Integración añade regresión geométrica y repite captura. El resto de layout se conserva.

## Pendiente

Review independiente y ejecución final congelada de integración. @s27 layout y foco real se verifican con Playwright, no se afirman mediante jsdom.

## Handoff final

38 tests verdes, build/lint verdes. Mutación final 143/148 = 96.62%; cinco sobrevivientes equivalentes documentados individualmente en progress/mutation_create_project_frontend.md. Ninguna exclusión, ningún umbral rebajado. Integración comunicó 8 E2E verdes con teclado real, 320/768/1440/reflow y axe inicial/guardado; repetirá después de la pequeña corrección geométrica del skip-link. Cambios limitados a frontend/ y estos dos informes. Sin commits propios ni edición de estados raíz.
