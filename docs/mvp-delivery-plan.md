# Plan de entrega del MVP

Actualizado el7 de septiembre de2026 tras cerrar técnicamente las sesiones (feature16). Previsión revisada: **8–15 horas efectivas para las funciones restantes y su validación integrada**, más **4–8 horas para desplegar y comprobar el servidor**, cuando haya acceso, dominio y entorno utilizables. Confianza media-baja en software y baja en despliegue. Terminar hoy sigue siendo el objetivo; no hay garantía de fecha ni predicción de cuota. Las esperas externas no tienen una cota conocida.

## Alcance de entrega

El MVP corresponde a1–18: acceso privado, proyectos y subtareas, disponibilidad, planificación y replanificación, Hoy, sesiones con pausa/reanudación/cierre, aviso de fin y consulta de lo realizado. React/TypeScript/pnpm/SCSS, Java/Spring Boot hexagonal/EDA, PostgreSQL/RabbitMQ en monorepo, desplegado en el servidor del usuario.

19–30 siguen autorizadas para después: revisión semanal, personalización, vistas/campos, importación/exportación, API de integración, webhooks, calendarios, GitHub, otros conectores y automatizaciones. Su inventario por proveedor todavía debe acotarse antes de estimar el proyecto completo.

## Estado comprobado

- 1–13 cerradas con sus dictámenes; Replanificar fusionado y CI verde, incluido control de concurrencia, presupuesto y recuperación. Las PR alternativas y checkpoints anteriores están resueltos; detalle histórico en Git y progress/history.md.
- 14 inicio y15 pausa/reanudación cerradas y fusionadas. Main b2ea1f211068e7d93c74d0a8d7e8717ec04323c3 tiene CI34078825723 SUCCESS.
- 16 cierre: dictamen final APPROVED y estado done. Init final1.985 pruebas Java,1.721 frontend y36 del arnés; build correcto.115 E2E y12 pasos smoke, recuperación tras reinicio y pérdida de respuesta. UX515 medidas/35axe sin violaciones, tres motores y zoom real, con límites físicos/humanos documentados. PIT original516/520=99,23 % y Stryker1104/1275=86,59 % con dos errores de herramienta explícitos; replays dirigidos7/7 y15/15 sin cambiar los resultados originales. Corte final6c0a2bf publicado en PR15; CI34084817356 SUCCESS, fusionado como fede342 y CI posterior main34085908723 SUCCESS. La CI anterior34082838516 pasó sobre la misma producción antes de los refuerzos de tests/documentación.
- 17 aviso y ampliación: contrato aprobado de44 escenarios/132 ejemplos; implementación en curso con tres agentes. Backend completo, persistencia, HTTP y Rabbit revisados: 2.074 pruebas Java verdes. Smoke de reinicios y recuperación después de cerrar aprobado,13 PASS. Panel corregido revisado y guardado; integración final de interfaz, navegador y gates pendientes. PIT backend activo.18 historial pendiente de contrato.
- No hay despliegue productivo acreditado.

## Plan y estimación pendiente

| Hito | Resultado verificable | Horas efectivas |
| --- | --- | ---: |
|17 Aviso de fin y ampliación deliberada | Aviso comprobado, fin efectivo persistido y decisión explícita sin reescribir el inicio ni sumar trabajo ficticio; contrato, TDD y gates |3–5|
|18 Historial | Hechos propios con fechas, tiempo real y acceso al detalle; filtros/orden y paginación acotados por contrato; gates |2,5–4,5|
|Validación integrada del software | Recorrido completo, revisión y CI del corte conjunto; sin repetir campañas aprobadas por cambios sólo documentales |1–2|
|Margen de correcciones | Hallazgos de contrato, integración, oráculos o herramientas |1,5–3|
|Software restante | Rango redondeado de8–14,5 |**8–15**|
|Despliegue condicionado | Imágenes/configuración de infraestructura, HTTPS/acceso, salud, persistencia, reinicio, respaldo/restauración y reversión comprobados |**4–8 adicionales**|

