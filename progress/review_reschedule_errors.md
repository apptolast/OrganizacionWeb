# Errores HTTP13 — APPROVED parcial

Revisión independiente del coordinador, limitada a BlockController, ApiErrors, RescheduleErrorsApiTest y la bitácora del autor. No cierra la funcionalidad ni el backend completo.

Diff y fuente leídos en ffcd8a: cuatro handlers existentes se trasladan al advice compartido. Presupuesto, solape, offsets y disponibilidad/tarea conservan los mismos status, mensajes, campos y content-type. Se retiran las copias locales; no se duplica lógica ni se cambian los errores privados de JSON, idempotencia, parser o cursor. La solución corrige la frontera HTTP común sin modificar el dominio ni Store.

Los nueve casos usan MockMvc con filtros Spring y PostgreSQL/Flyway reales, Clock fijo y fixtures coherentes. Comprueban presupuesto con días exactos, solape aunque exista consentimiento, disponibilidad antes de elegibilidad, zona retirada, tarea completada, ambigüedad DST, offset incorrecto, hora inexistente y privacidad por propietario. Cada respuesta tiene cuerpo cerrado, no-store y tablas de planificación/cambios/eventos/preferencias intactas. Esto no se presenta como una prueba de navegador ni de red TCP.

Verificación independiente fdcd0b: 224 pruebas XML verdes, sin errores ni omitidas (9 nuevas, 173 de bloques11, 42 de ProjectApiTest). Los tres hashes Java y el de bitácora coinciden con la entrega. El autor registra regresión 8d3280 EXIT0 y GJF focal efa03f EXIT0. El fallo de fixture de tarea completed se distingue de los RED reales por respuesta500; los casos que reutilizan handlers ya corregidos se registran inicialmente verdes.

Se aprueba integrar únicamente el diff posterior a f0fde3e. No integrar el snapshot Java d3ffecf: otros autores han avanzado desde ese corte. Después, incluir los handlers compartidos y los nuevos tests en la regresión y el alcance PIT definitivo de Replanificar.
