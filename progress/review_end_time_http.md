# Review del adaptador HTTP de ampliación

APPROVED para este paquete HTTP, sin declarar terminada17.

Root revisó el controlador y sus27 oráculos nuevos en4169eb/a33a52. Los cuatro hashes coinciden y los XML preservados acreditan100 casos sin fallos, errores ni omitidos:27 nuevos,48 pausa/reanudación y25 cierre (c5bf79). El autor obtuvo EXIT0 3b771b después de incorporar el checkpoint PG0d48abb. Los únicos cambios en los dos tests históricos son mocks de los puertos nuevos; sus aserciones permanecen.

E devuelve exactamente estado, reloj y fin, con revisión y sin ETag/Location. P mantiene precedencias de seguridad/query/cabeceras/JSON y rechaza cantidad ausente, tipo incorrecto, fracción, rango y overflow antes de invocar puertos. EXTEND conserva representación discriminada de siete campos, replay200/201 y Location; C/K recuperan el recibo sin lectura vigente auxiliar. Errores no filtran SQL ni fabrican confirmación. Las relaciones de negocio y atomicidad corresponden a los puertos y requieren integración PostgreSQL, no se atribuyen a mocks.

Pendientes: integración con wiring real, pruebas completas compartidas, navegador/UX, smoke y mutación. La propuesta de scopes es un documento separado y no una campaña ejecutada.
