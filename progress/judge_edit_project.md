# Revisión coordinada — edit_project

Estado final: **APPROVED localmente**. El coordinador no escribe producción ni tests. Ponytail full y Caveman lite aplicados. La CI del commit de edición se comprobará después de publicarlo; este dictamen no afirma despliegue en el servidor ni finalización del MVP.

Hallazgos comunicados a los autores durante TDD:

- Detalle y ETag deben proceder del mismo snapshot SQL. Implementado con ProjectSnapshot; evita cuerpo antiguo con versión nueva.
- La comprobación de versión y el no-op permanecen dentro de la transacción con bloqueo de fila propia. UPDATE conserva propietario/id/versión.
- UPDATE e INSERT outbox deben afectar exactamente una fila. Una escritura suprimida por un trigger no puede anunciar éxito; autor añade regresión real y guardas mínimas.
- La recarga de un conflicto debe bloquear guardar durante el GET; respuestas abortadas no deben repoblar otra ruta. El éxito anterior se retira cuando el usuario vuelve a escribir.
- El error del nombre debe permanecer junto al campo correspondiente. Se retira una comprobación duplicada de abort y se distingue un fallo de recarga de un resultado incierto de guardado.
- Contradicción documental resuelta: tipos/versiones no admitidos mantienen UNSUPPORTED_EVENT; payload incompatible mantiene INVALID_EVENT. No se cambia el contrato previo de publicación.
- La precondición no debe convertir etiquetas distintas en equivalentes al normalizar UUID o número. Formato emitido canónico, sin mayúsculas ni ceros iniciales, dentro de la validación estricta acordada. Referencia: [RFC 9110, comparación fuerte](https://www.rfc-editor.org/rfc/rfc9110.html#section-8.8.3.2).

Primera evidencia de integración: un recorrido real de dos pestañas pasó (primera200, segunda412, borrador conservado, recarga deliberada, nuevo200). No sustituye suite final, publicación, mutación ni revisión UX. La CI verde de read_projects pertenece a la entrega anterior.

Revisión frontend del corte estable: fuente de API/hook/componente, reutilización del validador de detalle y aislamiento por ruta revisados. Lint, build y suite de 115 tests verdes informados por el autor. La pérdida de foco al entrar desde el detalle se reprodujo y corrigió mediante foco inicial en h1; prueba focalizada y build posteriores verdes, 116 tests declarados. No se atribuye todavía ejecución global nueva de 116.

Inspección visual independiente: `outputs/edit-project-desktop.png`, `outputs/edit-project-mobile.png` y `outputs/edit-project-real-zoom-320.png` revisados por el coordinador. Formulario legible, campos y errores agrupados, acciones identificadas y apiladas en móvil, texto largo contenido por controles nativos. La captura de zoom muestra confirmación de guardado sin recortes de la página. Nombres y descripciones de las capturas son datos sintéticos. Pendientes la corrida conjunta final y el resultado de mutación; las capturas no certifican dispositivos físicos ni evaluación humana.

Regresión completa final: `node .harness/harness.mjs init`, sesión 8183, salida 0. Lint correcto, 240 pruebas backend y 122 frontend verdes. Recuento independiente de XML backend: cero fallos, errores y omitidos. Incluye los seis refuerzos de pruebas frontend posteriores al primer Stryker y la corrección de foco. El coordinador revisó el XML PIT: 125 resultados KILLED, sin otros estados. Pendientes integración final, broker caído y evaluación documentada de supervivientes frontend.

Revisión final frontend: **APPROVED** en su alcance de fuente, tests y mutación. El coordinador inspeccionó el JSON completo (209 Killed, 45 Survived, 1 NoCoverage) y el informe individual de supervivientes. Los seis refuerzos cierran 16 huecos observables; los restantes tienen equivalencia contextual razonada con el formulario y el montaje por ruta. El NoCoverage original era el filtrado de entradas inválidas; las nuevas pruebas lo recorren y el placeholder mutado no coincide con ningún campo del formulario. Se conservan las defensas de privacidad y validación, aunque algunos mutantes no alteren la salida observable. Replays 36/42 y 3/3 revisados por separado, sin sumarlos a una puntuación global ficticia. Umbral 80 % mantenido; resultado completo 81,96 %. Pendientes únicamente revisión independiente backend e integración final para el dictamen conjunto.

Dictamen conjunto final: revisión backend independiente APPROVED en `judge_edit_project_backend.md`; fuente de ampliación de `scripts/publisher-smoke.mjs` revisada por el coordinador. El script mantiene su fixture aislado, filtra explícitamente los dos tipos de evento y comprueba PUT200 con worker activo/broker detenido, reintento y recepción del evento original tras recuperar RabbitMQ. No modifica servicios del usuario. Su ejecución terminó con salida 0.

La corrida final de navegador pasó 18/18 E2E, incluyendo 22 anchos (matriz base y ambos lados de breakpoints), teclado/táctil y errores recuperables. Se conservan además dos recorridos reales Firefox/WebKit y la evidencia independiente de zoom nativo al 200 %. Matriz de 30 principios y límites revisada en `ux_edit_project.md`. No quedan bloqueos locales de corrección; la evaluación humana y de dispositivos físicos permanece explícitamente pendiente, sin atribuir una certificación global de accesibilidad.

Implementación publicada en `f8c1963cfe68cca85769b1e9efa3730b0160f861`. CI `33997062229` iniciada, resultado pendiente al registrar el cierre local. Los fixtures de integración y smoke fueron eliminados por sus propietarios, sin dejar servicios de prueba activos.
