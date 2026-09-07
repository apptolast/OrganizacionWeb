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

## UI ciclos2–8 (corte en progreso)

- @s33 contexto de otra tarea: REDa5028b (además rechazo no manejado al intentarF con fixtureequivocado), GREENa81edc. sameIdp/t se comprueba antes deF.
- @s27 F503/reintento: RED99692f (sin catch), GREEN5fcab1.
- @s33 App reconoce ruta estable alrecargar: RED6800eb, GREEN77a988 (4lector+15App).
- @s32 formulario/cierreexplícito: RED87fd31, GREEN02b6b9.
- @s40 pendinganunciado/focobusy/sinPOSTduplicado: RED70a55a, GREEN41bad8.
- @s5 borrador2001emoji no se transmite ni recorta: REDcb11e1, GREEN0ca606. Export mínimo validNote de API para usar mismo criterio; cuerpo del helper intacto, sin maxlength UTF16. El error es local/asociado a campos.
- @s35 POST503/K recupera intenciónexactasinotroPOST: RED25d18f, GREENb9d14e (8lector). Retainedkey/notas se mantienen hasta confirmación.

ProducciónUI aún en construcción, nofreeze: pendientes contexto alrecibirF, tiempos, foco/feedback completo,401/aborto,412/CSRF/reenvíomanual, active separado, enlace antesPOST yretornoauth. Nuevosrechazos previos sincatch fueron RED observables y no se atribuyen a backend. No se ejecutaron suitesglobales ni campañas.

## UI ciclos9–18 (en progreso)

9. @s35 K404 reconocido habilita sólo reenvío manual idéntico: RED079023→GREEN63e3c4.
10. @s36 POST412 conserva notas, consulta estado y exige otra decisión con key/revisión nuevas: REDcbf922→GREEN2a70b1.
11. @s37 renovación CSRF externa y reenvío manual conservan intención: RED7d4466→GREEN398991. Este foco usa renovación del token; composición SessionGate ya tiene recorrido14/15, no se afirma navegador16.
12. @s38 desmontaje aborta POST: RED84daa4→GREEN950954.
13. @s38 cambio de recurso retira borrador/aborta y descarta respuesta tardía: REDed67e9→GREENe49b01.
14. @s40 reintento de lectura pendiente anuncia y recibe foco: RED74677b. Verificación14/14 GREEN41f110; salida anterior excedió contexto, por eso se confirmó el foco.
15. @s32 entrada navega a URL estable sin comando: REDbfcefb→GREEN088d93 (35 panel).
16. @s24 estado closed no ofrece reanudar/cerrar de nuevo y enlaza recibo: REDbef2f0→GREEN72a174 (36 panel).
17. @s33 autenticación conserva ruta sesión: RED3b25eb→GREENb60b6b (74 autenticación). Sólo ampliación anidada de ruta privada.
18. @s40 consulta K anuncia «Comprobando cierre»: RED7c52ba→GREEN96677b (15 lector). Copy de notas ajustado a español corriente solicitado por review; ec76cd encontró expectativa literal antigua, actualizada sin alterar oráculo de rechazo/no POST/borrador.

No freeze de producto todavía. Faltan presentación final/contexto y actividad independiente, además de cierres de privacidad/esperas; no se atribuyen a estos focos pruebas de navegador ni toda la matriz contractual.

## UI ciclos19–32 y cierre del corte de implementación

