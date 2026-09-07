# Revisión del checkpoint de consulta y persistencia nominal16

Aprobado como dependencia compilable para continuar HTTP, no como cierre de la feature. Rootde87a8/f132a9 revisó las siete fuentes del manifest y sus tres pruebas afectadas. La bitácora acredita34 core verdes, los ciclos PG nominal/recuperación/propiedad y forwarding, y regresión focal d8c267 con formato. No equivale a suite backend completa.

El núcleo conserva precedencia revisión/estado/máximo antes del reloj, suma sólo running y rechaza tiempo/localdate fuera de rango. Fallback UTC se limita a zona histórica irresoluble. Notes normaliza null y preserva espacios/pares Unicode válidos; rechaza exceso, NUL y surrogates aislados con error de campo. Se corrigió el fixture de año local cero para partir de pausa inmediata con acumulado0; pasó inicialmente verde sin alterar producción.

Store escribe el último intervalo, el recibo CLOSE y el evento concreto en su transacción existente. La prueba nominal contrasta JSON esperado independiente, el evento de inicio intacto y los valores SQL. La consulta por sesión comprueba propiedad y lee el recibo en la plantilla RR read-only existente; la prueba posterior a retirar outbox demuestra independencia de ese almacenamiento, no publicación real. Los puertos incluyen sus implementaciones reales para evitar copiar una interfaz que deje clases concretas sin compilar.

Pendientes explícitos: intención de replay con notas, cierre paused persistido, colisiones/concurrencia/rollback y todas las variantes de lectura/snapshot/upgrade. El paquete no acredita atomicidad completa16 ni smoke, UI o mutación. Los recibos/eventos base ya estaban en db8bb0e y no se duplican.
