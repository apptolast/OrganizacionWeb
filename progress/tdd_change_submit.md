# TDD ChangeSubmit

Cesión extraída sin semántica nueva por autor principal: b8cd58,27GREEN. Sólo change-submit.tsx/test y esta bitácora. Ponytail full/Caveman lite. Focal propio, sin globals ni backend.

1. @s35 cancelación rechazada: RED3aa140 callback ausente, GREEN3f8a3c. Props onRejected opcional y rechazo local recuperable con consulta deliberada.
2. @s35 matriz rechazos movimiento: primeros55c3bb/7ac694 eran fixture inválido (date/startTime en vez de startLocal/endLocal), no evidencia de producción. Se retiró ampliación provisional, corrigió fixture y reprodujo12RED3e55d8 antes restaurar mínimo reconocido. PNPM emitió además error secundario0daf09 tras informe completo. GREEN33c08e13/13 con12problemas contractuales reales.
3. @s37 acceso vigente: REDf00229 callbacks401/404 ausentes; añadido onAccessFailure opcional, tras guardas/clasificación.
GREEN ciclo3 ac1c2e. Advertencia de proceso: ciclo2 incorporó simultáneamente12variantes bajo un test parametrizado del comportamiento «rechazos reconocidos», no12ciclos individuales. Esto se aparta de la granularidad solicitada si se consideran casos independientes; no se reescribe evidencia ni se simulan rojos. También se agruparon los focos parametrizados siguientes tal como se registran; próximos oráculos serán individuales.

4. @s33 cinco clases de incertidumbre inicialmente GREEN4a6226; sin producción adicional.
5. @s34 cancelación/movimiento, ausencia comprobada y reenvío manual idéntico inicialmente GREENaffb16; sin producción adicional.
6. @s38 foco enviar/comprobar: REDa22b22 sólo variante foco local (externo ya verde), GREEN8632fb. Ref de control y recuperación sólo si BODY.
7. @s35 @s38 recuperación estado tras412/cancelled:2RED0cda96, GREEN32258b; mismo ref en acción manual.
8. @s35 CSRF integración real useSession/apiRequest: inicialmente GREEN0f5fa0; dos acciones manuales separadas, token renovado y misma intención/revisiones; no renovación paralela nueva.
9. @s37 tres contextos proyecto/tarea/bloque: RED e84789 porque petición anterior no abortada; remontaje por identidad/revisión invalida petición y memoria retenida. Parámetros añadidos antes del recordatorio de granularidad individual de raíz.
GREEN ciclo9 963a0e.
10. @s37 JSON definitivo demorado tras cierre: inicialmente GREEN8bdda0, guardas existentes; ninguna edición productiva.
11. @s37 JSON CSRF demorado tras cierre: inicialmente GREEN0f10de, observer compartido no recibe403 obsoleto; ninguna edición productiva.
12. @s36 @s38 recibo válido, dobleclick y foco externo: inicialmente GREEN27af49; entrega una confirmación sin robo de foco ni segundo POST.
13. @s35 BLOCK_NOT_FOUND definitivo distinto de acceso padre: REDd3d3ac, ampliación mínima de lista reconocida; GREENa5f50734/34.
14. @s37 recibo válido cuyo JSON acaba tras cerrar: inicialmente GREENdba58b, no callback/robo de foco.

Formato focal53dcc3 y ESLint e5c5f4 EXIT0. Se conserva semántica actual de SessionGate/useSession y se comprueba hook real más apiRequest; no se añade lector de sesión ni se modifica infraestructura compartida. Prueba CSRF usa un host de prueba del hook, no render completo de SessionGate/App; la integración completa corresponde al autor principal.

Mapa: @s33 cinco incertidumbres; @s34 reenvío manual cuerpo/key/If-Match/Availability-Revision; @s35 rechazo conocido,412manual y CSRFmanual; @s36 entrega de recibo; @s37 ruta/identidad,401, clasificación y JSON tardíos; @s38 foco envío/comprobación/error y control externo. Padre conserva borrador/retira preview mediante onRejected y resuelve onReload deliberado; comprobación de ese padre fuera de este archivo. Sin globals, tsc global, backend ni mutación.
Freeze:35/35 GREEN77a664 tras formato. diffcheck550869 EXIT0. SHA256 fuente04D15F2DC099C38B6B3E4A34CE5CB5E3CC3A9A2692823F4E476135E600E37775; testDE43D899679341A94786C316397AEF58FE910970553C4B85346BD69BEFB0F9A9.

## Revisión raíz — anuncio de trabajo pendiente

Oráculo individual @s33/@s38: POST deferred, transición incierta, GET de comprobación deferred. Exige role=status durante ambas esperas, retirada al terminar, conservación de foco/control y mensaje de incertidumbre. RED2bfef1 por ausencia del anuncio. Cambio mínimo: un párrafo «Procesando cambio» con role=status condicionado por busy. GREEN133407:36/36. No se modifican callbacks, padres ni API.
Freeze renovado: formato629722, ESLintaf06c9, diffcheckd919fa EXIT0. SHA256 fuenteE12B6C1F5ABB4777976EF3792520D4365D53B8A4F5C8E3F06D1AE057A692FB02; test643384ED97528A17A1A98B204D5FA0692CE3FF9973F08BF1A6EB8B700828845F.
