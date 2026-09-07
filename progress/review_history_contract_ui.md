# Revisión independiente del contrato18: cliente e interfaz

**APPROVED para TDD**, sin bloqueos contractuales pendientes en el alcance revisado. No acredita implementación ni ejecución de los ejemplos.

Corte final: `features/history.feature` SHA256 `768AA48A5A0495BDC5AA292395F1DC42DADD0BD7702987F50D2FAD4664B72C13`, verificado en 0e3264. Normativa: sección18 de `project-spec.md`, corte D8A4C5B274832563B67EDF988B6733760107EDF70563191A73195B6582FE7245. El autor declara 39 escenarios, 142 ejemplos expandidos y 39 When; esta revisión no ejecutó un parser ni equipara esos ejemplos con pruebas realizadas.

## Alcance contrastado

- @s1–7 y @s13: cinco familias durables, identidad `(type,id)`, detalles históricos discriminados y etiquetas actuales. Reservas, trabajo, cierre y finalización de tarea conservan significados distintos; no se inventan agregados ni causalidad por UUID.
- @s8–12 y @s14–24: contexto propio, fechas del hecho en UTC, paginación y errores son coherentes con la consulta que presentará la interfaz. La continuidad admite commits tardíos detrás de la frontera y no promete un snapshot entre páginas. Root revisa adicionalmente la realización backend/cursor/tiempo.
- @s25–27: precisión decimal y temporal exacta, atribución histórica persistida, DTO cerrado y rechazo integral de páginas incompatibles con identidad, filtros, orden o unicidad. Se reutilizan los contratos de details existentes sin añadir campos ni relajar su aritmética.
- @s28–33: una vista accesible desde navegación y detalles existentes; una página por URL, aplicación explícita de filtros, quitar contexto, Back y recarga. Los enlaces reutilizan destinos existentes y details no requiere consultas de estado por fila. Notas permanecen texto con espacios y saltos; los hechos no confirman intenciones pendientes del navegador.
- @s34–38: carga, vacío y fallo diferenciados, reintento GET con filtros conservados, retirada privada ante 401/404 vigentes y descarte de respuestas anteriores antes del observador de sesión. El foco depende del iniciador y de la elección posterior de la persona.
- @s39: exige evidencia posterior de las 30 filas UX, matrices y ampliación existentes; no sustituye evaluación humana ni acredita ahora resultados de navegador.

## Delta puntual ratificado

La lectura inicial del corte 755D5C…440A2C detectó dos precisiones de fixture, comunicadas al autor y a root. Ambas están resueltas en el corte final:

1. @s8 usa ahora proyecto en estado `idea`, perteneciente al catálogo real, en lugar de proyecto pendiente.
2. @s13 declara dos SESSION_CHANGED y una entrada de cada una de las otras cuatro familias: seis hechos, coherentes con el resultado esperado.

También se verificó la precisión de root en @s25: no recalcular la atribución histórica con TZDB. Esto conserva el formato visual mediante Intl y el fallback UTC etiquetado; no prohíbe formatear fechas.

No se solicitan escenarios adicionales ni una matriz duplicada de contratos heredados. No se modificaron Gherkin, producción o pruebas, ni se ejecutaron suites. La activación y el commit del contrato corresponden al coordinador.