Son horas transcurridas de ejecución coordinada, incluyendo revisión y esperas técnicas; no horas-persona ni un total que deba dividirse entre agentes. La nueva estimación sustituye la hipótesis anterior30–60. Su base observable es el ciclo16: contrato05:02:31, implementación integrada05:54:01 y cierre local06:54:12, aproximadamente112 minutos. Es una sola muestra y no incluye toda la preparación previa ni acredita un ritmo futuro garantizado. Los márgenes por novedad de17 y contrato abierto18 son juicio de planificación, no coeficientes estadísticos. El desglose y las duraciones reales de gates están en [la revisión de estimación](../progress/review_mvp_estimate_after_close.md).

## Secuencia y dependencias

1. Completado: PR15 fusionada y CI posterior de main correcta.
2. En curso: implementar17 sobre contrato aprobado, con reparto backend/frontend/HTTP y revisión independiente de paquetes. Conservar hora original, tiempo trabajado y recuperación.
3. Acotar18, implementar historial de hechos y validar el recorrido conjunto. No incluir silenciosamente estadísticas semanales, edición retrospectiva ni exportación dentro de este contrato.
4. Desplegar y comprobar en el servidor. El acceso SSH y el dominio ya están preguntados; el último intento rechazó publickey. No se ha hecho una nueva conexión ni se infieren credenciales.

La capacidad documentada45MiB era margen de un presupuesto histórico de límites, no RAM libre medida. Hay que comprobar capacidad, catálogo Swarm, volúmenes, secrets y respaldo reales. Compose y CI de aplicación no equivalen a imágenes publicadas ni a un servicio productivo. Ampliación de recursos, adopción DNS o respaldo inexistente requerirían estimación adicional al observarlos. Detalles en [pendientes de despliegue](../progress/mvp_deployment_remaining.md).

Se revisará el rango tras17, al aprobar el contrato18 y al conseguir la primera comprobación autenticada del servidor. No se promete finalizar antes del reinicio de cuota ni se presenta un porcentaje de proyecto terminado.

## Criterio de entrega

Los recorridos del MVP deben funcionar contra API y base de datos reales, conservar datos al reiniciar, proteger privacidad, recuperar errores y evitar duplicados. Planificación y trabajo real permanecen separados. Deben pasar contrato, revisión, tests y mutación, además de criterios responsive/accesibilidad con evidencia y límites explícitos. La entrega completa requiere HTTPS, acceso y respaldo/restauración comprobados en el servidor. Esto no equivale a ausencia absoluta de errores ni a certificación universal de las30 leyes UX por herramientas automáticas.

## Reestimación tras el cierre técnico local17, 7 de septiembre, 10:20

Esta actualización sustituye el rango de8–15 horas de software pendiente indicado al cerrar16: quedan **5–9 horas efectivas** para18 y la validación integrada final, con confianza media-baja. Despliegue conserva **4–8 horas adicionales condicionadas** a acceso, hostname y capacidad utilizables; esperas externas sin cota. Hoy sigue siendo objetivo, no garantía.

17 está done técnicamente en local: init2076Java/1802frontend/40Node, build,121E2E, smoke13, UX515/35axe y campañas/revisiones aprobadas con límites. PR16 y CI final todavía pendientes; no se presenta como fusionada ni desplegada. Los cambios posteriores a producción son refuerzos de tests, fixtures y documentación.

Desglose restante: contrato e implementación18,2,5–4,5h; integración/CI del MVP,1–2h; margen de correcciones,1,5–2,5h. Total5–9h, sin dividirlo de nuevo entre agentes. Se preparará18 en una rama separada mientras CI valida17, evitando modificar la PR que está ejecutándose. No se añaden revisión semanal, personalización avanzada ni conectores19–30 al MVP por inferencia.

Base de revisión:17 requirió más trabajo de integración que la única muestra previa16, especialmente coordinación entre decisiones y recuperación. Sus gates medidos incluyen PIT38m55, Stryker25m42, globalE2E corregido11,3min y replay2m12; se solaparon y no deben sumarse como ruta crítica. Este rango es juicio de planificación, no extrapolación estadística ni predicción de cuota. Se volverá a revisar al fijar el contrato18.
