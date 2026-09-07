# TDD pausa y reanudación — núcleo y PostgreSQL

Autorización: contrato a5c556f, estado in_progress 5367b35. Baseline anterior vigente; no init repetido en fase documental. Ownership A+B: dominio/aplicación y persistencia; HTTP/publicador se coordinan con otro autor. No implementación de cierre 16.

## Ciclo 1 — @s2 pausa nominal

Único test ChangeWorkSessionTest.s2_pauseRecordsExactIntervalReceiptAndEvent: RED de compilación 709c15 por tipos/puerto ausentes; mínimo núcleo y contratos GREEN cf51b2, 1 test. Comprueba 1000001 µs exactos, reloj truncado y capturado una vez, recibo antes/después e identidad/evento de doce campos con valores esperados independientes. ChronoUnit.MICROS opera directamente en microsegundos, sin conversión a nanosegundos ni redondeo por minuto.

Corte compilable parcial: ChangeWorkSessionUseCase.pause(owner, session, key, expected) y WorkSessionChanging.commit(owner, session, key, action, expected, callback). WorkSessionState en domain usa long para revisión/acumulado y SessionStart original; DTO HTTP deberá serializar ambos long como texto. WorkSessionTransitionReceipt evita colisión con WorkSessionChange de 14; WorkSessionStateChanged ya usa cadenas para los campos del evento. Pendientes resume, guardas, lectura, PostgreSQL y HTTP; nominal no acredita feature completa.

## Ciclos 2 y 3 — reanudación y token completo

@s3 único nominal resume: RED279ee7 por método ausente; GREEN3c1309, 2 casos tras generalización mínima del comando. La pausa no se suma y el fin original permanece en SessionStart.

@s12 identidad del token: observación root detectó que long aislado perdía el UUID antes de la comprobación transaccional de propiedad. Único test de forwarding REDd0aba4; mínimo WorkSessionRevision(UUID sessionId,long value) en caso de uso y puerto, GREEN4d8d07, 3 casos. Firma pause/resume(owner,session,key,WorkSessionRevision). No GET adicional en controller ni validación de propiedad simulada en núcleo; Store deberá comprobar propiedad, identidad y replay en ese orden. Primer nominal todavía no incluye esas guardas.

Ventana Java/Gradle cedida al publicador tras este GREEN; tipos/puertos compilables notificados al coordinador y autor C. No freeze de feature completa.
