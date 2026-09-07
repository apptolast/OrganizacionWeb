# Revisión de estimación del MVP tras cerrar sesiones

Revisión del 7 de septiembre de 2026, corregida con la medición temporal aportada por root. **Software pendiente 17–18 y validación integrada: 8–15 horas efectivas. Despliegue: otras 4–8 horas efectivas, condicionado a acceso y entorno utilizables.** Confianza media-baja para software y baja para despliegue. La espera externa por SSH, dominio o recursos queda fuera y no tiene una cota acreditada.

La primera propuesta de este documento, 22–40 horas, no justificaba su multiplicador frente al ciclo observado y queda retirada. La hipótesis histórica de 30–60 horas tampoco debe mantenerse como previsión vigente por inercia. Estos rangos son una nueva hipótesis revisable; no se modifica aquí el plan de entrega vigente ni se promete terminar hoy.

«Horas efectivas» significa tiempo transcurrido de ejecución coordinada, con revisión y esperas técnicas necesarias de pruebas/CI; excluye interrupciones por cuota, acceso o información externa. No son horas-persona acumuladas, no se dividen entre agentes ni permiten predecir cuota disponible.

## Evidencia temporal y límites de la muestra

Root verificó en 5eaae0 la secuencia de 16: contrato 5cbe0b6 a las 05:02:31 +02, corte core/frontend 2580d2f/1f5d5b7 a las 05:54:01 y cierre local 6c0a2bf a las 06:54:12. Son aproximadamente **112 minutos desde contrato hasta gates/documentación**, con unos 52 minutos hasta el corte funcional y otros 60 hasta el cierre local. Incluyó 41 escenarios/113 ejemplos, UX, smoke, PIT, Stryker y refuerzos. Se trata de una sola muestra, no de una velocidad garantizada ni de todo el tiempo previo de propuesta y diseño.

PIT alrededor de 35 minutos, frontend 19, E2E 10 y CI 15 dan unos 79 minutos si se suman en serie. El ciclo real permite solapamiento y algunas comprobaciones se incluyen dentro de CI; por eso no se añade esa suma otra vez a los 112 minutos. El cierre local tampoco acredita por sí solo publicación, CI posterior o servidor. La validación por feature se incluye en sus partidas; la validación final sólo reserva lo necesario sobre el corte conjunto.

## Software restante

| Partida | Horas efectivas | Fundamento frente a la muestra de 16 |
| --- | ---: | --- |
| 17: contrato final, aviso y ampliación, integración y gates propios | 3–5 | Aproximadamente 1,6–2,7 veces el ciclo medido de 1,87 h. La normativa ya existe, pero quedan Gherkin y mayor novedad de temporizadores/visibilidad; se modifica además la guarda temporal compartida de las decisiones anteriores. |
| 18: contrato, historial acotado, integración y gates propios | 2,5–4,5 | Aproximadamente 1,3–2,4 veces la muestra. Reutiliza lecturas paginadas y hechos durables, pero el contrato de filtros/orden aún no está fijado y necesita su propia validación de privacidad y navegación. |
| Validación del recorrido conjunto, publicación y cierre de entrega software | 1–2 | Un corte integrado y su revisión/CI, sin repetir campañas completas ya aprobadas si no cambió su código. |
| Margen de integración y correcciones no previstas | 1,5–3 | Incertidumbre adicional a las partidas funcionales: un hallazgo de contrato, fixture o selección de mutantes puede exigir corrección y medición focal. No reserva una reescritura de arquitectura. |
| Total aritmético | 8–14,5 | Se comunica **8–15 h** para evitar precisión aparente. |

Los multiplicadores son márgenes de planificación razonados, no coeficientes estadísticos. El extremo inferior requiere un flujo comparable al de 16 y reutilización fluida; el superior contempla iteraciones adicionales y una muestra más lenta. Si la instrumentación o los gates se alargan materialmente, se actualiza con el tiempo observado, sin recortar pruebas para sostener la cifra.

La normativa 17 leída en project-spec.md (b6109e) mantiene SessionStart7, State6 y recibos anteriores, pero añade fin efectivo, EXTEND7 y evento nuevo. Ampliar sólo cambia la revisión del estado, conserva intervalos/changedAt y exige una marca interna de última decisión que también controle posteriores PAUSE/RESUME/CLOSE. Hay migración, compatibilidad JSONB, atomicidad, replay y concurrencia reales. En frontend hay reloj monotónico, revalidación al vencimiento/visibilidad, coalescencia, limpieza de timers y privacidad tras await. Es más que un aviso de texto; esas novedades justifican no extrapolar 112 minutos directamente.

Para 18 se presupone una colección de hechos propios con filtros y orden/cursor acotados, acceso a sesiones ya conocidas, duración neta y notas/atribución persistidas. No se incluyen búsqueda general, métricas semanales, edición retrospectiva, exportación ni estadísticas nuevas. Si su contrato las incorpora, el rango debe rehacerse antes de implementación. No se inventa un número de escenarios para estimarla.

## Despliegue: partida separada y condicionada

**4–8 horas efectivas** es una provisión inicial para integrar imágenes/configuración en la infraestructura existente, comprobar secrets/origin/DNS/HTTPS y validar salud, persistencia tras reinicio, respaldo/restauración y reversión compatible. No está calibrada por una ejecución remota reciente; por eso su confianza es baja y no se mezcla con una fecha de software.

El último intento SSH documentado rechazó publickey. Hostname/origin definitivo siguen pendientes de respuestas ya solicitadas. No se reintenta ni se repiten preguntas desde esta revisión. Los 45 MiB históricos son margen del presupuesto agregado de límites, no memoria libre actual. Capacidad, catálogo Swarm, volúmenes y respaldo externo deben verificarse antes de dar por válido el supuesto de entorno utilizable.

Si se necesita ampliar recursos, resolver adopción de estado DNS o preparar un sistema de respaldo inexistente, ese trabajo adicional se estima por separado al conocerlo. No hay evidencia para asignar una cota a la espera de esas dependencias. La CI de aplicación y Compose local no equivalen a publicación de imágenes ni a un servicio desplegado.

## Secuencia y revisión

Camino funcional: contrato/gates 17 → contrato/gates 18 → corte integrado. La preparación documental de infraestructura puede avanzar en paralelo; su puesta en servicio depende del acceso y de la validación remota. Se mantiene una feature en implementación cada vez, con reparto interno por archivos, sin aplicar un factor de aceleración por agentes.

Revisar la previsión después de los gates de 17, al aprobar el contrato de 18 y al conseguir la primera comprobación autenticada del servidor. No se presenta un porcentaje completado ni se transforma el rango en días o garantía de cuota. 19–30 continúan autorizadas fuera del MVP y no están estimadas aquí.

Fuentes: normativa 17 actual de project-spec.md; progress/proposal_end_time_notification.md; docs/mvp-delivery-plan.md; progress/review_mvp_estimate.md; progress/mvp_deployment_remaining.md; docs/deployment-readiness.md. Lecturas d4cd84/52b396/0c23dd/b6109e, y cronología de commits verificada y aportada por root en 5eaae0. Sólo este documento se modifica; no plan vigente, código, infraestructura, SSH, suites ni Git.
