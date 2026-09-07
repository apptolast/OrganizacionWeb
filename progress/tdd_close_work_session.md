# Índice de evidencia — cierre de sesión16

Contrato: [close_work_session.feature](../features/close_work_session.feature),41 escenarios y113 ejemplos expandidos. Este índice enlaza evidencia ejecutada; **no afirma113 tests ejecutados ni suma suites solapadas**. Corte de producto integrado hasta11a8ea9, con revisión contractual y paquetes parciales aprobados. Mutación, globales y CI siguen pendientes de sus resultados finales coordinados por root/A/C; no se deduce su éxito de estos focos.

## Fuentes de evidencia

- **N — Núcleo/PG:** [bitácora backend](tdd_close_work_backend.md),115 pruebas en8 suites, f5ba7c; [revisión independiente](review_close_work_backend_independent.md). Incluye reutilización15 declarada y XML preservado.
- **H — HTTP:** [bitácora HTTP](tdd_close_work_http.md),122 pruebas (25 nuevas+48 API15+49 API14),5744e9. MockMvc con puertos mock: no demuestra por sí solo persistencia o propiedad real.
- **I — Composición:** [HTTP+PostgreSQL real](tdd_close_work_integration.md),1 caso final c36faf, sin mocks de aplicación/persistencia.
- **P — Publicador:** [bitácora](tdd_close_work_publisher.md),207 pruebas de publicador/Rabbit,8495cb; [revisión](review_close_work_publisher.md).
- **S — Smoke:** [recuperación/reinicio/Rabbit](tdd_close_work_smoke.md),EXIT0 b2d649 y323 hashes idénticos al terminar aquel corte, anterior al ajusteSCSS. No equivale a entrega exactamente una vez.
- **F — Cliente/UI:** [bitácora frontend](tdd_close_work_frontend.md),306 pruebas en8 archivos relacionados,7d6cdd (18 API16,29 lector16 y regresiones compartidas); [revisión independiente](review_close_work_frontend_independent.md).
- **E — E2E funcional:** [cuatro casos](tdd_close_work_e2e.md),running d73a29,paused c8f915,ACK perdido log/lectura8cc115 y GETA anterior3d103a. El informe conserva el límite de metadatos de proceso del tercer caso.
- **U — UX:** [bitácora](tdd_close_work_ux.md) y [matriz completa de30 principios](ux_close_work_session.md),3 casos/7 ejecuciones pertinentes,515 medidas y35 axe sin violaciones. Rutas reales copiadas por root a `.e2e-work/close-work-ux-reviewed-20260907`; nativo conserva `.e2e-work/close-work-native/organizationweb-e2e-23576/evidence`.

## Mapa de los41 escenarios

