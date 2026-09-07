# Review de scopes y dispatcher17

APPROVED para preparar las campañas, sin autorización de ejecutarlas antes del freeze final.

Root revisó los cambios completos eb2331 y manifiesto16ab32:19 patrones Java incluyen casos de uso, guardas compartidas, Store, HTTP, publicador y wiring; se conservan todos los candidatos JUnit. Stryker contempla siete fuentes nuevas/compartidas previstas y conserva la exclusión protegida, Vitest y análisis por test. Umbral80 y concurrencia4/8 permanecen. Los informes y sandbox17 tienen rutas propias; destinos y snapshots anteriores no cambian. Contrastar de nuevo fuentes finales y hashes antes de medir.

Cuatro ciclos de arnés documentados;40 pruebas Node verdes EXIT0 a1beec, formato a1ac3c y diffcheck9827ba. Dry-run Gradle EXIT0 be4f62 sólo acredita configuración: tareas SKIPPED, ninguna campaña ni compilación de fuentes. Tests del dispatcher observan llamadas del runner inyectado y no ejecutan mutación. Este paquete no acredita calidad por mutación de17.
