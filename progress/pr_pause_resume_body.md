Pausar una sesión conserva la hora de fin prevista y acumula únicamente el tiempo trabajado. Reanudar continúa esa misma sesión; una respuesta perdida se recupera sin duplicar la transición. La web distingue el recibo histórico del estado actual.

Incluye persistencia transaccional PostgreSQL con revisión e idempotencia, API de consulta/transición, eventos durables, controles React/SCSS y recuperación manual. Mantiene una única sesión abierta por propietario, incluida la pausa.

Validación:
- 1.890 pruebas Java, 1.668 frontend y 32 del runner; lint y compilación verdes.
- CI completa sobre 9fc414e: 108 E2E y smoke real de respuesta perdida, caída de Rabbit y reinicio del backend.
- Chromium, Firefox y WebKit: 515 medidas responsive y 35 comprobaciones axe sin violaciones; texto y zoom nativo al 200%.
- PIT 523/525 (99,62%), dos sin cobertura y cero errores/timeouts. Stryker 741/861 (86,06%), 119 supervivientes y uno sin cobertura, sin errores/timeouts.
- Seis firmas detectadas en replay separado. Un error adicional del runner queda registrado y no cuenta como detección.

La CI posterior detectó un fixture sensible al orden de dos consultas. El último commit 2e492a0 asigna respuestas por ruta y comprueba el error concreto, sin cambiar producción. CI 34077907038 terminó SUCCESS sobre 2e492a0: init, compilación, 108 E2E y smoke real verdes. Descontando conservadoramente las tres detecciones de mutación atribuidas al fixture anterior, 738/861 (85,71%) conserva el umbral del 80%.

Los límites de cobertura y dispositivos físicos permanecen explícitos. Cierre de sesión, aviso de fin, historial y despliegue siguen pendientes para el MVP.

