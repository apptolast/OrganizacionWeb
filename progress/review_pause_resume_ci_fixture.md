# Revisión del fixture CI15

**APPROVED.** Lectura independiente71f789 del único delta `work-session.test.tsx` @s37 y bitácora. Hash confirmado `DAE59C04C0934B7E3679C4A2281CEA951C5A6EB1E54B7D627A007D74A3400BA4`. Sin ejecución nueva ni cambios de fuente/test por el juez. Ponytail full/Caveman lite.

La corrección separa respuestas por URL: primer active devuelve null, POST de inicio devuelve201 con Location/recibo originales, GETstate recibe snapshot15 válido y su token, segundo active devuelve503. El fallo de active ya no depende de que GETstate consuma antes una tercera respuesta genérica. Rutas inesperadas fallan explícitamente.

Los oráculos conservan fin previsto, confirmación histórica y ausencia de nuevo inicio. Además exigen el texto exacto del fallo de active mediante el único role=alert, estado En curso, dos consultas active y un solo POST. No se oculta la segunda alerta, no se usa first() ni se relaja el producto: se elimina el error artificial de state que no pertenecía al escenario.

La bitácora conserva CI1667/1668, pasada focal inicial sensible al orden y RED controlado04acda antes del arreglo. Autor reporta43/43 y1668/1668 GREEN8c78b0 con formato/ESLint focales; este dictamen no los reejecuta ni inventa un RED de producción. El delta es adecuado para integración selectiva del fix15 y no invalida la propuesta16 aprobada.
