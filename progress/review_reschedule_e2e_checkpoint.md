# Revisión del primer E2E de replanificación

Checkpoint de prueba todavía RED; no aprobado como entrega funcional.

Root revisó el recorrido y la composición real de TaskBlocks en 728a90/d38fc3. El test crea por UI, mueve y cancela por endpoints reales; conserva identidad y creación, consulta recibos e historial y comprueba filas/eventos sin confundir cancelación con trabajo realizado. Los ETag de POST no forman parte del contrato: retirarlos del test antes de alcanzarlos corrigió un oráculo inventado, sin debilitar revision del recibo ni Location.

Revisión de extracción 5fe41e/7436dc: configure y openEditor mantienen lógica, defaults y controles; no quedan dependencias de la constante days retirada fuera del helper. Regresión del recorrido11 551ece verde según bitácora. Baseline dedicado f4721d verde antes de la nueva prueba.

Evidencia alcanzada del nuevo test: b2b65e devuelve 500 en preview en el checkpoint backend publicado; no se alcanzaron las aserciones posteriores. La implementación core se desarrolla en otro árbol. Conservar este fallo hasta integrar y comprobar el flujo completo; ninguna afirmación de GREEN parcial sustituye ese resultado.

La matriz de 30 criterios UX se ha preparado como plan, con todas sus verificaciones para13 pendientes. No acredita automáticamente resultados de Hoy ni de otras funcionalidades.

## Revisión del primer GREEN nominal

**APPROVED para el recorrido nominal**, sin cierre global. Snapshot179 Java coordinado y verificado `2bd776`, posteriormente guardado sólo como WIP aislado35d5c92. Root leyó test, bitácora y log `cbb8d3`, y confirmó hashes de test/log `ba1653`: `0286BD…649E62` y `590954…A29FC15`. El log muestra1/1 PASS y retirada del stack; autor registra EXIT0 `993392`, recorrido5,7s/total8,8s. No se modifica el test para lograr este GREEN.

Se alcanzan las aserciones antes pendientes: identidad y DTO original, destino de movimiento y recibo revisión2, cancelación revisión3, retirada del listado, estado cancelado y foco lógico, historial de dos hechos sin GET por fila, consulta deliberada de vigencia, creación histórica intacta y conteos SQL de reservas/recibos/eventos. La tarea permanece pending: cancelar planificación no equivale a completar trabajo.

La correspondencia cerrada de respuestas, atomicidad y carreras tienen además pruebas específicas; este único E2E no las sustituye ni acredita todos los fallos. ACK perdido/reinicio13, otros motores, geometría/zoom y matrizUX30 siguen pendientes. Se autoriza el siguiente caso único s25 con backend reiniciado realmente y PostgreSQL conservado, reutilizando el mecanismo existente11 con regresión de su extracción.

## Revisión s25 — APPROVED en alcance recuperación real

Root revisó el nuevo test, helper y diff11 en `4cfc7d`, y verificó hashes/prueba material en `0c2a85`. Nuevo test `ADFB1636…BDD091F`, helper `A83F5CAA…B76AFACF`, legacy11 `880C1C3A…A19F6082`; nominal13 conserva `0286BD…649E62`. El helper traslada sin cambiar comportamiento el reinicio de11, mantiene guardia de stack aislado, comprobaciones Docker y reautenticación existente. La regresión heredada del autor fue2/2 GREENf778ae; el filtro textual seleccionó ambos casos lost, no sólo uno.

El caso13 inicialmente GREENc6e234 obtiene201 real mediante route.fetch antes de abortar la respuesta. La prueba material del stack64000 muestra StartedAt de backend20:03:02.168995431Z→20:03:13.170213293Z, con PostgreSQL20:02:58.436950851Z y montajes conservados. Luego una cancelación real confirma revisión3, y los dos BlockChanged se retiran sólo del outbox efímero para modelar retención. GET por key e ID recuperan íntegro el recibo original revisión2, sin alterar los snapshots SQL de recibos/proyecciones/eventos y sin otro POST de movimiento. UI distingue hecho histórico del estado actual cancelado.

La sesión permaneció autenticada: no se atribuye ejecución de la rama de reautenticación ni persistencia de borrador después de cerrar el navegador. No se acredita entrega Rabbit con eliminar eventos de fixture; publicación tiene su propia suite. El caso sí demuestra reinicio de proceso y recuperación sin dependencia de retención del outbox. UX/motores y gate final13 permanecen pendientes.
