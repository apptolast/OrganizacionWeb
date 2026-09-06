# Refuerzos prioritarios frontend14

Baseline init438035: 1751 backend, 1571 frontend y 29 scripts verdes. Propiedad de este autor: work-session-api.test.ts y work-session.test.tsx. Producción sólo por la autora UI si un oráculo descubre un fallo real. Ponytail full y Caveman lite; sin cambios backend, harness ni reportes originales.

1. API141, POST de 25 recibe duración26 y fin coherente: inicialmente GREEN e5d3c7, 33 API. No se atribuye kill sin replay.
2. API29, problema active con code desconocido y resto nominal: inicialmente GREEN 213841, 34 API. Una fila incorporada antes de ejecutarla; las demás condiciones se añadirán después.
3. Hallazgo real de E2E incorporado como prioridad: @s32 withdraws earlier absence while a transmitted start is pending or uncertain. RED 17bf9f durante POST pendiente: el párrafo “No hay una sesión de trabajo activa.” sigue presente. El mismo recorrido comprueba luego503, incertidumbre, duración25 y ausencia de otro POST. Fuente cedida a la autora UI para modificar sólo la condición de presentación del párrafo, sin cambiar canStart, elegibilidad ni retained.

3. GREEN cfe3a8 tras el cambio de fuente de la autora UI. Su condición limita sólo el párrafo a ausencia conocida sin busy ni uncertain. Este autor no editó producción.
4. Problema simple con type contradictorio: inicialmente GREEN61c50c. Las restantes filas se registran abajo individualmente.
5. UI active409: refuerzo del test existente antes de pulsar Actualizar, inicialmente GREENae0d63. No hay afirmación de ausencia, botón para empezar ni otra petición.
6. Elegibilidad independiente: tarea completed/proyecto active, inicialmente GREENa30fd1; tarea pending/proyecto desconocido, GREEN815281. Dos ciclos sobre el test existente; se conservan sus oráculos anteriores y el nominal final.
7. Lookup viejo401 después de actualizar, con Session y padre vivos: inicialmente GREEN0bbe1b. El callback de acceso no se llama, el finally viejo no acaba la carga nueva y sólo la respuesta nueva confirma ausencia.
8. Enter incierto: primer intento d497c0 falla por montaje, no por producto. user-event no emitió submit implícito en el formulario sin botón submit; no hubo POST. Oráculo corregido: Enter no reenvía, y un evento DOM público submit cancelable verifica preventDefault y la guarda de incertidumbre. GREEN739e37. No afirma que el navegador genere ese submit nativo en esa composición.

Mapa de objetivos de los seis grupos, sin atribuir Killed: API141; API29/32/35/37/54/58/61/65/66/69/72/73/76; UI367/369/370; UI257/258/261; UI429/440; UI502/504. Reader1/3 queda fuera de la propiedad de estos dos tests. El hallazgo de ausencia añade su propio oráculo contractual, sin reinterpretar la campaña original.

## Freeze para revisión

Foco conjunto caf80f EXIT0: 85/85 (43 API, 42 UI). Son 14 casos añadidos sobre los 71 anteriores; dos casos existentes también refuerzan sus estados intermedios. ESLint d41bb7, TypeScript a5804b, Prettier aplicado b8536b y check da3ac3, todos EXIT0. Sin Stryker/replay ni suites globales.

Hashes f695c4:
- work-session-api.test.ts: 8E6C39B5A0594188F44A19F1B8B8AD052676F0DF7066B0F71C0069D65C850485.
- work-session.test.tsx: F65E0F54EEB638741C9E57E6295AB19D500F36E173D2EC6BF469CBEB590075F7.
- API productiva permanece B50C97B6309B68389257D4A02BE132789F3A1B91E082696736166786BC1D87A9.
- UI corregida por su autora: 47A35F19C2E47E35C633A3D60C0880A1822DBDD5C130ED326177892646C66005. Este autor sólo formateó los dos tests.

Todos los refuerzos residuales fueron inicialmente verdes contra producción correcta. RED17bf9f pertenece al defecto real de ausencia obsoleta; REDd497c0 pertenece únicamente a la expectativa incorrecta del montaje de teclado. No se confunden con detecciones de mutantes.
- Problema active, type: inicialmente GREEN 63de21; fila individual, producción intacta.
- Problema active, body status: inicialmente GREEN 6cc36c; fila individual, producción intacta.
- Problema active, HTTP status: inicialmente GREEN fdd4a7; fila individual, producción intacta.
- Problema simple, body status: inicialmente GREEN d00641; fila individual.
- Problema simple, unknown404: inicialmente GREEN 779c76; fila individual.
- Problema simple, unknown409: inicialmente GREEN 7414c9; fila individual.
- Problema simple, notfound409: inicialmente GREEN bb3769; fila individual.
- Problema simple, timeout404: inicialmente GREEN 865f6e; fila individual.
