# Revisión incremental del cliente de bloques

Estado: APPROVED para el tramo de cliente API revisado. No acredita cierre de feature ni verificación de servidor/interfaz.

Durante la implementación del preview se señalaron al autor dos casos para TDD:

1. Date.parse trunca precisión inferior a milisegundos. Una respuesta con startAt/endAt terminados en .000001Z puede parecer igual al instante pedido. Los extremos de este contrato proceden de minutos locales y offsets de segundos enteros; la comparación debe rechazar esa fracción distinta. createdAt sí conserva precisión de microsegundos y no se restringe por este motivo.
2. La igualdad con la entrada enviada no sustituye la validación del DTO. Un objetivo vacío o superior a 500 puntos de código y una zona vacía siguen siendo inválidos aunque coincidan con la petición retenida.

Ambos hallazgos se resolvieron con RED/GREEN documentado por el autor. El coordinador leyó el cliente completo, el parser de errores y las pruebas de frontera, y ejecutó `pnpm exec vitest run src/schedule-block-api.test.ts`: salida 0, 190/190 tests, 08:48:19 del runner (salida f65607). La comparación de extremos exige segundos enteros; createdAt sigue admitiendo microsegundos. La validación intrínseca del texto precede a la coincidencia con intención.

Las cinco operaciones conservan señal y transporte de sesión/CSRF. Creación y recuperación comprueban contexto e intención, sin replicar TZDB. El parser distingue errores reconocidos mediante forma cerrada, estado HTTP y código; preserva errores desconocidos como incertidumbre para que la UI decida. No se añaden dependencias ni reenvíos automáticos. Los cursores permanecen opacos para el cliente y se codifican al transmitirse.

La trazabilidad inicial de lista/detalle citaba por error los tags de concurrencia. El autor corrigió mapa y nombres a s25/s26; estos tests no acreditan carreras backend. Su regresión de cuatro archivos API reporta 499 tests verdes, separada de la ejecución independiente de 190 del coordinador.

Se puede preparar mutación de este alcance tras estabilizar las pruebas UI que lo importan. No se ha ejecutado todavía mutación de feature 11. Cualquier cambio posterior exige revisar su efecto antes de usar este dictamen como puerta.