19. @s32 recibo con notas vacías explícitas, fecha/zona y tarea independiente: RED56e706→GREEN264e25.
20. @s34 POST confirmado seguido de A503 conserva recibo y reintenta sóloA: RED4d7069→GREENc231e9. En el fixture nuevo se corrigió antes de GREEN el envoltorio a session:null (contrato14), no active:null.
21. @s34 otra sesión propia se enlaza separadamente: REDd82e8c→GREENfdc06d.
22. @s33 F debe coincidir con contexto de ruta, además del sessionId validado por API: RED681434→GREENd16526.
23. @s32 inicio/fin previsto y aviso salir no revoca: RED9192be→GREEN29f196.
24. @s36 rechazo de estado definitivo consultaF sin enviar borrador: RED15c566→GREEN29e4c9.
25. @s14 límite de revisión definitivo: REDecfdca→GREENa099c9. Se reutiliza rejectionMessage15, sin duplicar catálogo ni lógica temporal.
26. @s35 K503 desconocido no habilita reenvío: inicialmente GREEN162df0.
27. @s38 HTTP401 tardío de ruta retirada no notifica observer: inicialmente GREEN3fe41d.
28. @s40 blur deliberado antes de respuesta: RED432d2a→GREENf2a5bb. La restauración se cancela al salir del foco y se consume al aplicarse, no permanece activada indefinidamente.
29. @s32 aviso previo de notas inmutables y estado de tarea: RED3b3f4d→GREEN3dfd74.
30. @s38 JSONF tardío tras navegación: inicialmente GREENad2a56.
31. @s38 clasificación de error tardía tras navegación: inicialmente GREENa4af5d.
32. @s40 respuesta retira iniciador todavía enfocado y pasa al encabezado: inicialmente GREEN540dbf.

Refactor de presentación en GREEN: helper SnapshotTime15 exportado sin cambiar su cuerpo (incluye fallbackUTC explícito); notas con clase closure-note, white-space:pre-wrap y overflow-wrap:anywhere, sin interpretar HTML. SCSS propio existente, controles nativos con .field/.task-form. No maxlength HTML ni truncadoUTF16. Geometría, motores, zoom, feedback400ms y treinta principios quedan para navegador real; JSDOM no los certifica.

Validación del corte: 306/306 en8 archivos GREEN7d6cdd. El comando también mencionó task-reader.test.tsx, que no existe; Vitest ejecutó los8 archivos reales de API14/15/16, panel15, lector16, UI14, autenticación y App. No se atribuye una novena suite. FormatoGREEN7d6cdd; tsc, ESLint focal y Vitebuild EXIT0 1a8684. No global/backend/E2E/mutación. API16 conserva18casos; lector29; panel36; auth74; resto reutilizado. Los47 nuevos casos API/lector no equivalen a113ejemplos Gherkin.

## Mapa de evidencia y límites para revisión

- @s1–5,8–11,24,27,30–31: API exacta/µs/closure/notas y atribución persistida en18casos más validadores15 heredados. No se demuestra persistencia backend desde estos clientes.
- @s12–23,25–29: responsabilidad dominio/PG/HTTP/publicador; este corte consume el contrato, no sustituye sus pruebas.
- @s32–33: URL antesPOST, retorno traslogin, lectura S/F/contexto, notas opcionales y confirmación explícita.
- @s34: A separado sólo tras confirmación local; A503 no retira historial ni prueba ausencia, otra propia enlazada. Volver a tarea permite flujo14 con elegibilidad vigente; no se crea sesión automáticamente. Reload del recibo conocido no depende deA.
- @s35–37: key retenida, recuperación/404 manual, 412 con borrador y nueva decisión, CSRF por infraestructura global existente. No se hizo nueva prueba navegador de SessionGate16.
- @s38: abort desmontaje/ruta, HTTP401, JSON y clasificación tardíos cubiertos individualmente.
- @s39: Reader no lanza S concurrente con un cierre (formulario sólo después deS; retry/412 retiran formulario durante nueva consulta), y A monta después de confirmar, con reintento bloqueado mientras espera. Lecturas retiradas se abortan por contexto. El caso compuesto desde tarea14 con GETA anterior pendiente requiere confirmación de navegador/integración; no se afirma que estos29casos reproduzcan ese escenario literal.
- @s40: foco/guardas/anuncios semánticos y blur deliberado cubiertos;400ms físico pendiente navegador.
- @s41: SCSS/marcado preparados, evidencia UX real pendiente. No feature17/18.

Se entrega implementación para revisión independiente, no aprobación global de feature16. Producción frontend congelada al comunicar hashes; sólo documentación puede ampliarse mientras review.
