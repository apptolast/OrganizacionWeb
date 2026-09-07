# Fixture15: fin histórico frente al nuevo panel17

Lectura COMMON ca1768/5ec267/6f0866: el caso78 del global7009 falló después de inicio, pausa, reanudación y recarga. El time exacto de plannedEndAt aparece tanto en el párrafo histórico «Fin previsto:» como en «Fin previsto original:» del panel17. No evidencia cambio de instante ni error de transición.

Error-context copiado sin modificar a progress/pause_e2e_historical_end_error.md, SHA B4EB53A4C051D9597E35A1860245B4422DF42332C8B0DCFFA9FDF968733363BF (e10352). Corrección autorizada exclusivamente en el test aislado: getByRole(paragraph).filter(hasText:/^Fin previsto:/) antes del time exacto. Conserva toBeVisible, comparación de sesión/tiempo trabajado y SQL de estado/revisión/cambios/intervalos. Búsqueda del mismo archivo encuentra sólo este selector datetime/plannedEndAt; no se amplían cambios.

Formato e10352 GREEN. Foco pendiente de liberación18080; no runner, Java, fuentes frontend ni campaña ejecutados durante el global/Stryker.
