# Dictamen final de pausa y reanudación

**APPROVED para cerrar feature15.** El ciclo de trabajo todavía requiere cierre de sesión16; el MVP conserva1–18 y no está desplegado.

Root reúne las revisiones independientes de backend, frontend y gates de mutación con sus propias revisiones HTTP, publicador, smoke y navegador. El mapa pause_resume_contract_evidence.md cubre39 escenarios con evidencia compuesta y límites explícitos:105 ejemplos declarados no se convierten en105 pruebas nuevas. No hay un defecto contractual bloqueante pendiente.

Evidencia:

- Init local:1890 Java/79 suites,1666 frontend/33 archivos y32 Node verdes. Falló únicamente el formato de un test; el delta estético y lint completo835359 lo resolvieron. Posteriormente el init remoto completo pasó sobre6b8f360, sin atribuir EXIT0 al init local anterior.
- CI34075064014 SUCCESS sobre6b8f360fb5ef932729931b4d91993d3f436655f8: init, build,108E2E y smoke real. Dos tests adicionales de recuperación en9fc414e pasan82/82 en su foco y tienen revisión independiente; su CI34076019493 está en curso. Producción no ha cambiado entre esos cortes.
- Smoke local0886c3 y remoto: PAUSE confirmado antes de perder respuesta, recuperación por key, RESUME posterior, publicación de dos eventos originales, reinicio real del backend conservando PostgreSQL y recuperación C/K sin outbox publicado. No se atribuye entrega exactly-once.
- UX revisada por root:515 medidas geométricas y35axe0, nominal/estados/texto200 en tres motores y zoom nativo Chromium200. Tabla30 y límites físicos en ux_pause_resume_session.md; no se declara certificación de toda pantalla o usabilidad humana.
- PIT original:523 KILLED/525 =99,6190476%;2 NO_COVERAGE, cero supervivientes, errores y timeouts. Rootd3ab4d/5c6e52 verificó XML F0B68A866A3BBBF29BC96284062A9EDDC2BFF2A81153654430BA9023EADC0D51;315 hashes actuales y antes/después intactos827a1d. Juez independiente acepta las dos salidas defensivas postcolisión para el protocolo actual de lock/replay, sin reclasificarlas ni fabricar estados inválidos.
- Stryker original:741/861 =86,0627177700348%;119 SURVIVED y1 NO_COVERAGE, cero errores/timeouts. Root e3c68b verificó recuentos y89 hashes idénticos. Los residuos quedan inventariados sin descontarlos del denominador.
- Dos oráculos prioritarios de recuperación se añadieron sin tocar producto. Replay separado: seis firmas objetivo Killed, verificadas por root7d93d5;17/18 Killed y1 RuntimeError adicional del runner,90 hashes intactos. Ese error no se cuenta como detección ni altera el score original.

Las mejoras restantes de cobertura, pruebas físicas y optimización de contenedores conservan sus límites documentados. No se exige100% para el umbral80 acordado, ni se omite un defecto observado para cumplir una fecha. La única corrección funcional de esta fase fue anunciar correctamente la consulta reintentada; está validada antes del corte productivo medido.

Se autoriza marcar15 done y preparar contrato16 bajo autorización global vigente. Mantener PR14 en borrador hasta completar la integración del corte final; no confundir cierre funcional local con merge o despliegue. La CI del commit posterior y de main se registrarán por separado. Si revelan un fallo, se resolverá antes de publicar el MVP.

Actualización posterior al dictamen: CI34076019493 SUCCESS sobre9fc414e completo (root6d29cf):1668 pruebas frontend,108E2E, init/build/smoke verdes. Los dos oráculos posteriores quedan así validados también por la suite completa remota. El cierre funcional se mantiene; la publicación final documental se controla desde ci_pause_resume.md.

CI34076959702 detectó después un fixture sensible al orden, sin cambio productivo entre ambos cortes. Corrección2e492a0 revisada en review_pause_resume_ci_fixture.md: respuestas por ruta y alerta específica,1668/1668 frontend verdes. PR14 permanece draft durante CI34077907038. El juez comprobó la influencia sobre la campaña original de mutación: sólo tres detecciones registradas dependían del test489; retirándolas conservadoramente,738/861=85,71% conserva el umbral. Root ef08ae verificó esos tres IDs649/666/823. No se reclasifican resultados ni se repite la campaña; detalles y límites en review_pause_resume_mutation_frontend.md.

Integración posterior verificada5443f0: CI34077907038 SUCCESS sobre2e492a0 con init/build/108E2E/smoke. PR14 fusionada a2026-09-07T03:11:50Z en main b2ea1f211068e7d93c74d0a8d7e8717ec04323c3 (bcb02f). El árbol de main coincide íntegramente con el head aprobado. CI posterior34078825723 se registra aparte;16 ya está en TDD sobre contrato revisado, sin modificar el corte15 congelado.
