# Espera de efectos en work-session: reparación del arnés

CI PR26 34192842547 falló en @s33, work-session.test.tsx:396: esperaba cinco llamadas y observó tres. El log remoto original externo export22-closure-ci-failure.log tiene SHA256 4DA4FD096CA795EC6EF4D67F4A66DF975DBB8DD80CA6972C22A05BE55A3280A5. Main83b027 había pasado con producto/tests iguales. No se atribuye un defecto de producto a este fallo.

## Causa y alcance

work-session.tsx confirma el recibo mediante setConfirmed/setActive; el padre muestra «Sesión iniciada» en ese render. Los dos GET adicionales (/state y /end-time) parten de useEffect en WorkSessionStatePanel (línea117) y WorkSessionEndPanel (línea169), montados por ActiveSession. Encontrar el texto del padre no constituye una barrera para esos efectos posteriores.

Se revisaron las cinco aserciones de cinco llamadas de esta suite. Las líneas396 y713 comparten fireEvent + findByText del padre; ahora esperan el mismo conteo exacto mediante waitFor. Las líneas431 y1398 ya lo hacían. La774 conserva su aserción síncrona: la prueba resuelve las promesas mediante dos await act(async...), incluido el lookup antiguo, antes de consultar las llamadas; esa barrera vacía los efectos. No se cambiaron los otros casos por semejanza textual.

Los dos oráculos siguen exigiendo exactamente cinco llamadas, la ruta por clave original, /state y un único /end-time. Mocks, datos, producción, timeouts, configuración y scope Stryker permanecen idénticos. No se añaden pausas ni reintentos de CI.

## Evidencia secuencial

El primer intento local no ejecutó Vitest porque este checkout documental no tenía node_modules (EXIT1, log work-session-before). Se conserva como fallo de entorno, no RED del test. Install oficial posterior EXIT0 c12888. El caso original sin cambios pasó localmente a11055; el RED conductual conservado es exclusivamente el remoto.

Tras el cambio mínimo autorizado, suite completa work-session47/47 EXIT0 a7a81f; frontend global2295/56 EXIT0 0fcbac; Prettier focal EXIT0 y lint frontend completo EXIT0 59c9f7. Sin Java/init, build ni mutación. Se contrastaron171 entradas frontend: sólo cambia este test respecto a HEAD42639f7 y ninguna cambia durante los gates. Logs completos e inventarios antes/después están fuera del repositorio en work/deployment-preparation/export22-closure-*. El manifiesto export22_work_session_async_freeze.json recoge rutas, tamaños y hashes.
