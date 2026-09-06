# Revisión final — dividir tareas

**Estado: APPROVED. Autoriza al autor cerrar split_task.**

La creación de subtareas, consulta de padre e hijos y navegación entre niveles cumplen el contrato revisado. La implementación reutiliza DTO8, validación, transporte y editor; conserva la colección plana y los eventos históricos. La migración limita cada relación al mismo proyecto y la transacción confirma relación, contenido y evento juntos. No hay movimientos, agregación de estimaciones ni cambios automáticos de estado. Ponytail full y Caveman lite se aplicaron sin retirar requisitos explícitos.

## Evidencia verificada

- Contrato aprobado: 38 escenarios; trazabilidad completa backend, cliente e interfaz en sus informes TDD.
- Inicio completo independiente del coordinador, sesión 9396, salida 0: **622 pruebas backend y 462 frontend**, sin fallos ni omisiones; lint global verde. Compilación frontend y compilación de imágenes para integración verificadas por sus autores.
- Revisión backend independiente APPROVED. XML PIT inspeccionado también por el coordinador: **235/236 (99,58 %)**, sin timeouts ni falta de cobertura. Único superviviente equivalente: null frente a cadena vacía en el helper privado, normalizados igual en sus dos usos actuales. No se necesitó replay.
- E2E sobre la imagen final DDEzlNbV: **37/38**, incluidos los cinco nuevos. El único fallo histórico dependía de un bucle fijo de Tab antes de estabilizar el foco; se corrigió sólo la prueba, conservando teclado real y foco visible. **Replay separado 1/1**, 17,5 segundos. No se afirma una ejecución agregada 38/38.
- Firefox/WebKit: **2/2**, recorrido real de jerarquía, navegación y recarga. No se extrapola a esos motores toda la matriz Chromium.
- Smoke del publicador, sesión 32635, salida 0: cinco rutas conservadas, confirmación HTTP con broker detenido, recuperación del evento original y retención tras reiniciar RabbitMQ con backend detenido. Recursos nuevos del fixture limpiados.
- UX: 22 anchos, controles y enlaces de un carácter medidos al menos 44 × 44, teclado y axe. Zoom nativo al 200 %, ancho interior 320, sin desbordamiento y creación real. Feedback observado: **1 ms** antes de liberar POST retenido. El coordinador inspeccionó las capturas móvil, escritorio y zoom, además del JSON de medidas.

## Hallazgos resueltos antes del corte

La recuperación de conflictos del proyecto conserva el borrador; la ruta con UUID en mayúsculas usa el contexto canónico confirmado; reintentar tras un 404 no vuelve a mostrar datos anteriores durante la espera. Relación y proyecto recuperan foco sin robar el destino elegido por el usuario. Las respuestas antiguas no restauran contenido tras navegar o cerrar sesión. Se revisaron fuente final y pruebas correspondientes.

## Mutación frontend y cierre

Campaña original inspeccionada: **558/601 (92,85 %)**, 41 supervivientes y dos NoCoverage, sin timeout ni error. Tras reforzar sólo pruebas, el replay separado da **56/58 (96,55 %)**. El coordinador emparejó archivo, posición, mutador y reemplazo: las 24 identidades originales objetivo quedan Killed, incluidos ambos NoCoverage y los errores controlados API 321/349. No se suman puntuaciones entre campañas.

Las 19 identidades restantes se justifican en las revisiones independientes: 12 equivalencias limitadas a los consumidores actuales y siete variantes permitidas de foco/espaciado. Se aceptan como variantes, no se presentan como equivalencias estrictas. Los dos supervivientes del replay son 205 (foco permitido) y 187 (contador de revisión equivalente). Ningún hueco contractual queda abierto. La prueba de cancelación 268 usa SessionGate y comprueba que el 401 tardío de otra ruta no elimina el borrador vigente.

Suite frontend final repetida por el coordinador, salida 0: **475/475**, 14 archivos, 6,15 segundos. Producción no cambió desde las imágenes y la verificación global anteriores; no se repitieron backend ni E2E sin causa. Lint final verde informado por el autor.

## Pendientes y límites

La CI del commit de cierre todavía debe ejecutarse; no se atribuye al corte 8 la CI exitosa de create_task/db4d20b. El servicio no está desplegado en el servidor y el MVP completo sigue pendiente de otras funcionalidades. La preparación de completar/reabrir tareas es sólo documental; feature 9 permanece pending al aprobar este corte.

La matriz de treinta principios está revisada en `ux_split_task.md`, con planificación, sesiones y progreso medido expresamente pendientes donde corresponde. No se probaron dispositivos físicos, teclado virtual ni lector de pantalla real; axe y emulación no certifican cumplimiento universal.

La revisión automática rechazó eliminar `.e2e-work/read-review-state.json` y `.e2e-work/read-review-stop` con «blocked by policy». Ambos permanecen ignorados, sin exponer su contenido ni eludir el bloqueo. Esta limitación heredada no se presenta como limpieza completa de temporales.
