# Revisión del primer E2E de replanificación

Checkpoint de prueba todavía RED; no aprobado como entrega funcional.

Root revisó el recorrido y la composición real de TaskBlocks en 728a90/d38fc3. El test crea por UI, mueve y cancela por endpoints reales; conserva identidad y creación, consulta recibos e historial y comprueba filas/eventos sin confundir cancelación con trabajo realizado. Los ETag de POST no forman parte del contrato: retirarlos del test antes de alcanzarlos corrigió un oráculo inventado, sin debilitar revision del recibo ni Location.

Revisión de extracción 5fe41e/7436dc: configure y openEditor mantienen lógica, defaults y controles; no quedan dependencias de la constante days retirada fuera del helper. Regresión del recorrido11 551ece verde según bitácora. Baseline dedicado f4721d verde antes de la nueva prueba.

Evidencia alcanzada del nuevo test: b2b65e devuelve 500 en preview en el checkpoint backend publicado; no se alcanzaron las aserciones posteriores. La implementación core se desarrolla en otro árbol. Conservar este fallo hasta integrar y comprobar el flujo completo; ninguna afirmación de GREEN parcial sustituye ese resultado.

La matriz de 30 criterios UX se ha preparado como plan, con todas sus verificaciones para13 pendientes. No acredita automáticamente resultados de Hoy ni de otras funcionalidades.
