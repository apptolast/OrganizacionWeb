# Contrato de inicio de trabajo14

Rol: gherkin_author. Base normativa aprobada: project-spec.md sección14,
commit f4a87c5 y review_start_work_spec.md. Ponytail full y Caveman lite.
Autorización global vigente; este corte espera revisión del coordinador antes
de TDD, sin solicitar otra autorización humana.

Se entrega features/start_work_session.feature con IDs estables @s1–@s42.
Sólo feature14 pasa de pending a spec_ready; descripción y acceptance remiten
al contrato. No se activan15–18 ni se habilita el temporizador para uso habitual
sin su ciclo posterior de pausa/cierre. No hay cambios de producción, pruebas
ejecutables, migraciones, configuración ni Git.

Mapa de familias:

- @s1–s5: hecho de inicio, límites1/1440, microsegundos, DST/medianoche,
  fallback histórico y ausencia de requisitos de planificación.
- @s6–s13: JSON cerrado, query en las cuatro rutas, precedencia, privacidad,
  elegibilidad y rango temporal. Los contratos comunes de11/13 se heredan
  explícitamente; no se copia su matriz completa.
- @s14–s20: replay inmutable, keys independientes, concurrencia entre tareas
  y propietarios, completed concurrente, atomicidad y fallos de persistencia.
- @s21–s27: ID/key/active, ausencia y503 diferenciados, reinicio real con DB
  conservada y publicación del noveno tipo sin prometer exactamente una vez.
- @s28–s42: duración elegida, actividad global del propietario, validación de
  intención y relación exacta con desviación de un microsegundo, incertidumbre,
  CSRF manual, recuperación, privacidad, foco y evidencia de30criterios UX.

Precisiones de composición: GET active puede devolver una sesión legítima
de otra tarea propia; no exige que coincida con la ruta abierta. POST y
recuperación de una intención sí comprueban su contexto conocido. El recibo
histórico no se rechaza por reloj o catálogo actuales. La observación de401
ocurre al entregar Response: el caso obsoleto difiere fetch antes de esa entrega,
sin inventar una ventana entre headers401 y el observador síncrono.

Verificación documental: Node estándar cuenta declaraciones, tags, When y
filas; JSON.parse valida feature_list.json. No hay parser Gherkin instalado
en las dependencias declaradas/locales ni módulo Python gherkin (ae30c9);
no se instala uno ni se presenta el recuento estático como parseo Gherkin.
El corte intermedio918a46 registró41 escenarios,27 outlines,108 filas y41 When;
el cierre añade @s42 de validación de active, sin otra fila de Examples.
Verificación final0c6c6d EXIT0:42 escenarios,27 outlines,108 filas de Examples,
15 escenarios simples y123 casos expandidos declarados; tags consecutivos,
un When por escenario, sin whitespace final y15–30 aún pending. Cada fila será un
ciclo futuro individual; ninguno de esos123 casos está ejecutado por este trabajo.
