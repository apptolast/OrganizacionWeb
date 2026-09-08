# Mutación frontend de integración API: campaña original

Campaña oficial `node scripts/project.mjs mutate integration_api-frontend`, EXIT 1. Universo completo: 997 mutantes, tres módulos completos y siete nodos AST aprobados. Resultados originales: 780 Killed, 208 Survived, 8 NoCoverage, 1 RuntimeError y 0 Timeout. Puntuación conservadora 780/997 = 78,2347041123 %, inferior al 80 %. La puntuación del motor 78,31 % excluye el error; no se presenta como puntuación estricta.

Raw JSON SHA256 80585FD8F8323FC505617158E23B97F82C9CE65FACD7922892BF276A8CC73F2F. JSON, HTML, log, EXIT, configuración original, inventario completo y resultados están preservados en `integration24_stryker_original`; el manifiesto hermano `integration24_stryker_original_freeze.json` fija sus bytes. Los 192 inputs antes/después sólo difieren en `scripts/project.test.mjs`: nueve expectativas históricas Node autorizadas por root, fuera del conjunto Vitest. Fuentes, pruebas frontend y configuración efectiva permanecen idénticas.

El RuntimeError 712 procede del runner Vitest/Stryker, `errorToString: Cannot convert object to primitive value`, después de dos intentos internos. Se conserva como error, sin reclasificación. El mensaje final de pnpm sobre el comando acompaña al EXIT 1 por umbral; el informe registra ejecución completa y 997 estados.

Propuesta de refuerzos públicos, pendiente de revisión antes de editar tests:

1. Marcar/desmarcar permisos y elegir 7/90 días, comprobando el PUT completo: 844–851.
2. Nombre con White_Space múltiple lateral y espacios interiores conservados, frontera de 80 puntos de código: 516–522 y 532.
3. Metadata con scopes vacíos, desconocidos, duplicados o desordenados; positivos de seis scopes canónicos y nombre de 80 frente a 81 puntos: 106–127.
4. Copia correcta del secreto mediante Clipboard API y anuncio, sin persistirlo: 799–805.
5. Problemas 400/409 con status/type incoherentes no se aceptan como rechazo definitivo y conservan la identidad incierta: 611–683.

Los IDs son candidatos de sensibilidad, no resultados anticipados. No se consideran equivalentes todos los demás supervivientes ni se persigue el 100 %. Se requieren al menos 798 Killed sobre el mismo denominador para superar el 80 %. Cualquier refuerzo necesita ejecución real posterior.

Después de capturar el freeze se añadió únicamente `frontend/.stryker-tmp-integration-api/` a `.dockerignore`, por revisión root: evita enviar el scratch al contexto Docker. No cambia fuentes ni la campaña preservada; no se repiten pruebas por esta exclusión operacional.