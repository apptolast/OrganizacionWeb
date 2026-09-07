Pausar una sesión conserva la hora de fin prevista y acumula únicamente el tiempo trabajado. Reanudar continúa esa misma sesión; una respuesta perdida se recupera sin duplicar la transición. La web distingue el recibo histórico del estado actual.

Incluye persistencia transaccional PostgreSQL con revisión e idempotencia, API de consulta/transición, eventos durables, controles React/SCSS y recuperación manual. Mantiene una única sesión abierta por propietario, incluida la pausa.

Validación:
- 1.890 pruebas Java, 1.668 frontend y 32 del runner; lint y compilación verdes.
- CI completa sobre9fc414e:108E2E y smoke real de respuesta perdida, caída de Rabbit y reinicio del backend.
- Chromium, Firefox y WebKit;515 medidas responsive y35axe sin violaciones, texto y zoom nativo al200%.
- PIT523/525 (99,62%), dos sin cobertura y cero errores/timeouts. Stryker741/861 (86,06%),119 supervivientes y uno sin cobertura, sin errores/timeouts.
- Dos oráculos de recuperación reforzados; seis firmas detectadas en replay separado. Un error adicional del runner queda registrado y no cuenta como detección.

Feature15 cerrada conforme a progress/judge_pause_resume_final.md. Los límites de cobertura y dispositivos físicos permanecen explícitos. Cierre de sesión16, aviso17, historial18 y despliegue son los siguientes pasos del MVP. El último corte añade evidencia y configuración reproducible del replay, con producto/tests idénticos al corte de CI validado.
