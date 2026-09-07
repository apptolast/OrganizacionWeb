# UX del cierre de sesión16

Criterio: docs/ux-requirements.md y close_work_session@s41. Se reutiliza la matriz geométrica15 con evidencia nueva16; no se trasladan sus resultados. Producción congelada durante pruebas. El primer caso único recorre formulario con notas largas, envío pendiente, respuesta incierta, recuperación del cierre y fallo de lectura tras recarga. El POST llega al backend real; la respuesta503 se controla después de confirmarse allí. Las mediciones incluyen textarea además de botones/enlaces,31anchos y altura reducida400a768.

Estado: primera ejecución en curso. Incidente de preparación identificado: faltó rellenar duración de inicio14 antes de esperar el POST; todavía no hay evidencia de defectoUX16 ni resultadoGREEN. No se atribuyen geometría, axe, zoom o motores antes de ejecución válida.

Se preservarán rutas .e2e-work/close-work-real/{engine}/{ux,text200} y un perfil nativo propio. Capturas grandes fuera deGit. Las capturas fullPage del skiplink se contrastarán con rectángulo/viewport antes de inferir superposición. Dispositivos físicos, teclado virtual, áreas seguras y estudios con usuarios permanecen límites explícitos.

## Primer caso y defectos reproducidos

- Preparación: syntaxcheck966732 detectó paréntesis extra al copiar cabecera; corregido antes del runner. No defecto de producto.
- Runner35800 EXIT1 d6c40c: timeout180s esperandoPOST14; el guion no rellenaba duración, requisito existente. Se añade fill25 y se conserva el mismo oráculoUX.
- Runner66324 EXIT1 e85063: primera ejecución válida alcanza formulario16. RED real a320: enlace Volver a la tarea112,703125×21, por debajo44px; página320sin overflow y otros controles medidos≥44. Geometry.json conservado. Root autoriza mínimo SCSS propio del lector, pendiente revalidar. TS/TSX intactos; no se atribuye fallo al API.

## Chromium primer GREEN

Mínimo autorizado en work-session.scss .reader.work-session a: inline-flex, min-width/min-height44. Formato959304 y SHA0E41A5408141BFE004548018AC35CF5C32E9B239955759FA18E304D7DB84FC80; TS/TSX intactos.

Runner6232 EXIT0ded1ca:1/1 en11,7s.155 mediciones (31anchos×5estados),5axe sin violaciones; feedback3,4ms desdeEnter al anuncio antes de liberar respuesta (c9786c). El cierre se confirma en servidor, entrega sustituida por503, recuperaciónK real deja una sola filaCLOSE. Incertidumbre320viewport inspeccionada: focoheading visible, aviso y control sin superposición; skiplink sinfoco fuera viewport(-100..-55). Rutas .e2e-work/close-work-real/chromium/ux. Geometry inicial defectuoso preservado como geometry-initial-44px.json, SHAddda83/51817A12…004520A.

Tras este GREEN se extrae el recorrido común del único caso, sin cambiar oráculos, y se añade solamente segundo caso texto200 (3anchos×5estados), pendiente de ejecución. No se cuenta comoGREEN ni se añade nativo antes de cerrar este ciclo. Puerto18080 cedido aC para@s39.

## Cierre de modalidades

Texto200 Chromium:36257a EXIT0,1/1,15medidas/5axe0. Después, mismos dos casos enFirefox yWebKit:3fc885 EXIT0,4/4;170medidas/10axe0 por motor. No se añadieron casos nuevos entre motores. Verificación de JSONf1449c confirma510medidas/30axe0 antes del nativo.

Sólo después de esosGREEN se añadió el tercer caso de zoom nativo, reutilizando el recorrido e inspectores existentes.1ae8ae EXIT0,1/1,5medidas/5axe0;6e0bde verifica zoom2,DPR1,5→3,320CSS y feedback2,2ms. Captura CDP de error nativo inspeccionada con texto/foco/control legibles; contexto aislado cerrado porfinally. Runner23576 retiró stack por cierre normal.

Freeze UX: e2e/close-work-session-ux.spec.mjs tiene3casos,7ejecuciones finales de modalidades/motores.515medidas/35axe0, detalle30principios en ux_close_work_session.md. El único RED de producto fue44px e85063; las primeras ejecuciones válidas de ampliación/motores/nativo fueronGREEN y se registran así. TS/TSX no se tocaron, sólo SCSS autorizado0E41…; sin nuevas campañas/globales. No hay runner propio activo ni reserva18080 al entregar.