| Escenario | Evidencia ejecutada y reutilización/límite |
| --- | --- |
| @s1 | N: CloseWorkSessionTest running y Store close con intervalo/recibo/evento independientes. I: POST/PG60000001µs. E: cierre running y contraste SQL exacto. F: POST/recibo válido. |
| @s2 | N: close paused y upgrade que cierra pausa sin intervalo adicional. F: recibo paused sin sumar descanso. E: paused conserva acumulado e intervalo. |
| @s3 | N: evidencia compuesta running/paused16 y casos15 pause/resume en mismo microsegundo; no CLOSEcero dedicado. Reutilización aceptada expresamente por juez independiente, no fila nueva fingida. P añade payload paused/cero válido. |
| @s4 | N: null,2000emoji, espacios/saltos. H: ausencia/null y Unicode2000 en constructor real. F:2000puntos suplementarios. E/U: notas literales con espacios y multilínea. |
| @s5 | N: límites por campo, NUL y surrogates. H: tipo numérico, exceso y caracteres inválidos antes de aplicación. F: negativos del recurso aislados víaGETF y borradorUI2001emoji sin POST. |
| @s6 | H: query antes de UUID/cuerpo, revisión requerida/sintaxis, JSON concatenado/campo desconocido, anonimato y CSRF. Resto seguridad y problemas reutiliza API14/15 en la misma regresión, no matriz16 duplicada. |
| @s7 | N: reloj anterior/año10000 y guarda requireTime15; año0 rechazado por ser anterior al changedAt válido. H conserva mapping temporal. |
| @s8 | N: rangoUTC completo315537897599999999µs. F: BigInt y validadores exactos14/15 reutilizados; P acepta máximo contractual sin nanosegundosLong. |
| @s9 | N: díaMadrid distinto delUTC y fallback sólo si ZoneId histórico no resuelve. I: cierre atribuido a día siguiente en Europe/Madrid. |
| @s10 | N: desbordamiento local superior y año local0 con UTC válido; no se transforma ese error en fallback. |
| @s11 | N: GETF devuelve recibo durable completo sin outbox y sin catálogo/reloj. F acepta atribución persistida sin Intl. No se simula instalar otra versiónTZDB. |
| @s12 | N: propiedad/token/replay del mismo commit15 más CLOSE/replay/notas16. H: precedencia de entrada y token completo412. No se equiparan mocksHTTP con propiedad real. |
| @s13 | N: revisión obsoleta en closed y rechazo de CLOSE nuevo; compatibilidad exacta de WorkSessionState compartida rechaza PAUSE/RESUMEclosed. F: estado closed no presenta reanudar ni otro cierre. |
| @s14 | N: MAXantes del reloj. F: rechazo definitivo de límite sin incertidumbre. P: revisión máxima válida del evento. |
| @s15 | N: replayCLOSE normalizado tras B y replays inicio/PAUSE/RESUME trasCLOSE/B, hechos y conteos idénticos. S: reinicio conserva C/K/F e inicio histórico. |
| @s16 | N: cada nota distinta; acción/revisión/sesión reutilizan requireIntent15. H: conflicto de intención cerrado. F: K con otras notas rechazado. |
| @s17 | N: guard de identidad compartido y colisión Aclosed/Búnica abierta prueban que key ajena no se consume; B permanece intacta. |
| @s18 | N: cuatro carreras CLOSE misma key, keys distintas, PAUSE y RESUME, ambos workers observados esperandoLock antes de liberar. No preferencia de scheduler impuesta. |
| @s19 | N: inicio concurrente con cierre sincommit y variante rollback; nuevo inicio sólo trascommit. E: nueva sesión real después del cierrepaused. |
| @s20 | N: cierre no espera locks ajenos/proyecto/tarea. Elegibilidad completed usa frontera común15 y lectura de ruta sin elegibilidad; no CLOSEcompleted dedicado, límite aceptado por juez. |
| @s21 | N: supresión de estado/intervalo/recibo/outbox, SQL tardío y fallo diferido decommit conservan filas/plaza. Son conexionesCLOSE reales al mecanismo15, inicialmente verdes. |
| @s22 | N: UNIQUE real con omisión inicial controlada del lookup y dos txid trasrollback,409 de intención ajena. No carrera natural de dos sesiones abiertas. Replay idéntico acredita vía normal/misma key; rama tardía idéntica no forzada con estado imposible. |
| @s23 | N: V17 aditiva, índice parcial de CLOSE único y migración real16→17 preservando filasrunning/paused/recibos15. No reescrituraV16. |
| @s24 | N: neto fijo closed con reloj anterior y rango de lectura compartido. H: State6/snapshot3/header. F: snapshot cerrado. S: estado terminal después de reinicio. |
| @s25 | N: snapshotrunning con writerCLOSE confirmado antes de leer reloj; lectura posteriorclosed. Dos consultasF en RR tampoco mezclan propiedad/recibo. |
| @s26 | N: F durable, propietario/abierta, forwarding y sinreloj; H: GETF cerrado sinLocation y404 correspondientes. I/E: F exacto después de cierre/recarga; S: F tras reinicio. |
| @s27 | N: SQL y cierre de transacción503, read-only/RR real. H:503 sin informaciónSQL. F/U: lectura fallida no significa ausencia ni sesión abierta; reintento disponible. |
| @s28 | S: upstream201 antes de perderACK, C/K/F y replay tras reinicio y retirada de outbox publicado. E: ACK perdido y recarga por URL sinkey. H/F sólo acreditan formato y lectura, no durabilidad por sí solos. |
| @s29 | P: evento11 cerrado, datos incompatibles bloqueados, ruta/quorum reales, retry y redelivery. S: Rabbit detenido/recuperado publica mismoeventId/payload sinnotas; HTTP no depende del broker. |
| @s30 | F: closure ausente, notas inválidas medianteGETF aislado, fecha imposible, zona noString y aritmética paused incoherente; precisión/revisión/SessionStart validadores15/14 reutilizados. P valida evento, que es un contrato distinto. |
| @s31 | F: atribución persistida admitida sin resolver zona; SnapshotTime15 compartido conserva fallbackUTC rotulado probado. No recalcular workDate conTZDB cliente. |
| @s32 | F: enlace previoPOST, formulario/avisos/contexto, notas y confirmación explícita. E: URL estable antes de escribir y tarea siguepending. U: formulario/espera/notas largas en tresmotores. |
| @s33 | F: App/ruta, retorno traslogin, S/F y contexto p/t/s. E: recarga nominal y después deACK perdido recupera notas sinPOST adicional. |
| @s34 | F: A503 preserva recibo y no prueba ausencia; otra propia enlazada. Epaused: nueva activa real en otra pestaña sigue separada del cierre yF original. |
| @s35 | F: key/notas retenidas, K sinPOST,404 reconocido permite reenvío manual idéntico, K503 desconocido no lo habilita; validadores heredados distinguen respuestas incompatibles. E/U: POSTreal conACKperdido o503controlado y recuperación. |
| @s36 | F:412 conserva texto, consulta deliberada y nuevakey/revisión sólo al confirmar; closed/conflicto recupera F sin enviar borrador. No nuevoE2E412 en este paquete. |
| @s37 | F: renovaciónexternaCSRF y reenvío manual conservan cuerpo/key/revisión; SessionGate14/15 reutilizado. No se presenta el setter de token del foco como interacción navegador de Recuperar acceso16. |
| @s38 | F: aborto desmontaje/ruta, HTTP401 obsoleto antes del observer, JSONF y clasificación de error tardíos; no restauran datos. No nueva matriz combinatoria por cada endpoint. |
| @s39 | E:3d103a GREEN, GETA real del padre retenido durante navegación/cierre; respuesta liberada no restaura Pausar ni sustituye cierre/ausenciaactual. Sustituye el pendiente anterior de la bitácoraF. La lectura de estado no tiene solapamiento alcanzable dentro del lector: formulario esperaS y la navegación aborta lecturas retiradas. No se afirma que la respuesta abortada llegue al componente desmontado. |
| @s40 | F: doble envío, consulta vsescritura, espera/reintento y foco sin robo trasblur. U: Enter, iniciador/heading y feedback medido3,4/3/6msR,2/3/5msT,2,2msN, antes de liberar respuesta. No promesa de tiempo de red. |
| @s41 | U exclusivamente:30 principios,515 medidas/35 axe sin violaciones,31 anchos×3motores, texto200 y zoom nativo200. Enlace21px fue RED real e85063 y mínimoSCSS44px quedóGREENded1ca. Límites físicos/humanos explícitos. |

