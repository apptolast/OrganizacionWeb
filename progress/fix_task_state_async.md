# Corrección del fixture asíncrono TaskState

Corte test-only: una línea añadida a frontend/src/task-state.test.tsx en
@s10 «distingue historial vacío confirmado de la carga». Producción intacta.

RED remoto real: CI34135534841 sobre ca09398, log
progress/weekly_review_ci_ca09398_failure.log, líneas604–609, TypeError
finish is not a function en línea295. SHA del log preservado:
A79F865D883642F3DB3FE7D44160CFC444FC427C0C1C2F1437C92952D1CE7F35.
No se inventa reproducción roja local ni se repite CI para obtenerla.

Causa: TaskHistory renderiza carga con page undefined; el GET nace desde
useEffect (task-history.tsx:26–42). findByText confirma ese render, pero no
establece que el mock de fetch haya asignado su resolvedor finish. La llamada
al resolvedor podía adelantarse al efecto bajo la planificación del runner.

Corrección: waitFor(() => expect(finish).toBeTypeOf("function")) justo antes
de resolver la respuesta, con la misma pauta existente en esta suite. No
se altera el mock, ni las aserciones de carga/ausencia de vacío/vacío final/
acción habilitada, ni timeouts, ni sleeps. Es sincronización de preparación
del fixture, no relajación del comportamiento esperado.

Diagnóstico limitado: rg de finish/waitFor en esta suite identificó casos
ya sincronizados y otros resueltos tras acciones o respuestas anteriores.
No se modifican otros casos sin fallo sustentado; no se afirma que todos
ellos hayan sufrido la misma carrera.

Validación: pnpm --dir frontend exec vitest run src/task-state.test.tsx,
ffb9bd EXIT0,55/55. Primer check Prettier d8e68a detectó formato de la línea
insertada y detuvo la cadena antes de lint. Prettier --write focal y ESLint
focal posteriores917c70 EXIT0. Diff final exacto de una línea; no hubo
cambio semántico por formato ni se ejecutó frontend global.

SHA final del test:
3F5122BED6BD5ABB9EFE26C787EF988AB22F3C3F81186718280C2C69F1CED16C.
Congelado para revisión/commit root. CI correctiva y cualquier gate global
quedan bajo su coordinación. No Git mutation ni cambios de Java/JS producto.
