# Revisión de duración y elegibilidad14

**APPROVED checkpoint parcial.** Sin bloqueantes en el cambio de `StartWorkSession` y su prueba. No aprueba temporal, fallback, replay real, PG ni HTTP.

La guarda de rango rechaza 0/1441 con ValidationException y field/code correctos antes de invocar el puerto o consultar Clock. Los positivos 1/1440 comprueban el fin exacto conservando microsegundos. El tipo int del puerto delimita correctamente esta validación; los tipos y ausencia JSON corresponden al futuro adaptador.

Proyecto completed precede a tarea completed. Ambas guardas están dentro del callback transaccional y antes del reloj, por lo que no anticipan el rechazo a una futura decisión de replay del puerto. No se afirma que ese replay ya esté implementado. Los tres fixtures completed proporcionan un reloj válido y verifican ausencia de interacciones; sus fallos no pueden depender de un Clock sin configurar. El helper sólo entrega contexto y ejecuta el callback, sin simular las decisiones de elegibilidad que se prueban.

Las excepciones existentes se reutilizan sin cambiar títulos de rutas heredadas. La bitácora distingue los incidentes de fixture/compilación de los RED funcionales y declara los casos inicialmente GREEN. El oráculo nominal previo sigue conservando identidad, evento y captura única del reloj.

Evidencia leída `17a8e2` y lectura focal de las tres pruebas completed. El autor registra regresión `e55cd8`, ocho casos GREEN después del formato. El juez no ejecutó suites ni Git y no modificó fuentes/tests. Ponytail full y Caveman lite.

SHA256 del corte:

- `StartWorkSession.java`: `20B805D5280BFE411110AAE391359FD2CF4A698C06D4C5124170D60FB04FAAA2`.
- `StartWorkSessionTest.java`: `2772D8133E45071EB5C202EE08085C22EDC7210633F7B3B63217C0CAA39219FE`.
