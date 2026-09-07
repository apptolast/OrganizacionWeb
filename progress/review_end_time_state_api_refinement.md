# Revisión independiente del refuerzo StateAPI17

**APPROVED, alcance de tres oráculos.** Diff a81a35:58 líneas añadidas, sin retirar aserciones ni cambiar producción. Test B1F53D3293EEFCED038C5FA3E5E0F55FD687B0D618B5A67E95BF0037B7EE9FA2 idéntico antes/después de lectura; fuente929352…CFAB8 coincide con freeze (96ae95).

- Estado: before paused y after closed admiten individualmente runningSince null, trabajo0 y changedAt inicial. Mantener revisión+1, sesión y aritmética evita que otro rechazo oculte la comparación de status que distingue1138/1142.
- Tiempo: ambos paused, changedAt del after avanza al occurredAt real del fixture; trabajo0 permanece válido y la fórmula EXTEND conserva fin previsto+1min. El único cambio indebido es changedAt, objetivo1147; no se usa running, cuya invariante runningSince habría enmascarado ese oráculo.
- Shape: recibo exterior y estados coherentes; extra:true sólo en extension. Cantidad1, instantes y fórmula válidos permiten distinguir1227/1228 sin depender de otra invalidez.

Los tres entran por readWorkSessionChange y esperan el error público controlado; no llaman validadores privados. Response.json simula la frontera HTTP del cliente, no acredita PostgreSQL ni una secuencia de comandos real. La bitácora conserva tres resultados inicialmente GREEN y final51/51/lint atribuida al autor; esta revisión no ejecutó suites.

Las cinco firmas son candidatos bien sustentados, no Killed anticipados. El replay deberá demostrar presencia por archivo/ubicación/mutador/reemplazo, inventariar extras y conservar original. No se exige ampliar casos ni perseguir1139, que no es objetivo de este paquete.
