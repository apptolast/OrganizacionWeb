# Importación23: E2E nominal real

Corte fijo DETACHED `f3bed6f` en `work/OrganizacionWeb-import-e2e`, que incluye frontend `d2fa8b2`. Sólo se transfirieron el nuevo `e2e/import-data.spec.mjs` y la expectativa de menú de `e2e/export-data.spec.mjs`: Hoy primero, Exportación penúltima e Importación última. Install oficial con lock, EXIT0. No se utilizó backend mutable de PRIMARY ni live.

Primer intento `import_real_nominal.log`: EXIT1 del instrumento Playwright. Preview real200, hash SHA256, byteLength y recuentos2/1/1 eran correctos; `postDataBuffer()` de File nativo no interceptado devolvió null. Root aprobó conservar la evidencia de bytes mediante hash/tamaño calculados por el backend frente al Buffer original, tanto en preview como en recibo, manteniendo cabeceras, recuentos y todos los demás oráculos. No hubo mocks, interceptación ni cambios productivos.

Segundo intento `import_real_repaired.log`:1/1, EXIT0;6,1s de prueba y8s de Playwright. Se parte de un proyecto creado porHTTP y su registro durable exportado; se prepara explícitamente un archivo con ese registro más otro UUIDnuevo, manteniendo las demás colecciones vacías. Este paso es preparación de un archivo portable, no una exportación original sin modificar ni cobertura de14familias de negocio.

La UI no envía al abrir/elegirFile. Preview muestra uno idéntico y uno nuevo sin escritura. El segundo gesto confirma exactamente la misma longitud/hash e identidad de petición. SQL comprueba el proyecto añadido, su owner/valor/versión y conserva exactamente proyecto previo y eventos outbox. La recuperación GET devuelve el mismo recibo y existe una única fila import_receipts propia. Esto acredita ausencia de eventos nuevos en outbox; no observación de un broker.

Ambos pases conservan682entradas idénticas antes/después. Evidencia copiada a `progress/import_real_e2e_nominal`: before/after iniciales y corregidos, ambos logs y `result.json`. Stacks15968/66728, redes y volúmenes propios retirados por el arnés. La limpieza SQL se limita a requestKey/owner del test y sus dos UUIDde proyecto mediante helper que rechaza entornos no efímeros. Ninguna escritura live ni cambios de8080.

Límite: nominal parcial sobre ese corte fijo. Quedan regresión integrada final, guardas backend y compatibilidad23→22→23 sobre el corte definitivo; no se declaran verdes por este único caso.
