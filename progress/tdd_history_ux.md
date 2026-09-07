# TDD UX Historial18

Primer caso individual en history-ux.spec.mjs; corte241a2f0 UI final/PG nominal, sin delta final PG. Plan:31anchos, cinco estados y teclado real; ejecución aún parcial por RED temprano.

## RED inicial real

Sesión77683, EXIT1 5d013a. A320, details, nav Hoy mide41,359px de ancho frente a44 requerido. JSON también observa Categoría19px alto, fechas27px, enlaces cortos/summary21px. La página no desborda (scroll320). No se declaran solapes de cajas inline como colisión efectiva: requieren remedir después del ajuste de áreas. Tab alcanzó summary, el foco fue visible y Enter abrió details. No se alcanzaron las otras30anchuras, axe ni estados posteriores; no se acredita la matriz30 por este fallo.

Evidencia copiada sin mover originales a history_ux_initial_artifacts (geometry, captura y error-context); log history_ux_initial.log con RUN_EXIT=1. Before/after323 entradas:322 iguales y PostgresHistoryQueries.java distinto al preservar after (94411f). Se notificó root para atribuir su copia coordinada; no se declara producción idéntica entre esos manifiestos. El fallo medido es CSS de frontend. Se informó root/B antes de cualquier CSS; C no cambia producción. Stack59880 retirado por lifecycle,18080 y Gradle libres. Propuesta mínima para B: áreas44 de controles/links/summary de .history y ancho mínimo del enlace Hoy, verificando reflow de navegación completa.

## Corrección mínima y ejecución intermedia

Root confirmó que el cambio PG del primer after ocurrió después del lifecycle, durante preservación: commits b639336/f03ce2f. Se conserva la comparación322/1; no hubo cambio de frontend durante ese RED.

history.scss incorpora áreas44 en filtros/links/summary y rejilla de etiquetas. Intento939b05 EXIT1 conserva el mismo RED Hoy: sustitución textual inicial no modificó styles.scss por diferencia de finales de línea (comprobación lo detectó durante run; no se editó entonces). Log geometry y copia history_ux_nav_omission_geometry.json preservados. Después se aplicó el único min-width44 de .sidebar nav a con verificación literal antes de ejecutar de nuevo. Ninguna fuente TS/TSX modificada.

## Geometría corregida; foco Limpiar pendiente del ajuste de B

Run7009 EXIT1 beffd9 supera124medidas (4estados ×31), cuatro axe sin violaciones y feedback medido; se detiene en foco de Limpiar. El oráculo exigía enlace enfocado, pero el formulario keyroute desmonta al iniciador: corresponde h1 según contrato. Se corrige sólo esa expectativa antes del siguiente run. Root detectó además ausencia de captura del iniciador en producción y B prepara su corrección puntual; no se atribuye GREEN global ni la quinta matriz aún. Capturas/JSON copiados en history_ux_geometry_fixed_artifacts sin mover originales. Runner22884 retirado, puerto libre para copia del bundle de foco.

## GREEN del primer UX sobre fuente final

Corte4d42253 incorpora foco de B y PG final. Mismo caso con oráculo h1 corregido GREENd934ae, EXIT0 propio, 1/1 (13,8s; Playwright16s).155medidas: cinco estados ×31anchos, incluida altura400 a768;5axe sin violaciones. Tab/Enter reales alcanzan details, quitar contexto, aplicar, reintentar y limpiar; el foco de h1 tras reintentar/limpiar desaparecidos se comprueba en navegador. No se atribuye todavía navegación física de paginación ni respuesta obsoleta a este primer caso.

Copias de capturas/JSON en history_ux_verified_artifacts; originales conservados. Before/after323inputs idénticos. Los dos SCSS y este primer test quedan congelados para integración root; texto200/zoom/motores son ciclos posteriores, todavía no acreditados. No se certifican los30principios por axe ni dispositivos/lectores físicos. Runner16288 retirado por lifecycle.