## Correcciones de etiquetas y estado de gates

La consola del smoke medido imprimió **@s29/@s41 por error**: su recuperación corresponde a **@s28** y su publicación a **@s29**. No se reescribe aquel script/log ni se altera su hash para corregir historia. La evidenciaUX@s41 pertenece únicamente al paquete U de B. Asimismo, las alusiones del mapaN a s28 como seguridadHTTP o s30 como publicación no cambian el significado del contrato final: este índice prioriza el escenario real, no una etiqueta copiada de otra feature.

El pendiente@s39 del primer freeze F queda cerrado por el cuarto E2E de C; no es una nueva ejecución nuestra. Errores de fixture/formato y pruebas inicialmente verdes permanecen descritos en cada bitácora, sin fabricar ciclos RED.

**Pendiente de resultado coordinado:** PIT16, Stryker16, global E2E iniciado por root 6843, consolidación de init/globales y CI. Este índice no los certifica ni cambia feature_list. Las revisiones parciales y focos anteriores no sustituyen esos gates. No se ejecutaron suites ni se modificaron fuentes/tests para redactarlo.


### Resultado posterior de integración y refuerzos

Root confirma E2E115/115 EXIT0 a958e5 y CI34082838516 SUCCESS sobre910f405 (a09918), con init/build/E2E/publicador. PIT original516/520 y Stryker1104/1275 superan80; denominador estricto frontend1277 y sus dos RuntimeError se conservan en los dictámenes. No se deduce cobertura contractual de esos porcentajes.

8ecf0f8 añade tres oráculos Reader para@s5/@s38/@s40 (error diferenciado y descripción accesible,401 retirado, consulta activa pendiente sin duplicado),32Reader verdes; y un oráculo publisher@s29 con workDate0001,192publisher verdes. Producción intacta. Sus ciclos inicialmenteGREEN y la corrección de sincronización del fixture están en tdd_close_work_frontend.md y tdd_close_publisher_year_one.md. Los replays dirigidos e init/build finales siguen pendientes de resultado; no alteran las campañas originales ni los41 escenarios.

## Cierre final autorizado

Root confirma todos los gates y autoriza status16=done, conservando los41 escenarios de aceptación y sin afirmar113 tests ejecutados. [Dictamen final APPROVED](judge_close_work_session_final.md): init17230 EXIT0 b68a48, backend1985/83 sin fallos/errores/omitidas (XML51651c), frontend1721/35 y36 Node verdes; build38374 EXIT0 3d8828. Logs finales y hashes verificados16cb18: init723EA9B3F025F587D91632D14616B786A31CD612839791DF32E619B0D52E15E4, buildAB790B52324C82D429C88E5BC8A7F2E60B4EDEF167C3E9BE394B9CF0DC2A6838.

PIT original516/520=99,23 %, Stryker original1104Killed/169Survived/2NoCoverage/2RuntimeError=86,59 % de herramienta; los originales permanecen intactos. Replay de frontera publicador7/7Killed EXIT0 e01e1a y322 hashes iguales; replay frontend final15/15Killed EXIT0 002701, doce firmas objetivo más tres extras y91 hashes iguales. Sus100 % dirigidos no sustituyen los scores originales ni reclasifican residuos. Global115 E2E, smoke, UX515 medidas/35 axe y CI34082838516 SUCCESS acreditados en el dictamen. Los pendientes anteriores de este índice corresponden a cortes históricos y quedan resueltos con esta evidencia.

Sólo se actualiza feature_list.json para cerrar16; aceptación y demás features se verificaron idénticas108bda. No cambios de producto/tests ni implementación17/18 en este cierre; current/history pertenecen a root.
