# Review del checkpoint PostgreSQL y guardas de ampliación

APPROVED para este paquete intermedio;17 continúa en implementación.

Root revisó los cambios de producción y oráculos en f0e8f4/b51feb/2afc87. Los18 hashes del manifiesto coinciden. La lectura E reutiliza la transacción read-only REPEATABLE_READ y consulta antes del Clock. EXTEND bloquea la sesión propia, comprueba identidad y replay antes de negocio/reloj; conserva intervalos y estado salvo revisión, y guarda fin, marca, recibo y evento. La fórmula usa max(fin anterior, instante capturado) con microsegundos y límites existentes. P/R/C validan la misma marca antes de escribir y persisten el instante del recibo, sin añadir otra lectura del reloj ni cambiar State6.

Los oráculos PostgreSQL comprueban JSON independiente, ausencia de modificación del inicio/evento original, replay tras cierre y nueva sesión, conflicto de cantidad y marca temporal. Evidencia del autor:21 casos verdes EXIT0 540101 (8Extend,3ReadEnd,1compatibilidad JSON,9PG), recuentos7339a3, Spotless final. No se atribuye a este paquete una regresión completa de los tests heredados; ésta sigue pendiente tras los cambios compartidos.

V18 es todavía una migración aditiva nominal de dos columnas nullable. Quedan integridad/upgrade, concurrencia, colisión postrollback, atomicidad y fallos, observación de RR, wiring, HTTP y gates integrados. Se permite continuar esos ciclos sin nueva aprobación. El checkpoint no está listo para merge a main.
