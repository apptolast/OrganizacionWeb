# Revisión independiente: estado y clientes de personalización21

Dictamen: existen huecos observables de pruebas que justifican refuerzo acotado. Esta lectura no demuestra un defecto de producción ni atribuye kills futuros. No se han modificado producto, tests ni configuración ni ejecutado campañas.

Original: `customization_stryker_original/mutation.json`, SHA256 `3DC6D9B548B532389660A53B9915B620683C552032FEF8E3510A10AFAE997942`. Universo completo:1824;1391Killed/413Survived/11NoCoverage/9RuntimeError/0Timeout; score informado76.64%. Se preserva íntegro y no se calcula una suma de campañas.

Alcance revisado: customization-api408K/92S/2NC; custom-fields-api291K/72S/1NC; customization-state257K/88S/3NC/4RuntimeError. Inventario adjunto `customization_state_clients_residues.json`:262 objetos noKilled con archivo, ID original, mutador, rango, replacement y razón original. Son252S+6NC+4errores, no262 fallos de producto.

## Prioridades propuestas

Se recomienda un archivo nuevo de pruebas de clientes, conservando los originales. Cada oráculo debe ejecutarse individualmente y registrar inicialmenteGREEN si ya cumple. Los IDs son objetivos candidatos, no kills acreditados.

1. **Confirmación completa con varios campos (@s28).** Configuración previa con dos definiciones distintas, incluida una inactiva. Primero confirmar nominalmente creación sobre configuración existente y actualización preservando todos los campos y vista; después recibos coherentes pero alterando identidad, tipo o activación del campo que no se editó. Comprobar rechazo público específico de incompatibilidad, evitando que un TypeError incidental satisfaga el oráculo. Firmas996 elimina verifyPrivateRevision en update;1001 elimina comprobación de longitud;1012/1016/1018 eliminan igualdad de ID/tipo/active en sameFields. La revisión del update requiere también un recibo válido salvo revisión antigua o instante anterior por un microsegundo. Las pruebas existentes de revisión se concentran en vista/primera creación; no acreditan esa llamada eliminada.

2. **Confirmación de varios valores (@s28).** Dos campos con tipos/valores distintos: una respuesta conserva el primero y cambia el segundo, otra conserva valores pero cambia identidad/tipo u omite una entrada. Nominal previamente válido y rechazo preciso después. Objetivos353/356/363 para conjunto completo y every→some;372/375/382/386 para orden/identidad/tipo preservados. No repetir la matriz de tipos del backend.

3. **Escritura TASK pública (@s10/@s11).** Guardar valores de una tarea mediante cliente real, verificar URL completa proyecto/tarea, PUT, If-Match compuesto y confirmación canónica. NC302 es el fragmento TASK del endpoint; NC1516 pertenece a la selección TASK al aceptar la confirmación en estado y necesita consumidor UI real, no un getter interno. El GET TASK existente no recorre estas escrituras. Coordinar el consumidor con C para evitar duplicación.

4. **Valores válidos en fronteras (@s27).** GET con BOOLEAN y DATE nulos, preservándolos; exactamente12 valores válidos; revisiones compuestas en máximo long y una versión de20 dígitos rechazada sin redondeo.185/204 hacen incondicional validación de valores no nulos,265 cambia >12 por >=12;83/84/87/88/90/91/93 corresponden al límite de revisiones. Los oráculos negativos de13 o long+1 no demuestran por sí solos aceptación del extremo permitido.

5. **Problema400 íntegro y aborto (@s34/@s37).** Un problema válido con dos errores por índices distintos debe producir el error de validación público; un solo miembro extra/inválido o índice igual a longitud debe preservar el fallo indeterminado completo. Abortar mientras json() de400 está pendiente debe rechazar como abortado al resolverse, sin publicar validación vigente. Candidatos1149/1150/1152/1157/1160/1163/1164/1165/1178/1179/1181/1190/1193 y312/317/321/326/327/330/331/333/334/336/337;1129 elimina comprobación de aborto y1132/1133 son mensajes NC de esa rama. No perseguir cambios de texto de excepciones internas si no alteran el resultado observable.

6. **Recuperación e independencia de consumidores (estado).** Después de escritura incierta, lectura manual válida y nuevo PUT confirmado; mantener simultáneamente borrador de otra entidad/ámbito y comprobar que no desaparece ni se transmite.1324/1325 y1451/1452 eliminan retirada de incertidumbre/obsolescencia; mutantes de vaciado de mapas como1258/1281/1321/1342/1388/1457/1493/1558/1572 necesitan un segundo consumidor para observar la pérdida. Cambio de esquema PROJECT no debe retirar datos TASK;1238/1240 afectan discriminación de ámbito. Priorizar un flujo público de recuperación y uno de aislamiento, no una prueba por setter.

7. **Identidad de esquema al aceptar respuesta tardía (estado).**1304 elimina comparación de UUID conservando comparación numérica. Un GET/ACK con identidad diferente y versión igual o superior debe rechazarse sin sustituir el esquema vigente. Las pruebas154/155 acreditan respuestas anteriores en la misma identidad, no esta frontera. Coordinar con los refuerzos UI de C antes de añadirla.

## Límites y residuos conservados

No se pide una prueba por superviviente ni perseguir100%. Mensajes internos, mutaciones de limpieza de mapas al desmontar y guardas duplicadas por cliente/UI requieren demostrar una diferencia pública antes de invertir en oráculos; quedan explícitamente como residuos, sin equivalencia general. Cambiar incremento de generación por decremento puede seguir invalidando la dependencia: no se prueba el número interno.

RuntimeError1410/1413/1416/1420 se conservan: el runner informó caída y TypeError Cannot convert object to primitive value en su conversión del error. Esta evidencia no identifica por sí sola un fallo productivo ni permite reclasificarlosKilled. Los otros cinco errores del universo están fuera de este alcance.

Toda propuesta debe conservar80, todos los candidatosVitest y universo original. Un eventual incremental completo debe ser autorizado y conservar su propio resultado/estados; no sustituye retrospectivamente el original.