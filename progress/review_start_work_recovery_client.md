# Revisión del cliente de recuperación14

**APPROVED parcial para GET por ID/key.** Sin defectos concretos encontrados en el corte. Se conserva la aprobación previa de POST/activa; no se aprueba todavía UI, recuperación durable real ni la feature completa.

Lecturas completas de las funciones añadidas, diez tests nuevos y append de bitácora: `1a184a`/`15fea7`. Se comparó con el contenido del cliente previo ya revisado, sin usar Git. El autor registra 27/27 GREEN `23584f`, formato `e28238`, ESLint `c072b1` y tipos `72b938`; el juez no ejecutó suites.

`readWorkSession` exige HTTP200, valida SessionStart completo y compara su id con el solicitado. `recoverWorkSession` usa la ruta literal by-request, valida el mismo DTO y compara proyecto, tarea y plannedMinutes con la intención retenida. Son parámetros escalares, sin objeto mutable de intención que pueda cambiar durante await. No exige el id todavía desconocido al recuperar por key ni intenta extraer una key del DTO que contractualmente no la contiene.

Ambas lecturas conservan señal, credentials y no-store, y no exigen Location de POST. El test nominal de cada una devuelve realmente una respuesta sin ese header. Los errores 404/503 se propagan como la misma Response antes de leer el cuerpo; no se convierten en ausencia ni en un error de forma. El caso de duración diferente mantiene un DTO temporalmente coherente, por lo que comprueba la correspondencia con la intención y no un fallo incidental del validador.

La validación temporal exacta y las otras funciones conservan su lógica previa. No se añade catálogo ni comparación con el reloj actual. Los dos casos inicialmente GREEN están identificados honestamente; no se fabrica RED. La UI aún debe distinguir 404 de rollback confirmado, decidir reenvío manual y aplicar las guardas de contexto tras await. Esas responsabilidades no se atribuyen a esta prueba de transporte.

Hashes coincidentes con freeze `08f499`:

- `work-session-api.ts`: `84A8F87112F2297986BA0A694F134FDF4E6EB65BA9C2EDB4828B08D61678CCB4`.
- `work-session-api.test.ts`: `1CE83520286C3956F98F5FD5B3D02560FCC08F21EB0BE42D16A0F3A008A2B2C7`.

Sin cambios de código/tests, suites, Git ni ampliación de combinaciones. Ponytail full y Caveman lite.
