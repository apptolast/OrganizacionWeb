# TDD frontend16 — cierre explícito

Contrato3E45F26004E2656E97443451E2D0368CBC9DE734D859725BD07896E206A44285, gate root80c94b6. Baseline15 corregido1668 verde; no se repite init global. Ponytail full/Caveman lite. Ownership frontend, sin Git/backend/metadata. Un caso introducido y ejecutado por ciclo; inicialmente verdes se declaran sin fabricar RED.

## Primer corte API

Se amplía work-session-state-api.ts, conservando SessionStart14, apiRequest y lectores existentes. Nuevo archivo close-work-session-api.test.ts separa oráculos16 del contrato15. Estado closed, unión CLOSE7 con closure4, notas exactas, POST y recuperación por key/ID de sesión; PAUSE/RESUME mantienen seis campos. No UI modificada en este corte.

| Ciclo / escenario | Oráculo | RED | GREEN |
| --- | --- | --- | --- |
|1 @s24|snapshot closed y neto final fijo|bceace|72add5 (49 con API15)|
|2 @s1|POST close running, body/key/revisión y recibo final exactos|78a437|70b1c4 (50 con API15)|
|3 @s2|close paused no suma descanso|8242ec|168844|
|4 @s30|CLOSE sin closure rechazado|795706|d3bc34 (52 con API15)|
|5 @s30|nota null no es string normalizado|cc9dc9|c14ed6|
|6 @s30|2001 puntos de código rechazados|9ec50a|9851f3|
|7 @s30|NUL decodificado rechazado|fe4063|28b29a|
|8 @s30|surrogate aislado rechazado|50ded5|2651e5; compatibilidad ES2022 0f1ed2|
|9 @s4|2000 emoji válidos preservados|inicialmente verde|62f9ad|
|10 @s30|workDate2026-02-30 rechazado|64f073|1a2136|
|11 @s30|closeZoneId no string rechazado|e65738|d1a36b|
|12 @s26|GETclosure por sessionId, sin Location y con signal/no-store|2b28b3|9b7cd6|
|13 @s35|recibo recuperado válido con notas distintas no confirma intención|892a20|acfdd5 (61 con API15)|
|14 @s28|K recupera recibo CLOSE original sin Location|inicialmente verde|b9ca5c|
|15 @s27|F503 preservado, no ausencia|inicialmente verde|bc7cf3|
|16 @s30|before paused/after closed coherentes salvo suma de descanso|inicialmente verde|ccbe6c|
|17 @s31|atribución persistida aceptada sin resolver zona/fecha cliente|inicialmente verde|df94be|
|18 @s33|F no acepta sesión distinta de la solicitada|inicialmente verde|bf9b7f|

Incidentes honestos: en ciclo2 una sustitución textual no coincidió con saltos del archivo;850356 conservó el mismo RED, lectura bce60c y patch explícito completaron el mínimo. En ciclo8 String.isWellFormed pasó Vitest pero tsc0fbc93 lo rechazó por libES2022; se usa regex Unicode de surrogates aislados sin modificar configuración. El positivo2000emoji comprueba pares válidos. No son fallos del backend ni motivos para ampliar dependencias.

Se reutiliza el guard de notas tanto en confirmación POST como recuperación por key: misma intención contractual, sin una segunda política. Los getters históricos mantienen validadores y contexto propios; no se compara atribución con Intl/TZDB. La API transporta notas strings; la normalización del formulario pendiente enviará cadenas vacías para campos vacíos.

Freeze API: formato077579,109/109 GREEN999bdf (18 nuevos+48 API15+43 API14), tsc d83816 y ESLint focal6deae6 EXIT0. No global/E2E/mutación. Queda UI/ruta/formulario/recuperación y composición; no se declara cobertura total de41/113 ni feature terminada. Oráculos de otras capas corresponden a sus autores.

## Ajuste de oráculos tras review parcial de root

Review65cbc9 detecta que la comparación sameNotes añadida en ciclo13 podía ocultar un defecto de validNote en cuatro negativos POST anteriores. Se cambió cada caso por separado a GETclosure200/readWorkSessionClosure, que valida recurso sin comparar intención: null36f72e,2001CPae5c7f,NUL006318,surrogate1505db; todos inicialmente verdes. Se conservan los RED originales y no se atribuye un nuevo fallo del producto. Producción API intacta. Regresión API14/15/16 posterior109 verde; hashes renovados en entrega.

## Inicio de lector UI

@s33 primer oráculo de recuperación por URL conocida con Sclosed y F original: RED2c2183 módulo inexistente, GREEN28d7a5. Se crea WorkSessionReader mínimo y se exporta seconds del panel15 para reutilizar formato decimal español; el oráculo usa coma decimal coherente con15. Este primer corte UI es incompleto: aún faltan errores, contexto, routing y formulario. No está conectado a App ni se entrega como producto terminado.
