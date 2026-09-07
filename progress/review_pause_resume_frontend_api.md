# Revisión parcial: cliente de pausa y reanudación

Root aprueba el corte de work-session-state-api.ts y su test, con los dos exports mínimos de work-session-api.ts. Lecturas independientes 6c1058, 4ea595 y 2b9655: contrato cerrado, coherencia de identidad/revisión/Location, instantes y sumas con BigInt, recibos inmutables, recuperación manual por clave y errores HTTP conservados. No se introduce polling ni reenvío automático. Los helpers existentes se reutilizan sin cambiar sus cuerpos.

Los tres hashes verificados por root2b9655 coinciden con71df39: cliente38743D9B82869708BD53C98850DF8C71FAEFB7CD0BE5348519679D8E32AB2FAA; test7AC5B1B3CDC1DE502CF6CB801566B4A4B54C8315777CE0A884C29B1A5FF31090; API14 con exports8B37B3C856FC208EB8A873A676D757E3C8945A60470C50A2739A7602540520C3.

El autor acredita 47 pruebas nuevas y 43 de regresión mediante salida c06d50, 90/90 verdes; formato, lint y tipos18ef8a sin errores. No se preservó un log separado y root no presenta esa ejecución como propia. La bitácora registra los ciclos y distingue controles inicialmente verdes e incidente de importación.

No hay hallazgo accionable en este corte. Falta revisar el panel, integración con SessionGate, respuestas tardías y recuperación, responsive/UX, recorrido real y mutación. Esta aprobación parcial no declara 15 terminada.
